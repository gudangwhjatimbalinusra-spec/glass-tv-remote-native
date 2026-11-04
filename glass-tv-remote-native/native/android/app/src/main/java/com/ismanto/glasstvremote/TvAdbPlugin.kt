package com.ismanto.glasstvremote

import com.getcapacitor.*
import dadb.Dadb
import java.io.File
import java.util.concurrent.Executors

@CapacitorPlugin(name = "TvAdb")
class TvAdbPlugin : Plugin() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var dadbClient: Dadb? = null

    private fun getOrCreateKeyPair(): dadb.AdbKeyPair {
        val privateKeyFile = File(context.filesDir, "adbkey")
        val publicKeyFile = File(context.filesDir, "adbkey.pub")
        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            dadb.AdbKeyPair.generate(privateKeyFile, publicKeyFile)
        }
        return dadb.AdbKeyPair.read(privateKeyFile, publicKeyFile)
    }

    @PluginMethod fun connect(call: PluginCall) {
        val host = call.getString("host")?.trim().orEmpty()
        val port = call.getInt("port", 5555) ?: 5555
        if (host.isBlank() || port !in 1..65535) { call.reject("IP atau port tidak valid"); return }
        executor.execute {
            try {
                closeClient()
                val keyPair = getOrCreateKeyPair()
                val adb = Dadb.create(host, port, keyPair)
                val response = adb.shell("getprop ro.product.model")
                if (response.exitCode != 0) { adb.close(); throw Exception("ADB shell gagal") }
                dadbClient = adb
                val ret = JSObject()
                ret.put("ok", true)
                ret.put("host", host)
                ret.put("port", port)
                ret.put("model", response.output.trim())
                call.resolve(ret)
            } catch (e: Exception) { call.reject("Koneksi ADB gagal: ${e.message}", e) }
        }
    }

    @PluginMethod fun shell(call: PluginCall) {
        val command = call.getString("command")?.trim().orEmpty()
        if (command.isBlank()) { call.reject("Perintah kosong"); return }
        executor.execute {
            try {
                val adb = dadbClient ?: throw Exception("Belum terhubung ke TV")
                val response = adb.shell(command)
                val ret = JSObject()
                ret.put("output", response.output.trim())
                ret.put("exitCode", response.exitCode)
                call.resolve(ret)
            } catch (e: Exception) { call.reject("ADB shell gagal: ${e.message}", e) }
        }
    }

    @PluginMethod fun disconnect(call: PluginCall) { closeClient(); call.resolve(JSObject().put("ok", true)) }
    private fun closeClient() { try { dadbClient?.close() } catch (_: Exception) {}; dadbClient = null }
    override fun handleOnDestroy() { closeClient(); executor.shutdownNow(); super.handleOnDestroy() }
}
