package com.jackos2500.localstats.data.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.jackos2500.localstats.data.IDataLoader.DataListener;
import com.jackos2500.localstats.data.IDataLoader.DatasetsListener;
import com.jackos2500.localstats.data.IDataLoader.MeasureAndDimensionsListener;
import com.jackos2500.localstats.data.IDataStore;
import com.jackos2500.localstats.tasks.DataLoadTask.Data;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;

public class DataCache implements IDataStore {
	private static final long DAY = 1000 * 60 * 60 * 24;
	private static final long MAX_AGE = DAY * 30;
	private static final long MEGABYTE = 1024 * 1024;

	private Context context;
	private Kryo kryo;

	private File cacheFile;

	private CacheHolder cache;
	private boolean dirty;

	public DataCache(Context context) throws IOException {
		this.context = context;
		
		kryo = new Kryo();
		kryo.register(CacheHolder.class);
		kryo.register(MeasureAndDimensions.class);
		kryo.register(Data.class);
		
		cacheFile = new File(context.getCacheDir(), "cache.bin");

		cache = new CacheHolder();
	}
	public void write() throws FileNotFoundException {
		Output output = new Output(new FileOutputStream(cacheFile));
		kryo.writeObjectOrNull(output, cache, CacheHolder.class);
		output.close();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		long maxSize = settings.getInt("settings_cache_size", 16) * MEGABYTE;
		System.out.println("max cache size: "+maxSize);
		
		if (cacheFile.length() > maxSize) {
			System.out.println("cache to big, purging...");
			cacheFile.delete();
		}
		
		/*while (cacheFile.length() > maxSize) {
			System.out.println("cache to big, purging...");
			cacheFile.delete();
			long ageToPurge = MAX_AGE;

			if (cache.getDatasetsAge() > ageToPurge) {
				cache.datasets = null;
			}

			List<String> mAndDsToPurge = new ArrayList<String>();
			for (Entry<String, MeasureAndDimensions> e : cache.mAndDs.entrySet()) {
				if (e.getValue().getAge() > ageToPurge) {
					mAndDsToPurge.add(e.getKey());
				}
			}
			for (String key : mAndDsToPurge) {
				cache.mAndDs.remove(key);
			}

			List<Data> dataToPurge = new ArrayList<Data>();
			for (Data data : cache.data) {
				if (data.getAge() > ageToPurge) {
					dataToPurge.add(data);
				}
			}
			for (Data data : dataToPurge) {
				cache.data.remove(data);
			}

			output = new Output(new FileOutputStream(cacheFile));
			kryo.writeObjectOrNull(output, cache, CacheHolder.class);
			output.close();
			ageToPurge -= DAY;
		}*/
	}
	public void read() throws FileNotFoundException {
		if (!cacheFile.exists()) {
			System.out.println("cache file does not exist, cancelling read.");
			return;
		}
		System.out.println("cache size: " + cacheFile.length());
		Input input = new Input(new FileInputStream(cacheFile));
		cache = (CacheHolder) kryo.readObjectOrNull(input, CacheHolder.class);
		input.close();
		if (cache == null) {
			cache = new CacheHolder();
		}
	}
	public void close() throws FileNotFoundException {
		if (dirty) {
			System.out.println("we are dirty, updating cache...");
			write();
		}
	}
	@Override
	public void setDatasets(Map<String, String> datasets, DatasetsListener listener) {
		if (datasets == null) {
			return;
		}
		if (datasets.equals(cache.datasets)) {
			System.out.println("datasets not dirty");
			return;
		}
		System.out.println("datasets are dirty");
		cache.datasets = datasets;
		dirty = true;
	}
	@Override
	public Map<String, String> getDatasets() {
		return cache.datasets;
	}
	@Override
	public void addMeasureAndDimensions(String datasetIri, MeasureAndDimensions mAndDs, MeasureAndDimensionsListener listener) {
		if (hasMeasureAndDimensions(datasetIri)) {
			if (getMeasureAndDimensions(datasetIri).equals(mAndDs)) {
				System.out.println("measure and dimensions not dirty");
				return;
			}
		}
		System.out.println("measure and dimensions are dirty");
		cache.mAndDs.put(datasetIri, mAndDs);
		dirty = true;
	}
	@Override
	public void addData(Data data, DataListener listener) {
		if (cache.data.contains(data)) {
			System.out.println("data not dirty");
			return;
		}
		System.out.println("data is dirty");
		cache.data.add(data);
		dirty = true;
	}
	@Override
	public MeasureAndDimensions getMeasureAndDimensions(String datasetIri) {
		return cache.mAndDs.get(datasetIri);
	}
	@Override
	public Map<String, MeasureAndDimensions> getMeasureAndDimensionsList() {
		return cache.mAndDs;
	}
	@Override
	public boolean hasMeasureAndDimensions(String datasetIri) {
		return cache.mAndDs.containsKey(datasetIri);
	}
	@Override
	public Data getData(List<String> eds, String dataset, Map<String, String> values) {
		for (Data d : cache.data) {
			if (eds.equals(d.eds) && dataset.equals(d.dataset) && values.equals(d.values)) {
				return d;
			}
		}
		return null;
	}
	@Override
	public List<Data> getDataList() {
		return cache.data;
	}
	@Override
	public boolean hasData(List<String> eds, String dataset, Map<String, String> values) {
		for (Data d : cache.data) {
			if (eds.equals(d.eds) && dataset.equals(d.dataset) && values.equals(d.values)) {
				return true;
			}
		}
		return false;
	}

	private static class CacheHolder {
		public Map<String, String> datasets;
		public Map<String, MeasureAndDimensions> mAndDs;
		public List<Data> data;

		public long datasetsCreateTime;

		public CacheHolder() {
			mAndDs = new HashMap<String, MeasureAndDimensions>();
			data = new ArrayList<Data>();
		}
		public long getDatasetsAge() {
			return new Date().getTime() - datasetsCreateTime;
		}
	}
}
