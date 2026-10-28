import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import versioning.BuildConfig
import java.util.zip.ZipFile

plugins {
    `maven-publish`
    thunder.`base-conventions`
    thunder.`shadow-conventions`
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
}

repositories {
    val localOverride = if (BuildConfig.mavenLocalOverride) mavenLocal() else null

    // Exclusive Repositories (One HTTP request per dep)
    exclusive("https://repo.papermc.io/repository/maven-public/", { name = "papermc" }) {
        includeGroup("io.papermc.paper")
        includeGroup("net.md-5")
    }

    exclusive("https://libraries.minecraft.net", { mavenContent { releasesOnly() } }) {
        includeModule("com.mojang", "brigadier")
    }

    exclusive("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        includeGroup("me.clip")
    }

    val thunderPublicReleases = maven("https://maven.grim.ac/public/releases") {
        mavenContent { releasesOnly() }
    }
    val thunderPublicSnapshots = maven("https://maven.grim.ac/public/snapshots") {
        mavenContent { snapshotsOnly() }
    }
    val thunderLegacySnapshots = maven("https://repo.grim.ac/snapshots")
    exclusiveContent {
        forRepositories(*listOfNotNull(localOverride, thunderPublicReleases, thunderPublicSnapshots, thunderLegacySnapshots).toTypedArray())
        filter {
            includeGroup("ac.grim.grimac")
            includeGroup("com.github.retrooper")
        }
    }

    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    mavenCentral()
}

val configuredLiteSharedProviderJar = providers.gradleProperty("grim.liteSharedProviderJar")
    .orElse(providers.gradleProperty("liteSharedProviderJar"))

// PE's published Spigot artifact is thin. Its runtime jar bundles these modules,
// with text serializers relocated into PE's own namespace.
val liteSharedLibraries by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}
val liteCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
if (!BuildConfig.shadePE) {
    val adventureVersion = liteCatalog.findVersion("adventure").get().requiredVersion
    val examinationVersion = liteCatalog.findVersion("examination").get().requiredVersion
    for (module in listOf("adventure-api", "adventure-key", "adventure-nbt")) {
        dependencies.add(liteSharedLibraries.name, "net.kyori:$module:$adventureVersion")
    }
    for (module in listOf("examination-api", "examination-string")) {
        dependencies.add(liteSharedLibraries.name, "net.kyori:$module:$examinationVersion")
    }
}

val liteSharedPrefixes = listOf(
    "net/kyori/adventure/",
    "net/kyori/examination/",
    "net/kyori/option/",
)

var cachedLiteSharedProviderClassEntries: Set<String>? = null
var cachedLiteSharedProviderFiles: List<File>? = null

fun liteSharedProviderFiles(): List<File> {
    cachedLiteSharedProviderFiles?.let { return it }

    val configured = configuredLiteSharedProviderJar.orNull
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { file(it) }
        ?.takeIf { it.isNotEmpty() }

    val files = configured ?: liteSharedLibraries.resolve().toList()

    cachedLiteSharedProviderFiles = files
    return files
}

fun liteSharedProviderClassEntries(): Set<String> {
    cachedLiteSharedProviderClassEntries?.let { return it }

    val entries = liteSharedProviderFiles().flatMap { jar ->
        ZipFile(jar).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { name ->
                    name.endsWith(".class") && liteSharedPrefixes.any(name::startsWith) &&
                        !name.startsWith("net/kyori/adventure/text/serializer/")
                }
                .toList()
        }
    }.toSet()

    cachedLiteSharedProviderClassEntries = entries
    return entries
}


dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.luckperms)

    if (BuildConfig.shadePE) {
        implementation(libs.packetevents.spigot)
    } else {
        compileOnly(libs.packetevents.spigot)
    }
    implementation(libs.cloud.paper)
    implementation(libs.adventure.platform.bukkit)
    implementation(libs.thunder.bukkit.internal)

    implementation(project(":common"))
    shadow(project(":common"))
}

