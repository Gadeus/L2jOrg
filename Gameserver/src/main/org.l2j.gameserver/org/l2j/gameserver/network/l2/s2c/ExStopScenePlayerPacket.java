package org.l2j.gameserver.network.l2.s2c;

/**
 * @author Bonux
 */
public class ExStopScenePlayerPacket extends L2GameServerPacket
{
	private final int _movieId;

	public ExStopScenePlayerPacket(int movieId)
	{
		_movieId = movieId;
	}

	@Override
	protected final void writeImpl()
	{
		writeInt(_movieId);
	}
}