package com.jackos2500.localstats.fragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.cardsui.CardListView;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.card.ResultsCard;
import com.jackos2500.localstats.data.disk.DataStore.DataQuery;
import com.jackos2500.localstats.data.disk.DataStore.DataQuerySet;
import com.jackos2500.localstats.ui.Dialogs;
import com.jackos2500.localstats.ui.Dialogs.EditTextDialogFragment.IEditTextDialog;
import com.jackos2500.localstats.ui.MultiCardAdapter;

public class ResultsFragment extends Fragment implements IEditTextDialog {
	private CardListView list;
	private MultiCardAdapter listAdapter;
	
	private DataQuerySet queries;
	
	private boolean allowSave;
	
	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		
		list = (CardListView)inflater.inflate(R.layout.card_layout, container, false);
		
		listAdapter = new MultiCardAdapter(getActivity());
		listAdapter.setAccentColorRes(android.R.color.holo_blue_light);
		
		queries = new DataQuerySet();
		
		Bundle args = getArguments();
		List<String> eds = args.getStringArrayList("eds");
		queries.eds = eds;
		
		allowSave = args.getBoolean("allowSave");
		
		MainActivity activity = (MainActivity)getActivity();
		activity.setMenuItemEnabled(MainActivity.ITEM_RESULTS, R.id.action_save, allowSave);
		
		Bundle queriesNode = args.getBundle("queries");
		for (String key : queriesNode.keySet()) {
			Bundle queryNode = queriesNode.getBundle(key);
			
			String measure = queryNode.getString("measure");
			String dataset = queryNode.getString("dataset");
			
			Bundle valuesNode = queryNode.getBundle("values");
			Map<String, String> values = new HashMap<String, String>();
			for (String dimension : valuesNode.keySet()) {
				values.put(dimension, valuesNode.getString(dimension));
			}
			
			DataQuery query = new DataQuery();
			query.dataset = dataset;
			query.measure = measure;
			query.values = values;
			queries.queries.add(query);
			
			listAdapter.add(new ResultsCard(getActivity(), this, eds, measure, dataset, values));
		}
		
		list.setAdapter(listAdapter);
		
		return list;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_save && allowSave) {
			Dialogs.EditTextDialogFragment.newInstance(R.string.save_query, R.string.enter_query_name, "", this).show(getFragmentManager(), "SaveQueryDialogFragment");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	public CardListView getList() {
		return list;
	}
	public MultiCardAdapter getListAdapter() {
		return listAdapter;
	}
	@Override
	public boolean canSave(String name) {
		return !MainActivity.dataStore.containsQuery(name);
	}
	@Override
	public void onSave(String name) {
		MainActivity.dataStore.addQuery(name, queries);
		Toast.makeText(getActivity(), "Saved query \""+name+"\"", Toast.LENGTH_SHORT).show();
	}
}
