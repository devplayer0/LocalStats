package com.jackos2500.localstats.data.disk;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class DataStore {
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy 'at' kk:mm", Locale.getDefault());
	
	private static String DATA_FILE = "data.bin";

	private Context context;
	private Kryo kryo;

	private DataHolder data;
	private boolean dirty;

	public DataStore(Context context) {
		this.context = context;
		
		kryo = new Kryo();
		kryo.register(DataHolder.class);
		kryo.register(DataQuerySet.class);
		kryo.register(DataQuery.class);

		data = new DataHolder();
	}
	public void write() throws FileNotFoundException {
		Output output = new Output(context.openFileOutput(DATA_FILE, Context.MODE_PRIVATE));
		kryo.writeObjectOrNull(output, data, DataHolder.class);
		output.close();
	}
	public void read() throws FileNotFoundException {
		boolean exists = false;
		for (String file : context.fileList()) {
			if (file.equals(DATA_FILE)) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			System.out.println("data file doesn't exist, cancelling read");
			return;
		}
		
		Input input = new Input(context.openFileInput(DATA_FILE));
		data = (DataHolder)kryo.readObjectOrNull(input, DataHolder.class);
		input.close();
		if (data == null) {
			data = new DataHolder();
		}
	}
	public void close() throws FileNotFoundException {
		if (dirty) {
			System.out.println("we are dirty, updating data...");
			write();
		}
	}
	
	public void addQuery(String name, DataQuerySet query) {
		if (!data.queries.containsKey(name)) {
			data.queries.put(name, query);
			dirty = true;
		}
	}
	public void renameQuery(String oldName, String newName) {
		if (data.queries.containsKey(oldName)) {
			DataQuerySet query = data.queries.remove(oldName);
			addQuery(newName, query);
		}
	}
	public void removeQuery(String name) {
		if (data.queries.containsKey(name)) {
			data.queries.remove(name);
			dirty = true;
		}
	}
	public Map<String, DataQuerySet> getQueries() {
		return data.queries;
	}
	public boolean containsQuery(String name) {
		return data.queries.containsKey(name);
	}

	private static class DataHolder {
		public Map<String, DataQuerySet> queries;

		public DataHolder() {
			queries = new HashMap<String, DataQuerySet>();
		}
	}
	public static class DataQuerySet {
		private long dateCreated;
		
		public List<String> eds;
		public List<DataQuery> queries;
		public DataQuerySet() {
			dateCreated = new Date().getTime();
			
			eds = new ArrayList<String>();
			queries = new ArrayList<DataQuery>();
		}
		
		public long getDateCreated() {
			return dateCreated;
		}
	}
	public static class DataQuery {
		public String measure;
		public String dataset;
		public Map<String, String> values;
		public DataQuery() {
			values = new HashMap<String, String>();
		}
	}
}
