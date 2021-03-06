package com.yy.voice.voicerecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import com.yy.voice.graph.WaveformView;
import com.yy.voice.record.AudioRecordManager;
import com.yy.voice.soundFile.SoundFile;
import com.yy.voice.soundFile.SoundFile.InvalidInputException;

public class MainActivity extends Activity {

	private WaveformView mWaveformView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		createViews();

	}

	private void createViews() {

		findViewById(R.id.btn_start_record).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doStartRecord();
			}
		});

		findViewById(R.id.btn_stop_record).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doStopRecord();
			}
		});

		findViewById(R.id.btn_play_record).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doPlayRecord();
			}
		});

		findViewById(R.id.btn_show_graph).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doShowGraph();
			}
		});

		mWaveformView = (WaveformView) findViewById(R.id.btn_record_graph);
	}

	/**
	 * 开始录音
	 */
	private void doStartRecord() {
		AudioRecordManager.getInstance().startRecord();
	}

	/**
	 * 结束录音
	 */
	private void doStopRecord() {
		AudioRecordManager.getInstance().stopRecord();
	}

	/**
	 * 播放录音
	 */
	private void doPlayRecord() {

		try {
			MediaPlayer player = new MediaPlayer();
			player.setDataSource(AudioRecordManager.AudioFileTool.getWavFilePath());

			player.prepare();
			player.start();

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 把音频转化成波形图
	 */
	private void doShowGraph() {
		String rawFilePath = AudioRecordManager.AudioFileTool.getWavFilePath();
		File rawFile = new File(rawFilePath);
		if (rawFile.exists()) {

			try {
				SoundFile soundFile = SoundFile.create(rawFilePath, null);
				mWaveformView.setSoundFile(soundFile);

				mWaveformView.invalidate();
			} catch (InvalidInputException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
