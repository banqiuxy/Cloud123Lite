package com.banqiu.thirdparty123pan.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.banqiu.thirdparty123pan.ui.screens.profile.ShareListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cloud123ActivityContent {
                ShareListScreen(onBack = ::finish)
            }
        }
    }
}