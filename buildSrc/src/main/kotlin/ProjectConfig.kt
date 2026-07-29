import org.gradle.api.JavaVersion

object ProjectConfig {
    const val minSdk = 26
    const val targetSdk = 37
    const val compileSdk = 37
    const val ndk = "21.3.6528147"
    const val versionName = "1.1.2"  // MAJOR must stay > 0 for DMG packaging
    const val versionCode = 77
    const val applicationId = "ir.kazemcodes.infinityreader"

    val desktopJvmTarget = JavaVersion.VERSION_21
    val androidJvmTarget = JavaVersion.VERSION_21
    val toolChain = 14
}
