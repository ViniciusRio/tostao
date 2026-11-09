package com.viniciusrio.finance

import android.app.Application
import android.content.Context
import com.viniciusrio.finance.data.FinanceDatabase
import com.viniciusrio.finance.data.FinanceRepository
import java.time.Clock

class FinanceApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    val repository = FinanceRepository(FinanceDatabase.create(context))
    val clock: Clock = Clock.systemDefaultZone()
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
}
