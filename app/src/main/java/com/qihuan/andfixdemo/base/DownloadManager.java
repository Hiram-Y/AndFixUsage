package com.qihuan.andfixdemo.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.qihuan.andfixdemo.WApplication;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by qihuan on 16/2/19.
 * 处理新版本的更新下载工作 仅在wifi下工作
 */
public class DownloadManager {

    private Context context = WApplication.getContext();
    private BroadcastReceiver receiver;
    /**
     * 取消下载，当环境为非WiFi时变更为true
     */
    private boolean cancel = false;
    /**
     * 网络连接
     */
    private Call call;
    private DownloadCallback callback;


    public DownloadManager() {
        register();
    }

    public static boolean isWifi(Context context) {
        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public DownloadManager enableCallback(DownloadCallback callback) {
        this.callback = callback;
        return this;
    }

    /**
     * 注册监听
     */
    private void register() {
        if (receiver == null) {
            receiver = new NetworkChangedBroadcastReceiver();
        }
        try {
            context.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * 取消注册监听
     */
    private void unRegister() {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * 开始下载
     *
     * @param storePath
     * @param downloadUrl
     */
    public void startDownload(final String storePath, final String downloadUrl) {
        //更新网络状况
        updateStatus(context);
        LogUtil.d("判断wifi条件" + cancel);
        if (cancel) {
            //当前非WIFI网络环境，不下载
            return;
        }

        LogUtil.d("启动线程");
        //进行下载
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).get().build();
        call = okHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                LogUtil.d("开始下载");
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    File file = new File(storePath + ".tmp");
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    file.renameTo(new File(storePath));

                    //下载成功
                    if (null != callback) {
                        callback.success();
                    }
                } catch (Exception e) {
                    LogUtil.d("取消下载 " + e.getMessage());
                } finally {
                    FileUtil.StreamUtil.close(is);
                    FileUtil.StreamUtil.close(fos);
                }

                LogUtil.d("结束下载");
            }
        });

    }

    private void updateStatus(Context context) {
        boolean isWifi = isWifi(context);
        //如果切换到了非wifi环境，则停止下载
        if (!isWifi) {
            stop();
        }
    }

    /**
     * 立即停止（通过切断网络连接的方式结束线程）
     */
    public void stop() {
        LogUtil.d("stop 下载");
        cancel = true;
        if (null != call && !call.isCanceled()) {
            call.cancel();
            call = null;
        }
    }

    /**
     * 下载回调，一般不需要处理失败的情况
     */
    public static interface DownloadCallback {
        void success();
    }

    /**
     * 监听网络情况的变化
     */
    private class NetworkChangedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateStatus(context);
            }
        }
    }
}
