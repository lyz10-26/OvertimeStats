package cn.pi.overtime;
import android.content.*;import org.json.*;import java.time.*;import java.util.*;
public final class NoticeStore {
 public static final Object LOCK=new Object();
 static JSONObject load(android.content.SharedPreferences p)throws Exception{return new JSONObject(p.getString("records","{}"));}
 public static void merge(Context c,JSONObject changes)throws Exception{synchronized(LOCK){android.content.SharedPreferences p=c.getSharedPreferences("attendance",0);JSONObject all=load(p);Iterator<String> keys=changes.keys();while(keys.hasNext()){String k=keys.next();all.put(k,changes.get(k));}p.edit().putString("records",all.toString()).commit();}}
 public static void receive(Context c,String text,long posted,String key)throws Exception{receive(c,text,posted,key,text.contains("工作通知")&&text.contains("考勤打卡"));}
 public static void receive(Context c,String text,long posted,String key,boolean attendanceSource)throws Exception{synchronized(LOCK){android.content.SharedPreferences p=c.getSharedPreferences("attendance",0);if(!p.getBoolean("capture",true))return;
  JSONArray notes=new JSONArray(p.getString("notices","[]"));String id=key+"|"+Instant.ofEpochMilli(posted).atZone(Rules.ZONE).toLocalDate()+"|"+text;for(int i=0;i<notes.length();i++)if(id.equals(notes.getJSONObject(i).optString("id")))return;
  NoticeParser.Result parsed=NoticeParser.parse(text,posted,attendanceSource);JSONObject note=new JSONObject();note.put("id",id);note.put("text",text);note.put("posted",posted);note.put("date",parsed.date);note.put("clock",parsed.clock);note.put("kind",parsed.kind);note.put("rest",parsed.rest);note.put("reason",parsed.reason);note.put("dateSource",parsed.inferredDate?"通知发布时间推定":"消息正文");note.put("attendanceSource",attendanceSource);String state="待确认";
  if(parsed.certain&&p.getBoolean("autoNotice",true)){
   JSONObject all=load(p);JSONObject r=all.optJSONObject(parsed.date);if(r==null)r=new JSONObject();String old=r.optString(parsed.kind,"");
   if(!old.isEmpty())state=old.equals(parsed.clock)||old.equals(parsed.date+" "+parsed.clock)?"重复记录":"待确认";
   else if(r.optString("source").equals("手动修正")){note.put("reason","已有手动修正记录，请确认后更新");}
   else{
    JSONObject candidate=new JSONObject(r.toString());candidate.put(parsed.kind,parsed.clock);LocalDate day=LocalDate.parse(parsed.date);LocalDateTime in=Rules.time(day,candidate.optString("in","")),out=Rules.time(day,candidate.optString("out",""));
    if(in!=null&&out!=null&&out.isBefore(in)){note.put("reason","上下班顺序异常或跨天，请人工确认所属工作日");}
    else{candidate.put("source","钉钉通知采集");if(!candidate.has("workday"))candidate.put("workday",!parsed.rest&&day.getDayOfWeek().getValue()<6);candidate.put("noticeEvidence",text);candidate.put(parsed.kind+"DateSource",parsed.inferredDate?"通知发布时间推定":"消息正文");all.put(parsed.date,candidate);p.edit().putString("records",all.toString()).commit();state="已自动记录";}
   }
   if(!old.isEmpty()&&state.equals("待确认"))note.put("reason","与已有打卡时间冲突，请确认是否更新");
  }
  note.put("state",state);JSONArray capped=new JSONArray();capped.put(note);for(int i=0;i<Math.min(notes.length(),99);i++)capped.put(notes.get(i));p.edit().putString("notices",capped.toString()).putLong("noticeLast",Math.max(posted,p.getLong("noticeLast",0))).commit();
 }}
 public static int recheck(Context c)throws Exception{android.content.SharedPreferences p=c.getSharedPreferences("attendance",0);JSONArray notes; synchronized(LOCK){notes=new JSONArray(p.getString("notices","[]"));}int n=0;for(int i=0;i<notes.length();i++){JSONObject note=notes.getJSONObject(i);if(!note.optString("state").equals("待确认"))continue;String raw=note.optString("text");long posted=note.optLong("posted",0);if(posted<=0)continue;receive(c,raw,posted,"recheck",note.optBoolean("attendanceSource",raw.contains("工作通知")&&raw.contains("考勤打卡")));synchronized(LOCK){JSONArray updated=new JSONArray(p.getString("notices","[]"));boolean done=false;for(int j=0;j<updated.length();j++){JSONObject a=updated.getJSONObject(j);if(a.optString("text").equals(raw)&&a.optLong("posted")==posted&&(a.optString("state").equals("已自动记录")||a.optString("state").equals("重复记录")))done=true;}if(done){for(int j=0;j<updated.length();j++)if(updated.getJSONObject(j).optString("id").equals(note.optString("id")))updated.getJSONObject(j).put("state","已重新识别");p.edit().putString("notices",updated.toString()).commit();}}n++;}return n;}
 public static void resolve(Context c,String id)throws Exception{synchronized(LOCK){android.content.SharedPreferences p=c.getSharedPreferences("attendance",0);JSONArray a=new JSONArray(p.getString("notices","[]"));for(int i=0;i<a.length();i++)if(id.equals(a.getJSONObject(i).optString("id")))a.getJSONObject(i).put("state","已人工确认");p.edit().putString("notices",a.toString()).commit();}}
}
