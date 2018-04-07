package vip.wangzs.imagequads.tools;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;

import java.io.File;

/**
 * Created by wangzs on 2018/4/4.
 */

public class FileUtil {

    /**
     * 打开文件选择器
     *
     * @param wndTitle    选择对话框title
     * @param fileType    选择文件的类型，如：file/ *  image/ *
     * @param requestCode 选择后对应onActivityResult中的请求码
     */
    public static void openFileChooser(Activity context, String wndTitle, String fileType, int requestCode) {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(fileType);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        context.startActivityForResult(Intent.createChooser(intent, wndTitle), requestCode);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getWangzsDir() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File wangzsDir = new File(externalStorageDirectory, "wangzs");
        if (wangzsDir.isFile()) {
            wangzsDir.delete();
        }
        if (!wangzsDir.exists()) {
            wangzsDir.mkdirs();
        }
        return wangzsDir;
    }
}
