plugins {
    id("logistics.spring-boot-server")
}

dependencies {
    implementation(libs.spring.cloud.config.server)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation(libs.spring.boot.starter.actuator)
}
