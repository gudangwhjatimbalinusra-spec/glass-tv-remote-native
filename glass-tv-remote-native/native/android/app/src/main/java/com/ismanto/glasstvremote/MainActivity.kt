package com.ismanto.glasstvremote

import android.os.Bundle
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPlugin(TvAdbPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
