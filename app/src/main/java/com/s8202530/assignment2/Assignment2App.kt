package com.s8202530.assignment2

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * @HiltAndroidApp starts Hilt: it generates the app-wide dependency container
 * that every Activity and ViewModel gets its objects from.
 */
@HiltAndroidApp
class Assignment2App : Application()
