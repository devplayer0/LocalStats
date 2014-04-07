package com.jackos2500.localstats.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.cardsui.CardHeader;
import com.afollestad.cardsui.CardListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.card.CriteriaCard;
import com.jackos2500.localstats.card.EmptyCard;
import com.jackos2500.localstats.card.MapCard;
import com.jackos2500.localstats.card.RadiusCard;
import com.jackos2500.localstats.data.IDataLoader.ElectoralDivisionsListener;
import com.jackos2500.localstats.tasks.ElectoralDivisionsLoadTask;
import com.jackos2500.localstats.ui.Dialogs;
import com.jackos2500.localstats.ui.MultiCardAdapter;

public class QueryFragment extends Fragment implements LocationListener, ElectoralDivisionsLoadTask.LocationRetrievable {
	public static final String DEFAULT_ED = "01001";
	public static final LatLng CENTER = new LatLng(53.5, -8);
	private final static int CONNECTION_FAIL = 9000;
	
	private LocationManager locationManager;
	public Location location;
	private String locProvider;
	
	private CardListView list;
	private MultiCardAdapter listAdapter;
	
	private RadiusCard radiusCard;
	private EmptyCard emptyCard;
	
	private MapView mapView;
	private GoogleMap map;
	public List<Polygon> polys;
	public Map<Marker, String> markers;
	
	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		list = (CardListView)inflater.inflate(R.layout.card_layout, container, false);
		listAdapter = new MultiCardAdapter(getActivity());
		listAdapter.setAccentColorRes(android.R.color.holo_blue_light);
		
		listAdapter.add(new CardHeader("Map", "Shows the area to be searched").setAction("Show Area", new CardHeader.ActionListener() {
			@Override
			public void onClick(CardHeader header) {
				RadiusCard radiusCard = (RadiusCard)listAdapter.getItem(3);
				loadElectoralDivisions(null, radiusCard.getRadius());
			}
		}));
		listAdapter.add(new MapCard(mapView));
		listAdapter.add(new CardHeader("Radius", "The area around your position to search"));
		
		radiusCard = new RadiusCard(this);
		listAdapter.add(radiusCard);
		
		emptyCard = new EmptyCard(0.5f, "No criteria. Tap the button above to add some.");
		listAdapter.add(new CardHeader("Criteria", "Criteria to query with").setAction("Add", new CardHeader.ActionListener() {
			@Override
			public void onClick(CardHeader header) {
				listAdapter.add(new CriteriaCard(getActivity(), QueryFragment.this));
				list.setSelection(listAdapter.getCount() - 1);
				
				updateEmptyCard(false);
			}
		}));
		listAdapter.add(emptyCard);
		
		list.setAdapter(listAdapter);
		
		return list;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		polys = new ArrayList<Polygon>();
		markers = new HashMap<Marker, String>();
		
		servicesAvailable();
		checkGps();
		
		locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		locProvider = locationManager.getBestProvider(criteria, true);
		
		mapView = new MapView(getActivity());
		mapView.onCreate(savedInstanceState);
		
		if (!initMap()) {
			Toast.makeText(getActivity(), "Map failed to initialise!", Toast.LENGTH_SHORT).show();
		} else {
			MapsInitializer.initialize(getActivity());
			
			map.getUiSettings().setAllGesturesEnabled(false);
			
			CameraUpdate center = CameraUpdateFactory.newLatLngZoom(CENTER, 6.5f);
			map.animateCamera(center);
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case CONNECTION_FAIL:
				getActivity().finish();
				break;
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates(locProvider, 400, 1, this);
		mapView.onResume();
	}
	@Override
	public void onPause() {
		super.onPause();
		MainActivity.dataAccess.onPause(false);
		locationManager.removeUpdates(this);
		mapView.onPause();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}
	private boolean initMap() {
		if (map == null) {
			map = mapView.getMap();
		}
		return map != null;
	}
	private boolean checkGps() {
		LocationManager locationService = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
		boolean gps = locationService.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if (!gps) {
			new Dialogs.EnableGPSDialogFragment().show(getFragmentManager(), "EnableGPSDialog");
		}
		return gps;
	}
	private boolean servicesAvailable() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
		if (resultCode == ConnectionResult.SUCCESS) {
			return true;
		}
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), CONNECTION_FAIL);
		errorDialog.show();
		return false;
	}
	@Override
	public void onLocationChanged(Location location) {
		if (location.getAccuracy() < 100) {
			this.location = location;
		}
	}
	@Override
	public Location getLocation() {
		return location;
	}
	@Override
	public void onProviderDisabled(String provider) {}
	@Override
	public void onProviderEnabled(String provider) {}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	public CardListView getList() {
		return list;
	}
	public MultiCardAdapter getListAdapter() {
		return listAdapter;
	}
	public void loadElectoralDivisions(ElectoralDivisionsListener listener, int radius) {
		MainActivity.dataAccess.loadElectoralDivisions(listener, getActivity(), getFragmentManager(), this, radius, map, polys, markers);
	}
	public List<String> getEDs() {
		if (markers.size() == 0) {
			return null;
		}
		List<String> eds = new ArrayList<String>(markers.size());
		eds.addAll(markers.values());
		return eds;
	}
	@SuppressWarnings("unchecked")
	public void updateEmptyCard(boolean show) {
		if (show) {
			listAdapter.add(emptyCard);
		} else {
			listAdapter.remove(emptyCard);
		}
		Util.reloadCard(list, listAdapter, radiusCard);
	}
}
