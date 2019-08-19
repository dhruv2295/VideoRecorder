package com.firebot.videorecorder;


import com.firebot.videorecorder.inteface.VideoBasePresenter;
import com.firebot.videorecorder.inteface.VideoBaseView;

public interface VideoStudioContract {

	interface View extends VideoBaseView<Presenter> {


		void videoProcessed();

		void recordingStartedViewStatus();

		void recordingResumedViewStatus();

		void recordingPausedViewStatus();

		void recordingStoppedViewStatus();

		void recordingFinishedVideoStatus();
	}


	interface Presenter extends VideoBasePresenter {

		void setupFiles();

		void startRecording(boolean first);

		void pauseRecording();

		void stopRecording();

		void processVideo();

	}
}
