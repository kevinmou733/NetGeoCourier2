package com.example.netgeocourier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.PermissionHelper
import com.example.netgeocourier.screen.EvaluationScreen
import com.example.netgeocourier.screen.NetTestScreen
import com.example.netgeocourier.ui.theme.NetGeoCourierTheme
import com.example.netgeocourier.viewmodel.NetTestViewModel
import com.google.android.gms.location.LocationServices

private enum class AppPage {
    TEST,
    EVALUATION
}

class MainActivity : ComponentActivity() {

    lateinit var viewmodel: NetTestViewModel

    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this)[NetTestViewModel::class.java]

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationHelper = LocationHelper(fusedLocationClient)

        setContent {
            var currentPage by rememberSaveable { mutableStateOf(AppPage.TEST) }

            NetGeoCourierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentPage) {
                        AppPage.TEST -> NetTestScreen(
                            locationHelper = locationHelper,
                            onOpenEvaluation = { currentPage = AppPage.EVALUATION }
                        )

                        AppPage.EVALUATION -> EvaluationScreen(
                            onBack = { currentPage = AppPage.TEST }
                        )
                    }
                }
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        PermissionHelper.registerPermissionLauncher(this) {
            // Permissions granted.
        }
    }
}
