package com.gmtusharx.friday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.gmtusharx.friday.data.FridayPrefs
import com.gmtusharx.friday.proactive.ProactiveScheduler
import com.gmtusharx.friday.ui.FridayScreen
import com.gmtusharx.friday.ui.theme.FridayTheme

class MainActivity : ComponentActivity() {

    private val vm: FridayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FridayTheme { FridayScreen(vm) } }
    }

    override fun onStart() {
        super.onStart()
        ProactiveScheduler.cancel(this)   // user is back; drop the pending background check-in
        vm.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        vm.onAppBackground()
        if (!isChangingConfigurations) {
            val prefs = FridayPrefs(this)
            if (prefs.proactiveEnabled) ProactiveScheduler.schedule(this, prefs.inactivityMinutes)
        }
    }
}
