package com.firebot.videorecorder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.os.Process.THREAD_PRIORITY_URGENT_AUDIO;
import static android.os.Process.setThreadPriority;

public class AudioRecorder implements Runnable {
	/**
	 * The stream to write to.
	 */
	private final DataOutputStream mOutputStream;
	private static final String TAG = "AudioRecorder";
	/**
	 * If true, the background thread will continue to loop and record audio. Once false, the thread
	 * will shut down.
	 */
	private volatile boolean mAlive;

	/**
	 * The background thread recording audio for us.
	 */
	private Thread mThread;

	/**
	 * A simple audio recorder.
	 */
	private int PAUSE_STATE = 3;
	private int RECORD_STATE = 4;
	private volatile int currentState = -1;
	private double volume = 0;

	public AudioRecorder(String filePath) throws FileNotFoundException {
		mOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
	}

	/**
	 * @return True if actively recording. False otherwise.
	 */
	private boolean isRecording() {
		return mAlive;
	}

	/**
	 * Starts recording audio.
	 */
	public void start() {
		if (isRecording()) {
			Log.w(TAG, "Already running");
			return;
		}

		mAlive = true;
		mThread = new Thread(this);
		mThread.start();
		currentState = RECORD_STATE;
	}

	@Override
	public void run() {
		{
			setThreadPriority(THREAD_PRIORITY_URGENT_AUDIO);

			Buffer buffer = new Buffer();
			AudioRecord record =
					new AudioRecord(
							MediaRecorder.AudioSource.MIC,
							buffer.sampleRate,
							AudioFormat.CHANNEL_IN_STEREO,
							AudioFormat.ENCODING_PCM_16BIT,
							buffer.size);

			if (record.getState() != AudioRecord.STATE_INITIALIZED) {
				Log.w(TAG, "Failed to start recording");
				mAlive = false;
				return;
			}

			record.startRecording();

			// While we're running, we'll read the bytes from the AudioRecord and write them
			// to our output stream.
			try {
				while (isRecording()) {

					if(currentState == PAUSE_STATE) {
						continue;
					}


					int len = record.read(buffer.data, 0, buffer.size);
					if (len >= 0 && len <= buffer.size) {

						long v = 0;
						for (int i = 0; i < len; i++) {
							v += buffer.data[i] * buffer.data[i];
						}
						volume = 0;
						double amplitude = v / (double) len;

						if (amplitude > 0) {
							volume = 10 * Math.log10(amplitude);
						}
//					Log.w(TAG,"Volume:"+volume);
//						Log.w(TAG,"Amplitude:"+amplitude);

						ByteBuffer bufferBytes = ByteBuffer.allocate(len * 2); // 2 bytes per short
						bufferBytes.order(ByteOrder.LITTLE_ENDIAN); // save little-endian byte from short buffer
						bufferBytes.asShortBuffer().put(buffer.data, 0, len);
						byte[] bytes = bufferBytes.array();
						mOutputStream.write(bytes, 0, bytes.length);
						mOutputStream.flush();
					} else {
						Log.w(TAG, "Unexpected length returned: " + len);
					}
				}
			} catch (IOException e) {
				Log.e(TAG, "Exception with recording stream", e);
			} finally {
				mAlive = false;
				try {
					record.stop();

					mOutputStream.close();

				} catch (IllegalStateException e) {
					Log.e(TAG, "Failed to stop AudioRecord", e);
				} catch (IOException e) {
					Log.e(TAG, "Failed to close output stream", e);
				}
				record.release();
			}
		}

	}

	public void resume() {
		currentState = RECORD_STATE;
	}
	public void pause() {
		currentState = PAUSE_STATE;
	}

	public double getVolume()
	{
		return volume;
	}

	/**
	 * Stops recording audio.
	 */
	public void stop() {
		mAlive = false;

		try {
			mThread.join();
		} catch (InterruptedException e) {
			Log.e(TAG, "Interrupted while joining AudioRecorder thread", e);
			Thread.currentThread().interrupt();
		}
	}

	private static class Buffer extends AudioBuffer {
		@Override
		protected boolean validSize(int size) {
			return size != AudioRecord.ERROR && size != AudioRecord.ERROR_BAD_VALUE;
		}

		@Override
		protected int getMinBufferSize(int sampleRate) {
			return AudioRecord.getMinBufferSize(
					sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		}
	}
}

