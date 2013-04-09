package com.devil.filespider;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.devil.filespider.SegDownloadThread.OnStopListener;
import com.devil.model.SiteInfo;

public class FileDownloader {
	// 文件信息Bean
	private SiteInfo siteInfo;
	// 子线程对象
	private SegDownloadThread[] segDownloadThreadArr;

	// 文件下载的临时信息，保存下载信息
	private File metaFile;
	// 输出到文件的输出流
	private DataOutputStream output;

	private int threadNum = 1;

	// 下载完成的线程数量
	private int stoppedThread = 0;

	public FileDownloader(SiteInfo site, int threadNum) {
		this.siteInfo = site;
		if (threadNum > 1) {
			this.threadNum = threadNum;
		}

		try {
	        init();
        } catch (IOException e) {
	        e.printStackTrace();
        }
	}
	public void download() throws IOException, InterruptedException {
		// 启动子线程
		output = new DataOutputStream(new FileOutputStream(metaFile));
		System.out.println("---------------------------------");
		for (SegDownloadThread thread : segDownloadThreadArr) {
			thread.start();
		}

		while (true) {
			if (stoppedThread == segDownloadThreadArr.length) {
				writeMeta();
				Thread.sleep(500);
				break;
			}
		}
	}
	
	
	
	private void init() throws IOException {
		this.metaFile = new File(siteInfo.savedPath, siteInfo.savedFileName + ".meta");

		OnStopListener listener = new OnStopListener() {
			@Override
			public void onStop() {
				stoppedThread++;
			}
		};
		File file = new File(siteInfo.savedPath, siteInfo.urlStr);
		System.out.println(metaFile.getAbsolutePath());
		if (metaFile.exists()) {
			// 读取下载信息
			List<long[]> metaList = readMeta();
			segDownloadThreadArr = new SegDownloadThread[metaList.size()];

			for (int i = 0; i < metaList.size(); i++) {
				long[] meta = metaList.get(i);
				if (meta[0] != meta[1]) {
					continue;
				}
				segDownloadThreadArr[i] = new SegDownloadThread(siteInfo.urlStr, file, meta[0], meta[1], "" + i, listener);
			}
		} else {
			long fileLength = getFileSize();
			// block开头
			segDownloadThreadArr = new SegDownloadThread[threadNum];
			long blockSize = fileLength / threadNum;
			for (int i = 0; i < threadNum - 1; i++) {
				long startPos = i * blockSize;
				long endPos = startPos + blockSize;
				segDownloadThreadArr[i] = new SegDownloadThread(siteInfo.urlStr, file, startPos, endPos, "" + i, listener);
			}
			int lastBlocIdx = threadNum - 1;
			long startPos = lastBlocIdx * blockSize;
			segDownloadThreadArr[lastBlocIdx] = new SegDownloadThread(siteInfo.urlStr, file, startPos, fileLength, "" + lastBlocIdx,
			        listener);
		}
	}


	private List<long[]> readMeta() throws IOException {
		DataInputStream input = null;
		List<long[]> metaList = null;
		try {
			input = new DataInputStream(new FileInputStream(metaFile));
			int count = input.readInt();
			metaList = new ArrayList<long[]>(count);
			for (int i = 0; i < count; i++) {
				long[] arr = new long[] { input.readLong(), input.readLong() };
				metaList.add(arr);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}

		return metaList;
	}

	// 保存下载信息（文件指针位置)
	private void writeMeta() {
		// 全部结束下载完毕

		// TODO Auto-generated method stub
		try {

			this.output = new DataOutputStream(new FileOutputStream(metaFile));
			// 下载线程数量
			this.output.writeInt(threadNum);
			for (SegDownloadThread segThread : segDownloadThreadArr) {
				long start = segThread.getCurrentPosition();
				long end = segThread.getTargetPosition();
				this.output.writeLong(start);
				this.output.writeLong(end);
			}
			this.output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获得文件长度
	private long getFileSize() {
		// TODO Auto-generated method stub
		int nFileLength = -1;

		try {
			URL url = new URL(this.siteInfo.urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent",
			        "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3");

			int responseCode = conn.getResponseCode();

			if (responseCode >= 400) {
				processErrorCode(responseCode);
				return -2; // -2 represents access is error
			}
			nFileLength = conn.getContentLength();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nFileLength;
	}

	private void processErrorCode(int responseCode) {
		// TODO Auto-generated method stub
		System.err.println("Error Code: " + responseCode);
	}

	public static void main(String args[]) throws IOException, InterruptedException {
		SiteInfo bean = new SiteInfo("http://127.0.0.1:8080/fileTest/download/a.zip", "d:/test", "a.zip", 1);
		// SiteInfo bean1 = new
		// SiteInfo("http://localhost/download/friends.rmvb","C:\\xampp\\htdocs\\test","hello.rmvb",5);
		FileDownloader downloader = new FileDownloader(bean, 5);
		downloader.download();
	}
}
