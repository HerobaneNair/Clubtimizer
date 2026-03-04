This mod is made for datapack developers to make bots that can fight back

the bots have more correct behaviour than carpet bots with many fixes

<details>
<summary>Commands</summary>

<details>
<summary><code>/distance</code></summary>

Measures distance between two positions or entities and stores the result as the command return value (scoreboard-friendly).

- Supports position to position, entity to position, position to entity, and entity to entity
- Default distance is spherical (3D)
- Optional exponent scaling for high precision scoreboard storage

**Syntax**
- `/distance from <pos> to <pos>`
- `/distance from <pos> to <entity>`
- `/distance from <entity> to <pos>`
- `/distance from <entity> to <entity>`

**Exponent Scaling**
- `/distance ... e <exp>`
- Multiplies the distance by 10^exp before returning
- Example: 4 blocks with e 3 returns 4000

**Horizontal and Vertical Modes**
- `/distance ... horizontal`
  - Measures XZ distance only
- `/distance ... vertical`
  - Measures Y distance only
- Both support e <exp>

</details>

<details>
<summary><code>/player</code></summary>

Controls fake players and real players (for look commands) using Carpet-style action packs.

Only fake players can be targeted for most actions.

**Targeting**
- `/player <targets> ...`
- Supports selectors and multiple targets

### Actions
- `use`, `attack`, `swing`, `jump`, `drop`, `dropStack`, `swapHands`
- Modes:
  - `once`
  - `continuous`
  - `interval <ticks>`
- Running the action name alone stops that action

### Movement
- `/player <targets> move`
  - Stops all movement
- Directions:
  - `forward`, `backward`, `left`, `right`

### Sneaking and Sprinting
- `sneak` and `unsneak`
- `sprint` and `unsprint`

### Turning
- `turn left`, `turn right`, `turn back`
- `turn <rotation>`

### Looking
- Cardinal directions:
  - `north`, `south`, `east`, `west`, `up`, `down`
- Look at position:
  - `look at <pos>`
- Look at entity:
  - `look upon <entity>`
  - Modes:
    - `eyes` (default)
    - `feet`
    - `closest` (closest hitbox point)

### Hotbar
- `/player <targets> hotbar <slot>`
  - Slots 1 to 9

### Item Cooldowns
- `/player <targets> itemCd`
  - Resets all cooldowns
- `/player <targets> itemCd <item>`
  - Shows remaining cooldown
- `/player <targets> itemCd <item> reset`
- `/player <targets> itemCd <item> set`
- `/player <targets> itemCd <item> set <ticks>`

### Mounting
- `mount`
- `mount anything`
- `dismount`

### Misc
- `stop` stops all actions
- `kill` kills fake players
- `disconnect` disconnects fake players without death

</details>

<details>
<summary><code>/playerspawn</code></summary>

Spawns a fake player with optional position, rotation, gamemode, and dimension.

> Replaces `/player <name> spawn` from carpet/carpetpvp [pre 1.4 for carpet pvp]

**Basic**
- `/playerspawn <name>`

**Position**
- `/playerspawn <name> at <pos>`

**Facing**
- `/playerspawn <name> at <pos> facing <rotation>`
- `/playerspawn <name> at <pos> facing <cardinal>`
  - `north`, `south`, `east`, `west`, `up`, `down`

**Gamemode**
- `/playerspawn <name> ... in <gamemode>`
- Defaults to creative

**Dimension**
- `/playerspawn <name> ... on <dimension>`

**Notes**
- Prevents duplicate names
- Respects whitelist and bans
- Spectators spawn flying
- Survival players spawn grounded

</details>

<details>
<summary><code>/herobot</code></summary>

Views and modifies HeRoBot settings.

**Usage**
- `/herobot <setting>`
  - Displays the current value and description of the setting
- `/herobot <setting> <value>`
  - Sets the setting temporarily for this session
- `/herobot <setting> <value> <perm>`
  - Sets the setting permanently (saved to config)

<details>
<summary><code>allowListingFakePlayers</code></summary>

Allows fake players to appear in the multiplayer player list.

Default: true

</details>

<details>
<summary><code>allowSpawningOfflinePlayers</code></summary>

Spawn offline players in online mode if an online-mode player with the specified name does not exist.

Default: true

</details>

<details>
<summary><code>creativeFlyDrag</code></summary>

