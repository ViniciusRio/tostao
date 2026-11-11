package com.viniciusrio.tostao

import android.content.Intent
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
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.theme.FinanceTheme
import com.viniciusrio.tostao.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as FinanceApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleOpen(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpen(intent)
    }

    /** Pedido externo (atalho sem conta) para abrir uma tela específica. */
    private fun handleOpen(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_ACCOUNT_FORM) {
            intent.removeExtra(EXTRA_OPEN)
            vm.say("Crie uma conta antes de registrar transações")
            if (vm.backStack.lastOrNull() != Route.AccountForm()) vm.navigate(Route.AccountForm())
        }
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_ACCOUNT_FORM = "account_form"

        // Mesmos scrims padrão do enableEdgeToEdge() para a barra de navegação de 3 botões.
        internal val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        internal val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
