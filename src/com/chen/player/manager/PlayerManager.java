package com.chen.player.manager;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.chen.config.Config;
import com.chen.db.bean.Role;
import com.chen.db.bean.User;
import com.chen.db.dao.RoleDao;
import com.chen.db.dao.UserDao;
import com.chen.login.bean.CharacterInfo;
import com.chen.login.message.req.ReqCreateCharacterToGameServerMessage;
import com.chen.login.message.req.ReqLoginCharacterToGameServerMessage;
import com.chen.login.message.res.ResCharacterInfosMessage;
import com.chen.login.message.res.ResLoginMessage;
import com.chen.login.message.res.ResPlayerQuitMessage;
import com.chen.login.message.res.ResSubstituteMessage;
import com.chen.player.message.res.ResCreateRoleResultMessage;
import com.chen.player.structs.Player;
import com.chen.player.structs.UserState;
import com.chen.server.GateServer;
import com.chen.util.MessageUtil;
import com.chen.util.SessionUtil;
/**
 * 玩家管理器，主要负责玩家登陆，创建角色的处理
 * @author chen
 *
 */
public class PlayerManager 
{
	private static volatile int sessionId = 0;
	private Logger log = LogManager.getLogger(PlayerManager.class);
	private static Logger closelog = LogManager.getLogger("GateSessionClose");
	private static Object obj = new Object();
	private static PlayerManager manager;
	//第一层key=>服务器id，第二层key=>userid
	private static ConcurrentHashMap<Integer, ConcurrentHashMap<String, Player>> user_players = new ConcurrentHashMap<Integer, ConcurrentHashMap<String,Player>>();
	//key=>玩家id（根据服务器id+时间随机生成唯一id）value=>Player
	private static ConcurrentHashMap<Long, Player> players = new ConcurrentHashMap<Long, Player>();
	//第一层key=>服务器id，第二层key=>userid
	private static ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> user_states = new ConcurrentHashMap<Integer, ConcurrentHashMap<String,Integer>>();
	
