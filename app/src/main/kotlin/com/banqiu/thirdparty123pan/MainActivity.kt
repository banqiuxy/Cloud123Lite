package com.banqiu.thirdparty123pan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.data.prefs.ThemeMode
import com.banqiu.thirdparty123pan.ui.navigation.AppNavHost
import com.banqiu.thirdparty123pan.ui.theme.Cloud123Theme
import com.banqiu.thirdparty123pan.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            com.banqiu.thirdparty123pan.ui.activities.Cloud123ActivityContent { AppNavHost() }
        }
    }
}
