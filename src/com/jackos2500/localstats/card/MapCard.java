package com.jackos2500.localstats.card;

import com.afollestad.cardsui.Card;
import com.google.android.gms.maps.MapView;
import com.jackos2500.localstats.R;

public class MapCard extends Card {
	private static final long serialVersionUID = 1L;
	
	private MapView map;
	public MapCard(MapView map) {
		super("map", "map");
		this.map = map;
	}
	public MapView getMap() {
		return map;
	}
	@Override
	public int getLayout() {
		return R.layout.map_card;
	}
}
