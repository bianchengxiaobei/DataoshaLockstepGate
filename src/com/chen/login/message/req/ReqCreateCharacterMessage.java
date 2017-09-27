package com.chen.login.message.req;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Message;

/**
 * 客户端向网关服务器发送创建角色请求消息1004
 * @author chen
 *
 */
public class ReqCreateCharacterMessage extends Message
{
	public String name;
	public int icon;
	public byte sex;
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 1004;
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
	public boolean read(IoBuffer buf) {
		this.name = readString(buf);
		this.icon = readInt(buf);
		this.sex = readByte(buf);
		return true;
	}
	@Override
	public boolean write(IoBuffer buf) {
		writeString(buf, name);
		writeInt(buf, icon);
		writeByte(buf,this.sex);
		return true;
	}
}
