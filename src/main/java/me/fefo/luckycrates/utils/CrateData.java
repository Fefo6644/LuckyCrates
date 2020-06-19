package me.fefo.luckycrates.utils;

import me.fefo.luckycrates.utils.hex.utils.Skull;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CrateData {
  private final @NotNull String name;
  private final @NotNull ItemStack customSkull;
  private final @NotNull Random rng = new Random();
  private final int lowerBound;
  private final int upperBound;
  private final @Nullable String permRequired;
  private final @NotNull ArrayList<Loot> lootTable = new ArrayList<>();

  public CrateData(final @NotNull String crateName,
                   final @NotNull YamlConfiguration crateYaml)
      throws AssertionError {

    lowerBound = 1000 * crateYaml.getIntegerList("secondsUntilReappearance").get(0);
    upperBound = 1000 * crateYaml.getIntegerList("secondsUntilReappearance").get(1);
    // compilers are stupid
    //noinspection ConstantConditions
    if (lowerBound <= 0 || lowerBound > upperBound) {
      throw new AssertionError("lowerBound must be greater than zero - " +
                               "lowerBound must be less than or equal to upperBound");
    }

    List<Map<?, ?>> rewardsListYaml = crateYaml.getMapList("rewards");
    for (final Map<?, ?> rewardYaml : rewardsListYaml) {
      if (((Integer) rewardYaml.get("rarity")) <= 0) {
        throw new AssertionError("All rarities must be greater than zero");
      }
    }

    for (final Map<?, ?> rewardYaml : rewardsListYaml) {
      final ItemStack[] items = new ItemStack[((List<?>) rewardYaml.get("items")).size()];

      List<?> itemsListYaml = (List<?>) rewardYaml.get("items");
      for (int i = 0; i < itemsListYaml.size(); ++i) {
        final Map<?, ?> itemYaml = ((Map<?, ?>) itemsListYaml.get(i));

        items[i] = new ItemStack(Material.getMaterial((String) itemYaml.get("item")),
                                 ((Integer) itemYaml.get("amount")));

        if (itemYaml.containsKey("displayName")) {
          final ItemMeta itemMeta = items[i].getItemMeta();
          itemMeta.setDisplayName("" + ChatColor.RESET +
                                  ((String) itemYaml.get("displayName"))
                                      .replace('&', '§'));
          items[i].setItemMeta(itemMeta);
        }

        if (itemYaml.containsKey("data")) {
          items[i].setDurability(((Integer) itemYaml.get("data")).shortValue());
        }

        if (((String) itemYaml.get("item")).contains("POTION")) {
          final PotionMeta potionMeta = ((PotionMeta) items[i].getItemMeta());
          final PotionEffect potionEffect = new PotionEffect(PotionEffectType.getByName((String) itemYaml.get("potionEffectType")),
                                                             (Integer) itemYaml.get("potionDurationSeconds") * 20,
                                                             (Integer) itemYaml.get("potionEffectAmplifier"),
                                                             false, true, Color.fromRGB(rng.nextInt(0x1000000)));

          potionMeta.addCustomEffect(potionEffect, true);
          potionMeta.setColor(potionEffect.getColor());
          items[i].setItemMeta(potionMeta);
        }

        final ItemMeta itemMeta = items[i].getItemMeta();
        itemMeta.setUnbreakable(((Boolean) itemYaml.get("unbreakable")));
        items[i].setItemMeta(itemMeta);

        if (!(itemYaml.get("enchants") instanceof List)) {
          continue;
        }

        List<?> enchantsListYaml = (List<?>) itemYaml.get("enchants");
        for (final Object enchantYamlUncasted : enchantsListYaml) {
          final Map<?, ?> enchantYaml = (Map<?, ?>) enchantYamlUncasted;
          items[i].addUnsafeEnchantment(Enchantment.getByName(((String) enchantYaml.get("enchant"))),
                                        ((Integer) enchantYaml.get("level")));
        }
      }

      List<String> commandsList = new ArrayList<>();

      if (rewardYaml.containsKey("commands")) {
        // sucks ass
        //noinspection unchecked
        commandsList.addAll(((List<String>) rewardYaml.get("commands")));
      }

      lootTable.add(new Loot((Integer) rewardYaml.get("rarity"),
                             commandsList.toArray(new String[0]),
                             items));
    }
    lootTable.sort((l1, l2) -> l2.rarity - l1.rarity);

    name = crateName;
    final String requiresPerm = crateYaml.getString("requiresPerm");
    permRequired = requiresPerm.equals("false") ? null : requiresPerm;
    customSkull = Skull.getCustomSkull("http://textures.minecraft.net/texture/" + crateYaml.getString("texture"));
  }

  public @NotNull String getName() { return name; }
  public @NotNull ItemStack getSkull() { return customSkull; }
  public int getRandomTime() { return rng.nextInt(1 + upperBound - lowerBound) + lowerBound; }
  public boolean requiresPerm() { return permRequired != null; }
  public @Nullable String getPermRequired() { return permRequired; }

  public Loot getRandomLoot() {
    int totalWeight = 0;
    for (Loot loot : lootTable) {
      totalWeight += loot.rarity;
    }
    int randomPick = (int) (rng.nextDouble() * totalWeight);

    for (Loot loot : lootTable) {
      if (randomPick < loot.rarity) {
        return loot;
      }
      randomPick -= loot.rarity;
    }

    // This will never be reached but compilers are stupid.
    return lootTable.get(0);
  }
}
