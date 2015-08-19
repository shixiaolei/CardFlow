package com.demo.card;

public class Utils {

    public static int dp2px(int dp) {
        return (int) (App.res().getDisplayMetrics().density * dp + 0.5f);
    }

    public static int px2dp(int dp) {
        return (int) (App.res().getDisplayMetrics().density * dp + 0.5f);
    }

    public static int getScreenWidth() {
        return App.res().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return App.res().getDisplayMetrics().heightPixels;
    }

    public static float constain(float num, float min, float max) {
        return num < min ? min : num > max ? max : num;
    }

    /**
     * 已知一元一次方程经过(x0, y0)、(x1, y1), 求x时的y
     */
    public static float linearValue(float x0, float y0, float x1, float y1, float x) {
        if (x0 != x1) {
            return (x - x0) * (y1 - y0) / (x1 - x0) + y0;
        }
        if (y0 == y1) {
            return y0;
        }
        throw new IllegalArgumentException("x0 == x1, y0 != y1");
    }

}
