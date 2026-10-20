# HuntLike

A boss-hunting mod for Minecraft Java Edition 26.3 (Fabric), inspired by Hypixel Skyblock's slayer quests.

## Build and test

Requirements: Java 25 (JDK).

- `gradlew runClient` – opens a test copy of Minecraft with the mod loaded.
- `gradlew build` – builds the mod; the jar is in `build/libs/`.

Every push to GitHub is also built automatically (Actions tab → latest run → Artifacts).

## Automated checks

Every push runs three checks on GitHub:

1. Build the mod.
2. Start a dedicated server with the mod.
3. Start a real client (`gradlew runClientGameTest`) that plays a Tier I Animal quest end to end and saves screenshots of every menu screen.

Each run pushes its logs and screenshots to the `ci-log` branch.

## Test command (operators)

- `/huntlike xp add <amount>` – add Animal Hunt XP.
- `/huntlike tokens add <amount>` – add Animal Ritual Tokens.
- `/huntlike reset` – wipe your HuntLike progress.
