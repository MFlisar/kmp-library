package com.michaelflisar.example.test

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test

class Test {

    private val appFolder = File(System.getProperty("user.dir"))

    @Test
    fun test() = runTest {
        println("Running test in: ${appFolder.path}")
    }

}