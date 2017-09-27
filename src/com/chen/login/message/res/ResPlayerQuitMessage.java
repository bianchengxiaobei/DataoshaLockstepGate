package com.chen.login.message.res;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Message;
/**
 * 服务器发送给客户端玩家退出服务区消息
 * @author Administrator
 *
 */
public class ResPlayerQuitMessage extends Message
{
	public int bIsForced;
	@Override
	public int getId() {
		return 10035;
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
		this.bIsForced = readInt(buffer);
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer) {
		writeInt(buffer, bIsForced);		
		return true;
	}

}
