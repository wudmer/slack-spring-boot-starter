import com.kreait.publish.meta.Contributor
import com.kreait.publish.meta.Developer
import com.kreait.publish.meta.License
import com.kreait.publish.meta.Organization
import com.kreait.publish.meta.Scm
import groovy.json.StringEscapeUtils
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven("http://repo.spring.io/plugins-release")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.41")
        classpath("io.spring.gradle:propdeps-plugin:0.0.9.RELEASE")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.0.1")
    }
}

plugins {
    id("org.sonarqube") version "2.7"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    id("org.jetbrains.kotlin.jvm") version "1.3.41" apply false
    id("com.gradle.build-scan") version "2.2.1"
    signing
}

sonarqube {
    properties {
        property("sonar.projectName", "Slack Spring Boot Starter")
        property("sonar.projectKey", "com.kreait.slack-spring-boot-starter")
        property("sonar.host.url", "https://sonarcloud.io/")
        property("sonar.organization", "kreait")
        property("sonar.jacoco.report missing.force.zero", "true")
        property("sonar.pullrequest.provider", "github")
        property("sonar.exclusions", "**/slack-jackson-dto-test-extensions/**," +
                "**/slack-jackson-dto/**," +
                "**/samples/**, " +
                "**/slack-api-client/**, " +
                "**/SL4JLoggingReceiver.kt")
    }
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

allprojects {

    val isRelease: String? by project

    apply {
        from("${rootProject.rootDir}/publish-meta.gradle.kts")
    }

    group = "com.kreait.slack"
    version = rootProject.file("version.txt").readText().trim()
            .plus(if (isRelease?.toBoolean() == true) "" else "-${System.getenv("TRAVIS_BUILD_NUMBER") ?: ""}-SNAPSHOT")

    project.ext {
        set("junitJupiterVersion", "5.4.2")
        set("junitPlatformVersion", "1.4.2")
        set("springBootVersion", "2.1.3.RELEASE")
        set("projectVersion", version)
        set("kotlinVersion", "1.3.21")
    }

    tasks.withType<Wrapper> {
        gradleVersion = "5.4.1"
        // anything else
    }

    apply {
        plugin("io.spring.dependency-management")
    }


    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${extra["springBootVersion"]}") {
                bomProperty("kotlin.version", "${extra["kotlinVersion"]}")
            }
        }
    }
}


subprojects {

    val isRelease: String? by project
    val rawVersion = rootProject.file("version.txt").readText().trim()

    apply {
        plugin("eclipse")
        plugin("idea")
        plugin("kotlin")
        plugin("jacoco")
        plugin("maven-publish")
        plugin("propdeps")
        plugin("org.jetbrains.dokka")
        plugin("io.gitlab.arturbosch.detekt")
        plugin("signing")
    }

    tasks.withType<DokkaTask> {
        moduleName = project.name
        outputFormat = "html"
        outputDirectory = "${rootProject.buildDir}/generated-docs/api"
    }

    afterEvaluate {
        configure<PublishingExtension> {
            val projectsNotToPublish = listOf<Project>(project(":slack-spring-boot-starter-sample"), project(":slack-spring-boot-docs"))
            repositories {
                maven {
                    name = "snapshot"
                    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
                    credentials {
                        username = System.getenv("SONATYPE_JIRA_USERNAME")
                        password = System.getenv("SONATYPE_JIRA_PASSWORD")
                    }
                }
                maven {
                    name = "release"
                    url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials {
                        username = System.getenv("SONATYPE_JIRA_USERNAME")
                        password = System.getenv("SONATYPE_JIRA_PASSWORD")
                    }
                }
            }

            // only create publications for relevant projects
            if (!projectsNotToPublish.contains(project)) {
                publications {
                    val sourcesJar by tasks.registering(Jar::class) {
                        archiveClassifier.value("sources")
                        from(project.the<SourceSetContainer>()["main"].allSource)
                    }
                    val javaDoc by tasks.registering(Jar::class) {
                        archiveClassifier.value("javadoc")
                        from(tasks.withType<DokkaTask>())
                    }
                    create(project.name, MavenPublication::class) {
                        from(components["java"])
                        artifact(sourcesJar.get())
                        artifact(javaDoc.get())
                        pom {
                            if (project.extra.has("displayName")) {
                                name.set(project.extra["displayName"] as? String)
                            }
                            description.set(project.description)
                            pom {
                                url.set("https://slack-spring-boot.kreait.dev")
                                (extra["organization"] as Organization).let {
                                    this.organization {
                                        name.set(it.name)
                                        url.set(it.url)
                                    }
                                }

                                licenses {
                                    (extra["license"] as License).let {
                                        this.license {
                                            name.set(it.name)
                                            url.set(it.url)
                                        }
                                    }
                                }
                                developers {
                                    (extra["developers"] as List<Developer>).forEach {
                                        this.developer {
                                            id.set(it.id)
                                            name.set(it.name)
                                            email.set(it.email)
                                        }
                                    }
                                }
                                contributors {
                                    (extra["contributors"] as List<Contributor>).forEach {
                                        this.contributor {
                                            name.set(it.name)
                                            email.set(it.email)
                                        }
                                    }
                                }
                                (extra["scm"] as Scm).let {
                                    this.scm {
                                        connection.set(it.connection)
                                        url.set(it.url)
                                    }
                                }
                            }
                        }
                    }
                }
                signing {
                    val signingKey: String? by project
                    val signingPassword: String? by project
                    useInMemoryPgpKeys(StringEscapeUtils.unescapeJava(signingKey), signingPassword)
                    sign(publications[project.name])
                }
            }

        }
    }

    /**
     * Replace illegal characters from the module key
     */
    sonarqube {
        val regex = "(.*)/(.*)"
        val projectKey = project.name.replace(regex, "\$1:\$2")
        val sonarModuleKey = "${rootProject.group}:${rootProject.name}:$projectKey"
        properties {
            property("sonar.moduleKey", sonarModuleKey)
            property("sonar.kotlin.detekt.reportPaths", "${project.buildDir}/reports/detekt/detekt.xml")
        }
    }

    tasks.withType<KotlinCompile>() {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }
    tasks.withType<KotlinCompile>() {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.3"
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.apply {
                isEnabled = true
            }

        }
    }

    //region Release settings
    tasks.withType<Sign>().configureEach {
        onlyIf {
            isRelease?.toBoolean() ?: false
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        val release = isRelease?.toBoolean() ?: false
        onlyIf {
            (repository.name == "snapshot" && !release)
                    ||
                    (repository.name == "release" && release)
        }
    }
    //endregion

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperties(System.getenv())
        systemProperties["user.dir"] = workingDir
    }

    tasks.withType<Detekt>().configureEach {
        config = files("${rootProject.rootDir}/detekt.yml")
    }

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        "testImplementation"("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
        "testImplementation"(group = "org.junit.jupiter", name = "junit-jupiter", version = "${extra["junitJupiterVersion"]}")
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}
