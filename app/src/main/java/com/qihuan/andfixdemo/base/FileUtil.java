package com.qihuan.andfixdemo.base;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by zhoukai on 13-12-11.
 * Util to save delete file and caculate file size
 */
public class FileUtil {
    public static final String SD_PATH = Environment.getExternalStorageDirectory().getPath();
    public static final String FILE_CACHE_PATH = SD_PATH + "/AndFixDemo";
    public static final String UTF_8 = "UTF-8";
    public static final String GBK = "GBK";
    public static final String ISO_8859_1 = "ISO-8859-1";

    static {
        if (!new File(FileUtil.FILE_CACHE_PATH).exists()) {
            new File(FileUtil.FILE_CACHE_PATH).mkdirs();
        }
    }

    /**
     * 用当前时间给取得的图片命名
     */
    public static String generateFileNameByTime() {
        Date date = new Date(System.currentTimeMillis());
        return date.getTime() + ".jpg";
    }

    /*
     * 获取文件或文件夹大小
     * @param path file path
     */
    public static long getFileSize(File path, FileFilter filter) {
        if (null == path || !path.exists()) {
            return 0L;
        }
        if (path.isFile()) {
            return path.length();
        }
        //文件夹
        File[] subFiles = (null == filter) ? path.listFiles() : path.listFiles(filter);
        long sum = 0;
        if (null != subFiles){
            for (File subFile : subFiles) {
                sum += getFileSize(subFile, filter);
            }
        }
        return sum;
    }

    /**
     * 删除文件或文件夹
     *
     * @param file
     */
    public static void deleteFile(File file) {
        if (null == file || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }

        //文件夹递归删除
        File[] files = file.listFiles();
        if (null == files){
            return;
        }
        for (File subFile : files) {
            deleteFile(subFile);
        }
        file.delete();
    }

    /**
     * 删除文件或文件夹
     *
     * @param filePath
     */
    public static void deleteFile(String filePath) {
        deleteFile(new File(filePath));
    }

    /**
     * 返回路径下所有的文件,可以指定文件名过滤规则
     */
    public static List<String> getFilePathsByFile(File file, FilenameFilter filter) {
        if (!file.exists()) {
            return new ArrayList<String>();
        }
        if (file.isFile()) {
            return Arrays.asList(new String[]{file.getPath()});
        }
        if (null == filter) {
            return Arrays.asList(file.list());
        }
        return Arrays.asList(file.list(filter));
    }

    /**
     * 返回路径下所有的文件
     */
    public static List<String> getFilePathsByFile(File file) {
        return getFilePathsByFile(file, null);
    }

    /**
     * 返回路径下所有的文件
     */
    public static List<String> getFilePathsByFile(String filePath) {
        return getFilePathsByFile(new File(filePath));
    }

    /**
     * 从流中读取内容
     *
     * @param inputStream
     * @return
     */
    public static String readString(InputStream inputStream, String encoding) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, encoding));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtil.close(br);
        }
        return null;
    }

    /**
     * 获取文件内容
     *
     * @param path
     * @return
     */
    public static String readString(String path) {
        return readString(new File(path));
    }

    /**
     * 获取文件内容
     *
     * @param file
     * @return
     */
    public static String readString(File file) {
        if (null == file || !file.exists()) {
            return "";
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            StringBuilder stringBuffer = new StringBuilder((int) file.length());
            String temp = null;
            while ((temp = br.readLine()) != null) {
                if (stringBuffer.length() != 0) {
                    stringBuffer.append('\n');
                }
                stringBuffer.append(temp);
            }
            return stringBuffer.toString();
        } catch (IOException e) {
        } finally {
            StreamUtil.close(br);
        }

        return "";
    }

    /**
     * 写字符串到文件
     *
     * @param dest
     * @param data
     */
    public static void writeString(String dest, String data) throws IOException {
        writeString(new File(dest), data);
    }

    /**
     * 写字符串到文件
     *
     * @param dest
     * @param data
     */
    public static void writeString(File dest, String data) throws IOException {
        outString(dest, data, null, false);
    }

    /**
     * 添加字符串到文件
     *
     * @param dest
     * @param data
     */
    public static void appendString(File dest, String data) throws IOException {
        outString(dest, data, null, true);
    }

    /**
     * 输出到文件
     *
     * @param dest     文件
     * @param data     数据
     * @param encoding 编码
     * @param append   写文件方式，true 是增量模式
     * @throws IOException
     */
    protected static void outString(File dest, String data, String encoding, boolean append) throws IOException {
        if (null == dest || (dest.exists() && dest.isDirectory())) {
            throw new IOException("复制出错，请检查参数合法性");
        }
        FileOutputStream out = null;
        try {
            if (!append) {
                //中间通过一个临时文件进行写入，防止进程结束导致的源文件异常
                File tempFile = new File(dest.getPath() + ".tmp");
                out = new FileOutputStream(tempFile);
                out.write(null == encoding ? data.getBytes() : data.getBytes(encoding));
                tempFile.renameTo(dest);
            } else {
                out = new FileOutputStream(dest, true);
                out.write(null == encoding ? data.getBytes() : data.getBytes(encoding));
            }
        } finally {
            StreamUtil.close(out);
        }
    }

    /*
     * check file if not exists  mk a file
     */
    public static void checkFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static boolean exist(String path) {
        if (StringUtil.isEmpty(path)) {
            return false;
        }
        return new File(path).exists();
    }

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> if card is mounted. Else - Android defines cache directory on
     * device's file system.
     *
     * @param context Application context
     * @return Cache {@link File directory}
     */
    public static File getCacheDirectory(Context context) {
        File appCacheDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
//            Log.w("Can't define system cache directory!");
            appCacheDir = context.getCacheDir(); // retry
        }
        return appCacheDir;
    }

    private static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
