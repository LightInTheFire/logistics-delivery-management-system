plugins {
    id("logistics.spring-boot-service")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation(libs.spring.cloud.config.client)
    implementation(libs.spring.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
}
