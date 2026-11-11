package com.mando.twistmusic;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Iterator;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String BASE="https://api.twistmena.com/music";
    private static final int BG=Color.rgb(10,8,19), PANEL=Color.rgb(29,23,43), PURPLE=Color.rgb(153,54,255);
    private static final int MONTHLY_LIMIT=2000;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private EditText phoneInput,codeInput;
    private TextView status,balanceView,usageView;
    private LinearLayout root,content;
    private String phone="",token="",accessToken="",tgToken="",refreshToken="";
    private int balance=0,monthlyUsed=0;
    private final int[] units={50,100,150,300,500,750,1000};
    private final int[] costs={100,200,300,600,1000,1500,2000};
    private final String[] productIds={"EAND_50_UNITS_ID_9","EAND_100_UNITS_ID_10","EAND_150_UNITS_ID_11","EAND_300_UNITS_ID_12","EAND_500_UNITS_ID_13","EAND_750_UNITS_ID_14","EAND_1000_UNITS_ID_15"};
    private final String[] packageIcons={"🌱","⭐","🌟","💎","👑","🔥","🚀"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("twist_private_session",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        loadSession();
        if(!token.isEmpty()) showDashboard(); else showLogin();
    }
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setPadding(12,10,12,10);return t;}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffaaa5b8);e.setInputType(type);e.setPadding(16,12,16,12);e.setBackground(shape(PANEL,18));return e;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(shape(PURPLE,18));b.setOnClickListener(v->action.run());return b;}
    private LinearLayout.LayoutParams wide(){return new LinearLayout.LayoutParams(-1,-2);}
    private void base(String subtitle){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,18,20,18);root.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);content.setGravity(Gravity.CENTER_HORIZONTAL);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        content.addView(text("♬  TWIST MUSIC",29,Color.WHITE,true));
        content.addView(text(subtitle,14,0xffd5b9ff,false));
        content.addView(text("DEVELOPER  @Mando_js",12,0xffc5aaff,true));
    }
    private void space(){content.addView(new View(this),new LinearLayout.LayoutParams(1,12));}
    private void addStatus(){status=text("",13,0xffffd27a,false);content.addView(status);}
    private void setStatus(String s){runOnUiThread(()->{if(status!=null)status.setText(s);});}
    private void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}
    private void showLogin(){
        base("إدارة حسابك ونقاطك في Twist");
        space();content.addView(text("تسجيل الدخول",21,Color.WHITE,true));
        phoneInput=field("رقم الهاتف مثال 01XXXXXXXXX",3);content.addView(phoneInput,wide());
        space();content.addView(button("إرسال كود التحقق",this::sendOtp),wide());
        codeInput=field("كود التحقق المكوّن من 6 أرقام",2);content.addView(codeInput,wide());
        space();content.addView(button("تأكيد الكود وتسجيل الدخول",this::verifyOtp),wide());
        space();addStatus();content.addView(text("قناة المطور: t.me/freeMohamedNet",12,0xffbcb5d1,false));
    }
    private String formatPhone(String p){p=p.trim().replace(" ","").replace("+","");if(p.startsWith("0"))return "20"+p.substring(1);if(p.startsWith("20"))return p;return "20"+p;}
    private JSONObject request(String path,String method,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();
        c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(25000);
        c.setRequestProperty("user-agent","Twist-Mobile/11.2.10 (Android; 15; music; ar-EG)");
        c.setRequestProperty("app_version","11.2.10");c.setRequestProperty("appversion","11.2.10");
        c.setRequestProperty("channel","mobileapp");c.setRequestProperty("platform","android");
        c.setRequestProperty("accept","application/json");c.setRequestProperty("accept-language","ar");
        c.setRequestProperty("content-type","application/json");c.setRequestProperty("device_id","AP3A.240905.015.A2");
        c.setRequestProperty("sessionid",UUID.randomUUID().toString());
        if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
        if(!accessToken.isEmpty())c.setRequestProperty("access-token",accessToken);
        if(!tgToken.isEmpty())c.setRequestProperty("tg-token",tgToken);
        if(!refreshToken.isEmpty())c.setRequestProperty("tg-refresh-token",refreshToken);
        if(body!=null){c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();InputStream in=(code>=200&&code<400)?c.getInputStream():c.getErrorStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(in!=null){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();}
        String raw=out.toString("UTF-8");c.disconnect();
        if(code<200||code>=300)throw new Exception("استجابة الخادم: "+code+(raw.isEmpty()?"":" — "+raw));
        return raw.isEmpty()?new JSONObject():new JSONObject(raw);
    }
    interface Task{String run()throws Exception;}
    private void runTask(String loading,Task task){
        hideKeyboard();setStatus(loading);
        worker.execute(()->{try{String result=task.run();setStatus(result);}catch(Exception e){setStatus("تعذر إكمال الطلب: "+e.getMessage());}});
    }
    private void sendOtp(){
        String p=phoneInput.getText().toString().trim();
        if(p.length()<8){setStatus("أدخل رقم هاتف صحيحاً");return;}
        phone=formatPhone(p);
        runTask("جارٍ إرسال كود التحقق...",()->{JSONObject b=new JSONObject();b.put("dial",phone);request("/Dlogin/sendCode","POST",b);return "تم إرسال طلب الكود. افحص هاتفك.";});
    }
    private void verifyOtp(){
        String code=codeInput.getText().toString().trim();
        if(code.length()!=6||!code.matches("[0-9]{6}")){setStatus("أدخل كود التحقق المكوّن من 6 أرقام");return;}
        runTask("جارٍ التحقق من الكود...",()->{
            JSONObject b=new JSONObject();b.put("dial",phone);b.put("verifyCode",code);b.put("socialServiceName","");b.put("socialServiceToken","");
            JSONObject d=request("/Dlogin/verify","POST",b);
            token=d.optString("token");accessToken=d.optString("accessToken");tgToken=d.optString("tgToken");refreshToken=d.optString("refreshToken");
            if(token.isEmpty())throw new Exception("لم يرجع الخادم رمز جلسة صالحاً");
            refreshBalance();saveSession();runOnUiThread(this::showDashboard);return "تم تسجيل الدخول بنجاح";
        });
    }
    private void refreshBalance()throws Exception{balance=request("/user/loyalty/balance/details","GET",null).optInt("balance",0);}
    private void saveSession(){
        prefs.edit().putString("phone",phone).putString("token",token).putString("access",accessToken)
            .putString("tg",tgToken).putString("refresh",refreshToken).putInt("balance",balance)
            .putInt("monthly",monthlyUsed).apply();
    }
    private void loadSession(){
        phone=prefs.getString("phone","");token=prefs.getString("token","");accessToken=prefs.getString("access","");
        tgToken=prefs.getString("tg","");refreshToken=prefs.getString("refresh","");
        balance=prefs.getInt("balance",0);monthlyUsed=prefs.getInt("monthly",0);
    }
    private void updateStats(){
        if(balanceView!=null)balanceView.setText("💎 الرصيد: "+balance+" كوينز");
        if(usageView!=null)usageView.setText("📊 استهلاك الشهر: "+monthlyUsed+" / "+MONTHLY_LIMIT+" وحدة");
    }
    private void showDashboard(){
        base("لوحة التحكم");
        space();content.addView(text("مرحباً بعودتك",19,Color.WHITE,true));
        content.addView(text("الحساب: "+phone,14,0xffd5cce8,false));
        balanceView=text("💎 الرصيد: "+balance+" كوينز",23,0xffffd27a,true);content.addView(balanceView);
        usageView=text("📊 استهلاك الشهر: "+monthlyUsed+" / "+MONTHLY_LIMIT+" وحدة",13,0xffc5aaff,false);content.addView(usageView);
        space();addStatus();
        content.addView(button("🎯 تجميع النقاط والمهام",this::collectTasks),wide());space();
        content.addView(button("📦 سحب الوحدات",this::packagesScreen),wide());space();
        content.addView(button("⚡ تجميع + سحب",this::collectThenRedeem),wide());space();
        content.addView(button("🔄 تحديث الرصيد",()->runTask("جارٍ تحديث الرصيد...",()->{refreshBalance();saveSession();runOnUiThread(this::updateStats);return "تم تحديث الرصيد";})),wide());space();
        content.addView(button("🚪 تسجيل الخروج",this::logout),wide());space();
        content.addView(text("قناة المطور: t.me/freeMohamedNet",12,0xffbcb5d1,false));
    }
    private int collectBadges(JSONObject data)throws Exception{
        int done=0;JSONArray categories=data.optJSONArray("badges");
        if(categories!=null)for(int i=0;i<categories.length();i++){
            JSONObject cat=categories.optJSONObject(i);JSONArray tasks=cat==null?null:cat.optJSONArray("badges");if(tasks==null)continue;
            for(int j=0;j<tasks.length();j++){
                JSONObject task=tasks.optJSONObject(j);if(task==null||task.optBoolean("rewarded"))continue;
                String id=task.optString("id","");if(id.isEmpty())continue;
                try{request("/loyalty/action/"+URLEncoder.encode(id,"UTF-8"),"POST",null);done++;}catch(Exception ignored){}
            }
        }
        return done;
    }
    private int collectMilestones(JSONObject data)throws Exception{
        int done=0;JSONArray progress=data.optJSONArray("progress");if(progress==null)return 0;
        for(int i=0;i<progress.length();i++){
            JSONObject p=progress.optJSONObject(i);if(p==null)continue;
            String id=p.optString("id","");int current=p.optInt("progress",0);JSONObject milestones=p.optJSONObject("milestoneToCoins");
            if(id.isEmpty()||milestones==null)continue;
            Iterator<String> keys=milestones.keys();
            while(keys.hasNext()){
                String key=keys.next();int milestone;
                try{milestone=Integer.parseInt(key);}catch(Exception ignored){continue;}
                if(current<milestone)continue;
                JSONObject body=new JSONObject();body.put("milestone",milestone);
                try{request("/loyalty/action/"+URLEncoder.encode(id,"UTF-8"),"POST",body);done++;}catch(Exception ignored){}
            }
        }
        return done;
    }
    private String collectInternal()throws Exception{
        JSONObject data=request("/user/loyalty/achievements/v2","GET",null);
        int count=collectBadges(data)+collectMilestones(data);
        refreshBalance();saveSession();runOnUiThread(this::updateStats);
        return count==0?"لا توجد مهام أو مكافآت مراحل جديدة متاحة الآن":"تم إرسال طلبات إكمال "+count+" من المهام والمراحل. راجع الرصيد لتأكيد المكافآت.";
    }
    private void collectTasks(){runTask("جارٍ جمع المهام والمكافآت...",this::collectInternal);}
    private void packagesScreen(){
        String[] labels=new String[units.length];
        for(int i=0;i<units.length;i++){
            boolean allowed=balance>=costs[i]&&monthlyUsed+units[i]<=MONTHLY_LIMIT;
            labels[i]=packageIcons[i]+"  "+units[i]+" وحدة  ←  "+costs[i]+" كوينز  "+(allowed?"✓":"غير متاح");
        }
        new AlertDialog.Builder(this).setTitle("اختر باقة السحب").setItems(labels,(dialog,which)->confirmRedeem(which)).setNegativeButton("إغلاق",null).show();
    }
    private void confirmRedeem(int i){
        if(monthlyUsed+units[i]>MONTHLY_LIMIT){setStatus("هذه الباقة ستتجاوز الحد الشهري 2000 وحدة");return;}
        if(balance<costs[i]){setStatus("رصيدك غير كافٍ. تحتاج "+costs[i]+" كوينز");return;}
        new AlertDialog.Builder(this).setTitle("تأكيد الاستبدال")
            .setMessage("الباقة: "+units[i]+" وحدة\\nالتكلفة: "+costs[i]+" كوينز\\nهل تريد إرسال طلب السحب؟")
            .setNegativeButton("إلغاء",null).setPositiveButton("تأكيد",(d,w)->redeem(i)).show();
    }
    private String redeemInternal(int i)throws Exception{
        request("/loyalty/redeem/"+productIds[i],"POST",null);
        monthlyUsed+=units[i];refreshBalance();saveSession();runOnUiThread(this::updateStats);
        return "تم إرسال طلب استبدال "+units[i]+" وحدة. تحقق من حسابك لتأكيد النتيجة.";
    }
    private void redeem(int i){runTask("جارٍ إرسال طلب السحب...",()->redeemInternal(i));}
    private void collectThenRedeem(){
        String[] labels=new String[units.length+1];labels[0]="تجميع فقط دون سحب";
        for(int i=0;i<units.length;i++)labels[i+1]=packageIcons[i]+"  "+units[i]+" وحدة مقابل "+costs[i]+" كوينز";
        new AlertDialog.Builder(this).setTitle("تجميع ثم اختيار السحب").setItems(labels,(d,which)->{
            if(which==0){collectTasks();return;}
            final int index=which-1;
            new AlertDialog.Builder(this).setTitle("تأكيد العملية")
                .setMessage("سيتم تجميع المهام أولاً، ثم محاولة سحب "+units[index]+" وحدة مقابل "+costs[index]+" كوينز إذا كان الرصيد كافياً.")
                .setNegativeButton("إلغاء",null).setPositiveButton("ابدأ",(x,y)->runTask("جارٍ تجميع المهام...",()->{
                    String result=collectInternal();
                    if(balance<costs[index])return result+" — الرصيد لا يكفي لسحب الباقة المختارة.";
                    if(monthlyUsed+units[index]>MONTHLY_LIMIT)return result+" — تجاوز الحد الشهري يمنع السحب.";
                    return result+"\\n"+redeemInternal(index);
                })).show();
        }).setNegativeButton("إلغاء",null).show();
    }
    private void logout(){
        new AlertDialog.Builder(this).setTitle("تسجيل الخروج").setMessage("هل تريد حذف الجلسة المحفوظة من هذا الجهاز؟")
            .setNegativeButton("إلغاء",null).setPositiveButton("خروج",(d,w)->{
                prefs.edit().clear().apply();phone="";token="";accessToken="";tgToken="";refreshToken="";
                balance=0;monthlyUsed=0;showLogin();
            }).show();
    }
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}
