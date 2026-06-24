# Time Engine

Minimal Minecraft 1.21.1 mod prototype using NeoForge and Java 21.

## Development

- Build: `./gradlew build`
- Client: `./gradlew runClient`
- Dedicated server: `./gradlew runServer`
- Format Java sources: `./gradlew spotlessApply`
- Verify formatting and tests: `./gradlew check`

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Configuration

NeoForge generates the configuration files after the first launch:

- `config/time_engine-common.toml` controls diagnostic logging.
- `<world>/serverconfig/time_engine-server.toml` controls sessions, snapshots, networking, phantom combat, afterimages, and temporal intercept limits.

Set `diagnosticLogging = true` to enable Time Engine lifecycle logs. Warnings and errors remain enabled regardless of this flag.

## Debug commands

The commands require permission level 2:

- `/timeengine session` shows the executing player's session, server tick, perceived tick, cooldown, and active session count.
- `/timeengine snapshots <player>` shows the target's buffer usage, latest snapshot tick, and whether an interpolated snapshot exists at the executing player's perceived tick.
- `/timeengine intercepts` shows temporal blocks and successfully intercepted targets for the executing player's active session.
- `/timeengine policies` shows loaded/rejected policy counts and the current reload generation.
- `/timeengine policies entity <entity>` explains the effective entity policy and its source.
- `/timeengine policies block <x> <y> <z>` explains the effective block policy and its source.
- `/timeengine policies reload` performs a full datapack reload and reports policy results.
- `/timeengine config` opens the live server configuration panel for an authorized player.

## Temporal policies

Datapacks can extend or override temporal behavior without rebuilding the mod. A datapack is not
required: when no policy matches, Time Engine preserves its built-in behavior.

### Creating a datapack

Create this directory structure inside the target world's `datapacks` directory:

```text
<world>/datapacks/my_time_engine_policies/
├── pack.mcmeta
└── data/
    └── my_pack/
        └── time_engine/
            └── temporal_policies/
                ├── armor_stand.json
                └── complex_machine.json
```

`my_pack` is an example namespace and may be replaced with a unique lowercase namespace. The
`time_engine/temporal_policies` portion is fixed and must not be renamed.

For Minecraft 1.21.1, `pack.mcmeta` can contain:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Custom Time Engine temporal policies"
  }
}
```

Policy files belong at
`data/<namespace>/time_engine/temporal_policies/<policy_name>.json`. The resulting policy resource
id is `<namespace>:<policy_name>`.

The datapack may remain as a directory or be zipped. When zipped, `pack.mcmeta` and `data` must be
at the root of the archive. Install it in `<world>/datapacks`, then run:

```text
/timeengine policies reload
```

This performs a full server datapack reload. A server restart is not required.

### Policy format

Every policy contains:

- `target`: `entity` or `block`.
- `priority`: optional integer; defaults to `0` and higher values win.
- `ids`: optional array of registry ids such as `minecraft:zombie`.
- `tags`: optional array of registry tag ids without a leading `#`.
- `operations`: the operations this policy explicitly overrides.

At least one `ids` or `tags` selector and at least one operation are required. IDs and tags in the
same policy are combined with OR: matching any selector is sufficient.

Entity operations:

- `snapshot`: include or exclude the entity from historical snapshot tracking.
- `phantom_combat`: allow or reject server-authoritative phantom attacks.
- `temporal_intercept`: allow or exclude the entity from Temporal Intercept correction.

Block operations:

- `temporal_intercept`: allow or ignore the block as a temporal obstacle.
- `interaction`: control interaction with a matching block placed during an active temporal
  session.

Supported decisions:

- `allow`: allow the operation.
- `ignore`: exclude the target from snapshot, phantom combat or Temporal Intercept processing.
- `lock_interaction`: temporarily prevent using or breaking a recorded temporal block. This value
  is valid only for the block `interaction` operation.

The block `interaction` operation accepts only `allow` and `lock_interaction`. `allow` explicitly
leaves interaction unlocked and can override a lower-priority lock rule. `ignore` is rejected for
this operation because it would otherwise be indistinguishable from `allow`.

Operations omitted from a policy continue searching lower-priority matching policies. If no rule
defines that operation, the built-in fallback is used.

### Entity example

`data/my_pack/time_engine/temporal_policies/armor_stand.json`:

```json
{
  "target": "entity",
  "priority": 100,
  "ids": ["minecraft:armor_stand"],
  "tags": ["my_pack:temporal_immune"],
  "operations": {
    "snapshot": "allow",
    "phantom_combat": "ignore",
    "temporal_intercept": "ignore"
  }
}
```

This explicitly enables snapshots for armor stands while disabling phantom damage and Temporal
Intercept correction for them.

### Block example

`data/my_pack/time_engine/temporal_policies/complex_machine.json`:

```json
{
  "target": "block",
  "priority": 100,
  "ids": ["example:complex_machine"],
  "operations": {
    "temporal_intercept": "ignore",
    "interaction": "lock_interaction"
  }
}
```

This prevents a complex machine from acting as a Temporal Intercept obstacle and locks interaction
with that particular recorded placement while the temporal state exists. It does not globally lock
all machines of that type during ordinary gameplay.

### Defining selector tags

Policies can reference existing vanilla/mod tags or tags supplied by the same datapack. Minecraft
1.21.1 uses these registry tag paths:

```text
data/<namespace>/tags/entity_type/<tag_name>.json
data/<namespace>/tags/block/<tag_name>.json
```

For example, `data/my_pack/tags/entity_type/temporal_immune.json`:

```json
{
  "replace": false,
  "values": [
    "minecraft:armor_stand",
    "example_mod:training_dummy"
  ]
}
```

Reference it from a policy as `"my_pack:temporal_immune"`.

### Priority and fallback behavior

Matching rules are evaluated from highest priority to lowest. Equal priorities are resolved by
policy resource id, making the result deterministic. A rule overrides only the operations it lists;
it does not replace the whole Time Engine configuration.

Without a matching policy, current defaults remain active:

- solid vanilla and modded blocks are eligible Temporal Intercept obstacles;
- players, `Mob` subclasses and `Projectile` subclasses use snapshot tracking defaults;
- unknown custom entity families remain excluded until explicitly enabled;
- server safety checks for dead entities, passengers, vehicles, distance and collision remain
  authoritative and cannot be bypassed by `allow`.

Invalid policy files are logged and skipped. Valid policies from the same reload are published
together, so gameplay never observes a partially constructed policy set.

After a successful policy reload, Time Engine clears snapshot history and active Temporal Intercept
placement state. This prevents ghost frames or obstacle records created under the previous rules
from surviving the reload. Active temporal sessions remain active; snapshot history begins filling
again on the next server tick.

### Diagnostics

Use these commands after installation or reload:

```text
/timeengine policies
/timeengine policies entity <entity>
/timeengine policies block <x> <y> <z>
```

The target commands show the effective decision and either the policy resource id or `fallback`.
Warnings for malformed policies are always written to the server log; detailed lifecycle messages
also require `diagnosticLogging = true`.
