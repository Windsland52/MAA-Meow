package com.aliothmoon.maameow.schedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.aliothmoon.maameow.schedule.service.ScheduleAlarmManager
import timber.log.Timber

/** 接收定时触发并启动 ScheduleExecutionService。 */
class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        private const val EXECUTION_SERVICE_CLASS =
            "com.aliothmoon.maameow.schedule.service.ScheduleExecutionService"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val strategyId = intent.getStringExtra(ScheduleAlarmManager.EXTRA_STRATEGY_ID) ?: return
        val scheduledTime = intent.getLongExtra(ScheduleAlarmManager.EXTRA_SCHEDULED_TIME, 0L)

        if (intent.action == ScheduleAlarmManager.ACTION_SCHEDULE_TRIGGER) {
            Timber.i("Schedule alarm triggered for strategy: %s", strategyId)
            val serviceIntent = Intent().apply {
                setClassName(context, EXECUTION_SERVICE_CLASS)
                action = ScheduleAlarmManager.ACTION_SCHEDULE_TRIGGER
                putExtra(ScheduleAlarmManager.EXTRA_STRATEGY_ID, strategyId)
                putExtra(ScheduleAlarmManager.EXTRA_SCHEDULED_TIME, scheduledTime)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
