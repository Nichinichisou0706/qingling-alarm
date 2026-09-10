package io.github.ming.alarm;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original vector mascot: silver hair, teal eyes and bow, inspired by the supplied palette. */
public class MascotView extends View {
    private final Paint p=new Paint(3);
    public MascotView(Context c){super(c);setContentDescription("银发青瞳的青铃小精灵");}
    private void oval(Canvas c,int color,float l,float t,float r,float b){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawOval(l,t,r,b,p);}
    private void path(Canvas c,int color,float... xy){Path q=new Path();q.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)q.lineTo(xy[i],xy[i+1]);q.close();p.setColor(color);c.drawPath(q,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=Math.min(getWidth()/200f,getHeight()/200f);c.translate((getWidth()-200*scale)/2,(getHeight()-200*scale)/2);c.scale(scale,scale);
        oval(c,0xFFD5E8DD,8,12,192,196);
        oval(c,0xFFCDD1D8,38,26,165,192);
        path(c,0xFFE3E5EA,48,65,23,170,62,154,72,79);
        path(c,0xFFE3E5EA,151,65,179,170,141,154,131,79);
        oval(c,0xFFF1F2F4,41,26,162,153);
        oval(c,0xFFFFEADC,53,52,150,143);
        path(c,0xFFF1F2F4,49,95,49,51,113,30,151,54,153,100,128,88,109,62,103,106,80,86,63,102);
        oval(c,0xFF183E39,67,101,84,117);oval(c,0xFF183E39,119,101,136,117);
        oval(c,0xFF69B5A5,71,108,82,116);oval(c,0xFF69B5A5,123,108,134,116);
        oval(c,Color.WHITE,69,101,74,106);oval(c,Color.WHITE,121,101,126,106);
        oval(c,0xFFFFCDBD,58,119,76,126);oval(c,0xFFFFCDBD,129,119,147,126);
        p.setColor(Ui.GREEN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);c.drawArc(94,120,110,131,0,180,false,p);p.setStyle(Paint.Style.FILL);
        path(c,0xFFFFFFFF,81,144,122,144,144,184,59,184);
        path(c,Ui.GREEN,100,159,77,149,77,172,100,164,125,174,125,148);
        oval(c,0xFF70B6A4,94,154,107,168);
        p.setColor(0xFFF1F2F4);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9);c.drawArc(79,12,116,44,180,190,false,p);p.setStyle(Paint.Style.FILL);
        oval(c,Ui.GREEN,139,61,157,79);p.setColor(Color.WHITE);p.setStrokeWidth(2);c.drawLine(148,65,148,75,p);c.drawLine(143,70,153,70,p);
        c.restore();
    }
}
