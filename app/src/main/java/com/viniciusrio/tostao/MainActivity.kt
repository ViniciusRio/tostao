package com.viniciusrio.tostao

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.viniciusrio.tostao.ui.FinanceRoot
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.theme.FinanceTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as FinanceApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleOpen(intent)
        setContent { FinanceTheme { FinanceRoot(vm) } }
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
    }
}
