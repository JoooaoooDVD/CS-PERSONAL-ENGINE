<div align="center">

  <img src="assets/readme/logo.svg" alt="CS Personal Engine Logo" width="104" height="104" style="border-radius: 24px;" />

  # CS Personal Engine

  ### *Autonomous Personal AI Engineering Workspace for Android (`v2.0.0`)*

  **Stream live AI reasoning & code generation, auto-execute Linux commands, push directly to GitHub, connect any OpenAI-compatible or custom gateway, and build full web/Python/C++/Android projects directly on your phone.**

  <br />

  [![Release v2.0.0](https://img.shields.io/badge/Release-v2.0.0-38BDF8?style=flat-square&logo=github&logoColor=white)](.github/workflows/android-build.yml)
  [![Android 9+](https://img.shields.io/badge/Android-9%2B-34D399?style=flat-square&logo=android&logoColor=white)](#system-requirements)
  [![ARM64](https://img.shields.io/badge/CPU-ARM64-818CF8?style=flat-square)](#system-requirements)
  [![MIT License](https://img.shields.io/badge/License-MIT-E2E8F0?style=flat-square)](LICENSE)

</div>

---

## Key Capabilities in CS Personal Engine `v2.0.0`

### 1. Live Streaming Visibility & Auto-Command Execution
- **Live Engine Telemetry HUD** directly above the chat composer: watch real-time token streaming (`▊`), active tool invocations, shell commands, and file writes as the agent works.
- **Auto-Expanded Active Steps**: currently running tool/command blocks expand automatically inline while executing.
- **1-Tap `▶ Run` & `Auto-Exec` Mode**: tap **`▶ Run`** on any shell snippet in chat to execute it immediately inside your Ubuntu PRoot workspace, or toggle **`Auto-Exec`** to automatically run shell blocks emitted by the AI (with destructive-command confirmation protection).
- **`⚡ Fix with AI` in Terminal**: when any Linux terminal command exits with a non-zero code, tap **`⚡ Fix with AI`** to send the command, exit code, and console tail straight to the active coding agent.

### 2. Full Git & GitHub Push Studio (Agents + Terminal + UI)
- **Visual Git & GitHub Push Studio** (`Push` / `Git (N)` button in the workspace top bar):
  - **Dual Authentication**: paste a **GitHub Personal Access Token** (`ghp_…` / `github_pat_…` with `repo` & `workflow` scopes — stored encrypted in Android Keystore) for instant 1-second setup even in offline builds, **or** use **Web OAuth Device Login** (`gh auth login --scopes repo,workflow,read:org,user:email` + `gh auth setup-git`).
  - **Zero-Prompt Non-Interactive Push**: `/opt/pocket/git-askpass.sh` + `GIT_TERMINAL_PROMPT=0` + `GITHUB_TOKEN`/`GH_TOKEN` + automatic Git author name/noreply email injection ensure `git commit`, `git push`, `git pull`, and `gh` CLI work out-of-the-box in **Claude Code**, **DeepSeek Harness**, **Antigravity**, and the **Project Terminal**.
  - **1-Tap Create Repo & Push**: create a brand-new Private or Public GitHub repository and push your entire workspace in one tap.

### 3. OpenAI-Compatible & Custom Universal Providers
- **Dedicated `OpenAI-Compatible API` Provider** + **`Custom Universal API`**:
  - **1-Tap Quick Presets**: OpenAI, Groq, Together AI, Mistral / Codestral, Fireworks AI, Cerebras, xAI Grok, Ollama (Local), and LM Studio / vLLM.
  - **Multi-Protocol Translation Engine (`LocalFormatGateway` v2)**: translates Anthropic `/v1/messages` ↔ OpenAI Chat Completions (`/v1/chat/completions` with real-time SSE streaming of `thinking_delta`, `text_delta`, and `input_json_delta` tool calls) and OpenAI Responses (`/v1/responses`) so **Claude Code** and **DeepSeek Harness** can run against **any** OpenAI-compatible or Anthropic-compatible model.
  - **Keyless Local Server Support**: leave the API key blank when connecting to local Ollama, LM Studio, llama.cpp, or vLLM instances.
  - **Custom HTTP Headers**: attach custom `Header: Value` pairs (Cloudflare Access, LiteLLM proxy headers, enterprise gateways) to model discovery, validation, and runtime requests.

### 4. Obsidian Dark-Grey & AMOLED Pitch-Black UI + Engine Power Tools
- **Obsidian Dark-Grey (`#090A0D` / `#111318`)** & **AMOLED Pitch-Black (`#000000` / `#0B0D11`)** themes with Electric Cyan (`#38BDF8`) & Emerald (`#34D399`) accents, animated orbital core telemetry (`AnimatedEngineOrb`), and spring-physics press interactions (`enginePressScale`).
- **Live Workspace Vision (`<live_workspace_context>`)**: automatically attaches your open editor file, Git branch/changes, recent failed terminal output, and Android build issues to prompts.
- **Persistent Personal Engine Rules**: define global coding preferences & system instructions in **Settings → Engine Automation & GitHub Push**.
- **`/` Slash Workflow Palette**: type `/` in chat for instant `/push`, `/fix`, `/run`, `/review`, `/test`, and `/ci` engineering workflows.
- **Live Dev-Server Port Detector**: automatically detects listening dev ports (`3000`, `5173`, `8000`, `8080`, etc.) and surfaces a 1-tap **Live Server Active** preview launcher.

---

## Building with GitHub Actions CI/CD

This repository includes a complete CI/CD pipeline at [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml):

1. Push to `main`/`master`, open a Pull Request, push a `v*` tag, or trigger **Actions → CS Personal Engine — Android CI & Release Build → Run workflow**.
2. The workflow automatically:
   - Checks out native submodules (`third_party/proot`, `third_party/libandroid-shmem`),
   - Installs JDK 17, Android SDK 36, Build Tools 35.0.0, CMake 3.22.1, and NDK `28.2.13676358`,
   - Fetches and SHA-256-verifies the ARM64 agent runtime bundle,
   - Runs `./gradlew testOnlineDebugUnitTest`,
   - Signs and builds **`CS-Personal-Engine-*-arm64-online-release.apk`** and **`CS-Personal-Engine-*-arm64-online-debug.apk`** (plus `SHA256SUMS.txt`), uploading them as downloadable GitHub Actions artifacts and publishing GitHub Releases on `v*` tags.

### Optional Custom Release Signing Secrets
Out of the box, CI generates a signing key when secrets are absent so every run produces installable APKs immediately. For persistent production signing across upgrades, add these GitHub Repository Secrets:
- `CS_KEYSTORE_BASE64` — `base64 -w0 your-release-key.jks`
- `CS_KEYSTORE_PASSWORD` — Keystore password
- `CS_KEY_ALIAS` — Key alias
- `CS_KEY_PASSWORD` — Key password

---

## Local Build & Test

```bash
git submodule update --init --recursive
./scripts/runtime-bundles/build-agy-from-official.sh
./gradlew testOnlineDebugUnitTest assembleOnlineDebug
```

## License

Released under the [MIT License](LICENSE).
