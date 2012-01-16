package com.sunlightlabs.android.congress;

import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.sunlightlabs.android.congress.fragments.LegislatorListFragment;
import com.sunlightlabs.android.congress.providers.LegislatorSuggestionsProvider;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.LocationUtils;
import com.sunlightlabs.android.congress.utils.LocationUtils.LocationListenerTimeout;
import com.sunlightlabs.android.congress.utils.LocationUtils.LocationTimer;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;
import com.sunlightlabs.android.congress.utils.Utils;

public class LegislatorSearch extends FragmentActivity implements LocationListenerTimeout {
	
	String query;
	String state;
	boolean location;
	
	TitlePageAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		
		Intent intent = getIntent();
		query = intent.getStringExtra(SearchManager.QUERY);
		state = intent.getStringExtra("state");
		location = intent.getBooleanExtra("location", false); 
		
		if (query != null) // may come in from the state list
			query = query.trim();
	    
		setupControls();
		setupPager();
	}
	
	
	public void setupPager() {
		adapter = new TitlePageAdapter(this);
		findViewById(R.id.pager_titles).setVisibility(View.GONE);
		
		// location search
		if (location) {
			Analytics.track(this, "/legislators/location");
			ActionBarUtils.setTitle(this, "Your Legislators");
			locate();
		} 
		
		// state search
		else if (state != null) {
			Analytics.track(this, "/legislators/state");
			ActionBarUtils.setTitle(this, "Legislators from " + Utils.stateCodeToName(this, state));
			ActionBarUtils.setTitleSize(this, 16);
			adapter.add("legislators_state", "Not seen", LegislatorListFragment.forState(state));
		}
		
		// zip code search
		else if (Pattern.compile("^\\d+$").matcher(query).matches()) {
			Analytics.track(this, "/legislators/zip");
			ActionBarUtils.setTitle(this, "Legislators For " + query);
			ActionBarUtils.setTitleSize(this, 16);
			
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this,LegislatorSuggestionsProvider.AUTHORITY, LegislatorSuggestionsProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
			
			adapter.add("legislators_zip", "Not seen", LegislatorListFragment.forZip(query));
		}
		
		// last name search
		else {
			Analytics.track(this, "/legislators/lastname");
			ActionBarUtils.setTitle(this, "Legislators Named \"" + query + "\"");
			ActionBarUtils.setTitleSize(this, 16);
			
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this,LegislatorSuggestionsProvider.AUTHORITY, LegislatorSuggestionsProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
			
			adapter.add("legislators_lastname", "Not seen", LegislatorListFragment.forLastName(query));
		}
	}
	
	public void setupControls() {
		refresh = findViewById(R.id.action_2);
		spinner = findViewById(R.id.action_spinner);
		
		if (location) {
			ActionBarUtils.setActionButton(this, R.id.action_2, R.drawable.refresh, new View.OnClickListener() {
				public void onClick(View v) {
					locate();
						
				}
			});
		} else {
			ActionBarUtils.setActionButton(this, R.id.action_2, R.drawable.location, new View.OnClickListener() {
				public void onClick(View v) {
					startActivity(new Intent(LegislatorSearch.this, LegislatorSearch.class).putExtra("location", true));
				}
			});
		}
		
		ActionBarUtils.setActionButton(this, R.id.action_1, R.drawable.search, new View.OnClickListener() {
			public void onClick(View v) { 
				onSearchRequested();
			}
		});
	}
	
	public void locate() {
		findViewById(R.id.pager).setVisibility(View.GONE);
		findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
		Utils.setLoading(this, R.string.menu_location_updating);
		updateLocation();
	}
	
	public void onLocated(double latitude, double longitude) {
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		findViewById(R.id.pager).setVisibility(View.VISIBLE);
		adapter.add("legislators_location", "Not seen", LegislatorListFragment.forLocation(latitude, longitude));
	}
	
	public void onNotLocated() {
		Utils.showEmpty(this, R.string.menu_location_no_location);
	}
	
	// Location finding code
	
	private View refresh;
	private View spinner;
	
	private LocationTimer timer;
	private boolean relocating = false;
	
	@Override
	public void onStop() {
		super.onStop();
		cancelTimer();
		stopRelocating();
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			onTimeout((String) msg.obj);
		}
	};
	

	private void stopRelocating() {
		relocating = false;
		spinner.setVisibility(View.GONE);
		refresh.setVisibility(View.VISIBLE);
	}
	
	private void startRelocating() {
		relocating = true;
		refresh.setVisibility(View.GONE);
		spinner.setVisibility(View.VISIBLE);
	}
	
	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			Log.d(Utils.TAG, "LegislatorSearch - cancelTimer(): end updating timer");
		}
	}
	
	private void updateLocation() {
		startRelocating();
		timer = LocationUtils.requestLocationUpdate(this, handler, LocationManager.GPS_PROVIDER);
	}
	
	public void onLocationUpdateError() {
		if (relocating) {
			Log.d(Utils.TAG, "LegislatorSearch - onLocationUpdateError(): cannot update location");
			stopRelocating();
			onNotLocated();
		}
	}
	
	public void onLocationChanged(Location location) {
		cancelTimer();
		stopRelocating();
		onLocated(location.getLatitude(), location.getLongitude());
	}
	
	public void onProviderDisabled(String provider) {}
	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	public void onTimeout(String provider) {
		Log.d(Utils.TAG, "LegislatorSearch - onTimeout(): timeout for provider " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			timer = LocationUtils.requestLocationUpdate(this, handler, LocationManager.NETWORK_PROVIDER);
			Log.d(Utils.TAG, "LegislatorSearch - onTimeout(): requesting update from network");
		} else
			onLocationUpdateError();
	}
	
}