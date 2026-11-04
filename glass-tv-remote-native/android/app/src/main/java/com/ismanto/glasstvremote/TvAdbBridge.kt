package com.ismanto.glasstvremote

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.flyfishxu.kadb.Kadb
import org.json.JSONObject
import java.util.concurrent.Executors

class TvAdbBridge(private val webView: WebView) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var client: Any? = null

    @JavascriptInterface
    fun connect(host: String, port: Int, callbackId: String) {
        executor.execute {
            try {
                require(host.isNotBlank() && port in 1..65535) { "IP atau port tidak valid" }
                closeClient()
                val adb = Kadb.create(host.trim(), port)
                val response = adb.shell("getprop ro.product.model && getprop ro.build.version.release")
                if (response.exitCode != 0) {
                    try { (adb as? AutoCloseable)?.close() } catch (_: Exception) {}
                    error(response.output.ifBlank { "ADB shell gagal" })
                }
                client = adb
                val lines = response.output.lines().filter { it.isNotBlank() }
                deliver(callbackId, JSONObject()
                    .put("ok", true)
                    .put("host", host)
                    .put("port", port)
                    .put("model", lines.getOrNull(0) ?: "Android TV")
                    .put("release", lines.getOrNull(1) ?: ""))
            } catch (e: Exception) {
                deliverError(callbackId, "Koneksi ADB gagal: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun pair(host: String, port: Int, code: String, callbackId: String) {
        executor.execute {
            try {
                require(host.isNotBlank() && port in 1..65535 && code.matches(Regex("\\d{6}"))) {
                    "Host, port pairing, atau kode pairing tidak valid"
                }
                Kadb.pair(host.trim(), port, code)
                deliver(callbackId, JSONObject().put("ok", true).put("host", host).put("paired", true))
            } catch (e: Exception) {
                deliverError(callbackId, "Pairing ADB gagal: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun shell(command: String, callbackId: String) {
        executor.execute {
            try {
                require(command.isNotBlank() && command.length <= 500) { "Perintah kosong atau terlalu panjang" }
                val adb = client ?: error("Belum terhubung ke TV")
                val response = adb.javaClass.getMethod("shell", String::class.java).invoke(adb, command)
                val output = response.javaClass.getMethod("getOutput").invoke(response)?.toString() ?: ""
                val exitCode = (response.javaClass.getMethod("getExitCode").invoke(response) as Number).toInt()
                deliver(callbackId, JSONObject().put("output", output).put("exitCode", exitCode))
            } catch (e: Exception) {
                deliverError(callbackId, "ADB shell gagal: ${e.cause?.message ?: e.message}")
            }
        }
    }

    @JavascriptInterface
    fun disconnect(callbackId: String) {
        executor.execute {
            closeClient()
            deliver(callbackId, JSONObject().put("ok", true))
        }
    }

    private fun closeClient() {
        try { (client as? AutoCloseable)?.close() } catch (_: Exception) {}
        client = null
    }

    private fun deliverError(id: String, message: String) {
        deliver(id, JSONObject().put("error", message))
    }

    private fun deliver(id: String, result: JSONObject) {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,80}"))) return
        val json = JSONObject.quote(result.toString())
        webView.post { webView.evaluateJavascript("window.__tvAdbCallback && window.__tvAdbCallback('$id', $json);", null) }
    }
}
