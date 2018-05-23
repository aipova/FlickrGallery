package ru.aipova.photogallery.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.AsyncTask
import android.os.Build
import android.support.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScheduledPollService : JobService() {
    private var currentTask: PollTask? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        currentTask = PollTask()
        currentTask?.execute(params)
        return true

    }

    override fun onStopJob(params: JobParameters?): Boolean {
        currentTask?.cancel(true)
        return true
    }

    inner class PollTask: AsyncTask<JobParameters, Void, Void>() {
        override fun doInBackground(vararg params: JobParameters?): Void? {
            PhotoChecker.checkAndNotifyAboutNewPhotos(this@ScheduledPollService)
            jobFinished(params[0], false)
            return null
        }

    }
}