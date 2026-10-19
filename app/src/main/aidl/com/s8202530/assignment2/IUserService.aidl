// IUserService.aidl
package com.s8202530.assignment2;

/**
 * Runs inside a process started by Shizuku with ADB-shell privileges.
 * `runCommand` therefore has the rights needed for `pm grant` / `pm revoke`.
 */
interface IUserService {

    // Called by the Shizuku server when the service is destroyed. Reserved id.
    void destroy() = 16777114;

    // Runs a command and returns "OK" on success, or "ERR:<message>" on failure.
    String runCommand(in String[] command) = 1;
}
