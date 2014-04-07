package com.jackos2500.localstats.fragment;

import java.util.Map.Entry;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.cardsui.CardHeader;
import com.afollestad.cardsui.CardListView;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.card.EmptyCard;
import com.jackos2500.localstats.card.SavedQueryCard;
import com.jackos2500.localstats.data.disk.DataStore.DataQuerySet;
import com.jackos2500.localstats.ui.MultiCardAdapter;

public class SavedQueriesFragment extends Fragment {
	private CardListView list;
	private MultiCardAdapter listAdapter;
	
	private EmptyCard emptyCard;
	
	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		list = (CardListView)inflater.inflate(R.layout.card_layout, container, false);
		
		listAdapter = new MultiCardAdapter(getActivity());
		listAdapter.setAccentColorRes(android.R.color.holo_blue_light);
		
		listAdapter.add(new CardHeader("Saved Queries", "This is a list of queries you've saved."));
		
		for (Entry<String, DataQuerySet> e : MainActivity.dataStore.getQueries().entrySet()) {
			listAdapter.add(new SavedQueryCard(this, e.getKey(), e.getValue()));
		}
		emptyCard = new EmptyCard(0.7f, "No saved queries.");
		updateEmptyCard();
		
		list.setAdapter(listAdapter);
		
		return list;
	}
	public CardListView getList() {
		return list;
	}
	public MultiCardAdapter getListAdapter() {
		return listAdapter;
	}
	@SuppressWarnings("unchecked")
	public void updateEmptyCard() {
		boolean empty = true;
		for (Object obj : listAdapter.getItems()) {
			if (obj instanceof SavedQueryCard) {
				empty = false;
			}
		}
		if (empty) {
			listAdapter.add(emptyCard);
		} else {
			listAdapter.remove(emptyCard);
		}
	}
}
