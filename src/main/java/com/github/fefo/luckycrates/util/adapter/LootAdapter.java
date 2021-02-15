package com.github.fefo.luckycrates.util.adapter;

import com.github.fefo.luckycrates.internal.Loot;
import com.google.common.collect.ImmutableList;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.md_5.bungee.api.ChatColor.translateAlternateColorCodes;

public final class LootAdapter extends TypeAdapter<Loot> {

  public static final LootAdapter ADAPTER = new LootAdapter();

  private LootAdapter() { }

  @Override
  public void write(final JsonWriter out, final Loot loot) throws IOException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Loot read(final JsonReader in) throws IOException {
    in.beginObject();

    Integer weight = null;
    List<String> commands = null;
    List<ItemStack> items = null;

    while (in.hasNext()) {
      switch (in.nextName()) {
        case "weight": {
          weight = in.nextInt();
          break;
        }

        case "commands": {
          in.beginArray();

          commands = new ArrayList<>();
          while (in.hasNext()) {
            commands.add(in.nextString());
          }

          in.endArray();
          break;
        }

        case "items": {
          in.beginArray();

          items = new ArrayList<>();
          while (in.hasNext()) {
            in.beginObject();

            Material material = null;
            Integer amount = null;
            String displayName = null;
            Boolean unbreakable = null;
            Byte data = null;
            Map<Enchantment, Integer> enchantments = null;
            PotionEffect potionEffect = null;

            while (in.hasNext()) {
              switch (in.nextName()) {
                case "type": {
                  material = Material.matchMaterial(in.nextString());
                  break;
                }

                case "amount": {
                  amount = in.nextInt();
                  break;
                }

                case "unbreakable": {
                  unbreakable = in.nextBoolean();
                  break;
                }

                case "data": {
                  data = (byte) in.nextInt();
                  break;
                }

                case "displayName": {
                  displayName = in.nextString();
                  break;
                }

                case "potionEffect": {
                  in.beginObject();

                  PotionEffectType type = null;
                  Integer duration = null;
                  Integer amplifier = null;

                  while (in.hasNext()) {
                    switch (in.nextName()) {
                      case "type": {
                        type = PotionEffectType.getByName(in.nextString());
                        break;
                      }

                      case "durationSeconds": {
                        duration = in.nextInt();
                        break;
                      }

                      case "amplifier": {
                        amplifier = in.nextInt();
                        break;
                      }

                      default: {
                        in.skipValue();
                        break;
                      }
                    }
                  }

                  if (type != null && duration != null && amplifier != null
                      && duration > 0 && amplifier >= 0) {
                    potionEffect = new PotionEffect(type, duration * 20, amplifier, false, true, Color.fromRGB((int) (Math.random() * (double) 0x01000000)));
                  }

                  in.endObject();
                  break;
                }

                case "enchantments": {
                  in.beginArray();

                  enchantments = new HashMap<>();

                  while (in.hasNext()) {
                    in.beginObject();

                    Enchantment type = null;
                    Integer level = null;

                    while (in.hasNext()) {
                      switch (in.nextName()) {
                        case "type": {
                          type = Enchantment.getByName(in.nextString());
                          break;
                        }

                        case "level": {
                          level = in.nextInt();
                          break;
                        }

                        default: {
                          in.skipValue();
                          break;
                        }
                      }
                    }

                    if (type != null && level != null && level > 0) {
                      enchantments.put(type, level);
                    }

                    in.endObject();
                  }
                  enchantments.remove(null);

                  in.endArray();
                  break;
                }

                default: {
                  in.skipValue();
                  break;
                }
              }
            }

            Validate.notNull(material, "material");
            Validate.notNull(amount, "amount");
            final ItemStack itemStack = new ItemStack(material, amount);

            if (data != null) {
              itemStack.getData().setData(data);
            }

            final ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null) {
              in.endObject();
              continue;
            }

            if (displayName != null) {
              itemMeta.setDisplayName(translateAlternateColorCodes('&', "&f" + displayName));
            }

            if (unbreakable != null) {
              itemMeta.setUnbreakable(unbreakable);
            }

            if (potionEffect != null && itemMeta instanceof PotionMeta) {
              final PotionMeta potionMeta = (PotionMeta) itemMeta;
              potionMeta.addCustomEffect(potionEffect, true);
              potionMeta.setColor(potionEffect.getColor());
            }

            if (enchantments != null && !enchantments.isEmpty()) {
              if (itemMeta instanceof EnchantmentStorageMeta) {
                final EnchantmentStorageMeta enchantmentStorage = (EnchantmentStorageMeta) itemMeta;
                enchantments.forEach((enchantment, level) -> enchantmentStorage.addStoredEnchant(enchantment, level, true));
              } else {
                enchantments.forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
              }
            }

            itemStack.setItemMeta(itemMeta);
            items.add(itemStack);

            in.endObject();
          }

          in.endArray();
          break;
        }

        default: {
          in.skipValue();
          break;
        }
      }
    }

    Validate.notNull(weight, "weight");
    Validate.isTrue(weight > 0, "weight must be greater than 0");
    Validate.notNull(items, "items");

    in.endObject();
    return new Loot(weight, commands == null ? ImmutableList.of() : commands, items);
  }
}