	public static int MAX_PLAYER = 4000;
	private PlayerManager()
	{
		
	}
	public static PlayerManager getInstance()
	{
		synchronized (obj)
		{
			if (manager == null)
			{
				manager = new PlayerManager();
			}			
		}
		return manager;
	}
	/**
	 * 玩家开始登陆
	 * @param session
	 * @param createServer
	 * @param userId
	 * @param password
	 */
	public void login(IoSession session,int createServer,String userId,String password)
	{
		String ip = null;
		try {
			InetSocketAddress remoteAddress = (InetSocketAddress)session.getRemoteAddress();
			if (remoteAddress != null)
			{
				ip = remoteAddress.getAddress().getHostAddress();
			}
			//获取客户端的ip地址判断是否是非法的ip地址
			if (ip == null)
			{
				//登陆失败，回馈登陆失败的消息
				ResLoginMessage msg = new ResLoginMessage();
				//这里应该是通过配置文件来设置错误码
				msg.setErrorCode(1);
				session.write(msg);
				return;
			}
			User user = null;
			synchronized (obj)
			{
				UserDao dao = new UserDao();
				user = dao.select(userId, createServer);
				if (user != null)
				{
					//用户密码错误
					if(!user.getPassword().equals(password))
					{
						ResLoginMessage msg = new ResLoginMessage();
						msg.setErrorCode(2);
						session.write(msg);
						log.info("玩家："+userId+"密码错误，登陆失败");
						return;
					}
					//更新玩家最后登录时间到数据库
					user.setLastlogintime(System.currentTimeMillis());
					dao.update(user);//更新用户数据库的数据
				}
				else
				{
					//说明没有该用户
					user = new User();
					user.setCreatetime(System.currentTimeMillis());
					user.setLastlogintime(System.currentTimeMillis());
					user.setUserid(Config.getId());
					user.setUsername(userId);
					user.setServer(createServer);
					user.setIsForbid(0);
					user.setPassword(password);
					dao.insert(user);
					//插入数据库
					//写入数据库日志
				}
			}
			if (user.getIsForbid() != null && user.getIsForbid() == 1)
			{
				//说明该玩家被封号
				//发送登陆失败的回馈消息
				ResLoginMessage msg = new ResLoginMessage();
				msg.setErrorCode(6);
				return;
			}
			if (players.size() > MAX_PLAYER)
			{
				ResLoginMessage msg = new ResLoginMessage();
				msg.setErrorCode(4);
				//在线玩家太多，登陆失败
				return;
			}
			//用户user_id字符串形式，由服务器自动生成，当成功注册账号的时候
			userId = Long.toString(user.getUserid());
			//设置用户状态为登陆状态
			synchronized (user_states) 
			{
				int state = this.getUserState(createServer, userId);
				if (state != 0)
				{
					log.error("玩家Session["+session+"]，玩家ID["+userId+"] 状态："+getUserStateInfo(state));
					return ;
				}
				//设置玩家状态为登陆状态，在选择的服务器
				setUserState(createServer, userId, UserState.Logining.getValue());
				log.info("玩家Session["+session+"]，玩家ID["+userId+"] 设置状态为："+getUserStateInfo(UserState.Logining.getValue()));
			}
			//检测当前登陆的账号是否已经登录了
			IoSession oldSession = GateServer.getInstance().getSessionByUser(createServer, userId);
			//如果已经登录了
			if (oldSession != null && oldSession.getId() != session.getId())
			{
				closelog.error("玩家ID："+userId+"，IP["+oldSession.getRemoteAddress()+"]被踢下线,另一个玩家IP["+session.getRemoteAddress()+"]上线");
				if (session != null && session.getRemoteAddress() != null && oldSession.isConnected())
				{
					//给老上线的玩家发送一个被顶替下线的消息
					ResSubstituteMessage msg = new ResSubstituteMessage();
					try {
						msg.setIp(ip);
					} catch (Exception e) {
						log.error(e,e);
					}
					oldSession.write(msg);
				}
				//然后再移除网关服务器里面的玩家通信和用户通信
				long playerId = 0;
				if (oldSession.containsAttribute("player_id"))
				{
					try {
						playerId = ((Long)oldSession.removeAttribute("player_id")).longValue();
						GateServer.getInstance().removePlayerSession(playerId);
					} catch (Exception e) {
						log.error(e,e);
					}
				}
				if (oldSession.containsAttribute("user_id"))
				{
					try {
						String oldUserId = (String)oldSession.getAttribute("user_id");
						GateServer.getInstance().removeUserSession(oldUserId);
					} catch (Exception e) {
						log.error(e,e);
					}
				}
				//最后关闭连接,这里呢，我有点不想直接退出，我想客户端发送再退出
				SessionUtil.closeSession(oldSession, "被另一个玩家顶替下线",false);
			}
			//设置通信的sessionIp,sessionId,user_name属性
			try {
				session.setAttribute("session_ip", ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress());
			} catch (Exception e) {
				log.error(e,e);
			}
			sessionId++;
			//所在session队列号
			session.setAttribute("session_id", sessionId);
			//玩家账号
			session.setAttribute("user_name", user.getUsername());
			//注册当前玩家到网关服务器的User通信列表中
			GateServer.getInstance().registerUser(session, createServer, userId, 0);
			//当前服务器是否有角色在线
			ConcurrentHashMap<String, Player> players = user_players.get(createServer);
			if (players != null)
			{
				Player player = players.get(userId);
				if (player != null)
				{
					synchronized (user_states)
					{
						int state = removeUserState(createServer, userId);
					}
					//通知游戏服务器登陆角色
					log.debug("重连选择角色");
					selectCharacter(session, player.getId());
					return;
				}
			}	
			//查询当前用户可登陆角色列表
			RoleDao roleDao = new RoleDao();		
			List<Role> characters = roleDao.selectByUser(userId, createServer);
			ResCharacterInfosMessage msg = new ResCharacterInfosMessage();
			//如果玩家还没有角色
			if (characters != null)
			{
				for (int i = 0; i < characters.size(); i++) {
					Role role = characters.get(i);
					CharacterInfo info = new CharacterInfo();
					info.name = role.getName();
					info.playerId = role.getRoleid();
					info.level = role.getLevel();
					info.icon = role.getIcon();
					info.sex = (byte) (int) role.getSex();
					msg.getCharacters().add(info);
				}
			}	
			//发送角色列表给客户端，如果发送是空的话，客户端判断然后创建角色
			session.write(msg);
			synchronized (user_states)
			{
				int state = removeUserState(createServer, userId);
			}
			log.debug("玩家ID："+userId+"在服务器："+createServer+"登陆了");
		} catch (Exception e) {
			log.error(e,e);
			SessionUtil.closeSession(session, e.getMessage());
		}finally{
			synchronized (user_states) {
				int state = removeUserState(createServer, userId);
			}
		}		
	}
	
