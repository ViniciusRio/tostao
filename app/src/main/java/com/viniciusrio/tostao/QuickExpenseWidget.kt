package com.viniciusrio.tostao

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget 1x1 "Nova despesa": um botão estático, sem estado nem atualização periódica.
 * RemoteViews basta; Glance traria dependências sem ganho (ver design do change).
 */
class QuickExpenseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val intent = Intent(context, QuickAddActivity::class.java)
            .setAction(QuickAddActivity.ACTION_QUICK_ADD)
            .putExtra(QuickAddActivity.EXTRA_TYPE, "EXPENSE")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context, REQUEST_EXPENSE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val views = RemoteViews(context.packageName, R.layout.widget_quick_expense).apply {
            setOnClickPendingIntent(R.id.widget_root, pending)
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        private const val REQUEST_EXPENSE = 1001
    }
}
