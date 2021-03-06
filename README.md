# LuckyCrates
### Spice up your KitPvP matches with some fancy lootcrates

This plugin allows you to create fully customizable rotating custom-textured custom-loot lootcrates throughout your map, with features like
* Disappear on use for a certain amount of time
* Rotating speed
* Enchanted items
* Custom potions
* Permissions requirement
* Randomized loot
* Loot weights/rarity
* More???

Each crate type file has its own structure to is, you can check out a [detailed commented example here](https://raw.githubusercontent.com/Fefo6644/LuckyCrates/master/src/main/resources/crates/example.json)

___

This project was designed for Spigot 1.12.2 servers, but it works on 1.13+ as well.


## Commands & Permissions

All listed sub-commands fall under the `/luckycrates`/`/lc` name/label/alias and require the `luckycrates.use` permission.
* `/lc help` - Prints some plugin info and the list of available commands.
* `/lc nearest` - Teleports the player to the nearest (loaded) rotating crates in that world.
* `/lc reload` - Reloads the config file and crate files.
* `/lc remove` - Puts the player in a "crates removal" state where you can't use rotating crates and, you can remove one of them by hitting them. Run this command again to exit said state without removing any crates.
* `/lc remove nearest` - Removes the nearest (loaded) rotating crate in that world and notifies the player of where it was located. This does **not** put the player in the removal state.
* `/lc set <type>` - Places a rotating crate of the specified type (of the respective file name in the `LuckyCrates/crates` folder) that hides on use, preventing its usage until it appears again (see `secondsHidden.min` and `secondsHidden.max` in [config.yml](https://github.com/Fefo6644/LuckyCrates/blob/master/src/main/resources/crates/donor.json#L4-L7)).
* `/lc setpersistent <type>` - Places a rotating crate of the specified type that doesn't hide on use.


## Compiling

All you have to do is clone this repository in any directory and run `./gradlew` on Linux or `.\gradlew.bat` on Windows in your terminal of choice, the compiled jar file will be located in `./build/libs/luckycrates-{version}-all.jar`
```
git clone https://github.com/Fefo6644/LuckyCrates.git luckycrates
cd luckycrates
./gradlew
```


## Contributing

Contributions are way more than welcome! Everything will be taken into consideration and hopefully discussed.

This project follows [Google Java code style](https://google.github.io/styleguide/javaguide.html). Whilst it isn't strict (e.g. line width isn't always 100 chars/columns wide), try to follow the general layout of the file you're editing :)


## Attributions

* [adventure](https://github.com/KyoriPowered/adventure) by [KyoriPowered](https://github.com/KyoriPowered) was chosen for messages and chat components
* [brigadier](https://github.com/Mojang/brigadier) by [Mojang](https://github.com/Mojang) was chosen as command framework
