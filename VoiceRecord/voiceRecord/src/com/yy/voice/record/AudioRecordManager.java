package com.yy.voice.record;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

/**
 * 音频记录管理类
 *
 * AudioRecord：语音的实时处理，可以用代码实现各种音频的封装。
 * 输出是PCM语音数据，如果保存成音频文件，是不能够被播放器播放的，所以必须先写代码实现数据编码以及压缩。
 *
 * http://www.cnblogs.com/Amandaliu/archive/2013/02/04/2891604.html
 *
 * @author yangyang.qian
 *
 */
public class AudioRecordManager {
	// 缓冲区字节大小
	private int bufferSizeInBytes = 0;

	// AudioName裸音频数据文件 ，麦克风
	private String pcmFileName = "";

	// NewAudioName可播放的音频文件
	private String wavFileName = "";

	private AudioRecord audioRecord;
	private boolean isRecord = false;// 设置正在录制的状态

	private static AudioRecordManager mInstance;

	private AudioRecordManager() {

	}

	public synchronized static AudioRecordManager getInstance() {
		if (mInstance == null) {
			mInstance = new AudioRecordManager();
		}
		return mInstance;
	}

	public int startRecord() {
		// 判断是否有外部存储设备sdcard
		if (AudioFileTool.isSdcardExit()) {
			if (isRecord) {
				return ErrorCode.E_STATE_RECODING;
			} else {
				if (audioRecord == null) {
					creatAudioRecord();
				}

				audioRecord.startRecording();
				// 让录制状态为true
				isRecord = true;
				// 开启音频文件写入线程
				new Thread(new AudioRecordThread()).start();

				return ErrorCode.SUCCESS;
			}

		} else {
			return ErrorCode.E_NOSDCARD;
		}

	}

	public void stopRecord() {
		close();
	}

	public long getRecordFileSize() {
		return AudioFileTool.getFileSize(wavFileName);
	}

	private void close() {
		if (audioRecord != null) {
			isRecord = false;// 停止文件写入
			audioRecord.stop();
			audioRecord.release();// 释放资源
			audioRecord = null;
		}
	}

	private void creatAudioRecord() {
		// 获取音频文件路径
		pcmFileName = AudioFileTool.getRawFilePath();
		wavFileName = AudioFileTool.getWavFilePath();

		// 获得缓冲区字节大小
		bufferSizeInBytes = AudioRecord.getMinBufferSize(AudioFileTool.AUDIO_SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

		// 创建AudioRecord对象
		audioRecord = new AudioRecord(AudioFileTool.AUDIO_INPUT, AudioFileTool.AUDIO_SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
	}

	/**
	 * 录制声音的线程
	 *
	 * @author yangyang.qian
	 *
	 */
	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateToFile(pcmFileName);// 往文件中写入裸数据
			copyWaveFile(pcmFileName, wavFileName);// 给裸数据加上头文件
		}
	}

	/**
	 * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
	 * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
	 * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
	 */
	private void writeDateToFile(String fileName) {
		// new一个byte数组用来存一些字节数据，大小为缓冲区大小
		byte[] audiodata = new byte[bufferSizeInBytes];
		FileOutputStream fos = null;
		int readsize = 0;
		try {
			File file = new File(fileName);
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(file);// 建立一个可存取字节的文件
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (isRecord == true) {
			readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
			if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
				try {
					fos.write(audiodata);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			if (fos != null) {
				fos.close();// 关闭写入流
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 这里得到可播放的音频文件
	private void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = AudioFileTool.AUDIO_SAMPLE_RATE;
		int channels = 2;
		long byteRate = 16 * AudioFileTool.AUDIO_SAMPLE_RATE * channels / 8;
		byte[] data = new byte[bufferSizeInBytes];
		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			addWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
			while (in.read(data) != -1) {
				out.write(data);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
	 * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有 自己特有的头文件。 pcm和wav的区别就在于多了这个头。
	 */
	private void addWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
			int channels, long byteRate) throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = 16; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}

	/**
	 * 相关常量和基本操作封装
	 *
	 * @author yangyang.qian
	 *
	 */
	public static class AudioFileTool {
		// 音频输入-麦克风
		public final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;

		// 采用频率
		// 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
		public final static int AUDIO_SAMPLE_RATE = 44100; // 44.1KHz,普遍使用的频率

		// 录音输出文件
		private final static String AUDIO_RAW_FILENAME = "RawAudio.raw";
		private final static String AUDIO_WAV_FILENAME = "FinalAudio.wav";

		/**
		 * 判断是否有外部存储设备sdcard
		 *
		 * @return true | false
		 */
		public static boolean isSdcardExit() {
			if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				return true;
			} else {
				return false;
			}
		}

		/**
		 * 获取麦克风输入的原始音频流文件路径
		 *
		 * @return
		 */
		public static String getRawFilePath() {
			String mAudioRawPath = "";
			if (isSdcardExit()) {
				String fileBasePath = Environment.getExternalStorageDirectory().getAbsolutePath();
				mAudioRawPath = fileBasePath + "/" + AUDIO_RAW_FILENAME;
			}

			return mAudioRawPath;
		}

		/**
		 * 获取编码后的WAV格式音频文件路径
		 *
		 * @return
		 */
		public static String getWavFilePath() {
			String mAudioWavPath = "";
			if (isSdcardExit()) {
				String fileBasePath = Environment.getExternalStorageDirectory().getAbsolutePath();
				mAudioWavPath = fileBasePath + "/" + AUDIO_WAV_FILENAME;
			}
			return mAudioWavPath;
		}

		/**
		 * 获取文件大小
		 *
		 * @param path
		 *            ,文件的绝对路径
		 * @return
		 */
		public static long getFileSize(String path) {
			File mFile = new File(path);
			if (!mFile.exists()) {
				return -1;
			}
			return mFile.length();
		}

	}
}