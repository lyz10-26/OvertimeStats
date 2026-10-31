package cn.pi.overtime;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.*;import java.io.*;
/** Exposes only the verified update APK, and only via explicit URI grants. */
public final class UpdateProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public String getType(Uri u){return "application/vnd.android.package-archive";}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"/update.apk".equals(u.getPath())||!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(new File(getContext().getCacheDir(),"update.apk"),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){android.database.MatrixCursor c=new android.database.MatrixCursor(new String[]{"_display_name","_size"});if("/update.apk".equals(u.getPath()))c.addRow(new Object[]{"Overtime-update.apk",new File(getContext().getCacheDir(),"update.apk").length()});return c;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
