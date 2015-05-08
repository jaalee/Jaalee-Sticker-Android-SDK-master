package com.jaalee.sdk.connection;

import com.jaalee.sdk.internal.Objects;

/**
 * Value object for beacon's characteristics (battery level, broadcasting power, advertising interval, software and hardware version)
 */
public class StickerCharacteristics
{
	private final String StickerName;
	private final Integer BatteryPercent;
	
	public StickerCharacteristics(JaaleeService JLService, StickerNameService Name, StickerTxPower txPower, StickerStateService state, BatteryLifeService BattLevel)
	{
		this.StickerName = Name.getStickerName().replaceAll(" ", "");
		this.BatteryPercent = BattLevel.getBatteryPercent();
	}
	
	public String getStickerName()
	{
		return this.StickerName;
	}
	
	public Integer getBatteryPercent()
	{
		return this.BatteryPercent;
	}

	public String toString() {
		return Objects.toStringHelper(this)
				.add("StickerName", this.StickerName).toString();
	}
}
