package com.jackos2500.localstats.data;

import java.util.List;
import java.util.Map;

import android.app.FragmentManager;
import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.jackos2500.localstats.tasks.DataLoadTask.Data;
import com.jackos2500.localstats.tasks.ElectoralDivisionsLoadTask.LocationRetrievable;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;

public interface IDataLoader {
	public void onPause(boolean save);
	
	public void loadElectoralDivisions(ElectoralDivisionsListener listener, Context context, FragmentManager fragmentManager, LocationRetrievable locationRetrievable, int radius, GoogleMap map, List<Polygon> polys, Map<Marker, String> markers);
	public void loadDatasets(DatasetsListener listener, Context context, FragmentManager fragmentManager);
	public void loadMeasureAndDimensions(MeasureAndDimensionsListener listener, Context context, FragmentManager fragmentManager, String datasetIri);
	public void loadData(DataListener listener, Context context, FragmentManager fragmentManager, List<String> eds, String dataset, String measure, Map<String, String> values);
	
	//public void addDataListener(DataListener dataListener);
	//public void removeDataListener(DataListener dataListener);
	
	/*public static interface DataListener {
		public void onElectoralDivisionsCancelled();
		public void onDatasetsCancelled();
		public void onMeasureAndDimensionsCancelled();
		public void onDataCancelled();
	}*/
	public static interface ElectoralDivisionsListener {
		public void onElectoralDivisionsReceived();
	}
	public static interface DatasetsListener {
		public void onDatasetsReceived(Map<String, String> datasets);
	}
	public static interface MeasureAndDimensionsListener {
		public void onMeasureAndDimensionsReceived(String datasetIri, MeasureAndDimensions mAndDs);
	}
	public static interface DataListener {
		public void onDataReceived(Data data);
	}
}
