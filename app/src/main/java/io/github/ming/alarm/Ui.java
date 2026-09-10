package io.github.ming.alarm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class Ui {
    public static final int BG=Color.rgb(244,247,244), INK=Color.rgb(25,58,52), GREEN=Color.rgb(38,121,109), MUTED=Color.rgb(96,119,111), PALE=Color.rgb(228,239,232);
    public static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static GradientDrawable bg(int color,int radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    public static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(c,4),0,dp(c,4));return t;}
    public static TextView title(Context c,String s,int size){TextView t=text(c,s,size,INK);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static LinearLayout card(Context c,LinearLayout parent,int color){LinearLayout l=column(c);l.setPadding(dp(c,20),dp(c,14),dp(c,20),dp(c,14));l.setBackground(bg(color,24,c));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,12);parent.addView(l,p);return l;}
    public static Button button(Context c,String label,boolean primary,Runnable action){Button b=new Button(c);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(primary?Color.WHITE:GREEN);b.setBackground(bg(primary?GREEN:PALE,18,c));b.setMinHeight(dp(c,52));b.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,10);b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
    public static LinearLayout page(Activity a){
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout body=column(a);body.setPadding(dp(a,22),dp(a,16),dp(a,22),dp(a,28));scroll.addView(body);a.setContentView(scroll);
        if(android.os.Build.VERSION.SDK_INT>=30) scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets sys=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
            v.setPadding(sys.left,sys.top,sys.right,sys.bottom);return insets;
        });
        else scroll.setFitsSystemWindows(true);
        return body;
    }
}
