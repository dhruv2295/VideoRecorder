package com.firebot.videorecorder.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebot.videorecorder.R;
import com.firebot.videorecorder.utils.PermissionUtility;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PermissionUtility mPermissionUtil = new PermissionUtility(this);

		if (mPermissionUtil.allRecordingPermissionsGranted()) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, VideoStudioFragment.newInstance(), "studio")
					.commit();
		} else
			mPermissionUtil.getRecordingPermissions();
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			//RECORD AUDIO PERMISSION GRANTED
			case PermissionUtility.RequestCodes.PERMISSION_REQUESTS:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.container, VideoStudioFragment.newInstance(), "studio")
							.commit();
				}
				break;
		}
	}
}
