# Ufonirpt

Unofficial fork of NewPipe integrating recommendations powered by Tournesol.
This application is not affiliated with or endorsed by the NewPipe project nor the Tournesol project.

![Screenshots](assets/ufonirpt-screens.png)

## Build

1) Clone with submodules:

```bash
git clone --recurse-submodules https://github.com/venturqx/ufonirpt.git
```

2) Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

Note: Gradle - JDK Eclipse Temurin 17

## Submodules

This repo vendors the extractor as a git submodule at `./NewPipeExtractor`.
After cloning, run:

```bash
git submodule update --init --recursive
```

## Fork and attribution

This project is a fork of NewPipe and remains licensed under GPL-3.0-or-later.
It uses a fork of NewPipeExtractor and the Tournesol API (subject to its terms
of service). See `NOTICE` for details.

## License

Licensed under GPL-3.0-or-later. See `LICENSE`.