//                L.w("Unable to create external cache directory");
                return null;
            }
            try {
                new File(appCacheDir, ".nomedia").createNewFile();
            } catch (IOException e) {
//                L.i("Can't create \".nomedia\" file in application external cache directory");
            }
        }
        return appCacheDir;
    }

    public static String FormetFileSize(long fileS) {// 转换文件大小
        if (fileS == 0) {//2013-11-22 传入0的时候直接返回，
            return "0.00B";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "G";
        }
        return fileSizeString;
    }

    /**
     * 获取文件数，不包括文件夹
     *
     * @param f
     * @return
     */
    public static long getFileCount(File f) {// 递归求取目录文件个数
        long size = 0;
        File fileList[] = f.listFiles();
        if (null == fileList){
            return 0;
        }
        size = fileList.length;
        for (File file : fileList) {
            if (file.isDirectory()) {
                size = size + getFileCount(file);
                size--;
            }
        }
        return size;
    }

    /**
     * 复制文件
     *
     * @param src
     * @param dest
     */
    public static void copyFile(File src, File dest) throws IOException {
        if (null == src || null == dest) {
            return;
        }
        if (!src.exists() || (dest.exists() && dest.isDirectory())) {
            throw new IOException("复制出错，请检查参数合法性");
        }
        // do copy file
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(src);
            output = new FileOutputStream(dest);
            StreamUtil.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtil.close(output);
            StreamUtil.close(input);
        }
    }

    /**
     * 复制文件到目录
     *
     * @param src
     * @param destDir
     */
    public static File copyFileToDir(File src, File destDir) throws IOException {
        if (null == src || null == destDir) {
            return null;
        }
        if (destDir.exists() && !destDir.isDirectory()) {
            throw new IOException("复制出错，请检查参数合法性");
        }
        File dest = new File(destDir, src.getName());
        copyFile(src, dest);
        return dest;
    }

    /**
     * 生成一个随机的文件名
     *
     * @return
     */
    public static String generateFileName() {
        return UUID.randomUUID().toString() + ".jpg";
    }


    /**
     * 操作stream的工具类
     */
    public static class StreamUtil {
        private static int ioBufferSize = 1024;

        public static void close(InputStream in) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioex) {
                    // ignore
                }
            }
        }

        public static void close(OutputStream out) {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException ioex) {
                    // ignore
                }
                try {
                    out.close();
                } catch (IOException ioex) {
                    // ignore
                }
            }
        }

        public static void close(Reader in) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioex) {
                    // ignore
                }
            }
        }

        public static void close(Writer out) {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException ioex) {
                    // ignore
                }
                try {
                    out.close();
                } catch (IOException ioex) {
                    // ignore
                }
            }
        }

        public static int copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[ioBufferSize];
            int count = 0;
            int read;
            while (true) {
                read = input.read(buffer, 0, ioBufferSize);
                if (read == -1) {
                    break;
                }
                output.write(buffer, 0, read);
                count += read;
            }
            return count;
        }

        public static int copy(Reader input, Writer output) throws IOException {
            char[] buffer = new char[ioBufferSize];
            int count = 0;
            int read;
            while ((read = input.read(buffer, 0, ioBufferSize)) >= 0) {
                output.write(buffer, 0, read);
                count += read;
            }
            output.flush();
            return count;
        }
    }

    /**
     * ZIP压缩工具
     * add by qihuan 2013-12-02
     *
     * @author 梁栋
     * @since 1.0
     */
    public static class ZipUtil {

        public static final String EXT = ".zip";
        private static final String BASE_DIR = "";
        private static final String PATH = File.separator;
        private static final int BUFFER = 1024;

        /**
         * 文件 解压缩
         *
         * @param srcPath 源文件路径
         * @throws Exception
         */
        public static void decompress(String srcPath) throws Exception {
            File srcFile = new File(srcPath);

            decompress(srcFile);
        }

        /**
         * 解压缩
         *
         * @param srcFile
         * @throws Exception
         */
        public static void decompress(File srcFile) throws Exception {
            String basePath = srcFile.getParent();
            decompress(srcFile, basePath);
        }

        /**
         * 解压缩
         *
         * @param srcFile
         * @param destFile
         * @throws Exception
         */
        public static void decompress(File srcFile, File destFile) throws Exception {
            decompress(new FileInputStream(srcFile), destFile);
        }

        /**
         * 解压缩
         *
         * @param srcInputStream
         * @param destFile
         * @throws Exception
         */
        public static void decompress(InputStream srcInputStream, File destFile) throws Exception {

            CheckedInputStream cis = new CheckedInputStream(srcInputStream, new CRC32());

            ZipInputStream zis = new ZipInputStream(cis);

            decompress(destFile, zis);

            zis.close();

        }

        /**
         * 解压缩
         *
         * @param srcFile
         * @param destPath
         * @throws Exception
         */
        public static void decompress(File srcFile, String destPath)
                throws Exception {
            decompress(srcFile, new File(destPath));

        }

        /**
         * 文件 解压缩
         *
         * @param srcPath  源文件路径
         * @param destPath 目标文件路径
         * @throws Exception
         */
        public static void decompress(String srcPath, String destPath)
                throws Exception {

            File srcFile = new File(srcPath);
            decompress(srcFile, destPath);
        }

        /**
         * 文件 解压缩
         *
         * @param destFile 目标文件
         * @param zis      ZipInputStream
         * @throws Exception
         */
        private static void decompress(File destFile, ZipInputStream zis)
                throws Exception {

            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {

                // 文件
                String dir = destFile.getPath() + File.separator + entry.getName();

                File dirFile = new File(dir);

                // 文件检查
                fileProber(dirFile);

                if (entry.isDirectory()) {
                    dirFile.mkdirs();
                } else {
                    decompressFile(dirFile, zis);
                }

                zis.closeEntry();
            }
        }

        /**
         * 解压zip到指定目录
         *
         * @param destFile
         * @param srcFile
         * @throws Exception
         */
        public static void decompressFilesToDest(File srcFile, File destFile)
                throws Exception {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(srcFile));
            if (!destFile.exists()) {
                destFile.mkdirs();
            }
            if (destFile.isFile()) {
                throw new IllegalArgumentException("destFile must be directory");
            }

            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                // 文件检查
                fileProber(destFile);
                String entryName = entry.getName();
                int index = entryName.lastIndexOf("/");
                if (index > 0) {
                    entryName = entryName.substring(index + 1);
                }
                if (!entry.isDirectory() && !entryName.startsWith(".")) {
                    // 文件
                    File copyFile = new File(destFile, entryName);
                    decompressFile(copyFile, zis);
                }
                zis.closeEntry();
            }
            zis.close();
        }

        /**
         * 文件探针
         * <p/>
         * <p/>
         * 当父目录不存在时，创建目录！
         *
         * @param dirFile
         */
        private static void fileProber(File dirFile) {

            File parentFile = dirFile.getParentFile();
            if (!parentFile.exists()) {

                // 递归寻找上级目录
                fileProber(parentFile);

                parentFile.mkdir();
            }

        }

        /**
         * 文件解压缩
         *
         * @param destFile 目标文件
         * @param zis      ZipInputStream
         * @throws Exception
         */
        private static void decompressFile(File destFile, ZipInputStream zis)
                throws Exception {

            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(destFile));

            int count;
            byte data[] = new byte[BUFFER];
            while ((count = zis.read(data, 0, BUFFER)) != -1) {
                bos.write(data, 0, count);
            }

            bos.close();
        }

    }
}

