/*
 * Copyright © 2019-2020 L2JOrg
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
package org.l2j.scripts.handlers.usercommandhandlers;

import org.l2j.gameserver.engine.olympiad.Olympiad;
import org.l2j.gameserver.handler.IUserCommandHandler;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.network.SystemMessageId;

import static org.l2j.gameserver.network.SystemMessageId.FOR_THE_CURRENT_OLYMPIAD_YOU_HAVE_PARTICIPATED_IN_S1_MATCH_ES_AND_HAD_S2_WIN_S_AND_S3_DEFEAT_S_YOU_CURRENTLY_HAVE_S4_OLYMPIAD_POINT_S;

/**
 * Olympiad Stat user command.
 * @author JoeAlisson
 */
public class OlympiadStat implements IUserCommandHandler {

	private static final int[] COMMAND_IDS = {
		109
	};
	
	@Override
	public boolean useUserCommand(int id, Player player) {
		if (!Olympiad.getInstance().checkLevelAndClassResction(player)) {
			player.sendPacket(SystemMessageId.THIS_COMMAND_IS_AVAILABLE_ONLY_WHEN_THE_TARGET_HAS_COMPLETED_THE_2ND_CLASS_TRANSFER);
			return false;
		}
		Olympiad.getInstance().showRecord(player);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
