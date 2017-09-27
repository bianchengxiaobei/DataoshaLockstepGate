package com.chen.player.message.res;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Message;
/**
 * 服务器发送给客户端创建角色失败的消息
 * @author Administrator
 *
 */
public class ResCreateRoleResultMessage extends Message
{
	public int errCode;
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 1005;
	}

	@Override
	public String getQueue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean read(IoBuffer buffer) 
	{
		this.errCode = readInt(buffer);
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer) 
	{
		writeInt(buffer, errCode);
		return true;
	}
	
}
