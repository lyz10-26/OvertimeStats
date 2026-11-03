package cn.pi.overtime;
import java.time.*;
public class NoticeParserTest {
 static int n=0;static void check(boolean x){n++;if(!x)throw new AssertionError("case "+n);}
 static NoticeParser.Result p(String s){return NoticeParser.parse(s,LocalDateTime.of(2026,10,31,19,0).atZone(Rules.ZONE).toInstant().toEpochMilli());}
 public static void main(String[] args){
  NoticeParser.Result r=p("打卡结果\n11:08 上班打卡·成功\n班次时间 10月31日 11:08 上班\n今天休息,加班辛苦了\n2026年10月31日");check(r.certain);check(r.clock.equals("11:08"));check(r.kind.equals("in"));check(r.date.equals("2026-10-31"));check(r.rest);
  r=p("2026-10-30\n19:35 下班打卡·成功\n班次时间 10月30日18:00下班");check(r.certain);check(r.clock.equals("19:35"));
  check(!p("班次时间 2026年10月30日18:00 下班").certain);
  check(!p("2026-10-30 18:00 请及时下班打卡").certain);
  check(!p("11:08 上班打卡·成功").certain);
  check(!p("2026-10-30\n2026-10-31\n11:08 上班打卡·成功").certain);
  check(!p("2026-10-30\n08:30 上班打卡成功\n19:00 下班打卡成功").certain);
  check(!p("2026-10-31\n23:59 下班打卡成功").certain);
  check(!p("2026-02-30\n08:30 上班打卡成功").certain);
  check(!p("2026-10-30\n24:00 下班打卡成功").certain);
  check(!p("2026-10-30\n08:30 上班打卡失败").certain);
  check(p("2026年10月30日\n8：30 上班打卡：成功").certain);
  check(p("2026-10-30\n08:30 上班打卡成功\n08:30 上班打卡成功").certain);
  long posted=LocalDateTime.of(2026,10,31,19,9,15).atZone(Rules.ZONE).toInstant().toEpochMilli();
  String sample="工作通知:个人考勤\n考勤打卡：19:09 下班打卡·成功";
  r=NoticeParser.parse(sample,posted);check(r.certain);check(r.inferredDate);check(r.kind.equals("out"));check(r.clock.equals("19:09"));check(r.date.equals("2026-10-31"));
  check(!NoticeParser.parse(sample,posted,false).certain);
  check(NoticeParser.parse("考勤打卡：19:09 下班打卡·成功",posted,true).certain);
  check(!NoticeParser.parse("考勤打卡：昨天19:09 下班打卡·成功",posted,true).certain);
  check(!NoticeParser.parse("考勤打卡：补卡19:09 下班打卡·成功",posted,true).certain);
  check(!NoticeParser.parse("考勤打卡：10月30日19:09 下班打卡·成功",posted,true).certain);
  check(!NoticeParser.parse("考勤打卡：18:30 下班打卡·成功",posted,true).certain);
  check(!NoticeParser.parse("考勤打卡：19:10 下班打卡·成功",posted,true).certain);
  check(!NoticeParser.parse("工作通知\n考勤打卡：23:59 下班打卡·成功",LocalDateTime.of(2026,11,1,0,1).atZone(Rules.ZONE).toInstant().toEpochMilli()).certain);
  check(!NoticeParser.parse("工作通知\n考勤打卡：00:01 下班打卡·成功",LocalDateTime.of(2026,11,1,0,1).atZone(Rules.ZONE).toInstant().toEpochMilli()).certain);
  check(NoticeParser.parse("工作通知\n考勤打卡：19:09 下班打卡·成功",posted+86400000L).date.equals("2026-11-01"));
  long quickIn=LocalDateTime.of(2026,10,31,8,50,42).atZone(Rules.ZONE).toInstant().toEpochMilli();
  r=NoticeParser.parse("工作通知:民机外协人员\\n[3条]考勤打卡：\\n08:50 极速打卡·成功",quickIn,true);
  check(r.certain);check(r.kind.equals("auto"));check(r.clock.equals("08:50"));check(r.date.equals("2026-10-31"));
  long quickOut=LocalDateTime.of(2026,10,31,19,5,20).atZone(Rules.ZONE).toInstant().toEpochMilli();
  r=NoticeParser.parse("工作通知:民机外协人员\\n考勤打卡：19:05 极速打卡·成功",quickOut,true);
  check(r.certain);check(r.kind.equals("auto"));check(r.clock.equals("19:05"));
  check(!NoticeParser.parse("工作通知:民机外协人员\\n考勤打卡：08:50 极速打卡·失败",quickIn,true).certain);
  System.out.println("PASS: "+n+" notification parser assertions");
 }
}
