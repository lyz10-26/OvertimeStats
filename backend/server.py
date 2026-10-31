"""Single-employee DingTalk read-only bridge. TLS termination is required."""
import os, json, time, threading, secrets, urllib.request, urllib.parse
from datetime import datetime, date, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

CST = timezone(timedelta(hours=8))
TOKEN_LOCK = threading.Lock()
CACHE = {'token': '', 'expires': 0}


def call_api(path, payload=None, query=None):
    url = 'https://oapi.dingtalk.com/' + path
    if query:
        url += '?' + urllib.parse.urlencode(query)
    req = urllib.request.Request(url, data=None if payload is None else json.dumps(payload).encode(),
                                 headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=20) as response:
        result = json.load(response)
    if result.get('errcode', -1) != 0:
        # Do not forward raw error strings/URLs: they can contain credentials.
        raise ValueError('DingTalk error code: ' + str(result.get('errcode')))
    return result


def access_token():
    with TOKEN_LOCK:
        if CACHE['expires'] > time.time():
            return CACHE['token']
        r = call_api('gettoken', query={'appkey': os.environ['DING_APP_KEY'],
                                       'appsecret': os.environ['DING_APP_SECRET']})
        CACHE.update(token=r['access_token'], expires=time.time() + max(0, int(r.get('expires_in', 7200)) - 120))
        return CACHE['token']


def stamp(value):
    return datetime.fromtimestamp(int(value) / 1000, CST)


def normalize(rows, user_id, start, end):
    days, seen = {}, set()
    for row in rows:
        if row.get('userId') != user_id:
            raise ValueError('Unexpected employee in DingTalk response')
        if row.get('timeResult') == 'NotSigned' or not row.get('userCheckTime'):
            continue
        work = stamp(row['workDate']).date()
        if not start <= work <= end:
            raise ValueError('DingTalk returned a work date outside query range')
        kind = {'OnDuty': 'in', 'OffDuty': 'out'}.get(row.get('checkType'))
        if kind is None:
            continue
        actual = stamp(row['userCheckTime']).strftime('%Y-%m-%d %H:%M:%S')
        ident = (str(work), kind, actual)
        if ident in seen:
            continue
        seen.add(ident)
        entry = days.setdefault(str(work), {'date': str(work), 'in': '', 'out': '',
                                'workday': work.weekday() < 5})
        if entry[kind] and entry[kind] != actual:
            raise ValueError('Multiple shifts in one day; please review shift configuration')
        entry[kind] = actual
        entry.setdefault('original', []).append({k: row.get(k) for k in
            ('id', 'workDate', 'checkType', 'userCheckTime', 'timeResult', 'locationResult')})
    return [days[k] for k in sorted(days)]


def get_records(start, end):
    user = os.environ['DING_USER_ID']
    rows = []
    chunk = start
    while chunk <= end:
        last = min(chunk + timedelta(days=6), end)
        for page in range(100):
            result = call_api('attendance/list', {
                'workDateFrom': str(chunk) + ' 00:00:00',
                'workDateTo': str(last) + ' 23:59:59',
                'userIdList': [user], 'offset': page * 50, 'limit': 50},
                {'access_token': access_token()})
            batch = result.get('recordresult', [])
            if not isinstance(batch, list):
                raise ValueError('Invalid DingTalk response')
            rows.extend(batch)
            if not result.get('hasMore', False):
                break
        else:
            raise ValueError('DingTalk pagination limit exceeded')
        chunk = last + timedelta(days=1)
    return normalize(rows, user, start, end)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass  # no request/credential logs

    def respond(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if not secrets.compare_digest(self.headers.get('Authorization', ''), 'Bearer ' + os.environ['BRIDGE_TOKEN']):
            self.respond(401, {'ok': False, 'error': 'unauthorized'})
            return
        try:
            parsed = urllib.parse.urlparse(self.path)
            if parsed.path != '/attendance':
                self.respond(404, {'ok': False})
                return
            args = urllib.parse.parse_qs(parsed.query)
            if set(args) != {'start', 'end'} or any(len(v) != 1 for v in args.values()):
                raise ValueError('Only start/end are supported; employee is fixed server-side')
            start, end = date.fromisoformat(args['start'][0]), date.fromisoformat(args['end'][0])
            today = datetime.now(CST).date()
            if start > end or (end-start).days > 30 or end > today or start < today - timedelta(days=180):
                raise ValueError('Query range must be at most 31 days within the recent 180 days')
        except (ValueError, KeyError) as exc:
            self.respond(400, {'ok': False, 'error': str(exc)})
            return
        try:
            self.respond(200, {'ok': True, 'records': get_records(start, end)})
        except Exception:
            self.respond(502, {'ok': False, 'error': 'DingTalk query failed; check application permissions, employee scope, data and connectivity'})


def main():
    for key in ('DING_APP_KEY', 'DING_APP_SECRET', 'DING_USER_ID', 'BRIDGE_TOKEN'):
        if not os.environ.get(key):
            raise SystemExit('Missing configuration: ' + key)
    if len(os.environ['BRIDGE_TOKEN']) < 32:
        raise SystemExit('BRIDGE_TOKEN must be at least 32 characters')
    # Bind to loopback; expose only through an HTTPS reverse proxy.
    server = ThreadingHTTPServer(('127.0.0.1', int(os.getenv('PORT', '8787'))), Handler)
    print('Attendance bridge listening on loopback. Configure HTTPS before connecting the APK.')
    server.serve_forever()


if __name__ == '__main__':
    main()
