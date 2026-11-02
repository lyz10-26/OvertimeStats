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
  if(parsed.certain&&p.getBoolean("autoNotice",true)){
   JSONObject r=all.optJSONObject(parsed.date);if(r==null)r=new JSONObject();String old=r.optString(parsed.kind,"");
   if(!old.isEmpty())state=old.equals(parsed.clock)||old.equals(parsed.date+" "+parsed.clock)?"重复记录":"待确认";
   else if("手动修正".equals(r.optString("source"))){note.put("reason","已有手动修正记录，请确认后更新");}
   else{
    JSONObject candidate=new JSONObject(r.toString());candidate.put(parsed.kind,parsed.clock);
    LocalDate day=LocalDate.parse(parsed.date);LocalDateTime in=Rules.time(day,candidate.optString("in","")),out=Rules.time(day,candidate.optString("out",""));
    if(in!=null&&out!=null&&out.isBefore(in)){note.put("reason","上下班顺序异常或跨天，请人工确认所属工作日");}
    else{candidate.put("source","钉钉通知采集");if(!candidate.has("workday"))candidate.put("workday",!parsed.rest&&day.getDayOfWeek().getValue()<6);candidate.put("noticeEvidence",note.optString("text"));candidate.put(parsed.kind+"DateSource",parsed.inferredDate?"通知发布时间推定":"消息正文");all.put(parsed.date,candidate);state="已自动记录";}
   }
   if(!old.isEmpty()&&state.equals("待确认"))note.put("reason","与已有打卡时间冲突，请确认是否更新");
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
   JSONArray notes=new JSONArray(p.getString("notices","[]"));String id=noticeId(key,posted,text);
   for(int i=0;i<notes.length();i++)if(id.equals(notes.getJSONObject(i).optString("id")))return;
   JSONObject note=new JSONObject();note.put("id",id);note.put("notificationKey",key);note.put("text",text);note.put("posted",posted);note.put("receivedAt",System.currentTimeMillis());note.put("attendanceSource",attendanceSource);
   JSONObject all=load(p);applyParsed(p,note,all);JSONArray capped=new JSONArray();capped.put(note);for(int i=0;i<Math.min(notes.length(),99);i++)capped.put(notes.get(i));saveNotes(p,capped,posted,all);
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