package io.github.ming.alarm;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.widget.Toast;

/** The launcher owns widget instances; reject a second instance during configuration. */
public class WidgetSetupActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        setResult(RESULT_CANCELED);
        if(id==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
        for(int existing:AlarmWidget.ids(this))if(existing!=id){
            Toast.makeText(this,"闹钟表已添加到桌面，只能添加一次",Toast.LENGTH_LONG).show();finish();return;
        }
        AlarmWidget.update(this);
        setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();
    }
}
