package com.ugleh.plugins.silentnametags;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SilentNametagManager {

    private final JavaPlugin plugin;
    private final NamespacedKey pdcKey;
    private final String defaultDisplayName;
    private final List<String> defaultLore;
    private final ConfigurationSection recipeSection;

    public SilentNametagManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pdcKey = new NamespacedKey(plugin, "silent_nametag");

        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection snSection = cfg.getConfigurationSection("silent-nametag");
        if (snSection == null) {
            plugin.getLogger().severe("Missing 'silent-nametag' section in config.yml!");
            throw new IllegalStateException("Missing config section");
        }

        this.defaultDisplayName = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                snSection.getString("display-name", "&bSilent Nametag"));

        this.defaultLore = snSection.getStringList("lore");
        this.recipeSection = snSection.getConfigurationSection("recipe");
        if (recipeSection == null) {
            plugin.getLogger().severe("Missing 'recipe' section in config.yml under 'silent-nametag'!");
            throw new IllegalStateException("Missing recipe section");
        }
    }

    public ItemStack getSilentNametagItem() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.setDisplayName(defaultDisplayName);
        List<String> coloredLore = defaultLore.stream()
                .map(line -> org.bukkit.ChatColor.translateAlternateColorCodes('&', line))
                .toList();
        meta.setLore(coloredLore);

        meta.addEnchant(org.bukkit.enchantments.Enchantment.INFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(pdcKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isSilentNametag(ItemStack stack) {
        if (stack == null || stack.getType() != Material.NAME_TAG)
            return false;
        if (!stack.hasItemMeta())
            return false;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(pdcKey, PersistentDataType.BYTE);
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public void registerRecipe() {
        String type = recipeSection.getString("type", "shapeless").toLowerCase();
        NamespacedKey recipeKey = new NamespacedKey(plugin, "silent_nametag_recipe");

        if (type.equals("shaped")) {
            var shape = recipeSection.getStringList("shape");
            if (shape.size() != 3) {
                plugin.getLogger().severe("Shaped recipe 'shape' must have exactly 3 lines!");
                return;
            }
            ShapedRecipe shaped = new ShapedRecipe(recipeKey, getSilentNametagItem());
            shaped.shape(shape.get(0), shape.get(1), shape.get(2));

            var ingredientsMap = recipeSection.getConfigurationSection("ingredients");
            if (ingredientsMap == null) {
                plugin.getLogger().severe("Missing 'ingredients' map under shaped recipe!");
                return;
            }
            for (String keyChar : ingredientsMap.getKeys(false)) {
                String matName = ingredientsMap.getString(keyChar);
                if (matName == null)
                    continue;
                Material mat = Material.getMaterial(matName.toUpperCase());
                if (mat == null) {
                    plugin.getLogger().warning("Unknown material: " + matName);
                    continue;
                }
                shaped.setIngredient(keyChar.charAt(0), mat);
            }
            Bukkit.addRecipe(shaped);
            plugin.getLogger().info("Registered shaped Silent Nametag recipe.");

        } else if (type.equals("shapeless")) {
            var list = recipeSection.getStringList("ingredients");
            if (list.isEmpty()) {
                plugin.getLogger().severe("Shapeless recipe needs at least 1 ingredient!");
                return;
            }
            ShapelessRecipe shapeless = new ShapelessRecipe(recipeKey, getSilentNametagItem());
            for (String matName : list) {
                Material mat = Material.getMaterial(matName.toUpperCase());
                if (mat == null) {
                    plugin.getLogger().warning("Unknown material: " + matName);
                    continue;
                }
                shapeless.addIngredient(mat);
            }
            Bukkit.addRecipe(shapeless);
            plugin.getLogger().info("Registered shapeless Silent Nametag recipe.");

        } else {
            plugin.getLogger().severe("Invalid recipe type '" + type + "' (must be 'shaped' or 'shapeless').");
        }
    }
}
