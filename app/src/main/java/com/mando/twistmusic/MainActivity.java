package com.mando.twistmusic;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String BASE="https://api.twistmena.com/music";
    private static final int BG=Color.rgb(12,12,20), PANEL=Color.rgb(27,25,40), PURPLE=Color.rgb(142,87,255);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private EditText phoneInput,codeInput;
    private TextView status,balanceView;
    private LinearLayout root,content;
    private String phone="",token="",accessToken="",tgToken="",refreshToken="";
    private int balance=0,monthlyUsed=0;
    private final int[] units={50,100,150,300,500,750,1000};
    private final int[] costs={100,200,300,600,1000,1500,2000};
    private final String[] productIds={"EAND_50_UNITS_ID_9","EAND_100_UNITS_ID_10","EAND_150_UNITS_ID_11","EAND_300_UNITS_ID_12","EAND_500_UNITS_ID_13","EAND_750_UNITS_ID_14","EAND_1000_UNITS_ID_15"};

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);showLogin();}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setPadding(12,10,12,10);return t;}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffaaa5b8);e.setInputType(type);e.setPadding(16,12,16,12);e.setBackground(shape(PANEL,18));return e;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(shape(PURPLE,18));b.setOnClickListener(v->action.run());return b;}
    private LinearLayout.LayoutParams wide(){return new LinearLayout.LayoutParams(-1,-2);}
    private void base(String subtitle){root=new LinearLayout(this);root.setOrientation(1);root.setPadding(22,24,22,20);root.setBackgroundColor(BG);ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);content.setGravity(Gravity.CENTER_HORIZONTAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);content.addView(text("♬  TWIST MUSIC",27,Color.WHITE,true));content.addView(text(subtitle,13,0xffbcb5d1,false));content.addView(text("DEVELOPER  @Mando_js",12,0xffc5aaff,true));}
    private void space(){content.addView(new View(this),new LinearLayout.LayoutParams(1,12));}
    private void addStatus(){status=text("",13,0xffffcf72,false);content.addView(status);}
    private void setStatus(String s){runOnUiThread(()->{if(status!=null)status.setText(s);});}
    private void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}
    private void showLogin(){base("إدارة حسابك ونقاط Twist");space();content.addView(text("تسجيل الدخول",20,Color.WHITE,true));phoneInput=field("رقم الهاتف مثال 01XXXXXXXXX",3);content.addView(phoneInput,wide());space();content.addView(button("إرسال كود التحقق",this::sendOtp),wide());codeInput=field("كود التحقق المكوّن من 6 أرقام",2);content.addView(codeInput,wide());space();content.addView(button("تأكيد الكود وتسجيل الدخول",this::verifyOtp),wide());space();addStatus();content.addView(text("قناة المطور: t.me/freeMohamedNet",12,0xffbcb5d1,false));}
    private String formatPhone(String p){p=p.trim().replace(" ","").replace("+","");if(p.startsWith("0"))return "20"+p.substring(1);if(p.startsWith("20"))return p;return "20"+p;}
    private JSONObject request(String path,String method,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(25000);
        c.setRequestProperty("user-agent","Twist-Mobile/11.2.10 (Android; 15; music; ar-EG)");c.setRequestProperty("app_version","11.2.10");c.setRequestProperty("appversion","11.2.10");c.setRequestProperty("channel","mobileapp");c.setRequestProperty("platform","android");c.setRequestProperty("accept","application/json");c.setRequestProperty("accept-language","ar");c.setRequestProperty("content-type","application/json");c.setRequestProperty("device_id","AP3A.240905.015.A2");c.setRequestProperty("sessionid",UUID.randomUUID().toString());
        if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);if(!accessToken.isEmpty())c.setRequestProperty("access-token",accessToken);if(!tgToken.isEmpty())c.setRequestProperty("tg-token",tgToken);if(!refreshToken.isEmpty())c.setRequestProperty("tg-refresh-token",refreshToken);
        if(body!=null){c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();InputStream in=(code>=200&&code<400)?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();if(in!=null){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}String raw=out.toString("UTF-8");c.disconnect();if(code<200||code>=300)throw new Exception("استجابة الخادم: "+code+" "+raw);return raw.isEmpty()?new JSONObject():new JSONObject(raw);
    }
    interface Task{String run()throws Exception;}
    private void runTask(String loading,Task task){hideKeyboard();setStatus(loading);worker.execute(()->{try{String result=task.run();setStatus(result);}catch(Exception e){setStatus("تعذر إكمال الطلب: "+e.getMessage());}});}
    private void sendOtp(){String p=phoneInput.getText().toString().trim();if(p.length()<8){setStatus("أدخل رقم هاتف صحيحاً");return;}phone=formatPhone(p);runTask("جارٍ إرسال كود التحقق...",()->{JSONObject b=new JSONObject();b.put("dial",phone);request("/Dlogin/sendCode","POST",b);return "تم إرسال طلب الكود. افحص هاتفك.";});}
    private void verifyOtp(){String code=codeInput.getText().toString().trim();if(code.length()!=6){setStatus("أدخل كود التحقق الصحيح");return;}runTask("جارٍ التحقق...",()->{JSONObject b=new JSONObject();b.put("dial",phone);b.put("verifyCode",code);b.put("socialServiceName","");b.put("socialServiceToken","");JSONObject d=request("/Dlogin/verify","POST",b);token=d.optString("token");accessToken=d.optString("accessToken");tgToken=d.optString("tgToken");refreshToken=d.optString("refreshToken");if(token.isEmpty())throw new Exception("لم يرجع الخادم رمز جلسة صالحاً");refreshBalance();runOnUiThread(this::showDashboard);return "تم تسجيل الدخول بنجاح";});}
    private void refreshBalance()throws Exception{balance=request("/user/loyalty/balance/details","GET",null).optInt("balance",0);}
    private void showDashboard(){base("لوحة التحكم");space();content.addView(text("الحساب: "+phone,15,Color.WHITE,true));balanceView=text("💎 الرصيد: "+balance+" كوينز",22,0xffffcf72,true);content.addView(balanceView);content.addView(text("الحد الشهري للوحدات: "+monthlyUsed+" / 2000",13,0xffc5aaff,false));space();addStatus();content.addView(button("🔄 تحديث الرصيد",()->runTask("جارٍ تحديث الرصيد...",()->{refreshBalance();runOnUiThread(()->balanceView.setText("💎 الرصيد: "+balance+" كوينز"));return "تم تحديث الرصيد";})),wide());space();content.addView(button("🎯 تنفيذ المهام المتاحة",this::collectTasks),wide());space();content.addView(text("باقات الوحدات",18,Color.WHITE,true));for(int i=0;i<units.length;i++){final int index=i;content.addView(button(units[i]+" وحدة  •  "+costs[i]+" كوينز",()->redeem(index)),wide());}space();content.addView(button("تسجيل الخروج",()->{token="";accessToken="";tgToken="";refreshToken="";phone="";showLogin();}),wide());space();content.addView(text("قناة المطور: t.me/freeMohamedNet",12,0xffbcb5d1,false));}
    private void collectTasks(){runTask("جارٍ جلب المهام...",()->{JSONObject data=request("/user/loyalty/achievements/v2","GET",null);int done=0;JSONArray categories=data.optJSONArray("badges");if(categories!=null)for(int i=0;i<categories.length();i++){JSONObject cat=categories.optJSONObject(i);JSONArray tasks=cat==null?null:cat.optJSONArray("badges");if(tasks==null)continue;for(int j=0;j<tasks.length();j++){JSONObject t=tasks.optJSONObject(j);if(t==null||t.optBoolean("rewarded"))continue;String id=t.optString("id");if(id.isEmpty())continue;request("/loyalty/action/"+URLEncoder.encode(id,"UTF-8"),"POST",null);done++;}}refreshBalance();runOnUiThread(()->balanceView.setText("💎 الرصيد: "+balance+" كوينز"));return done==0?"لا توجد مهام جديدة متاحة حالياً":"تم إرسال طلبات "+done+" مهمة. المكافآت تخضع لتأكيد الخدمة.";});}
    private void redeem(int i){if(monthlyUsed+units[i]>2000){setStatus("تجاوزت الحد الشهري المسموح");return;}if(balance<costs[i]){setStatus("رصيدك لا يكفي لهذه الباقة");return;}new AlertDialog.Builder(this).setTitle("تأكيد الاستبدال").setMessage("استبدال "+costs[i]+" كوينز مقابل "+units[i]+" وحدة؟").setNegativeButton("إلغاء",null).setPositiveButton("تأكيد",(d,w)->runTask("جارٍ إرسال طلب الاستبدال...",()->{request("/loyalty/redeem/"+productIds[i],"POST",null);monthlyUsed+=units[i];refreshBalance();runOnUiThread(()->balanceView.setText("💎 الرصيد: "+balance+" كوينز"));return "تم إرسال طلب الاستبدال. راجع حسابك لتأكيد النتيجة."; })).show();}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}
