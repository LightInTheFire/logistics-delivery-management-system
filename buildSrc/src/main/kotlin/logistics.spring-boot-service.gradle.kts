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
    implementation("org.mapstruct:mapstruct:${Versions.MAPSTRUCT}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${Versions.MAPSTRUCT}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.LOGSTASH}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
