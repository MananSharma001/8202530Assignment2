package com.s8202530.assignment2.di

import javax.inject.Qualifier

/** The class location used in the login URL: "footscray", "sydney" or "br". */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CampusLocation

/** The background thread pool for network calls. Swapped for a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
