package com.skobbler.ngx.sdktools.download;

import java.util.List;

/**
 * Describes an item that is downloaded
 * Created by CatalinM on 11/13/2014.
 */
public class SKToolsDownloadItem {

    /**
     * constants for the states of a download resource
     */
    public static final byte NOT_QUEUED = 0;

    public static final byte QUEUED = 1;

    public static final byte DOWNLOADING = 2;

    public static final byte PAUSED = 3;

    public static final byte DOWNLOADED = 4;

    public static final byte INSTALLING = 5;

    public static final byte INSTALLED = 6;

    /**
     * download item code
     */
    private String itemCode;

    /**
     * download item state
     */
    private byte downloadState;

    /**
     * list of download steps for current item
     */
    private List<SKToolsFileDownloadStep> downloadSteps;

    /**
     * current download step index
     */
    private byte currentStepIndex;

    /**
     * number of downloaded bytes
     */
    private long noDownloadedBytes;

    /**
     * number of downloaded bytes in this connection
     */
    private long noDownloadedBytesInThisConnection;

    /**
     * true, if an unzip is needed
     */
    private boolean unzipIsNeeded;

    /**
     * true, if install operation is needed
     */
    private boolean installOperationIsNeeded;

    /**
     * constructs an object of SKToolsDownloadItem type
     * @param itemCode current item code
     * @param downloadSteps current download steps for this item
     * @param downloadState download state
     * @param unzipIsNeeded unzip is needed
     * @param installOperationIsNeeded install operation is needed
     */
    public SKToolsDownloadItem(String itemCode, List<SKToolsFileDownloadStep> downloadSteps, byte downloadState, boolean unzipIsNeeded, boolean installOperationIsNeeded) {
        this.itemCode = itemCode;
        this.downloadSteps = downloadSteps;
        this.downloadState = downloadState;
        this.unzipIsNeeded = unzipIsNeeded;
        this.installOperationIsNeeded = installOperationIsNeeded;
    }

    /**
     * returns if an UNZIP operation is needed
     * @return true if an unzip is needed
     */
    public boolean unzipIsNeeded() {
        return unzipIsNeeded;
    }

    /**
     * sets current download step index
     * @param currentStepIndex current step index
     */
    public void setCurrentStepIndex(byte currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }

    /**
     * gets the index of the current download step object
     * @return current step for this item
     */
    public byte getCurrentStepIndex() {
        return this.currentStepIndex;
    }

    /**
     * gets current download step object
     * @return current download step
     */
    public SKToolsFileDownloadStep getCurrentDownloadStep() {
        if (downloadSteps != null) {
            if (downloadSteps.size() > currentStepIndex) {
                return downloadSteps.get(currentStepIndex);
            }
        }
        return null;
    }

    /**
     * returns if current item is downloaded
     * @return true if current item is downloaded, false otherwise
     */
    public boolean isDownloadFinished() {
        if (downloadSteps != null) {
            if (downloadSteps.size() <= currentStepIndex) {
                return true;
            }
        }
        return false;
    }

    /**
     * go to next download step
     */
    public void goToNextDownloadStep() {
        currentStepIndex++;
    }

    /**
     * sets download state for current item
     * @param downloadState download state for current item
     */
    public void setDownloadState(byte downloadState) {
        this.downloadState = downloadState;
    }

    /**
     * gets the current download item state
     * @return current download state
     */
    public byte getDownloadState() {
        return this.downloadState;
    }

    /**
     * gets the number of downloaded bytes
     * @return no downloaded bytes
     */
    public long getNoDownloadedBytes() {
        return noDownloadedBytes;
    }

    /**
     * sets the number of downloaded bytes
     * @param noDownloadedBytes no downloaded bytes that will be set
     */
    public void setNoDownloadedBytes(long noDownloadedBytes) {
        this.noDownloadedBytes = noDownloadedBytes;
        for (int i = 0; i < currentStepIndex; i++) {
            if ((downloadSteps != null) && (i < downloadSteps.size())) {
                SKToolsFileDownloadStep currentStep = downloadSteps.get(i);
                if (currentStep != null) {
                    this.noDownloadedBytes += currentStep.getDownloadItemSize();
                }
            }
        }
    }

    /**
     * gets current download item code
     * @return current download item code
     */
    public String getItemCode() {
        return itemCode;
    }

    /**
     * marks current item as NOT-QUEUED (e.g. if its download is cancelled from some reason)
     */
    public void markAsNotQueued() {
        // removes already downloaded bytes from current item
        for (int i = 0; i <= currentStepIndex; i++) {
            if ((downloadSteps != null) && (i < downloadSteps.size())) {
                SKToolsFileDownloadStep currentStep = downloadSteps.get(i);
                if (currentStep != null) {
                    SKToolsDownloadUtils.removeCurrentLocationFromDisk(currentStep.getDestinationPath());
                }
            }
        }
        // revert current item state
        noDownloadedBytes = 0;
        noDownloadedBytesInThisConnection = 0;
        downloadState = NOT_QUEUED;
        currentStepIndex = 0;
    }

    /**
     * sets the number of downloaded bytes during current internet connection
     * @param noDownloadedBytesInThisConnection no downloaded bytes that will be set
     */
    public void setNoDownloadedBytesInThisConnection(long noDownloadedBytesInThisConnection) {
        this.noDownloadedBytesInThisConnection = noDownloadedBytesInThisConnection;
    }

    /**
     * gets current download step destination path
     */
    public String getCurrentStepDestinationPath() {
        if ((downloadSteps != null) && (currentStepIndex < downloadSteps.size())) {
            return downloadSteps.get(currentStepIndex).getDestinationPath();
        }
        return null;
    }

    /**
     * returns if install operation is needed
     * @return true if install operation is need, false otherwise
     */
    public boolean isInstallOperationIsNeeded() {
        return installOperationIsNeeded;
    }

    /**
     * @return item size starting with current step (e.g if current step is 0, returns size for all sub-items, otherwise the size starting with current-sub-item)
     */
    public long getRemainingSize() {
        long remainingSize = 0;
        if (downloadSteps != null) {
            for (int i = currentStepIndex; i < downloadSteps.size(); i++) {
                SKToolsFileDownloadStep currentStep = downloadSteps.get(i);
                if (currentStep != null) {
                    remainingSize += currentStep.getDownloadItemSize();
                }
            }
        }
        return remainingSize;
    }

    @Override
    public boolean equals(Object another) {
        if (another == null) {
            return false;
        } else if (!(another instanceof SKToolsDownloadItem)) {
            return false;
        } else {
            SKToolsDownloadItem anotherItem = (SKToolsDownloadItem) another;
            if ((itemCode == null) || (anotherItem.getItemCode() == null)) {
                return false;
            }
            return itemCode.equals(anotherItem.getItemCode());
        }
    }
}