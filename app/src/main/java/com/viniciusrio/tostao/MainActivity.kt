package com.viniciusrio.tostao

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.viniciusrio.tostao.ui.FinanceRoot
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.theme.FinanceTheme
import com.viniciusrio.tostao.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as FinanceApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by vm.themeMode.collectAsState()
            val dark = mode.isDark()
            // Ícones da barra de status/navegação seguem o tema do app, não só o do sistema.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            FinanceTheme(dark) { FinanceRoot(vm) }
        }
    }

    private companion object {
        // Mesmos scrims padrão do enableEdgeToEdge() para a barra de navegação de 3 botões.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
