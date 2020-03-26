package com.llewkcor.ares.core.network.handlers;

import com.google.common.collect.Maps;
import com.llewkcor.ares.commons.logger.Logger;
import com.llewkcor.ares.commons.promise.SimplePromise;
import com.llewkcor.ares.commons.util.bukkit.Scheduler;
import com.llewkcor.ares.commons.util.general.Time;
import com.llewkcor.ares.core.network.NetworkHandler;
import com.llewkcor.ares.core.network.data.Network;
import com.llewkcor.ares.core.network.data.NetworkMember;
import com.llewkcor.ares.core.network.data.NetworkPermission;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class NetworkManageHandler {
    @Getter public final NetworkHandler handler;
    @Getter public final Map<UUID, Long> renameCooldowns;

    public NetworkManageHandler(NetworkHandler handler) {
        this.handler = handler;
        this.renameCooldowns = Maps.newConcurrentMap();
    }

    /**
     * Handles the deletion of a network
     * @param player Player
     * @param networkName Network Name
     * @param promise Promise
     */
    public void deleteNetwork(Player player, String networkName, SimplePromise promise) {
        final Network network = handler.getManager().getNetworkByName(networkName);
        final boolean admin = player.hasPermission("arescore.admin");

        if (network == null) {
            promise.fail("Network not found");
            return;
        }

        final NetworkMember networkMember = network.getMember(player);

        if (networkMember == null) {
            promise.fail("You are not a member of this network");
            return;
        }

        if (!networkMember.hasPermission(NetworkPermission.ADMIN) && !admin) {
            promise.fail("You do not have permission to perform this action");
            return;
        }

        // TODO: Delete all snitches and factories related to this network

        network.sendMessage(ChatColor.RED + network.getName() + " has been disbanded by " + player.getName());
        network.getMembers().clear();
        network.getPendingMembers().clear();
        handler.getManager().getNetworkRepository().remove(network);

        Logger.print("Network " + network.getName() + "(" + network.getUniqueId().toString() + ") has been disbanded by " + player.getName() + "(" + player.getUniqueId().toString() + ")");

        promise.success();
    }

    /**
     * Handles a player leaving a network
     * @param player Player
     * @param networkName Network Name
     * @param promise Promise
     */
    public void leaveNetwork(Player player, String networkName, SimplePromise promise) {
        final Network network = handler.getManager().getNetworkByName(networkName);

        if (network == null) {
            promise.fail("Network not found");
            return;
        }

        final NetworkMember networkMember = network.getMember(player);

        if (networkMember == null) {
            promise.fail("You are not a member of this network");
            return;
        }

        if (networkMember.hasPermission(NetworkPermission.ADMIN) && network.getMembersWithPermission(NetworkPermission.ADMIN).size() <= 1) {
            promise.fail("There must be at least one other member with the ADMIN permission. Promote another member of disband the network.");
            return;
        }

        network.removeMember(player.getUniqueId());
        network.sendMessage(ChatColor.RED + player.getName() + " has left " + network.getName());
        promise.success();
    }

    public void kickFromNetwork(Player player, String network, String username, SimplePromise promise) {

    }

    public void renameNetwork(Player player, String networkName, String newName, SimplePromise promise) {
        final Network network = handler.getManager().getNetworkByName(networkName);
        final UUID bukkitID = player.getUniqueId();
        final boolean admin = player.hasPermission("arescore.admin");

        if (network == null) {
            promise.fail("Network not found");
            return;
        }

        final NetworkMember networkMember = network.getMember(player);

        if (networkMember == null && !admin) {
            promise.fail("You are not a member of this network");
            return;
        }

        if (networkMember != null && !networkMember.hasPermission(NetworkPermission.ADMIN)) {
            promise.fail("You do not have permission to perform this action");
            return;
        }

        if (renameCooldowns.containsKey(player.getUniqueId()) && !admin) {
            final long remaining = renameCooldowns.get(player.getUniqueId()) - Time.now();
            promise.fail("Please wait " + Time.convertToRemaining(remaining) + " before attempting to rename another network");
            return;
        }

        if (!newName.matches("^[A-Za-z0-9_.]+$")) {
            promise.fail("Name may only contain characters A-Z & 0-9");
            return;
        }

        if (newName.length() < handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getMinNetworkNameLength()) {
            promise.fail("Name must be at least " + handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getMinNetworkNameLength() + " characters long");
            return;
        }

        if (newName.length() > handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getMaxNetworkNameLength()) {
            promise.fail("Name must be " + handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getMaxNetworkNameLength() + " characters long or less");
            return;
        }

        if (handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getBannedNetworkNames().contains(newName.toUpperCase())) {
            promise.fail("This network name is not allowed");
            return;
        }

        if (handler.getManager().getNetworkByName(newName) != null) {
            promise.fail("Network name is already in use");
            return;
        }

        network.sendMessage(ChatColor.YELLOW + player.getName() + " renamed " + network.getName() + " to " + newName);
        Logger.print("Network " + network.getName() + "(" + network.getUniqueId().toString() + ") has been renamed to " + newName + " by " + player.getName() + "(" + player.getUniqueId().toString() + ")");

        network.setName(newName);

        renameCooldowns.put(player.getUniqueId(), (Time.now() + (handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getNetworkRenameCooldown() * 1000L)));
        new Scheduler(handler.getManager().getPlugin()).sync(() -> renameCooldowns.remove(bukkitID)).delay(handler.getManager().getPlugin().getConfigManager().getGeneralConfig().getNetworkRenameCooldown() * 1000L).run();

        promise.success();
    }
}
