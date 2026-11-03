package cn.pi.overtime;
import android.content.*;
import org.json.*;
import java.time.*;
import java.util.*;

public final class NoticeStore {
 public static final Object LOCK=new Object();
 static JSONObject load(SharedPreferences p)throws Exception{return new JSONObject(p.getString("records","{}"));}
 public static void merge(Context c,JSONObject changes)throws Exception{synchronized(LOCK){SharedPreferences p=c.getSharedPreferences("attendance",0);JSONObject all=load(p);Iterator<String> keys=changes.keys();while(keys.hasNext()){String k=keys.next();all.put(k,changes.get(k));}p.edit().putString("records",all.toString()).commit();}}
 static String noticeId(String key,long posted,String text){return key+"|"+Instant.ofEpochMilli(posted).atZone(Rules.ZONE).toLocalDate()+"|"+text;}
 static String applyParsed(SharedPreferences p,JSONObject note,JSONObject all)throws Exception{
  String state="待确认";
  NoticeParser.Result parsed=NoticeParser.parse(note.optString("text"),note.optLong("posted"),note.optBoolean("attendanceSource"));
  note.put("date",parsed.date);note.put("clock",parsed.clock);note.put("kind",parsed.kind);note.put("rest",parsed.rest);note.put("reason",parsed.reason);note.put("dateSource",parsed.inferredDate?"通知发布时间推定":"消息正文");
  if(parsed.certain&&!p.getBoolean("autoNotice",true))note.put("reason","已识别到打卡时间，但自动记录功能已关闭，请人工确认");
  if(parsed.certain&&p.getBoolean("autoNotice",true)){
   JSONObject r=all.optJSONObject(parsed.date);if(r==null)r=new JSONObject();
   if("手动修正".equals(r.optString("source"))||"钉钉通知·人工确认".equals(r.optString("source"))){note.put("reason","已有人工修正记录，请确认后更新");}
   else{
    LocalDate day=LocalDate.parse(parsed.date);ArrayList<LocalDateTime> punches=new ArrayList<>();
    JSONArray saved=r.optJSONArray("punchTimes");
    if(saved!=null){for(int i=0;i<saved.length();i++){try{punches.add(LocalDateTime.parse(saved.getString(i)));}catch(Exception ignored){}}}
    else{
     LocalDateTime previousIn=Rules.time(day,r.optString("in","")),previousOut=Rules.time(day,r.optString("out",""));
     if(previousIn!=null)punches.add(previousIn);if(previousOut!=null)punches.add(previousOut);
    }
    LocalDateTime actual=Rules.time(day,parsed.clock);boolean duplicate=punches.contains(actual);
    if(!duplicate)punches.add(actual);
    LocalDateTime[] bounds=Rules.punchBounds(punches);
    if(bounds[0]==null){note.put("reason","未取得有效打卡时间");}
    else{
     JSONObject candidate=new JSONObject(r.toString());JSONArray savedTimes=new JSONArray();ArrayList<LocalDateTime> ordered=new ArrayList<>(punches);Collections.sort(ordered);
     for(LocalDateTime punch:ordered)savedTimes.put(punch.toString());
     candidate.put("punchTimes",savedTimes);candidate.put("in",Rules.clock(day,bounds[0]));candidate.put("out",bounds[1]==null?"":Rules.clock(day,bounds[1]));
     candidate.put("source","钉钉通知采集");candidate.put("punchRule","最早打卡为上班，最后一次打卡为下班");
     if(!candidate.has("workday"))candidate.put("workday",!parsed.rest&&day.getDayOfWeek().getValue()<6);
     candidate.put("noticeEvidence",note.optString("text"));candidate.put("inDateSource",parsed.inferredDate?"通知发布时间推定":"消息正文");
     if(bounds[1]!=null)candidate.put("outDateSource",parsed.inferredDate?"通知发布时间推定":"消息正文");
     all.put(parsed.date,candidate);state=duplicate?"重复记录":"已自动记录";
    }
   }
  }
  note.put("state",state);note.put("processedAt",System.currentTimeMillis());return state;
 }
 static void saveNotes(SharedPreferences p,JSONArray notes,long posted,JSONObject all)throws Exception{
  SharedPreferences.Editor edit=p.edit().putString("notices",notes.toString()).putLong("noticeLast",Math.max(posted,p.getLong("noticeLast",0)));
  if(all!=null)edit.putString("records",all.toString());edit.commit();
 }
 public static void receive(Context c,String text,long posted,String key)throws Exception{receive(c,text,posted,key,text.contains("考勤打卡")||text.contains("工作通知")&&text.contains("考勤"));}
 public static void receive(Context c,String text,long posted,String key,boolean attendanceSource)throws Exception{
  synchronized(LOCK){SharedPreferences p=c.getSharedPreferences("attendance",0);if(!p.getBoolean("capture",true))return;
   JSONArray notes=new JSONArray(p.getString("notices","[]"));String id=noticeId(key,posted,text);JSONObject note=null;int existing=-1;
   for(int i=0;i<notes.length();i++){JSONObject old=notes.getJSONObject(i);if(key.equals(old.optString("notificationKey"))&&posted==old.optLong("posted")){note=old;existing=i;break;}}
   if(note==null){note=new JSONObject();note.put("id",id);note.put("receivedAt",System.currentTimeMillis());}
   note.put("notificationKey",key);note.put("text",text);note.put("posted",posted);note.put("attendanceSource",attendanceSource);
   JSONObject all=load(p);String state=applyParsed(p,note,all);JSONArray capped=new JSONArray();capped.put(note);
   for(int i=0;i<notes.length()&&capped.length()<100;i++)if(i!=existing)capped.put(notes.get(i));saveNotes(p,capped,posted,all);
   p.edit().putLong("lastCaptureAt",System.currentTimeMillis()).putString("lastCaptureResult",state).putString("lastCaptureReason",note.optString("reason","")).putString("lastCaptureNotificationKey",key).apply();
  }
 }
 public static void markClicked(Context c,String key,long posted,int reason){
  synchronized(LOCK){SharedPreferences p=c.getSharedPreferences("attendance",0);try{JSONArray notes=new JSONArray(p.getString("notices","[]"));long now=System.currentTimeMillis();boolean found=false;
   for(int i=0;i<notes.length();i++){JSONObject note=notes.getJSONObject(i);boolean match=key.equals(note.optString("notificationKey"))||(note.optString("id").startsWith(key+"|")&&note.optLong("posted")==posted);if(match){note.put("clicked",true);note.put("clickedAt",now);note.put("clickReason",reason);found=true;}}
   SharedPreferences.Editor e=p.edit().putLong("lastNoticeClickAt",now).putString("lastNoticeClickKey",key).putLong("lastNoticeClickPosted",posted).putBoolean("lastNoticeClickMatched",found).putInt("noticeClickCount",p.getInt("noticeClickCount",0)+1);
   if(found)e.putString("notices",notes.toString());e.commit();
  }catch(Exception ex){p.edit().putString("noticeError","通知点击状态保存失败").apply();}}
 }
 public static void markAppResumed(Context c){c.getSharedPreferences("attendance",0).edit().putLong("lastAppResumeAt",System.currentTimeMillis()).apply();}
 public static int recheck(Context c)throws Exception{
  synchronized(LOCK){SharedPreferences p=c.getSharedPreferences("attendance",0);JSONArray notes=new JSONArray(p.getString("notices","[]"));JSONObject all=load(p);int checked=0;
   for(int i=0;i<notes.length();i++){JSONObject note=notes.getJSONObject(i);if(!"待确认".equals(note.optString("state")))continue;String before=note.optString("state");String after=applyParsed(p,note,all);if(!before.equals(after))checked++;}
   p.edit().putString("notices",notes.toString()).putString("records",all.toString()).putLong("noticeRecheckAt",System.currentTimeMillis()).commit();return checked;
  }
 }
 public static void resolve(Context c,String id)throws Exception{synchronized(LOCK){SharedPreferences p=c.getSharedPreferences("attendance",0);JSONArray a=new JSONArray(p.getString("notices","[]"));for(int i=0;i<a.length();i++)if(id.equals(a.getJSONObject(i).optString("id")))a.getJSONObject(i).put("state","已人工确认");p.edit().putString("notices",a.toString()).commit();}}
}