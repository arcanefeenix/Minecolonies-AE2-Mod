# Applied Colonies

Integration mod for **Applied Energistics 2** and **Minecolonies**. Your colonists request items; your ME network delivers them.

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.256+ |
| Applied Energistics 2 | 19.2.12+ (`<20`) |
| Minecolonies | 1.1.1400+ |

## Installation

Drop `appliedcolonies-1.0.0.jar` into your `mods/` folder alongside NeoForge, AE2, and Minecolonies. Required on both client and server.

## Usage

1. Craft the Bridge: 4 stripped oak logs, 3 logic processors, 1 ME Interface, 1 Minecolonies Clipboard.
2. Place it in your colony and connect it to your ME network.
3. Optional: insert an AE2 Crafting Card in its upgrade slot to enable autocrafting.

The Bridge watches for colonist item requests and fulfills them from your network — pulling existing items, or (with a card installed) kicking off AE2 autocrafts for items you don't have.

## Delivery modes

Controlled by `requireCourier` in `config/AppliedColonies/general.toml`:

- **`true` (default)** — items land in the Bridge's rack and a courier delivers them via the warehouse. Requires a built Warehouse with a hired courier.
- **`false`** — items are extracted from AE and inserted directly into the requester's building rack. No courier needed.

## Configuration

| Option | Default | Description |
| --- | --- | --- |
| `requireCourier` | `true` | See delivery modes above. |
| `enableDebugMode` | `false` | Verbose debug logging. |

## Building from source

```bash
./gradlew build
```

Requires JDK 21. Output jar lands in `build/libs/`.

## License

MIT — see [LICENSE](LICENSE).

## Issues

[github.com/arcanefeenix/Minecolonies-AE2-Mod/issues](https://github.com/arcanefeenix/Minecolonies-AE2-Mod/issues)
