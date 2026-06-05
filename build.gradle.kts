// Fichier de configuration Gradle au niveau du projet (racine)
// Les plugins sont résolus via pluginManagement dans settings.gradle.kts
// Ils sont appliqués directement dans le module :app
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}