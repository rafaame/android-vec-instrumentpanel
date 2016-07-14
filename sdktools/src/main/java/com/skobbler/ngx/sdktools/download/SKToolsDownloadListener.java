package com.skobbler.ngx.sdktools.download;

/**
 * Listener for download component
 * Must be implemented by anyone who uses download sdk tools component
 * Created by CatalinM on 11/13/2014.
 */
public interface SKToolsDownloadListener {

    public void onDownloadProgress(SKToolsDownloadItem currentDownloadItem);

    public void onDownloadCancelled(String currentDownloadItemCode);

    public void onDownloadPaused(SKToolsDownloadItem currentDownloadItem);

    public void onInternetConnectionFailed(SKToolsDownloadItem currentDownloadItem, boolean responseReceivedFromServer);

    public void onAllDownloadsCancelled();

    public void onNotEnoughMemoryOnCurrentStorage(SKToolsDownloadItem currentDownloadItem);

    public void onInstallStarted(SKToolsDownloadItem currentInstallingItem);

    public void onInstallFinished(SKToolsDownloadItem currentInstallingItem);
}