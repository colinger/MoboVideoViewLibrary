package com.clov4r.moboplayer.android.nil.codec;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.util.SparseArray;

import com.clov4r.moboplayer.android.nil.library.DataSaveLib;

/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 MoboPlayer.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */
public class StreamingDownloadManager {
	private static StreamingDownloadManager mStreamingDownloadManager = null;
	private ArrayList<MoboDownloadListener> listenerList = null;
	private HashMap<Integer, StreamingDownloadData> dataMap = null;
	private HashMap<Integer, StreamingDownloadLib> libMap = null;
	private Context mContext = null;
	private DataSaveLib mDataSaveLib = null;

	public StreamingDownloadManager(Context con) {
		mContext = con;
		mDataSaveLib = new DataSaveLib(con,
				DataSaveLib.name_of_streaming_download_info, false);
		dataMap = (HashMap) mDataSaveLib.readData();
		if (dataMap == null)
			dataMap = new HashMap<Integer, StreamingDownloadData>();
		libMap = new HashMap<Integer, StreamingDownloadLib>();
		listenerList = new ArrayList<MoboDownloadListener>();
	}

	public static StreamingDownloadManager getInstance(Context con) {
		if (mStreamingDownloadManager == null)
			mStreamingDownloadManager = new StreamingDownloadManager(con);
		return mStreamingDownloadManager;
	}

	/**
	 * Save the download task info.
	 */
	public void saveDownloadInfo() {
		mDataSaveLib.saveData(dataMap);
	}

	public void stopAll() {
		if (libMap == null)
			return;
		Iterator<Integer> iterator = libMap.keySet().iterator();
		while (iterator.hasNext()) {
			int id = iterator.next();
			StreamingDownloadLib tmpLib = libMap.get(id);
			if (tmpLib.isBufferLib)
				stopBuffer(id);
			else
				stopDownload(iterator.next());
		}
		saveDownloadInfo();
		dataMap.clear();
		dataMap = null;
		libMap.clear();
		libMap = null;
		mStreamingDownloadManager = null;
	}

	/**
	 * Start a buffer task.
	 * 
	 * @param streamingUrl
	 * @param fileSavePath
	 * @param pktPath
	 * @param startPos
	 * @param timeout
	 * @return
	 */
	public int startBuffer(String streamingUrl, String fileSavePath,
			String pktPath, int startPos, int timeout) {
		StreamingDownloadLib tmpLib = getDownloadLib(streamingUrl, pktPath,
				fileSavePath, 0, false, timeout);
		tmpLib.startBuffer(startPos);
		return tmpLib.downloadData.id;
	}

	/**
	 * Start a download task.It will be created if not existed.
	 * 
	 * @param streamingUrl
	 * @param fileSavePath
	 * @param timeToDownload
	 * @param isLive
	 * @param timeout
	 * @return
	 */
	public int startDownload(String streamingUrl, String fileSavePath,
			int timeToDownload, boolean isLive, int timeout) {
		StreamingDownloadLib tmpLib = getDownloadLib(streamingUrl, null,
				fileSavePath, timeToDownload, isLive, timeout);
		if (tmpLib.getDownloadStatus() == StreamingDownloadData.download_status_stoped
				|| tmpLib.getDownloadStatus() == StreamingDownloadData.download_status_failed)
			tmpLib.startDownload();
		else if (tmpLib.getDownloadStatus() == StreamingDownloadData.download_status_paused)
			tmpLib.resumeDownload();
		return tmpLib.downloadData.id;
	}

	private StreamingDownloadLib getDownloadLib(String streamingUrl,
			String pktFilePath, String fileSavePath, int timeToDownload,
			boolean isLive, int timeout) {
		int key = getKeyOf(streamingUrl, fileSavePath);
		StreamingDownloadLib tmpLib = null;
		StreamingDownloadData downloadData = null;
		if (libMap.containsKey(key)) {
			tmpLib = libMap.get(key);
			downloadData = tmpLib.downloadData;
		} else {
			if (dataMap.containsKey(key))
				downloadData = dataMap.get(key);
			else
				downloadData = new StreamingDownloadData();
			downloadData.streamingUrl = streamingUrl;
			downloadData.fileSavePath = fileSavePath;
			if (pktFilePath != null && !"".equals(pktFilePath))
				downloadData.packetFile = pktFilePath;
			else
				downloadData.packetFile = fileSavePath + ".pkts";
			downloadData.timeStartToDownload = timeToDownload;
			downloadData.id = key;
			dataMap.put(key, downloadData);

			tmpLib = new StreamingDownloadLib(downloadData);
			tmpLib.setDownloadListener(mMoboDownloadListener);
			libMap.put(key, tmpLib);
		}
		downloadData.timeout = timeout;
		return tmpLib;
	}

