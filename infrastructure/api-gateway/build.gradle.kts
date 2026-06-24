plugins {
    id("logistics.spring-boot-service")
}

dependencies {
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation(libs.spring.cloud.config.client)
}
