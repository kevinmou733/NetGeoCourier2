package com.example.netgeocourier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.PermissionHelper
import com.example.netgeocourier.screen.NetTestScreen
import com.example.netgeocourier.ui.theme.NetGeoCourierTheme
import com.google.android.gms.location.LocationServices

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationHelper = LocationHelper(fusedLocationClient)

        setContent {
            NetGeoCourierTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NetTestScreen(locationHelper)
                }
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        PermissionHelper.registerPermissionLauncher(this) {
            // Permission granted
        }
    }
}
