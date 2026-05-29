package io.github.benji377.timety

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetLaunchIntent
import es.antonborri.home_widget.HomeWidgetProvider
import java.io.File

class HabitWidgetProvider : HomeWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        widgetData: SharedPreferences
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.habit_widget).apply {
                // Header Image
                val headerPath = widgetData.getString("habit_widget_header", null)
                if (headerPath != null) {
                    val file = File(headerPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        setImageViewBitmap(R.id.widget_header, bitmap)
                    }
                }
                
                // Set up the ListView
                val intent = Intent(context, TimetyWidgetService::class.java).apply {
                    putExtra("widget_type", "habit")
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                setRemoteAdapter(R.id.widget_list, intent)
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                
                // Set pending intent template for list item clicks
                val pendingIntent = HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java
                )
                setPendingIntentTemplate(R.id.widget_list, pendingIntent)
                
                // Also allow clicking header to open app
                setOnClickPendingIntent(R.id.widget_header, pendingIntent)
                setOnClickPendingIntent(R.id.widget_empty, pendingIntent)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }
}
