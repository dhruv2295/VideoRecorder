package com.firebot.videorecorder;

import android.annotation.SuppressLint;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.firebot.videorecorder.data.VideoSession;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


import static androidx.core.util.Preconditions.checkNotNull;
import static com.firebot.videorecorder.camerautils.CameraUtils.mergeMediaFiles;

@SuppressLint("RestrictedApi")
public class VideoStudioPresenter implements VideoStudioContract.Presenter {

	@NonNull
	private VideoStudioContract.View mStudioView;
	private VideoSession studioSession;
	private boolean audioMixed = false;
	private boolean videoMixed = false;

	private File mediaStorageDir;

	public VideoStudioPresenter(VideoStudioContract.View videoStudioFragment, VideoSession studioSession) {
		mStudioView = checkNotNull(videoStudioFragment, "tasksView cannot be null!");
		mStudioView.setPresenter(this);
		this.studioSession = studioSession;

		mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS), "VideoRecorder");

		if (!mediaStorageDir.exists()) mediaStorageDir.mkdirs();
	}


	@Override
	public void setupFiles() {
		try {
			createTrackFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void createTrackFiles() throws IOException {
		File currentVideoFile = new File(mediaStorageDir.getPath() + File.separator + "Current_VID" + ".mp4");
		currentVideoFile.delete();

		File mainVideoFile = new File(mediaStorageDir.getPath() + File.separator + "Main_VID" + ".mp4");
		mainVideoFile.delete();
		File targetVideoFile = new File(mediaStorageDir.getPath() + File.separator + "Target_VID" + ".mp4");
		targetVideoFile.delete();
		try {
			currentVideoFile.createNewFile();
			mainVideoFile.createNewFile();
			targetVideoFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}



/*
		File masterRecordingRaw = new File(mediaStorageDir.getAbsolutePath(), "masterVocalRaw.pcm");
		if (masterRecordingRaw.exists())
			masterRecordingRaw.delete();

		masterRecordingRaw.createNewFile();


		File masterVocal = new File(mediaStorageDir.getAbsolutePath(), "masterVocal.wav");
		if (masterVocal.exists())
			masterVocal.delete();

		masterVocal.createNewFile();



		File finalRap = new File(mediaStorageDir.getAbsolutePath() + File.separator + "rap.aac");
		if (finalRap.exists())
			finalRap.delete();

		finalRap.createNewFile();
*/


		studioSession.setCurrentVideoFile(currentVideoFile);
		studioSession.setMainVideoFile(mainVideoFile);
		studioSession.setTargetVideoFile(targetVideoFile);
/*
		studioSession.setFinalRap(finalRap);
		studioSession.setMasterVoiceFile(masterVocal);
		studioSession.setMasterDecodedVoiceFile(masterRecordingRaw);
*/

	}

	@Override
	public void startRecording(boolean first) {

	}

	@Override
	public void processVideo() {
		studioSession.getVideoFiles().clear();

		ArrayList<String> videoFiles = new ArrayList<>();
		videoFiles.clear();

		if (studioSession.getMainVideoFile() != null && studioSession.getMainVideoFile().length() > 0)
			videoFiles.add(studioSession.getMainVideoFile().getAbsolutePath());

		videoFiles.add(studioSession.getCurrentVideoFile().getAbsolutePath());
		studioSession.setVideoFiles(videoFiles);

		if (videoFiles.size() == 2) {
			mergeMediaFiles(false, videoFiles, studioSession.getTargetVideoFile().getAbsolutePath());
			studioSession.getTargetVideoFile().renameTo(studioSession.getMainVideoFile());
		} else {
			studioSession.getCurrentVideoFile().renameTo(studioSession.getMainVideoFile());
		}

		mStudioView.videoProcessed();
		videoMixed = true;
		saveMixAndUpload();

	}

	@Override
	public void pauseRecording() {

		mStudioView.recordingPausedViewStatus();
	}

	@Override
	public void stopRecording() {

		mStudioView.recordingStoppedViewStatus();

	}


	private void saveMixAndUpload() {
		if ( audioMixed && videoMixed) {
			mStudioView.recordingFinishedVideoStatus();
		}

	}


	@Override
	public void subscribe() {

	}

	@Override
	public void unsubscribe() {

	}
}
