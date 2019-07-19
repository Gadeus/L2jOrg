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
package handlers.targethandlers;

import org.l2j.gameserver.geoengine.GeoEngine;
import org.l2j.gameserver.handler.ITargetTypeHandler;
import org.l2j.gameserver.model.WorldObject;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.skills.Skill;
import org.l2j.gameserver.model.skills.targets.TargetType;
import org.l2j.gameserver.network.SystemMessageId;
import org.l2j.gameserver.util.MathUtil;

/**
 * Target dead monster.
 * @author Nik
 */
public class NpcBody implements ITargetTypeHandler
{
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.NPC_BODY;
	}
	
	@Override
	public WorldObject getTarget(Creature activeChar, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (selectedTarget == null)
		{
			return null;
		}
		
		if (!selectedTarget.isCharacter())
		{
			return null;
		}
		
		if (!selectedTarget.isNpc() && !selectedTarget.isSummon())
		{
			if (sendMessage)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			return null;
		}
		
		final Creature cha = (Creature) selectedTarget;
		if (cha.isDead())
		{
			// Check for cast range if character cannot move. TODO: char will start follow until within castrange, but if his moving is blocked by geodata, this msg will be sent.
			if (dontMove)
			{
				if (!MathUtil.isInsideRadius2D(activeChar, cha, skill.getCastRange()))
				{
					if (sendMessage)
					{
						activeChar.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
					}
					return null;
				}
			}
			
			// Geodata check when character is within range.
			if (!GeoEngine.getInstance().canSeeTarget(activeChar, cha))
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				}
				return null;
			}
			return cha;
		}
		
		// If target is not dead or not player/pet it will not even bother to walk within range, unlike Enemy target type.
		if (sendMessage)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
		return null;
	}
}
