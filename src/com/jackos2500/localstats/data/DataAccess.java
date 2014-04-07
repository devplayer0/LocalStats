package com.jackos2500.localstats.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask.Status;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.jackos2500.localstats.data.disk.DataCache;
import com.jackos2500.localstats.fragment.QueryFragment;
import com.jackos2500.localstats.tasks.DataLoadTask;
import com.jackos2500.localstats.tasks.DataLoadTask.Data;
import com.jackos2500.localstats.tasks.DatasetsLoadTask;
import com.jackos2500.localstats.tasks.ElectoralDivisionsLoadTask;
import com.jackos2500.localstats.tasks.ElectoralDivisionsLoadTask.LocationRetrievable;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;

public class DataAccess implements IDataStore, IDataLoader {
	private Map<String, String> datasets;
	private Map<String, MeasureAndDimensions> mAndDs;
	private List<Data> data;
	
	private List<ElectoralDivisionsLoadTask> edLoadTasks;
	private List<DatasetsLoadTask> datasetsLoadTasks;
	private List<MeasureAndDimensionsLoadTask> mAndDsLoadTasks;
	private List<DataLoadTask> dataLoadTasks;
	
	private DataCache cache;
	
	public DataAccess(Context context) {
		mAndDs = new HashMap<String, MeasureAndDimensions>();
		data = new ArrayList<Data>();
		
		edLoadTasks = new CopyOnWriteArrayList<ElectoralDivisionsLoadTask>();
		datasetsLoadTasks = new CopyOnWriteArrayList<DatasetsLoadTask>();
		mAndDsLoadTasks = new CopyOnWriteArrayList<MeasureAndDimensionsLoadTask>();
		dataLoadTasks = new CopyOnWriteArrayList<DataLoadTask>();
		
		try {
			cache = new DataCache(context);
			cache.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void cleanUpTasks() {
		for (ElectoralDivisionsLoadTask t : edLoadTasks) {
			if (t.getStatus() == Status.FINISHED) {
				edLoadTasks.remove(t);
			}
		}
		for (DatasetsLoadTask t : datasetsLoadTasks) {
			if (t.getStatus() == Status.FINISHED) {
				datasetsLoadTasks.remove(t);
			}
		}
		for (MeasureAndDimensionsLoadTask t : mAndDsLoadTasks) {
			if (t.getStatus() == Status.FINISHED) {
				mAndDsLoadTasks.remove(t);
			}
		}
		for (DataLoadTask t : dataLoadTasks) {
			if (t.getStatus() == Status.FINISHED) {
				dataLoadTasks.remove(t);
			}
		}
	}
	
	@Override
	public void setDatasets(Map<String, String> datasets, DatasetsListener listener) {
		this.datasets = datasets;
		if (listener != null) {
			listener.onDatasetsReceived(datasets);
		}
	}
	@Override
	public Map<String, String> getDatasets() {
		return datasets;
	}
	@Override
	public void addMeasureAndDimensions(String datasetIri, MeasureAndDimensions mAndD, MeasureAndDimensionsListener listener) {
		mAndDs.put(datasetIri, mAndD);
		if (listener != null) {
			listener.onMeasureAndDimensionsReceived(datasetIri, mAndD);
		}
	}
	@Override
	public MeasureAndDimensions getMeasureAndDimensions(String datasetIri) {
		return mAndDs.get(datasetIri);
	}
	@Override
	public Map<String, MeasureAndDimensions> getMeasureAndDimensionsList() {
		return mAndDs;
	}
	@Override
	public boolean hasMeasureAndDimensions(String datasetIri) {
		return mAndDs.containsKey(datasetIri);
	}
	@Override
	public void addData(Data d, DataListener listener) {
		if (!data.contains(d)) {
			data.add(d);
		}
		if (listener != null) {
			listener.onDataReceived(d);
		}
	}
	@Override
	public boolean hasData(List<String> eds, String dataset, Map<String, String> values) {
		for (Data d : data) {
			if (eds.equals(d.eds) && dataset.equals(d.dataset) && values.equals(d.values)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public Data getData(List<String> eds, String dataset, Map<String, String> values) {
		for (Data d : data) {
			if (eds.equals(d.eds) && dataset.equals(d.dataset) && values.equals(d.values)) {
				return d;
			}
		}
		return null;
	}
	@Override
	public List<Data> getDataList() {
		return data;
	}
	
	@Override
	public void loadElectoralDivisions(final ElectoralDivisionsListener listener, Context context, FragmentManager fragmentManager, LocationRetrievable locationRetrievable, int radius, GoogleMap map, List<Polygon> polys, Map<Marker, String> markers) {
		ElectoralDivisionsListener passListener = new ElectoralDivisionsListener() {
			@Override
			public void onElectoralDivisionsReceived() {
				cleanUpTasks();
				if (listener != null) {
					listener.onElectoralDivisionsReceived();
				}
			}
		};
		ElectoralDivisionsLoadTask task = new ElectoralDivisionsLoadTask(passListener, context, fragmentManager, locationRetrievable, radius, map, polys, markers);
		edLoadTasks.add(task);
		task.execute();
	}
	@Override
	public void loadDatasets(final DatasetsListener listener, Context context, FragmentManager fragmentManager) {
		if (datasets != null) {
			setDatasets(datasets, listener);
			return;
		}
		if (cache.getDatasets() != null) {
			setDatasets(cache.getDatasets(), listener);
			return;
		}
		DatasetsListener passListener = new DatasetsListener() {
			@Override
			public void onDatasetsReceived(Map<String, String> datasets) {
				cleanUpTasks();
				setDatasets(datasets, listener);
			}
		};
		DatasetsLoadTask task = new DatasetsLoadTask(passListener, context, fragmentManager);
		datasetsLoadTasks.add(task);
		task.execute(QueryFragment.DEFAULT_ED);
	}
	@Override
	public void loadMeasureAndDimensions(final MeasureAndDimensionsListener listener, Context context, FragmentManager fragmentManager, String datasetIri) {
		if (hasMeasureAndDimensions(datasetIri)) {
			addMeasureAndDimensions(datasetIri, mAndDs.get(datasetIri), listener);
			return;
		}
		if (cache.hasMeasureAndDimensions(datasetIri)) {
			addMeasureAndDimensions(datasetIri, cache.getMeasureAndDimensions(datasetIri), listener);
			return;
		}
		MeasureAndDimensionsListener passListener = new MeasureAndDimensionsListener() {
			@Override
			public void onMeasureAndDimensionsReceived(String datasetIri,  MeasureAndDimensions mAndDs) {
				cleanUpTasks();
				addMeasureAndDimensions(datasetIri, mAndDs, listener);
			}
		};
		MeasureAndDimensionsLoadTask task = new MeasureAndDimensionsLoadTask(passListener, context, fragmentManager);
		mAndDsLoadTasks.add(task);
		task.execute(QueryFragment.DEFAULT_ED, datasetIri);
	}
	@Override
	public void loadData(final DataListener listener, Context context, FragmentManager fragmentManager, List<String> eds, String dataset, String measure, Map<String, String> values) {
		if (hasData(eds, dataset, values)) {
			addData(getData(eds, dataset, values), listener);
			return;
		}
		if (cache.hasData(eds, dataset, values)) {
			addData(cache.getData(eds, dataset, values), listener);
			return;
		}
		DataListener passListener = new DataListener() {
			@Override
			public void onDataReceived(Data data) {
				cleanUpTasks();
				addData(data, listener);
			}
		};
		DataLoadTask task = new DataLoadTask(passListener, context, fragmentManager);
		dataLoadTasks.add(task);
		task.execute(eds, dataset, measure, values);
	}
	
	@Override
	public void onPause(boolean save) {
		cleanUpTasks();
		for (ElectoralDivisionsLoadTask t : edLoadTasks) {
			t.cancel(true);
		}
		for (DatasetsLoadTask t : datasetsLoadTasks) {
			t.cancel(true);
		}
		for (MeasureAndDimensionsLoadTask t : mAndDsLoadTasks) {
			t.cancel(true);
		}
		for (DataLoadTask t : dataLoadTasks) {
			t.cancel(true);
		}
		
		if (save) {
			cache.setDatasets(datasets, null);
			for (Entry<String, MeasureAndDimensions> e : mAndDs.entrySet()) {
				cache.addMeasureAndDimensions(e.getKey(), e.getValue(), null);
			}
			for (Data d : data) {
				cache.addData(d, null);
			}
			try {
				//TODO: possibly move this to a seperate thread?
				cache.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
