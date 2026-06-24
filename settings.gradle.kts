rootProject.name = "logistics-delivery-management-system"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "infrastructure:discovery-server",
    "infrastructure:config-server",
    "infrastructure:api-gateway",
    "services:cargo-service",
    "services:transport-service",
    "services:delivery-service",
    "services:notification-service",
    "services:reporting-service"
)
