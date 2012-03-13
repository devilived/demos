package com.devil.soundrec;
import java.util.ArrayList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.view.SurfaceView;
public class RecordComp {
	/**
	 * X轴缩小的比例
	 */
	public int rateX = 4;
	/**
	 * Y轴缩小的比例
	 */
	public int rateY = 4;
	/**
	 * Y轴基线
	 */
	public int baseLine = 0;
	
	private ArrayList<short[]> data = new ArrayList<short[]>();
	private boolean isRecording = false;// 线程控制标记
	private int recBufSize;
	private AudioRecord record;
	
	private SurfaceView surfaceView;
	private Paint paint;
	
	public RecordComp(int frequent, int channel, int encodeLen, SurfaceView sfv,Paint paint){
		this.recBufSize = AudioRecord.getMinBufferSize(frequent,channel, encodeLen);
		
		 //录音组件,8000khz,mono,16bit
		this.record = new AudioRecord(MediaRecorder.AudioSource.MIC, frequent,
				channel, encodeLen, recBufSize);
		
		this.surfaceView = sfv;
		this.paint = paint;
	}
	/**
	 * 初始化
	 */
	public void init(int rateX, int rateY, int baseLine) {
		this.rateX = rateX;
		this.rateY = rateY;
		this.baseLine = baseLine;
	}
	
	/**
	 * 开始
	 * 
	 * @param recBufSize
	 *            AudioRecord的MinBufferSize
	 */
	public void start() {
		isRecording = true;
		new RecordThread().start();// 开始录制线程
		new DrawThread().start();// 开始绘制线程
	}
	/**
	 * 停止
	 */
	public void Stop() {
		isRecording = false;
		data.clear();// 清除
	}
	/**
	 * 负责从MIC保存数据到inBuf
	 * 
	 * @author GV
	 * 
	 */
	private class RecordThread extends Thread {
		public void run() {
			try {
				short[] buffer = new short[recBufSize];
				record.startRecording();// 开始录制
				while (isRecording) {
					// 从MIC保存数据到缓冲区
					int dateLen = record.read(buffer, 0, recBufSize);
					short[] tmpBuf = new short[dateLen / rateX];
					for (int i = 0, j = 0,n=tmpBuf.length; i < n; i++, j = i* rateX) {
						tmpBuf[i] = buffer[j];
					}
					synchronized (data) {//
						data.add(tmpBuf);// 添加数据
					}
				}
				record.stop();
			} catch (Throwable t) {
			}
		}
	};
	/**
	 * 负责绘制inBuf中的数据
	 * 
	 * @author GV
	 * 
	 */
	private class DrawThread extends Thread {
		private int oldX = 0;// 上次绘制的X坐标
		private int oldY = 0;// 上次绘制的Y坐标
		private int x_start = 0;// 当前画图所在屏幕X轴的坐标
		
		public void run() {
			while (isRecording) {
				ArrayList<short[]> buf;
				synchronized (data) {
					if (data.size() == 0){
						continue;
					}
					buf = (ArrayList<short[]>) data.clone();// 保存
					data.clear();// 清除
				}
				for (int i = 0, n = buf.size(); i < n; i++) {
					short[] tmpBuf = buf.get(i);
					SimpleDraw(x_start, tmpBuf, rateY, baseLine);// 把缓冲区数据画出来
					x_start = x_start + tmpBuf.length;
					if (x_start > surfaceView.getWidth()) {
						x_start = 0;
					}
				}
			}
		}
		/**
		 * 绘制指定区域
		 * 
		 * @param start
		 *            X轴开始的位置(全屏)
		 * @param buffer
		 *            缓冲区
		 * @param rate
		 *            Y轴数据缩小的比例
		 * @param baseLine
		 *            Y轴基线
		 */
		private void SimpleDraw(int start, short[] buffer, int rate, int baseLine) {
			if (start == 0){
				oldX = 0;
			}
			Canvas canvas = surfaceView.getHolder().lockCanvas(
					new Rect(start, 0, start + buffer.length, surfaceView.getHeight()));// 关键:获取画布
			canvas.drawColor(Color.BLACK);// 清除背景
			int y;
			for (int i = 0; i < buffer.length; i++) {// 有多少画多少
				int x = start + i;
				y = buffer[i] / rate + baseLine;// 调节缩小比例，调节基准线
				canvas.drawLine(oldX, oldY, x, y, paint);
				oldX = x;
				oldY = y;
			}
			surfaceView.getHolder().unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像
		}
	}
}
