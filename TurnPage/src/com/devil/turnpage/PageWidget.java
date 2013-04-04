package com.devil.turnpage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class PageWidget extends View {
	public static enum Corner {
		LT, RT, LB, RB
	};

	private static final String TAG = PageWidget.class.getSimpleName();
	private Corner mCorner;
	private boolean mIsRTandLB; // 是否属于右上左下
	private boolean mReset = false;
	private boolean mDefaultTouchListener =true;
	
	private int mWidth;// = 480;
	private int mHeight;// = 800;
	private float mMaxLength;// = (float) Math.hypot(mWidth, mHeight);
	private int mCornerX = 0; // 拖拽点对应的页脚
	private int mCornerY = 0;

	private Path mPath0;
	private Path mPath1;
	private Bitmap mCurPageBitmap = null; // 当前页
	private Bitmap mNextPageBitmap = null;

	private PointF mDrapPnt = new PointF(); // 拖拽点
	private PointF mBezierStart1 = new PointF(); // 贝塞尔曲线起始点
	private PointF mBezierControl1 = new PointF(); // 贝塞尔曲线控制点
	private PointF mBeziervertex1 = new PointF(); // 贝塞尔曲线顶点
	private PointF mBezierEnd1 = new PointF(); // 贝塞尔曲线结束点

	private PointF mBezierStart2 = new PointF(); // 另一条贝塞尔曲线
	private PointF mBezierControl2 = new PointF();
	private PointF mBeziervertex2 = new PointF();
	private PointF mBezierEnd2 = new PointF();

	private float mMiddleX;
	private float mMiddleY;
	private float mDegrees;
	private float mTouchToCornerDis;
	private ColorMatrixColorFilter mColorMatrixFilter;
	private Matrix mMatrix;
	private float[] mMatrixArray = { 0, 0, 0, 0, 0, 0, 0, 0, 1.0f };

	private int[] mBackShadowColors;
	private int[] mFrontShadowColors;
	private GradientDrawable mBackShadowDrawableLR;
	private GradientDrawable mBackShadowDrawableRL;
	private GradientDrawable mFolderShadowDrawableLR;
	private GradientDrawable mFolderShadowDrawableRL;

	private GradientDrawable mFrontShadowDrawableHBT;
	private GradientDrawable mFrontShadowDrawableHTB;
	private GradientDrawable mFrontShadowDrawableVLR;
	private GradientDrawable mFrontShadowDrawableVRL;

	private Paint mPaint;

	private Scroller mScroller;

	public PageWidget(Context context) {
		super(context);
		mPath0 = new Path();
		mPath1 = new Path();
		createDrawable();

		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);

		ColorMatrix cm = new ColorMatrix();
		float array[] = { 0.55f, 0, 0, 0, 80.0f, 0, 0.55f, 0, 0, 80.0f, 0, 0,
				0.55f, 0, 80.0f, 0, 0, 0, 0.2f, 0 };
		cm.set(array);
		mColorMatrixFilter = new ColorMatrixColorFilter(cm);
		mMatrix = new Matrix();
		mScroller = new Scroller(getContext());

		mDrapPnt.x = 0.01f; // 不让x,y为0,否则在点计算时会有问题
		mDrapPnt.y = 0.01f;
	}

	public int[] getCornerXY() {
		return new int[] { mCornerX, mCornerY };
	}

	public void setCorner(Corner corner) {
		mCorner = corner;
		mIsRTandLB = (mCorner == Corner.RT || mCorner == Corner.LB);

		switch (mCorner) {
		case LT:
			mCornerX = 0;
			mCornerY = 0;
			break;
		case RT:
			mCornerX = mWidth;
			mCornerY = 0;
			break;
		case LB:
			mCornerX = 0;
			mCornerY = mHeight;
			break;
		case RB:
			mCornerX = mWidth;
			mCornerY = mHeight;
			break;
		}
		mDrapPnt.x = mCornerX;
		mDrapPnt.y = mCornerY;
	}

	public void turnTo(float x, float y) {
		if (mCorner == null) {
			throw new IllegalStateException("You haven't set the corner");
		}
		if (mCurPageBitmap == null || mNextPageBitmap == null) {
			throw new IllegalStateException(
					"You haven't set the current and the next bitmap");
		}
		mDrapPnt.x = x;
		mDrapPnt.y = y;
		postInvalidate();
	}

	public void reset() {
		mCorner = null;
		mDrapPnt.x = 0;
		mDrapPnt.y = 0;
		mReset = true;
		if (mCurPageBitmap == null) {
			throw new IllegalStateException(
					"You haven't set the current and the next bitmap");
		}
		postInvalidate();
	}

	public void scrollTheRest(int delayMillis) {
		if (mCorner == null) {
			throw new IllegalStateException(
					"you might has not been set the corner");
		}
		if (mCurPageBitmap == null || mNextPageBitmap == null) {
			throw new IllegalStateException(
					"You haven't set the current and the next bitmap");
		}

		int dx, dy;
		// dx 水平方向滑动的距离，负值会使滚动向左滚动
		// dy 垂直方向滑动的距离，负值会使滚动向上滚动
		if (mCornerX > 0) {
			dx = -(int) (mWidth + mDrapPnt.x);
		} else {
			dx = (int) (mWidth - mDrapPnt.x + mWidth);
		}
		if (mCornerY > 0) {
			dy = (int) (mHeight - mDrapPnt.y);
		} else {
			dy = (int) (1 - mDrapPnt.y); // 防止mTouch.y最终变为0
		}
		mScroller.startScroll((int) mDrapPnt.x, (int) mDrapPnt.y, dx, dy,
				delayMillis);
		postInvalidate();
	}

	public void setBitmaps(Bitmap bm1, Bitmap bm2) {
		mCurPageBitmap = bm1;
		mNextPageBitmap = bm2;
	}

	public void abortAnimation() {
		Log.d("test", "abort animation");
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}
		mCorner = null;
	}

	public void setDefaultTouchEvent(boolean defaultTouchEvent) {
		mDefaultTouchListener = defaultTouchEvent;
	}

	/*******************************************************/
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!mDefaultTouchListener){
			return super.onTouchEvent(event);
		}
		float x = event.getX();
		float y = event.getY();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			abortAnimation();
			boolean left = (x <= mWidth >> 2);
			boolean top = (y <= mHeight >> 2);
			Corner corner = Corner.LT;
			if (left && top) {
				corner = Corner.LT;
			} else if (!left && top) {
				corner = Corner.RT;
			} else if (left && !top) {
				corner = Corner.LB;
			} else if (!left && !top) {
				corner = Corner.RB;
			}
			setCorner(corner);

			turnTo(event.getX(), y);
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			turnTo(x, y);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			int[] corner = getCornerXY();
			boolean canDragOver = Math.abs(corner[0] - x) > mWidth / 4;
			if (canDragOver) {
				Log.d("test", "drag over");
				scrollTheRest(1200);
			} else {
				Log.d("test", "not drag over");
				reset();
			}
		}
		return true;
	}

	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			float x = mScroller.getCurrX();
			float y = mScroller.getCurrY();
			mDrapPnt.x = x;
			mDrapPnt.y = y;
			postInvalidate();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		this.mWidth = this.getMeasuredWidth();
		this.mHeight = this.getMeasuredHeight();
		this.mMaxLength = (float) Math.hypot(mWidth, mHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(0xFFAAAAAA);
		if (mReset) {
			mReset = false;
			canvas.drawBitmap(mCurPageBitmap, 0, 0, null);
			return;
		}
		calcPoints();
		drawCurrentPageArea(canvas, mCurPageBitmap, mPath0);
		drawNextPageAreaAndShadow(canvas, mNextPageBitmap);
		drawCurrentPageShadow(canvas);
		drawCurrentBackArea(canvas, mCurPageBitmap);
	}

	/***************************************************************************/
	private void calcPoints() {
		mMiddleX = (mDrapPnt.x + mCornerX) / 2;
		mMiddleY = (mDrapPnt.y + mCornerY) / 2;
		mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
				* (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
		mBezierControl1.y = mCornerY;
		mBezierControl2.x = mCornerX;
		mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
				* (mCornerX - mMiddleX) / (mCornerY - mMiddleY);

		// Log.i("hmg", "mTouchX  " + mTouch.x + "  mTouchY  " + mTouch.y);
		// Log.i("hmg", "mBezierControl1.x  " + mBezierControl1.x
		// + "  mBezierControl1.y  " + mBezierControl1.y);
		// Log.i("hmg", "mBezierControl2.x  " + mBezierControl2.x
		// + "  mBezierControl2.y  " + mBezierControl2.y);

		mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x)
				/ 2;
		mBezierStart1.y = mCornerY;

		// 当mBezierStart1.x < 0或者mBezierStart1.x > 480时
		// 如果继续翻页，会出现BUG故在此限制
		if (mDrapPnt.x > 0 && mDrapPnt.x < mWidth) {
			if (mBezierStart1.x < 0 || mBezierStart1.x > mWidth) {
				if (mBezierStart1.x < 0)
					mBezierStart1.x = mWidth - mBezierStart1.x;

				float f1 = Math.abs(mCornerX - mDrapPnt.x);
				float f2 = mWidth * f1 / mBezierStart1.x;
				mDrapPnt.x = Math.abs(mCornerX - f2);

				float f3 = Math.abs(mCornerX - mDrapPnt.x)
						* Math.abs(mCornerY - mDrapPnt.y) / f1;
				mDrapPnt.y = Math.abs(mCornerY - f3);

				mMiddleX = (mDrapPnt.x + mCornerX) / 2;
				mMiddleY = (mDrapPnt.y + mCornerY) / 2;

				mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
						* (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
				mBezierControl1.y = mCornerY;

				mBezierControl2.x = mCornerX;
				mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
						* (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
				// Log.i("hmg", "mTouchX --> " + mTouch.x + "  mTouchY-->  "
				// + mTouch.y);
				// Log.i("hmg", "mBezierControl1.x--  " + mBezierControl1.x
				// + "  mBezierControl1.y -- " + mBezierControl1.y);
				// Log.i("hmg", "mBezierControl2.x -- " + mBezierControl2.x
				// + "  mBezierControl2.y -- " + mBezierControl2.y);
				mBezierStart1.x = mBezierControl1.x
						- (mCornerX - mBezierControl1.x) / 2;
			}
		}
		mBezierStart2.x = mCornerX;
		mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y)
				/ 2;

		mTouchToCornerDis = (float) Math.hypot((mDrapPnt.x - mCornerX),
				(mDrapPnt.y - mCornerY));

		mBezierEnd1 = GeometryUtil.getCross(mDrapPnt, mBezierControl1,
				mBezierStart1, mBezierStart2);
		mBezierEnd2 = GeometryUtil.getCross(mDrapPnt, mBezierControl2,
				mBezierStart1, mBezierStart2);

		// Log.i("hmg", "mBezierEnd1.x  " + mBezierEnd1.x + "  mBezierEnd1.y  "
		// + mBezierEnd1.y);
		// Log.i("hmg", "mBezierEnd2.x  " + mBezierEnd2.x + "  mBezierEnd2.y  "
		// + mBezierEnd2.y);

		/*
		 * mBeziervertex1.x 推导
		 * ((mBezierStart1.x+mBezierEnd1.x)/2+mBezierControl1.x)/2 化简等价于
		 * (mBezierStart1.x+ 2*mBezierControl1.x+mBezierEnd1.x) / 4
		 */
		mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
		mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
		mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
		mBeziervertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
	}

	private void drawCurrentPageArea(Canvas canvas, Bitmap bitmap, Path path) {
		mPath0.reset();
		mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x,
				mBezierEnd1.y);
		mPath0.lineTo(mDrapPnt.x, mDrapPnt.y);
		mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x,
				mBezierStart2.y);
		mPath0.lineTo(mCornerX, mCornerY);
		mPath0.close();

		canvas.save();
		canvas.clipPath(path, Region.Op.XOR);
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.restore();
	}

	private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {
		mPath1.reset();
		mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.lineTo(mCornerX, mCornerY);
		mPath1.close();

		mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
				- mCornerX, mBezierControl2.y - mCornerY));
		int leftx;
		int rightx;
		GradientDrawable mBackShadowDrawable;
		if (mIsRTandLB) {
			leftx = (int) (mBezierStart1.x);
			rightx = (int) (mBezierStart1.x + mTouchToCornerDis / 4);
			mBackShadowDrawable = mBackShadowDrawableLR;
		} else {
			leftx = (int) (mBezierStart1.x - mTouchToCornerDis / 4);
			rightx = (int) mBezierStart1.x;
			mBackShadowDrawable = mBackShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		mBackShadowDrawable.setBounds(leftx, (int) mBezierStart1.y, rightx,
				(int) (mMaxLength + mBezierStart1.y));
		mBackShadowDrawable.draw(canvas);
		canvas.restore();
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 创建阴影的GradientDrawable
	 */
	private void createDrawable() {
		int[] color = { 0x333333, 0xb0333333 };
		mFolderShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, color);
		mFolderShadowDrawableRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFolderShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, color);
		mFolderShadowDrawableLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowColors = new int[] { 0xff111111, 0x111111 };
		mBackShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
		mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
		mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowColors = new int[] { 0x80111111, 0x111111 };
		mFrontShadowDrawableVLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
		mFrontShadowDrawableVLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mFrontShadowDrawableVRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
		mFrontShadowDrawableVRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHTB = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
		mFrontShadowDrawableHTB
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHBT = new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
		mFrontShadowDrawableHBT
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 绘制翻起页的阴影
	 */
	private void drawCurrentPageShadow(Canvas canvas) {
		double degree;
		if (mIsRTandLB) {
			degree = Math.PI
					/ 4
					- Math.atan2(mBezierControl1.y - mDrapPnt.y, mDrapPnt.x
							- mBezierControl1.x);
		} else {
			degree = Math.PI
					/ 4
					- Math.atan2(mDrapPnt.y - mBezierControl1.y, mDrapPnt.x
							- mBezierControl1.x);
		}
		// 翻起页阴影顶点与touch点的距离
		double d1 = (float) 25 * 1.414 * Math.cos(degree);
		double d2 = (float) 25 * 1.414 * Math.sin(degree);
		float x = (float) (mDrapPnt.x + d1);
		float y;
		if (mIsRTandLB) {
			y = (float) (mDrapPnt.y + d2);
		} else {
			y = (float) (mDrapPnt.y - d2);
		}
		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mDrapPnt.x, mDrapPnt.y);
		mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
		mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.close();
		canvas.save();

		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		int leftx;
		int rightx;
		GradientDrawable mCurrentPageShadow;
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl1.x);
			rightx = (int) mBezierControl1.x + 25;
			mCurrentPageShadow = mFrontShadowDrawableVLR;
		} else {
			leftx = (int) (mBezierControl1.x - 25);
			rightx = (int) mBezierControl1.x + 1;
			mCurrentPageShadow = mFrontShadowDrawableVRL;
		}

		float rotateDegrees = (float) Math.toDegrees(Math.atan2(mDrapPnt.x
				- mBezierControl1.x, mBezierControl1.y - mDrapPnt.y));
		canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
		mCurrentPageShadow.setBounds(leftx,
				(int) (mBezierControl1.y - mMaxLength), rightx,
				(int) (mBezierControl1.y));
		mCurrentPageShadow.draw(canvas);
		canvas.restore();

		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mDrapPnt.x, mDrapPnt.y);
		mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.close();
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl2.y);
			rightx = (int) (mBezierControl2.y + 25);
			mCurrentPageShadow = mFrontShadowDrawableHTB;
		} else {
			leftx = (int) (mBezierControl2.y - 25);
			rightx = (int) (mBezierControl2.y + 1);
			mCurrentPageShadow = mFrontShadowDrawableHBT;
		}
		rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
				- mDrapPnt.y, mBezierControl2.x - mDrapPnt.x));
		canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
		float temp;
		if (mBezierControl2.y < 0)
			temp = mBezierControl2.y - mHeight;
		else
			temp = mBezierControl2.y;

		int hmg = (int) Math.hypot(mBezierControl2.x, temp);
		if (hmg > mMaxLength)
			mCurrentPageShadow
					.setBounds((int) (mBezierControl2.x - 25) - hmg, leftx,
							(int) (mBezierControl2.x + mMaxLength) - hmg,
							rightx);
		else
			mCurrentPageShadow.setBounds(
					(int) (mBezierControl2.x - mMaxLength), leftx,
					(int) (mBezierControl2.x), rightx);

		// Log.i("hmg", "mBezierControl2.x   " + mBezierControl2.x
		// + "  mBezierControl2.y  " + mBezierControl2.y);
		mCurrentPageShadow.draw(canvas);
		canvas.restore();
	}

	/**
	 * Author : hmg25 Version: 1.0 Description : 绘制翻起页背面
	 */
	private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
		int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
		float f1 = Math.abs(i - mBezierControl1.x);
		int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
		float f2 = Math.abs(i1 - mBezierControl2.y);
		float f3 = Math.min(f1, f2);
		mPath1.reset();
		mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
		mPath1.lineTo(mDrapPnt.x, mDrapPnt.y);
		mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath1.close();
		GradientDrawable mFolderShadowDrawable;
		int left;
		int right;
		if (mIsRTandLB) {
			left = (int) (mBezierStart1.x - 1);
			right = (int) (mBezierStart1.x + f3 + 1);
			mFolderShadowDrawable = mFolderShadowDrawableLR;
		} else {
			left = (int) (mBezierStart1.x - f3 - 1);
			right = (int) (mBezierStart1.x + 1);
			mFolderShadowDrawable = mFolderShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);

		mPaint.setColorFilter(mColorMatrixFilter);

		float dis = (float) Math.hypot(mCornerX - mBezierControl1.x,
				mBezierControl2.y - mCornerY);
		float f8 = (mCornerX - mBezierControl1.x) / dis;
		float f9 = (mBezierControl2.y - mCornerY) / dis;
		mMatrixArray[0] = 1 - 2 * f9 * f9;
		mMatrixArray[1] = 2 * f8 * f9;
		mMatrixArray[3] = mMatrixArray[1];
		mMatrixArray[4] = 1 - 2 * f8 * f8;
		mMatrix.reset();
		mMatrix.setValues(mMatrixArray);
		mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
		mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
		canvas.drawBitmap(bitmap, mMatrix, mPaint);
		// canvas.drawBitmap(bitmap, mMatrix, null);
		mPaint.setColorFilter(null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		mFolderShadowDrawable.setBounds(left, (int) mBezierStart1.y, right,
				(int) (mBezierStart1.y + mMaxLength));
		mFolderShadowDrawable.draw(canvas);
		canvas.restore();
	}
}
