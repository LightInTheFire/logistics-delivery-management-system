plugins {
    id("logistics.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${Versions.SPRING_CLOUD}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("org.projectlombok:lombok:${Versions.LOMBOK}")
    annotationProcessor("org.projectlombok:lombok:${Versions.LOMBOK}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
