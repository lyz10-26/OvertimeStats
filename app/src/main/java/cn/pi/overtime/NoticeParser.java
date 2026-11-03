package cn.pi.overtime;
import java.time.*;import java.util.*;import java.util.regex.*;
/** Only the success-result line may supply an actual punch time; shift lines never do. */
public final class NoticeParser {
 public static class Result {public String date="",clock="",kind="",reason="";public boolean certain=false,rest=false,inferredDate=false;}
 static final Pattern DATE=Pattern.compile("(?<!\\d)(20\\d{2})[年/-](\\d{1,2})[月/-](\\d{1,2})(?:日)?(?!\\d)");
 static final Pattern RESULT=Pattern.compile("(?<!\\d)([0-2]?\\d[:：][0-5]\\d)(?:[:：][0-5]\\d)?\\s*(上班|下班|极速)打卡[ ·・:：\\-—]*成功");
 public static Result parse(String text,long received){return parse(text,received,text.contains("工作通知")&&text.contains("考勤打卡"));}
 public static Result parse(String text,long received,boolean attendanceSource){
  Result r=new Result();r.rest=text.contains("今天休息")||text.contains("休息日");Set<String> dates=new HashSet<>(),punches=new HashSet<>();Matcher d=DATE.matcher(text);
  try{while(d.find())dates.add(LocalDate.of(Integer.parseInt(d.group(1)),Integer.parseInt(d.group(2)),Integer.parseInt(d.group(3))).toString());}catch(Exception e){r.reason="日期格式异常";return r;}
  Matcher m=RESULT.matcher(text);try{while(m.find()){String clock=LocalTime.parse(m.group(1).replace('：',':'),java.time.format.DateTimeFormatter.ofPattern("H:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)).toString();punches.add((m.group(2).equals("上班")?"in":m.group(2).equals("下班")?"out":"auto")+"|"+clock);}}catch(Exception e){r.reason="打卡时间格式异常";return r;}
  if(punches.size()!=1){r.reason=punches.isEmpty()?"未找到含实际时间的打卡成功结果":"通知含多条打卡结果，请人工核对";return r;}
  String[] p=punches.iterator().next().split("\\|");r.kind=p[0];r.clock=p[1];
  LocalDateTime delivery=Instant.ofEpochMilli(received).atZone(Rules.ZONE).toLocalDateTime();
  if(dates.size()>1){r.date=delivery.toLocalDate().toString();r.reason="通知包含多个日期，请核对实际日期";return r;}
  if(dates.isEmpty()){
   r.date=delivery.toLocalDate().toString();r.inferredDate=true;
   if(!attendanceSource){r.reason="正文未提供完整日期或无法确认考勤通知来源，请人工核对";return r;}
   if(text.contains("昨天")||text.contains("昨日")||text.contains("前天")||text.contains("补卡")||text.contains("补打卡")){r.reason="疑似历史或补卡消息，不能按通知当天自动记录";return r;}
   java.util.regex.Matcher partial=Pattern.compile("(\\d{1,2})月(\\d{1,2})日").matcher(text);
   while(partial.find())if(Integer.parseInt(partial.group(1))!=delivery.getMonthValue()||Integer.parseInt(partial.group(2))!=delivery.getDayOfMonth()){r.reason="正文日期与通知日期不一致，请核对";return r;}
   LocalTime clock=LocalTime.parse(r.clock);
   if(clock.getHour()<6||delivery.getHour()<6){r.reason="凌晨打卡需确认所属工作日";return r;}
   long delay=Duration.between(delivery.toLocalDate().atTime(clock),delivery).getSeconds();
   if(delay<0||delay>600){r.reason="打卡时间与通知发布时间间隔异常，请核对日期";return r;}
  }else r.date=dates.iterator().next();
  LocalDateTime actual=Rules.time(LocalDate.parse(r.date),r.clock);
  if(actual.isAfter(delivery.plusMinutes(1))){r.reason="打卡时间晚于通知接收时间，请核对";return r;}
  r.certain=true;r.reason=r.kind.equals("auto")?"识别到极速打卡成功时间；上下班类型按当日最早/最后打卡规则确定":r.inferredDate?"打卡时间来自正文；日期按通知发布时间推定":"识别到完整日期、实际打卡时间及类型";return r;
 }
}
