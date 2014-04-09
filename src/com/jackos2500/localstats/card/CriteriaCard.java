package com.jackos2500.localstats.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.afollestad.cardsui.Card;
import com.afollestad.silk.adapters.SilkSpinnerAdapter;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.DataAccess;
import com.jackos2500.localstats.data.IDataLoader.DatasetsListener;
import com.jackos2500.localstats.data.IDataLoader.MeasureAndDimensionsListener;
import com.jackos2500.localstats.fragment.QueryFragment;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;

public class CriteriaCard extends Card {
	private static final long serialVersionUID = 1L;
	
	private Context context;
	private QueryFragment queryFragment;
	private DataAccess dataAccess;
	
	private UUID uuid;
	
	private String selectedDataset;
	
	private DatasetsAdapter datasetsAdapter;
	private DatasetSelectedListener datasetSelectedListener;
	
	private MeasureAndDimensions mAndDs;
	private List<String> dimensions;
	private Map<ValuesAdapter, Integer> valuesAdapters;
	
	public CriteriaCard(Context context, final QueryFragment queryFragment) {
		super("");
		
		this.context = context;
		this.queryFragment = queryFragment;
		
		datasetSelectedListener = new DatasetSelectedListener();
		datasetsAdapter = new DatasetsAdapter(context);
		dimensions = new ArrayList<String>();
		valuesAdapters = new HashMap<ValuesAdapter, Integer>();
		
		uuid = UUID.randomUUID();
		
		dataAccess = MainActivity.dataAccess;
		dataAccess.loadDatasets(new DatasetsListener() {
			@Override
			public void onDatasetsReceived(Map<String, String> datasets) {
				datasetsAdapter.setDatasets(datasets);
				if (selectedDataset == null) {
					selectedDataset = datasetsAdapter.getDatasetIriAt(0);
				}
				reloadDimensions();
			}
		}, context, queryFragment.getFragmentManager());
		
		setPopupMenu(R.menu.criteria_card, new Card.CardMenuListener<Card>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onMenuItemClick(Card card, MenuItem item) {
				queryFragment.getListAdapter().remove(card);
				
				boolean empty = true;
				for (Object obj : queryFragment.getListAdapter().getItems()) {
					if (obj instanceof CriteriaCard) {
						empty = false;
					}
				}
				if (empty) {
					queryFragment.updateEmptyCard(true);
				}
			}
		});
	}
	public UUID getUUID() {
		return uuid;
	}
	@Override
	public boolean equalTo(Card other) {
		if (other instanceof CriteriaCard) {
			CriteriaCard card = (CriteriaCard)other;
			return card.getUUID().equals(uuid);
		}
		return false;
	}
	@Override
	public int getLayout() {
		return R.layout.criteria_card;
	}
	public String getSelectedDataset() {
		return selectedDataset;
	}
	public AdapterView.OnItemSelectedListener getDatasetSelectedListener() {
		datasetSelectedListener.reset();
		return datasetSelectedListener;
	}
	public DatasetsAdapter getDatasetsAdapter() {
		return datasetsAdapter;
	}
	public List<String> getDimensions() {
		return dimensions;
	}
	public ValuesAdapter getValuesAdapter(int dimension) {
		for (ValuesAdapter a : valuesAdapters.keySet()) {
			if (a.getDimension() == dimension) {
				return a;
			}
		}
		return null;
	}
	public void putSelectedValue(int dimension, int value) {
		ValuesAdapter adapter = null;
		for (ValuesAdapter a : valuesAdapters.keySet()) {
			if (a.getDimension() == dimension) {
				adapter = a;
				break;
			}
		}
		if (adapter != null) {
			valuesAdapters.put(adapter, value);
		}
	}
	public int getSelectedValue(int dimension) {
		ValuesAdapter adapter = getValuesAdapter(dimension);
		if (adapter != null) {
			return valuesAdapters.get(adapter);
		}
		return -1;
	}
	public ValueSelectedListener getValueSelectedListener(int dimension) {
		return new ValueSelectedListener(dimension);
	}
	
	public String getMeasure() {
		if (mAndDs == null) {
			return null;
		}
		return mAndDs.measureIri;
	}
	public Map<String, String> getSelectedValueIris() {
		Map<String, String> values = new HashMap<String, String>();
		for (Entry<ValuesAdapter, Integer> e : valuesAdapters.entrySet()) {
			int dimensionIndex = e.getKey().getDimension();
			int valueIndex = e.getValue();
			
			String dimension = null;
			int j = 0;
			for (String d : mAndDs.dimensions.keySet()) {
				if (j == dimensionIndex) {
					dimension = d;
					break;
				}
				j++;
			}
			
			String value = null;
			int k = 0;
			for (String v : mAndDs.dimensionValues.get(dimension).keySet()) {
				if (k == valueIndex) {
					value = v;
					break;
				}
				k++;
			}
			values.put(dimension, value);
		}
		return values;
	}
	
	private void reloadDimensions() {
		if (getSelectedDataset() == null) {
			return;
		}
		System.out.println("reloading dimensions...");
		dimensions.clear();
		valuesAdapters.clear();
		
		MainActivity.dataAccess.loadMeasureAndDimensions(new MeasureAndDimensionsListener() {
			@Override
			public void onMeasureAndDimensionsReceived(String datasetIri, MeasureAndDimensions mAndD) {
				mAndDs = mAndD;
				
				int i = 0;
				for (Entry<String, String> entry : mAndD.dimensions.entrySet()) {
					dimensions.add(entry.getValue());
					Map<String, String> values = mAndD.dimensionValues.get(entry.getKey());
					
					ValuesAdapter valuesAdapter = new ValuesAdapter(context);
					valuesAdapter.addAll(values.values());
					valuesAdapter.setDimension(i);
					
					valuesAdapters.put(valuesAdapter, 0);
					i++;
				}
				
				Util.reloadCard(queryFragment.getList(), queryFragment.getListAdapter(), CriteriaCard.this);
			}
		}, context, queryFragment.getFragmentManager(), getSelectedDataset());
		System.out.println("selected dataset: "+getSelectedDataset());
	}
	private class DatasetSelectedListener implements AdapterView.OnItemSelectedListener {
		private boolean firstRun = true;
		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
			DatasetsAdapter adapter = (DatasetsAdapter)parentView.getAdapter();
			if (firstRun) {
				firstRun = false;
				return;
			}
			if (getSelectedDataset().equals(adapter.getDatasetIriAt(position))) {
				return;
			}
			selectedDataset = adapter.getDatasetIriAt(position);
			reloadDimensions();
			
			System.out.println("selected dimension: "+position);
		}
		public void reset() {
			firstRun = true;
		}
		@Override
		public void onNothingSelected(AdapterView<?> parentView) {}
	}
	public static class DatasetsAdapter extends SilkSpinnerAdapter {
		private Map<String, Integer> positionMap;
		private Map<String, String> reversedDatasets;
		public DatasetsAdapter(Context context) {
			super(context);
			positionMap = new HashMap<String, Integer>();
			reversedDatasets = new HashMap<String, String>();
		}
		public String getDatasetIriAt(int position) {
			return reversedDatasets.get(getItem(position));
		}
		public int getPositionForDatasetIri(String datasetIri) {
			return positionMap.get(datasetIri);
		}
		public void setDatasets(Map<String, String> datasets) {
			clear();
			reversedDatasets.clear();
			positionMap.clear();
			
			addAll(datasets.values());
			int i = 0;
			for (Entry<String, String> e : datasets.entrySet()) {
				positionMap.put(e.getKey(), i);
				reversedDatasets.put(e.getValue(), e.getKey());
				i++;
			}
		}
	}
	public static class ValuesAdapter extends SilkSpinnerAdapter {
		private int dimension;
		public ValuesAdapter(Context context) {
			super(context);
		}
		public void setDimension(int dimension) {
			this.dimension = dimension;
		}
		public int getDimension() {
			return dimension;
		}
	}
	private class ValueSelectedListener implements AdapterView.OnItemSelectedListener {
		private boolean firstRun = true;
		
		private int dimension;
		private ValueSelectedListener(int dimension) {
			this.dimension = dimension;
		}
		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
			if (firstRun) {
				firstRun = false;
				return;
			}
			putSelectedValue(dimension, position);
			System.out.println("selected value: "+position);
		}
		@Override
		public void onNothingSelected(AdapterView<?> parentView) {}
	}
}
