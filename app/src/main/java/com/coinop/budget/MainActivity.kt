package com.coinop.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coinop.budget.ui.MainViewModel
import com.coinop.budget.ui.screens.CoinopRoot
import com.coinop.budget.ui.screens.ConfigMissingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) {
                ConfigMissingScreen()
            } else {
                val vm: MainViewModel = viewModel()
                CoinopRoot(vm)
            }
        }
    }
}
