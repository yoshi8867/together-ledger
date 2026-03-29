package com.yoshi0311.togetherledger

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yoshi0311.togetherledger.data.AppContainer
import com.yoshi0311.togetherledger.data.AppDataContainer
import com.yoshi0311.togetherledger.worker.KeepAliveWorker
import java.util.concurrent.TimeUnit

class LedgerApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        scheduleKeepAlive()
    }

    private fun scheduleKeepAlive() {
        val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "keep_alive",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}