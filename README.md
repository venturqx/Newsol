# Newsol

Newsol is a fork of NewPipe that adds Tournesol-powered recommendations. It is
not affiliated with or endorsed by the NewPipe project.

## Status

- Community fork for experimentation and feature work.
- Not an official NewPipe release.

## Features

- Stream browsing, playback, and background audio.
- Tournesol recommendations for supported services.
- Local subscriptions, playlists, and history.

## Build

1) Clone with submodules:

```bash
git clone --recurse-submodules <your-repo-url>
```

2) Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

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
