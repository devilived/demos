/**
 *  Description :
 */
package com.devil.turnpage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.graphics.Paint;
import android.graphics.Paint.Align;

public class PageManager {

	private File mBookFile = null;
	private MappedByteBuffer mFileBuf = null;
	private long mFileLen = 0;
	private int mPageStart = 0;
	private int mPageEnd = 0;
	private String mCharset = "GBK";
	// private Bitmap mPageBg = null;

	private List<String> mLineList = new ArrayList<String>();

	private int mLineCount; // 每页可以显示的行数
	// private float mVisibleHeight; // 绘制内容的宽
	// private float mVisibleWidth; // 绘制内容的宽
	// private boolean mIsFirstPage, mIsLastPage;

	private int mWidth;
	private int mHeight;
	private Paint mPaint;

	public PageManager(int w, int h, Paint paint) {
		mWidth = w;
		mHeight = h;
		mPaint = paint;
		mLineCount = (int) (mHeight / mPaint.getTextSize());
	}

	public void openbook(String strFilePath) throws IOException {
		mBookFile = new File(strFilePath);
		mFileLen = mBookFile.length();
		mFileBuf = new RandomAccessFile(mBookFile, "r").getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, mFileLen);
		// mFileBuf.flip();
	}

	private byte[] readParagraphBack(int end) {
		int i;
		byte b0, b1;
		if (mCharset.equals("UTF-16LE")) {
			i = end - 2;
			while (i > 0) {
				b0 = mFileBuf.get(i);
				b1 = mFileBuf.get(i + 1);
				if (b0 == 0x0a && b1 == 0x00 && i != end - 2) {
					i += 2;
					break;
				}
				// i--;
				i -= 2;
			}

		} else if (mCharset.equals("UTF-16BE")) {
			i = end - 2;
			while (i > 0) {
				b0 = mFileBuf.get(i);
				b1 = mFileBuf.get(i + 1);
				if (b0 == 0x00 && b1 == 0x0a && i != end - 2) {
					i += 2;
					break;
				}
				// i--;
				i -= 2;
			}
		} else {
			i = end - 1;
			while (i > 0) {
				b0 = mFileBuf.get(i);
				if (b0 == 0x0a && i != end - 1) {
					i++;
					break;
				}
				i--;
			}
		}
		if (i < 0) {
			i = 0;
		}
		int size = end - i;
		byte[] buf = new byte[size];
		for (int j = 0; j < size; j++) {
			buf[j] = mFileBuf.get(i + j);
		}
		return buf;
	}

	// 读取上一段落
	private byte[] readParagraphForward(int start) {
		int i = start;
		byte b0, b1;
		// 根据编码格式判断换行
		if (mCharset.equals("UTF-16LE")) {
			while (i < mFileLen - 1) {
				b0 = mFileBuf.get(i++);
				b1 = mFileBuf.get(i++);
				if (b0 == 0x0a && b1 == 0x00) {
					break;
				}
			}
		} else if (mCharset.equals("UTF-16BE")) {
			while (i < mFileLen - 1) {
				b0 = mFileBuf.get(i++);
				b1 = mFileBuf.get(i++);
				if (b0 == 0x00 && b1 == 0x0a) {
					break;
				}
			}
		} else {
			while (i < mFileLen) {
				b0 = mFileBuf.get(i++);
				if (b0 == 0x0a) {
					break;
				}
			}
		}
		int size = i - start;
		byte[] buf = new byte[size];
		for (int j = 0; j < size; j++) {
			buf[j] = mFileBuf.get(start + j);
		}
		return buf;
	}

	private List<String> pageDown() {
		String paragraph = "";
		List<String> lines = new ArrayList<String>();
		try {
			while (lines.size() < mLineCount && mPageEnd < mFileLen) {
				byte[] paraBuf = readParagraphForward(mPageEnd); // 读取一个段落
				mPageEnd += paraBuf.length;

				paragraph = new String(paraBuf, mCharset);
				String lineSeperator = "";
				if (paragraph.indexOf("\r\n") != -1) {
					lineSeperator = "\r\n";
					paragraph = paragraph.replaceAll("\r\n", "");
				} else if (paragraph.indexOf("\n") != -1) {
					lineSeperator = "\n";
					paragraph = paragraph.replaceAll("\n", "");
				}

				if (paragraph.length() == 0) {
					lines.add(paragraph);
				}
				while (paragraph.length() > 0) {
					int nSize = mPaint.breakText(paragraph, true, mWidth, null);
					lines.add(paragraph.substring(0, nSize));
					paragraph = paragraph.substring(nSize);
					if (lines.size() >= mLineCount) {
						break;
					}
				}
				if (paragraph.length() != 0) {
					mPageEnd -= (paragraph + lineSeperator).getBytes(mCharset).length;

				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return lines;
	}

	private void pageUp() {
		if (mPageStart < 0) {
			mPageStart = 0;
		}
		List<String> lines = new ArrayList<String>();
		String strParagraph = "";
		try {
			while (lines.size() < mLineCount && mPageStart > 0) {
				List<String> paraLines = new ArrayList<String>();
				byte[] paraBuf = readParagraphBack(mPageStart);
				mPageStart -= paraBuf.length;

				strParagraph = new String(paraBuf, mCharset);

				strParagraph = strParagraph.replaceAll("\r\n", "");
				strParagraph = strParagraph.replaceAll("\n", "");

				if (strParagraph.length() == 0) {
					paraLines.add(strParagraph);
				}
				while (strParagraph.length() > 0) {
					int nSize = mPaint.breakText(strParagraph, true, mWidth,
							null);
					paraLines.add(strParagraph.substring(0, nSize));
					strParagraph = strParagraph.substring(nSize);
				}
				lines.addAll(0, paraLines);
			}
			while (lines.size() > mLineCount) {
				mPageStart += lines.get(0).getBytes(mCharset).length;
				lines.remove(0);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		mPageEnd = mPageStart;
		return;
	}

	public void prePage() {
		if (mPageStart <= 0) {
			mPageStart = 0;
			return;
		} else {
		}
		mLineList.clear();
		pageUp();
		mLineList = pageDown();
	}

	public void nextPage() {
		if (mPageEnd >= mFileLen) {
			return;
		}

		mLineList.clear();
		mPageStart = mPageEnd;
		mLineList = pageDown();
	}

	public List<String> getCurrentLines() {
		return mLineList;
	}

	public boolean isfirstPage() {
		return mPageStart < 1;
	}

	public boolean islastPage() {
		return mPageEnd >= mFileLen;
	}

	public float getPercent() {
		return (float) (mPageStart * 1.0 / mFileLen);
	}
}
