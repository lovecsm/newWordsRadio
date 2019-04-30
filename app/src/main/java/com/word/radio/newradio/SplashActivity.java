package com.word.radio.newradio;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.word.radio.utils.LogUtils;

import cn.waps.AppConnect;
import cn.waps.AppListener;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            //下面图1
            // lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            // 下面图2
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            //下面图3
            // lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            getWindow().setAttributes(lp);
        }

        Context mContext = this;
        // 初始化统计器
        AppConnect.getInstance(mContext);
        // 初始化广告
        AppConnect.getInstance(mContext).initAdInfo();
        AppConnect.getInstance(mContext).initPopAd(mContext);
        //LogUtils.d("ads", "" + AppConnect.getInstance(this).hasPopAd(this));
        // 设置插屏广告无数据时的回调监听（该方法必须在showPopAd之前调用）
        AppConnect.getInstance(mContext).setPopAdNoDataListener(new AppListener() {
            @Override
            public void onPopNoData() {
                LogUtils.i("ads", "插屏广告暂无可用数据");
            }
        });
        // 显示广告
        AppConnect.getInstance(mContext).showPopAd(mContext);

        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 6000);

    }

}
