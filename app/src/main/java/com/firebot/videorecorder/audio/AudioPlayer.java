package com.firebot.videorecorder.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static android.os.Process.setThreadPriority;

public class AudioPlayer {
	/** The audio stream we're reading from. */
	private static final String TAG = "AudioPlayer";
	private final InputStream mInputStream;

	/**
	 * If true, the background thread will continue to loop and play audio. Once false, the thread
	 * will shut down.
	 */
	private volatile boolean mAlive;

	/** The background thread recording audio for us. */
	private Thread mThread;

	/**
	 * A simple audio player.
	 *
	 * @param path The input stream of the recording.
	 */
	public AudioPlayer(String path) throws FileNotFoundException {
		mInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
	}

	/** @return True if currently playing. */
	public boolean isPlaying() {
		return mAlive;
	}

	/** Starts playing the stream. */
	public void start() {
		mAlive = true;
		mThread =
				new Thread() {
					@Override
					public void run() {
						setThreadPriority(THREAD_PRIORITY_AUDIO);

						Buffer buffer = new Buffer();
						AudioTrack audioTrack = new AudioTrack(
										AudioManager.STREAM_MUSIC,
										44100,
										AudioFormat.CHANNEL_IN_STEREO,
										AudioFormat.ENCODING_PCM_16BIT,
										buffer.size,
										AudioTrack.MODE_STREAM);
						audioTrack.play();

						int len;
						try {
							while (isPlaying()) {

								ByteBuffer bufferBytes = ByteBuffer.allocate(buffer.data.length * 2); // 2 bytes per short
								bufferBytes.order(ByteOrder.LITTLE_ENDIAN); // save little-endian byte from short buffer
								bufferBytes.asShortBuffer().put(buffer.data, 0, buffer.data.length);
								byte[] bytes = bufferBytes.array();

								if ((len = mInputStream.read(bytes)) > 0)
									audioTrack.write(buffer.data, 0, len);
								else
									break;
							}

						} catch (IOException e) {
							Log.e(TAG, "Exception with playing stream", e);
						} finally {
							stopInternal();
							audioTrack.release();
							onFinish();
						}
					}
				};
		mThread.start();
	}

	private void stopInternal() {
		mAlive = false;
		try {
			mInputStream.close();
		} catch (IOException e) {
			Log.e(TAG, "Failed to close input stream", e);
		}
	}

	/** Stops playing the stream. */
	public void stop() {
		stopInternal();
		try {
			mThread.join();
		} catch (InterruptedException e) {
			Log.e(TAG, "Interrupted while joining AudioRecorder thread", e);
			Thread.currentThread().interrupt();
		}
	}

	/** The stream has now ended. */
	protected void onFinish() {}

	private static class Buffer extends AudioBuffer {
		@Override
		protected boolean validSize(int size) {
			return size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE;
		}

		@Override
		protected int getMinBufferSize(int sampleRate) {
			return AudioTrack.getMinBufferSize(
					sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		}
	}
}

