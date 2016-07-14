package com.skobbler.ngx.sdktools.navigationui;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.navigation.SKAdvisorSettings;


/**
 * The purpose of this class is to play an advice. An advice basically consists
 * of a series of sound files that combined, represent the advice that should be
 * played to the user.
 */

public class SKToolsAdvicePlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "SKToolsAdvicePlayer";

    // constants for advice priority - user requested advices have the highest,
    // speed warnings the lowest
    public static final int PRIORITY_USER = 0;

    public static final int PRIORITY_NAVIGATION = 1;

    public static final int PRIORITY_SPEED_WARNING = 2;

    /**
     * The singleton instance of the advice player.
     */
    private static SKToolsAdvicePlayer instance;

    /**
     * The single player.
     */
    private MediaPlayer player;

    /**
     * The temporary file for storing the current advice
     */
    private String tempAdviceFile = null;

    /**
     * Queued advice that will be played after the player finishes playing the
     * current advice.
     */
    private String[] nextAdvice;

    /**
     * The priority of the queued advice.
     */
    private int nextAdvicePriority;

    /**
     * Indicates if the user has chosen to mute the advices.
     */
    private boolean isMuted;

    /**
     * Indicates whether the player is busy playing an advice.
     */
    private boolean isBusy;

    public static SKToolsAdvicePlayer getInstance() {
        if (instance == null) {
            instance = new SKToolsAdvicePlayer();
        }
        return instance;
    }

    private SKToolsAdvicePlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    /**
     * method that retrieves the current volume level of the device audio
     * manager with stream type STREAM_MUSIC
     * @param activity
     * @return
     */
    public static int getCurrentDeviceVolume(Activity activity) {
        final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * method that retrieves the maximum volume level of the device audio
     * manager with the stream type STREAM_MUSIC
     * @param activity - the current activity
     * @return
     */
    public static int getMaximAudioLevel(Activity activity) {
        final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public void enableMute() {
        isMuted = true;
    }

    public void disableMute() {
        isMuted = false;
    }

    public boolean isMuted() {
        return isMuted;
    }

    /**
     * Plays an advice. The individual sound files to play are contained in an
     * array list.
     * @param adviceParts an array list of sound file names
     */
    public void playAdvice(String[] adviceParts, int priority) {
        if (isMuted || adviceParts == null) {
            return;
        }

        if (isBusy) {
            if (nextAdvice == null || (priority <= nextAdvicePriority)) {
                nextAdvice = adviceParts;
                nextAdvicePriority = priority;
            }
            return;
        }

        SKAdvisorSettings advisorSettings = SKMaps.getInstance().getMapInitSettings().getAdvisorSettings();
        String soundFilesDirPath = advisorSettings.getResourcePath() + advisorSettings.getLanguage().getValue() + "/sound_files/";

        tempAdviceFile = soundFilesDirPath + "temp.mp3";
        boolean validTokensFound = false;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < adviceParts.length; i++) {
            String soundFilePath = soundFilesDirPath + adviceParts[i] + ".mp3";
            try {
                InputStream is = new FileInputStream(new File(soundFilePath));
                int availableBytes = is.available();
                byte[] tmp = new byte[availableBytes];
                is.read(tmp, 0, availableBytes);
                if (stream != null) {
                    stream.write(tmp);
                }
                is.close();
                validTokensFound = true;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        if (validTokensFound) {
            // valid tokens were found - set busy state until finishing to play
            // advice
            isBusy = true;
        } else {
            // valid tokens not found - return without playing anything
            return;
        }

        writeFile(stream.toByteArray(), tempAdviceFile);
        playFile(tempAdviceFile);
    }

    public void reset() {
        Log.w(TAG, "Entering reset");
        if (player != null) {
            try {
                player.reset();
                deleteTempFile();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        isBusy = false;
    }

    /**
     * Deletes the temporary file stored at "tempAdviceFile" path
     */
    private void deleteTempFile() {
        File fc = new File(tempAdviceFile);
        if (fc.exists()) {
            fc.delete();
        }
    }

    /**
     * Stops playing the current advice
     */
    public void stop() {
        isBusy = false;
        player.stop();
    }

    /**
     * Writes "data" to the "filePath" path on the disk
     * @param data
     * @param filePath
     */
    private void writeFile(byte[] data, String filePath) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(filePath));
            out.write(data);
            try {
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Plays an .mp3 file which should be found at filePath
     * @param filePath
     */
    private void playFile(String filePath) {
        try {
            player.reset();
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            try {
                player.setDataSource(fileDescriptor);
            } catch (IllegalStateException ile) {
                player.reset();
                player.setDataSource(fileDescriptor);
            }
            fileInputStream.close();

            player.prepare();
            player.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        reset();
        if (nextAdvice != null) {
            String[] adviceToPlay = nextAdvice;
            nextAdvice = null;
            playAdvice(adviceToPlay, nextAdvicePriority);
            return;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true; //error was handled
    }
}
