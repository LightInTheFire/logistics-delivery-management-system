plugins {
    id("logistics.spring-boot-server")
}

dependencies {
    implementation(libs.spring.cloud.starter.netflix.eureka.server)
    implementation(libs.caffeine)
}
