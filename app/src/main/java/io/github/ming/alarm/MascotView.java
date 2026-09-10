package io.github.ming.alarm;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Artwork from the user-supplied Whom001x/- repository; see docs/ARTWORK.md. */
public class MascotView extends View {
    private static Bitmap art;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF();
    public MascotView(Context c){super(c);setContentDescription("青铃少女");if(art==null)art=BitmapFactory.decodeResource(c.getResources(),R.drawable.mascot);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(art==null)return;
        float scale=Math.min((float)getWidth()/art.getWidth(),(float)getHeight()/art.getHeight());
        float w=art.getWidth()*scale,h=art.getHeight()*scale;
        destination.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
        canvas.drawBitmap(art,null,destination,paint);
    }
}
