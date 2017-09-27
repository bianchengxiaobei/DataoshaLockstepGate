package com.chen.login.message.req;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Message;

public class ReqSelectCharacterMessage extends Message
{
	public long playerId;
	@Override
	public int getId()
	{		
		return 1006;
	}

	@Override
	public String getQueue() {
		
		return null;
	}

	@Override
	public String getServer() {
		
		return null;
	}

	@Override
	public boolean read(IoBuffer buffer) {
		this.playerId = readLong(buffer);
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer) {
		writeLong(buffer, playerId);
		return true;
	}
}
