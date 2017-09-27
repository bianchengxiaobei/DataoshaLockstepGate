package com.chen.server.handler;

import com.chen.command.Handler;
import com.chen.server.message.req.ReqHeartMessage;

public class ReqHeartHandler extends Handler
{
	@Override
	public void action()
	{
		try 
		{
			ReqHeartMessage message = (ReqHeartMessage)getMessage();
			//System.out.println("处理心跳");
			message.getSession().setAttribute("pre_heart",this.getCreateTime());
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}

}
