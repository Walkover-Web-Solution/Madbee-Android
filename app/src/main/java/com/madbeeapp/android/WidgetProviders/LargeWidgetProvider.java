package com.madbeeapp.android.WidgetProviders;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.madbeeapp.android.AsyncTasks.AsyncUpdateLargeWidgetTask;

public class LargeWidgetProvider extends AppWidgetProvider {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), LargeWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mContext = context;
        final int N = appWidgetIds.length;
        AsyncUpdateLargeWidgetTask task = new AsyncUpdateLargeWidgetTask(mContext, N, appWidgetIds, appWidgetManager);
        task.execute();
    }
}
