package cn.pi.overtime;
import java.time.*;
import java.time.format.*;
import java.util.*;
public final class Rules {
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 public static LocalDate today(){return LocalDate.now(ZONE);}
 public static LocalDateTime time(LocalDate date,String s){
  if(s==null||s.trim().isEmpty()||s.equals("--"))return null;
  s=s.trim().replace('T',' ');
  if(s.startsWith("次日"))return LocalDateTime.of(date.plusDays(1),LocalTime.parse(s.substring(2).trim(),DateTimeFormatter.ofPattern("H:mm[:ss]").withResolverStyle(ResolverStyle.STRICT)));
  if(s.contains(" "))return LocalDateTime.parse(s,DateTimeFormatter.ofPattern("uuuu-MM-dd H:mm[:ss]").withResolverStyle(ResolverStyle.STRICT));
  return LocalDateTime.of(date,LocalTime.parse(s,DateTimeFormatter.ofPattern("H:mm[:ss]").withResolverStyle(ResolverStyle.STRICT)));
 }
 public static LocalDateTime end(LocalDate d,LocalDateTime in){
  int sec=in.toLocalTime().toSecondOfDay();sec=Math.max(30600,Math.min(32400,sec));
  return d.atStartOfDay().plusSeconds(sec+34200);
 }
 public static boolean normalLateWindow(LocalDateTime in){
  if(in==null)return false;LocalTime t=in.toLocalTime();
  return !t.isBefore(LocalTime.of(9,0))&&!t.isAfter(LocalTime.of(9,30));
 }
 public static String status(LocalDate d,LocalDateTime in,LocalDateTime out,boolean work){
  if(in==null)return "缺少上班打卡";
  if(!in.toLocalDate().equals(d))return "上班日期异常";
  if(work&&in.toLocalTime().isAfter(LocalTime.of(9,30)))return "迟到记录 · 待确认";
  if(out==null)return "缺少下班打卡";
  if(out.isBefore(in)||out.isAfter(d.plusDays(1).atTime(12,0)))return "打卡顺序异常";
  return "已确认";
 }
 public static long roundDownHalfHour(long minutes){return Math.max(0,minutes)/30*30;}
 public static long minutes(LocalDate d,LocalDateTime in,LocalDateTime out,boolean work,int buffer){
  if(!status(d,in,out,work).equals("已确认"))return 0;
  long raw=Math.max(0,Duration.between(start(d,in,work,buffer),out).toMinutes());
  if(work&&normalLateWindow(in))raw=Math.max(0,raw-30);
  return roundDownHalfHour(raw);
 }
 public static LocalDateTime start(LocalDate d,LocalDateTime in,boolean work,int buffer){return work?end(d,in).plusMinutes(buffer):in.plusHours(1);}
 public static LocalDateTime[] punchBounds(Collection<LocalDateTime> punches){
  if(punches==null||punches.isEmpty())return new LocalDateTime[]{null,null};
  ArrayList<LocalDateTime> sorted=new ArrayList<>(punches);Collections.sort(sorted);
  return new LocalDateTime[]{sorted.get(0),sorted.size()>1?sorted.get(sorted.size()-1):null};
 }
 public static long monthlyTargetMinutes(int workdays,int ratioPercent){if(workdays<0)throw new IllegalArgumentException("workdays");if(ratioPercent!=15&&ratioPercent!=20)throw new IllegalArgumentException("ratioPercent");return workdays*480L*ratioPercent/100L;}
 public static String duration(long m){String sign=m<0?"-":"";long value=Math.abs(m);String hours=String.format(Locale.ROOT,"%.1f",value/60.0);if(hours.endsWith(".0"))hours=hours.substring(0,hours.length()-2);return sign+hours+"h";}
 public static String clock(LocalDate d,LocalDateTime t){if(t==null)return "--:--";return (t.toLocalDate().isAfter(d)?"次日 ":"")+t.format(DateTimeFormatter.ofPattern("HH:mm"));}
}
