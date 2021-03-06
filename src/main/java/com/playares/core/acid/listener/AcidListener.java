package com.playares.core.acid.listener;

import com.playares.commons.location.BLocatable;
import com.playares.core.acid.AcidManager;
import com.playares.core.acid.data.AcidBlock;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.List;

@AllArgsConstructor
public final class AcidListener implements Listener {
    @Getter public final AcidManager manager;

    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Block block = event.getBlock();
        final AcidBlock acid = manager.getAcidBlockByBlock(new BLocatable(block));

        if (acid == null) {
            return;
        }

        manager.getHandler().deleteAcid(acid);
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();

        if (!manager.getAcidBlockByChunk(chunk).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final List<Block> blocks = event.getBlocks();

        for (Block block : blocks) {
            final AcidBlock acidBlock = manager.getAcidBlockByBlock(new BLocatable(block));

            if (acidBlock != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final List<Block> blocks = event.getBlocks();

        for (Block block : blocks) {
            final AcidBlock acidBlock = manager.getAcidBlockByBlock(new BLocatable(block));

            if (acidBlock != null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
