package com.skobbler.ngx.sdktools.download;

/**
 * Created by Tudor on 11/13/2014.
 * Defines a download step for an item (e.g for maps download, there are three steps: SKM, ZIP, TXG, fo)
 */
public class SKToolsFileDownloadStep {

    /**
     * download URL
     */
    private String downloadURL;

    /**
     * destination path
     */
    private String destinationPath;

    /**
     * download item size
     */
    private long downloadItemSize;

    /**
     * creates an object of SKToolsFileDownloadStep type
     * @param downloadURL download URL
     * @param destinationPath destination path
     * @param downloadItemSize download item size
     */
    public SKToolsFileDownloadStep(String downloadURL, String destinationPath, long downloadItemSize) {
        this.downloadURL = downloadURL;
        this.destinationPath = destinationPath;
        this.downloadItemSize = downloadItemSize;
    }

    /**
     * gets current download URL
     * @return download URL
     */
    public String getDownloadURL() {
        return downloadURL;
    }

    /**
     * gets current destination path
     * @return destination path
     */
    public String getDestinationPath() {
        return destinationPath;
    }

    /**
     * gets current item size
     * @return download item size
     */
    public long getDownloadItemSize() {
        return downloadItemSize;
    }
}