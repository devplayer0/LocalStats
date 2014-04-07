package com.jackos2500.localstats.card;

import android.view.View;

import com.afollestad.cardsui.Card;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.Util;
import com.jackos2500.localstats.data.IDataLoader.ElectoralDivisionsListener;
import com.jackos2500.localstats.fragment.QueryFragment;
import com.jackos2500.localstats.fragment.ResultsFragment;
import com.jackos2500.localstats.ui.Dialogs.SetRadiusDialogFragment;

public class RadiusCard extends Card {
	private static final long serialVersionUID = 1L;
	
	private QueryFragment queryFragment;
	
	private int radius;
	
	private View.OnClickListener radiusListener;
	private View.OnClickListener queryListener;
	public RadiusCard(QueryFragment queryFragment) {
		this.queryFragment = queryFragment;
		radiusListener = new SetRadiusListener(queryFragment);
		queryListener = new QueryListener(queryFragment);
	}
	public int getRadius() {
		return radius;
	}
	public void setRadius(int radius) {
		this.radius = radius;
	}
	public boolean canQuery() {
		for (Object obj : queryFragment.getListAdapter().getItems()) {
			if (obj instanceof EmptyCard) {
				return false;
			}
		}
		return true;
	}
	public View.OnClickListener getRadiusListener() {
		return radiusListener;
	}
	public View.OnClickListener getQueryListener() {
		return queryListener;
	}
	@Override
	public int getLayout() {
		return R.layout.radius_card;
	}
	private class SetRadiusListener implements View.OnClickListener {
		//private FragmentManager fragmentManager;
		private QueryFragment queryFragment;
		public SetRadiusListener(QueryFragment queryFragment) {
			//this.fragmentManager = fragmentManager;
			this.queryFragment = queryFragment;
		}
		@Override
		public void onClick(View view) {
			SetRadiusDialogFragment dialog = SetRadiusDialogFragment.newInstance(RadiusCard.this, queryFragment);
			dialog.show(queryFragment.getFragmentManager(), "SetRadiusDialog");
		}
	}
	private class QueryListener implements View.OnClickListener {
		private QueryFragment queryFragment;
		
		public QueryListener(QueryFragment queryFragment) {
			this.queryFragment = queryFragment;
		}
		@Override
		public void onClick(final View view) {
			queryFragment.loadElectoralDivisions(new ElectoralDivisionsListener() {
				@Override
				public void onElectoralDivisionsReceived() {
					ResultsFragment resultsFragment = Util.getResultsFragment(queryFragment.getEDs(), queryFragment.getListAdapter(), true);
					
					((MainActivity)queryFragment.getActivity()).addTopLevelFragment(resultsFragment, MainActivity.ITEM_RESULTS, "ResultsFragment");
				}
			}, getRadius());
		}
	}
}
