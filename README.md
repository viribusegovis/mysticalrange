# Mystical Range

A client-side NeoForge mod for Minecraft 26.1.2. It adds a button to the Mystical Agriculture
**Harvester** and Mystical Automation **Fertilizer** screens that outlines the area the machine
works on. The full feature description is in [modrinth-description.md](modrinth-description.md),
which is also the Modrinth project page.

## Building

Needs a Java 25 JDK. `gradle.properties` points Gradle at the one Prism Launcher installs
(`org.gradle.java.installations.paths`); change that path to point at your own JDK.

```bash
./gradlew build
```

The jar lands in `build/libs/`.

Mystical Agriculture and Cucumber aren't published on a Maven repository we can reach, so the
build compiles against their jars in `libs/`. Those jars aren't committed; download them from
CurseForge into `libs/` before building:

- [MysticalAgriculture-26.1.2-9.0.9.jar](https://www.curseforge.com/minecraft/mc-mods/mystical-agriculture/files/8797046)
- [Cucumber-26.1.2-9.0.6.jar](https://www.curseforge.com/minecraft/mc-mods/cucumber/files/8796937)

To build against other releases, use those jars instead and update `mysticalagriculture_jar` /
`cucumber_jar` in `gradle.properties`.

## Publishing

`./gradlew modrinth --no-configuration-cache` uploads the jar as a new Modrinth version. The token
comes from `modrinthToken` in `~/.gradle/gradle.properties` or the `MODRINTH_TOKEN` environment
variable; it is never stored in this repository.

## License

MIT
