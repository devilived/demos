package com.devil.model;

/**
 * Ҫץȡ���ļ�����Ϣ�����ļ������Ŀ¼�����֣�ץȡ�ļ��� URL �ȡ�
 * 
 * @author ETHAN
 * 
 */
public class SiteInfo {
	public String urlStr;// site's url
	public String savedPath; // saved file's path
	public String savedFileName; // saved file's name
	public int blockNum; // count of splited downloading file

	public SiteInfo() {
		this("", "", "", 5); // default value of nSplitter is 5
	}

	public SiteInfo(String urlPath, String savedFilePath, String savedFileName, int blockNum) {
		// TODO Auto-generated constructor stub
		this.urlStr = urlPath;
		this.savedFileName = savedFileName;
		this.savedPath = savedFilePath;
		this.blockNum = blockNum;
	}
}
