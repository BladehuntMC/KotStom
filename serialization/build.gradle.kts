plugins {
    id("buildlogic.common")
    `maven-publish`
}

dependencies {
    compileOnly(libs.minestom)
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.adventure.minimessage)

    testImplementation(libs.minestom)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.test)
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifactId = "kotstom-serialization"

            pom {
                name = "KotStom Serialization"
                description = "kotlinx.serialization utilities for Minestom"
                url = "https://github.com/bladehuntmc/KotStom"

                licenses {
                    license {
                        name = "MIT License"
                        url = "http://www.opensource.org/licenses/mit-license.php"
                    }
                }

                developers {
                    developer {
                        id = "oglass"
                        name = "oglass"
                        email = "him@oglass.dev"
                    }
                }

                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/bladehuntmc/KotStom/issues"
                }

                scm {
                    connection = "scm:git:git:github.com/bladehuntmc/KotStom.git"
                    developerConnection = "scm:git:https://github.com/bladehuntmc/KotStom.git"
                    url = "https://github.com/bladehuntmc/KotStom"
                }
            }
        }
    }
}
