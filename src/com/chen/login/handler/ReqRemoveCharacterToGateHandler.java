package com.chen.login.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.chen.command.Handler;
import com.chen.login.message.req.ReqRemoveCharacterToGateMessage;
import com.chen.player.manager.PlayerManager;

public class ReqRemoveCharacterToGateHandler extends Handler
{
	private Logger log = LogManager.getLogger(ReqRemoveCharacterToGateHandler.class);
	@Override
	public void action()
	{
		try 
		{
			ReqRemoveCharacterToGateMessage message = (ReqRemoveCharacterToGateMessage)getMessage();
			log.debug("移除网关角色");
			PlayerManager.getInstance().removePlayer(message.playerId);
		} 
		catch (Exception e) 
		{
			log.error(e);
		}
	}

}
