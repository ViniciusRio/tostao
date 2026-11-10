package com.viniciusrio.tostao

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.screens.TransactionFormScreen
import com.viniciusrio.tostao.ui.theme.FinanceTheme
import kotlinx.coroutines.launch

/**
 * Formulário de nova transação aberto pelo widget ou pelos atalhos do ícone. Roda numa tarefa
 * própria (taskAffinity vazia, fora dos recentes): ao salvar ou voltar, fecha e o usuário volta
 * para onde estava, sem tocar na pilha do app principal.
 */
class QuickAddActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as FinanceApp).container) }

    /** Muda a cada novo pedido (widget tocado de novo) para o formulário recomeçar vazio. */
    private var launchKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Rota só na primeira criação do VM; na rotação ele sobrevive com o formulário.
        if (vm.backStack.lastOrNull() !is Route.TxForm) start(intent)
        vm.onRootBack = {
            lifecycleScope.launch { if (vm.awaitWrites()) finish() }
        }
        setContent {
            FinanceTheme {
                val data by vm.data.collectAsState()
                val message by vm.message.collectAsState()
                var redirected by rememberSaveable { mutableIntStateOf(0) }

                LaunchedEffect(message?.id) {
                    message?.let { m ->
                        Toast.makeText(applicationContext, m.text, Toast.LENGTH_SHORT).show()
                        vm.consumeMessage(m.id)
                    }
                }
                BackHandler { vm.back() }

                Surface(Modifier.fillMaxSize()) {
                    val d = data
                    val route = vm.backStack.lastOrNull() as? Route.TxForm
                    when {
                        d == null || route == null -> Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                        d.accounts.isEmpty() -> LaunchedEffect(Unit) {
                            if (redirected++ == 0) openAccountForm()
                        }
                        else -> Box(Modifier.safeDrawingPadding()) {
                            key(launchKey) { TransactionFormScreen(vm, d, route.type, null) }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        start(intent)
        launchKey++
    }

    private fun start(intent: Intent?) {
        val type = intent?.getStringExtra(EXTRA_TYPE)?.let { t -> TxType.entries.firstOrNull { it.name == t } } ?: TxType.EXPENSE
        vm.backStack.clear()
        vm.backStack.add(Route.TxForm(type))
    }

    private fun openAccountForm() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_ACCOUNT_FORM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        const val ACTION_QUICK_ADD = "com.viniciusrio.tostao.action.QUICK_ADD"
        const val EXTRA_TYPE = "type"
    }
}
