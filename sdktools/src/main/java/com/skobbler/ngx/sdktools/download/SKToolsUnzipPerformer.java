package com.skobbler.ngx.sdktools.download;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import android.util.Log;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.packages.SKPackageManager;
import com.skobbler.ngx.util.SKLogging;


/**
 * perform unzip for queued items
 * Created by CatalinM on 11/26/2014.
 */
public class SKToolsUnzipPerformer extends Thread {

    /**
     * the tag associated with this class, used for debugging
     */
    private static final String TAG = "SKToolsUnzipPerformer";

    /**
     * queued installing items
     */
    private Queue<SKToolsDownloadItem> queuedInstallingItems;

    /**
     * current installing item
     */
    private SKToolsDownloadItem currentInstallingItem;

    /**
     * download listener
     */
    private SKToolsDownloadListener downloadListener;

    /**
     * tells that current install process is paused
     */
    private volatile boolean isInstallProcessPaused;

    /**
     * creates an object of SKToolsUnzipPerformer type
     * @param downloadListener download listener
     */
    public SKToolsUnzipPerformer(SKToolsDownloadListener downloadListener) {
        synchronized (SKToolsUnzipPerformer.class) {
            this.queuedInstallingItems = new LinkedList<SKToolsDownloadItem>();
        }
        this.downloadListener = downloadListener;
    }

    public void setDownloadListener(SKToolsDownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    /**
     * installs a list of DOWNLOADED resources
     */
    @Override
    public void run() {
        while (existsAnyRemainingInstall()) {
            if ((currentInstallingItem == null) || (queuedInstallingItems == null) || isInstallProcessPaused) {
                break;
            }
            String filePath = currentInstallingItem.getCurrentStepDestinationPath();
            SKLogging.writeLog(TAG, "The path of the file that must be installed = " + filePath, SKLogging.LOG_DEBUG);
            boolean zipFileExists = false;
            File zipFile = null;
            String rootFilePath = null;
            if (filePath != null) {
                zipFile = new File(filePath);
                zipFileExists = zipFile.exists();
                rootFilePath = filePath.substring(0, filePath.indexOf(new StringBuilder(currentInstallingItem.getItemCode()).append(SKToolsDownloadManager.POINT_EXTENSION)
                        .toString()));
            }
            if (zipFileExists) {
                // change the state for current download item
                currentInstallingItem.setDownloadState(SKToolsDownloadItem.INSTALLING);

                // database and UI update
                if (downloadListener != null) {
                    downloadListener.onInstallStarted(currentInstallingItem);
                }

                SKLogging.writeLog(TAG, "Start unzipping file with path = " + filePath, SKLogging.LOG_DEBUG);
                SKMaps.getInstance().unzipFile(zipFile.getAbsolutePath(), rootFilePath);
                SKLogging.writeLog(TAG, "Unzip finished. Start installing current resource (performed by NG library)", SKLogging.LOG_DEBUG);

                if (isInstallProcessPaused) {
                    SKLogging.writeLog(TAG, "Install was not finalized, because install process was stopped by client", SKLogging.LOG_DEBUG);
                    break;
                }

                if (currentInstallingItem.isInstallOperationIsNeeded()) {
                    int result = SKPackageManager.getInstance().addOfflinePackage(rootFilePath, currentInstallingItem.getItemCode());
                    SKLogging.writeLog(TAG, "Current resource installing result code = " + result, SKLogging.LOG_DEBUG);
                    if ((result & SKPackageManager.ADD_PACKAGE_MISSING_SKM_RESULT & SKPackageManager.ADD_PACKAGE_MISSING_NGI_RESULT & SKPackageManager
                            .ADD_PACKAGE_MISSING_NGI_DAT_RESULT) == 0) {
                        // current install was performed with success set current resource as already download
                        currentInstallingItem.setDownloadState(SKToolsDownloadItem.INSTALLED);
                        SKLogging.writeLog(TAG, "The " + currentInstallingItem.getItemCode() + " resource was successfully downloaded and installed by our NG component.",
                                SKLogging.LOG_DEBUG);
                        // notify the UI that current resource was installed
                        if (downloadListener != null) {
                            downloadListener.onInstallFinished(currentInstallingItem);
                        }
                    } else {
                        // current install was performed with error => set current resource as NOT_QUEUED, remove downloaded bytes etc
                        currentInstallingItem.markAsNotQueued();
                        SKLogging.writeLog(TAG, "The " + currentInstallingItem.getItemCode() + " resource couldn't be installed by our NG component,although it was downloaded.",
                                SKLogging.LOG_DEBUG);
                        // notify the UI that current resource was not installed
                        if (downloadListener != null) {
                            downloadListener.onDownloadProgress(currentInstallingItem);
                        }
                    }
                } else {
                    // current install was performed with success set current resource as already download
                    currentInstallingItem.setDownloadState(SKToolsDownloadItem.INSTALLED);
                    SKLogging.writeLog(TAG, "The " + currentInstallingItem.getItemCode() + " resource was successfully downloaded and installed by our NG component.",
                            SKLogging.LOG_DEBUG);
                    // notify the UI that current resource was installed
                    if (downloadListener != null) {
                        downloadListener.onInstallFinished(currentInstallingItem);
                    }
                }
                // remove current ZIP file from device
                SKToolsDownloadUtils.removeCurrentLocationFromDisk(filePath);
            } else {
                SKLogging.writeLog(TAG, "The zip file doesn't exist => download again the resource !!! " + filePath, Log.DEBUG);
                // prepare again current resource for download queue(change its state, remove all related downloaded bytes)
                currentInstallingItem.markAsNotQueued();
                currentInstallingItem.setDownloadState(SKToolsDownloadItem.QUEUED);

                // notify the UI that current resource is again put in download queue
                if (downloadListener != null) {
                    downloadListener.onDownloadProgress(currentInstallingItem);
                }

                // add again the resource in download queue
                List<SKToolsDownloadItem> downloadItems = new ArrayList<SKToolsDownloadItem>();
                downloadItems.add(currentInstallingItem);
                SKToolsDownloadManager.getInstance(downloadListener).startDownload(downloadItems);
            }
            // remove current download from download queue
            synchronized (SKToolsUnzipPerformer.class) {
                if (queuedInstallingItems != null) {
                    queuedInstallingItems.poll();
                }
            }
        }
        SKLogging.writeLog(TAG, "The install thread has stopped", SKLogging.LOG_DEBUG);
    }

    /**
     * adds downloaded item for install (in the install queue)
     * @param currentItem current item
     */
    public void addItemForInstall(SKToolsDownloadItem currentItem) {
        this.queuedInstallingItems.add(currentItem);
    }

    /**
     * stops the install process
     */
    public void stopInstallProcess() {
        isInstallProcessPaused = true;
    }

    /**
     * checks if there is any remaining item to install
     * @return true, if there is any remaining item to install, false otherwise
     */
    private boolean existsAnyRemainingInstall() {
        synchronized (SKToolsUnzipPerformer.class) {
            if ((queuedInstallingItems != null) && !queuedInstallingItems.isEmpty()) {
                currentInstallingItem = queuedInstallingItems.peek();
                if (currentInstallingItem != null) {
                    return true;
                }
            }
            return false;
        }
    }
}