	/**
	 * 创建角色
	 * @param session
	 * @param name
	 * @param icon
	 */
	public void createCharacter(IoSession session,String name,int icon)
	{
		String userId = (String)session.getAttribute("user_id");
		int createServer = (Integer)session.getAttribute("server_id");
		int isAdult = 0;
		if (session.containsAttribute("is_adult"))
		{
			isAdult = (Integer)session.getAttribute("is_adult");
		}
		String username = "";
		if (session.containsAttribute("user_name"))
		{
			username = (String)session.getAttribute("user_name");
		}
		synchronized (user_states) 
		{
			int state = getUserState(createServer, userId);
			if (state != 0)
			{
				return;
			}
			setUserState(createServer, userId, UserState.Creating.getValue());
//			if (session.containsAttribute("precreatetime"))
//			{
//				long time = (long)session.getAttribute("precreatetime");
//				if (System.currentTimeMillis() - time < 1000)
//				{
//					//创建角色失败，创建间隔过短
//					ResCreateRoleResultMessage message = new ResCreateRoleResultMessage();
//					message.errCode = 9000;
//					session.write(message);
//					return ;
//				}
//			}
//			session.setAttribute("precreatetime", System.currentTimeMillis());
		}
		//发送消息到游戏服务器
		ReqCreateCharacterToGameServerMessage msg = new ReqCreateCharacterToGameServerMessage();
		msg.setGateId(GateServer.getInstance().getServer_id());
		msg.setCreateServer(createServer);
		msg.setUserId(userId);
		msg.setUserName(username);
		msg.setName(name);
		msg.setIcon(icon);
		msg.setIsAdult((byte)isAdult);
		String ip = "";
		try {
			ip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
		} catch (Exception e) {
			log.error(e,e);
		}
		msg.setOptIp(ip);
		if (session.containsAttribute("loginType"))
		{
			int loginType = (Integer)session.getAttribute("loginType");
			msg.setLoginType(loginType);
		}
		int sessionId = (Integer)session.getAttribute("session_id");
		boolean success = MessageUtil.sendMessageToGameServer(createServer, sessionId, msg);
		if (!success)
		{
			log.error("网关发送创建角色消息给游戏服务器的时候失败玩家Id："+userId);
			synchronized (this.user_states)
			{
				int state = removeUserState(createServer, userId);
			}
			//发送创建失败的消息
			ResCreateRoleResultMessage retrueMsg = new ResCreateRoleResultMessage();
			retrueMsg.errCode = 9001;
			session.write(retrueMsg);
		}		
		log.debug("开始创建角色");
	}
	
