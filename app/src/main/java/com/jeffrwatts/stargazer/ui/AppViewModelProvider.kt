package com.jeffrwatts.stargazer.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jeffrwatts.stargazer.StarGazerApplication
import com.jeffrwatts.stargazer.ui.sights.SightsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {

        initializer {
            SightsViewModel(
                this.createSavedStateHandle(),
                starGazerApplication().container.celestialObjRepository
            )
        }
    }
}

fun CreationExtras.starGazerApplication(): StarGazerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as StarGazerApplication)
