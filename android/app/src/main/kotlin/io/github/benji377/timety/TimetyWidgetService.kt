package io.github.benji377.timety

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import es.antonborri.home_widget.HomeWidgetPlugin
import java.io.File

class TimetyWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetType = intent.getStringExtra("widget_type") ?: "task"
        return TimetyRemoteViewsFactory(this.applicationContext, widgetType)
    }
}

class TimetyRemoteViewsFactory(
    private val context: Context,
    private val widgetType: String
) : RemoteViewsService.RemoteViewsFactory {

    private var imagePaths = mutableListOf<String>()
    private val prefs: SharedPreferences by lazy {
        HomeWidgetPlugin.getData(context)
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        imagePaths.clear()
        val count = prefs.getInt("${widgetType}_item_count", 0)
        for (i in 0 until count) {
            val path = prefs.getString("${widgetType}_item_$i", null)
            if (path != null) {
                imagePaths.add(path)
            }
        }
    }

    override fun onDestroy() {
        imagePaths.clear()
    }

    override fun getCount(): Int = imagePaths.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        if (position < imagePaths.size) {
            val file = File(imagePaths[position])
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                views.setImageViewBitmap(R.id.item_image, bitmap)
            }
        }
        
        // Add fill-in intent to open app normally
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.item_image, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
