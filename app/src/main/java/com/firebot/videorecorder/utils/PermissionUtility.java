package com.firebot.videorecorder.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtility {

    private Activity activity;

    public class RequestCodes {
        public static final int RECORD = 101;
        public static final int EXTERNAL_READ = 102;
        public static final int EXTERNAL_WRITE = 103;
        public static final int SMS = 104;
        public static final int CONTACTS = 105;
        public static final int CAMERA = 106;
        public static final int PERMISSION_REQUESTS = 1;
    }

    public static boolean allGranted(int[] grantResults) {
        for (int grantResult : grantResults)
            if (grantResult == PackageManager.PERMISSION_DENIED)
                return false;
        return true;
    }

    public static boolean anyGranted(int[] grantResults) {
        for (int grantResult : grantResults)
            if (grantResult == PackageManager.PERMISSION_GRANTED)
                return true;
        return false;
    }

    public PermissionUtility(Activity activity) {
        this.activity = activity;
    }

    public void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.RECORD_AUDIO}, RequestCodes.RECORD);
    }

    public void requestExternalStorageReadPermission() {
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodes.EXTERNAL_READ);
    }

    public void requestExternalStorageWritePermission() {
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.EXTERNAL_WRITE);
    }

    public void requestSMSPermission() {
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.RECEIVE_SMS}, RequestCodes.SMS);
    }

    public void requestContactsPermission() {
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.READ_CONTACTS}, RequestCodes.CONTACTS);
    }

    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.CAMERA}, RequestCodes.CAMERA);
    }

    public boolean isRecordPermissionGranted() {
        return isPermissionSet(Manifest.permission.RECORD_AUDIO);
    }

    public boolean isReadExternalStoragePermissionGranted() {
        return isPermissionSet(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public boolean isWriteExternalStoragePermissionGranted() {
        return isPermissionSet(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public boolean isReadContactsPermissionGranted() {
        return isPermissionSet(Manifest.permission.READ_CONTACTS);
    }

    public boolean isSMSPermissionGranted() {
        return isPermissionSet(Manifest.permission.RECEIVE_SMS);
    }

    public boolean isContactsPermissionGranted() {
        return isPermissionSet(Manifest.permission.READ_CONTACTS);
    }

    public boolean isCameraPermissiongranted() {
        return isPermissionSet(Manifest.permission.CAMERA);
    }

    private boolean isPermissionSet(String tempPermission) {
        return ContextCompat.checkSelfPermission(this.activity, tempPermission) == 0;
    }

	public boolean allRecordingPermissionsGranted() {
		return isPermissionSet(Manifest.permission.READ_EXTERNAL_STORAGE)
				&& isPermissionSet(Manifest.permission.WRITE_EXTERNAL_STORAGE)
				&& isPermissionSet(Manifest.permission.CAMERA)
				&& isPermissionSet(Manifest.permission.RECORD_AUDIO);
	}

	public void getRecordingPermissions() {
		List<String> allNeededPermissions = new ArrayList<>();
		if (!isPermissionGranted(this.activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			allNeededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		if (!isPermissionGranted(this.activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			allNeededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
		}

		if (!isPermissionGranted(this.activity, Manifest.permission.CAMERA)) {
			allNeededPermissions.add(Manifest.permission.CAMERA);
		}

		if (!isPermissionGranted(this.activity, Manifest.permission.RECORD_AUDIO)) {
			allNeededPermissions.add(Manifest.permission.RECORD_AUDIO);
		}


			if (!allNeededPermissions.isEmpty()) {
				ActivityCompat.requestPermissions(this.activity, allNeededPermissions.toArray(new String[0]), RequestCodes.PERMISSION_REQUESTS);
			}
	}

	public static boolean isPermissionGranted(Context context, String permission) {
		return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
	}
}

