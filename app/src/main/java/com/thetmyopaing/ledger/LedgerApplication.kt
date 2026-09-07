package com.thetmyopaing.ledger

import android.app.Application
import com.thetmyopaing.ledger.data.LedgerDatabase
import com.thetmyopaing.ledger.data.LedgerRepository

class LedgerApplication : Application() {
    val repository by lazy { LedgerRepository(LedgerDatabase.get(this)) }
}