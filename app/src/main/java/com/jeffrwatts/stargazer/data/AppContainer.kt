package com.jeffrwatts.stargazer.data

import android.content.Context

interface AppContainer {
}

class AppContainerImpl (private val context: Context) : AppContainer {
}