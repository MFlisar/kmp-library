plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("BuildLogicPlugin") {
            id = "com.michaelflisar.buildlogic"
            implementationClass = "com.michaelflisar.buildlogic.BuildLogicPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.maven.publish.plugin)
    implementation(libs.kotlin.multiplatform)
    implementation(libs.android.build.tools)
    implementation(libs.android.build.tools)
}