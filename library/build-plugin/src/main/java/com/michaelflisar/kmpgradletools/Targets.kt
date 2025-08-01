package com.michaelflisar.kmpgradletools

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class Targets(
    val android: Boolean = false,
    val iOS: Boolean = false,
    val windows: Boolean = false,
    val linux: Boolean = false,
    val macOS: Boolean = false,
    val wasm: Boolean = false,
    val js: Boolean = false,
) {
    private val enabledTargets = Target.values()
        .filter {
            when (it) {
                Target.ANDROID -> android
                Target.IOS -> iOS
                Target.WINDOWS -> windows
                Target.MACOS -> macOS
                Target.LINUX -> linux
                Target.WASM -> wasm
                Target.JS -> js
            }
        }

    fun updateSourceSetDependencies(
        sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
        apply: (groupMain: KotlinSourceSet, target: Target) -> Unit,
    ) {
        enabledTargets
            .forEach { target ->
                val groupMain = sourceSets.maybeCreate(target.nameMain)
                apply(groupMain, target)
                target.targets.forEach {
                    sourceSets.getByName("${it}Main").dependsOn(groupMain)
                }
            }
    }
}