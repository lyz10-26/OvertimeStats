package cn.pi.overtime;
import java.time.*;import java.util.*;import java.util.regex.*;
/** Only the success-result line may supply an actual punch time; shift lines never do. */
public final class NoticeParser {
 public static class Result {public String date="",clock="",kind="",reason="";public boolean certain=false,rest=false;}
 static final Pattern DATE=Pattern.compile("(?<!\\d)(20\\d{2})[年/-](\\d{1,2})[月/-](\\d{1,2})(?:日)?(?!\\d)");
 static final Pattern RESULT=Pattern.compile("(?<!\\d)([0-2]?\\d[:：][0-5]\\d)(?:[:：][0-5]\\d)?\\s*(上班|下班)打卡[ ·・:：\\-—]*成功");
 public static Result parse(String text,long received){
  Result r=new Result();r.rest=text.contains("今天休息")||text.contains("休息日");Set<String> dates=new HashSet<>(),punches=new HashSet<>();Matcher d=DATE.matcher(text);
  try{while(d.find())dates.add(LocalDate.of(Integer.parseInt(d.group(1)),Integer.parseInt(d.group(2)),Integer.parseInt(d.group(3))).toString());}catch(Exception e){r.reason="日期格式异常";return r;}
  Matcher m=RESULT.matcher(text);try{while(m.find()){String clock=LocalTime.parse(m.group(1).replace('：',':'),java.time.format.DateTimeFormatter.ofPattern("H:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)).toString();punches.add((m.group(2).equals("上班")?"in":"out")+"|"+clock);}}catch(Exception e){r.reason="打卡时间格式异常";return r;}
  if(punches.size()!=1){r.reason=punches.isEmpty()?"未找到含实际时间的打卡成功结果":"通知含多条打卡结果，请人工核对";return r;}
  String[] p=punches.iterator().next().split("\\|");r.kind=p[0];r.clock=p[1];
  if(dates.size()!=1){r.date=Instant.ofEpochMilli(received).atZone(Rules.ZONE).toLocalDate().toString();r.reason=dates.isEmpty()?"正文未提供完整打卡日期，候选日期仅供核对":"通知包含多个日期，请核对实际日期";return r;}
  r.date=dates.iterator().next();LocalDateTime actual=Rules.time(LocalDate.parse(r.date),r.clock),delivery=Instant.ofEpochMilli(received).atZone(Rules.ZONE).toLocalDateTime();
  if(actual.isAfter(delivery.plusMinutes(1))){r.reason="打卡时间晚于通知接收时间，请核对";return r;}
  r.certain=true;r.reason="识别到完整日期、实际打卡时间及类型";return r;
 }
}
