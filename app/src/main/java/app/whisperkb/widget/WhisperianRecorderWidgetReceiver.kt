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
        views.setTextViewText(R.id.widget_action_start, "Start")
        views.setTextViewText(R.id.widget_action_stop, "Stop")
        views.setTextViewText(R.id.widget_action_retry, "Retry")
        views.setTextViewText(R.id.widget_action_cancel, "Cancel")
        views.setTextViewText(R.id.widget_action_copy, "Copy")
        views.setOnClickPendingIntent(R.id.widget_root, actionPendingIntent(context, RecorderWidgetActionReceiver.ACTION_START))
        views.setOnClickPendingIntent(R.id.widget_action_start, actionPendingIntent(context, RecorderWidgetActionReceiver.ACTION_START))
        views.setOnClickPendingIntent(R.id.widget_action_stop, actionPendingIntent(context, RecorderWidgetActionReceiver.ACTION_STOP))
        views.setOnClickPendingIntent(R.id.widget_action_retry, actionPendingIntent(context, RecorderWidgetActionReceiver.ACTION_RETRY))
        views.setOnClickPendingIntent(R.id.widget_action_cancel, actionPendingIntent(context, RecorderWidgetActionReceiver.ACTION_CANCEL))
        views.setOnClickPendingIntent(R.id.widget_action_copy, actionPendingIntent(context, RecorderWidgetActionReceiver.ACTION_COPY))
        return views
    }

    private fun labelFor(state: ServiceState, error: String?): String = when (state) {
        ServiceState.RECORDING -> "Recording"
        ServiceState.TRANSCRIBING -> "Transcribing"
        ServiceState.ERROR -> error ?: "Error"
        ServiceState.COMPLETED -> "Ready"
        ServiceState.IDLE -> "Idle"
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
