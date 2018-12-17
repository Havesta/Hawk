/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk;

import me.islandscout.hawk.check.Check;
import me.islandscout.hawk.util.ClientBlock;
import me.islandscout.hawk.util.ServerUtils;
import net.minecraft.server.v1_7_R4.PacketPlayInEntityAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides essential and additional tools to analyze players during
 * packet interception. Recommended to use this rather than
 * Bukkit's implementation. Also provides tools for the Netty
 * thread.
 */
public class HawkPlayer {

    private final UUID uuid;
    private final Map<Check, Double> vl;
    private boolean digging;
    private boolean receiveNotifications;
    private boolean online;
    private final Player p;
    private boolean teleporting;
    private Location teleportLoc;
    private long lastTeleportTime;
    private final Hawk hawk;
    private Location location;
    private Vector velocity;
    private float deltaYaw;
    private float deltaPitch;
    private boolean onGround;
    private int ping;
    private short pingJitter;
    private long lastMoveTime;
    private long currentTick;
    private boolean sneaking;
    private boolean sprinting;
    private double maxY;
    private double jumpedHeight;
    private long flyPendingTime;
    private final Set<ClientBlock> clientBlocks;

    HawkPlayer(Player p, Hawk hawk) {
        this.uuid = p.getUniqueId();
        vl = new ConcurrentHashMap<>();
        receiveNotifications = true;
        this.p = p;
        this.location = p.getLocation();
        this.velocity = new Vector();
        this.onGround = ((Entity) p).isOnGround();
        this.hawk = hawk;
        this.ping = ServerUtils.getPing(p);
        clientBlocks = new HashSet<>();
    }

    public int getVL(Check check) {
        return (int) (double) vl.getOrDefault(check, 0D);
    }

    public void setVL(Check check, double vl) {
        this.vl.put(check, vl);
    }

    public void incrementVL(Check check) {
        this.vl.put(check, vl.getOrDefault(check, 0D) + 1D);
    }

    public void multiplyVL(Check check, double factor) {
        vl.put(check, vl.getOrDefault(check, 0D) * factor);
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isDigging() {
        return digging;
    }

    //this should really not be public
    public void setDigging(boolean digging) {
        this.digging = digging;
    }

    public boolean canReceiveNotifications() {
        return receiveNotifications && p.hasPermission(Hawk.BASE_PERMISSION + ".notify");
    }

    public void setReceiveNotifications(boolean status) {
        receiveNotifications = status;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isTeleporting() {
        return teleporting;
    }

    //this should really not be public
    public void setTeleporting(boolean status) {
        teleporting = status;
    }

    public Location getTeleportLoc() {
        return teleportLoc;
    }

    public void setTeleportLoc(Location teleportLoc) {
        this.teleportLoc = teleportLoc;
    }

    public long getLastTeleportTime() {
        return lastTeleportTime;
    }

    public void setLastTeleportTime(long lastTeleportTime) {
        this.lastTeleportTime = lastTeleportTime;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isOnGround() {
        return onGround;
    }

    //this should really not be public
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public short getPingJitter() {
        return pingJitter;
    }

    public void setPingJitter(short pingJitter) {
        this.pingJitter = pingJitter;
    }

    public long getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(long lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public void setDeltaYaw(float deltaYaw) {
        this.deltaYaw = deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public void setDeltaPitch(float deltaPitch) {
        this.deltaPitch = deltaPitch;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void incrementCurrentTick() {
        this.currentTick++;
        manageClientBlocks();
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public double getFallDistance() {
        return maxY - location.getY();
    }

    //call this before updating whether on ground or not
    public void updateFallDistance(Location loc) {
        if (onGround)
            maxY = loc.getY();
        else
            maxY = Math.max(loc.getY(), maxY);

    }

    public double getTotalAscensionSinceGround() {
        return jumpedHeight;
    }

    //call this before updating whether on ground or not
    public void updateTotalAscensionSinceGround(double y1, double y2) {
        if (onGround) {
            jumpedHeight = 0;
            return;
        }
        double deltaY = y2 - y1;
        if(deltaY > 0)
            jumpedHeight += deltaY;
    }

    public boolean hasFlyPending() {
        return System.currentTimeMillis() - flyPendingTime <= 100;
    }

    public void setFlyPendingTime(long time) {
        flyPendingTime = time;
    }

    public Player getPlayer() {
        return p;
    }

    public Set<ClientBlock> getClientBlocks() {
        return clientBlocks;
    }

    public void addClientBlock(ClientBlock pBlock) {
        if (clientBlocks.size() >= ClientBlock.MAX_PER_PLAYER)
            return;
        clientBlocks.add(pBlock);
    }

    private void manageClientBlocks() {
        Set<ClientBlock> oldPBlocks = new HashSet<>();
        for (ClientBlock loopPBlock : clientBlocks) {
            if (currentTick - loopPBlock.getInitTick() > ClientBlock.CLIENTTICKS_UNTIL_EXPIRE) {
                oldPBlocks.add(loopPBlock);
            }
        }
        clientBlocks.removeAll(oldPBlocks);
    }

    //safely kill the connection
    public void kickPlayer(String reason) {
        online = false;
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> p.kickPlayer(reason), 0L);
    }

    //safely teleport player
    public void teleportPlayer(Location location, PlayerTeleportEvent.TeleportCause teleportCause) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> p.teleport(location, teleportCause), 0L);
    }

    //Returns predicted location/direction of player between current and next client move
    //Useful for predicting where a 1.7 player may be facing during an attack packet
    public Location getPredictedLocation() {
        Vector movement = velocity.clone();
        Vector rotation = new Vector(deltaYaw, deltaPitch, 0);
        double moveDelay = System.currentTimeMillis() - lastMoveTime;
        if (moveDelay >= 100) {
            moveDelay = 0D;
        } else {
            moveDelay = moveDelay / 50;
        }
        movement.multiply(moveDelay);
        rotation.multiply(moveDelay);

        Location loc = location.clone();
        loc.add(movement);
        loc.setYaw(loc.getYaw() + (float) rotation.getX());
        loc.setPitch(loc.getPitch() + (float) rotation.getY());

        return loc;
    }

    /*
    public PlayerCheckData getCheckData(Check check) {
        return playerCheckData[check.getId()];
    }

    public void setCheckData(Check check, PlayerCheckData data) {
        playerCheckData[check.getId()] = data;
    }
    */
}
