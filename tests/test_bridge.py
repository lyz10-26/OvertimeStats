import unittest, sys
from pathlib import Path
from datetime import date, datetime
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'backend'))
import server
class BridgeTests(unittest.TestCase):
 def row(self, kind, clock, **extra):
  x={'userId':'me','workDate':int(datetime(2026,10,30,tzinfo=server.CST).timestamp()*1000),'checkType':kind,'userCheckTime':int(datetime.fromisoformat('2026-10-30T'+clock).replace(tzinfo=server.CST).timestamp()*1000),'timeResult':'Normal'}
  x.update(extra);return x
 def norm(self, rows):return server.normalize(rows,'me',date(2026,10,30),date(2026,10,30))
 def test_pair(self):
  r=self.norm([self.row('OnDuty','08:40'),self.row('OffDuty','19:10')])[0]
  self.assertTrue(r['workday']);self.assertEqual(r['in'],'2026-10-30 08:40:00');self.assertEqual(r['out'],'2026-10-30 19:10:00')
 def test_missing_does_not_become_scheduled_time(self):self.assertEqual(self.norm([self.row('OffDuty','18:00',timeResult='NotSigned')]),[])
 def test_employee_scope(self):
  with self.assertRaises(ValueError):self.norm([self.row('OnDuty','08:30',userId='other')])
 def test_multi_shift(self):
  with self.assertRaises(ValueError):self.norm([self.row('OnDuty','08:30'),self.row('OnDuty','13:30')])
 def test_repeat_idempotent(self):
  r=self.row('OnDuty','08:30');self.assertEqual(len(self.norm([r,r])),1)
 def test_outside_range(self):
  with self.assertRaises(ValueError):server.normalize([self.row('OnDuty','08:30')],'me',date(2026,10,31),date(2026,10,31))
 def test_missing_off(self):self.assertEqual(self.norm([self.row('OnDuty','08:30')])[0]['out'],'')
if __name__=='__main__':unittest.main()
