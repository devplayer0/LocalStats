package com.jackos2500.localstats.card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;

import com.afollestad.cardsui.Card;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.DataAccess;
import com.jackos2500.localstats.data.IDataLoader.DataListener;
import com.jackos2500.localstats.data.IDataLoader.DatasetsListener;
import com.jackos2500.localstats.data.IDataLoader.MeasureAndDimensionsListener;
import com.jackos2500.localstats.fragment.ResultsFragment;
import com.jackos2500.localstats.tasks.DataLoadTask.Data;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;

public class ResultsCard extends Card {
	private static final long serialVersionUID = 1L;
	
	private ReadableData readableData;
	
	public ResultsCard(final Context context, final ResultsFragment resultsFragment, final List<String> eds, final String measure, final String dataset, final Map<String, String> values) {
		readableData = new ReadableData();
		final DataAccess dataAccess = MainActivity.dataAccess;
		dataAccess.loadDatasets(new DatasetsListener() {
			@Override
			public void onDatasetsReceived(Map<String, String> datasets) {
				readableData.dataset = datasets.get(dataset);
				
				dataAccess.loadMeasureAndDimensions(new MeasureAndDimensionsListener() {
					@Override
					public void onMeasureAndDimensionsReceived(String datasetIri, MeasureAndDimensions mAndD) {
						readableData.measure = mAndD.measureName;
						for (Entry<String, String> e : values.entrySet()) {
							String dimensionIri = e.getKey();
							String valueIri = e.getValue();
							
							String k = mAndD.dimensions.get(dimensionIri);
							String v = mAndD.dimensionValues.get(dimensionIri).get(valueIri);
							readableData.values.put(k, v);
						}
						
						dataAccess.loadData(new DataListener() {
							@Override
							public void onDataReceived(Data data) {
								readableData.count = data.count;
								readableData.total = data.total;
								
								Util.reloadCard(resultsFragment.getList(), resultsFragment.getListAdapter(), ResultsCard.this);
							}
						}, context, resultsFragment.getFragmentManager(), eds, dataset, measure, values);
					}
				}, context, resultsFragment.getFragmentManager(), dataset);
			}
		}, context, resultsFragment.getFragmentManager());
	}
	public ReadableData getReadableData() {
		return readableData;
	}
	@Override
	public int getLayout() {
		return R.layout.results_card;
	}
	public class ReadableData {
		public String measure;
		public String dataset;
		public Map<String, String> values;
		public int count;
		public int total;
		
		public ReadableData() {
			values = new HashMap<String, String>();
		}
	}
}
