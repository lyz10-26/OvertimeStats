package cn.pi.overtime;
import android.service.notification.*;
import android.app.Notification;
import android.os.*;
import android.content.pm.*;
import java.util.*;

public final class DingNoticeService extends NotificationListenerService {
 public static volatile boolean connected=false;
 public void onListenerConnected(){connected=true;getSharedPreferences("attendance",0).edit().putBoolean("listenerConnected",true).apply();try{StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification sbn:active)onNotificationPosted(sbn);}catch(Exception e){}}
 public void onListenerDisconnected(){connected=false;getSharedPreferences("attendance",0).edit().putBoolean("listenerConnected",false).apply();}
 public void onNotificationPosted(StatusBarNotification sbn){
  if(sbn==null||!"com.alibaba.android.rimet".equals(sbn.getPackageName()))return;
  Notification n=sbn.getNotification();if(n==null||(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return;
  Bundle b=n.extras;if(b==null)return;
  LinkedHashSet<String> parts=new LinkedHashSet<>();
  for(String k:new String[]{Notification.EXTRA_TITLE,Notification.EXTRA_TITLE_BIG,Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT,Notification.EXTRA_SUB_TEXT,Notification.EXTRA_SUMMARY_TEXT}){
   CharSequence t=b.getCharSequence(k);if(t!=null&&!t.toString().trim().isEmpty())parts.add(t.toString().trim());
  }
  CharSequence[] lines=b.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);if(lines!=null)for(CharSequence l:lines)if(l!=null&&!l.toString().trim().isEmpty())parts.add(l.toString().trim());
  Parcelable[] messages=b.getParcelableArray(Notification.EXTRA_MESSAGES);if(messages!=null)for(Parcelable item:messages)if(item instanceof Bundle){Bundle message=(Bundle)item;CharSequence body=message.getCharSequence("text");if(body!=null&&!body.toString().trim().isEmpty())parts.add(body.toString().trim());}
  if(n.tickerText!=null&&!n.tickerText.toString().trim().isEmpty())parts.add(n.tickerText.toString().trim());
  StringBuilder joined=new StringBuilder();for(String part:parts)joined.append(part).append('\n');String text=joined.toString().trim();
  if(text.length()==0||(!text.contains("打卡")&&!text.contains("考勤")))return;
  if(text.length()>12000)text=text.substring(0,12000);
  try{CharSequence title=b.getCharSequence(Notification.EXTRA_TITLE);String titleText=title==null?"":title.toString();boolean attendance=titleText.contains("考勤")||text.contains("考勤打卡")||titleText.contains("工作通知")&&text.contains("打卡");NoticeStore.receive(this,text,sbn.getPostTime(),sbn.getKey(),attendance);}
  catch(Exception e){getSharedPreferences("attendance",0).edit().putString("noticeError","通知处理失败，请在应用中检查记录").apply();}
 }
 public void onNotificationRemoved(StatusBarNotification sbn,RankingMap rankingMap,int reason){
  super.onNotificationRemoved(sbn,rankingMap,reason);
  if(sbn==null||!"com.alibaba.android.rimet".equals(sbn.getPackageName()))return;
  if(reason==NotificationListenerService.REASON_CLICK)NoticeStore.markClicked(this,sbn.getKey(),sbn.getPostTime(),reason);
 }
}