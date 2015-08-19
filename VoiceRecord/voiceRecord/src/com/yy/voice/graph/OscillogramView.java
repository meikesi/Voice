package com.yy.voice.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 *
 * 显示波形图的view
 *
 * @author yangyang.qian
 *
 */
public class OscillogramView extends View {

	private byte[] pcmData;

	public OscillogramView(Context context) {
		this(context, null);
	}

	public OscillogramView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public OscillogramView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	/**
	 * 传入pcm数据流
	 *
	 * @param pcmData
	 */
	public void setPcmData(byte[] pcmData) {
		this.pcmData = pcmData;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int width = getWidth();
		int height = getHeight();

		Paint paint = new Paint();
		paint.setColor(Color.RED);

		canvas.drawRect(new Rect(0, 0, width, height), paint);

		if (pcmData != null) {
			drawOscillogram(canvas, pcmData);
		}
	}

	/**
	 * 绘制音频波形图
	 *
	 * @param canvas
	 * @param data
	 */
	private void drawOscillogram(Canvas canvas, byte[] data) {

		int viewWidth = getWidth();
		int x_axis = 1;
		int canvasOriginX = 0;
		int dataLen = data.length;

		Paint paint = new Paint();
		paint.setColor(Color.GREEN);

		int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
		int vPointStep = 1;
		int vDataStep = 1;
		int vDataIndex = 0;

		if (dataLen >= viewWidth) {
			vPointStep = 1;
			vDataStep = dataLen / viewWidth;
			vDataIndex = -canvasOriginX;
		} else {
			vPointStep = viewWidth / dataLen;
			vDataStep = 1;
			vDataIndex = -canvasOriginX / vPointStep;
		}

		// 每隔几个点选择一个，赋值100，效率提供100
		int dataHole = 1;

		// 对于view中的每个点进行绘图
		for (int i = 0; i < viewWidth; i += vPointStep, vDataIndex++) {
			int vPlusVal = 0;
			int vNegaVal = 0;
			int vCurVal = 0;
			for (int j = 0; j < vDataStep; j += dataHole) {
				int vIdx = vDataIndex * vDataStep + j;
				if (!(vIdx < dataLen)) {
					break;
				}
				if (vIdx < dataLen && vIdx >= 0) {
					vCurVal = (data[vIdx * x_axis]) / (65535 / 2);
				} else {
					vCurVal = 0;
				}

				if (vCurVal > 0) {
					if (vCurVal > vPlusVal) {
						vPlusVal = vCurVal;
					}
				} else if (vCurVal < 0) {
					if (vCurVal < vNegaVal) {
						vNegaVal = vCurVal;
					}
				}
			}

			// 如果绘图密度小于等于数据密度
			if (viewWidth <= dataLen) {
				if (vPlusVal > 0) {
					canvas.drawLine(i, 0, i, -vPlusVal, paint);
				}
				if (vNegaVal < 0) {
					canvas.drawLine(i, 0, i, -vNegaVal, paint);
				}
			} else {
				// 如果绘图密度大于数据密度
				if (vPlusVal > 0) {
					x2 = i;
					y2 = -vPlusVal;
					canvas.drawLine(x1, y1, x2, y2, paint);
					x1 = x2;
					y1 = y2;
				}

				if (vNegaVal < 0) {
					x2 = i;
					y2 = -vNegaVal;
					canvas.drawLine(x1, y1, x2, y2, paint);
					x1 = x2;
					y1 = y2;

				}
			}
		}
	}
}