	/**
	 * 玩家选择角色
	 * @param session
	 * @param playerId
	 */
	public void selectCharacter(IoSession session,long playerId)
	{
		String userId = (String)session.getAttribute("user_id");
		String userName = (String)session.getAttribute("user_name");
		int createServer = (Integer)session.getAttribute("server_id");
		int isAdult = 0;
		if (session.containsAttribute("is_adult"))
		{
			isAdult = (Integer)session.getAttribute("is_adult");
		}	
		synchronized (user_states) 
		{
			int state = getUserState(createServer, userId);
			if (state != 0)
			{
				return ;
			}
			setUserState(createServer, userId, UserState.Selecting.getValue());
		}
		ReqLoginCharacterToGameServerMessage msg = new ReqLoginCharacterToGameServerMessage();
		msg.setGateId(GateServer.getInstance().getServer_id());
		msg.setServerId(createServer);
		msg.setUserId(userId);
		msg.setPlayerId(playerId);
		msg.setIsAdult((byte)isAdult);
		msg.setUserName(userName);
		if (session.containsAttribute("session_loginType"))
		{
			int loginType = (Integer)session.getAttribute("session_loginType");
			msg.setLoginType(loginType);
		}
		try {
			String hostAddress =((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
			msg.setLoginIp(hostAddress);
		} catch (Exception e) {
			log.error(e,e);
		}
		int server = 0;
		Player player = players.get(playerId);
		if (player != null)
		{
			server = player.getCreateServer();
		}
		else
		{
			RoleDao roleDao = new RoleDao();
			try {
				Role character = roleDao.selectById(playerId);
				System.out.println("hh:"+playerId);
				System.out.println("tt:"+character.getCreateServer());
				server = character.getCreateServer();
			} catch (Exception e) {
				log.error(e,e);
				SessionUtil.closeSession(session, e.getMessage());
				return ;
			} 
		}
		int sessionId = ((Integer)session.getAttribute("session_id")).intValue();
		boolean success = MessageUtil.sendMessageToGameServer(server, sessionId, msg);
		if (!success)
		{
			System.err.println("选择角色失败");
		}
	}
	/**
	 * 取得玩家所在服务器内的状态，登陆，创建，选择，退出等等，如果取不到就返回0
	 * @param server所在服务器
	 * @param userId用户id
	 * @return 状态码
	 */
	private int getUserState(int server,String userId)
	{
		if (user_states.containsKey(server))
		{
			ConcurrentHashMap<String, Integer> states = user_states.get(server);
			if (states.contains(userId))
			{
				return states.get(userId);
			}
		}
		return 0;
	}
	/**
	 * 根据状态码取得玩家的状态信息描述
	 * @param state
	 * @return 状态信息描述
	 */
	private String getUserStateInfo(int state)
	{
		switch (state) {
		case 1:
			return "logining";
		case 2:
			return "creating";
		case 3:
			return "selecting";
		case 4:
			return "waitquiting";
		case 5:
			return "quiting";
		default:
			return "";
		}
	}
	/**
	 * 在选择的服务器设置玩家状态
	 * @param server
	 * @param userId
	 * @param value
	 */
	private void setUserState(int server,String userId,int value)
	{
		if (user_states.containsKey(server))
		{
			user_states.get(server).put(userId, value);
		}
		else
		{
			ConcurrentHashMap<String, Integer> states = new ConcurrentHashMap<String, Integer>();
			states.put(userId, value);
			user_states.put(server, states);
		}
	}
	/**
	 * 移除用户状态
	 * @param server
	 * @param userId
	 * @return 成功该状态所在的索引，失败为0
	 */
	private int removeUserState(int server, String userId)
	{
		if (user_states.containsKey(server))
		{
			if (user_states.get(server).containsKey(userId))
			{
				return user_states.get(server).remove(userId);
			}
		}
		return 0;
	}
	/**
	 * 注册玩家
	 * @param server
	 * @param createServer
	 * @param userId
	 * @param playerId
	 */
	public void registerPlayer(int server,int createServer,String userId,long playerId)
	{
		Player player = null;
		if (players.containsKey(playerId))
		{
			player = players.get(playerId);
		}
		else
		{
			player = new Player();
			player.setId(playerId);
			players.put(playerId, player);
		}
		player.setCreateServer(createServer);
		player.setUserid(userId);
		IoSession session = GateServer.getInstance().getSessionByUser(createServer, userId);
		if (session == null || !session.isConnected())
		{
			//用户强制退出游戏，需要发送退出游戏消息给游戏服务器
			quit(player, true);
			return;
			
		}
		else if (session.getAttribute("player_id") == null)
		{
			GateServer.getInstance().registerRole(session, playerId);
		}
		ConcurrentHashMap<String, Player> sPlayers = user_players.get(createServer);
		if (sPlayers == null)
		{
			sPlayers = new ConcurrentHashMap<>();
			user_players.put(createServer, sPlayers);
		}
		sPlayers.put(userId, player);
		synchronized (user_states)
		{
			int state = removeUserState(createServer, userId);
		}
	}
	/**
	 * 移除Player出游戏服务器
	 * @param playerId
	 * @return
	 */
	public void removePlayer(long playerId)
	{
		Player player = players.remove(playerId);
		if (player != null)
		{
			//移除User_Player的缓存
			ConcurrentHashMap<String, Player> splayers = user_players.get(player.getCreateServer());
			if (splayers != null)
			{
				splayers.remove(player.getUserid());
			}
			//移除当前玩家的状态
			removeUserState(player.getCreateServer(), player.getUserid());
			IoSession session = GateServer.getInstance().getSessionByUser(player.getCreateServer(),player.getUserid());
			if (session != null)
			{
				if (session.containsAttribute("player_id"))
				{
					try {
						long roleId = (long)session.removeAttribute("player_id");
						GateServer.getInstance().removePlayerSession(roleId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (session.containsAttribute("user_id"))
				{
					try 
					{
						String userId = (String)session.removeAttribute("user_id");
						GateServer.getInstance().removeUserSession(userId);		
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				//关闭连接
				SessionUtil.closeSession(session, "玩家退出游戏");
			}
		}
	}
	/**
	 * 玩家退出游戏服务器
	 * @param session
	 */
	public void quit(IoSession session)
	{
		String userId = (String)session.getAttribute("user_id");
		int serverId = (int)session.getAttribute("server_id");
		ConcurrentHashMap<String, Player> sPlayers = user_players.get(serverId);
		if (sPlayers != null)
		{
			Player player = sPlayers.get(userId);
			if (player != null)
			{
				quit(player,true);
			}
		}
	}
	/**
	 * 发送玩家退出游戏消息给客户端
	 * @param player
	 * @param bForced
	 */
	public void quit(Player player,boolean bForced)
	{
		String userId = player.getUserid();
		int serverId = player.getCreateServer();
		
		ResPlayerQuitMessage msg = new ResPlayerQuitMessage();
		msg.bIsForced = bForced ? 1 : 0;
		msg.getRoleId().add(player.getId());
		int sessionId = 0;
		IoSession session = GateServer.getInstance().getSessionByUser(serverId, userId);
		if (session != null)
		{
			sessionId = (int)session.getAttribute("session_id");
		}
		MessageUtil.sendMessageToGameServer(player.getCreateServer(), sessionId, msg);
	}
	/**
	 * 根据角色id取得在线的角色实例
	 * @param playerId
	 * @return
	 */
	public Player getPlayer(long playerId)
	{
		return players.get(playerId);
	}
}
