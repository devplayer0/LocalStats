package com.jackos2500.localstats;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.jackos2500.localstats.data.DataAccess;
import com.jackos2500.localstats.data.disk.DataStore;
import com.jackos2500.localstats.fragment.QueryFragment;
import com.jackos2500.localstats.fragment.ResultsFragment;
import com.jackos2500.localstats.fragment.SavedQueriesFragment;
import com.jackos2500.localstats.fragment.SettingsFragment;
import com.jackos2500.localstats.ui.Dialogs;

public class MainActivity extends Activity {
	private final static int CONNECTION_FAIL = 9000;
	
	private static final int ITEM_QUERY = 0;
	private static final int ITEM_SAVED = 1;
	private static final int ITEM_SETTINGS = 2;
	
	public static final int ITEM_RESULTS = 10;
	
	public static int deviceHeight;
	public static DataAccess dataAccess;
	public static DataStore dataStore;
	
	private String title;
	private int lastPosition;
	private String[] navItems;
	private Map<Class<? extends Fragment>, String> navTitles;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private ListView navList;
	
	private int currentMenuItem;
	private SparseArray<MenuConfig> menuItems;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dataAccess = new DataAccess(this);
		dataStore = new DataStore(this);
		try {
			dataStore.read();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		menuItems = new SparseArray<MenuConfig>();
		MenuConfig resultsConfig = new MenuConfig();
		resultsConfig.addItem(R.id.action_save);
		menuItems.put(ITEM_RESULTS, resultsConfig);
		
		deviceHeight = getResources().getDisplayMetrics().heightPixels;
		
		navItems = getResources().getStringArray(R.array.nav_items);
		navTitles = new HashMap<Class<? extends Fragment>, String>();
		navTitles.put(QueryFragment.class, navItems[0]);
		navTitles.put(ResultsFragment.class, "Results");
		
		drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
		if (drawerLayout != null) {
			drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
			drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer_white, R.string.drawer_open, R.string.drawer_close) {
				@Override
				public void onDrawerClosed(View view) {
					super.onDrawerClosed(view);
					getActionBar().setTitle(title);
					invalidateOptionsMenu();
				}
				@Override
				public void onDrawerOpened(View view) {
					 super.onDrawerOpened(view);
					 getActionBar().setTitle(R.string.app_name);
					 invalidateOptionsMenu();
				}
			};
			drawerLayout.setDrawerListener(drawerToggle);
			
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeButtonEnabled(true);
		}
		
		navList = (ListView)findViewById(R.id.left_nav);
		navList.setAdapter(new ArrayAdapter<String>(this, R.layout.nav_item, navItems));
		navList.setOnItemClickListener(new NavItemClickListener());
		
		navList.getViewTreeObserver().addOnGlobalLayoutListener(new ItemHighlighter());
		lastPosition = -1;
		if (savedInstanceState == null) {
			selectItem(0);
			getActionBar().setTitle(title);
		}
		
