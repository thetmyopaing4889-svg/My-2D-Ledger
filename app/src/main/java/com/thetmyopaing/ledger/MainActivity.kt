package com.thetmyopaing.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.thetmyopaing.ledger.ui.LedgerApp
import com.thetmyopaing.ledger.ui.LedgerViewModel
import com.thetmyopaing.ledger.ui.theme.LedgerTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LedgerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                LedgerApp(viewModel)
            }
        }
    }
}