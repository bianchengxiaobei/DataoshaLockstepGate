package com.chen.login.message.res;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.login.bean.RoleAllInfo;
import com.chen.message.Message;

public class ResEnterLobbyClientMessage extends Message
{
	public RoleAllInfo roleAllInfo = new RoleAllInfo();
	public int isInBattle;
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
	public boolean write(IoBuffer buf) {
		writeBean(buf, this.roleAllInfo);
		writeInt(buf, isInBattle);
		return true;
	}

	@Override
	public boolean read(IoBuffer buf) {
		this.roleAllInfo = (RoleAllInfo)readBean(buf,RoleAllInfo.class);
		return true;
	}

}
