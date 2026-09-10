package io.github.ming.alarm;

import android.content.*;
import android.widget.*;
import java.util.*;

public class WidgetListService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Rows(getApplicationContext());}
    public static class Rows implements RemoteViewsFactory {
        private final Context context;
        private List<Alarm> alarms=new ArrayList<>();
        public Rows(Context c){context=c;}
        public void onCreate(){onDataSetChanged();}
        public void onDataSetChanged(){alarms=Store.all(context);}
        public void onDestroy(){alarms.clear();}
        public int getCount(){return alarms.size();}
        public RemoteViews getViewAt(int position){
            if(position<0||position>=alarms.size())return null;
            Alarm a=alarms.get(position);RemoteViews row=new RemoteViews(context.getPackageName(),R.layout.widget_row);
            row.setTextViewText(R.id.row_time,a.time());
            row.setTextViewText(R.id.row_label,a.label+" · "+a.repeatName());
            row.setTextViewText(R.id.row_state,(a.enabled?"开启":"关闭")+" · "+a.modeName());
            row.setTextColor(R.id.row_state,a.enabled?Ui.GREEN:Ui.MUTED);
            row.setOnClickFillInIntent(R.id.row_root,new Intent().putExtra("edit",a.id));
            return row;
        }
        public RemoteViews getLoadingView(){return null;}
        public int getViewTypeCount(){return 1;}
        public long getItemId(int position){return position>=0&&position<alarms.size()?alarms.get(position).id:0;}
        public boolean hasStableIds(){return true;}
    }
}
