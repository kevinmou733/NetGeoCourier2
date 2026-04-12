package com.example.netgeocourier

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.netgeocourier.helper.AuthTokenStore
import androidx.lifecycle.ViewModelProvider
import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.PermissionHelper
import com.example.netgeocourier.screen.AuthScreen
import com.example.netgeocourier.screen.EvaluationScreen
import com.example.netgeocourier.screen.HistoryScreen
import com.example.netgeocourier.screen.NetTestScreen
import com.example.netgeocourier.ui.theme.NetGeoCourierTheme
import com.example.netgeocourier.viewmodel.NetTestViewModel
import com.example.netgeocourier.viewmodel.AppPage


class MainActivity : ComponentActivity() {

    lateinit var viewmodel: NetTestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this)[NetTestViewModel::class.java]

        setContent {
            NetGeoCourierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (viewmodel.currentPage) {
                        AppPage.AUTH -> AuthScreen(
                            onBack = { viewmodel.currentPage = AppPage.TEST },
                            onAuthSuccess = {
                                viewmodel.syncAllLocalRecordsInBackground()
                                viewmodel.currentPage = AppPage.TEST
                            }
                        )
                        AppPage.TEST -> NetTestScreen(
                            viewModel = viewmodel,
                            onOpenEvaluation = { viewmodel.currentPage = AppPage.EVALUATION },
                            onOpenAuth = { viewmodel.currentPage = AppPage.AUTH },
                            onLogout = {
                                AuthTokenStore.clearAccessToken(this)
                                viewmodel.currentPage = AppPage.TEST
                            }
                        )
                        AppPage.EVALUATION -> EvaluationScreen(
                            viewModel = viewmodel,
                            onBack = { viewmodel.currentPage = AppPage.TEST },
                            onOpenAuth = { viewmodel.currentPage = AppPage.AUTH },
                            onLogout = {
                                AuthTokenStore.clearAccessToken(this)
                                viewmodel.currentPage = AppPage.TEST
                            }
                        )
                    }
                }
            }
        }
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        // 如果已经有权限，直接返回
        if (PermissionHelper.hasLocationPermission(this)) {
            return
        }

        // 没有权限，请求权限
        PermissionHelper.registerPermissionLauncher(this) { allGranted ->
            if (allGranted) {
                Toast.makeText(this, getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.location_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
