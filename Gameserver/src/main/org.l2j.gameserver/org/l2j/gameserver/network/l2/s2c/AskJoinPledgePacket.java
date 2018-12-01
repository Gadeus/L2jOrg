package org.l2j.gameserver.network.l2.s2c;

public class AskJoinPledgePacket extends L2GameServerPacket
{
	private int _requestorId;
	private String _pledgeName;

	public AskJoinPledgePacket(int requestorId, String pledgeName)
	{
		_requestorId = requestorId;
		_pledgeName = pledgeName;
	}

	@Override
	protected final void writeImpl()
	{
		writeInt(_requestorId);
		writeString("");
		writeString(_pledgeName);
		writeInt(0);
		writeString("");
	}
}