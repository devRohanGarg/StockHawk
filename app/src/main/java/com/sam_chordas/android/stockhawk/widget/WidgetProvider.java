package com.sam_chordas.android.stockhawk.widget;

/**
 * Created by Rohan Garg on 15-05-2016.
 */


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;

public class WidgetProvider extends AppWidgetProvider {

    /*
             * this method is called every 30 mins as specified on widget_info.xml
             * this method is also called on every phone reboot
             */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        Log.d("WidgetProvider", "onUpdate");
        final int N = appWidgetIds.length;
        Log.d("N", String.valueOf(N));
        /*int[] appWidgetIds holds ids of multiple instance of your widget
         * meaning you are placing more than one widgets on your homescreen*/
        for (int i = 0; i < N; ++i) {
            RemoteViews remoteViews = updateWidgetListView(context, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);

    }

    private RemoteViews updateWidgetListView(Context context, int appWidgetId) {

        Log.d("WidgetProvider", "updateWidgetListView");
        //which layout to show on widget
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

        //RemoteViews Service needed to provide adapter for ListView
        Intent svcIntent = new Intent(context, WidgetService.class);
        //passing app widget id to that RemoteViews Service
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        //setting a unique Uri to the intent
        //don't know its purpose to me right now
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        //setting adapter to listview of the widget
        //        remoteViews.setRemoteAdapter(appWidgetId, R.id.widget_list,
        //                svcIntent);
        remoteViews.setRemoteAdapter(R.id.widget_list, svcIntent);
        //setting an empty view in case of no data
        remoteViews.setEmptyView(R.id.widget_list, R.id.empty_view);
        return remoteViews;
    }

}