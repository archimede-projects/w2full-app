package com.archimede.w2full

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.archimede.w2full.ui.refueling.RefuelingRoute
import com.archimede.w2full.ui.theme.W2FullTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            W2FullTheme {
                RefuelingRoute()
            }
        }
    }
}
