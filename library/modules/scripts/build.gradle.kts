plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    //sourceSets {
    //    val main by getting {
    //        kotlin.srcDir("../../build-logic/src/main/java/com/michaelflisar/buildlogic/shared")
    //    }
    //}
}

dependencies {
    implementation(deps.ktoml.core)
    implementation(deps.ktoml.file)
    implementation(deps.jsoup)
    implementation(deps.yaml)

    implementation(project(":shared"))
}

// allows to run the application with `./gradlew run -PmainClass=com.michaelflisar.scripts.UpdateDocsKt`
if (System.getenv("CI")?.toBoolean() == true) {
    application {
        val mc = project.findProperty("mainClass") as? String
        if (mc != null) {
            mainClass.set(mc)
        }
    }
}