package com.lorenzo.monefyme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.lorenzo.monefyme.ui.BudgetApp
import com.lorenzo.monefyme.ui.theme.MonefyMeTheme
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import com.lorenzo.monefyme.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val app = application as MonefyMeApp
        MainViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MonefyMeTheme {
                BudgetApp(viewModel = viewModel)
            }
        }
    }
}
