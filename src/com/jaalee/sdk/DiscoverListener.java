package com.jaalee.sdk;

import com.jaalee.sdk.Sticker;

/**
 * Callback to be invoked when beacons are ranged.
 *
 */
public abstract interface DiscoverListener
{
  public abstract void onStickersDiscovered(Sticker param);
}