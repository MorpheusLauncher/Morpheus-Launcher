![](https://repository-images.githubusercontent.com/728714946/42abb677-a9ff-45e6-820f-d517dc615ec2)

# 📃 Description
Next-gen CLI launcher for minecraft java edition, it features dynamic classloading (for reliability) and traditional classpath (for compatibility)

# ⚡ Features
- Compatible with vanilla, optifine, fabric, forge*
- Automatic fabric, forge and optifine installation
- Compatible with windows, macos, linux
- Support for Arm® and Risc-V processors
- Support for Premium, Ely.by and SP accounts
- Compatible with java 8+ runtimes
- Command-line operation
- Discord rich-presence
- Highly scalable code
- Lightweight

*currently Forge compatibility is limited to a subset amount of versions

# 📘 Installation
[**Automatic installation (for basic users) [recommended]**](https://morpheus-launcher.gitbook.io/home/for-intel-and-arm/automatic-install)

[**Manual installation (for advanced users)**](https://morpheus-launcher.gitbook.io/home/for-intel-and-arm/manual-install)

# ⬇️ Prebuilt binaries
[**Download Launcher (cli)**](https://morpheuslauncher.it/downloads/Launcher.jar)

# ⚙️ Compiling from source code

## Requirements

To compile Morpheus Launcher you need:

- **JDK 8** (Java Development Kit)
    - Azul zulu 8 is highly recommended
- **Apache Maven 3.x**
- **Git** (optional, required only if cloning the repository)
- `lib/discord-activity4j.jar` must be present in the project directory

> Morpheus Launcher is compiled targeting Java 8 and the resulting JAR is compatible with Java 8+.
>
> The Java version required to run Minecraft itself may vary depending on the selected Minecraft version.

### Check your Java version

Before compiling, make sure Maven is actually running with JDK 8:

```bash
java -version
javac -version
mvn -version
```

# 📣 Need more help?
- Official launcher wiki: https://morpheus-launcher.gitbook.io/home/
- Join my discord: https://discord.com/invite/aerXnBe

# ⚖️ Opensource licensing
Follow Creative Commons BY-NC-SA:
- BY: you should keep original author of this software mentioned
- Non-commercial: you cannot use this project for any commercial use
- Share-Alike: Adaptations must be shared under the same conditions as the source material

**Warranty: this software is provided as-is without any warranty and nobody is responsible for any damage**
