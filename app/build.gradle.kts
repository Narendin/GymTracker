import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Идентификатор приложения на телефоне берётся из secrets.properties (файл в .gitignore),
// чтобы публичный репозиторий его не раскрывал.
val secrets = Properties().apply {
    val file = rootProject.file("secrets.properties")
    // Читаем как UTF-8: свойства по умолчанию читаются в ISO-8859-1,
    // и кириллица в пути ключа превращается в недопустимые символы.
    if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
}
val applicationIdFromSecrets = secrets.getProperty("applicationId")
    ?: throw GradleException(
        "Не найден applicationId. Создайте secrets.properties в корне проекта " +
            "(см. secrets.properties.example) и укажите applicationId=…",
    )

// Данные релизного ключа: без них релизная сборка остаётся неподписанной.
val signingKeystore = secrets.getProperty("signing.keystore")
val signingStorePassword = secrets.getProperty("signing.storePassword")
val signingKeyAlias = secrets.getProperty("signing.keyAlias")
val signingKeyPassword = secrets.getProperty("signing.keyPassword")
val signingReady = listOf(
    signingKeystore, signingStorePassword, signingKeyAlias, signingKeyPassword,
).all { !it.isNullOrEmpty() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.gymtracker"
    compileSdk = 35

    signingConfigs {
        if (signingReady) {
            create("release") {
                storeFile = file(signingKeystore)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = applicationIdFromSecrets
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.coil.compose)

    ksp(libs.androidx.room.compiler)

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // Espresso 3.5/3.6 не работает на API 36 (InputManager.getInstance удалён) — нужен 3.7+.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
