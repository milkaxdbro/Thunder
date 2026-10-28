<div align="center">
  <h1>Thunder</h1>
  <h1>Главный кодер</h1>
  <h1>Claude Opus 5.5</h1>
  <br>
</div>

Thunder — античит для Minecraft с русской локализацией из коробки (форк GrimAC).
Поддерживаются версии Minecraft 1.19–26.3. Игроки Geyser полностью освобождены от проверок во избежание ложных срабатываний.
Команды: `/thunder` (алиас `/th`), права: `thunder.*` (например `thunder.alerts`, `thunder.exempt`).
This project is considered feature-complete for the 2.0 (open-source) branch. If you would like a bug fix or enhancement and cannot sponsor the work, pull requests are welcome.
A premium version is planned, which will offer additional subscription-based paid checks, such as heuristics.

## Downloads

- Скачивание: [GitHub Releases](https://github.com/milkaxdbro/Thunder/releases) и Telegram [@froriym81](https://t.me/froriym81).

## Requirements & Installation

- Java 17 or higher.
- A Paper or Purpur server environment, Minecraft 1.19 – 26.3.

If you use a proxy such as Velocity or BungeeCord:
- If you use Geyser, Floodgate must be installed on the backend server (where Thunder is) so Thunder can access the Floodgate API.
- If you use ViaVersion, it must be installed on the backend server (where Thunder is) ONLY.
  Thunder does not support having ViaVersion installed on the proxy, even if it is also installed on the backend.

## Resources

- Поддержка и вопросы: Telegram [@froriym81](https://t.me/froriym81).

## Pull Requests

See [Contributing](CONTRIBUTING.md) for more information about contributing and what our guidelines
are.

## Developer Plugin API

Thunder's plugin API allows you to integrate Thunder into your own plugins. Visit
the [plugin API repository](https://github.com/GrimAnticheat/GrimAPI) for the source code and more
information.

## Compiling From Source

1. `git clone https://github.com/Axionize/LightningThunder`
2. `cd LightningThunder`
3. `./gradlew build`
4. The final jars will compile into the `<platform>/build/libs` folders

## Thunder Supremacy

What makes Thunder stand out against other anticheats?

### Movement Simulation Engine

* We have a 1:1 replication of the player's possible movements
    * This covers everything from basic walking, swimming, knockback, cobwebs, to bubble columns
    * It even covers riding entities from boats to pigs to striders
* Built upon covering edge cases to confirm accuracy
* 1.13+ clients on 1.13+ servers, 1.12- clients on 1.13+ servers, 1.13+ clients on 1.12- servers,
  and 1.12- clients on 1.12- servers are all supported regardless of the large technical changes
  between these versions.
* The order of collisions depends on the client version and is correct
* Accounts for minor bounding box differences between versions, for example:
    * Single glass panes will be a + shape for 1.7-1.8 players and * for 1.9+ players
    * 1.13+ clients on 1.8 servers see the + glass pane hitbox due to ViaVersion
    * Many other blocks have this extreme attention to detail.
    * Waterlogged blocks do not exist for 1.12 or below players
    * Blocks that do not exist in the client's version use ViaVersion's replacement block
    * Block data that cannot be translated to previous versions is replaced correctly
    * All vanilla collision boxes have been implemented

### Fully asynchronous and multithreaded design

* All movement checks and the overwhelming majority of listeners run on the Netty thread
* The anticheat can scale to many hundreds of players, if not more
* Thread safety is carefully thought out
* The next core allows for this design

### Full world replication

* The anticheat keeps a replica of the world for each player
* The replica is created by listening to chunk data packets, block places, and block changes
* On all versions, chunks are compressed to 16-64 kb per chunk using palettes
* Using this cache, the anticheat can safely access the world state
* Per player, the cache allows for multithreaded design
* Sending players fake blocks with packets is safe and does not lead to falses
* The world is recreated for each player to allow lag compensation
* Client sided blocks cause no issues with packet based blocks. Block glitching does not false the
  anticheat.

### Latency compensation

* World changes are queued until they reach the player
* This means breaking blocks under a player does not false the anticheat
* Everything from flying status to movement speed will be latency compensated

### Inventory compensation

* The player's inventory is tracked to prevent ghost blocks at high latency, and other errors

### Secure by design, not obscurity

* All systems are designed to be highly secure and mathematically impossible to bypass
* For example, the prediction engine knows all possible movements and cannot be bypassed
