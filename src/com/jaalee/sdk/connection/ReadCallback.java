package com.jaalee.sdk.connection;

import android.R.integer;

public abstract interface ReadCallback
{
	public abstract void onReadStringValue(String value);
	public abstract void onReadIntegerValue(Integer value);
	public abstract void onError();
}