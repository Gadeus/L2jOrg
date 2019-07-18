/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2j.gameserver.ai;

import org.l2j.commons.threading.ThreadPoolManager;
import org.l2j.gameserver.model.World;
import org.l2j.gameserver.model.WorldObject;
import org.l2j.gameserver.model.Location;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.actor.instance.Door;
import org.l2j.gameserver.model.actor.instance.Defender;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.model.skills.Skill;

/**
 * @author mkizub
 */
public class DoorAI extends CreatureAI {
    public DoorAI(Door door) {
        super(door);
    }

    @Override
    protected void onIntentionIdle() {
    }

    @Override
    protected void onIntentionActive() {
    }

    @Override
    protected void onIntentionRest() {
    }

    @Override
    protected void onIntentionAttack(Creature target) {
    }

    @Override
    protected void onIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove) {
    }

    @Override
    protected void onIntentionMoveTo(Location destination) {
    }

    @Override
    protected void onIntentionFollow(Creature target) {
    }

    @Override
    protected void onIntentionPickUp(WorldObject item) {
    }

    @Override
    protected void onIntentionInteract(WorldObject object) {
    }

    @Override
    protected void onEvtThink() {
    }

    @Override
    protected void onEvtAttacked(Creature attacker) {
        ThreadPoolManager.execute(new onEventAttackedDoorTask((Door) actor, attacker));
    }

    @Override
    protected void onEvtAggression(Creature target, int aggro) {
    }

    @Override
    protected void onEvtActionBlocked(Creature attacker) {
    }

    @Override
    protected void onEvtRooted(Creature attacker) {
    }

    @Override
    protected void onEvtReadyToAct() {
    }

    @Override
    protected void onEvtArrived() {
    }

    @Override
    protected void onEvtArrivedRevalidate() {
    }

    @Override
    protected void onEvtArrivedBlocked(Location blocked_at_loc) {
    }

    @Override
    protected void onEvtForgetObject(WorldObject object) {
    }

    @Override
    protected void onEvtCancel() {
    }

    @Override
    protected void onEvtDead() {
    }

    private class onEventAttackedDoorTask implements Runnable {
        private final Door _door;
        private final Creature _attacker;

        public onEventAttackedDoorTask(Door door, Creature attacker) {
            _door = door;
            _attacker = attacker;
        }

        @Override
        public void run() {
            World.getInstance().forEachVisibleObject(_door, Defender.class, guard ->
            {
                if (actor.isInsideRadius3D(guard, guard.getTemplate().getClanHelpRange())) {
                    guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 15);
                }
            });
        }
    }
}
