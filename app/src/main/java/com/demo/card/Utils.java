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

}
