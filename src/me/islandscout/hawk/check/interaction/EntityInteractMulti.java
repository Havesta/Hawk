/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractEntityEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityInteractMulti extends EntityInteractionCheck {

    private final Map<UUID, Long> lastHitTime; //in client ticks

    public EntityInteractMulti() {
        super("fightmulti", "%player% failed entity multi-interact, VL %vl%");
        lastHitTime = new HashMap<>();
    }

    @Override
    protected void check(InteractEntityEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        if(pp.getCurrentTick() == lastHitTime.getOrDefault(pp.getUuid(), -1L)) {
            punish(pp, true, e);
        }
        else {
            reward(pp);
        }
        lastHitTime.put(pp.getUuid(), pp.getCurrentTick());
    }

    @Override
    public void removeData(Player p) {
        lastHitTime.remove(p.getUniqueId());
    }
}