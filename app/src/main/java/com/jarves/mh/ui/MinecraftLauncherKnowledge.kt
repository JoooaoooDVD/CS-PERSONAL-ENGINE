package com.jarves.mh.ui

data class McLauncherBlueprint(
    val id: String,
    val badge: String,
    val title: String,
    val stackLabel: String,
    val summary: String,
    val prompt: String,
)

/**
 * Specialist Domain Brain for Android Minecraft Launcher Engineering + Java / Android XML / Kotlin.
 *
 * Automatically supplies authoritative architecture, Mojang/Fabric/Modrinth protocol specs,
 * ARM64 JRE/JNI/LWJGL3 launch knowledge, and strict Java + XML + Kotlin coding standards
 * to the active AI model so it produces production-grade Minecraft Launcher code on the first shot.
 */
object MinecraftLauncherKnowledge {

    /**
     * Compact, high-signal engineering specification injected into AI execution prompts.
     */
    fun buildSpecialistSystemBrief(userPrompt: String): String {
        val lower = userPrompt.lowercase()
        val mcExplicit = listOf(
            "minecraft", "launcher", "pojav", "zalith", "mojang", "lwjgl", "fabric",
            "forge", "neoforge", "modrinth", "jre", "libjli", "gl4es", "zink", "turnip",
            "skin", "xbox", "xsts", "asset", "version_manifest", "mc ",
        ).any { lower.contains(it) }

        return buildString {
            appendLine("[CS Personal Engine v5.0 — Primary Stack & Minecraft Launcher Specialist Context]")
            appendLine("1. PRIMARY LANGUAGE & UI FOCUS (MANDATORY UNLESS ASKED OTHERWISE):")
            appendLine("   - Prioritize clean, production-grade **Java**, **Android XML** (`res/layout/*.xml`, `res/drawable/*.xml`, `res/values/*.xml`, `ConstraintLayout`, `RecyclerView`, `MaterialCardView`, `ViewBinding`), and **Kotlin**.")
            appendLine("   - Design Android XML UIs for **Horizontal Widescreen (`sensorLandscape`)** ergonomics first: left navigation/profile rail, central content/news/mod grid, and crisp bottom launch/action bar with zero wasted space.")
            appendLine("   - Always write complete, compilable `.java`, `.kt`, and `.xml` files (never leave `// TODO` stubs) and verify builds when possible.")
            appendLine("2. MINECRAFT JAVA/ANDROID LAUNCHER ARCHITECTURE REFERENCE:")
            appendLine("   - Mojang Manifests & Downloads: `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` -> version JSON (`libraries[]` with OS/arch rules, `assetIndex.url`, `downloads.client.url`, `mainClass`, `arguments.game` & `arguments.jvm`, `javaVersion.majorVersion`), plus `https://resources.download.minecraft.net/<first2hex>/<sha1>` verified via SHA-1 on a multi-threaded `ExecutorService` / Kotlin `Dispatchers.IO` pool.")
            appendLine("   - Modloaders & Mods: Fabric Meta v2 (`https://meta.fabricmc.net/v2/versions/loader/<mcVer>/<loaderVer>/profile/json`, `KnotClient`), Forge / NeoForge (`BootstrapLauncher`), and Modrinth v2 (`https://api.modrinth.com/v2/search` & `/project/{id}/version`).")
            appendLine("   - Authentication: Microsoft OAuth2 / Device-Code -> Xbox Live (`https://user.auth.xboxlive.com/user/authenticate`) -> XSTS (`https://xsts.auth.xboxlive.com/xsts/authorize` relyingParty `rp://api.minecraftservices.com/`) -> `https://api.minecraftservices.com/authentication/login_with_xbox` -> `/minecraft/profile` (accessToken + UUID + skin URL), plus Offline mode UUID v3 (`UUID.nameUUIDFromBytes((\"OfflinePlayer:\" + name).getBytes(UTF_8))`).")
            appendLine("   - Android ARM64 JVM & Renderer Pipeline (Pojav/Zalith/Amethyst style): Select ARM64 OpenJDK runtime by MC version (`JRE 8` for <=1.16.5, `JRE 17` for 1.17–1.20.4, `JRE 21` for 1.20.5+/1.21+); JNI `dlopen(\"libjli.so\")` -> `JLI_Launch` on a dedicated native thread with `-Xms`/`-Xmx` heap args, `-Djava.library.path` pointing to Android `arm64-v8a` LWJGL3 + GLFW stub natives, `SurfaceView`/`TextureView` `ANativeWindow` binding, renderer env (`LIBGL_ES`, `GL4ES` / `Zink` + `Turnip` Vulkan), and customizable XML touch-control overlays.")
            if (mcExplicit) {
                appendLine("   - Active Request Detected as Minecraft Launcher Task: Apply the exact Mojang/Modloader JSON schemas, horizontal XML gaming launcher polish (dark obsidian `#090C12` + emerald `#10B981` accents), thread-safe progress callbacks, and defensive error handling.")
            }
        }.trim()
    }

