/*
 * Copyright © 2019-2021 L2JOrg
 *
 * This file is part of the L2JOrg project.
 *
 * L2JOrg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2JOrg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2j.gameserver.engine.timedzone;

import org.l2j.gameserver.world.zone.type.TimedZone;

/**
 * @author JoeAlisson
 */
public class TimedZoneInfo {

    private int remainingTime;
    private int refillTime;
    private long lastRemainingTimeUpdate;

    public int remainingTime() {
        return remainingTime;
    }

    public void updateRemainingTime() {
        if(lastRemainingTimeUpdate > 0) {
            var currentTime = System.currentTimeMillis();
            remainingTime -= (currentTime - lastRemainingTimeUpdate)  / 1000.0;
            lastRemainingTimeUpdate = currentTime;
        }
    }

    public int getRechargedTime() {
        return refillTime;
    }

    public void setLastRemainingTimeUpdate(long lastRemainingTimeUpdate) {
        this.lastRemainingTimeUpdate = lastRemainingTimeUpdate;
    }

    public static TimedZoneInfo init(TimedZone timedZone) {
        var info = new TimedZoneInfo();
        info.remainingTime = timedZone.getTime();
        return info;
    }
}
