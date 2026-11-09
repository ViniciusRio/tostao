package com.viniciusrio.tostao

import android.app.Application
import android.content.Context
import com.viniciusrio.tostao.data.DataTransferService
import com.viniciusrio.tostao.data.FinanceDatabase
import com.viniciusrio.tostao.data.FinanceRepository
import java.time.Clock

class FinanceApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    val repository = FinanceRepository(FinanceDatabase.create(context))
    val clock: Clock = Clock.systemDefaultZone()
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val transfer = DataTransferService(context.contentResolver, repository, clock, prefs)
}
