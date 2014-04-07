package com.jackos2500.localstats.ui;

import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.cardsui.CardBase;
import com.afollestad.cardsui.LightItalicTextView;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;
import com.jackos2500.localstats.card.CriteriaCard;
import com.jackos2500.localstats.card.EmptyCard;
import com.jackos2500.localstats.card.MapCard;
import com.jackos2500.localstats.card.RadiusCard;
import com.jackos2500.localstats.card.ResultsCard;
import com.jackos2500.localstats.card.ResultsCard.ReadableData;
import com.jackos2500.localstats.card.SavedQueryCard;
import com.jackos2500.localstats.data.disk.DataStore;

public class MultiCardAdapter extends AnimatedCardAdapter {
	public MultiCardAdapter(Context context) {
		super(context);
		
		registerLayout(R.layout.map_card);
		registerLayout(R.layout.radius_card);
		registerLayout(R.layout.criteria_card);
		registerLayout(R.layout.results_card);
		registerLayout(R.layout.empty_card);
		registerLayout(R.layout.saved_query_card);
	}
	public static ViewGroup getParent(View view) {
		return (ViewGroup) view.getParent();
	}
	public static void removeView(View view) {
		ViewGroup parent = getParent(view);
		if (parent != null) {
			parent.removeView(view);
		}
	}
	public static void replaceView(View currentView, View newView) {
		ViewGroup parent = getParent(currentView);
		if (parent == null) {
			return;
		}
		final int index = parent.indexOfChild(currentView);
		removeView(currentView);
		removeView(newView);
		parent.addView(newView, index);
	}
	@SuppressWarnings("rawtypes")
	@Override
	public View onViewCreated(int index, View recycled, CardBase item) {
		if (item instanceof MapCard) {
			MapCard card = (MapCard)item;
			if (recycled.findViewById(R.id.map) != null) {
				replaceView(recycled.findViewById(R.id.map), card.getMap());
			}
			recycled.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, MainActivity.deviceHeight / 2));
		}
		if (item instanceof RadiusCard) {
			RadiusCard card = (RadiusCard)item;
			
			Button setRadius = (Button)recycled.findViewById(R.id.set_radius);
			setRadius.setOnClickListener(card.getRadiusListener());
			
			Button query = (Button)recycled.findViewById(R.id.query);
			query.setEnabled(card.canQuery());
			query.setOnClickListener(card.getQueryListener());
		}
		if (item instanceof CriteriaCard) {
			CriteriaCard card = (CriteriaCard)item;
			
			Spinner datasetsSpinner = (Spinner)recycled.findViewById(R.id.dataset_spinner);
			datasetsSpinner.setAdapter(card.getDatasetsAdapter());
			if (card.getSelectedDataset() != null) {
				datasetsSpinner.setSelection(card.getDatasetsAdapter().getPositionForDatasetIri(card.getSelectedDataset()));
			}
			datasetsSpinner.setOnItemSelectedListener(card.getDatasetSelectedListener());
			
			LinearLayout form = (LinearLayout)recycled.findViewById(R.id.criteria_form);
			
			int dimensionCount = card.getDimensions().size();
			int prevDimensionCount = datasetsSpinner.getTag() != null ? (Integer)datasetsSpinner.getTag() : 0;
			if (dimensionCount < prevDimensionCount) {
				int start = prevDimensionCount - dimensionCount;
				for (int i = start; i < prevDimensionCount; i++) {
					View header = form.findViewById(1000 + (i * 10) + 1);
					form.removeView(header);
					
					View spinner = form.findViewById(1000 + (i * 10) + 2);
					form.removeView(spinner);
				}
			}
			
			for (int i = 0; i < dimensionCount; i++) {
				LightItalicTextView header = (LightItalicTextView)form.findViewById(1000 + (i * 10) + 1);
				if (header == null) {
					header = new LightItalicTextView(getContext());
					header.setId(1000 + (i * 10) + 1);
					form.addView(header);
				}
				header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				header.setText(card.getDimensions().get(i));
				
				Spinner valuesSpinner = (Spinner)form.findViewById(1000 + (i * 10) + 2);
				if (valuesSpinner == null) {
					valuesSpinner = new Spinner(getContext());
					valuesSpinner.setId(1000 + (i * 10) + 2);
					form.addView(valuesSpinner);
				}
				valuesSpinner.setAdapter(card.getValuesAdapter(i));
				valuesSpinner.setSelection(card.getSelectedValue(i));
				valuesSpinner.setOnItemSelectedListener(card.getValueSelectedListener(i));
			}
			datasetsSpinner.setTag(dimensionCount);
		}
		if (item instanceof ResultsCard) {
			ResultsCard card = (ResultsCard)item;
			ReadableData data = card.getReadableData();
			
			LinearLayout valuesTable = (LinearLayout)recycled.findViewById(R.id.values_table);
			
			int dimensionCount = data.values.size();
			int prevDimensionCount = valuesTable.getTag() != null ? (Integer)valuesTable.getTag() : 0;
			if (dimensionCount < prevDimensionCount) {
				int start = prevDimensionCount - dimensionCount;
				for (int i = start; i < prevDimensionCount; i++) {
					View value = valuesTable.findViewById(2000 + (i * 10) + 1);
					valuesTable.removeView(value);
				}
			}
			valuesTable.setTag(dimensionCount);
			
			LightItalicTextView dataset = (LightItalicTextView)valuesTable.findViewById(R.id.dataset);
			dataset.setText(data.dataset);
			
			int i = 0;
			for (Entry<String, String> e : data.values.entrySet()) {
				TextView value = (TextView)valuesTable.findViewById(2000 + (i * 10) + 1);
				if (value == null) {
					value = new TextView(getContext());
					value.setId(2000 + (i * 10) + 1);
					valuesTable.addView(value);
				}
				value.setText(e.getKey()+": "+e.getValue());
				i++;
			}
			
			TextView result = (TextView)recycled.findViewById(R.id.result);
			if (data.measure != null) {
				result.setText("There are "+data.count+" out of "+data.total+" "+data.measure.toLowerCase(Locale.getDefault())+" in your area matching the above criteria.");
			}
			
			PieChartView chart = (PieChartView)recycled.findViewById(R.id.results_chart);
			chart.setLayoutParams(new LinearLayout.LayoutParams(chart.getLayoutParams().width, MainActivity.deviceHeight / 2));
			
			if (data.total != 0) {
				chart.setValue(data.count, data.total);
			}
		}
		if (item instanceof EmptyCard) {
			EmptyCard card = (EmptyCard)item;
			
			TextView emptyText = (TextView)recycled.findViewById(R.id.empty_text);
			emptyText.setText(card.getEmptyText());
			
			recycled.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, (int)(MainActivity.deviceHeight * card.getDisplayHeightFraction())));
		}
		if (item instanceof SavedQueryCard) {
			SavedQueryCard card = (SavedQueryCard)item;
			
			recycled.setOnClickListener(card.getOnClickListener());
			
			LightItalicTextView title = (LightItalicTextView)recycled.findViewById(R.id.title);
			title.setText(card.getName());
			
			TextView dateCreated = (TextView)recycled.findViewById(R.id.date_created);
			Date date = new Date(card.getQuery().getDateCreated());
			dateCreated.setText("Created: "+DataStore.DATE_FORMAT.format(date));
		}
		
		return super.onViewCreated(index, recycled, item);
	}
}
