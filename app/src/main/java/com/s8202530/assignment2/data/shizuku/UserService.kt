package com.s8202530.assignment2.data.shizuku

import com.s8202530.assignment2.IUserService
import kotlin.system.exitProcess

/**
 * The implementation that Shizuku starts in a separate, shell-privileged
 * process. Shizuku requires a public no-arg constructor.
 */
class UserService : IUserService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun runCommand(command: Array<String>): String = try {
        val process = Runtime.getRuntime().exec(command)
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code == 0) "OK" else "ERR:${err.ifBlank { out }.trim().ifBlank { "exit $code" }}"
    } catch (e: Exception) {
        "ERR:${e.message}"
    }
}
