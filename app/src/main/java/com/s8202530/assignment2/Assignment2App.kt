package com.s8202530.assignment2

import android.app.Application
import com.s8202530.assignment2.data.shizuku.ShizukuController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * @HiltAndroidApp starts Hilt: it generates the app-wide dependency container
 * that every Activity and ViewModel gets its objects from. It also starts
 * listening for Shizuku so the permission switches know when they can act.
 */
@HiltAndroidApp
class Assignment2App : Application() {

    @Inject lateinit var shizukuController: ShizukuController

    override fun onCreate() {
        super.onCreate()
        shizukuController.init()
    }
}
