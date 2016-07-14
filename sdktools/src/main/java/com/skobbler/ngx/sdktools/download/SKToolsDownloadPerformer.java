package com.skobbler.ngx.sdktools.download;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import com.skobbler.ngx.packages.SKPackageManager;
import com.skobbler.ngx.util.SKLogging;

/**
 * perform download for queued items
 * Created by Tudor on 11/14/2014.
 */
public class SKToolsDownloadPerformer extends Thread {

    /**
     * the tag associated with this class, used for debugging
     */
    public static final String TAG = "SKToolsDownloadPerformer";

    /**
     * Key for HTTP header request property to send the DOWNLOADED bytes
     */
    private static final String HTTP_PROP_RANGE = "Range";

    /**
     * represents the number of bytes from one megabyte
     */
    private static final int NO_BYTES_INTO_ONE_MB = 1048576;

    /**
     * the timeout limit for the edge cases requests
     */
    private static final int TIME_OUT_LIMIT_FOR_EDGE_CASES = 20000;

    /**
     * represents the number of milliseconds from one second
     */
    private static final int NO_MILLIS_INTO_ONE_SEC = 1000;

    /**
     * the timeout limit for the requests
     */
    private static final int TIME_OUT_LIMIT = 15000;

    /**
     * true, if current download is cancelled while download thread is running
     */
    private volatile boolean isCurrentDownloadCancelled;

    /**
     * true, if download process is cancelled
     */
    private volatile boolean isDownloadProcessCancelled;

    /**
     * true, if download request doesn't respond
     */
    private volatile boolean isDownloadRequestUnresponsive;

    /**
     * tells that current download process is paused
     */
    private volatile boolean isDownloadProcessPaused;

    /**
     * queued downloads
     */
    private Queue<SKToolsDownloadItem> queuedDownloads;

    /**
     * download listener
     */
    private SKToolsDownloadListener downloadListener;

    /**
     * current download step
     */
    private SKToolsFileDownloadStep currentDownloadStep;

    /**
     * current download item
     */
    private SKToolsDownloadItem currentDownloadItem;

    // WifiLock used for long running downloads
    private WifiManager.WifiLock wifiLock;

    /**
     * instance of HttpClient used for download
     */
    private HttpClient httpClient;

    /**
     * current HTTP request
     */
    private HttpRequestBase httpRequest;

    /**
     * download timeout handler ; added for the edge cases (networks on which HttpClient blocks)
     */
    private Handler downloadTimeoutHandler;

    /**
     * runs when download request cannot return a response, after a while
     */
    private Runnable downloadTimeoutRunnable;

    /**
     * time at first retry
     */
    private long timeAtFirstRetry;

    /**
     * true, if any retry was made (reset after an INTERNET connection is received)
     */
    private volatile boolean anyRetryMade;

    /**
     * last time when the INTERNET was working
     */
    private long lastTimeWhenInternetWorked;

    /**
     * SK-TOOLS unzip performer
     */
    private SKToolsUnzipPerformer skToolsUnzipPerformer;

    /**
     * creates an object of SKToolsDownloadPerformer type
     * @param queuedDownloads queued downloads
     * @param downloadListener download listener
     */
    public SKToolsDownloadPerformer(Queue<SKToolsDownloadItem> queuedDownloads, SKToolsDownloadListener downloadListener) {
        synchronized (SKToolsDownloadPerformer.class) {
            this.queuedDownloads = queuedDownloads;
        }
        this.downloadListener = downloadListener;
    }

    public void setDownloadListener(SKToolsDownloadListener downloadListener) {
        this.downloadListener = downloadListener;
        if (skToolsUnzipPerformer != null) {
            skToolsUnzipPerformer.setDownloadListener(downloadListener);
        }
    }

