package com.chen.login.bean;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Bean;

public class CharacterInfo extends Bean
{
	public long playerId;
	public String name;
	public int level;
	public byte sex;
	public int icon;
	@Override
	public boolean read(IoBuffer buf) {
		this.playerId = readLong(buf);
		this.name = readString(buf);
		this.level = readInt(buf);
		this.sex = readByte(buf);
		this.icon = readInt(buf);
		return true;
	}
	@Override
	public boolean write(IoBuffer buf) {
		writeLong(buf, playerId);
		writeString(buf, name);
		writeInt(buf, level);
		writeByte(buf, sex);
		writeInt(buf, icon);
		return true;
	}
}