bukkit {
    name = "Thunder"
    author = "Thunder"
    main = "ac.thunder.thunder.platform.bukkit.ThunderBukkitLoaderPlugin"
    website = "https://thunder.ac/"
    apiVersion = "1.13"
    foliaSupported = true

    if (!BuildConfig.shadePE) {
        depend = listOf("packetevents")
    }

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "Essentials",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "floodgate",
        "FastLogin",
        "PlaceholderAPI",
        "LuckPerms",
        // Driver holder mods — softdepend so each backend's driver class
        // resolves through the linked classloader.
        "sqlite-jdbc",
        "mysql-jdbc",
        "postgresql-jdbc",
        "mongodb-driver",
        "jedis",
    )

    permissions {
        register("thunder.alerts") {
            description = "Получение оповещений о нарушениях"
            default = Permission.Default.OP
        }

        register("thunder.alerts.enable-on-join") {
            description = "Включать оповещения при входе"
            default = Permission.Default.OP
        }

        register("thunder.performance") {
            description = "Просмотр метрик производительности"
            default = Permission.Default.OP
        }

        register("thunder.profile") {
            description = "Просмотр профиля игрока"
            default = Permission.Default.OP
        }

        register("thunder.brand") {
            description = "Показ брендов клиентов при входе"
            default = Permission.Default.OP
        }

        register("thunder.brand.enable-on-join") {
            description = "Включать показ брендов при входе"
            default = Permission.Default.OP
        }

        register("thunder.sendalert") {
            description = "Отправка оповещения о читере"
            default = Permission.Default.OP
        }

        register("thunder.nosetback") {
            description = "Отключить откат (сетбэк)"
            default = Permission.Default.FALSE
        }

        register("thunder.nomodifypacket") {
            description = "Запретить изменение пакетов"
            default = Permission.Default.FALSE
        }

        register("thunder.disabled") {
            description = "Отключить проверки Thunder, сохранив отслеживание игрока"
            default = Permission.Default.FALSE
        }

        register("thunder.exempt") {
            description = "Освобождение от всех проверок"
            default = Permission.Default.FALSE
        }

        register("thunder.verbose") {
            description = "Получение детальных оповещений о нарушениях"
            default = Permission.Default.OP
        }

        register("thunder.verbose.enable-on-join") {
            description =
                "Включать детальные оповещения при входе"
            default = Permission.Default.FALSE
        }

        register("thunder.list") {
            description =
                "Просмотр списков данных"
            default = Permission.Default.FALSE
        }

        register("thunder.spec") {
            description =
                "Получение заявок на слежку (/spec)"
            default = Permission.Default.OP
        }

    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["shadowJar"])
}

tasks {
    // 1.8.8 - 1.16.5   = Java 8
    // 1.17             = Java 16
    // 1.18 - 1.20.4    = Java 17
    // 1.20.5 - 1.21.11 = Java 21
    // 26.1+            = Java 25
    val version = "26.3"
    val javaVersion = JavaLanguageVersion.of(25)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true",
        "-Dpaper.explicit-flush=true",
        "-DPaper.IgnoreJavaVersion=true"
    )

    runServer {
        minecraftVersion(version)
        runDirectory = projectDir.resolve("run/$version")

        val javaToolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher = javaToolchains.launcherFor {
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = javaVersion
        }

        jvmArgs = jvmArgsExternal
    }

    shadowJar {
        exclude("META-INF/services/javax.annotation.processing.Processor")

        if (!BuildConfig.shadePE) {
            inputs.files(provider { liteSharedProviderFiles() }).withPropertyName("liteSharedProviders")
            exclude {
                val path = it.path
                path.endsWith(".class") && path in liteSharedProviderClassEntries()
            }

            doFirst {
                logger.lifecycle(
                    "Excluding ${liteSharedProviderClassEntries().size} shared class entries supplied by PacketEvents: " +
                        liteSharedProviderFiles().joinToString { it.name }
                )
            }
        }

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }
}