	/**
	 * Pause download task
	 * 
	 * @param downloadId
	 */
	public void pauseDownload(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			tmpLib.pauseDownload();
		}
	}

	/**
	 * Restart an existed download task
	 * 
	 * @param downloadId
	 */
	public void resumeDownload(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			tmpLib.resumeDownload();
		}
	}

	/**
	 * Pause buffer task
	 * 
	 * @param downloadId
	 */
	public void pauseBuffer(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			tmpLib.pauseBuffer();
		}
	}

	/**
	 * Restart an existed buffer task
	 * 
	 * @param downloadId
	 */
	public void resumeBuffer(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			tmpLib.resumeBuffer();
		}
	}

	/**
	 * Stop download task
	 * 
	 * @param downloadId
	 */
	public void stopDownload(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			tmpLib.stopDownload();
			libMap.remove(downloadId);
			saveDownloadInfo();
		}
	}

	public void stopBuffer(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			tmpLib.stopBuffer();
			libMap.remove(downloadId);
			saveDownloadInfo();
			File pkt_file = new File(tmpLib.downloadData.packetFile);
			pkt_file.deleteOnExit();
			File tmp_pkt_file = new File(tmpLib.downloadData.packetFile
					+ ".tmp");
			tmp_pkt_file.deleteOnExit();
		}

	}

	public void removeDownload(int downloadId, boolean deleteDownloadedFile) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			int status = tmpLib.getDownloadStatus();
			if (status == StreamingDownloadData.download_status_started
					|| status == StreamingDownloadData.download_status_paused)
				tmpLib.stopDownload();

			libMap.remove(downloadId);
			dataMap.remove(downloadId);
			saveDownloadInfo();
			if (deleteDownloadedFile) {
				File file = new File(tmpLib.downloadData.fileSavePath);
				file.deleteOnExit();
				File pkt_file = new File(tmpLib.downloadData.packetFile);
				pkt_file.deleteOnExit();
				File tmp_pkt_file = new File(tmpLib.downloadData.packetFile
						+ ".tmp");
				tmp_pkt_file.deleteOnExit();
			}
		}
	}

	private int getKeyOf(String url, String path) {
		return (url + path).hashCode();
	}

	/**
	 * Get the duration of video
	 * 
	 * @param id
	 * @return
	 */
	public int getDurationOf(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			return tmpLib.getDuration();
		}
		return 0;
	}

	/**
	 * Get the time of video has been downloaded to.
	 * 
	 * @return
	 */
	public int getCurrentTimeDownloadedToOf(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			return tmpLib.getCurrentTimeDownloadedTo();
		}
		return 0;
	}

	/**
	 * Get the time of video start to download
	 * 
	 * @return
	 */
	public int getStartDownloadedTimeOf(int downloadId) {
		if (libMap.containsKey(downloadId)) {
			StreamingDownloadLib tmpLib = libMap.get(downloadId);
			return tmpLib.getStartDownloadedTime();
		}
		return 0;
	}

	public StreamingDownloadData getDownloadDataOfUrl(String url,
			String savePath) {
		Iterator<Integer> iterator = dataMap.keySet().iterator();
		while (iterator.hasNext()) {
			int id = iterator.next();
			StreamingDownloadData tmpData = dataMap.get(id);
			if (tmpData.streamingUrl.equals(url)
					&& tmpData.fileSavePath != null
					&& tmpData.fileSavePath.equals(savePath)) {
				return tmpData;
			}
		}
		return null;
	}

	public int getDownloadIdOf(String url, String savePath) {
		StreamingDownloadData tmpData = getDownloadDataOfUrl(url, savePath);
		if (tmpData != null)
			return tmpData.id;
		return 0;
	}

	MoboDownloadListener mMoboDownloadListener = null;

	public void setDownloadListener(MoboDownloadListener listener) {
		mMoboDownloadListener = listener;
	}

	public interface MoboDownloadListener {

		public void onDownloadProgressChanged(StreamingDownloadData data,
				int currentTime);

		public void onDownloadFinished(StreamingDownloadData data);

		public void onDownloadFailed(StreamingDownloadData data);

		public void onBuffering();
	}

	public static class StreamingDownloadData implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7489788052720159408L;
		public static final int download_status_paused = -1;
		public static final int download_status_stoped = 0;
		public static final int download_status_started = 1;
		public static final int download_status_finished = 2;
		public static final int download_status_failed = 3;

		public static final int DOWNLOAD_DEFAULT = 0;
		public static final int DOWNLOAD_PACKET_TEMP = 1;

		public int id;
		public String streamingUrl;
		public String fileSavePath;
		public String packetFile;
		// public int progress;
		/** 已经下载的字节数 **/
		public long finishSize;
		/** 当前下载到的时间 ，单位秒 **/
		public int currentTime;
		public int startTime = -1;
		/**  **/
		public int timeStartToDownload = 0;
		public int duration = 0;
		// public boolean isFinished;
		// public boolean isDownloadFailed;
		public int status = download_status_stoped;
		public String failedMsg = null;
		public long last_video_dts;
		/** 是否是直播 **/
		public boolean isLive;
		public int timeout;
		/** 数据类型 :0，下载；1，缓冲 **/
		public int downloadType;
	}

}
