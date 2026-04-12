package com.example.netgeocourier

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 根据屏幕方向自动选择背景图
            OrientationAwareSplash(
                onSplashFinished = {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

/**
 * 根据屏幕方向自动选择横屏/竖屏背景的开屏动画
 */
@Composable
fun OrientationAwareSplash(
    onSplashFinished: () -> Unit,
    splashDuration: Long = 2000
) {
    // 获取当前屏幕方向
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val orientation = configuration.orientation

    // 根据方向选择资源ID
    val backgroundRes = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> R.drawable.splash_background  // 横屏使用 drawable-land 中的图片
        Configuration.ORIENTATION_PORTRAIT -> R.drawable.splash_background   // 竖屏使用 drawable-port 中的图片
        else -> R.drawable.splash_background
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Splash Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop  // 裁剪填充，确保覆盖全屏
        )

        LaunchedEffect(Unit) {
            delay(splashDuration)
            onSplashFinished()
        }
    }
}