package com.qihuan.andfixdemo.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;

import com.alipay.euler.andfix.patch.PatchManager;
import com.qihuan.andfixdemo.WApplication;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by qihuan on 16/6/3.
 */
public class ApkUtil {
    private static String APATCH_PATH = FileUtil.FILE_CACHE_PATH + "/out";

    static {
        if (!FileUtil.exist(APATCH_PATH)) {
            new File(APATCH_PATH).mkdirs();
        }
    }

    /**
     * 判断是否有新版本的apatch需要下载
     */
    public static void checkApatchUpdate(String versionCode) {
        //TODO 请求服务器下发配置文件,这里用一个临时的
        final ApathConfig apatchConfig = new ApathConfig();
        apatchConfig.setApatchVersion("1.3");
        apatchConfig.setApatchUrl("http://10.209.77.195:8000/out.patch-0bf76538a761602ba1b6e0f3e542c5e3.apatch");
        apatchConfig.setApatchName("out.patch-0bf76538a761602ba1b6e0f3e542c5e3.apatch");
        apatchConfig.setApatchMd5("4c96328a7c1dc5aa5029224085a2b9e");

        if (StringUtil.isAnyEmpty(apatchConfig.getApatchVersion(), apatchConfig.getApatchMd5(), apatchConfig.getApatchName(), apatchConfig.getApatchUrl())) {
            //有内容为空
            LogUtil.d("有内容为空");
            return;
        }

        //首先下载apatch
        if (!versionCode.equals(apatchConfig.getApatchVersion())) {
            //apatch版本不匹配
            LogUtil.d("apatch版本不匹配");
            return;
        }
        final String apatchPath = APATCH_PATH + "/" + apatchConfig.getApatchName();

        //判断是否已经下载过
        if (FileUtil.exist(apatchPath)) {
            LogUtil.d("已经下载过");
            return;
        }

        new DownloadManager().enableCallback(new DownloadManager.DownloadCallback() {
            @Override
            public void success() {
                //下载成功之后，校验md5
                File file = new File(apatchPath);
                if (!apatchConfig.getApatchMd5().equals(getFileMD5(file))) {
                    LogUtil.d("md5校验失败,删除文件 " + getFileMD5(file));
                    file.delete();
                    return;
                }

                //下次启动之后，如果签名校验成功，则插件生效
                LogUtil.d("下载完");
            }
        }).startDownload(apatchPath, apatchConfig.getApatchUrl());
    }

    /**
     * 初始化插件化的处理
     */
    public static void initPatch(Context context, String versionCode) {
        PatchManager mPatchManager = new PatchManager(context);
        mPatchManager.init(versionCode);//current version

        //获取apk签名
        String publicSignCode = "";
        try {
            publicSignCode = getLocalSignature(context);
        } catch (Throwable e) {
        }

        // load patch
        mPatchManager.loadPatch();
        LogUtil.d("apatch loaded.");

        // add patch at runtime
        try {
            // .apatch file path
            for (File file : new File(APATCH_PATH).listFiles()) {
                if (file.getName().endsWith(".apatch")) {
                    //找到apatch文件，对其进行签名校验
                    if (!checkCertificates(file.getPath(), publicSignCode)) {
                        //签名校验失败，删除这个补丁文件
                        file.delete();
                        continue;
                    }
                    mPatchManager.addPatch(file.getPath());
                    LogUtil.d("apatch:" + file.getPath() + " added.");
                    file.delete();
                }
            }
        } catch (IOException e) {
            LogUtil.d(e);
        }
    }

    /**
     * get file md5
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static String getFileMD5(File file) {
        if (null == file || !file.isFile()) {
            return null;
        }
        FileInputStream in = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte buffer[] = new byte[1024];
            in = new FileInputStream(file);
            int len;
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }

            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16);
        } catch (Throwable e) {
            return null;
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 得到当前apk包的签名 publicKey中的signCode
     */
    private static String getLocalSignature(Context ctx) throws IOException,
            PackageManager.NameNotFoundException, CertificateException {
        //get signature info depends on package name
        PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
                ctx.getPackageName(), PackageManager.GET_SIGNATURES);
        Signature sign = packageInfo.signatures[0];
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory
                .generateCertificate(new ByteArrayInputStream(sign.toByteArray()));
        String pubKey = cert.getPublicKey().toString();

        String signCode = pubKey.substring(pubKey.indexOf("modulus") + 8,
                pubKey.indexOf(",", pubKey.indexOf("modulus")));

        return signCode;
    }

    /**
     * 读取压缩包内文件，获取其对应的签名
     *
     * @param jarFile
     * @param je
     * @return
     * @throws IOException
     */
    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je) throws IOException {
        // We must read the stream for the JarEntry to retrieve
        // its certificates.
        InputStream is = new BufferedInputStream(jarFile.getInputStream(je));
        byte[] readBuffer = new byte[1024];
        while (is.read(readBuffer, 0, readBuffer.length) != -1) {
            // not using
        }
        is.close();
        return je != null ? je.getCertificates() : null;
    }

    /**
     * 对压缩包文件进行签名校验
     *
     * @param archivePath apatch文件
     * @param apkSignCode apk中得到的签名code
     * @return
     */
    private static boolean checkCertificates(String archivePath, String apkSignCode) {
        try {
            JarFile jarFile = new JarFile(archivePath);

            //记录所有需要进行校验的压缩包文件
            final List<JarEntry> toVerify = new ArrayList<>();
            //verify all thing
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                if (entry.getName().startsWith("META-INF/")) continue;

                //需要校验
                toVerify.add(entry);
            }

            //首先校验压缩包内部文件的签名是否一致
            String mPublicKey = null;

            for (JarEntry jarEntry : toVerify) {
                Certificate[] certs = loadCertificates(jarFile, jarEntry);
                String publicKey = certs[0].getPublicKey().toString();
                if (null == mPublicKey) {
                    mPublicKey = publicKey;
                } else {
                    if (!mPublicKey.equals(publicKey)) {
                        //apatch包中有签名不同的文件，校验失败
                        return false;
                    }
                }
            }

            if (TextUtils.isEmpty(apkSignCode)) {
                //获取apk签名失败，认为只要jar包签名一致即可通过校验
                return true;
            }

            String jarSingCode = mPublicKey.substring(mPublicKey.indexOf("modulus") + 8,
                    mPublicKey.indexOf(",", mPublicKey.indexOf("modulus")));

            //将压缩包的签名文件和apk的签名文件进行对比，校验是否一致
            return apkSignCode.equals(jarSingCode);
        } catch (IOException e) {
            return false;
        }

    }

    public static void cleanApatch() {
        PatchManager mPatchManager = new PatchManager(WApplication.getContext());
        mPatchManager.removeAllPatch();
    }
}
