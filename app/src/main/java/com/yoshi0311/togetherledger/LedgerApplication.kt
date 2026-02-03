package com.yoshi0311.togetherledger

import android.app.Application
import com.yoshi0311.togetherledger.data.AppContainer
import com.yoshi0311.togetherledger.data.AppDataContainer

class LedgerApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}