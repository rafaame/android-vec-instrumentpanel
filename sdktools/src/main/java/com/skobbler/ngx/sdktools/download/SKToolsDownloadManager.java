package com.skobbler.ngx.sdktools.download;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Component that manages current downloads list
 * Created by CatalinM on 11/13/2014.
 */
public class SKToolsDownloadManager {

    /**
     * download files extensions
     */
    public static final String SKM_FILE_EXTENSION = ".skm";

    public static final String ZIP_FILE_EXTENSION = ".zip";

    public static final String TXG_FILE_EXTENSION = ".txg";

    public static final String POINT_EXTENSION = ".";

    /**
     * contains all items that are in download queue
     */
    private Queue<SKToolsDownloadItem> queuedDownloads;

    /**
     * current download listener (used to notify the user interface)
     */
    private SKToolsDownloadListener downloadListener;

    /**
     * current download thread
     */
    private SKToolsDownloadPerformer downloadThread;

    /**
     * single instance for SKToolsDownloadManager reference
     */
    private static SKToolsDownloadManager skToolsDownloadManagerInstance;

    /**
     * constructs an object of SKToolsDownloadManager type
     * @param downloadListener download listener
     */
    private SKToolsDownloadManager(SKToolsDownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    /**
     * gets a single instance of SKToolsDownloadManager reference
     * @param downloadListener download listener
     * @return a single instance of SKToolsDownloadManager reference
     */
    public static SKToolsDownloadManager getInstance(SKToolsDownloadListener downloadListener) {
        if (skToolsDownloadManagerInstance == null) {
            skToolsDownloadManagerInstance = new SKToolsDownloadManager(downloadListener);
        } else {
            skToolsDownloadManagerInstance.setDownloadListener(downloadListener);
        }
        return skToolsDownloadManagerInstance;
    }

    /**
     * sets a download listener for download manager component
     * @param downloadListener download listener that will be set
     */
    public void setDownloadListener(SKToolsDownloadListener downloadListener) {
        this.downloadListener = downloadListener;
        if (downloadThread != null) {
            downloadThread.setDownloadListener(downloadListener);
        }
    }

    /**
     * start download operation
     * @param downloadItems download items that will be added to download queue
     */
    public void startDownload(List<SKToolsDownloadItem> downloadItems) {
        synchronized (SKToolsDownloadPerformer.class) {
            if ((downloadThread == null) || (!downloadThread.isAlive())) {
                if (queuedDownloads != null) {
                    queuedDownloads.clear();
                } else {
                    queuedDownloads = new LinkedList<SKToolsDownloadItem>();
                }
                putAnyPausedItemFirst(downloadItems);
                queuedDownloads.addAll(downloadItems);
                downloadThread = new SKToolsDownloadPerformer(queuedDownloads, downloadListener);
                downloadThread.start();
            } else {
                if (queuedDownloads != null) {
                    queuedDownloads.addAll(downloadItems);
                }
            }
        }
    }

    /**
     * cancels a download item (from download queue) that has a specific code
     * @param downloadItemCode current download item code
     * @return true, if current download is cancelled, false otherwise (because download process is not running)
     */
    public boolean cancelDownload(String downloadItemCode) {
        synchronized (SKToolsDownloadPerformer.class) {
            if ((downloadThread != null) && downloadThread.isAlive()) {
                if (queuedDownloads != null) {
                    SKToolsDownloadItem removedItem = null;
                    for (SKToolsDownloadItem currentItem : queuedDownloads) {
                        if ((currentItem != null) && (currentItem.getItemCode() != null) && currentItem.getItemCode().equalsIgnoreCase(downloadItemCode)) {
                            byte currentItemState = currentItem.getDownloadState();
                            // if the download is already running (cannot cancel an already downloaded, installing or installed map)
                            if ((currentItemState == SKToolsDownloadItem.PAUSED) || (currentItemState == SKToolsDownloadItem.DOWNLOADING)) {
                                // mark that current download is cancelled, if download thread is running
                                downloadThread.setCurrentDownloadAsCancelled();
                                return true;
                            } else if (currentItemState == SKToolsDownloadItem.QUEUED) {
                                removedItem = currentItem;
                                break;
                            }
                        }
                    }
                    if (removedItem != null) {
                        // remove current item from download queue
                        queuedDownloads.remove(removedItem);
                        // notify the UI that current download was cancelled
                        if (downloadListener != null) {
                            downloadListener.onDownloadCancelled(removedItem.getItemCode());
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * pause download thread
     * @return true, if download thread is paused, false otherwise (because download process is not running)
     */
    public boolean pauseDownloadThread() {
        synchronized ((SKToolsDownloadPerformer.class)) {
            // if download thread is alive, stop it
            if ((downloadThread != null) && downloadThread.isAlive()) {
                downloadThread.setDownloadProcessAsPaused();
                return true;
            }
            return false;
        }
    }

    /**
     * cancel all downloads from download queue
     * @return true, if download thread is cancelled, false otherwise (because download process is not running)
     */
    public boolean cancelAllDownloads() {
        synchronized (SKToolsDownloadPerformer.class) {
            // if download thread is alive, stop it and return true
            if ((downloadThread != null) && downloadThread.isAlive()) {
                downloadThread.setDownloadProcessAsCancelled();
                return true;
            }
            return false;
        }
    }

    /**
     * @return true if download process is running, false otherwise
     */
    public boolean isDownloadProcessRunning() {
        synchronized (SKToolsDownloadPerformer.class) {
            // if download thread is alive, stop it and return true
            if ((downloadThread != null) && downloadThread.isAlive()) {
                return true;
            }
            return false;
        }
    }

    /**
     * put the paused/downloading item at the top of the list
     * @param downloadItems download items that will be added to download queue
     */
    private void putAnyPausedItemFirst(List<SKToolsDownloadItem> downloadItems) {
        SKToolsDownloadItem downloadingItem = null;
        int downloadingItemIndex = 0;
        for (SKToolsDownloadItem currentItem : downloadItems) {
            if ((currentItem.getDownloadState() == SKToolsDownloadItem.DOWNLOADING) || (currentItem.getDownloadState() == SKToolsDownloadItem.PAUSED)) {
                downloadingItem = currentItem;
                break;
            }
            downloadingItemIndex++;
        }
        if (downloadingItem != null) {
            downloadItems.remove(downloadingItemIndex);
            downloadItems.add(0, downloadingItem);
        }
    }
}