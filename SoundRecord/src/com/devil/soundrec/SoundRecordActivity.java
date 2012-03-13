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
    
	private static final int FRENQENT = 8000;//采样率
	private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;//单声道
	private static final int ENCODELEN = AudioFormat.ENCODING_PCM_16BIT;//采样16位
	private static final int xMAX = 16;//X轴缩小比例最大值,X轴数据量巨大，容易产生刷新延时
	private static final int xMIM = 8;//X轴缩小比例最小值
	private static final int yMAX = 10;//Y轴缩小比例最大值
	private static final int yMIN = 1;//Y轴缩小比例最小值
	
	 private RecordComp recordComp;
	
	private Paint mPaint;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
		//按键
		btnStart = (Button) this.findViewById(R.id.btnStart);
		btnExit = (Button) this.findViewById(R.id.btnExit);
		//画板和画笔
		sf = (SurfaceView) this.findViewById(R.id.SurfaceView01);
				
		btnStart.setOnClickListener(new ClickEvent());
		btnExit.setOnClickListener(new ClickEvent());
		sf.setOnTouchListener(new TouchEvent());
		
        mPaint = new Paint();  
        mPaint.setColor(Color.GREEN);// 画笔为绿色  
        mPaint.setStrokeWidth(1);// 设置画笔粗细 
        //示波器类库
        recordComp=new RecordComp(FRENQENT,CHANNEL,ENCODELEN,sf,mPaint);
        recordComp.init(xMAX/2, yMAX/2, sf.getHeight()/2);
        
        //缩放控件，X轴的数据缩小的比率高些
		zoomX = (ZoomControls)this.findViewById(R.id.zctlX);
		zoomX.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateX>xMIM){
					recordComp.rateX--;
				}
				setTitle("X轴缩小"+recordComp.rateX+"倍" +","+"Y轴缩小"+recordComp.rateY+"倍");
			}
		});
		zoomX.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateX<xMAX){
					recordComp.rateX++;	
				}
				setTitle("X轴缩小"+recordComp.rateX+"倍" +","+"Y轴缩小"+recordComp.rateY+"倍");
			}
		});
		zoomY = (ZoomControls)this.findViewById(R.id.zctlY);
		zoomY.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateY>yMIN){
					recordComp.rateY--;
				}
				setTitle("X轴缩小"+recordComp.rateX+"倍"	+","+"Y轴缩小"+recordComp.rateY+"倍");
			}
		});
		
		zoomY.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(recordComp.rateY<yMAX){
					recordComp.rateY++;	
				}
				setTitle("X轴缩小"+recordComp.rateX+"倍"	+","+"Y轴缩小"+recordComp.rateY+"倍");
			}
		});
    }
	@Override
	protected void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	/**
	 * 按键事件处理
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
	 * 触摸屏动态设置波形图基线
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