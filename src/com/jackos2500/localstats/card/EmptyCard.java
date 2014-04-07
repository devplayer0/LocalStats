package com.jackos2500.localstats.card;

import com.afollestad.cardsui.Card;
import com.jackos2500.localstats.R;

public class EmptyCard extends Card {
	private static final long serialVersionUID = 1L;
	
	private float displayHeightFraction;
	private String emptyText;
	
	public EmptyCard(float displayHeightPercentage, String emptyText) {
		super("");
		this.displayHeightFraction = displayHeightPercentage;
		this.emptyText = emptyText;
	}
	public float getDisplayHeightFraction() {
		return displayHeightFraction;
	}
	public void setDisplayHeightFraction(float displayHeightPercentage) {
		this.displayHeightFraction = displayHeightPercentage;
	}
	public String getEmptyText() {
		return emptyText;
	}
	public void setEmptyText(String emptyText) {
		this.emptyText = emptyText;
	}
	@Override
	public int getLayout() {
		return R.layout.empty_card;
	}
}
