package com.jaalee.sdk.connection;

import java.util.UUID;
/**
 * @author JAALEE, Inc
 * 
 * @Support dev@jaalee.com
 * @Sales: sales@jaalee.com
 * 
 * @see http://www.jaalee.com/
 */
public class JaaleeUuid
{
	public static final UUID JAALEE_STICKER_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_KEEP_CONNECT_CHAR = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
	
	public static final UUID STICKER_ALERT = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_ALERT_CHAR = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

	public static final UUID STICKER_NAME = UUID.fromString("0000ff80-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_NAME_CHAR = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_NEW_NAME_CHAR = UUID.fromString("00002a90-0000-1000-8000-00805f9b34fb");

	
	public static final UUID STICKER_BATTERY_LIFE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_BATTERY_LIFE_CHAR = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	
	//新版中有的 STICKER状态控制
	public static final UUID STICKER_STATE_SERVICE = UUID.fromString("0000ff70-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_STATE_CHAR = UUID.fromString("00002a80-0000-1000-8000-00805f9b34fb");
	
	//	Beacon铃声控制
	public static final UUID STICKER_AUDIO_STATE_SERVICE = UUID.fromString("0000FF60-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_AUDIO_STATE_CHAR = UUID.fromString("00002a70-0000-1000-8000-00805f9b34fb");
	
	//发射功率
	public static final UUID STICKER_TX_POWER_SERVICE = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_TX_POWER_CHAR = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
	
	//断开连接报警
	public static final UUID STICKER_LINK_LOSS_SERVICE = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
	public static final UUID STICKER_LINK_LOSS_CHAR = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
	
}
