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
    process.waitFor(15, TimeUnit.MINUTES)
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

    println("Ensure an emulator is running!")

    // 1. Run the instrumentation test.
    val gradlew = File(projectDir, "gradlew").absolutePath
    runCommand(
        gradlew,
        "connectedAndroidTest",
        "-Pandroid.testInstrumentationRunnerArguments.class=io.github.benji377.timety.ScreenshotTest",
        workingDir = projectDir
    )

    // 2. Collect the screenshots.
    // ScreenshotTest writes into AGP's additionalTestOutputDir, which the connected test
    // task downloads to build/outputs/ before it uninstalls the app. (An `adb pull` of the
    // app-private Pictures dir would find nothing - it is wiped along with the uninstall.)
    val additionalOutput =
        File(projectDir, "app/build/outputs/connected_android_test_additional_output")
    val screenshots = additionalOutput.walkTopDown()
        .filter { it.isFile && it.extension == "png" }
        .toList()
    if (screenshots.isEmpty()) {
        throw RuntimeException(
            "No screenshots found under $additionalOutput. Ensure the test saved them properly."
        )
    }
    screenshots.forEach { it.copyTo(File(outputDir, it.name), overwrite = true) }
    println("${screenshots.size} screenshots saved to ${outputDir.absolutePath}")
}

main()
