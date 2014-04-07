package com.jackos2500.localstats.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.IDataLoader.ElectoralDivisionsListener;
import com.jackos2500.localstats.fragment.QueryFragment;
import com.jackos2500.localstats.ui.Dialogs.LoadingDialogFragment;

public class ElectoralDivisionsLoadTask extends AsyncTask<Void, Object, Object[]> {
	private Context context;
	private ElectoralDivisionsListener listener;
	private FragmentManager fragmentManager;
	private LocationRetrievable locationRetrievable;
	private int radius;
	private GoogleMap map;
	public List<Polygon> polys;
	private Map<Marker, String> markers;
	
	private boolean testMode;
	LoadingDialogFragment progressDialog;
	public ElectoralDivisionsLoadTask(ElectoralDivisionsListener listener, Context context, FragmentManager fragmentManager, LocationRetrievable locationRetrievable, int radius, GoogleMap map, List<Polygon> polys, Map<Marker, String> markers) {
		this.context = context;
		this.listener = listener;
		this.fragmentManager = fragmentManager;
		this.locationRetrievable = locationRetrievable;
		this.radius = radius;
		this.map = map;
		this.polys = polys;
		this.markers = markers;
	}
	@Override
	protected void onPreExecute() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		testMode = settings.getBoolean("settings_test_mode", false);
		
		progressDialog = LoadingDialogFragment.newInstance(R.string.area_retrieve, R.string.loading, true, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				cancel(true);
			}
		});
		progressDialog.setCancelable(false);
		progressDialog.show(fragmentManager, "LoadingDialog");
		for (Polygon p : polys) {
			p.remove();
		}
		polys.clear();
		for (Marker m : markers.keySet()) {
			m.remove();
		}
		markers.clear();
	}
	@Override
	protected void onProgressUpdate(Object... progress) {
		boolean mode = (Boolean)progress[0];
		for (Object o : progress) {
			if (o instanceof String) {
				String msg = (String)o;
				if (mode) {
					progressDialog.getLoadingTextView().setText(msg);
				} else {
					Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
				}
			}
		}
	}
	@Override
	protected void onCancelled(Object[] objects) {
		progressDialog.dismissAllowingStateLoss();
		for (Polygon p : polys) {
			p.remove();
		}
		polys.clear();
		for (Marker m : markers.keySet()) {
			m.remove();
		}
		markers.clear();
	}
	@Override
	protected Object[] doInBackground(Void... args) {
		try {
			if (!testMode) {
				if (locationRetrievable.getLocation() == null) {
					publishProgress(true, "Waiting for GPS...");
				}
				while (locationRetrievable.getLocation() == null) {
					try {
						if (isCancelled()) {
							return null;
						}
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			publishProgress(true, "Contacting server...");
			
			JSONObject req = new JSONObject();
			if (testMode) {
				req.put("lat", QueryFragment.CENTER.latitude);
				req.put("lon", QueryFragment.CENTER.longitude);
			} else {
				req.put("lat", locationRetrievable.getLocation().getLatitude());
				req.put("lon", locationRetrievable.getLocation().getLongitude());
			}
			req.put("radius", radius);
			
			JSONObject replyJson = Util.apiCall("get_electoral_divisions", req);
			
			String status = replyJson.getString("status");
			if (status.equals("ok")) {
				List<PolygonOptions> polys = new ArrayList<PolygonOptions>();
				Map<MarkerOptions, String> markers = new HashMap<MarkerOptions, String>();
				
				JSONArray electoralDivs = replyJson.getJSONArray("electoral_divisions");
				for (int i = 0; i < electoralDivs.length(); i++) {
					JSONObject ed = electoralDivs.getJSONObject(i);
					JSONArray vertices = ed.getJSONArray("polygon");
					
					PolygonOptions edPoly = new PolygonOptions();
					int[] colors = Util.randomColorsWAndWOAlpha(80);
					edPoly.strokeColor(colors[0]);
					edPoly.fillColor(colors[1]);
					
					double minLat = 0;
					double maxLat = 0;
					double minLon = 0;
					double maxLon = 0;
					for (int j = 0; j < vertices.length(); j++) {
						JSONObject vertex = vertices.getJSONObject(j);
						
						double lat = vertex.getDouble("lat");
						if (lat < minLat || minLat == 0) {
							minLat = lat;
						}
						if (lat > maxLat || maxLat == 0) {
							maxLat = lat;
						}
						double lon = vertex.getDouble("lon");
						if (lon < minLon || minLon == 0) {
							minLon = lon;
						}
						if (lon > maxLon || maxLon == 0) {
							maxLon = lon;
						}
						edPoly.add(new LatLng(lat, lon));
					}
					double centerLat = (minLat + maxLat) / 2;
					double centerLon = (minLon + maxLon) / 2;
					
					String name = ed.getString("name");
					String id = ed.getString("id");
					
					MarkerOptions marker = new MarkerOptions();
					marker.title(name);
					marker.position(new LatLng(centerLat, centerLon));
					markers.put(marker, id);
					
					polys.add(edPoly);
				}
				
				JSONObject bounds = replyJson.getJSONObject("bounds");
				PolygonOptions area = new PolygonOptions();
				area.strokeColor(Color.RED);
				area.fillColor(0x80FF0000);
				
				double x1 = bounds.getDouble("x1");
				double x2 = bounds.getDouble("x2");
				double y1 = bounds.getDouble("y1");
				double y2 = bounds.getDouble("y2");
				area.add(new LatLng(y2, x1));
				area.add(new LatLng(y2, x2));
				area.add(new LatLng(y1, x2));
				area.add(new LatLng(y1, x1));
				polys.add(area);
				return new Object[] {polys, markers};
			} else {
				throw new Exception(replyJson.getString("msg"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			publishProgress(false, "Error: "+e.getMessage());
		}
		
		return null;
	}
	@SuppressWarnings("unchecked")
	@Override
	protected void onPostExecute(Object[] objects) {
		if (objects != null) {
			List<PolygonOptions> polys = (List<PolygonOptions>)objects[0];
			Map<MarkerOptions, String> markers = (Map<MarkerOptions, String>)objects[1];
			for (PolygonOptions p : polys) {
				Polygon poly = map.addPolygon(p);
				this.polys.add(poly);
			}
			for (MarkerOptions m : markers.keySet()) {
				Marker marker = map.addMarker(m);
				this.markers.put(marker, markers.get(m));
			}
		}
		CameraUpdate user = null;
		if (testMode) {
			user = CameraUpdateFactory.newLatLngZoom(QueryFragment.CENTER, 11f);
		} else {
			user = CameraUpdateFactory.newLatLngZoom(new LatLng(locationRetrievable.getLocation().getLatitude(), locationRetrievable.getLocation().getLongitude()), 11f);
		}
		map.animateCamera(user);
		progressDialog.dismiss();
		listener.onElectoralDivisionsReceived();
	}
	public static interface LocationRetrievable {
		public Location getLocation();
	}
}