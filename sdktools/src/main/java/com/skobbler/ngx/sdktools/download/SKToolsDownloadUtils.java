package com.skobbler.ngx.sdktools.download;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.os.Environment;
import android.os.StatFs;
import com.skobbler.ngx.util.SKLogging;

/**
 * Contains utility methods used for download component
 * Created by CatalinM on 11/20/2014.
 */
public class SKToolsDownloadUtils {

    /**
     * the values used to convert the memory amount to the proper units(KB, MB, GB...)
     */
    public static final long KILO = 1024;

    public static final long MEGA = KILO * KILO;

    public static final long GIGA = MEGA * KILO;

    public static final long TERRA = GIGA * KILO;

    public static final long MINIMUM_FREE_MEMORY = 20 * MEGA;

    /**
     * removes files/folders corresponding to a certain path
     * @param currentLocationPath current location path
     */
    public static void removeCurrentLocationFromDisk(String currentLocationPath) {
        String deleteCmd = "rm -r " + currentLocationPath;
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
            SKLogging.writeLog(SKToolsDownloadPerformer.TAG, "The file was deleted from its current installation folder", SKLogging.LOG_DEBUG);
        } catch (IOException e) {
            SKLogging.writeLog(SKToolsDownloadPerformer.TAG, "The file couldn't be deleted !!!", SKLogging.LOG_DEBUG);
        }
    }

    /**
     * gets the bytes needed to perform a download
     * @param neededBytes number of bytes that should be available, on the device, for performing a download
     * @param path the path where resources will be downloaded
     * @return needed bytes in order to perform the current download, or 0 if there are enough available bytes is -1 if given path is wrong
     */
    public static long getNeededBytesForADownload(long neededBytes, String path) {
        if (path == null) {
            return -1;
        } else {
            if (!isDataAccessible(path)) {
                return -1;
            } else if (path.startsWith("/data")) { // resources are on internal memory
                long availableMemorySize = getAvailableMemorySize(Environment.getDataDirectory().getPath());
                if ((neededBytes + MINIMUM_FREE_MEMORY) <= availableMemorySize) {
                    return 0;
                } else {
                    return (neededBytes + MINIMUM_FREE_MEMORY - availableMemorySize);
                }
            } else { // resources are on other storage
                String memoryPath = null;
                int androidFolderIndex = path.indexOf("/Android");
                if ((androidFolderIndex > 0) && (androidFolderIndex < path.length())) {
                    memoryPath = path.substring(0, androidFolderIndex);
                }
                if (memoryPath == null) {
                    return -1;
                } else {
                    long availableMemorySize = getAvailableMemorySize(memoryPath);
                    if ((neededBytes + MINIMUM_FREE_MEMORY) <= availableMemorySize) {
                        return 0;
                    } else {
                        return (neededBytes + MINIMUM_FREE_MEMORY - availableMemorySize);
                    }
                }
            }
        }
    }

    /**
     * gets the available internal memory size
     * @return available memory size in bytes
     */
    public static long getAvailableMemorySize(String path) {
        StatFs statFs = null;
        try {
            statFs = new StatFs(path);
        } catch (IllegalArgumentException ex) {
            SKLogging.writeLog(SKToolsDownloadPerformer.TAG, "Exception when creating StatF ; message = " + ex, SKLogging.LOG_DEBUG);
        }
        if (statFs != null) {
            Method getAvailableBytesMethod = null;
            try {
                getAvailableBytesMethod = statFs.getClass().getMethod("getAvailableBytes");
            } catch (NoSuchMethodException e) {
                SKLogging.writeLog(SKToolsDownloadPerformer.TAG, "Exception at getAvailableMemorySize method = " + e.getMessage(), SKLogging.LOG_DEBUG);
            }

            if (getAvailableBytesMethod != null) {
                try {
                    SKLogging.writeLog(SKToolsDownloadPerformer.TAG, "Using new API for getAvailableMemorySize method !!!", SKLogging.LOG_DEBUG);
                    return (Long) getAvailableBytesMethod.invoke(statFs);
                } catch (IllegalAccessException e) {
                    return (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
                } catch (InvocationTargetException e) {
                    return (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
                }
            } else {
                return (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
            }
        } else {
            return 0;
        }
    }

    /**
     * checks if data on this path is accessible
     * @param path the path whose availability is checked
     * @return true if the data from the given path is accessible, false otherwise (data erased, SD card removed, etc)
     */
    public static boolean isDataAccessible(String path) {
        // if file is on internal memory, check its existence
        if (path != null) {
            if (path.startsWith("/data")) {
                return new File(path).exists();
            } else {
                String memoryPath = null;
                int androidFolderIndex = path.indexOf("/Android");
                if (androidFolderIndex > 0 && androidFolderIndex < path.length()) {
                    memoryPath = path.substring(0, androidFolderIndex);
                }
                if (memoryPath != null) {
                    boolean check = false;
                    try {
                        FileInputStream fs = new FileInputStream("/proc/mounts");
                        DataInputStream in = new DataInputStream(fs);
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        String strLine;
                        while ((strLine = br.readLine()) != null && !check) {
                            if (strLine.contains(memoryPath)) {
                                check = true;
                            }
                        }
                        br.close();
                    } catch (Exception e) {
                        SKLogging.writeLog(SKToolsDownloadPerformer.TAG, "Exception in isDataAccessible method ; message = " + e.getMessage(), SKLogging.LOG_DEBUG);
                    }
                    return check && (new File(path)).exists();
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }
}