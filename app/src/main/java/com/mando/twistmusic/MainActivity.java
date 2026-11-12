package com.mando.twistmusic;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
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
    private GradientDrawable gradient(int a,int b,int r){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(r);return d;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(7),dp(8),dp(7));t.setIncludeFontPadding(true);return t;}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xff9992aa);e.setInputType(type);e.setTextSize(15);e.setPadding(dp(17),dp(13),dp(17),dp(13));e.setBackground(shape(0xff211a30,dp(16)));e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return e;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setPadding(dp(14),dp(9),dp(14),dp(9));b.setBackground(gradient(0xffff168d,0xff9824d8,dp(19)));b.setElevation(dp(3));b.setOnClickListener(v->action.run());return b;}
    private Button linkButton(String label,String url){
        Button b=new Button(this);b.setText(label);b.setTextColor(0xfff6eaff);
        b.setTextSize(11);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);
        b.setPadding(dp(5),dp(4),dp(5),dp(4));
        b.setBackground(shape(0xff2a1b3b,dp(15)));
        b.setOnClickListener(v->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
            catch(Exception e){setStatus("تعذر فتح الرابط. تأكد من وجود تطبيق مناسب.");}
        });
        return b;
    }
    private LinearLayout.LayoutParams wide(){return new LinearLayout.LayoutParams(-1,-2);}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams margin(int w,int h,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.bottomMargin=dp(bottom);return p;}
    private TextView label(String s){TextView t=text(s,13,0xffbcb2d0,true);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setPadding(0,dp(7),dp(3),dp(7));return t;}
    private LinearLayout panel(){LinearLayout p=vertical();p.setPadding(dp(16),dp(15),dp(16),dp(15));p.setBackground(shape(0xff1c1728,dp(22)));p.setElevation(dp(2));return p;}
    private void base(String subtitle){
        root=vertical();root.setPadding(dp(18),dp(14),dp(18),dp(18));root.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=vertical();content.setGravity(Gravity.CENTER_HORIZONTAL);content.setPadding(0,0,0,dp(14));content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        LinearLayout brand=horizontal();brand.setPadding(dp(15),dp(12),dp(15),dp(12));brand.setBackground(gradient(0xff35165b,0xff171025,dp(22)));
        TextView logo=text("♫",30,0xffffd45c,true);logo.setBackground(shape(0xff4c2675,dp(16)));brand.addView(logo,new LinearLayout.LayoutParams(dp(55),dp(55)));
        LinearLayout names=vertical();TextView title=text("TWIST MUSIC",20,Color.WHITE,true);title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);TextView sub=text(subtitle,12,0xffd6c2f2,false);sub.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);names.addView(title);names.addView(sub);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.rightMargin=dp(12);brand.addView(names,np);
        content.addView(brand,margin(-1,-2,15));
        LinearLayout links=horizontal();
        Button developer=linkButton("المطور  @Mando_js","https://t.me/Mando_js");
        Button channel=linkButton("قناة المطور","https://t.me/freeMohamedNet");
        LinearLayout.LayoutParams developerParams=new LinearLayout.LayoutParams(0,dp(46),1);
        developerParams.leftMargin=dp(4);developerParams.rightMargin=dp(4);
        links.addView(developer,developerParams);
        LinearLayout.LayoutParams channelParams=new LinearLayout.LayoutParams(0,dp(46),1);
        channelParams.leftMargin=dp(4);channelParams.rightMargin=dp(4);
        links.addView(channel,channelParams);
        content.addView(links,margin(-1,-2,12));
    }
    private void space(){content.addView(new View(this),new LinearLayout.LayoutParams(1,dp(10)));}
    private void addStatus(){LinearLayout box=panel();box.setBackground(shape(0xff292039,dp(15)));status=text("جاهز",12,0xffffd98a,false);status.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);box.addView(status);content.addView(box,margin(-1,-2,12));}
    private void setStatus(String s){runOnUiThread(()->{if(status!=null)status.setText(s);});}
    private void addFieldCaption(String caption){content.addView(label(caption),margin(-1,-2,4));}
    private void addPrimary(String caption,Runnable action){Button b=button(caption,action);content.addView(b,margin(-1,dp(54),10));}
    private void addFooter(){TextView f=text("قناة المطور  •  t.me/freeMohamedNet",11,0xff8e829f,false);content.addView(f,margin(-1,-2,2));}
    private void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}
    private void showLogin(){
        base("تسجيل الدخول إلى حسابك");
        LinearLayout intro=panel();intro.setBackground(gradient(0xffa51fc2,0xff4a176e,dp(22)));
        intro.addView(text("مرحباً بك في Twist Music",22,Color.WHITE,true));
        intro.addView(text("سجّل دخولك لإدارة رصيدك ومهامك بسهولة",13,0xffd9c8ed,false));
        content.addView(intro,margin(-1,-2,16));
        LinearLayout form=panel();
        TextView heading=text("تسجيل الدخول",19,Color.WHITE,true);heading.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);form.addView(heading);
        TextView note=text("أدخل رقم الهاتف المرتبط بحسابك",13,0xffcbb8e7,true);note.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);form.addView(note);
        phoneInput=field("رقم الهاتف  01XXXXXXXXX",3);LinearLayout.LayoutParams fp=wide();fp.topMargin=dp(10);fp.bottomMargin=dp(12);form.addView(phoneInput,fp);
        Button send=button("إرسال كود التحقق  ←",this::sendOtp);form.addView(send,wide());
        View gap=new View(this);form.addView(gap,new LinearLayout.LayoutParams(1,dp(14)));
        TextView codeLabel=text("رمز التحقق",14,0xffe6d8f5,true);codeLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);form.addView(codeLabel);
        codeInput=field("أدخل الرمز المكوّن من 6 أرقام",2);LinearLayout.LayoutParams cp=wide();cp.topMargin=dp(7);cp.bottomMargin=dp(12);form.addView(codeInput,cp);
        Button verify=button("تأكيد الكود والدخول",this::verifyOtp);form.addView(verify,wide());
        content.addView(form,margin(-1,-2,14));
        addStatus();addFooter();
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
        base("لوحة تحكم حسابك");
        LinearLayout welcome=panel();welcome.setBackground(gradient(0xff47216f,0xff21152f,dp(23)));
        TextView hi=text("مرحباً بعودتك 👋",19,Color.WHITE,true);hi.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);welcome.addView(hi);
        TextView account=text("الحساب  "+phone,12,0xffd6c2f2,false);account.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);welcome.addView(account);
        content.addView(welcome,margin(-1,-2,13));
        LinearLayout balanceCard=panel();balanceCard.setBackground(gradient(0xff6c35b2,0xff39205d,dp(23)));
        TextView cap=text("إجمالي رصيدك",13,0xffe3d0ff,true);cap.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);balanceCard.addView(cap);
        balanceView=text("💎  "+balance+" كوينز",30,0xffffdf79,true);balanceView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);balanceCard.addView(balanceView);
        TextView hint=text("رصيد المكافآت المتاح في حسابك",11,0xffdfc9ff,false);hint.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);balanceCard.addView(hint);
        content.addView(balanceCard,margin(-1,-2,12));
        LinearLayout usage=panel();
        TextView usageTitle=text("استهلاك الشهر",13,0xffcdbce8,true);usageTitle.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);usage.addView(usageTitle);
        usageView=text(monthlyUsed+" / "+MONTHLY_LIMIT+" وحدة",17,Color.WHITE,true);usageView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);usage.addView(usageView);
        View track=new View(this);GradientDrawable trackBg=shape(0xff3a3049,dp(8));track.setBackground(trackBg);LinearLayout.LayoutParams tp=wide();tp.height=dp(7);tp.topMargin=dp(8);usage.addView(track,tp);
        int percent=Math.min(100,(int)(monthlyUsed*100f/MONTHLY_LIMIT));
        View fill=new View(this);fill.setBackground(gradient(0xffd66bff,0xff8c43ff,dp(8)));FrameLayout bar=new FrameLayout(this);bar.addView(track,new FrameLayout.LayoutParams(-1,dp(7)));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(7),Gravity.RIGHT);bp.width=dp(260)*percent/100;bar.addView(fill,bp);usage.addView(bar,margin(-1,dp(7),0));
        content.addView(usage,margin(-1,-2,16));
        TextView actions=text("الخدمات المتاحة",17,Color.WHITE,true);actions.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);content.addView(actions,margin(-1,-2,8));
        addStatus();
        addPrimary("🎯   تجميع النقاط والمهام",this::collectTasks);
        addPrimary("📦   سحب الوحدات",this::packagesScreen);
        addPrimary("⚡   تجميع المهام + السحب",this::collectThenRedeem);
        Button refresh=button("↻   تحديث الرصيد",()->runTask("جارٍ تحديث الرصيد...",()->{refreshBalance();saveSession();runOnUiThread(this::updateStats);return "تم تحديث الرصيد";}));
        refresh.setBackground(shape(0xff302441,dp(17)));content.addView(refresh,margin(-1,dp(52),10));
        Button logout=button("تسجيل الخروج",this::logout);logout.setBackground(shape(0xff3a202d,dp(17)));content.addView(logout,margin(-1,dp(50),12));
        addFooter();
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
