package com.jeffrwatts.stargazer.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jeffrwatts.stargazer.StarGazerApplication
import com.jeffrwatts.stargazer.ui.info.InfoViewModel
import com.jeffrwatts.stargazer.ui.polar.PolarAlignViewModel
import com.jeffrwatts.stargazer.ui.sightdetail.SightDetailViewModel
import com.jeffrwatts.stargazer.ui.sights.SightsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            InfoViewModel(
                starGazerApplication().container.locationRepository
            )
        }
        initializer {
            PolarAlignViewModel(
                starGazerApplication().container.celestialObjRepository,
                starGazerApplication().container.locationRepository
            )
        }
        initializer {
            SightsViewModel(
                starGazerApplication().container.celestialObjRepository,
                starGazerApplication().container.locationRepository,
                starGazerApplication().container.planetPosRepository
            )
        }
        initializer {
            SightDetailViewModel(
                starGazerApplication().container.celestialObjRepository
            )
        }
    }
}

fun CreationExtras.starGazerApplication(): StarGazerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as StarGazerApplication)
