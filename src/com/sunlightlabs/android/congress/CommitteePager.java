package com.sunlightlabs.android.congress;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.sunlightlabs.android.congress.fragments.CommitteeListFragment;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.TitlePageAdapter;

public class CommitteePager extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pager_titled);
		
		Analytics.track(this, "/committees");
		
		ActionBarUtils.setTitle(this, "Committees");
		
		setupPager();
	}
	
	private void setupPager() {
		TitlePageAdapter adapter = new TitlePageAdapter(this);
		adapter.add("house", R.string.tab_house, CommitteeListFragment.forChamber("house"));
		adapter.add("senate", R.string.tab_senate, CommitteeListFragment.forChamber("senate"));
		adapter.add("joint", R.string.tab_joint, CommitteeListFragment.forChamber("joint"));
	}
}