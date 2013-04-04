package com.devil.turnpage;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static int m_fontSize = 24;
	private static int m_textColor = Color.BLACK;
	private static int marginWidth = 15; // 左右与边缘的距离
	private static int marginHeight = 20; // 上下与边缘的距离
	private static int m_backColor = 0xffff9e85; // 背景颜色
	/** Called when the activity is first created. */
	private PageWidget mPageWidget;
	private Bitmap mCurPageBitmap, mNextPageBitmap;
	private Canvas mCurPageCanvas, mNextPageCanvas;
	// private BookPageFactory pagefactory;
	private PageManager mPageManager;
	private Bitmap mBgBmp;
	private static DecimalFormat DF = new DecimalFormat("#0.0");
	private Paint mPaint;
	private int mWidth;
	private int mHeight;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Display disp = getWindowManager().getDefaultDisplay();

		int SCREEN_W = disp.getWidth();
		int SCREEN_H = disp.getHeight();

		mCurPageBitmap = Bitmap.createBitmap(SCREEN_W, SCREEN_H,
				Bitmap.Config.ARGB_8888);
		mNextPageBitmap = Bitmap.createBitmap(SCREEN_W, SCREEN_H,
				Bitmap.Config.ARGB_8888);

		mCurPageCanvas = new Canvas(mCurPageBitmap);
		mNextPageCanvas = new Canvas(mNextPageBitmap);

		mBgBmp = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.bg);
		mBgBmp = Bitmap.createScaledBitmap(mBgBmp, SCREEN_W, SCREEN_H, true);

		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setTextAlign(Align.LEFT);
		mPaint.setTextSize(m_fontSize);
		mPaint.setColor(m_textColor);

		mWidth = SCREEN_W - marginWidth * 2;
		mHeight = SCREEN_H - marginHeight * 2;
		mPageManager = new PageManager(mWidth, mHeight, mPaint);

		try {
			mPageManager.openbook("/sdcard/test.txt");
			mPageManager.nextPage();
			fillBitMap(mCurPageCanvas, mPageManager);
		} catch (IOException e1) {
			e1.printStackTrace();
			Toast.makeText(this, "电子书不存在,请将《test.txt》放在SD卡根目录下",
					Toast.LENGTH_SHORT).show();
		}

		mPageWidget = new PageWidget(this) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					boolean left = (event.getX() <= mWidth >> 2);

					fillBitMap(mCurPageCanvas, mPageManager);

					if (left) {
						if (mPageManager.isfirstPage()) {
							return false;
						}
						mPageManager.prePage();
					} else {
						if (mPageManager.islastPage()) {
							return false;
						}
						mPageManager.nextPage();
					}
					fillBitMap(mNextPageCanvas, mPageManager);
					setBitmaps(mCurPageBitmap, mNextPageBitmap);
				}
				return super.onTouchEvent(event);
			}
		};
		setContentView(mPageWidget);
		mPageWidget.setDefaultTouchEvent(true);
		mPageWidget.setBitmaps(mCurPageBitmap, mNextPageBitmap);
		mPageWidget.reset();
	}

	private void fillBitMap(Canvas c, PageManager pageManager) {
		// if (m_lines.size() == 0)
		// m_lines = pageDown();
		List<String> lines = pageManager.getCurrentLines();
		if (lines.size() > 0) {
			if (mBgBmp == null) {
				c.drawColor(m_backColor);
			} else {
				c.drawBitmap(mBgBmp, 0, 0, null);
			}
			int y = marginHeight;
			for (String strLine : lines) {
				y += m_fontSize;
				c.drawText(strLine, marginWidth, y, mPaint);
			}
		}
		String strPercent = DF.format(pageManager.getPercent() * 100) + "%";
		int nPercentWidth = (int) mPaint.measureText("999.9%") + 1;
		c.drawText(strPercent, mWidth - nPercentWidth, mHeight - 5, mPaint);
	}
}