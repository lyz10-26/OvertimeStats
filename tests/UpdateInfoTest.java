package cn.pi.overtime;
public class UpdateInfoTest {
 static int n=0;static void ok(boolean b){n++;if(!b)throw new AssertionError("update case "+n);}
 static String sample="{\"versionCode\":8,\"versionName\":\"1.4.0\",\"apkUrl\":\"https://github.com/lyz10-26/OvertimeStats/releases/download/v1.4.0/Overtime-1.4.0.apk\",\"sha256\":\""+new String(new char[64]).replace('\0','a')+"\"}";
 static void bad(String s){boolean fail=false;try{new UpdateInfo(s);}catch(Exception e){fail=true;}ok(fail);}
 public static void main(String[] a)throws Exception{UpdateInfo u=new UpdateInfo(sample);ok(u.newer(7));ok(!u.newer(8));ok(!u.newer(9));bad(sample.replace("https:","http:"));bad(sample.replace("github.com","evil.example"));bad(sample.replace("lyz10-26","other"));bad(sample.replace("OvertimeStats","other"));bad(sample.replace("1.4.0.apk","1.4.0.apk?token=x"));bad(sample.replace("\"versionCode\":8","\"versionCode\":0"));bad(sample.replace("aaaaaaaa","zzzzzzzz"));bad("{}");System.out.println("PASS: "+n+" update metadata assertions");}
}