    val blueprints: List<McLauncherBlueprint> = listOf(
        McLauncherBlueprint(
            id = "mc_xml_home",
            badge = "JAVA + XML",
            title = "Horizontal Launcher UI + Activity",
            stackLabel = "Java · ConstraintLayout XML · ViewBinding",
            summary = "Complete widescreen landscape Minecraft Launcher home screen with profile rail, version picker, RAM slider, live download progress bar, and PLAY button.",
            prompt = """
                Build a complete, production-ready Android **Horizontal (Landscape) Minecraft Launcher Home UI** using **Java** and **Android XML** (`ConstraintLayout`, custom `drawable` XMLs, and clean MVC/MVVM separation):
                1. `res/layout/activity_mc_launcher.xml` optimized for `sensorLandscape` widescreen:
                   - Left sidebar (`220dp`): Player Avatar/Skin head card, Account switcher (Microsoft / Offline), Navigation buttons (Play, Installations, Mods/Modrinth, JVM & Renderer Settings, Console Logs).
                   - Main center stage: Hero banner card, Quick-stat chips (Selected Java Runtime JRE 8/17/21, Renderer GL4ES/Zink, Allocated RAM MB), and JVM RAM `SeekBar` (`512 MB` – `8192 MB`).
                   - Bottom launch dock: Version/Profile selector `Spinner` or card, real-time asset download `ProgressBar` + status `TextView`, and a bold emerald `LAUNCH MINECRAFT` button.
                2. Custom XML drawables (`bg_launcher_obsidian.xml`, `bg_card_glass.xml`, `btn_launch_emerald.xml`) in dark obsidian + emerald gaming style.
                3. `LauncherActivity.java` + `LauncherProfile.java` model wiring all views, saving selected version/RAM/renderer in `SharedPreferences`, and simulating/hooking into the async launch pipeline.
            """.trimIndent(),
        ),
        McLauncherBlueprint(
            id = "mc_mojang_downloader",
            badge = "JAVA CORE",
            title = "Mojang Manifest + SHA-1 Asset Downloader",
            stackLabel = "Java · ExecutorService · JSON · SHA-1",
            summary = "Fetches Mojang `version_manifest_v2.json`, parses libraries & asset indexes, and downloads game JAR + assets in parallel with SHA-1 verification.",
            prompt = """
                Write a complete, production-grade **Mojang Version Manifest & Parallel Asset/Library Downloader Engine** in **Java** for an Android Minecraft Launcher:
                1. `MojangManifestService.java`: Fetches and parses `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` (filterable by `release` / `snapshot`), then parses the selected `<version>.json` (`mainClass`, `javaVersion.majorVersion`, `arguments.game`, `arguments.jvm`, `downloads.client`, `assetIndex`, and `libraries[]` with Mojang `rules` evaluation).
                2. `ParallelAssetDownloader.java`: Multi-threaded (`ExecutorService` fixed pool of 8 threads) downloader for `client.jar`, Maven libraries, and `https://resources.download.minecraft.net/<2-hex>/<sha1>` assets with:
                   - Exact SHA-1 verification before & after download (skips already-valid local files!)
                   - Automatic retry with exponential backoff
                   - Real-time `DownloadProgressListener` (`onProgress(completedFiles, totalFiles, bytesDownloaded, currentFileName)`) marshaled safely to Android's `Handler(Looper.getMainLooper())`.
            """.trimIndent(),
        ),
        McLauncherBlueprint(
            id = "mc_auth_xbox",
            badge = "KOTLIN + XML",
            title = "Microsoft / Xbox Live / Offline Auth",
            stackLabel = "Kotlin · Coroutines · Android XML Dialog",
            summary = "Full Microsoft Device-Code / OAuth2 -> Xbox Live -> XSTS -> Minecraft Services token chain + OfflinePlayer UUID v3 generator & account XML dialog.",
            prompt = """
                Build a complete **Minecraft Launcher Account & Authentication Module** using **Kotlin** and **Android XML**:
                1. `MinecraftAuthManager.kt` using `Dispatchers.IO`:
                   - Full Microsoft OAuth2 Device-Code flow + token refresh -> Xbox Live (`https://user.auth.xboxlive.com/user/authenticate`) -> XSTS (`https://xsts.auth.xboxlive.com/xsts/authorize` for `rp://api.minecraftservices.com/`, handling XErr codes like `2148916233` / `2148916238`) -> `https://api.minecraftservices.com/authentication/login_with_xbox` -> `https://api.minecraftservices.com/minecraft/profile` (extracting `id` UUID, `name`, and skin URL).
                   - Offline account generator using deterministic `UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).toByteArray(Charsets.UTF_8))`.
                   - Encrypted/safe JSON persistence for multiple saved accounts.
                2. `res/layout/dialog_mc_accounts.xml` + `AccountDialogController.kt` for switching accounts, displaying the Microsoft Device Code with 1-tap Copy & Open Browser, or adding an Offline username.
            """.trimIndent(),
        ),
        McLauncherBlueprint(
            id = "mc_jvm_jni_bridge",
            badge = "JAVA + JNI + XML",
            title = "ARM64 JRE 8/17/21 + LWJGL3 Launch Bridge",
            stackLabel = "Java · Native C (`libjli.so`) · SurfaceView XML",
            summary = "Selects ARM64 OpenJDK 8/17/21, builds full Minecraft classpath & JVM flags, binds `SurfaceView` `ANativeWindow`, and invokes `JLI_Launch`.",
            prompt = """
                Create the **Android ARM64 JVM / JNI Launch & Renderer Bridge** for a Minecraft Java Edition Launcher using **Java**, **Android XML**, and **C JNI**:
                1. `JreRuntimeManager.java`: Detects and unpacks bundled/downloaded `arm64-v8a` OpenJDK `tar.xz` runtimes (`JRE 8` for <=1.16.5, `JRE 17` for 1.17–1.20.4, `JRE 21` for 1.20.5+/1.21+), sets executable permissions, and configures `JAVA_HOME`, `LD_LIBRARY_PATH`, and renderer env vars (`GL4ES` vs `Zink/Turnip`).
                2. `MinecraftCommandBuilder.java`: Builds the exact JVM argument array (`-Xms`, `-Xmx`, `-Djava.library.path`, `-Djna.nosys=true`, `-Dorg.lwjgl.util.Debug=true`, `-cp <libraries:client.jar>`, `mainClass`, `--username`, `--version`, `--gameDir`, `--assetsDir`, `--assetIndex`, `--uuid`, `--accessToken`, `--userType`).
                3. `GameSurfaceActivity.java` + `res/layout/activity_game_surface.xml` (`SurfaceView` + toggleable live log overlay) + `cpp/mc_jre_bridge.c` (`ANativeWindow_fromSurface`, `dlopen("libjli.so")`, `JLI_Launch` on a dedicated pthread, and stdout/stderr pipe redirect to `latestlog.txt`).
            """.trimIndent(),
        ),
        McLauncherBlueprint(
            id = "mc_modloader_modrinth",
            badge = "KOTLIN + JAVA + XML",
            title = "Fabric Installer + Modrinth Mod Browser",
            stackLabel = "Kotlin · Java · RecyclerView XML",
            summary = "1-tap Fabric Loader profile installer (`meta.fabricmc.net`) + horizontal Modrinth v2 Mod, Shader & ResourcePack search/download UI.",
            prompt = """
                Build a **Fabric Modloader Installer + In-Launcher Modrinth Mod/Shader Browser** using **Java, Kotlin, and Android XML**:
                1. `FabricInstallerService.java`: Queries `https://meta.fabricmc.net/v2/versions/loader/<mcVersion>`, downloads the merged Fabric profile JSON (`/profile/json`), merges Fabric Maven libraries (`https://maven.fabricmc.net/`) with the base vanilla version JSON, and writes the custom launch profile into `versions/<id>/<id>.json`.
                2. `ModrinthRepository.kt`: Queries `https://api.modrinth.com/v2/search` (filtering by `project_type:mod|shader|resourcepack`, Minecraft version, and `categories:fabric|forge|neoforge`) and downloads compatible `.jar`/`.zip` files directly into `<instance>/mods`, `shaderpacks`, or `resourcepacks`.
                3. `res/layout/fragment_mod_browser.xml` + `res/layout/item_modrinth_mod.xml` (`RecyclerView` 2-column horizontal landscape grid with icon, title, author, download count, and 1-tap `Install` button).
            """.trimIndent(),
        ),
        McLauncherBlueprint(
            id = "mc_touch_controls",
            badge = "JAVA + XML",
            title = "Custom Minecraft Touch + Mouse HUD Overlay",
            stackLabel = "Java · Custom View · Layout XML",
            summary = "Customizable landscape touch overlay with virtual WASD/joystick, virtual mouse trackpad + crosshair mode, hotbar 1–9, F3/Inventory/Chat/Pri-Sec buttons.",
            prompt = """
                Create a responsive **Minecraft Android Landscape Touch & Virtual Mouse Control Overlay** in **Java** and **Android XML**:
                1. `res/layout/overlay_mc_controls.xml`: Clean landscape gaming overlay (`FrameLayout`/`ConstraintLayout`) with left movement pad (WASD / sprint-lock), top utility pills (`ESC`, `F3`, `F5`, `Chat`, `KB`, `GUI`, `Mouse Lock`), right action buttons (`Attack/Break` LMB, `Place/Use` RMB, `Jump`, `Sneak` toggle), and centered bottom 9-slot Hotbar bar.
                2. `VirtualMouseTouchpadView.java`: Custom Java `View` handling relative trackpad pointer deltas when mouse-grabbed (`GLFW_CURSOR_DISABLED`) vs direct tap-to-click cursor coordinates when in inventory/GUI mode (`GLFW_CURSOR_NORMAL`), dispatching events through a clean `InputBridgeListener` interface.
                3. `ControlsEditorHelper.java`: Allows dragging, scaling, and adjusting button opacity (`0.2f..1.0f`) and saving custom control profiles as JSON!
            """.trimIndent(),
        ),
    )
}
