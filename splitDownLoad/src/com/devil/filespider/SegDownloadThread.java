package com.devil.filespider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import com.devil.utils.Util;

/**
 * 负责部分文件的抓取, 多线程
 * 
 * @author ETHAN
 * 
 */
public class SegDownloadThread extends Thread {
	private static final String HTTP_USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3";

	private String urlStr; // file url
	private long startPos;// file snippet start position
	private long endPos; // file snippet end position

	private boolean stoped = false; // stop identical相同

	private RandomAccessFile savedFile;
	private OnStopListener stopListener;

	public SegDownloadThread(String url, File fPath, long startPos, long endPos) {
		this(url, fPath, startPos, endPos, null, null);

	}

	public SegDownloadThread(String url, File fPath, long startPos, long endPos, String threadName, OnStopListener stopListener) {
		super(threadName);
		this.urlStr = url;
		this.startPos = startPos;
		this.endPos = endPos;
		this.stopListener = stopListener;
		try {
			this.savedFile = new RandomAccessFile(fPath, "rw");
			this.savedFile.seek(startPos);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void run() {
		HttpURLConnection conn = null;
		InputStream inputStream = null;
		try {
			while (!stoped && startPos < endPos) {
				System.out.println("-----------------------------------" + this.getName() + "---------开始写数据");
				URL url = new URL(urlStr);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", HTTP_USER_AGENT);
				conn.setRequestProperty("range", "bytes=" + startPos + "-");
				inputStream = conn.getInputStream();

				// 高速缓存，设置缓冲区
				byte[] buffer = new byte[1024];
				int readLen;

				while ((readLen = inputStream.read(buffer, 0, 1024)) > 0) {
					System.out.println("=================================" + this.getName());
					savedFile.write(buffer, 0, readLen);
					startPos += readLen;
				}

				Util.log("Thread " + this.getName() + " is over!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stopListener != null) {
				stopListener.onStop();
			}
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// 打印回应的头信息
	public void logResponseHead(HttpURLConnection conn) {
		for (int i = 1;; i++) {
			String headerKey = conn.getHeaderFieldKey(i);

			// key 不为空
			if (headerKey != null) {
				Util.log(headerKey + " : " + conn.getHeaderField(headerKey));
			} else {
				break;
			}
		}
	}

	public void stopThread() {
		stoped = true;
		if (stopListener != null) {
			stopListener.onStop();
		}
	}

	public long getCurrentPosition() {
		return startPos;
	}

	public long getTargetPosition() {
		return endPos;
	}

	public static interface OnStopListener {
		public void onStop();
	}

}
