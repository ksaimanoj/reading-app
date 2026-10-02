plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val releaseSigningEnvironment = mapOf(
    "LITTLEWORDS_RELEASE_KEYSTORE" to System.getenv("LITTLEWORDS_RELEASE_KEYSTORE"),
    "LITTLEWORDS_RELEASE_STORE_PASSWORD" to System.getenv("LITTLEWORDS_RELEASE_STORE_PASSWORD"),
    "LITTLEWORDS_RELEASE_KEY_ALIAS" to System.getenv("LITTLEWORDS_RELEASE_KEY_ALIAS"),
    "LITTLEWORDS_RELEASE_KEY_PASSWORD" to System.getenv("LITTLEWORDS_RELEASE_KEY_PASSWORD"),
)
val releaseSigningRequested = releaseSigningEnvironment.values.any { !it.isNullOrBlank() }
if (releaseSigningRequested) {
    require(releaseSigningEnvironment.values.all { !it.isNullOrBlank() }) {
        "Release signing requires all four LITTLEWORDS_RELEASE_* environment variables."
    }
}

android {
    namespace = "com.littlewords.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.littlewords.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    if (releaseSigningRequested) {
        signingConfigs {
            create("release") {
                val keystore = file(releaseSigningEnvironment.getValue("LITTLEWORDS_RELEASE_KEYSTORE")!!)
                require(keystore.isFile) { "Release keystore does not exist: $keystore" }
                storeFile = keystore
                storePassword = releaseSigningEnvironment.getValue("LITTLEWORDS_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningEnvironment.getValue("LITTLEWORDS_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningEnvironment.getValue("LITTLEWORDS_RELEASE_KEY_PASSWORD")
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
