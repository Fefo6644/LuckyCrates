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

Each crate type file has its own structure to is, you can check out a detailed commented example here https://raw.githubusercontent.com/Fefo6644/LuckyCrates/master/src/main/resources/crates/example.json

___

This project was designed for 1.12.2 Paper (a.k.a. PaperSpigot) servers, but it works on 1.13+ as well.

## Compiling
All you have to do is clone the repository in any directory and run `./gradlew` on Linux or `.\gradlew.bat` on Windows in your terminal of choice, the compiled jar file will be located in `./build/libs/luckycrates-{version}-all.jar`
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
