package cn.pi.overtime;
import android.view.View;import android.content.Context;import android.graphics.*;
public final class LineIcon extends View {
 final String name;final int color;final Paint p=new Paint(3);
 public LineIcon(Context c,String n,int ink){super(c);name=n;color=ink;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 protected void onDraw(Canvas original){super.onDraw(original);Canvas c=original; c.save();float size=Math.min(getWidth(),getHeight());c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24,size/24);p.setColor(color);p.setStrokeWidth(1.7f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
 if(name.equals("today")){c.drawRoundRect(4,5,20,21,4,4,p);c.drawLine(4,10,20,10,p);c.drawLine(8,3,8,7,p);c.drawLine(16,3,16,7,p);c.drawLine(8,14,11,14,p);c.drawLine(8,17,15,17,p);}
 else if(name.equals("stats")){c.drawLine(5,20,5,13,p);c.drawLine(12,20,12,5,p);c.drawLine(19,20,19,9,p);}
 else if(name.equals("settings")){c.drawLine(4,7,20,7,p);c.drawLine(4,17,20,17,p);c.drawCircle(9,7,2.5f,p);c.drawCircle(16,17,2.5f,p);}
 else if(name.equals("plus")){c.drawLine(5,12,19,12,p);c.drawLine(12,5,12,19,p);}
 else if(name.equals("back")){c.drawLine(15,5,8,12,p);c.drawLine(8,12,15,19,p);}
 c.restore();}
}
