package com.firebot.videorecorder.data;

import android.graphics.Point;

import java.io.File;
import java.util.ArrayList;


public class VideoSession {
	private File mainVideoFile;
	private File targetVideoFile;
	private File currentVideoFile;
	private ArrayList<String> videoFiles = new ArrayList<>();
	private File mMasterVoiceFile;
	private File mMasterDecodedVoiceFile;
	private File mixedRaw;
	private File mixedWav;
	private File finalRap;
	private File fxFile;
	private Point dimension;


	public File getMainVideoFile() {
		return mainVideoFile;
	}

	public void setMainVideoFile(File mainVideoFile) {
		this.mainVideoFile = mainVideoFile;
	}

	public File getTargetVideoFile() {
		return targetVideoFile;
	}

	public void setTargetVideoFile(File targetVideoFile) {
		this.targetVideoFile = targetVideoFile;
	}

	public File getCurrentVideoFile() {
		return currentVideoFile;
	}

	public void setCurrentVideoFile(File currentVideoFile) {
		this.currentVideoFile = currentVideoFile;
	}

	public ArrayList<String> getVideoFiles() {
		return videoFiles;
	}

	public void setVideoFiles(ArrayList<String> videoFiles) {
		this.videoFiles = videoFiles;
	}

	public File getMasterVoiceFile() {
		return mMasterVoiceFile;
	}

	public void setMasterVoiceFile(File mMasterVoiceFile) {
		this.mMasterVoiceFile = mMasterVoiceFile;
	}

	public File getMasterDecodedVoiceFile() {
		return mMasterDecodedVoiceFile;
	}

	public void setMasterDecodedVoiceFile(File mMasterDecodedVoiceFile) {
		this.mMasterDecodedVoiceFile = mMasterDecodedVoiceFile;
	}

	public void cleanUp() {


		if (null != getMasterVoiceFile() && getMasterVoiceFile().exists()) {
			getMasterVoiceFile().delete();
			setMasterVoiceFile(null);
		}

		if (null != getMasterDecodedVoiceFile() && getMasterDecodedVoiceFile().exists()) {
			getMasterDecodedVoiceFile().delete();
			setMasterDecodedVoiceFile(null);
		}

	}
}
