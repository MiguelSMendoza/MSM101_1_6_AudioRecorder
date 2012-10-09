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

/**
 * @author Miguel S. Mendoza
 * 
 *         Clase AudioRecorderActivity Implementa la clase Runnable para
 *         controlar el objeto ProgressBar
 * 
 */
public class AudioRecorderActivity extends Activity implements Runnable {
	private static final String LOG_TAG = "AudioRecorder"; // TAG para errores

	// Fichero temporal utilizado tanto para grabar el audio, como para
	// reproducirlo
	private static File mFile;

	// Objetos que se encargan de grabar y reproducir audio
	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;

	// Objetos de Interfaz
	private Button mRecordButton = null;
	private Button mPlayButton = null;
	private Button mStopButton = null;
	private TextView Status;
	Chronometer mChronometer;
	private ProgressBar progressBar;

	// Booleanos para controlar el estado
	boolean mIsRecording = false;
	boolean mIsPlaying = false;
	boolean player = false;
	boolean Editing = false;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.audiorecorder);
		setTitle("Audio Player");

		// Enlazamos con los objetos de Interfaz
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		Status = (TextView) findViewById(R.id.textView1);
		mRecordButton = (Button) findViewById(R.id.button0);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);

		// OnClickListeners para los botones
		mRecordButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mIsRecording && !mIsPlaying) {
					// Llamamos al método que se encargará de iniciar el objeto
					// MediaRecorder
					startRecording();
					Status.setText("Recording...");
					// Cambiamos el aspecto de los botones durante la
					// reproducción
					mRecordButton.setCompoundDrawablesWithIntrinsicBounds(0,
							R.drawable.rec_pressed, 0, 0);
					// Almacenamos el estado actual
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
						// Llamamos al método que se encargará de iniciar el
						// objeto MediaPlayer
						startPlaying();
						// Cambiamos el aspecto de los botones durante la
						// reproducción
						mPlayButton.setCompoundDrawablesWithIntrinsicBounds(0,
								R.drawable.audioplayer_button_pause, 0, 0);
						Status.setText("Playing...");
						// Almacenamos el estado actual
						mIsPlaying = !mIsPlaying;
					}
				} else if (mIsPlaying && !mIsRecording) {
					// Si estaba reproduciendo pausamos la reproducción
					pausePlaying();
				}
			}
		});
		// El botón STOP realizará diferentes acciones dependiendo del estado
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

	/**
	 * Método que inicia el objeto MediaPlayer con el fichero mFile
	 */
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

	/**
	 * Método que Pausa la reproducción de MediaPlayer
	 */
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

	/**
	 * Inicializa un objeto MediaRecorder estableciendo como fichero destino
	 * mFile y comienza a obtener sonido del micrófono
	 */
	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mFile = getFileName("3gp");
		Log.e("Error", mFile.getAbsolutePath());
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

	/**
	 * Crea un fichero temporal con la extensión ext
	 * 
	 * @param ext la extensión del fichero temporal que queremos crear
	 * @return fichero temporal a utilizar
	 */
	private File getFileName(String ext) {
		File path = new File(Environment.getExternalStorageDirectory()
				.getPath());
		File file = null;
		try {
			file = File.createTempFile("temp", "." + ext, path);
		} catch (IOException e) {
		}
		return file;
	}

	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
		Status.setText("Status");
		mRecordButton.setCompoundDrawablesWithIntrinsicBounds(0,
				R.drawable.audioplayer_button_rec, 0, 0);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * 
	 * Controla la ejecución de un hilo para controlar el progreso del objeto ProgressBar
	 */
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