Changes creative air drag.

- Default value: 0.09
- Lower values reduce air resistance while flying

</details>

<details>
<summary><code>creativeFlySpeed</code></summary>

Changes the creative flying speed multiplier.

- Default value: 1.0
- Higher values make the client fly faster

</details>

<details>
<summary><code>creativeNoClip</code></summary>

Creative no clip mode. Allows creative players who are flying to phase through blocks.

Default: false

</details>

<details>
<summary><code>editablePlayerNbt</code></summary>

Allows editing player NBT data directly.

Default: false

</details>

<details>
<summary><code>explosionNoBlockDamage</code></summary>

Controls whether explosions destroy blocks.

Values:
- FALSE: vanilla behavior
- MOST: affects non-solid blocks except glowstone and redstone-interaction blocks
- TRUE: prevents all block damage

Default: FALSE

</details>

<details>
<summary><code>explosionNoFire</code></summary>

Prevents explosions from beds and respawn anchors from creating fire.

Default: false

</details>

<details>
<summary><code>shieldStunning</code></summary>

Enables shield stunning, allowing entities to be damaged immediately after a shield is disabled.

Default: false

</details>

<details>
<summary><code>smoothClientAnimations</code></summary>

Smooths client animations when the server TPS is low.

Default: false

</details>

<details>
<summary><code>xpNoCooldown</code></summary>

Players absorb experience instantly without delay.

Default: false

</details>

</details>

</details>


<details>
<summary>Differences from Carpet Mod</summary>
  
<details>
<summary>Fixes</summary>

- Fixed gliding to work with `/player jump` while in midair

- Fixed fall distance not updating correctly (fixes crit and mace behavior)

- Fixed downward motion not being saved in some cases

- Fixed `/player look` yaw/pitch being reversed

- Fixed multiple edge cases related to fake player movement and actions

</details>

<details>
<summary>Fake Player Changes</summary>

- Added a toggle to allow/disallow shield stunning for players

- Added target selector support to player commands

- Renamed `/player <name> spawn` to `/playerspawn <name>`

  - now it is `/playerspawn <name> [at <coords> [in <gamemode> [on <world>]]]` 


- Added look mode options to `/player <target> look upon <entity> [eyes|feet|closest]`

  - `eyes` - looks at the entity’s eyes (default)

  - `feet` - looks at the entity’s feet

  - `closest` - looks at the closest point of the entity’s hitbox

- Added `itemCd` command for item cooldown control

  - `itemCd` - resets all item cooldowns [returns how many items were reset]

  - `itemCd <item>` - shows how many ticks left on the item's cooldown, says ready if no cooldown [returns how many ticks left]

  - `itemCd <item> reset` - resets cooldown [returns how many players were targeted]

  - `itemCd <item> set` - applies the default item cooldown without using it [returns how many players were targeted]. If there is no default item cooldown, will not do anything [for example cobblestone]

  - `itemCd <item> set <ticks>` - applies a custom total cooldown duration without using it [returns how many players were targeted]

- Added swing animation command for fake players

  - Swings a hand without performing an action

  - Resets attack cooldown

  - No parameters stop any ongoing swinging

- Changed `/player use` (no parameters) to stop using without triggering another use

  - Previously behaved the same as `/player use once`

</details>

<details>
<summary>Distance Command Changes</summary>

- Changed `/distance` to store spherical distance values into scoreboards

- Added `e` argument to `/distance` for higher scale

  - for example: `/distance from HerobaneNair to TheobaldTheBot e 4` while we're 4 blocks away will return `4000` to be stored in a scoreboard as needed

- Added support for measuring distance between two entities

- Added horizontal and vertical distance measurement:

  - `/distance from <from> to <to> horizontal|vertical`

  - Allows measuring horizontal or vertical distance between entities or positions

</details>

<details>
<summary>Carpet Rule Changes</summary>

- Added carpet rule for shield stunning 

- Added carpet rule for editable player NBT

  - `/carpet editablePlayerNbt`

- Added carpet rule to disable fire from anchors and beds

  - `/carpet explosionNoFire`

- Changed explosion block damage rule behavior:

  - `false` - vanilla behavior

  - `most` - affects non-solid blocks except glowstone and redstone-redirecting blocks (buttons, levers, etc.)

  - `true` - prevents all block damage

</details>

</details>
