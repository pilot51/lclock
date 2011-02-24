package com.pilot51.lclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Main extends Activity implements OnClickListener {
	private Common common;
	private Button btnNASA, btnSfN;
	
	protected Common newCommon() {
		return new Common(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		common = newCommon();
		setContentView(R.layout.main);
		btnNASA = (Button) findViewById(R.id.btnNASA);
		btnNASA.setOnClickListener(this);
		btnSfN = (Button) findViewById(R.id.btnSfN);
		btnSfN.setOnClickListener(this);
		common.ad();
	}
	
	@Override
	public void onClick(View src) {
		Intent i = common.intentList();
		switch (src.getId()) {
			case R.id.btnNASA:
				i.putExtra("source", 1);
				startActivity(i);
				break;
			case R.id.btnSfN:
				i.putExtra("source", 2);
				startActivity(i);
				break;
		}
	}
	
	public void finish() {
		super.finish();
		Process.killProcess(Process.myPid());
	}
}