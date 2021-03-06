package com.playares.core.configs.type;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.playares.commons.logger.Logger;
import com.playares.commons.util.general.Configs;
import com.playares.core.chat.data.ChatMessageType;
import com.playares.core.configs.AresConfig;
import com.playares.core.configs.ConfigManager;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;

public final class GeneralConfig implements AresConfig {
    @Getter public final ConfigManager configManager;
    @Getter public YamlConfiguration config;

    @Getter public String databaseName;
    @Getter public String databaseUri;

    @Getter public int maxAltLifespan;

    @Getter public int minNetworkNameLength;
    @Getter public int maxNetworkNameLength;
    @Getter public List<String> bannedNetworkNames;
    @Getter public int minPasswordLength;
    @Getter public int maxPasswordLength;
    @Getter public int maxNetworkMembers;
    @Getter public int maxJoinedNetworks;
    @Getter public int networkInactiveExpireSeconds;
    @Getter public int networkCreateCooldown;
    @Getter public int networkRenameCooldown;
    @Getter public boolean rangedChatEnabled;

    @Getter public Map<ChatMessageType, Double> chatRanges;
    @Getter public int combatLoggerEnemyRadius;
    @Getter public int pearlProtectionDuration;
    @Getter public int combatTagAttackedDuration;
    @Getter public int combatTagAttackerDuration;
    @Getter public int combatLoggerDuration;
    @Getter public int enderpearlDuration;
    @Getter public int crappleDuration;
    @Getter public int gappleDuration;

    @Getter public int mapNumber;

    @Getter public List<String> helpContext;

    public GeneralConfig(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @SuppressWarnings("unchecked") @Override
    public void load() {
        config = Configs.getConfig(configManager.getPlugin(), "general");

        databaseName = config.getString("database.name");
        databaseUri = config.getString("database.uri");

        maxAltLifespan = config.getInt("alt-tracking.alt-entry-lifespan");

        minNetworkNameLength = config.getInt("network-settings.name.min-length");
        maxNetworkNameLength = config.getInt("network-settings.name.max-length");
        bannedNetworkNames = (List<String>)config.getList("network-settings.name.banned-names");
        minPasswordLength = config.getInt("network-settings.password.min-length");
        maxPasswordLength = config.getInt("network-settings.password.max-length");
        maxNetworkMembers = config.getInt("network-settings.max-members");
        maxJoinedNetworks = config.getInt("network-settings.max-joined-networks");
        networkInactiveExpireSeconds = config.getInt("network-settings.inactive-delete-time");
        networkCreateCooldown = config.getInt("network-settings.cooldowns.create");
        networkRenameCooldown = config.getInt("network-settings.cooldowns.rename");

        combatLoggerDuration = config.getInt("combat-logger-settings.enemy-radius");
        pearlProtectionDuration = config.getInt("timer-settings.fresh-spawn-pearl-protection");
        combatTagAttackedDuration = config.getInt("timer-settings.combat-tag.attacked");
        combatTagAttackerDuration = config.getInt("timer-settings.combat-tag.attacker");
        combatLoggerDuration = config.getInt("timer-settings.combat-tag.logger-keep-alive");
        enderpearlDuration = config.getInt("timer-settings.enderpearl");
        crappleDuration = config.getInt("timer-settings.crapple");
        gappleDuration = config.getInt("timer-settings.gapple");

        rangedChatEnabled = config.getBoolean("message-settings.range-chat");
        chatRanges = Maps.newHashMap();

        mapNumber = config.getInt("map-context.map-number");
        helpContext = Lists.newArrayList();

        for (String chatType : config.getConfigurationSection("message-settings.chat-ranges").getKeys(false)) {
            ChatMessageType type = null;

            try {
                type = ChatMessageType.valueOf(chatType);
            } catch (IllegalArgumentException ignored) {}

            final double distance = config.getInt("message-settings.chat-ranges." + chatType);
            chatRanges.put(type, distance);
        }

        for (String line : config.getStringList("help-context")) {
            helpContext.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        Logger.print("General configuration loaded");
    }

    @Override
    public void reload() {
        load();

        Logger.print("General configuration reloaded");
    }
}