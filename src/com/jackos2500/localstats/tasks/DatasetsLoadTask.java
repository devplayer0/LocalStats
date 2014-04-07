package com.jackos2500.localstats.tasks;

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
import com.jackos2500.localstats.data.IDataLoader.DatasetsListener;
import com.jackos2500.localstats.ui.Dialogs.LoadingDialogFragment;

public class DatasetsLoadTask extends AsyncTask<String, String, Map<String, String>> {
	private DatasetsListener listener;
	private Context context;
	private FragmentManager fragmentManager;
	private String ed;
	
	private LoadingDialogFragment progressDialog;
	public DatasetsLoadTask(DatasetsListener listener, Context context, FragmentManager fragmentManager) {
		this.listener = listener;
		this.context = context;
		this.fragmentManager = fragmentManager;
	}
	@Override
	protected void onPreExecute() {
		progressDialog = LoadingDialogFragment.newInstance(-1, R.string.datasets_loading, false, null);
		progressDialog.setCancelable(false);
		progressDialog.show(fragmentManager, "LoadDatasetsDialog");
	}
	@Override
	protected void onProgressUpdate(String... progress) {
		for (String p : progress) {
			Toast.makeText(context, p, Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	protected void onCancelled(Map<String, String> result) {
		progressDialog.dismissAllowingStateLoss();
	}
	@Override
	protected Map<String, String> doInBackground(String... params) {
		try {
			ed = params[0];
			
			JSONObject req = new JSONObject();
			req.put("ed", ed);
			
			JSONObject res = Util.apiCall("get_datasets", req);
			String status = res.getString("status");
			if (status.equals("ok")) {
				Map<String, String> datasets = new HashMap<String, String>();
				JSONArray datasetsJson = res.getJSONArray("datasets");
				for (int i = 0; i < datasetsJson.length(); i++) {
					JSONObject datasetJson = datasetsJson.getJSONObject(i);
					
					String iri = datasetJson.getString("iri");
					String name = datasetJson.getString("name");
					
					datasets.put(iri, name);
				}
				return datasets;
			} else {
				throw new Exception(res.getString("msg"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			publishProgress(e.getMessage());
		}
		
		return null;
	}
	@Override
	protected void onPostExecute(final Map<String, String> datasets) {
		progressDialog.dismiss();
		if (datasets != null) {
			listener.onDatasetsReceived(datasets);
		}
	}
}
