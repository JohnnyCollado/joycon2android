plugins {
    id("joycon.android.library")
}

android {
    namespace = "com.joegec.joycon2android.gamepad.data"
}

dependencies {
    implementation(project(":feature:gamepad:domain"))
    implementation(project(":core:model"))
    implementation(libs.libadb.android)
    implementation(libs.sun.security.android)
    implementation(libs.conscrypt.android)
}
