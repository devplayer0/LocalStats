package com.jackos2500.localstats.tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.IDataLoader.DataListener;
import com.jackos2500.localstats.data.disk.Cacheable;
import com.jackos2500.localstats.tasks.DataLoadTask.Data;
import com.jackos2500.localstats.ui.Dialogs.LoadingDialogFragment;

public class DataLoadTask extends AsyncTask<Object, String, Data> {
	private DataListener listener;
	private Context context;
	private FragmentManager fragmentManager;
	
	private LoadingDialogFragment progressDialog;
	public DataLoadTask(DataListener listener, Context context, FragmentManager fragmentManager) {
		this.listener = listener;
		this.context = context;
		this.fragmentManager = fragmentManager;
	}
	@Override
	protected void onPreExecute() {
		progressDialog = LoadingDialogFragment.newInstance(-1, R.string.data_loading, false, null);
		progressDialog.setCancelable(false);
		progressDialog.show(fragmentManager, "LoadDataDialog");
	}
	@Override
	protected void onProgressUpdate(String... progress) {
		for (String p : progress) {
			//progressDialog.getLoadingTextView().setText(p);
			Toast.makeText(context, p, Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	protected void onCancelled(Data data) {
		progressDialog.dismissAllowingStateLoss();
	}
	@SuppressWarnings("unchecked")
	@Override
	protected Data doInBackground(Object... params) {
		try {
			List<String> eds = (List<String>)params[0];
			String dataset = (String)params[1];
			String measure = (String)params[2];
			Map<String, String> values = (Map<String, String>)params[3];
			
			JSONArray edsArray = new JSONArray();
			for (String ed : eds) {
				edsArray.put(ed);
			}
			
			JSONObject req = new JSONObject();
			req.put("eds", edsArray);
			req.put("dataset", dataset);
			req.put("measure", measure);
			
			JSONArray dimensions = new JSONArray();
			for (Entry<String, String> e : values.entrySet()) {
				JSONObject dimension = new JSONObject();
				dimension.put("dimensionIri", e.getKey());
				dimension.put("valueIri", e.getValue());
				dimensions.put(dimension);
			}
			req.put("dimensions", dimensions);
			
			JSONObject res = Util.apiCall("get_data_count", req);
			String status = res.getString("status");
			if (status.equals("ok")) {
				int count = res.getInt("count");
				int total = res.getInt("total");
				
				Data data = new Data();
				data.eds = eds;
				data.dataset = dataset;
				data.values = values;
				data.count = count;
				data.total = total;
				return data;
			} else {
				throw new Exception(res.getString("msg"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			publishProgress("Error: "+e.getMessage());
		}
		return null;
	}
	@Override
	protected void onPostExecute(Data data) {
		progressDialog.dismiss();
		if (data != null) {
			listener.onDataReceived(data);
		}
	}
	public static class Data implements Cacheable {
		private long timeCreated;
		
		public List<String> eds;
		public String dataset;
		public Map<String, String> values;
		public int count;
		public int total;
		public Data() {
			timeCreated = new Date().getTime();
			eds = new ArrayList<String>();
		}
		@Override
		public long getAge() {
			return new Date().getTime() - timeCreated;
		}
	}
}
