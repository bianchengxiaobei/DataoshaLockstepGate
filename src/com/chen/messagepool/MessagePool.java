package com.chen.messagepool;

import java.util.HashMap;

import com.chen.command.Handler;
import com.chen.login.handler.ReqCreateCharacterHandler;
import com.chen.login.handler.ReqLoginHandler;
import com.chen.login.handler.ReqRemoveCharacterToGateHandler;
import com.chen.login.handler.ReqSelectCharacterHandler;
import com.chen.login.handler.ResEnterLobbyHandler;
import com.chen.login.handler.ResLoginSuccessToGateHandler;
import com.chen.login.message.req.ReqCreateCharacterMessage;
import com.chen.login.message.req.ReqLoginMessage;
import com.chen.login.message.req.ReqRemoveCharacterToGateMessage;
import com.chen.login.message.req.ReqSelectCharacterMessage;
import com.chen.login.message.res.ResEnterLobbyMessage;
import com.chen.login.message.res.ResLoginSuccessToGateMessage;
import com.chen.message.Message;
import com.chen.server.handler.ReqHeartHandler;
import com.chen.server.handler.ReqRegisterGateHandler;
import com.chen.server.message.req.ReqHeartMessage;
import com.chen.server.message.req.ReqRegisterGateMessage;

public class MessagePool 
{
	HashMap<Integer, Class<?>> messages = new HashMap<Integer, Class<?>>();
	HashMap<Integer, Class<?>> handlers = new HashMap<Integer, Class<?>>();
	public MessagePool()
	{
		register(10003, ReqRegisterGateMessage.class,ReqRegisterGateHandler.class);
		register(10008, ResLoginSuccessToGateMessage.class, ResLoginSuccessToGateHandler.class);
		register(10036, ReqRemoveCharacterToGateMessage.class, ReqRemoveCharacterToGateHandler.class);
		register(10005, ResEnterLobbyMessage.class, ResEnterLobbyHandler.class);
		
		register(0, ReqHeartMessage.class, ReqHeartHandler.class);
		register(1001, ReqLoginMessage.class, ReqLoginHandler.class);
		register(1004, ReqCreateCharacterMessage.class, ReqCreateCharacterHandler.class);

		register(1006, ReqSelectCharacterMessage.class, ReqSelectCharacterHandler.class);
	}
	private void register(int id,Class<?> messageClass,Class<?> handlerClass)
	{
		messages.put(id, messageClass);
		if (handlerClass != null)
		{
			handlers.put(id, handlerClass);
		}
	}
	public Message getMessage(int id) throws InstantiationException, IllegalAccessException 
	{
		if (!messages.containsKey(id))
		{
			return null;
		}
		else
		{
			return (Message)messages.get(id).newInstance();
		}
	}
	public Handler getHandler(int id) throws InstantiationException, IllegalAccessException
	{
		if (!handlers.containsKey(id))
		{
			return null;
		}
		else 
		{
			return (Handler)handlers.get(id).newInstance();
		}
	}
}
