package com.jackos2500.localstats.data;

import java.util.List;
import java.util.Map;

import com.jackos2500.localstats.data.IDataLoader.DataListener;
import com.jackos2500.localstats.data.IDataLoader.DatasetsListener;
import com.jackos2500.localstats.data.IDataLoader.MeasureAndDimensionsListener;
import com.jackos2500.localstats.tasks.DataLoadTask.Data;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;

public interface IDataStore {
	public void setDatasets(Map<String, String> datasets, DatasetsListener listener);
	public Map<String, String> getDatasets();
	
	public void addMeasureAndDimensions(String datasetIri, MeasureAndDimensions mAndD, MeasureAndDimensionsListener listener);
	public boolean hasMeasureAndDimensions(String datasetIri);
	public MeasureAndDimensions getMeasureAndDimensions(String datasetIri);
	public Map<String, MeasureAndDimensions> getMeasureAndDimensionsList();
	
	public void addData(Data data, DataListener listener);
	public boolean hasData(List<String> eds, String dataset, Map<String, String> values);
	public Data getData(List<String> eds, String dataset, Map<String, String> values);
	public List<Data> getDataList();
}
