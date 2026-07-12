/*
    Settings for Village Clinic Ledger – configures repository sources and
    the set of included modules.

    Note: This file uses the Kotlin DSL (.kts) while the other Gradle files
    use Groovy. Mixing DSLs is intentional: the settings file benefits from
    Kotlin's type-safe accessors for repository declarations.
*/

pluginManagement {
    repositories {
        gradlePluginPortal()    // AGP, Kotlin plugins
        google()               // Android-specific plugins
        mavenCentral()         // General-purpose plugin registry
    }
}

dependencyResolutionManagement {
    // Fail the build if any sub-module tries to declare its own repository.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()               // AndroidX, Material, Play Services
        mavenCentral()         // Standard OSS libraries
    }
}

rootProject.name = "Village Clinic Ledger (ग्राम क्लिनिक लेजर)"
include(":app")
