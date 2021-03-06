package it.forgottenworld.fwparties.listener;

import it.forgottenworld.fwparties.FWParties;
import it.forgottenworld.fwparties.controller.ChatController;
import it.forgottenworld.fwparties.controller.PartyController;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;

public class PlayerListener implements Listener {

    private final FWParties plugin;

    public PlayerListener() {
        this.plugin = FWParties.getInstance();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        PartyController partyController = plugin.getPartyController();
        if(partyController.isPlayerInParty(event.getPlayer().getUniqueId())){
            partyController.removePlayerFromScoreboard(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity damagedEntity = event.getEntity();
        if (damagedEntity instanceof Player) {
            Player target = (Player) damagedEntity;
            Player damager;
            if (damagerEntity instanceof Projectile) {
                ProjectileSource projectileSource = ((Projectile) damagerEntity).getShooter();
                if (projectileSource instanceof Player) {
                    damager = (Player) projectileSource;
                } else {
                    return;
                }
            } else if (damagerEntity instanceof Player) {
                damager = (Player) damagerEntity;
            } else {
                return;
            }
            if(FWParties.getInstance().getPluginConfig().getConfig().getStringList("always_on_friendly_fire_worlds").contains(Objects.requireNonNull(target.getLocation().getWorld()).getName())){
                return;
            }
            if (plugin.getPartyController().areInSameParty(damager, target)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamaged(EntityDamageEvent event){
        if(event.getEntity() instanceof Player){
            updatePlayerHealthInScoreboard((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event){
        if(event.getEntity() instanceof Player){
            updatePlayerHealthInScoreboard((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (event.getEntity().getEffects().stream().map(PotionEffect::getType).anyMatch(potionEffect -> potionEffect == PotionEffectType.BLINDNESS || potionEffect == PotionEffectType.HARM || potionEffect == PotionEffectType.POISON || potionEffect == PotionEffectType.SLOW)) {
            if (event.getEntity().getShooter() instanceof Player) {
                Player shooter = (Player) event.getEntity().getShooter();
                Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
                event.getAffectedEntities().forEach(livingEntity -> {
                    if (livingEntity instanceof Player) {
                        Player damaged = (Player) livingEntity;
                        if(FWParties.getInstance().getPluginConfig().getConfig().getStringList("always_on_friendly_fire_worlds").contains(Objects.requireNonNull(damaged.getLocation().getWorld()).getName())){
                            return;
                        }
                        if (plugin.getPartyController().areInSameParty(shooter, damaged)) {
                            affectedEntities.remove(livingEntity);
                        }
                    }
                });
                try {
                    Field affectedEntitiesField = event.getClass().getDeclaredField("affectedEntities");
                    affectedEntitiesField.setAccessible(true);
                    affectedEntitiesField.set(event, affectedEntities);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        PartyController partyController = plugin.getPartyController();
        ChatController chatController = plugin.getChatController();
        if(partyController.isPlayerInParty(player.getUniqueId())){
            if (chatController.isPlayerChatting(player.getUniqueId())) {
                event.setCancelled(true);
                chatController.sendMessageToPartyMembers(partyController.getPlayerParty(player.getUniqueId()).getLeader(), "&2[PARTY] &a" + player.getName() + ": " + message);
            }
        }
    }

    private void updatePlayerHealthInScoreboard(Player player){
        PartyController partyController = plugin.getPartyController();
        if(partyController.isPlayerInParty(player.getUniqueId())){
            partyController.updatePlayerHealthInScoreboard(player);
        }
    }
}
