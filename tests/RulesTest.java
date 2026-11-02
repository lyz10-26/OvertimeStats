package cn.pi.overtime;
import java.time.*;import java.util.*;
public class RulesTest {
 static int assertions=0;
 static void eq(Object a,Object b){assertions++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 static long calc(String a,String b){LocalDate d=LocalDate.of(2026,10,30);return Rules.minutes(d,Rules.time(d,a),Rules.time(d,b),true,30);}
 public static void main(String[] args){
  eq(calc("08:30","18:20"),0L);eq(calc("08:30","18:30"),0L);eq(calc("08:30","18:31"),1L);eq(calc("08:30","19:00"),30L);
  eq(calc("08:40","19:10"),30L);eq(calc("08:50","20:00"),70L);eq(calc("09:00","20:00"),60L);eq(calc("08:00","19:00"),30L);
  eq(calc("09:01","20:00"),0L);eq(calc("","20:00"),0L);eq(calc("08:30",""),0L);eq(calc("08:30","08:00"),0L);
  eq(calc("08:30","次日 01:00"),390L);eq(calc("08:30:30","18:31:00"),0L);eq(calc("08:30:30","18:31:30"),1L);
  LocalDate d=LocalDate.of(2026,10,31);eq(Rules.minutes(d,Rules.time(d,"08:30"),Rules.time(d,"20:00"),false,30),630L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"18:00"),false,30),352L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"12:07"),false,30),0L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"12:08"),false,30),0L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"12:09"),false,30),1L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),null,false,30),0L);
  eq(Rules.minutes(d,null,Rules.time(d,"18:00"),false,30),0L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"次日 01:08"),false,30),780L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"18:00"),false,0),352L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"18:00"),false,180),352L);
  eq(Rules.minutes(d,Rules.time(d,"08:30"),Rules.time(d,"19:00"),true,30),30L);
  eq(Rules.minutes(d,Rules.time(d,"11:08"),Rules.time(d,"10:00"),false,30),0L);
  eq(Rules.start(d,Rules.time(d,"11:08"),false,30),d.atTime(12,8));
  LocalDateTime[] bounds=Rules.punchBounds(Arrays.asList(d.atTime(12,0),d.atTime(8,30),d.atTime(18,45),d.atTime(13,0)));
  eq(bounds[0],d.atTime(8,30));eq(bounds[1],d.atTime(18,45));
  LocalDateTime[] one=Rules.punchBounds(Collections.singletonList(d.atTime(9,5)));
  eq(one[0],d.atTime(9,5));eq(one[1],null);
  System.out.println("PASS: "+assertions+" calculation assertions");
 }
}
