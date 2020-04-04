package com.llewkcor.ares.core.bastion;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.llewkcor.ares.commons.location.BLocatable;
import com.llewkcor.ares.core.Ares;
import com.llewkcor.ares.core.bastion.data.Bastion;
import com.llewkcor.ares.core.bastion.listener.BastionListener;
import com.llewkcor.ares.core.network.data.Network;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BastionManager {
    @Getter public final Ares plugin;
    @Getter public final BastionHandler handler;
    @Getter public final Set<Bastion> bastionRepository;

    public BastionManager(Ares plugin) {
        this.plugin = plugin;
        this.handler = new BastionHandler(this);
        this.bastionRepository = Sets.newConcurrentHashSet();

        Bukkit.getPluginManager().registerEvents(new BastionListener(this), plugin);
    }

    /**
     * Returns a Bastion matching the provided Bastion UUID
     * @param uniqueId Bastion UUID
     * @return Bastion
     */
    public Bastion getBastionByID(UUID uniqueId) {
        return bastionRepository.stream().filter(bastion -> bastion.getUniqueId().equals(uniqueId)).findFirst().orElse(null);
    }

    /**
     * Returns a Bastion matching the provided Block location
     * @param location Block location
     * @return Bastion
     */
    public Bastion getBastionByBlock(BLocatable location) {
        return bastionRepository.stream().filter(block -> block.getLocation().getX() == location.getX() && block.getLocation().getY() == location.getY() && block.getLocation().getZ() == location.getZ() && block.getLocation().getWorldName().equals(location.getWorldName())).findFirst().orElse(null);
    }

    /**
     * Returns a Immutable Set containing all bastions owned by the provided Network
     * @param network Network
     * @return Immutable Set of Bastions
     */
    public ImmutableSet<Bastion> getBastionByOwner(Network network) {
        return ImmutableSet.copyOf(bastionRepository.stream().filter(bastion -> bastion.getOwnerId().equals(network.getCreatorId())).collect(Collectors.toSet()));
    }

    /**
     * Returns an Immutable Set containing all bastions in range of the provided block location and radius
     * @param location Block location
     * @param radius Radius
     * @return Immutable Set of Bastions
     */
    public ImmutableSet<Bastion> getBastionInRange(BLocatable location, double radius) {
        return ImmutableSet.copyOf(bastionRepository.stream().filter(bastion -> bastion.inside(location, radius)).collect(Collectors.toSet()));
    }

    /**
     * Returns an Immutable Set containing all bastions in a flat range of the provided block location and radius
     * @param location Block location
     * @param radius Radius
     * @return Immutable Set of Bastions
     */
    public ImmutableSet<Bastion> getBastionInRangeFlat(BLocatable location, double radius) {
        return ImmutableSet.copyOf(bastionRepository.stream().filter(bastion -> bastion.insideFlat(location, radius)).collect(Collectors.toSet()));
    }
}