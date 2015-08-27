package com.demo.card;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.ViewPropertyAnimator;

import java.util.Arrays;
import java.util.Random;

public class Utils {

    public static int dip2px(Context context, int dp) {
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


    public static float ofValue(float x0, float y0, float x1, float y1, float x, TimeInterpolator interpolator) {
        if (x0 != x1) {
            float ratio = (x - x0) / (x1 - x0);
            float r = (ratio >= 0 && ratio <= 1) ? interpolator.getInterpolation(ratio) : ratio;
            return r * (y1 - y0) + y0;
        }
        if (y0 == y1) {
            return y0;
        }
        throw new IllegalArgumentException("x0 == x1, y0 != y1");
    }


    /**
     * 把数组的内容和下标互换，例如输入[1, 3, 4, 0, 2], 返回[3, 0, 4, 1, 2]
     */
    public static int[] getReversedArray(int[] input) {
        if (!(isArrayInRange(input))) {
            return null;
        }
        int[] output = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            int index = input[i];
            output[index] = i;
        }
        return output;
    }

    /**
     * 检查一个长度为length的数组，是否元素为0到length-1，且每个数组有且只有一个.
     */
    public static boolean isArrayInRange(int[] input) {
        if (input == null) {
            return false;
        }
        if (input.length == 0) {
            return true;
        }
        int[] counter = new int[input.length];
        Arrays.fill(counter, 0);
        for (int num : input) {
            if (num < 0 || num >= input.length) {
                return false;
            }
            counter[num] = counter[num] + 1;
        }
        for (int num : counter) {
            if (num != 1) {
                return false;
            }
        }
        return true;
    }


    private static final Random sRandom = new Random();

    public static int random(int howsmall, int howbig) {
        if (howsmall >= howbig) return howsmall;
        return (int) (sRandom.nextFloat() * (howbig - howsmall) + howsmall);
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static void setAnimatorEndAction(ViewPropertyAnimator animator, final Runnable endAction) {
        if (animator == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            animator.withEndAction(null != endAction ? new Runnable() {
                @Override
                public void run() {
                    endAction.run();
                }
            } : null);
        } else {
            animator.setListener(null != endAction ? new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endAction.run();
                }
            } : null);
        }
    }

    public static boolean isLandscape() {
        return App.res().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }
}
