package app.whisperkb.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.whisperkb.AppStateStore
import app.whisperkb.R
import app.whisperkb.ServiceState

class WhisperkbRecorderWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppStateStore.ACTION_STATE_CHANGED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WhisperkbRecorderWidgetReceiver::class.java))
            ids.forEach { appWidgetId -> manager.updateAppWidget(appWidgetId, buildViews(context)) }
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        val snapshot = AppStateStore.snapshot(context)
        val views = RemoteViews(context.packageName, R.layout.whisperkb_recorder_widget)
        views.setTextViewText(R.id.widget_status, labelFor(snapshot.state, snapshot.error))
        views.setOnClickPendingIntent(R.id.widget_root, actionPendingIntent(context, when (snapshot.state) {
            ServiceState.RECORDING, ServiceState.TRANSCRIBING -> RecorderWidgetActionReceiver.ACTION_STOP
            ServiceState.ERROR -> RecorderWidgetActionReceiver.ACTION_RETRY
            else -> RecorderWidgetActionReceiver.ACTION_START
        }))
        return views
    }

    private fun labelFor(state: ServiceState, error: String?): String = when (state) {
        ServiceState.RECORDING -> "Stop recording"
        ServiceState.TRANSCRIBING -> "Transcribing"
        ServiceState.ERROR -> error ?: "Retry"
        else -> "Start recording"
    }

    private fun actionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, RecorderWidgetActionReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
