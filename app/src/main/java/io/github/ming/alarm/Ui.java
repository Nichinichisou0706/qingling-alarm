package io.github.ming.alarm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class Ui {
    public static final int BG=Color.rgb(245,246,249), INK=Color.rgb(38,49,58), GREEN=Color.rgb(49,126,116), MUTED=Color.rgb(111,122,130), PALE=Color.rgb(227,242,237), RED=Color.rgb(183,50,61);
    public static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static GradientDrawable bg(int color,int radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    public static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(c,4),0,dp(c,4));return t;}
    public static TextView title(Context c,String s,int size){TextView t=text(c,s,size,INK);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static LinearLayout card(Context c,LinearLayout parent,int color){LinearLayout l=column(c);l.setPadding(dp(c,20),dp(c,14),dp(c,20),dp(c,14));l.setBackground(bg(color,24,c));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,12);parent.addView(l,p);return l;}
    public static Button button(Context c,String label,boolean primary,Runnable action){Button b=new Button(c);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(primary?Color.WHITE:GREEN);b.setBackground(bg(primary?GREEN:PALE,18,c));b.setMinHeight(dp(c,52));b.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,10);b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
    public static LinearLayout page(Activity a){
        LinearLayout root=column(a);root.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipToPadding(true);
        LinearLayout body=column(a);body.setPadding(dp(a,22),dp(a,12),dp(a,22),dp(a,24));scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(a instanceof MainActivity main){
            LinearLayout nav=row(a);nav.setBackgroundColor(Color.WHITE);nav.setPadding(dp(a,8),dp(a,6),dp(a,8),dp(a,6));
            String[] titles={"设定闹钟","我的闹钟","系统设置"};
            for(int i=0;i<3;i++){
                final int target=i;Button b=button(a,titles[i],main.currentTab()==i,()->main.selectTab(target));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(a,52),1);lp.leftMargin=dp(a,3);lp.rightMargin=dp(a,3);nav.addView(b,lp);
            }
            root.addView(nav);
        }
        a.setContentView(root);
        if(android.os.Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets sys=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout()|android.view.WindowInsets.Type.ime());
            v.setPadding(sys.left,sys.top,sys.right,sys.bottom);return insets;
        });
        else root.setFitsSystemWindows(true);
        return body;
    }
}
