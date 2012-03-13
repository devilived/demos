package com.devil.soundrec;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ZoomControls;
public class SoundRecordActivity extends Activity {
    /** Called when the activity is first created. */
	private Button btnStart,btnExit;
	private SurfaceView sf;
    private ZoomControls zoomX,zoomY;
    
	private static final int FRENQENT = 8000;//������
	private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;//������
	private static final int ENCODELEN = AudioFormat.ENCODING_PCM_16BIT;//����16λ
	private static final int xMAX = 16;//X����С�������ֵ,X���������޴����ײ���ˢ����ʱ
	private static final int xMIM = 8;//X����С������Сֵ
	private static final int yMAX = 10;//Y����С�������ֵ
	private static final int yMIN = 1;//Y����С������Сֵ
	
	 private RecordComp recordComp;
	
	private Paint mPaint;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
		//����
		btnStart = (Button) this.findViewById(R.id.btnStart);
		btnExit = (Button) this.findViewById(R.id.btnExit);
		//����ͻ���
		sf = (SurfaceView) this.findViewById(R.id.SurfaceView01);
				
		btnStart.setOnClickListener(new ClickEvent());
		btnExit.setOnClickListener(new ClickEvent());
		sf.setOnTouchListener(new TouchEvent());
		
        mPaint = new Paint();  
        mPaint.setColor(Color.GREEN);// ����Ϊ��ɫ  
        mPaint.setStrokeWidth(1);// ���û��ʴ�ϸ 
        //ʾ�������
        recordComp=new RecordComp(FRENQENT,CHANNEL,ENCODELEN,sf,mPaint);
        recordComp.init(xMAX/2, yMAX/2, sf.getHeight()/2);
        
        //���ſؼ���X���������С�ı��ʸ�Щ
		zoomX = (ZoomControls)this.findViewById(R.id.zctlX);
		zoomX.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateX>xMIM){
					recordComp.rateX--;
				}
				setTitle("X����С"+recordComp.rateX+"��" +","+"Y����С"+recordComp.rateY+"��");
			}
		});
		zoomX.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateX<xMAX){
					recordComp.rateX++;	
				}
				setTitle("X����С"+recordComp.rateX+"��" +","+"Y����С"+recordComp.rateY+"��");
			}
		});
		zoomY = (ZoomControls)this.findViewById(R.id.zctlY);
		zoomY.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateY>yMIN){
					recordComp.rateY--;
				}
				setTitle("X����С"+recordComp.rateX+"��"	+","+"Y����С"+recordComp.rateY+"��");
			}
		});
		
		zoomY.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateY<yMAX){
					recordComp.rateY++;	
				}
				setTitle("X����С"+recordComp.rateX+"��"	+","+"Y����С"+recordComp.rateY+"��");
			}
		});
    }
	@Override
	protected void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	/**
	 * �����¼�����
	 * @author GV
	 *
	 */
	private class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (v == btnStart) {
				recordComp.baseLine=sf.getHeight()/2;
				recordComp.start();
			} else if (v == btnExit) {
				recordComp.Stop();
			}
		}
	}
	/**
	 * ��������̬���ò���ͼ����
	 * @author GV
	 *
	 */
	class TouchEvent implements OnTouchListener{
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			recordComp.baseLine=(int)event.getY();
			return true;
		}
		
	}
}