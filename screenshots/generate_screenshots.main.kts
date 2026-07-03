#!/usr/bin/env kotlin

import java.io.File
import java.util.concurrent.TimeUnit

fun runCommand(vararg cmd: String, workingDir: File? = null): String {
    println("Running: ${cmd.joinToString(" ")}")
    val process = ProcessBuilder(*cmd)
        .directory(workingDir)
        .redirectErrorStream(true)
        .start()
    
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor(5, TimeUnit.MINUTES)
    if (process.exitValue() != 0) {
        println("Command failed: ${cmd.joinToString(" ")}")
        println("Output: $output")
        throw RuntimeException("Command failed with exit code ${process.exitValue()}")
    }
    return output
}

fun main() {
    val projectDir = File("..").absoluteFile.normalize()
    val outputDir = File(projectDir, "screenshots/output")
    
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    
    val packageId = "io.github.benji377.timety"
    val devicePicDir = "/sdcard/Android/data/$packageId/files/Pictures/"
    
    println("Ensure an emulator is running!")
    
    // 1. Run the instrumentation test
    val gradlew = File(projectDir, "gradlew").absolutePath
    runCommand(gradlew, "connectedAndroidTest", "-Pandroid.testInstrumentationRunnerArguments.class=io.github.benji377.timety.ScreenshotTest", workingDir = projectDir)
    
    // 2. Pull the screenshots
    println("Pulling screenshots from device...")
    try {
        runCommand("adb", "pull", devicePicDir, outputDir.absolutePath)
        println("Screenshots successfully saved to ${outputDir.absolutePath}")
    } catch (e: Exception) {
        println("Error pulling screenshots. Ensure the test saved them properly.")
    }
    
    // 3. Cleanup device
    try {
        runCommand("adb", "shell", "rm", "-r", devicePicDir)
    } catch (e: Exception) {
        // ignore cleanup error
    }
}

main()
