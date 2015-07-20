package com.madbeeapp.android.WidgetProviders;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import com.madbeeapp.android.AsyncTasks.AsyncUpdateSmallWidgetTask;

public class SmallWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        final int N = appWidgetIds.length;
        AsyncUpdateSmallWidgetTask task = new AsyncUpdateSmallWidgetTask(context, N, appWidgetIds, appWidgetManager);
        task.execute();
    }
}
