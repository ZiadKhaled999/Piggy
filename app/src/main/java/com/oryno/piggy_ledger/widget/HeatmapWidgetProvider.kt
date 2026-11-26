package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HeatmapWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        HeatmapWidgetWorker.schedulePeriodic(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        HeatmapWidgetWorker.cancel(context.applicationContext)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderFor(context.applicationContext, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "onUpdate failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderFor(context.applicationContext, appWidgetManager, intArrayOf(appWidgetId))
            } catch (e: Exception) {
                android.util.Log.e(TAG, "onAppWidgetOptionsChanged failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "HeatmapWidgetProvider"
        private const val DEFAULT_WIDTH_DP = 320f
        private const val DEFAULT_HEIGHT_DP = 140f
        private const val HORIZONTAL_PADDING_DP = 32f
        private const val VERTICAL_PADDING_DP = 20f

        suspend fun updateAllWidgetsNow(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, HeatmapWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids != null && ids.isNotEmpty()) {
                renderFor(context.applicationContext, appWidgetManager, ids)
            }
        }

        private suspend fun renderFor(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            val days = HeatmapDataProvider.dailyActivityCounts(context, 365)
            val localizedContext = WidgetUtils.getLocalizedContext(context)
            val density = context.resources.displayMetrics.density
            val isLandscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val contentDesc = localizedContext.getString(R.string.widget_heatmap_content_desc)

            for (id in ids) {
                val opts = manager.getAppWidgetOptions(id)

                val sizes: List<SizeF>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    opts.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                        ?.takeIf { it.isNotEmpty() }
                } else {
                    null
                }

                val views = if (sizes != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val perBitmapMaxBytes = (700_000L / sizes.size.coerceAtLeast(1))
                        .coerceIn(120_000L, 350_000L)
                    RemoteViews(
                        sizes.associateWith { size ->
                            buildRemoteViews(
                                context = context,
                                widgetId = id,
                                days = days,
                                density = density,
                                widgetWidthDp = size.width,
                                widgetHeightDp = size.height,
                                contentDesc = contentDesc,
                                maxBitmapBytes = perBitmapMaxBytes
                            )
                        }
                    )
                } else {
                    val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                    val maxW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
                    val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                    val maxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

                    val widgetWidthDp = when {
                        isLandscape && maxW > 0 -> maxW.toFloat()
                        !isLandscape && minW > 0 -> minW.toFloat()
                        maxW > 0 -> maxW.toFloat()
                        minW > 0 -> minW.toFloat()
                        else -> DEFAULT_WIDTH_DP
                    }
                    val widgetHeightDp = when {
                        isLandscape && minH > 0 -> minH.toFloat()
                        !isLandscape && maxH > 0 -> maxH.toFloat()
                        maxH > 0 -> maxH.toFloat()
                        minH > 0 -> minH.toFloat()
                        else -> DEFAULT_HEIGHT_DP
                    }

                    buildRemoteViews(
                        context = context,
                        widgetId = id,
                        days = days,
                        density = density,
                        widgetWidthDp = widgetWidthDp,
                        widgetHeightDp = widgetHeightDp,
                        contentDesc = contentDesc
                    )
                }

                manager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            widgetId: Int,
            days: List<Int>,
            density: Float,
            widgetWidthDp: Float,
            widgetHeightDp: Float,
            contentDesc: String,
            maxBitmapBytes: Long = 350_000L
        ): RemoteViews {
            val contentWidthDp = (widgetWidthDp - HORIZONTAL_PADDING_DP).coerceAtLeast(32f)
            val contentHeightDp = (widgetHeightDp - VERTICAL_PADDING_DP).coerceAtLeast(36f)
            val widthPx = (contentWidthDp * density).toInt().coerceAtLeast(32)
            val heightPx = (contentHeightDp * density).toInt().coerceAtLeast(36)

            val heatmapBitmap = HeatmapRenderer.renderHeatmap(
                density = density,
                days = days,
                widthPx = widthPx,
                heightPx = heightPx,
                maxBitmapBytes = maxBitmapBytes
            )

            return RemoteViews(context.packageName, R.layout.widget_heatmap).apply {
                setImageViewBitmap(R.id.img_heatmap, heatmapBitmap)
                setContentDescription(R.id.img_heatmap, contentDesc)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    10_000 + widgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, openPendingIntent)
            }
        }

        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, HeatmapWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, HeatmapWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "triggerUpdate failed", e)
            }
        }
    }
}
