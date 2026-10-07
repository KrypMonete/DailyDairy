package com.dailydairy

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.dailydairy.theme.ThemeStore
import com.dailydairy.theme.launchBackground

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val saved = ThemeStore(this).read()
        val background = launchBackground(this, saved.first, saved.second)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(background))
        window.statusBarColor = background
        window.navigationBarColor = background
        super.onCreate(savedInstanceState)
        applyTaskIcon()
        enableEdgeToEdge()
        setContent {
            DailyDairyApp()
        }
    }

    override fun onResume() {
        super.onResume()
        applyTaskIcon()
    }

    private fun applyTaskIcon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setTaskDescription(
                ActivityManager.TaskDescription.Builder()
                    .setLabel(getString(R.string.app_name))
                    .setIcon(R.drawable.icon)
                    .build(),
            )
            return
        }
        val icon = decodeAppIcon() ?: return
        @Suppress("DEPRECATION")
        setTaskDescription(
            ActivityManager.TaskDescription(getString(R.string.app_name), icon),
        )
    }

    private fun decodeAppIcon(): Bitmap? =
        BitmapFactory.decodeResource(resources, R.drawable.icon)
}