    @Override
    public void run() {
        // current input stream
        InputStream responseStream = null;
        byte[] data;
        // total bytes read
        long bytesReadSoFar;
        // bytes read during current INTERNET connection
        long bytesReadInThisConnection = 0;
        // time of the download during current INTERNET connection
        long lastDownloadProgressTime = 0;
        // memory needed
        long memoryNeeded;

        anyRetryMade = false;
        timeAtFirstRetry = 0;
        lastTimeWhenInternetWorked = System.currentTimeMillis();

        initializeResourcesWhenDownloadThreadStarts();

        while (existsAnyRemainingDownload()) {
            if ((currentDownloadItem == null) || (queuedDownloads == null) || (currentDownloadStep == null) || (currentDownloadStep.getDestinationPath() == null) ||
                    (currentDownloadStep.getDownloadURL() == null)) {
                break;
            }

            // change the state for current download item
            if (currentDownloadItem.getDownloadState() != SKToolsDownloadItem.DOWNLOADING) {
                currentDownloadItem.setDownloadState(SKToolsDownloadItem.DOWNLOADING);
            }

            // check if current item is already downloaded (could be the case when download is performed very slow and the user exists the download thread without finishing the
            // download, but with the file downloaded)
            if (isCurrentItemFullyDownloaded()) {
                try {
                    finishCurrentDownload();
                } catch (SocketException ex) {
                    // restart the download in this case
                    SKLogging.writeLog(TAG, "Not possible, because in this case the download wouldn't appear to be finished => restart the download and remove the old data!!!",
                            SKLogging.LOG_DEBUG);
                    // reset the number of downloaded bytes and remove them from storage
                    currentDownloadItem.setNoDownloadedBytes(0);
                    String deleteCmd = "rm -r " + currentDownloadItem.getCurrentStepDestinationPath();
                    Runtime runtime = Runtime.getRuntime();
                    try {
                        runtime.exec(deleteCmd);
                        SKLogging.writeLog(TAG, "The file was deleted from its current installation folder", SKLogging.LOG_DEBUG);
                    } catch (IOException e) {
                        SKLogging.writeLog(TAG, "The file couldn't be deleted !!!", SKLogging.LOG_DEBUG);
                    }
                }
            } else {
                // create a new download request
                httpRequest = new HttpGet(currentDownloadStep.getDownloadURL());

                SKLogging.writeLog(TAG, "Current url = " + currentDownloadStep.getDownloadURL() + " ; current step = " + currentDownloadItem.getCurrentStepIndex(),
                        SKLogging.LOG_DEBUG);

                // resume operation => send already downloaded bytes
                bytesReadSoFar = sendAlreadyDownloadedBytes();

                // check if exists any free memory and return if there is not enough memory for this download
                memoryNeeded = getNeededMemoryForCurrentDownload(bytesReadSoFar);
                if (memoryNeeded != 0) {
                    SKLogging.writeLog(TAG, "Not enough memory on current storage", SKLogging.LOG_DEBUG);
                    pauseDownloadProcess();
                    // notify the UI about memory issues
                    if (downloadListener != null) {
                        downloadListener.onNotEnoughMemoryOnCurrentStorage(currentDownloadItem);
                    }
                    break;
                }

                // database and UI update
                if (downloadListener != null) {
                    downloadListener.onDownloadProgress(currentDownloadItem);
                }

                try {
                    // starts the timeout handler
                    startsDownloadTimeoutHandler();
                    // executes the download request
                    HttpResponse response = httpClient.execute(httpRequest);
                    if (response == null) {
                        throw new SocketException();
                    } else {
                        anyRetryMade = false;
                        HttpEntity entity = response.getEntity();
                        int statusCode = response.getStatusLine().getStatusCode();
                        // if other status code than 200 or 206(partial download) throw exception
                        if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_PARTIAL) {
                            SKLogging.writeLog(TAG, "Wrong status code returned !", SKLogging.LOG_DEBUG);
                            throw new IOException("HTTP response code: " + statusCode);
                        }
                        // stops the timeout handler
                        stopsDownloadTimeoutHandler();
                        SKLogging.writeLog(TAG, "Correct response status code returned !", SKLogging.LOG_DEBUG);
                        try {
                            if (entity != null) {
                                responseStream = entity.getContent();
                            }
                        } catch (final IllegalStateException e) {
                            SKLogging.writeLog(TAG, "The returned response content is not correct !", SKLogging.LOG_DEBUG);
                        }
                        if (responseStream == null) {
                            SKLogging.writeLog(TAG, "Response stream is null !!!", SKLogging.LOG_DEBUG);
                        }

                        // create a byte array buffer of 1Mb
                        data = new byte[NO_BYTES_INTO_ONE_MB];
                        // creates the randomAccessFile - if exists it opens it
                        RandomAccessFile localFile = new RandomAccessFile(currentDownloadStep.getDestinationPath(), "rw");
                        bytesReadSoFar = localFile.length();
                        // position in the file
                        localFile.seek(bytesReadSoFar);
                        while (true) {
                            // starts the timeout handler
                            startsDownloadTimeoutHandler();
                            // reads 1 MB data
                            int bytesReadThisTime = (responseStream != null) ? responseStream.read(data, 0, data.length) : 0;
                            // stops the timeout handler
                            stopsDownloadTimeoutHandler();
                            // check number of read bytes
                            if (bytesReadThisTime > 0) {
                                lastTimeWhenInternetWorked = System.currentTimeMillis();
                                bytesReadSoFar += bytesReadThisTime;
                                currentDownloadItem.setNoDownloadedBytes(bytesReadSoFar);
                                bytesReadInThisConnection += bytesReadThisTime;
                                currentDownloadItem.setNoDownloadedBytesInThisConnection(bytesReadInThisConnection);
                                // write the chunk of data in the file
                                localFile.write(data, 0, bytesReadThisTime);
                                long newTime = System.currentTimeMillis();
                                // notify the UI every second
                                if ((newTime - lastDownloadProgressTime) > NO_MILLIS_INTO_ONE_SEC) {
                                    lastDownloadProgressTime = newTime;
                                    if (downloadListener != null) {
                                        downloadListener.onDownloadProgress(currentDownloadItem);
                                    }
                                }
                            } else if (bytesReadThisTime == -1) {
                                SKLogging.writeLog(TAG, "No more data to read, so exit !", SKLogging.LOG_DEBUG);
                                // if no more data to read, exit
                                break;
                            }
                            if (isCurrentDownloadCancelled || isDownloadProcessCancelled || isDownloadRequestUnresponsive || isDownloadProcessPaused) {
                                break;
                            }
                        }
                        localFile.close();
                        responseStream = null;

                        if (!isDownloadRequestUnresponsive) {
                            if (isDownloadProcessCancelled) {
                                cancelDownloadProcess();
                            } else if (isCurrentDownloadCancelled) {
                                cancelCurrentDownload();
                            } else if (isDownloadProcessPaused) {
                                pauseDownloadProcess();
                            } else {
                                finishCurrentDownload();
                            }
                        }
                    }
                } catch (final SocketException e) {
                    SKLogging.writeLog(TAG, "Socket Exception ; " + e.getMessage(), SKLogging.LOG_DEBUG);
                    if (!isDownloadRequestUnresponsive) {
                        stopIfTimeoutLimitEnded(false);
                    }
                } catch (final UnknownHostException e) {
                    SKLogging.writeLog(TAG, "Unknown Host Exception ; " + e.getMessage(), SKLogging.LOG_DEBUG);
                    if (!isDownloadRequestUnresponsive) {
                        stopIfTimeoutLimitEnded(false);
                    }
                } catch (final IOException e) {
                    SKLogging.writeLog(TAG, "IO Exception ; " + e.getMessage(), SKLogging.LOG_DEBUG);
                    if (!isDownloadRequestUnresponsive) {
                        stopIfTimeoutLimitEnded(false);
                    }
                } catch (final IndexOutOfBoundsException e) {
                    SKLogging.writeLog(TAG, "Index Out Of Bounds Exception ; " + e.getMessage(), SKLogging.LOG_DEBUG);
                    stopsDownloadTimeoutHandler();
                }
            }
        }
        releaseResourcesWhenDownloadThreadFinishes();
        SKLogging.writeLog(TAG, "The download thread has stopped", SKLogging.LOG_DEBUG);
    }

    /**
     * set current download as cancelled
     */
    public void setCurrentDownloadAsCancelled() {
        isCurrentDownloadCancelled = true;
    }

    /**
     * sets download process as cancelled
     */
    public void setDownloadProcessAsCancelled() {
        isDownloadProcessCancelled = true;
    }

    /**
     * sets download process as paused
     */
    public void setDownloadProcessAsPaused() {
        isDownloadProcessPaused = true;
    }

    /**
     * send the number of bytes already DOWNLOADED - for the resume operation
     */
    private long sendAlreadyDownloadedBytes() {
        long bytesRead;
        try {
            final RandomAccessFile destinationFile = new RandomAccessFile(currentDownloadItem.getCurrentStepDestinationPath(), "r");
            bytesRead = destinationFile.length();
            if (bytesRead > 0) {
                SKLogging.writeLog(TAG, "There are some bytes at this path ; number of downloaded bytes for current resource is " + currentDownloadItem.getNoDownloadedBytes()
                                + " ; download step = " + currentDownloadItem.getCurrentStepIndex() + " ; current path = " + currentDownloadItem.getCurrentStepDestinationPath(),
                        SKLogging.LOG_DEBUG);
                if (currentDownloadItem.getNoDownloadedBytes() == 0) {
                    SKLogging.writeLog(TAG, "There remained some resources with the same name at the same path ! Try to delete the file " + currentDownloadItem
                            .getCurrentStepDestinationPath(), SKLogging.LOG_DEBUG);
                    String deleteCmd = "rm -r " + currentDownloadItem.getCurrentStepDestinationPath();
                    Runtime runtime = Runtime.getRuntime();
                    try {
                        runtime.exec(deleteCmd);
                        SKLogging.writeLog(TAG, "The file was deleted from its current installation folder", SKLogging.LOG_DEBUG);
                    } catch (IOException e) {
                        SKLogging.writeLog(TAG, "The file couldn't be deleted !!!", SKLogging.LOG_DEBUG);
                    }
                    bytesRead = 0;
                } else {
                    SKLogging.writeLog(TAG, "Current resource is only partially downloaded, so its download will continue = ", SKLogging.LOG_DEBUG);
                    httpRequest.addHeader(HTTP_PROP_RANGE, "bytes=" + bytesRead + "-");
                }
            }
            destinationFile.close();
        } catch (final FileNotFoundException e) {
            bytesRead = 0;
        } catch (final IOException e) {
            bytesRead = 0;
        }

        return bytesRead;
    }

    /**
     * gets needed memory for current download
     * @param bytesRead bytes already read
     */
    private long getNeededMemoryForCurrentDownload(long bytesRead) {
        long neededSize = currentDownloadItem.getRemainingSize() - bytesRead;
        String filePath = currentDownloadItem.getCurrentStepDestinationPath();
        String basePath = filePath.substring(0, filePath.indexOf(new StringBuilder(currentDownloadItem.getItemCode()).append(SKToolsDownloadManager.POINT_EXTENSION).toString()));
        long memoryNeeded = SKToolsDownloadUtils.getNeededBytesForADownload(neededSize, basePath);
        SKLogging.writeLog(TAG, "Memory needed = " + memoryNeeded, SKLogging.LOG_DEBUG);
        return memoryNeeded;
    }

    /**
     * checks if there is any remaining item to download
     * @return true, if there is any remaining item to download, false otherwise
     */
    private boolean existsAnyRemainingDownload() {
        synchronized (SKToolsDownloadPerformer.class) {
            if ((queuedDownloads != null) && !queuedDownloads.isEmpty()) {
                currentDownloadItem = queuedDownloads.peek();
                while (currentDownloadItem != null) {
                    if ((currentDownloadItem.getDownloadState() == SKToolsDownloadItem.INSTALLING) || (currentDownloadItem.getDownloadState() == SKToolsDownloadItem.NOT_QUEUED)
                            || (currentDownloadItem.getDownloadState() == SKToolsDownloadItem.INSTALLED)) {
                        if (currentDownloadItem.getDownloadState() == SKToolsDownloadItem.INSTALLING) {
                            SKLogging.writeLog(TAG, "Current download item = " + currentDownloadItem.getItemCode() + " is in INSTALLING state => add it to install queue",
                                    SKLogging.LOG_DEBUG);
                            // add current resource to install queue
                            synchronized (SKToolsUnzipPerformer.class) {
                                if ((skToolsUnzipPerformer == null) || (!skToolsUnzipPerformer.isAlive())) {
                                    skToolsUnzipPerformer = new SKToolsUnzipPerformer(downloadListener);
                                    skToolsUnzipPerformer.addItemForInstall(currentDownloadItem);
                                    skToolsUnzipPerformer.start();
                                } else {
                                    skToolsUnzipPerformer.addItemForInstall(currentDownloadItem);
                                }
                            }
                        } else {
                            SKLogging.writeLog(TAG, "Current download item = " + currentDownloadItem.getItemCode() + " is in NOT_QUEUED / INSTALLED state => remove it from " +
                                    "download queue", SKLogging.LOG_DEBUG);
                        }
                        // remove this item from download queue and go to next item
                        queuedDownloads.poll();
                        currentDownloadItem = queuedDownloads.peek();
                    } else {
                        SKLogging.writeLog(TAG, "Current download item = " + currentDownloadItem.getItemCode() + " is added to download queue", SKLogging.LOG_DEBUG);
                        currentDownloadStep = currentDownloadItem.getCurrentDownloadStep();
                        if (currentDownloadStep != null) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * initializes resources(http client, wifi lock) when download thread starts
     */
    private void initializeResourcesWhenDownloadThreadStarts() {
        // instance of HttpClient instance
        httpClient = new DefaultHttpClient();
        if ((downloadListener != null) && (downloadListener instanceof Activity)) {
            WifiManager wifimanager = (WifiManager) ((Activity) downloadListener).getSystemService(Context.WIFI_SERVICE);
            wifiLock = wifimanager.createWifiLock("my_lock");
        }
    }

    /**
     * release resources(http client, wifi lock) when download thread finishes
     */
    private void releaseResourcesWhenDownloadThreadFinishes() {
        // release the WI-FI lock
        if (wifiLock != null) {
            wifiLock.release();
        }
        // release the HttpClient resource
        if (httpClient != null) {
            if (httpClient.getConnectionManager() != null) {
                try {
                    httpClient.getConnectionManager().shutdown();
                } catch (Exception ex) {
                    SKLogging.writeLog(TAG, "Thrown exception when release the HttpClient resource ; exception = " + ex.getMessage(), SKLogging.LOG_DEBUG);
                }
            }
        }
    }

    /**
     * starts the download timeout handler
     */
    private void startsDownloadTimeoutHandler() {
        if ((downloadListener != null) && downloadListener instanceof Activity) {
            ((Activity) downloadListener).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (downloadTimeoutHandler == null) {
                        downloadTimeoutHandler = new Handler();
                        downloadTimeoutRunnable = new Runnable() {

                            @Override
                            public void run() {
                                SKLogging.writeLog(TAG, "The blocked request is stopped now => the user is notified that connection was lost", SKLogging.LOG_DEBUG);
                                isDownloadRequestUnresponsive = true;
                                // abort current request
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        if (httpRequest != null) {
                                            httpRequest.abort();
                                        }
                                        return null;
                                    }
                                }.execute();
                                downloadTimeoutHandler = null;
                                if (!isDownloadProcessCancelled && !isCurrentDownloadCancelled || !isDownloadProcessPaused) {
                                    stopDownloadProcessWhenInternetConnectionFails(false);
                                } else if (isDownloadProcessCancelled) {
                                    cancelDownloadProcess();
                                } else if (isCurrentDownloadCancelled) {
                                    cancelCurrentDownload();
                                } else if (isDownloadProcessPaused) {
                                    pauseDownloadProcess();
                                }
                            }
                        };
                        downloadTimeoutHandler.postDelayed(downloadTimeoutRunnable, TIME_OUT_LIMIT_FOR_EDGE_CASES);
                    }
                }
            });
        }
    }

    /**
     * stops the download timeout handler
     */
    private void stopsDownloadTimeoutHandler() {
        if ((downloadListener != null) && downloadListener instanceof Activity) {
            ((Activity) downloadListener).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (downloadTimeoutHandler != null) {
                        downloadTimeoutHandler.removeCallbacks(downloadTimeoutRunnable);
                        downloadTimeoutRunnable = null;
                        downloadTimeoutHandler = null;
                    }
                }
            });
        }
    }

    /**
     * if timeout limit ended, stops otherwise, performs the retry mechanism
     * @param stopRequest true if the request must be stopped
     */
    private void stopIfTimeoutLimitEnded(boolean stopRequest) {
        stopsDownloadTimeoutHandler();
        if (((System.currentTimeMillis() - lastTimeWhenInternetWorked) > TIME_OUT_LIMIT) || stopRequest) {
            SKLogging.writeLog(TAG, "The request last more than 15 seconds, so no timeout is made", SKLogging.LOG_DEBUG);
            if (!isDownloadProcessCancelled && !isCurrentDownloadCancelled && !isDownloadProcessPaused) {
                // stop download process and notifies the UI
                stopDownloadProcessWhenInternetConnectionFails(true);
            } else if (isDownloadProcessCancelled) {
                cancelDownloadProcess();
            } else if (isCurrentDownloadCancelled) {
                cancelCurrentDownload();
            } else if (isDownloadProcessPaused) {
                pauseDownloadProcess();
            }
        } else {
            retryUntilTimeoutLimitReached();
        }
    }

    /**
     * if download process is not paused/cancelled, or current download is not cancelled, retries until timeout limit is reached
     */
    private void retryUntilTimeoutLimitReached() {
        if (!isDownloadProcessCancelled && !isCurrentDownloadCancelled && !isDownloadProcessPaused) {
            // if no retry was made, during current INTERNET connection, then retain the time at which the first one is made
            if (!anyRetryMade) {
                timeAtFirstRetry = System.currentTimeMillis();
                anyRetryMade = true;
            }

            // if it didn't pass 15 seconds from the first retry, will sleep 0.5 seconds and then will make a new attempt to download the resource
            if ((System.currentTimeMillis() - timeAtFirstRetry) < TIME_OUT_LIMIT) {
                SKLogging.writeLog(TAG, "Sleep and then retry", SKLogging.LOG_DEBUG);
                try {
                    Thread.sleep(NO_MILLIS_INTO_ONE_SEC / 2);
                } catch (final InterruptedException e1) {
                    SKLogging.writeLog(TAG, "Retry ; interrupted exception = " + e1.getMessage(), SKLogging.LOG_DEBUG);
                }
            } else {
                stopIfTimeoutLimitEnded(true);
            }
        } else if (isDownloadProcessCancelled) {
            cancelDownloadProcess();
        } else if (isCurrentDownloadCancelled) {
            cancelCurrentDownload();
        } else if (isDownloadProcessPaused) {
            pauseDownloadProcess();
        }
    }

    /**
     * cancels current download while download process is running
     */
    private void cancelCurrentDownload() {
        isCurrentDownloadCancelled = false;
        if (isDownloadProcessPaused) {
            isDownloadProcessPaused = false;
        }
        // cancel download for current item
        if (currentDownloadItem != null) {
            currentDownloadItem.markAsNotQueued();
        }
        // remove current download from download queue (first element from queue)
        synchronized (SKToolsDownloadPerformer.class) {
            if (queuedDownloads != null) {
                queuedDownloads.poll();
            }
        }
        // notify the UI that current download was cancelled
        if (downloadListener != null) {
            downloadListener.onDownloadCancelled(currentDownloadItem.getItemCode());
        }
    }

    /**
     * cancels download process while running
     */
    private void cancelDownloadProcess() {
        isDownloadProcessCancelled = false;
        if (isCurrentDownloadCancelled) {
            isCurrentDownloadCancelled = false;
        }
        if (isDownloadProcessPaused) {
            isDownloadProcessPaused = false;
        }
        // cancel download for current item
        if (currentDownloadItem != null) {
            currentDownloadItem.markAsNotQueued();
        }
        // remove all resources from download queue => stop download thread
        synchronized (SKToolsDownloadPerformer.class) {
            if (queuedDownloads != null) {
                queuedDownloads.clear();
            }
        }
        if (downloadListener != null) {
            downloadListener.onAllDownloadsCancelled();
        }
    }

    /**
     * pause download process while running
     */
    private void pauseDownloadProcess() {
        pauseDownloadThread();
        if (downloadListener != null) {
            downloadListener.onDownloadPaused(currentDownloadItem);
        }
    }

    /**
     * finishes current download
     */
    private void finishCurrentDownload() throws SocketException {
        if (currentDownloadItem != null) {
            // check the total read bytes for current download
            long totalBytesRead;
            try {
                RandomAccessFile currentDestinationFile = new RandomAccessFile(currentDownloadItem.getCurrentStepDestinationPath(), "r");
                totalBytesRead = currentDestinationFile.length();
            } catch (final FileNotFoundException e) {
                totalBytesRead = 0;
            } catch (final IOException e) {
                totalBytesRead = 0;
            }
            if (totalBytesRead < currentDownloadItem.getCurrentDownloadStep().getDownloadItemSize()) {
                SKLogging.writeLog(TAG, "The " + currentDownloadItem.getItemCode() + " current file was not fully downloaded ; total bytes read = " + totalBytesRead + " ; size = "
                                + currentDownloadItem.getCurrentDownloadStep().getDownloadItemSize() + " ; current step index = " + currentDownloadItem.getCurrentStepIndex(),
                        SKLogging.LOG_DEBUG);
                throw new SocketException();
            } else {
                currentDownloadItem.goToNextDownloadStep();
                if (currentDownloadItem.isDownloadFinished()) {
                    currentDownloadItem.setDownloadState(SKToolsDownloadItem.DOWNLOADED);
                    // remove current download from download queue
                    synchronized (SKToolsDownloadPerformer.class) {
                        if (queuedDownloads != null) {
                            queuedDownloads.poll();
                        }
                    }
                    if (currentDownloadItem.unzipIsNeeded()) { // UNZIP is needed for current resource
                        SKLogging.writeLog(TAG, "Current item = " + currentDownloadItem.getItemCode() + " is now downloaded => add it to install queue for unzip",
                                SKLogging.LOG_DEBUG);
                        // we know that UNZIP operation corresponds to last download step
                        currentDownloadItem.setCurrentStepIndex((byte) (currentDownloadItem.getCurrentStepIndex() - 1));

                        // notify the UI that current resource was downloaded
                        if (downloadListener != null) {
                            downloadListener.onDownloadProgress(currentDownloadItem);
                        }

                        // add current resource to install queue
                        synchronized (SKToolsUnzipPerformer.class) {
                            if ((skToolsUnzipPerformer == null) || (!skToolsUnzipPerformer.isAlive())) {
                                skToolsUnzipPerformer = new SKToolsUnzipPerformer(downloadListener);
                                skToolsUnzipPerformer.addItemForInstall(currentDownloadItem);
                                skToolsUnzipPerformer.start();
                            } else {
                                skToolsUnzipPerformer.addItemForInstall(currentDownloadItem);
                            }
                        }
                    } else { // UNZIP is not needed for current resource => INSTALL it now
                        // go back to previous step
                        currentDownloadItem.setCurrentStepIndex((byte) (currentDownloadItem.getCurrentStepIndex() - 1));
                        String rootFilePath = null;
                        String destinationPath = currentDownloadItem.getCurrentStepDestinationPath();
                        if (destinationPath != null) {
                            rootFilePath = destinationPath.substring(0, destinationPath.indexOf(new StringBuilder(currentDownloadItem.getItemCode()).append(SKToolsDownloadManager
                                    .POINT_EXTENSION).toString()));
                        }
                        SKLogging.writeLog(TAG, "Current item = " + currentDownloadItem.getItemCode() + " is now downloaded => unzip is not needed => install it now at base path" +
                                " = " + rootFilePath, SKLogging.LOG_DEBUG);
                        if (rootFilePath != null) {
                            int result = SKPackageManager.getInstance().addOfflinePackage(rootFilePath, currentDownloadItem.getItemCode());
                            SKLogging.writeLog(TAG, "Current resource installing result code = " + result, SKLogging.LOG_DEBUG);
                            if ((result & SKPackageManager.ADD_PACKAGE_MISSING_SKM_RESULT & SKPackageManager.ADD_PACKAGE_MISSING_NGI_RESULT & SKPackageManager
                                    .ADD_PACKAGE_MISSING_NGI_DAT_RESULT) == 0) {
                                // current install was performed with success set current resource as already download
                                currentDownloadItem.setDownloadState(SKToolsDownloadItem.INSTALLED);
                                SKLogging.writeLog(TAG, "The " + currentDownloadItem.getItemCode() + " resource was successfully downloaded and installed by our NG component.",
                                        SKLogging.LOG_DEBUG);
                                // notify the UI that current resource was installed
                                if (downloadListener != null) {
                                    downloadListener.onInstallFinished(currentDownloadItem);
                                }
                            } else {
                                // current install was performed with error => set current resource as NOT_QUEUED, remove downloaded bytes etc
                                currentDownloadItem.markAsNotQueued();
                                SKLogging.writeLog(TAG, "The " + currentDownloadItem.getItemCode() + " resource couldn't be installed by our NG component, " +
                                        "although it was downloaded.", SKLogging.LOG_DEBUG);
                                // notify the UI that current resource was not installed
                                if (downloadListener != null) {
                                    downloadListener.onDownloadProgress(currentDownloadItem);
                                }
                            }
                        } else {
                            // current install was performed with error => set current resource as NOT_QUEUED, remove downloaded bytes etc
                            currentDownloadItem.markAsNotQueued();
                            SKLogging.writeLog(TAG, "The " + currentDownloadItem.getItemCode() + " resource couldn't be installed by our NG component, " +
                                    "although it was downloaded, because installing path is null", SKLogging.LOG_DEBUG);
                            // notify the UI that current resource was not installed
                            if (downloadListener != null) {
                                downloadListener.onDownloadProgress(currentDownloadItem);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * stops download process when internet connection fails
     * @param failureResponseReceivedFromServer true, if a response was received from server (add to identify a blocking request)
     */
    private void stopDownloadProcessWhenInternetConnectionFails(boolean failureResponseReceivedFromServer) {
        pauseDownloadProcess();
        // update the UI (set current resource as paused, shows a toast)
        if (downloadListener != null) {
            downloadListener.onInternetConnectionFailed(currentDownloadItem, failureResponseReceivedFromServer);
        }
    }

    /**
     * pauses download thread (stops current download thread, pauses current downloading resource)
     */
    private void pauseDownloadThread() {
        // pause current resource
        if (currentDownloadItem != null) {
            currentDownloadItem.setDownloadState(SKToolsDownloadItem.PAUSED);
        }
        // automatically stop the download thread
        synchronized (SKToolsDownloadPerformer.class) {
            if (queuedDownloads != null) {
                queuedDownloads.clear();
            }
        }
    }

    /**
     * return true if current item is fully downloaded
     */
    private boolean isCurrentItemFullyDownloaded() {
        // check the total read bytes for current download
        long totalBytesRead;
        try {
            RandomAccessFile currentDestinationFile = new RandomAccessFile(currentDownloadItem.getCurrentStepDestinationPath(), "r");
            totalBytesRead = currentDestinationFile.length();
        } catch (final FileNotFoundException e) {
            totalBytesRead = 0;
        } catch (final IOException e) {
            totalBytesRead = 0;
        }
        if (totalBytesRead == currentDownloadItem.getCurrentDownloadStep().getDownloadItemSize()) {
            return true;
        }
        return false;
    }
}