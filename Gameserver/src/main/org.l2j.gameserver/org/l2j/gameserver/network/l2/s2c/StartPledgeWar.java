package org.l2j.gameserver.network.l2.s2c;

public class StartPledgeWar extends L2GameServerPacket
{
	private String _pledgeName;
	private String _char;

	public StartPledgeWar(String pledge, String charName)
	{
		_pledgeName = pledge;
		_char = charName;
	}

	@Override
	protected final void writeImpl()
	{
		writeString(_char);
		writeString(_pledgeName);
	}
}