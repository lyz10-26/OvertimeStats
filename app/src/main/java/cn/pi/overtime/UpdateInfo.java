package cn.pi.overtime;
import org.json.*;import java.net.URI;
public final class UpdateInfo {
 public final int code;public final String version,url,sha,notes;
 public UpdateInfo(String json)throws Exception{JSONObject j=new JSONObject(json);code=j.getInt("versionCode");version=j.getString("versionName");url=j.getString("apkUrl");sha=j.getString("sha256");notes=j.optString("notes","");URI uri=new URI(url);if(code<1||!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!sha.matches("[0-9a-fA-F]{64}")||!"https".equals(uri.getScheme())||!"github.com".equals(uri.getHost())||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!uri.getPath().equals("/lyz10-26/OvertimeStats/releases/download/v"+version+"/Overtime-"+version+".apk"))throw new IllegalArgumentException("更新信息不完整或地址无效");}
 public boolean newer(int installed){return code>installed;}
}
