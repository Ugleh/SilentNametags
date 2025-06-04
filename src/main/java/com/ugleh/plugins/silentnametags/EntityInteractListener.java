package com.ugleh.plugins.silentnametags;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class EntityInteractListener implements Listener {

    private final SilentNametagsPlugin plugin;
    private final SilentNametagManager manager;

    public EntityInteractListener(SilentNametagsPlugin plugin, SilentNametagManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("silentnametags.use")) {
            String raw = plugin.getConfig()
                    .getString("messages.no_permission", "&cYou do not have permission to use Silent Nametags.");
            String colored = ChatColor.translateAlternateColorCodes('&', raw);
            player.sendMessage(colored);
            return;
        }

        if (!(event.getRightClicked() instanceof LivingEntity)) {
            return;
        }

        LivingEntity mob = (LivingEntity) event.getRightClicked();

        ItemStack itemInHand = switch (event.getHand()) {
            case HAND -> player.getInventory().getItemInMainHand();
            case OFF_HAND -> player.getInventory().getItemInOffHand();
            default -> null;
        };

        if (itemInHand == null || itemInHand.getType() != Material.NAME_TAG) {
            return;
        }

        if (manager.isSilentNametag(itemInHand)) {
            event.setCancelled(true);
            applySilentNametag(player, mob, itemInHand);
            return;
        }

        if (mob.isSilent()) {
            event.setCancelled(true);
            applyNormalNametagToSilentMob(player, mob, itemInHand);
            return;
        }
    }

    private void applySilentNametag(Player player, LivingEntity mob, ItemStack silentTag) {
        var meta = silentTag.getItemMeta();
        if (meta == null)
            return;

        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : "";
        String defaultName = manager.getDefaultDisplayName();

        // If renamed, give that name
        if (!displayName.equals(defaultName)) {
            mob.setCustomName(displayName);
            mob.setCustomNameVisible(true);
        }

        mob.setSilent(true);

        consumeOne(player, silentTag);
    }

    private void applyNormalNametagToSilentMob(Player player, LivingEntity mob, ItemStack normalTag) {
        var meta = normalTag.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            mob.setCustomName(meta.getDisplayName());
            mob.setCustomNameVisible(true);
        }

        mob.setSilent(false);

        consumeOne(player, normalTag);
    }

    private void consumeOne(Player player, ItemStack stack) {
        int slot = player.getInventory().getHeldItemSlot();
        if (stack.getAmount() <= 1) {
            player.getInventory().setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
    }
}
