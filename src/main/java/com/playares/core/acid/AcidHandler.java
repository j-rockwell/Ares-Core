package com.playares.core.acid;

import com.playares.commons.location.BLocatable;
import com.playares.commons.location.PLocatable;
import com.playares.commons.logger.Logger;
import com.playares.commons.promise.SimplePromise;
import com.playares.commons.util.bukkit.Scheduler;
import com.playares.commons.util.general.Time;
import com.playares.core.acid.data.AcidBlock;
import com.playares.core.acid.data.AcidDAO;
import com.playares.core.acid.menu.AcidListMenu;
import com.playares.core.bastion.data.Bastion;
import com.playares.core.network.data.Network;
import com.playares.core.network.data.NetworkMember;
import com.playares.core.network.data.NetworkPermission;
import com.playares.core.player.data.AresPlayer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class AcidHandler {
    @Getter public final AcidManager manager;

    /**
     * Load all Acid Blocks from the MongoDB instance to memory
     * @param blocking Block the thread
     */
    public void loadAll(boolean blocking) {
        if (blocking) {
            Logger.warn("Blocking the thread while attempting to load all acid blocks from the database");
            manager.getAcidRepository().addAll(AcidDAO.getAcidBlocks(manager.getPlugin().getDatabaseInstance()));
            Logger.print("Loaded " + manager.getAcidRepository().size() + " Acid Blocks");
            return;
        }

        new Scheduler(manager.getPlugin()).async(() -> {
            manager.getAcidRepository().addAll(AcidDAO.getAcidBlocks(manager.getPlugin().getDatabaseInstance()));
            new Scheduler(manager.getPlugin()).sync(() -> Logger.print("Loaded " + manager.getAcidRepository().size() + " Acid Blocks")).run();
        }).run();
    }

    /**
     * Save all Acid Blocks in memory to the MongoDB instance
     * @param blocking Block the thread
     */
    public void saveAll(boolean blocking) {
        if (blocking) {
            Logger.warn("Blocking the thread while attempting to save all acid blocks to the database");
            AcidDAO.saveAcidBlocks(manager.getPlugin().getDatabaseInstance(), manager.getAcidRepository());
            Logger.print("Saved " + manager.getAcidRepository().size() + " Acid Blocks");
            return;
        }

        new Scheduler(manager.getPlugin()).async(() -> {
            AcidDAO.saveAcidBlocks(manager.getPlugin().getDatabaseInstance(), manager.getAcidRepository());
            new Scheduler(manager.getPlugin()).sync(() -> Logger.print("Saved " + manager.getAcidRepository().size() + " Acid Blocks")).run();
        }).run();
    }

    /**
     * Performs a database cleanup for Expired Acid Blocks
     */
    public void performAcidCleanup() {
        Logger.warn("Starting Acid Cleanup...");

        new Scheduler(manager.getPlugin()).async(() -> {
            manager.getExpiredAcidBlocks().forEach(expired -> {
                manager.getAcidRepository().remove(expired);
                AcidDAO.deleteAcidBlock(manager.getPlugin().getDatabaseInstance(), expired);
            });

            new Scheduler(manager.getPlugin()).sync(() -> Logger.print("Completed Acid Cleanup")).run();
        }).run();
    }

    /**
     * Handles creating a new Acid Block
     * @param player Player
     * @param networkName Network Name
     * @param block Block
     * @param promise Promise
     */
    public void createAcid(Player player, String networkName, Block block, SimplePromise promise) {
        final AresPlayer account = manager.getPlugin().getPlayerManager().getPlayer(player.getUniqueId());
        final boolean admin = player.hasPermission("arescore.admin");
        final Network network = manager.getPlugin().getNetworkManager().getNetworkByName(networkName);
        final BLocatable location = new BLocatable(block);

        if (account == null) {
            promise.fail("Failed to obtain your account");
            return;
        }

        if (!account.isSpawned()) {
            promise.fail("You have not spawned in yet");
            return;
        }

        if (network == null) {
            promise.fail("Network not found");
            return;
        }

        final NetworkMember networkMember = network.getMember(player);

        if (networkMember == null && !admin) {
            promise.fail("You are not a member of this network");
            return;
        }

        if (!admin && !(networkMember.hasPermission(NetworkPermission.ADMIN) || networkMember.hasPermission(NetworkPermission.MODIFY_ACID))) {
            promise.fail("You do not have permission to perform this action");
            return;
        }

        final AcidBlock existing = manager.getAcidBlockByBlock(location);

        if (existing != null) {
            promise.fail("This block is already an acid block");
            return;
        }

        final Set<Bastion> badBastions = manager.getPlugin().getBastionManager().getBastionInRange(new BLocatable(block), manager.getPlugin().getConfigManager().getBastionsConfig().getBastionRadius());
        final AcidBlock acidBlock = new AcidBlock(network, block, (Time.now() + (manager.getPlugin().getConfigManager().getAcidConfig().getAcidMatureTime() * 1000L)), (Time.now() + (manager.getPlugin().getConfigManager().getAcidConfig().getAcidExpireTime() * 1000L)));

        manager.getAcidRepository().add(acidBlock);
        new Scheduler(manager.getPlugin()).async(() -> AcidDAO.saveAcidBlock(manager.getPlugin().getDatabaseInstance(), acidBlock)).run();

        network.sendMessage(ChatColor.BLUE + player.getName() + ChatColor.YELLOW + " created an " + ChatColor.RED + "Acid Block" + ChatColor.YELLOW + " at " + acidBlock.getLocation().toString());
        player.sendMessage(ChatColor.YELLOW + "This Acid Block will mature in " + Time.convertToRemaining(acidBlock.getMatureTime() - Time.now()));

        for (Bastion bastion : badBastions) {
            if (bastion.isMature() || bastion.getOwnerId().equals(network.getUniqueId())) {
                continue;
            }

            player.sendMessage(ChatColor.RED + "This Acid Block is being blocked by a Bastion at " + bastion.toString());
        }

        promise.success();
    }

    /**
     * Handles deleting an Acid Block
     * @param acidBlock Acid Block
     */
    public void deleteAcid(AcidBlock acidBlock) {
        manager.getAcidRepository().remove(acidBlock);
        new Scheduler(manager.getPlugin()).async(() -> AcidDAO.deleteAcidBlock(manager.getPlugin().getDatabaseInstance(), acidBlock));
    }

    /**
     * Handles proving lookup info for an Acid Block
     * @param player Player
     * @param block Block
     * @param promise Promise
     */
    public void lookupAcid(Player player, Block block, SimplePromise promise) {
        final AcidBlock acid = manager.getAcidBlockByBlock(new BLocatable(block));

        if (acid == null) {
            promise.fail("This block is not an acid block");
            return;
        }

        final Network network = manager.getPlugin().getNetworkManager().getNetworkByID(acid.getOwnerId());

        if (network == null) {
            promise.fail("There was an unexpected error");
            return;
        }

        final Set<Bastion> badBastions = manager.getPlugin().getBastionManager().getBastionInRange(new BLocatable(block), manager.getPlugin().getConfigManager().getBastionsConfig().getBastionRadius());

        player.sendMessage(ChatColor.YELLOW + "Acid claimed by " + ChatColor.BLUE + network.getName() + ChatColor.YELLOW + ", " + (acid.isMature() ? "matured." : "matures in " + Time.convertToRemaining(acid.getMatureTime() - Time.now())));
        player.sendMessage(ChatColor.YELLOW + "Expires in " + Time.convertToRemaining(acid.getExpireTime() - Time.now()));

        for (Bastion bastion : badBastions) {
            if (bastion.isMature() || bastion.getOwnerId().equals(network.getUniqueId())) {
                continue;
            }

            player.sendMessage(ChatColor.YELLOW + "This " + ChatColor.RED + "Acid Block" + ChatColor.YELLOW + " is being blocked by a Bastion at " + ChatColor.BLUE + bastion.toString());
        }

        promise.success();
    }

    /**
     * Handles displaying a menu of all Acid Blocks owned by the provided network name
     * @param player Player
     * @param networkName Network Name
     * @param promise Promise
     */
    public void listByNetwork(Player player, String networkName, SimplePromise promise) {
        final Network network = manager.getPlugin().getNetworkManager().getNetworkByName(networkName);
        final boolean admin = player.hasPermission("arescore.admin");

        if (network == null) {
            promise.fail("Network not found");
            return;
        }

        final NetworkMember member = network.getMember(player);

        if (member == null && !admin) {
            promise.fail("You are not a member of this network");
            return;
        }

        if (!admin && !(member.hasPermission(NetworkPermission.ADMIN) || member.hasPermission(NetworkPermission.VIEW_SNITCHES))) {
            promise.fail("You do not have permission to perform this action");
            return;
        }

        final Set<AcidBlock> acid = manager.getAcidBlockByOwner(network);

        if (acid.isEmpty()) {
            promise.fail("This network does not have any active acid blocks");
            return;
        }

        final AcidListMenu menu = new AcidListMenu(manager.getPlugin(), player, "Acid Blocks: " + network.getName(), acid);
        menu.open();
        promise.success();
    }

    /**
     * Handles opening a menu showing all nearby Acid Blocks a player can see
     * @param player Player
     * @param promise Promise
     */
    public void listByNearby(Player player, SimplePromise promise) {
        final UUID uniqueId = player.getUniqueId();
        final PLocatable location = new PLocatable(player);

        new Scheduler(manager.getPlugin()).async(() -> {
            final List<AcidBlock> nearby = manager.getAcidRepository().stream().filter(acid -> acid.getLocation().nearby(location, 64.0)).collect(Collectors.toList());
            final List<AcidBlock> friendly = nearby.stream().filter(acid -> manager.getPlugin().getNetworkManager().getNetworkByID(acid.getOwnerId()).isMember(uniqueId)).collect(Collectors.toList());

            new Scheduler(manager.getPlugin()).sync(() -> {
                if (friendly.isEmpty()) {
                    promise.fail("There are no acid blocks nearby");
                    return;
                }

                final AcidListMenu menu = new AcidListMenu(manager.getPlugin(), player, "Nearby Acid Blocks", friendly);
                menu.open();
                promise.success();
            }).run();
        }).run();
    }
}