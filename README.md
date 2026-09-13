<div align="center">

![](https://repository-images.githubusercontent.com/728714946/42abb677-a9ff-45e6-820f-d517dc615ec2)

# Morpheus Launcher

**A next-generation command-line launcher for Minecraft: Java Edition.**

![Java](https://img.shields.io/badge/Java-8%2B-ED8B00?logo=openjdk\&logoColor=white)
![Platforms](https://img.shields.io/badge/platforms-Windows%20%7C%20macOS%20%7C%20Linux-4B5563)
![Architectures](https://img.shields.io/badge/architectures-x86%20%7C%20ARM%20%7C%20RISC--V-5856D6)
![Engineering Confidence: 8/10](https://img.shields.io/badge/Engineering%20Confidence-8%2F10-43A047)

</div>

---

## 📃 Description

**Morpheus Launcher** is a lightweight, cross-platform CLI launcher for Minecraft: Java Edition.

It supports both **dynamic class loading** for improved reliability and the traditional **classpath-based launch method** for maximum compatibility across Minecraft versions, mod loaders and Java runtimes.

The project is designed around portability, scalability and long-term compatibility while remaining fully usable from the command line.

## ⚡ Features

* ✅ **Vanilla Minecraft support**
* ✅ **Fabric support**
* ✅ **OptiFine support**
* ✅ **Forge support***
* ✅ **NeoForge support***
* ✅ **Automatic Fabric, Forge, NeoForge and OptiFine installation**
* ✅ **Windows, macOS and Linux**
* ✅ **x86, ARM® and RISC-V support**
* ✅ **Premium Microsoft accounts**
* ✅ **Ely.by accounts**
* ✅ **SP / offline accounts**
* ✅ **Java 8+ launcher runtime compatibility**
* ✅ **Dynamic class loading**
* ✅ **Traditional classpath launching**
* ✅ **Command-line operation**
* ✅ **Discord Rich Presence**
* ✅ **Lightweight runtime**
* ✅ **Scalable codebase**

> * Forge compatibility is currently limited to a subset of Minecraft and Forge versions.

## 🧠 Engineering confidence

**8/10 — Serious, manually engineered project**

Morpheus Launcher is primarily designed and written manually, with AI used occasionally as a development aid rather than as the primary implementation source.

AI-assisted changes are reviewed, understood and tested before being integrated into the project.

The launcher has been developed as a long-term project with deliberate architecture, compatibility work and extensive practical testing across different Minecraft versions, loaders, Java runtimes, operating systems and processor architectures.

The score reflects the **engineering process, code ownership, testing depth and long-term maintainability of the project**.

## 🧩 Compatibility

### Minecraft distributions

| Platform / Loader |   Support  |
| ----------------- | :--------: |
| Vanilla           |      ✅     |
| Fabric            |      ✅     |
| OptiFine          |      ✅     |
| Forge             | ⚠️ Partial |

Forge support currently covers only a subset of versions.

### Operating systems

| Operating System | Support |
| ---------------- | :-----: |
| Windows          |    ✅    |
| macOS            |    ✅    |
| Linux            |    ✅    |

### Architectures

Morpheus Launcher is designed to support multiple processor architectures, including:

* x86 / x86-64;
* ARM®;
* RISC-V.

Actual Minecraft compatibility can still depend on the Java runtime, native libraries and mods used by the selected game version.

### Accounts

Supported authentication modes include:

* **Microsoft / Premium**
* **Ely.by**
* **SP / offline accounts**

## 📘 Installation

For most users, the automatic installer is recommended.

### Automatic installation

[**Automatic installation for basic users →**](https://morpheus-launcher.gitbook.io/home/for-intel-and-arm/automatic-install)

### Manual installation

For users who prefer to configure the launcher manually:

[**Manual installation for advanced users →**](https://morpheus-launcher.gitbook.io/home/for-intel-and-arm/manual-install)

## ⬇️ Prebuilt binaries

The latest prebuilt CLI launcher can be downloaded here:

[**Download Morpheus Launcher →**](https://morpheuslauncher.it/downloads/Launcher.jar)

## ⚙️ Compiling from source

### Requirements

To compile Morpheus Launcher you need:

* **JDK 8**

  * Azul Zulu 8 is highly recommended;
* **Apache Maven 3.x**;
* **Git**

  * optional if the repository has already been downloaded;
* `lib/discord-activity4j.jar` in the project directory.

> [!NOTE]
> Morpheus Launcher itself is compiled targeting **Java 8** and the resulting JAR is compatible with **Java 8 and newer runtimes**.
>
> The Java version required to actually run Minecraft depends on the selected Minecraft version.

### Verify your Java environment

Before compiling, make sure Maven is using the correct JDK:

```bash
java -version
javac -version
mvn -version
```

For the recommended build environment, all relevant Java commands should resolve to a JDK 8 installation.

### Build

Clone the repository:

```bash
git clone <repository-url>
cd Morpheus-Launcher
```

Then compile it with Maven:

```bash
mvn clean package
```

Make sure `lib/discord-activity4j.jar` is available before starting the build.

## ☕ Java compatibility

The launcher itself targets **Java 8+**, but Minecraft versions have their own runtime requirements.

For example, newer Minecraft releases may require newer Java runtimes even though Morpheus Launcher itself can still run on Java 8.

The launcher runtime and the Minecraft runtime should therefore be treated as separate requirements.

## 🎮 Launch architecture

Morpheus Launcher supports two primary approaches when starting Minecraft:

```text
Minecraft version
       ↓
Version / loader resolution
       ↓
Dependency resolution
       ↓
┌──────────────────────────────┐
│ Dynamic class loading       │
│ or                          │
│ Traditional classpath       │
└──────────────────────────────┘
       ↓
Minecraft
```

Dynamic loading is intended to improve reliability where possible, while traditional classpath launching remains available for compatibility with versions or loaders that expect the conventional launch model.

## 💬 Discord Rich Presence

Morpheus Launcher can expose the current launcher or game state through Discord Rich Presence.

The integration uses **DiscordActivity4J**.

The required library must be available at:

```text
lib/discord-activity4j.jar
```

when compiling the project from source.

## 📚 Documentation

More detailed installation and usage documentation is available through the official launcher wiki:

[**Morpheus Launcher Wiki →**](https://morpheus-launcher.gitbook.io/home/)

## 📣 Community

Need help, found a bug or want to discuss the project?

* **Wiki:** https://morpheus-launcher.gitbook.io/home/
* **Discord:** https://discord.com/invite/aerXnBe

## ⚖️ Open-source licensing

This project is distributed under **Creative Commons BY-NC-SA** terms.

In short:

* **BY — Attribution**
  The original author of this software must remain credited.

* **NC — NonCommercial**
  The project may not be used for commercial purposes.

* **SA — ShareAlike**
  Adaptations must be distributed under the same terms as the original work.

> [!IMPORTANT]
> Review the full license terms before redistributing or modifying the project.

## Warranty

> [!CAUTION]
> This software is provided **as-is**, without warranty of any kind.
>
> The author is not responsible for damage, data loss, incompatibilities or other issues resulting from use of the software.

---

<div align="center">

**Built to launch Minecraft anywhere Java can go.**

</div>
