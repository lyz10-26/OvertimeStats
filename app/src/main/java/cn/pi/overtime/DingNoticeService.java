package cn.pi.overtime;
import android.service.notification.*;import android.app.Notification;import android.os.*;import java.util.*;
public final class DingNoticeService extends NotificationListenerService {
 public static volatile boolean connected=false;
 public void onListenerConnected(){connected=true;getSharedPreferences("attendance",0).edit().putBoolean("listenerConnected",true).apply();}
 public void onListenerDisconnected(){connected=false;getSharedPreferences("attendance",0).edit().putBoolean("listenerConnected",false).apply();}
 public void onNotificationPosted(StatusBarNotification sbn){
  if(sbn==null||!"com.alibaba.android.rimet".equals(sbn.getPackageName()))return;
  Notification n=sbn.getNotification();if(n==null||(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return;
  Bundle b=n.extras;if(b==null)return;LinkedHashSet<String> parts=new LinkedHashSet<>();for(String k:new String[]{Notification.EXTRA_TITLE,Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT}){CharSequence t=b.getCharSequence(k);if(t!=null&&!t.toString().trim().isEmpty())parts.add(t.toString());}
  CharSequence[] lines=b.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);if(lines!=null)for(CharSequence l:lines)if(l!=null)parts.add(l.toString());
  StringBuilder s=new StringBuilder();for(String p:parts)s.append(p).append('\n');String text=s.toString().trim();
  if(!text.contains("打卡")&&!text.contains("考勤"))return;
  if(text.length()>12000)text=text.substring(0,12000);
  try{NoticeStore.receive(this,text,System.currentTimeMillis(),sbn.getKey());}catch(Exception e){getSharedPreferences("attendance",0).edit().putString("noticeError","通知处理失败，请在应用中检查记录").apply();}
 }
}
