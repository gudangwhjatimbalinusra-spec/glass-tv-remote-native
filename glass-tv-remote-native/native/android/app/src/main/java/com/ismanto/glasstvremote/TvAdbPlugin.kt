package com.ismanto.glasstvremote

import com.getcapacitor.*
import com.flyfishxu.kadb.Kadb
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

@CapacitorPlugin(name = "TvAdb")
class TvAdbPlugin : Plugin() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var client: Any? = null

    @PluginMethod fun connect(call: PluginCall) {
        val host = call.getString("host")?.trim().orEmpty()
        val port = call.getInt("port", 5555)
        if (host.isBlank() || port !in 1..65535) { call.reject("IP atau port tidak valid"); return }
        executor.execute {
            try {
                closeClient()
                val adb = Kadb.create(host, port)
                val response = adb.shell("getprop ro.product.model && getprop ro.build.version.release")
                if (response.exitCode != 0) { adb.close(); throw Exception(response.output.ifBlank { "ADB shell gagal" }) }
                client = adb
                val lines = response.output.lines().filter { it.isNotBlank() }
                val ret = JSObject(); ret.put("ok", true); ret.put("host", host); ret.put("port", port)
                ret.put("model", lines.getOrNull(0) ?: "Android TV"); ret.put("release", lines.getOrNull(1) ?: "")
                call.resolve(ret)
            } catch (e: Exception) { call.reject("Koneksi ADB gagal: ${e.message}", e) }
        }
    }

    @PluginMethod fun pair(call: PluginCall) {
        val host = call.getString("host")?.trim().orEmpty()
        val port = call.getInt("port", -1)
        val code = call.getString("code")?.trim().orEmpty()
        if (host.isBlank() || port !in 1..65535 || !code.matches(Regex("\\d{6}"))) { call.reject("Host, port pairing, atau kode pairing tidak valid"); return }
        executor.execute {
            try {
                runBlocking {
                    Kadb.pair(host, port, code)
                }
                val ret = JSObject(); ret.put("ok", true); ret.put("host", host); ret.put("paired", true); call.resolve(ret)
            } catch (e: Exception) { call.reject("Pairing ADB gagal: ${e.message}", e) }
        }
    }

    @PluginMethod fun shell(call: PluginCall) {
        val command = call.getString("command")?.trim().orEmpty()
        if (command.isBlank() || command.length > 500) { call.reject("Perintah kosong atau terlalu panjang"); return }
        executor.execute {
            try {
                val adb = client ?: throw Exception("Belum terhubung ke TV")
                val response = adb.javaClass.getMethod("shell", String::class.java).invoke(adb, command)
                val cls = response.javaClass
                val output = cls.getMethod("getOutput").invoke(response)?.toString() ?: ""
                val exit = (cls.getMethod("getExitCode").invoke(response) as Number).toInt()
                val ret = JSObject(); ret.put("output", output); ret.put("exitCode", exit); call.resolve(ret)
            } catch (e: Exception) { call.reject("ADB shell gagal: ${e.cause?.message ?: e.message}", e) }
        }
    }

    @PluginMethod fun disconnect(call: PluginCall) { closeClient(); call.resolve(JSObject().put("ok", true)) }
    private fun closeClient() { try { (client as? AutoCloseable)?.close() } catch (_: Exception) {}; client = null }
    override fun handleOnDestroy() { closeClient(); executor.shutdownNow(); super.handleOnDestroy() }
}
