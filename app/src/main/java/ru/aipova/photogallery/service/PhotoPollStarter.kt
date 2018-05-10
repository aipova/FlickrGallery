package ru.aipova.photogallery.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobInfo.NETWORK_TYPE_UNMETERED
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.os.Build
import android.os.SystemClock
import android.support.annotation.RequiresApi
import android.util.Log
import java.util.concurrent.TimeUnit

class PhotoPollStarter private constructor() {

    companion object {
        private const val TAG = "PhotoPollStarter"
        private const val JOB_ID = 1
        private val POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1)

        fun isPollingOn(context: Context?) =
            if (jobSchedulerAvailable()) {
                getJobScheduler(context).allPendingJobs.any { it.id == JOB_ID }
            } else {
                val pendingIntent = PendingIntent.getService(
                    context, 0,
                    IntentPollService.newIntent(context),
                    PendingIntent.FLAG_NO_CREATE
                )
                pendingIntent != null
            }

        private fun jobSchedulerAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun getJobScheduler(context: Context?) =
            context?.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

        fun startPolling(context: Context?) {
            if (jobSchedulerAvailable()) {
                schedulePollingJob(context)
            } else {
                setupAlarmManager(context)
            }
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun schedulePollingJob(context: Context?) {
            getJobScheduler(context).schedule(buildPollJobInfo(context))
            Log.i(TAG, "JobScheduler for polling was scheduled")
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun buildPollJobInfo(context: Context?): JobInfo? {
            return JobInfo.Builder(JOB_ID, ComponentName(context, ScheduledPollService::class.java))
                .setRequiredNetworkType(NETWORK_TYPE_UNMETERED)
                .setPeriodic(POLL_INTERVAL_MS)
                .setPersisted(true)
                .build()
        }

        private fun setupAlarmManager(context: Context?) {
            getAlarmManager(context).setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime(),
                POLL_INTERVAL_MS, getPendingIntent(context)
            )
            Log.i(TAG, "AlarmManager for polling service started")
        }

        private fun getAlarmManager(context: Context?) =
            context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        private fun getPendingIntent(context: Context?): PendingIntent? {
            val intent = IntentPollService.newIntent(context)
            return PendingIntent.getService(context, 0, intent, 0)
        }

        fun stopPolling(context: Context?) {
            if (jobSchedulerAvailable()) {
                cancelScheduledJob(context)
            } else {
                cancelAlarmSetup(context)
            }
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun cancelScheduledJob(context: Context?) {
            getJobScheduler(context).cancel(JOB_ID)
        }

        private fun cancelAlarmSetup(context: Context?) {
            val pendingIntent = getPendingIntent(context)
            getAlarmManager(context).cancel(pendingIntent)
            pendingIntent?.cancel()
        }
    }
}