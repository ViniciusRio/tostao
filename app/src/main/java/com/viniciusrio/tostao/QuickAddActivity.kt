package com.viniciusrio.tostao

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.screens.TransactionFormScreen
import com.viniciusrio.tostao.ui.theme.FinanceTheme
import com.viniciusrio.tostao.ui.theme.isDark

/**
 * Formulário de nova transação aberto pelo widget ou pelos atalhos do ícone. Roda numa tarefa
 * própria (taskAffinity vazia, fora dos recentes): ao salvar ou voltar, fecha e o usuário volta
 * para onde estava, sem tocar na pilha do app principal.
 */
class QuickAddActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as FinanceApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Rota só na primeira criação do VM; na rotação ele sobrevive com o formulário.
        if (vm.backStack.lastOrNull() !is Route.TxForm) vm.startQuick(typeOf(intent), newRequest = false)
        setContent {
            val mode by vm.themeMode.collectAsState()
            val dark = mode.isDark()
            // Mesmo tema e barras da MainActivity: segue a preferência do app, não só a do sistema.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(MainActivity.LIGHT_SCRIM, MainActivity.DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            FinanceTheme(dark) {
                val data by vm.data.collectAsState()
                val message by vm.message.collectAsState()
                val close by vm.quickClose.collectAsState()
                var redirected by rememberSaveable { mutableIntStateOf(0) }
                // Última mensagem já mostrada, para a confirmação não aparecer duas vezes.
                var toasted by rememberSaveable { mutableLongStateOf(-1L) }

                fun toast(m: com.viniciusrio.tostao.ui.UiMessage) {
                    if (m.id == toasted) return
                    // Mensagem de uma gravação de outro pedido (ex.: o atalho tocado de novo no meio dela).
                    if (m.request != vm.quickRequest) { vm.consumeMessage(m.id); return }
                    toasted = m.id
                    Toast.makeText(applicationContext, m.text, Toast.LENGTH_SHORT).show()
                    vm.consumeMessage(m.id)
                }

                LaunchedEffect(message?.id) { message?.let(::toast) }
                // Fecha uma vez, quando o VM confirma que a gravação do pedido atual terminou bem.
                LaunchedEffect(close) {
                    val c = close ?: return@LaunchedEffect
                    if (c.request != vm.quickRequest) return@LaunchedEffect
                    c.message?.let(::toast)
                    finish()
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
                            key(vm.quickRequest) { TransactionFormScreen(vm, d, route.type, null) }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.startQuick(typeOf(intent), newRequest = true)
    }

    private fun typeOf(intent: Intent?): TxType =
        intent?.getStringExtra(EXTRA_TYPE)?.let { t -> TxType.entries.firstOrNull { it.name == t } } ?: TxType.EXPENSE

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
