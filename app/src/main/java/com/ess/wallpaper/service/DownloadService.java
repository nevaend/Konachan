package com.ess.wallpaper.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.ess.wallpaper.bean.ImageBean;
import com.ess.wallpaper.bean.ThumbBean;
import com.ess.wallpaper.global.Constants;
import com.ess.wallpaper.http.MyProgressListener;
import com.ess.wallpaper.http.OkHttp;
import com.ess.wallpaper.utils.BitmapUtils;
import com.ess.wallpaper.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import me.jessyan.progressmanager.ProgressManager;
import okhttp3.Response;

public class DownloadService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final ArrayList<Runnable> mThreadList = new ArrayList<>();

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                downloadBitmap(intent);
                synchronized (mThreadList) {
                    mThreadList.remove(this);
                    checkToStopService();
                }
            }
        };

        synchronized (mThreadList) {
            mThreadList.add(runnable);
            new Thread(runnable).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void downloadBitmap(Intent intent) {
        if (intent == null) {
            // 下载过程中若关闭app会导致intent为null
            // 此时终止下载并清除所有notification
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            return;
        }

        String url = intent.getStringExtra(Constants.JPEG_URL);
        String bitmapPath = intent.getStringExtra(Constants.BITMAP_PATH);
        ThumbBean thumbBean = intent.getParcelableExtra(Constants.THUMB_BEAN);
        ImageBean imageBean = intent.getParcelableExtra(Constants.IMAGE_BEAN);

        // 绑定下载进度监听器
        MyProgressListener listener;
        if (!OkHttp.getInstance().isUrlInProgressListener(url)) {
            listener = new MyProgressListener(this, imageBean, intent);
            listener.setNotifyThumb(thumbBean.thumbUrl);
            ProgressManager.getInstance().addResponseListener(url, listener);
            OkHttp.getInstance().addUrlToProgressListener(url, listener);
        } else {
            listener = OkHttp.getInstance().getProgressListener(url);
            listener.prepareNotification();
        }

        // 临时下载文件
        File tempFolder = new File(Constants.IMAGE_TEMP);
        String tempName = bitmapPath.substring(bitmapPath.lastIndexOf("/") + 1, bitmapPath.lastIndexOf("."));
        File tempFile = new File(tempFolder, tempName);
        try {
            // 下载
            Response response = OkHttp.getInstance().execute(url);
            if (tempFolder.exists() || tempFolder.mkdirs()) {
                InputStream inputStream = response.body().byteStream();
                FileUtils.streamToFile(inputStream, tempFile);

                // 下载成功，保存为图片
                File folder = new File(Constants.IMAGE_DIR);
                if (folder.exists() || folder.mkdirs()) {
                    File file = new File(bitmapPath);
                    FileUtils.copyFile(tempFile, file);
                    // 添加图片到媒体库（刷新相册）
                    BitmapUtils.insertToMediaStore(this, file);
                    // 通知监听器完成下载 （由于lolibooru监听不到下载进度，所以在这里进行弥补）
                    listener.performFinish();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            ProgressManager.getInstance().notifyOnErorr(url, e);
        } finally {
            OkHttp.getInstance().removeUrlFromDownloadQueue(url);
            tempFile.delete();
        }
    }

    private void checkToStopService() {
        if (mThreadList.isEmpty()) {
            stopSelf();
        }
    }
}
