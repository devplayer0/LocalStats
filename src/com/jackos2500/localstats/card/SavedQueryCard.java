package com.jackos2500.localstats.card;

import java.util.UUID;

import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.cardsui.Card;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.disk.DataStore.DataQuerySet;
import com.jackos2500.localstats.fragment.ResultsFragment;
import com.jackos2500.localstats.fragment.SavedQueriesFragment;
import com.jackos2500.localstats.ui.Dialogs;
import com.jackos2500.localstats.ui.Dialogs.EditTextDialogFragment.IEditTextDialog;

@SuppressWarnings("unchecked")
public class SavedQueryCard extends Card implements IEditTextDialog {
	private static final long serialVersionUID = 1L;
	
	private UUID uuid;
	
	private SavedQueriesFragment savedQueriesFragment;
	private String name;
	private DataQuerySet query;
	
	private OnClickListener onClickListener;
	
	public SavedQueryCard(final SavedQueriesFragment savedQueriesFragment, String name, DataQuerySet query) {
		super("");
		this.savedQueriesFragment = savedQueriesFragment;
		this.name = name;
		this.query = query;
		
		uuid = UUID.randomUUID();
		
		onClickListener = new OnClickListener(savedQueriesFragment);
		
		setPopupMenu(R.menu.saved_query_card, new Card.CardMenuListener<Card>() {
			@Override
			public void onMenuItemClick(Card card, MenuItem item) {
				if (item.getItemId() == R.id.action_rename) {
					Dialogs.EditTextDialogFragment.newInstance(R.string.rename_query, R.string.enter_new_name, getName(), SavedQueryCard.this).show(savedQueriesFragment.getFragmentManager(), "RenameQueryDialogFragment");
				}
				if (item.getItemId() == R.id.action_delete) {
					savedQueriesFragment.getListAdapter().remove(card);
					MainActivity.dataStore.removeQuery(getName());
					
					savedQueriesFragment.updateEmptyCard();
				}
			}
		});
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public DataQuerySet getQuery() {
		return query;
	}
	public OnClickListener getOnClickListener() {
		return onClickListener;
	}
	public UUID getUUID() {
		return uuid;
	}
	@Override
	public boolean equalTo(Card other) {
		if (other instanceof SavedQueryCard) {
			SavedQueryCard card = (SavedQueryCard)other;
			return card.getUUID().equals(uuid);
		}
		return false;
	}
	@Override
	public int getLayout() {
		return R.layout.saved_query_card;
	}
	public class OnClickListener implements View.OnClickListener {
		private SavedQueriesFragment savedQueriesFragment;
		public OnClickListener(SavedQueriesFragment savedQueriesFragment) {
			this.savedQueriesFragment = savedQueriesFragment;
		}
		@Override
		public void onClick(View v) {
			ResultsFragment resultsFragment = Util.getResultsFragment(query, false);
			((MainActivity)savedQueriesFragment.getActivity()).addTopLevelFragment(resultsFragment, MainActivity.ITEM_RESULTS, "ResultsFragment");
		}
	}
	@Override
	public boolean canSave(String name) {
		return !MainActivity.dataStore.containsQuery(name);
	}
	@Override
	public void onSave(String newName) {
		MainActivity.dataStore.renameQuery(name, newName);
		Toast.makeText(savedQueriesFragment.getActivity(), "Renamed query \""+name+"\" to \""+newName+"\"", Toast.LENGTH_SHORT).show();
		
		setName(newName);
		Util.reloadCard(savedQueriesFragment.getList(), savedQueriesFragment.getListAdapter(), this);
	}
}
