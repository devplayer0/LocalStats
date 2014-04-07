package com.jackos2500.localstats.tasks;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.IDataLoader.MeasureAndDimensionsListener;
import com.jackos2500.localstats.data.disk.Cacheable;
import com.jackos2500.localstats.tasks.MeasureAndDimensionsLoadTask.MeasureAndDimensions;
import com.jackos2500.localstats.ui.Dialogs.LoadingDialogFragment;

public class MeasureAndDimensionsLoadTask extends AsyncTask<String, String, MeasureAndDimensions> {
	private MeasureAndDimensionsListener listener;
	private Context context;
	private FragmentManager fragmentManager;
	private String datasetIri;
	
	private LoadingDialogFragment progressDialog;
	public MeasureAndDimensionsLoadTask(MeasureAndDimensionsListener listener, Context context, FragmentManager fragmentManager) {
		this.listener = listener;
		this.context = context;
		this.fragmentManager = fragmentManager;
	}
	@Override
	protected void onPreExecute() {
		progressDialog = LoadingDialogFragment.newInstance(-1, R.string.loading, false, null);
		progressDialog.setCancelable(false);
		progressDialog.show(fragmentManager, "LoadMeasureAndDimensionsDialog");
	}
	@Override
	protected void onProgressUpdate(String... progress) {
		for (String p : progress) {
			Toast.makeText(context, p, Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	protected void onCancelled(MeasureAndDimensions result) {
		progressDialog.dismissAllowingStateLoss();
	}
	@Override
	protected MeasureAndDimensions doInBackground(String... params) {
		try {
			datasetIri = params[1];
			
			JSONObject req = new JSONObject();
			req.put("ed", params[0]);
			req.put("dataset", params[1]);
			
			JSONObject res = Util.apiCall("get_measure_dimensions", req);
			String status = res.getString("status");
			if (status.equals("ok")) {
				MeasureAndDimensions mAndDs = new MeasureAndDimensions();
				mAndDs.measureIri = res.getString("measure");
				mAndDs.measureName = res.getString("measureName");
				
				JSONArray dimensions = res.getJSONArray("dimensions");
				for (int i = 0; i < dimensions.length(); i++) {
					JSONObject dimension = dimensions.getJSONObject(i);
					String dimensionIri = dimension.getString("iri");
					String dimensionName = dimension.getString("name");
					mAndDs.dimensions.put(dimensionIri, dimensionName);
					
					JSONArray valuesNode = dimension.getJSONArray("values");
					Map<String, String> values = new HashMap<String, String>();
					for (int j = 0; j < valuesNode.length(); j++) {
						JSONObject value = valuesNode.getJSONObject(j);
						values.put(value.getString("iri"), value.getString("name"));
					}
					mAndDs.dimensionValues.put(dimensionIri, values);
				}
				return mAndDs;
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
	protected void onPostExecute(MeasureAndDimensions mAndDs) {
		progressDialog.dismiss();
		if (mAndDs != null) {
			listener.onMeasureAndDimensionsReceived(datasetIri, mAndDs);
		}
	}
	public static class MeasureAndDimensions implements Cacheable {
		private long timeCreated;
		
		public String measureIri;
		public String measureName;
		
		public Map<String, String> dimensions;
		public Map<String, Map<String, String>> dimensionValues;
		public MeasureAndDimensions() {
			dimensions = new HashMap<String, String>();
			dimensionValues = new HashMap<String, Map<String, String>>();
			
			timeCreated = new Date().getTime();
		}
		public long getAge() {
			long currentTime = new Date().getTime();
			return currentTime - timeCreated;
		}
	}
}
