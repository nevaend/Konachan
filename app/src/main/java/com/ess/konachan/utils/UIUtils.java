package com.ess.konachan.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;

/**
 * 获取手机窗口属性
 *
 * @author Zero
 */
public class UIUtils {

    /**
     * 获取屏幕尺寸
     *
     * @param context 上下文
     * @return 整形数组[宽度，高度]
     */
    public static int[] getWindowSize(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        return new int[]{width, height};
    }

    /**
     * 获取状态栏高度
     *
     * @param context 上下文
     * @return 状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0;
        int statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    /**
     * 获取底部导航栏尺寸
     *
     * @param activity  上下文
     * @return  导航栏高度 Point （宽：point.x, 高：point.y）
     */
    public static Point getNavigationBarSize(Activity activity) {
        Point appUsablePoint = getAppUsableScreenSize(activity);
        Point realScreenPoint = getRealScreenSize(activity);

        // navigation bar on the right
        if (appUsablePoint.x < realScreenPoint.x) {
            return new Point(realScreenPoint.x - appUsablePoint.x, appUsablePoint.y);
        }

        // navigation bar at the bottom
        if (appUsablePoint.y < realScreenPoint.y) {
            return new Point(appUsablePoint.x, realScreenPoint.y - appUsablePoint.y);
        }

        // navigation bar is not present
        return new Point();
    }

    /**
     * 获取用户正在使用的屏幕尺寸
     *
     * @param activity  activity
     * @return  屏幕使用尺寸 Point （宽：point.x, 高：point.y）
     */
    public static Point getAppUsableScreenSize(Activity activity) {
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point;
    }

    /**
     * 获取屏幕实际尺寸
     *
     * @param activity  activity
     * @return  屏幕实际尺寸 Point （宽：point.x, 高：point.y）
     */
    public static Point getRealScreenSize(Activity activity) {
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(point);
        } else if (Build.VERSION.SDK_INT >= 16) {
            View decorView = activity.getWindow().getDecorView();
            point.x = decorView.getWidth();
            point.y = decorView.getHeight();
        }
        return point;
    }


    public static int dp2px(Context context, float dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int sp2px(Context context, float sp) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * fontScale + 0.5f);
    }

    public static int px2dp(Context context, float px) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    public static int px2sp(Context context, float px) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (px / fontScale + 0.5f);
    }

    /**
     * 设置背景透明度
     *
     * @param context 上下文
     * @param alpha   透明度, 1.0为完全不透明，0.0为完全透明
     */
    public static void setBackgroundAlpha(Activity context, float alpha) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.alpha = alpha;
        context.getWindow().setAttributes(lp);
    }

    /**
     * 获得软键盘高度
     *
     * @param view 当前view层
     * @return 软键盘的高度，int值
     */
    public static int getSoftInputHeight(View view) {
        Rect r = new Rect();
        view.getWindowVisibleDisplayFrame(r);
        int screenHeight = view.getRootView().getHeight();
        return screenHeight - (r.bottom - r.top);
    }

    /**
     * 关闭软键盘
     *
     * @param context 上下文
     */
    public static void closeSoftInput(Activity context) {
        InputMethodManager imm = (InputMethodManager) context.
                getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isActive() && context.getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(context.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 切换软键盘开关状态
     *
     * @param context 上下文
     */
    public static void switchSoftInput(Context context) {
        InputMethodManager imm = (InputMethodManager) context.
                getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isActive()) {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
