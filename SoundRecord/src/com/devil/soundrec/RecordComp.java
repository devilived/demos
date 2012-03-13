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
	 * X����С�ı���
	 */
	public int rateX = 4;
	/**
	 * Y����С�ı���
	 */
	public int rateY = 4;
	/**
	 * Y�����
	 */
	public int baseLine = 0;
	
	private ArrayList<short[]> data = new ArrayList<short[]>();
	private boolean isRecording = false;// �߳̿��Ʊ��
	private int recBufSize;
	private AudioRecord record;
	
	private SurfaceView surfaceView;
	private Paint paint;
	
	public RecordComp(int frequent, int channel, int encodeLen, SurfaceView sfv,Paint paint){
		this.recBufSize = AudioRecord.getMinBufferSize(frequent,channel, encodeLen);
		
		 //¼�����,8000khz,mono,16bit
		this.record = new AudioRecord(MediaRecorder.AudioSource.MIC, frequent,
				channel, encodeLen, recBufSize);
		
		this.surfaceView = sfv;
		this.paint = paint;
	}
	/**
	 * ��ʼ��
	 */
	public void init(int rateX, int rateY, int baseLine) {
		this.rateX = rateX;
		this.rateY = rateY;
		this.baseLine = baseLine;
	}
	
	/**
	 * ��ʼ
	 * 
	 * @param recBufSize
	 *            AudioRecord��MinBufferSize
	 */
	public void start() {
		isRecording = true;
		new RecordThread().start();// ��ʼ¼���߳�
		new DrawThread().start();// ��ʼ�����߳�
	}
	/**
	 * ֹͣ
	 */
	public void Stop() {
		isRecording = false;
		data.clear();// ���
	}
	/**
	 * �����MIC�������ݵ�inBuf
	 * 
	 * @author GV
	 * 
	 */
	private class RecordThread extends Thread {
		public void run() {
			try {
				short[] buffer = new short[recBufSize];
				record.startRecording();// ��ʼ¼��
				while (isRecording) {
					// ��MIC�������ݵ�������
					int dateLen = record.read(buffer, 0, recBufSize);
					short[] tmpBuf = new short[dateLen / rateX];
					for (int i = 0, j = 0,n=tmpBuf.length; i < n; i++, j = i* rateX) {
						tmpBuf[i] = buffer[j];
					}
					synchronized (data) {//
						data.add(tmpBuf);// �������
					}
				}
				record.stop();
			} catch (Throwable t) {
			}
		}
	};
	/**
	 * �������inBuf�е�����
	 * 
	 * @author GV
	 * 
	 */
	private class DrawThread extends Thread {
		private int oldX = 0;// �ϴλ��Ƶ�X����
		private int oldY = 0;// �ϴλ��Ƶ�Y����
		private int x_start = 0;// ��ǰ��ͼ������ĻX�������
		
		public void run() {
			while (isRecording) {
				ArrayList<short[]> buf;
				synchronized (data) {
					if (data.size() == 0){
						continue;
					}
					buf = (ArrayList<short[]>) data.clone();// ����
					data.clear();// ���
				}
				for (int i = 0, n = buf.size(); i < n; i++) {
					short[] tmpBuf = buf.get(i);
					SimpleDraw(x_start, tmpBuf, rateY, baseLine);// �ѻ��������ݻ�����
					x_start = x_start + tmpBuf.length;
					if (x_start > surfaceView.getWidth()) {
						x_start = 0;
					}
				}
			}
		}
		/**
		 * ����ָ������
		 * 
		 * @param start
		 *            X�Ὺʼ��λ��(ȫ��)
		 * @param buffer
		 *            ������
		 * @param rate
		 *            Y��������С�ı���
		 * @param baseLine
		 *            Y�����
		 */
		private void SimpleDraw(int start, short[] buffer, int rate, int baseLine) {
			if (start == 0){
				oldX = 0;
			}
			Canvas canvas = surfaceView.getHolder().lockCanvas(
					new Rect(start, 0, start + buffer.length, surfaceView.getHeight()));// �ؼ�:��ȡ����
			canvas.drawColor(Color.BLACK);// �������
			int y;
			for (int i = 0; i < buffer.length; i++) {// �ж��ٻ�����
				int x = start + i;
				y = buffer[i] / rate + baseLine;// ������С���������ڻ�׼��
				canvas.drawLine(oldX, oldY, x, y, paint);
				oldX = x;
				oldY = y;
			}
			surfaceView.getHolder().unlockCanvasAndPost(canvas);// �����������ύ���õ�ͼ��
		}
	}
}
