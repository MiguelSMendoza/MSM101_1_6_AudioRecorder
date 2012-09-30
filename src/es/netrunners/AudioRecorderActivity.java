package es.netrunners;

import java.io.File;
import java.io.IOException;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AudioRecorderActivity extends Activity implements Runnable {
	private static final String LOG_TAG = "AudioRecorder"; // TAG for errors
	private static File mFile;
	private Button mRecordButton = null;
	private MediaRecorder mRecorder = null;

	private Button mPlayButton = null;
	private MediaPlayer mPlayer = null;

	private Button mStopButton = null;

	private TextView Status;

	Chronometer mChronometer;

	private ProgressBar progressBar;

//	private String PATH = Environment.getExternalStorageDirectory()
//			.getAbsolutePath() + "/audio/";

	boolean mIsRecording = false; // Booleans to control the state
	boolean mIsPlaying = false;
	boolean player = false;
	boolean Editing = false;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.audiorecorder);
		setTitle("Audio Player");
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		Status = (TextView) findViewById(R.id.textView1);
		mRecordButton = (Button) findViewById(R.id.button0);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);

		// OnClickListener methods for buttons
		mRecordButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mIsRecording && !mIsPlaying) {
					startRecording();
					Status.setText("Recording...");
					// We change the aspect of the button during recording
					mRecordButton.setCompoundDrawablesWithIntrinsicBounds(0,
							R.drawable.rec_pressed, 0, 0);
					mIsRecording = !mIsRecording;
				}
			}
		});
		mPlayButton = (Button) findViewById(R.id.button1);
		mPlayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mIsPlaying && !mIsRecording) {
					if (mFile != null && mFile.length() > 0) {
						startPlaying();
						mPlayButton.setCompoundDrawablesWithIntrinsicBounds(0,
								R.drawable.audioplayer_button_pause, 0, 0);
						Status.setText("Playing...");
						mIsPlaying = !mIsPlaying;
					}
				} else if (mIsPlaying && !mIsRecording) {
					pausePlaying();
				}
			}
		});
		mStopButton = (Button) findViewById(R.id.button2);
		mStopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mIsRecording) {
					stopRecording();
					mIsRecording = !mIsRecording;
					progressBar.setProgress(0);
				}
				if (mIsPlaying) {
					stopPlaying();
					mIsPlaying = !mIsPlaying;
					progressBar.setProgress(0);
				}
				if (!mIsPlaying && mPlayer != null) {
					stopPlaying();
				}
				progressBar.setProgress(0);
				mChronometer.stop();
				mChronometer.setBase(SystemClock.elapsedRealtime());

			}
		});
	}

	// Functions which controls the playback or recording

	private void startPlaying() {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
			try {
				mPlayer.setDataSource(mFile.getAbsolutePath());
				mPlayer.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer arg0) {
						progressBar.setProgress(mPlayer.getDuration());
						mPlayer.release();
						mChronometer.stop();
						mPlayer = null;
						Status.setText("");
						mPlayButton.setCompoundDrawablesWithIntrinsicBounds(0,
								R.drawable.audioplayer_button_play, 0, 0);
						mIsPlaying = !mIsPlaying;
					}
				});
				mPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

					@Override
					public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
						progressBar.setProgress(mPlayer.getCurrentPosition());

					}
				});
				mPlayer.prepare();
				mPlayer.start();
				mChronometer.setBase(SystemClock.elapsedRealtime());
				mChronometer.start();
				progressBar.setProgress(0);
				progressBar.setMax(mPlayer.getDuration());
				new Thread(this).start();
			} catch (IOException e) {
				Log.e(LOG_TAG, "prepare() failed");
			}
		} else {
			mPlayer.start();

			mChronometer.start();
		}

	}

	private void pausePlaying() {
		if (mIsPlaying) {
			mPlayer.pause();
			mChronometer.stop();
			mPlayButton.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.audioplayer_button_play, 0, 0);
			Status.setText("PAUSED");
			mIsPlaying = !mIsPlaying;
		}
	}

	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
		Status.setText("Status");
		mPlayButton.setCompoundDrawablesWithIntrinsicBounds(0,
				R.drawable.audioplayer_button_play, 0, 0);
		progressBar.setProgress(0);

	}

	// Media Recorder properties, preparation and start
	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mFile = getFileName(mFile, "3gp");
		mRecorder.setOutputFile(mFile.getAbsolutePath());
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed" + e.getMessage());
		}

		mRecorder.start();
		progressBar.setProgress(0);
		mChronometer.setBase(SystemClock.elapsedRealtime());
		mChronometer.start();
	}

	private File getFileName(File mFile, String ext) {
		File path = new File(Environment.getExternalStorageDirectory()
				.getPath());
		try {
			mFile = File.createTempFile("temp", "." + ext, path);
		} catch (IOException e) {
		}
		return mFile;
	}

	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
		Status.setText("Status");
		mRecordButton.setCompoundDrawablesWithIntrinsicBounds(0,
				R.drawable.audioplayer_button_rec, 0, 0);
	}

	@Override
	public void run() {
		int currentPosition = 0;
		int total = mPlayer.getDuration();
		progressBar.setMax(total);
		while (mPlayer != null && currentPosition < total) {
			try {
				Thread.sleep(1000);
				currentPosition = mPlayer.getCurrentPosition();
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				return;
			}
			progressBar.setProgress(currentPosition);
		}
	}

}
