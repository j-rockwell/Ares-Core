package com.playares.core.acid.data;

import com.playares.commons.connect.mongodb.MongoDocument;
import com.playares.commons.location.BLocatable;
import com.playares.commons.util.general.Time;
import com.playares.core.network.data.Network;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bukkit.block.Block;

import java.util.UUID;

public final class AcidBlock implements MongoDocument<AcidBlock> {
    @Getter public UUID uniqueId;
    @Getter public UUID ownerId;
    @Getter public int chunkX;
    @Getter public int chunkZ;
    @Getter public BLocatable location;
    @Getter @Setter public long matureTime;
    @Getter @Setter public long expireTime;
    @Getter public int damageDealt;

    public AcidBlock() {
        this.uniqueId = null;
        this.ownerId = null;
        this.location = null;
        this.matureTime = 0L;
        this.expireTime = 0L;
        this.damageDealt = 0;
    }

    public AcidBlock(Network owner, Block block, long matureTime, long expireTime) {
        this.uniqueId = UUID.randomUUID();
        this.ownerId = owner.getUniqueId();
        this.chunkX = block.getChunk().getX();
        this.chunkZ = block.getChunk().getZ();
        this.location = new BLocatable(block);
        this.matureTime = matureTime;
        this.expireTime = expireTime;
        this.damageDealt = 0;
    }

    /**
     * Returns true if this Acid Block is mature
     * @return True if matured
     */
    public boolean isMature() {
        return this.matureTime <= Time.now();
    }

    /**
     * Returns true if this Acid Block is expired
     * @return True if expired
     */
    public boolean isExpired() {
        return this.expireTime <= Time.now();
    }

    /**
     * Returns true if the provided location is within the provided radius of this Acid Block
     * @param location Location
     * @param radius Radius
     * @return True if within radius
     */
    public boolean inside(BLocatable location, double radius) {
        final double distance = this.location.distance(location);
        return (distance >= 0.0 && distance <= radius);
    }

    /**
     * Adds damage dealth to this Acid Block
     * @param amount Amount to add
     */
    public void addDamage(int amount) {
        this.damageDealt += amount;
    }

    @Override
    public AcidBlock fromDocument(Document document) {
        this.uniqueId = (UUID)document.get("id");
        this.ownerId = (UUID)document.get("owner");
        this.chunkX = document.getInteger("chunk_x");
        this.chunkZ = document.getInteger("chunk_z");
        this.location = new BLocatable().fromDocument(document.get("location", Document.class));
        this.matureTime = document.getLong("mature");
        this.expireTime = document.getLong("expire");
        this.damageDealt = document.getInteger("damage_dealt");

        return this;
    }

    @Override
    public Document toDocument() {
        return new Document()
                .append("id", uniqueId)
                .append("owner", ownerId)
                .append("chunk_x", chunkX)
                .append("chunk_z", chunkZ)
                .append("location", location.toDocument())
                .append("mature", matureTime)
                .append("expire", expireTime)
                .append("damage_dealt", damageDealt);
    }
}