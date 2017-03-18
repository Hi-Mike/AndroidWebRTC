package himike.win.androidwebrtc;

import android.hardware.Camera;
import android.util.Log;

/**
 * Created by HiMike on 2017/3/18.
 */

public class CameraUtils {
    public static String getNameOfFrontFacingDevice() {
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();

            try {
                Camera.getCameraInfo(i, info);
                if (info.facing == 1) {
                    return getDeviceName(i);
                }
            } catch (Exception var3) {
                Log.e("VideoCapturerAndroid", "getCameraInfo failed on index " + i, var3);
            }
        }

        return null;
    }

    public static String getNameOfBackFacingDevice() {
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();

            try {
                Camera.getCameraInfo(i, info);
                if (info.facing == 0) {
                    return getDeviceName(i);
                }
            } catch (Exception var3) {
                Log.e("VideoCapturerAndroid", "getCameraInfo failed on index " + i, var3);
            }
        }

        return null;
    }

    public static String getDeviceName(int index) {
        Camera.CameraInfo info = new Camera.CameraInfo();

        try {
            Camera.getCameraInfo(index, info);
        } catch (Exception var3) {
            Log.e("VideoCapturerAndroid", "getCameraInfo failed on index " + index, var3);
            return null;
        }

        String facing = info.facing == 1 ? "front" : "back";
        return "Camera " + index + ", Facing " + facing + ", Orientation " + info.orientation;
    }
}
