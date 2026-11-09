package com.viniciusrio.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.viniciusrio.finance.ui.FinanceRoot
import com.viniciusrio.finance.ui.MainViewModel
import com.viniciusrio.finance.ui.theme.FinanceTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as FinanceApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FinanceTheme { FinanceRoot(vm) } }
    }
}
