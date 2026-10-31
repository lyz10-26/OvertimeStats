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
  System.out.println("PASS: "+n+" notification parser assertions");
 }
}
