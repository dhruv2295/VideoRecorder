package com.firebot.videorecorder.camerautils;

import android.content.Context;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TimingLogger;
import android.view.Surface;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class CameraUtils {
	private static final String TAG = "CamUtils";

	public static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
	public static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
	public static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
	public static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

	static {
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
	}

	static {
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
	}


	/**
	 * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
	 * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
	 *
	 * @param choices The list of available sizes
	 * @return The video size
	 */
	public static Size chooseVideoSize(Size[] choices) {
		for (Size size : choices) {
			if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
				return size;
			}
		}
		Log.e(TAG, "Couldn't find any suitable video size");
		return choices[choices.length - 1];
	}

	/**
	 * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
	 * width and height are at least as large as the respective requested values, and whose aspect
	 * ratio matches with the specified value.
	 *
	 * @param choices     The list of sizes that the camera supports for the intended output class
	 * @param width       The minimum desired width
	 * @param height      The minimum desired height
	 * @param aspectRatio The aspect ratio
	 * @return The optimal {@code Size}, or an arbitrary one if none were big enough
	 */
	public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
		// Collect the supported resolutions that are at least as big as the preview Surface
		List<Size> bigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for (Size option : choices) {
			if (option.getHeight() == option.getWidth() * h / w &&
					option.getWidth() >= width && option.getHeight() >= height) {
				bigEnough.add(option);
			}
		}

		// Pick the smallest of those, assuming we found any
		if (bigEnough.size() > 0) {
			return Collections.min(bigEnough, new CompareSizesByArea());
		} else {
			Log.e(TAG, "Couldn't find any suitable preview size");
			return choices[0];
		}
	}

	/**
	 * Compares two {@code Size}s based on their areas.
	 */
	public static class CompareSizesByArea implements Comparator<Size> {

		@Override
		public int compare(Size lhs, Size rhs) {
			// We cast here to ensure the multiplications won't overflow
			return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
					(long) rhs.getWidth() * rhs.getHeight());
		}

	}

	public static String getVideoFilePath(Context context) {
		final File dir = context.getExternalFilesDir(null);
		return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
				+ System.currentTimeMillis() + ".mp4";
	}


	public static boolean mergeMediaFiles(boolean isAudio, ArrayList<String> sourceFiles, String targetFile) {
		try {
			String mediaKey = isAudio ? "soun" : "vide";
			List<Movie> listMovies = new ArrayList<>();
			for (String filename : sourceFiles) {
				listMovies.add(MovieCreator.build(filename));
			}
			List<Track> listTracks = new LinkedList<>();
			for (Movie movie : listMovies) {
				for (Track track : movie.getTracks()) {
					if (track.getHandler().equals(mediaKey)) {
						listTracks.add(track);
					}
				}
			}
			Movie outputMovie = new Movie();
			if (!listTracks.isEmpty()) {
				outputMovie.addTrack(new AppendTrack(listTracks.toArray(new Track[listTracks.size()])));
			}
			Container container = new DefaultMp4Builder().build(outputMovie);
			FileChannel fileChannel = new RandomAccessFile(targetFile, "rw").getChannel();
			container.writeContainer(fileChannel);
			fileChannel.close();
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Error merging media files. exception: " + e.getMessage());
			return false;
		}
	}

	public static boolean mixMedia(String videoF, String audioF, String targetFile) {
		TimingLogger timings = new TimingLogger("PreviewVideoFragment", "methodB");

		Movie video;
		try {
			video = MovieCreator.build(videoF);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		try {
			AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(audioF));
			video.addTrack(aacTrack);
		} catch (IOException e) {
			e.printStackTrace();
		}
		timings.addSplit("work A");

		Container out = new DefaultMp4Builder().build(video);

		FileOutputStream fos;

		try {
			fos = new FileOutputStream(targetFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		timings.addSplit("work B");
		BufferedWritableFileByteChannel byteBufferByteChannel = new BufferedWritableFileByteChannel(fos);

		try {
			out.writeContainer(byteBufferByteChannel);
			byteBufferByteChannel.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		timings.addSplit("work C");
		timings.dumpToLog();
		return true;
	}

	public static boolean packAudio(String audioF, String targetFile) {
		Movie audio;
		try {
			AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(audioF));
			audio = new Movie();
			audio.addTrack(aacTrack);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return false;
		}


		Container output = new DefaultMp4Builder().build(audio);

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(targetFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		BufferedWritableFileByteChannel byteBufferByteChannel = new BufferedWritableFileByteChannel(fos);
		try {
			output.writeContainer(byteBufferByteChannel);
			byteBufferByteChannel.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static class BufferedWritableFileByteChannel implements WritableByteChannel {
		private static final int BUFFER_CAPACITY = 1000000;

		private boolean isOpen = true;
		private final OutputStream outputStream;
		private final ByteBuffer byteBuffer;
		private final byte[] rawBuffer = new byte[BUFFER_CAPACITY];

		private BufferedWritableFileByteChannel(OutputStream outputStream) {
			this.outputStream = outputStream;
			this.byteBuffer = ByteBuffer.wrap(rawBuffer);
		}

		@Override
		public int write(ByteBuffer inputBuffer) {
			int inputBytes = inputBuffer.remaining();

			if (inputBytes > byteBuffer.remaining()) {
				dumpToFile();
				byteBuffer.clear();

				if (inputBytes > byteBuffer.remaining()) {
					throw new BufferOverflowException();
				}
			}

			byteBuffer.put(inputBuffer);

			return inputBytes;
		}

		@Override
		public boolean isOpen() {
			return isOpen;
		}

		@Override
		public void close() throws IOException {
			dumpToFile();
			isOpen = false;
		}

		private void dumpToFile() {
			try {
				outputStream.write(rawBuffer, 0, byteBuffer.position());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
