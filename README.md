# Gravely

Gravely places a grave when you die and keeps your inventory and experience there for recovery. It also handles Curios accessories and items stored in Sophisticated Backpacks.

## Features

- A grave holds your items and experience until you recover them.
- Grave access can be owner-only, time-limited, or unrestricted in `config/gravely-common.toml`.
- Graves can be placed underwater and search nearby for a safe position when the death location cannot hold a block.
- Curios accessories can be stored and re-equipped when recovered. Sophisticated Backpacks retain their contents.
- With JourneyMap installed on the client, a grave waypoint is added at death and removed when the grave is recovered.
- The `gravely:soulbound` enchantment keeps enchanted equipment with you after death instead of placing it in the grave.

## Install

Install Minecraft 1.21.1 with NeoForge 21.1.227 or newer, then place the Gravely JAR in `mods/`. Curios API is optional for accessory support. For multiplayer, install Gravely on the server and clients; JourneyMap integration runs on the client.

## Configuration

The default protection mode is `OWNER_ONLY`. `TIMED` releases ownership after the configured duration, while `NONE` allows immediate access. Other options control grave placement, experience storage, notifications, and whether recovery removes the grave block.

## License

Gravely is [All Rights Reserved](LICENSE). Modpack inclusion is allowed without permission or credit.

## Downloads and support

For bugs and questions, DM [@kuronami333 on X](https://x.com/kuronami333).

[Source](https://github.com/KURONAMI333/gravely) · [License](LICENSE)
