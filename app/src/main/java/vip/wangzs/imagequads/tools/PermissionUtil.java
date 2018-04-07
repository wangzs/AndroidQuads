package vip.wangzs.imagequads.tools;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by wangzs on 2018/4/4.
 */

public class PermissionUtil {
    public static void verifyPermissions(Activity activity,
                                         String checkPermission,
                                         String[] requestPermissions,
                                         int requestCode) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity, checkPermission);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, requestPermissions, requestCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPermission(Context context, String checkPermission) {
        try {
            int permission = ActivityCompat.checkSelfPermission(context, checkPermission);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
