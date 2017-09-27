package com.chen.login.handler;

import org.apache.mina.core.session.IoSession;

import com.chen.command.Handler;
import com.chen.login.message.res.ResEnterLobbyClientMessage;
import com.chen.login.message.res.ResEnterLobbyMessage;
import com.chen.server.GateServer;
import com.chen.util.MessageUtil;

public class ResEnterLobbyHandler extends Handler
{
	
	@Override
	public void action()
	{
		ResEnterLobbyMessage message = (ResEnterLobbyMessage)getMessage();
		IoSession session = GateServer.getInstance().getSessionByUser(message.server, message.userId);
		ResEnterLobbyClientMessage message2 = new ResEnterLobbyClientMessage();
		message2.roleAllInfo = message.roleAllInfo;
		message2.isInBattle = message.isInBattle;
		if (session != null)
		{
			session.write(message2);
		}
	}
}
