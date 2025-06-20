import java.util.UUID

plugins {
    id("com.android.library")
}

android {
    namespace = "com.apiaryassistant.models"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        buildConfig = false
    }

    sourceSets {
        named("main") {
            assets.srcDirs("src/main/assets")
            res.srcDirs()
        }
    }
}

tasks.register("genUUID") {
    doLast {
        val uuid = UUID.randomUUID().toString()
        val uuidFile = file("src/main/assets/model/vosk-model-small-ru-0.22/uuid")
        uuidFile.parentFile.mkdirs()
        uuidFile.writeText(uuid)
        println("✅ UUID сгенерирован в models: $uuid")
    }
}

tasks.named("preBuild") {
    dependsOn("genUUID")
}
