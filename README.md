# AutoSwap (Minecraft 1.21.4 / Fabric)

A client-side Fabric mod that moves the first `minecraft:player_head` found in the main inventory or hotbar into the offhand when you press the configured key.

## Features
- Press **O** to open the AutoSwap menu.
- Toggle AutoSwap on or off in the menu.
- Choose a custom swap key in the menu (default: **H**).
- The swap uses the normal inventory swap action and works with Player Head stacks, including heads with custom names/NBT.
- Settings are saved in `.minecraft/config/autoswap.properties`.

## Build
Requires JDK 21.

```bash
gradle build
```
The mod jar will be in `build/libs/`.

## Install
Copy `autoswap-1.0.0.jar` into your Minecraft `mods` folder. Install Fabric Loader for Minecraft 1.21.4 and Fabric API.

## Controls
Open Minecraft **Options → Controls → Key Binds → AutoSwap** to change the menu key. The swap key can also be changed from the in-game AutoSwap menu.

## Notes
This is a client-side inventory convenience mod. In multiplayer, the server still validates inventory actions and may apply its own restrictions.