		checkGps();
		servicesAvailable();
	}
	private boolean checkGps() {
		LocationManager locationService = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		boolean gps = locationService.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if (!gps) {
			new Dialogs.EnableGPSDialogFragment().show(getFragmentManager(), "EnableGPSDialog");
		}
		return gps;
	}
	private boolean servicesAvailable() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode == ConnectionResult.SUCCESS) {
			return true;
		}
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAIL);
		errorDialog.show();
		return false;
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case CONNECTION_FAIL:
				finish();
				break;
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (drawerLayout != null) {
			drawerToggle.syncState();
		}
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (drawerLayout != null) {
			drawerToggle.onConfigurationChanged(newConfig);
		}
	}
	private void syncUpButtonState() {
		if (drawerLayout != null) {
			drawerToggle.setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);
		}
		if (getFragmentManager().getBackStackEntryCount() == 0) {
			currentMenuItem = navList.getCheckedItemPosition();
		}
	}
	private void setOldTitle() {
		if (getFragmentManager().getBackStackEntryCount() > 1) {
			FragmentManager.BackStackEntry stackEntry = getFragmentManager().getBackStackEntryAt(getFragmentManager().getBackStackEntryCount() - 1);
			title = navTitles.get(getFragmentManager().findFragmentByTag(stackEntry.getName()).getClass());
		} else {
			title = navItems[navList.getCheckedItemPosition()];
		}
		getActionBar().setTitle(title);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("back stack count: "+getFragmentManager().getBackStackEntryCount());
		if (drawerLayout != null && getFragmentManager().getBackStackEntryCount() == 0) {
			if (drawerToggle.onOptionsItemSelected(item)) {
				return true;
			}
		} else {
			if (item.getItemId() == android.R.id.home) {
				setOldTitle();
				getFragmentManager().popBackStackImmediate();
				syncUpButtonState();
				invalidateOptionsMenu();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onBackPressed() {
		setOldTitle();
		super.onBackPressed();
		syncUpButtonState();
		invalidateOptionsMenu();
	}
	private void selectItem(int position) {
		Fragment fragment = null;
		if (getFragmentManager().getBackStackEntryCount() > 0) {
			getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			syncUpButtonState();
		}
		if (position != lastPosition) {
			switch(position) {
				case ITEM_QUERY:
					fragment = new QueryFragment();
					break;
				case ITEM_SAVED:
					fragment = new SavedQueriesFragment();
					break;
				case ITEM_SETTINGS:
					fragment = new SettingsFragment();
					break;
			}
		}
		lastPosition = position;
		if (fragment != null) {
			currentMenuItem = position;
			getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
		}
		navList.setItemChecked(position, true);
		title = navItems[position];
		if (drawerLayout != null) {
			drawerLayout.closeDrawer(navList);
		}
	}
	@Override
	public void onPause() {
		super.onPause();
		dataAccess.onPause(true);
		try {
			dataStore.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		for (int i = 0; i < menuItems.size(); i++) {
			if (menuItems.keyAt(i) == currentMenuItem) {
				for (Entry<Integer, Boolean> e : menuItems.valueAt(i).getItemEntries()) {
					menu.findItem(e.getKey()).setVisible(e.getValue());
				}
			} else {
				for (Entry<Integer, Boolean> e : menuItems.valueAt(i).getItemEntries()) {
					menu.findItem(e.getKey()).setVisible(false);
				}
			}
		}
		
		if (drawerLayout != null) {
			boolean drawerOpen = drawerLayout.isDrawerOpen(navList);
			if (drawerOpen) {
				menu.findItem(R.id.action_save).setVisible(false);
			}
		}
		return super.onPrepareOptionsMenu(menu);
    }
	private class NavItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}
	private class ItemHighlighter implements ViewTreeObserver.OnGlobalLayoutListener {
		@Override
		public void onGlobalLayout() {
			for (int i = 0; i < navList.getChildCount(); i++) {
				TextView item = (TextView)navList.getChildAt(i);
				if (navList.isItemChecked(i)) {
					item.setTextAppearance(getApplicationContext(), R.style.LocalStats_NavText_Selected);
				} else {
					item.setTextAppearance(getApplicationContext(), R.style.LocalStats_NavText);
				}
			 }
		}
	}
	public void addTopLevelFragment(Fragment fragment, int position, String tag) {
		currentMenuItem = position;
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.add(R.id.content_frame, fragment, tag).addToBackStack(tag).commit();
		
		if (drawerLayout != null) {
			drawerToggle.setDrawerIndicatorEnabled(false);
		}
		title = navTitles.get(fragment.getClass());
		
		getActionBar().setTitle(title);
		invalidateOptionsMenu();
	}
	
	public void setMenuItemEnabled(int position, int item, boolean enabled) {
		menuItems.get(position).setItemEnabled(item, enabled);
	}
	public boolean isItemEnabled(int position, int item) {
		return menuItems.get(position).isEnabled(item);
	}
	private static class MenuConfig {
		private Map<Integer, Boolean> items;
		public MenuConfig() {
			items = new HashMap<Integer, Boolean>();
		}
		public void addItem(int item) {
			items.put(item, true);
		}
		public void setItemEnabled(int item, boolean enabled) {
			items.put(item, enabled);
		}
		public boolean isEnabled(int item) {
			return items.get(item);
		}
		public Set<Entry<Integer, Boolean>> getItemEntries() {
			return items.entrySet();
		}
	}
}
