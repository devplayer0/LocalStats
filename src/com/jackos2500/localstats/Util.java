package com.jackos2500.localstats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardListView;
import com.jackos2500.localstats.card.CriteriaCard;
import com.jackos2500.localstats.data.disk.DataStore.DataQuery;
import com.jackos2500.localstats.data.disk.DataStore.DataQuerySet;
import com.jackos2500.localstats.fragment.ResultsFragment;
import com.jackos2500.localstats.ui.MultiCardAdapter;

public class Util {
	public static final String API_URL = "http://localstats.herokuapp.com/";
	public static JSONObject apiCall(String action, JSONObject req) throws ClientProtocolException, IOException, JSONException {
		HttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setConnectionTimeout(client.getParams(), 20000);
		
		HttpPost post = new HttpPost(API_URL+action);
		StringEntity se = new StringEntity(req.toString());
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		post.setEntity(se);
		
		HttpResponse response = client.execute(post);
		String reply = EntityUtils.toString(response.getEntity());
		JSONObject replyJson = new JSONObject(reply);
		
		return replyJson;
	}
	public static int randomColor() {
		Random random = new Random();
		
		int r = random.nextInt(255);
		int g = random.nextInt(255);
		int b = random.nextInt(255);
		
		return Color.rgb(r, g, b);
	}
	public static int randomColorWithAlpha(int alpha) {
		Random random = new Random();
		
		int r = random.nextInt(255);
		int g = random.nextInt(255);
		int b = random.nextInt(255);
		
		return Color.argb(alpha, r, g, b);
	}
	public static int[] randomColorsWAndWOAlpha(int alpha) {
		Random random = new Random();
		
		int r = random.nextInt(255);
		int g = random.nextInt(255);
		int b = random.nextInt(255);
		
		return new int[] {Color.rgb(r, g, b), Color.argb(alpha, r, g, b)};
	}
	@SuppressWarnings({ "rawtypes", "deprecation" })
	public static void reloadCard(CardListView list, CardAdapter listAdapter, Card card) {
		int start = list.getFirstVisiblePosition();
		for (int i = start; i < list.getLastVisiblePosition() + 1; i++) {
			if (list.getItemAtPosition(i).equals(card)) {
				System.out.println("reloading card...");
				View view = list.getChildAt(i - start);
				listAdapter.getView(i, view, list);
				break;
			}
		}
	}
	public static ResultsFragment getResultsFragment(DataQuerySet query, boolean allowSave) {
		Bundle args = new Bundle();
		
		args.putStringArrayList("eds", (ArrayList<String>)query.eds);
		args.putBoolean("allowSave", allowSave);
				
		Bundle queries = new Bundle();
		
		int i = 0;
		for (DataQuery q : query.queries) {
			Bundle queryNode = new Bundle();
			
			queryNode.putString("measure", q.measure);
					
			queryNode.putString("dataset", q.dataset);
			
			Bundle values = new Bundle();
			for (Entry<String, String> e : q.values.entrySet()) {
				values.putString(e.getKey(), e.getValue());
			}
			queryNode.putBundle("values", values);
			queries.putBundle(""+i, queryNode);
			i++;
		}
		args.putBundle("queries", queries);
				
		ResultsFragment resultsFragment = new ResultsFragment();
		resultsFragment.setArguments(args);
		return resultsFragment;
	}
	@SuppressWarnings("rawtypes")
	public static ResultsFragment getResultsFragment(List<String> eds, MultiCardAdapter adapter, boolean allowSave) {
		Bundle args = new Bundle();
		
		args.putStringArrayList("eds", (ArrayList<String>)eds);
		args.putBoolean("allowSave", allowSave);
				
		Bundle queries = new Bundle();
				
		List cards = adapter.getItems();
		for (int i = 0; i < cards.size(); i++) {
			Object item = cards.get(i);
			if (item instanceof CriteriaCard) {
				CriteriaCard card = (CriteriaCard)item;
				Bundle query = new Bundle();
						
				String measure = card.getMeasure();
				query.putString("measure", measure);
						
				String dataset = card.getSelectedDataset();
				query.putString("dataset", dataset);
				
				Bundle values = new Bundle();
				for (Entry<String, String> e : card.getSelectedValueIris().entrySet()) {
					values.putString(e.getKey(), e.getValue());
				}
				query.putBundle("values", values);
				queries.putBundle(""+i, query);
			}
		}
		args.putBundle("queries", queries);
				
		ResultsFragment resultsFragment = new ResultsFragment();
		resultsFragment.setArguments(args);
		return resultsFragment;
	}
}
