plugins {
    java
    id("io.quarkus")
    checkstyle
    id("org.owasp.dependencycheck") version "12.2.2"
    id("org.cyclonedx.bom") version "3.2.4"
}

group = findProperty("group") as String
version = findProperty("version") as String

// Los artefactos de Nova se publican en el GitHub Packages de cada repositorio, que pide credenciales incluso
// para leer. En CI llega el token del workflow; en local, un GITHUB_TOKEN con read:packages.
fun RepositoryHandler.nova(repository: String) = maven {
    name = repository
    url = uri("https://maven.pkg.github.com/ahincho/$repository")
    credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("NOVA_PACKAGES_READ_TOKEN") ?: System.getenv("GITHUB_TOKEN")
    }
}

repositories {
    mavenCentral()
    nova("nova-java-13-bom")
    nova("nova-java-10-api-standard-quarkus-extension")
    nova("nova-java-01-api-standard")
    nova("nova-java-23-secrets")
    nova("nova-java-28-persistence")
}

val novaBom = "4.2.1"

dependencies {
    // El BOM de Quarkus de Nova: Quarkus 3.33.3.3 y las extensiones de Nova en versiones que se conocen entre sí.
    implementation(enforcedPlatform("pe.edu.nova.java:nova-quarkus-bom:$novaBom"))

    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-opentelemetry")

    // El sobre y los errores por capas de Nova (ADR-031 y ADR-050).
    implementation("pe.edu.nova.java.starters:nova-api-standard-quarkus-extension")
    // Las credenciales de la base salen de Vault (ADR-049).
    implementation("pe.edu.nova.java.starters:nova-secrets-quarkus-extension")
    runtimeOnly("pe.edu.nova.java.libs:nova-secrets-vault")
    // El contrato de la paginación por cursor, el mismo que pedidos (ADR-054): el núcleo es Java puro.
    implementation("pe.edu.nova.java.libs:nova-persistence:1.0.0")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.tngtech.archunit:archunit:1.4.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial", "-Werror"))
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

checkstyle {
    toolVersion = "10.12.0"
}

dependencyCheck {
    // Lo inyecta reusable-owasp-check.yml; en local, sin variables, nunca falla.
    failBuildOnCVSS = (System.getenv("NOVA_OWASP_FAIL_ON_CVSS") ?: "11").toFloat()
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
    // Solo lo que se despliega: el plugin de Quarkus agrega configuraciones del Dev UI y de la augmentation.
    scanConfigurations = listOf("compileClasspath", "runtimeClasspath")
    formats = listOf("HTML", "JSON")
    // El workflow trae un mirror de NVD; con autoUpdate, el plugin lo ignora y sincroniza desde cero.
    autoUpdate = false
    data.directory = System.getenv("NOVA_OWASP_DATA_DIR") ?: "${System.getProperty("user.home")}/.dependency-check-data"
}
