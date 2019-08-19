package com.firebot.videorecorder.ui;


import android.annotation.SuppressLint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCaptureConfig;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.firebot.videorecorder.R;
import com.firebot.videorecorder.VideoCapture;
import com.firebot.videorecorder.data.VideoSession;
import com.firebot.videorecorder.VideoStudioContract;
import com.firebot.videorecorder.VideoStudioPresenter;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import com.firebot.videorecorder.camerautils.AutoFitPreviewBuilder;

/**
 * A simple {@link Fragment} subclass.
 */
public class VideoStudioFragment extends Fragment implements VideoStudioContract.View {

	private final int FLAGS_FULLSCREEN = View.SYSTEM_UI_FLAG_LOW_PROFILE |
			View.SYSTEM_UI_FLAG_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
			View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
			View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

	private VideoStudioContract.Presenter mPresenter;
	private VideoSession studioSession;


	@BindView(R.id.view_finder)
	TextureView viewFinder;
	@BindView(R.id.videoView)
	VideoView videoView;
	@BindView(R.id.camera_switch_button)
	ImageButton cameraSwitchButton;
	@BindView(R.id.camera_capture_button)
	ToggleButton record_button;


	private CameraX.LensFacing lensFacing = CameraX.LensFacing.FRONT;
	private VideoCapture videoCapture;
//	private AudioRecorder audioRecorder;

	private boolean mIsRecordingVideo;
	private Point dimensions = new Point();

	public static VideoStudioFragment newInstance(Bundle extraBundle) {
		VideoStudioFragment videoStudioFragment = new VideoStudioFragment();
		videoStudioFragment.setArguments(extraBundle);
		return videoStudioFragment;
	}

	public static VideoStudioFragment newInstance() {
		return new VideoStudioFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		studioSession = new VideoSession();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_video, container, false);
		ButterKnife.bind(this, view);
		getActivity().getWindowManager().getDefaultDisplay().getRealSize(dimensions);

		viewFinder.post(() -> {
			bindCameraUseCases();
			new VideoStudioPresenter(this, studioSession);
			mPresenter.setupFiles();

		});


		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@SuppressLint("RestrictedApi")
	private void bindCameraUseCases() {

		// Make sure that there are no other use cases bound to CameraX
		CameraX.unbindAll();

		DisplayMetrics metrics = new DisplayMetrics();

		viewFinder.getDisplay().getRealMetrics(metrics);

		Size screenSize = new Size(metrics.widthPixels, metrics.heightPixels);
		Rational screenAspectRatio = new Rational(metrics.widthPixels, metrics.heightPixels);

		// Set up the view finder use case to display camera preview

		PreviewConfig viewFinderConfig =
				new PreviewConfig.Builder()
						.setLensFacing(lensFacing)
						.setTargetResolution(screenSize)
						.setTargetRotation(viewFinder.getDisplay().getRotation())
						.build();


		// Use the auto-fit preview builder to automatically handle size and orientation changes

		Preview preview = AutoFitPreviewBuilder.Companion.build(viewFinderConfig, viewFinder);


		VideoCaptureConfig videoCaptureConfig = new VideoCaptureConfig.Builder().
				setLensFacing(lensFacing).
				// We request aspect ratio but no resolution to match preview config but letting
				// CameraX optimize for whatever specific resolution best fits requested capture mode
						setTargetAspectRatio(screenAspectRatio).
						setTargetRotation(viewFinder.getDisplay().getRotation())
				.build();


		videoCapture = new VideoCapture(videoCaptureConfig);


		CameraX.bindToLifecycle(this, preview, videoCapture);
	}


	private boolean first = true;

	@OnCheckedChanged(R.id.camera_capture_button)
	void _record(CompoundButton button, boolean checked) {

		if (checked) {
			Toast.makeText(getActivity().getApplicationContext(), "Started", Toast.LENGTH_SHORT).show();
			mPresenter.startRecording(first);
			videoCapture.startRecording(studioSession.getCurrentVideoFile(), new VideoCapture.OnVideoSavedListener() {
				@Override
				public void onVideoSaved(File file) {
					mPresenter.processVideo();
				}

				@Override
				public void onError(VideoCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {

				}
			});

			if (first) {
//				try {
//					audioRecorder = new AudioRecorder(studioSession.getMasterDecodedVoiceFile().getAbsolutePath());
//					audioRecorder.start();
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//				}
				recordingStartedViewStatus();
			} else {
				recordingResumedViewStatus();
//				audioRecorder.resume();
			}
			if (first) first = false;

			mIsRecordingVideo = true;

		} else {
			videoCapture.stopRecording();

			mIsRecordingVideo = false;
//			audioRecorder.pause();
			mPresenter.pauseRecording();
			Toast.makeText(getActivity().getApplicationContext(), "Paused", Toast.LENGTH_SHORT).show();

		}
	}

	private int second = 0;

	@Override
	public void recordingStartedViewStatus() {
		cameraSwitchButton.setEnabled(false);
	}

	@Override
	public void recordingPausedViewStatus() {
		cameraSwitchButton.setEnabled(true);
	}

	@Override
	public void recordingResumedViewStatus() {
		cameraSwitchButton.setEnabled(false);
	}

	@Override
	public void recordingStoppedViewStatus() {
		cameraSwitchButton.setEnabled(true);
		record_button.setChecked(false);
	}

	@Override
	public void recordingFinishedVideoStatus() {


	}


	@Override
	public void videoProcessed() {
		getActivity().runOnUiThread(() -> {

			Uri uri = FileProvider.getUriForFile(
					getContext(),
					getContext().getApplicationContext()
							.getPackageName() + ".provider", studioSession.getMainVideoFile());

			videoView.setVideoURI(uri);

		});

	}

	@SuppressLint("RestrictedApi")
	@OnClick(R.id.camera_switch_button)
	void _switchCamera() {

		if (CameraX.LensFacing.FRONT == lensFacing) {
			lensFacing = CameraX.LensFacing.BACK;
		} else {
			lensFacing = CameraX.LensFacing.FRONT;
		}
		try {
			// Only bind use cases if we can query a camera with this orientation
			CameraX.getCameraWithLensFacing(lensFacing);
			bindCameraUseCases();
		} catch (CameraInfoUnavailableException exc) {
			// Do nothing
		}

	}


	@Override
	public void onResume() {
		super.onResume();
		if (null != getActivity().getWindow()) {
			getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		View decorView = getActivity().getWindow().getDecorView();
		// Hide the status bar.
		decorView.setSystemUiVisibility(FLAGS_FULLSCREEN);

		viewFinder.post(this::bindCameraUseCases);
	}

	@Override
	public void onPause() {
		CameraX.unbindAll();
		super.onPause();
		if (mIsRecordingVideo) {
			mPresenter.stopRecording();
		}
	}


	@Override
	public void onDestroyView() {
		super.onDestroyView();
		CameraX.unbindAll();
		studioSession.cleanUp();
	}

	@Override
	public void setPresenter(VideoStudioContract.Presenter presenter) {
		this.mPresenter = presenter;
	}
}
