package com.word.radio.newradio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.sunflower.FlowerCollector;

import com.word.radio.utils.LogUtils;
import com.word.radio.utils.NotificationUtil;

public class MainActivity extends AppCompatActivity {

    // UI 相关
    private View rootView;
    private TextView currentWord, currentChinese, allWordTextView;
    private Spinner spinner;
    private Button speechButton;
    private ProgressBar mProgressBarHorizontal;
    private MenuItem menuItem, menuItem1, menuItem2, menuItem3, menuItem4;
    private boolean isExit, openFloatWindow;
    private FloatView mFloatView;
    private ProgressDialog progressDialog;
    private int originalW;
    private int originalH;
    private Handler mHandler;
    private ImageView dialogBg;
    private boolean hashMapComplete;

    // 单词相关
    private String words;
    private final Pattern p = Pattern.compile("\\d.*?\\t(.+?)：(.*)");  //匹配单词和解释
    private Matcher m;
    private int selectedNum = 0;
    @SuppressLint("UseSparseArrays")
    private final Map<Integer, String> wordsHashMap = new HashMap<>();
    private int allWordNum = 0, targetLocation = 0;

    private MediaPlayer mediaPlayer;
    private int times = 2; //单词播放总次数
    private int count = 0; //控制单词当前已经播放次数
    private boolean needPlayChinese = true;
    private boolean isPlaying;
    private String[] content; //[单词, 释义]
    private SharedPreferences sharedPreferences = null;
    private boolean flag, reversed, autoRestart, pause = true;
    private SharedPreferences.Editor editor = null;
    private boolean repeat;
    // 讯飞TTS
    private static final String TAG = MainActivity.class.getSimpleName();
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 引擎类型
    private final String mEngineType;
    // 云端发音人名称列表
    private String[] mCloudVoicersEntries;
    private String[] mCloudVoicersValue;
    // 发音人声音配置
    private String voicer = "xiaoqi";       // 默认发音人
    final private String VOICE_VOL;  // 音量
    final private String VOICE_TONE; // 音调
    final private String VOICE_SPEED;// 语速

    {
        mEngineType = SpeechConstant.TYPE_CLOUD;
        VOICE_VOL = "85";
        VOICE_TONE = "50";
        VOICE_SPEED = "50";
    }

    // 线控相关
    private MediaButtonReceiver mediaButtonReceiver;
    private MyBroadcastReceiver myBroadcastReceiver;
    private HeadSetReceiver headSetReceiver;
    private int savedSpinnerPos, savedWordPos;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        restoreData();  //恢复数据
        initView();     //初始化界面
        initSpinner();  //初始化spinner
        applyData();    //应用恢复出来的数据
        initSpeech();   //初始化发音引擎
        initBroadCast();//初始化线控广播接收器
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveData();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreData();
        applyData();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        File file = new File(getExternalCacheDir() + "/wordAudios");
        if (!file.exists()) {
            initMp3();
        } else {
            System.gc();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.readChinese:
                flag = sharedPreferences.getBoolean("read", false);
                if (flag) {
                    editor.putBoolean("read", false);
                    needPlayChinese = true;
                    menuItem.setIcon(R.mipmap.chinese);
                    menuItem.setTitle(R.string.read_chinese);
                    showTip(getString(R.string.read_chinese));
                } else {
                    editor.putBoolean("read", true);
                    needPlayChinese = false;
                    menuItem.setIcon(R.mipmap.not_chinese);
                    menuItem.setTitle(R.string.not_read_chinese);
                    showTip(getString(R.string.not_read_chinese));
                }
                editor.commit();//提交修改
                break;
            case R.id.readTimes:
                if (times == 2) {
                    menuItem1.setTitle(R.string.read_times_two);
                    times = 1;
                } else {
                    menuItem1.setTitle(R.string.read_times);
                    times = 2;
                }
                break;
            case R.id.reversed:
                if (reversed) {
                    reversed = false;
                    targetLocation = 0;
                    menuItem2.setTitle(R.string.reversed);
                } else {
                    reversed = true;
                    targetLocation = allWordNum - 1;
                    menuItem2.setTitle(R.string.not_reversed);
                }
                break;
            case R.id.autoRestart:
                if (autoRestart) {
                    autoRestart = false;
                    menuItem3.setTitle(R.string.auto_restart);
                } else {
                    autoRestart = true;
                    menuItem3.setTitle(R.string.not_auto_restart);
                }
                break;
            case R.id.floatWindow:
                if (openFloatWindow) {
                    openFloatWindow = false;
                    mFloatView.close();
                    mFloatView = null;
                    menuItem4.setTitle(R.string.open_float_window);
                } else {
                    openFloatWindow = true;
                    initFloatWindow();
                    mFloatView.show();
                    menuItem4.setTitle(R.string.close_float_window);
                }
                break;
            case R.id.chooseVoice:
                showPersonSelectDialog();
                break;
            default:
                break;
        }
        return true;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menuItem = menu.findItem(R.id.readChinese); // 朗读中文
        menuItem1 = menu.findItem(R.id.readTimes);  // 朗读次数
        menuItem2 = menu.findItem(R.id.reversed);   // 倒序播放
        menuItem3 = menu.findItem(R.id.autoRestart);// 自动重播
        menuItem4 = menu.findItem(R.id.floatWindow);// 悬浮窗开关
        // 恢复数据时看是否需要重复播放
        if (reversed)
            menuItem2.setTitle(R.string.not_reversed);
        else
            menuItem2.setTitle(R.string.reversed);
        // 恢复数据时看是否需要自动重播
        if (autoRestart)
            menuItem3.setTitle(R.string.not_auto_restart);
        else
            menuItem3.setTitle(R.string.auto_restart);
        sharedPreferences = getSharedPreferences("read", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();      //获取编辑器
        flag = sharedPreferences.getBoolean("read", false);
        if (flag) {
            menuItem.setIcon(R.mipmap.not_chinese);
            menuItem.setTitle(R.string.not_read_chinese);
        } else {
            menuItem.setIcon(R.mipmap.chinese);
            menuItem.setTitle(R.string.read_chinese);
        }
        needPlayChinese = !flag;
        return true;
    }

    @SuppressLint("HandlerLeak")
    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setFitsSystemWindows(true);
        rootView = findViewById(R.id.main_root);
        currentWord = findViewById(R.id.current_word);
        currentChinese = findViewById(R.id.current_chinese);
        allWordTextView = findViewById(R.id.allWords);
        mProgressBarHorizontal = findViewById(R.id.progressBarHorizontal);
        speechButton = findViewById(R.id.speech);
        mCloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
        mCloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);
        dialogBg = findViewById(R.id.iv_dialog_bg);
        //创建activity先把对话框背景图设为不可见
        dialogBg.setImageAlpha(0);
        dialogBg.setVisibility(View.GONE);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    dialogBg.setVisibility(View.GONE);
                    System.gc();
                }
            }
        };
    }

    private void saveData() {
        SharedPreferences sp = getSharedPreferences("saved_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("savedSpinnerPos", savedSpinnerPos);
        editor.putInt("savedWordPos", targetLocation);
        editor.putBoolean("repeat", repeat);
        editor.putBoolean("reversed", reversed);
        editor.putBoolean("autoRestart", autoRestart);
        editor.apply();
    }

    private void restoreData() {
        SharedPreferences sp = getSharedPreferences("saved_data", Context.MODE_PRIVATE);
        savedSpinnerPos = sp.getInt("savedSpinnerPos", 0);
        savedWordPos = sp.getInt("savedWordPos", 0);
        repeat = sp.getBoolean("repeat", false);
        reversed = sp.getBoolean("reversed", false);
        autoRestart = sp.getBoolean("autoRestart", false);
    }

    private void applyData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    spinner.setSelection(savedSpinnerPos);
                    while (!hashMapComplete)
                        Thread.sleep(2);
                    if (!reversed) {
                        targetLocation = savedWordPos - 1;
                        LogUtils.i("get number is: ", targetLocation + "");
                        getTargetWord(targetLocation++);
                    } else {
                        targetLocation = savedWordPos + 1;
                        LogUtils.i("get number is: ", targetLocation + "");
                        getTargetWord(targetLocation--);
                    }
                    updateProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean hasNavBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasNavigationBar;
    }

    private int getNavBarHeight(Context ct) {
        boolean hasMenuKey = ViewConfiguration.get(ct).hasPermanentMenuKey();
        int resourceId = ct.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0 && !hasMenuKey) {
            return ct.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private void initMp3() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 在UI线程进行弹窗背景模糊
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleBlur();
                        }
                    });
                    buildProgressDialog(R.string.loading);
                    ReadFile.unZip(MainActivity.this, "wordAudios.zip", getExternalCacheDir() + "/wordAudios", true);
                    cancelProgressDialog();
                    showTip(getString(R.string.init_complete));
                } catch (Exception e) {
                    e.printStackTrace();
                    File file = new File(getExternalCacheDir() + "/wordAudios/");
                    ReadFile.deleteAllFilesOfDir(file);
                    showTip(getString(R.string.init_failed));
                }
            }
        }).start();
    }

    /**
     * 加载框
     * @param id 作为标题的string资源的id
     */
    private void buildProgressDialog(final int id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                }
                progressDialog.setMessage(getString(id));
                progressDialog.setCancelable(false);
                progressDialog.show();
                setScreenBgLight(progressDialog);//设置窗口背景明暗度
            }
        });

    }

    /**
     * 取消加载框
     */
    private void cancelProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null)
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        hideBlur();
                    }
            }
        });
    }

    /*模糊背景加载*/
    private Bitmap captureScreen(Activity activity) {
        activity.getWindow().getDecorView().destroyDrawingCache();  //先清理屏幕绘制缓存(重要)
        activity.getWindow().getDecorView().setDrawingCacheEnabled(true);
        Bitmap bmp = activity.getWindow().getDecorView().getDrawingCache();
        //如果有导航栏则将截图剪切一下去掉导航栏
        if (hasNavBar(activity)) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight() - getNavBarHeight(activity));
        }
        //获取原图尺寸
        originalW = bmp.getWidth();
        originalH = bmp.getHeight();
        //对原图进行缩小，提高下一步高斯模糊的效率
        bmp = Bitmap.createScaledBitmap(bmp, originalW / 4, originalH / 4, false);
        return bmp;
    }

    private void setScreenBgLight(Dialog dialog) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp;
        if (window != null) {
            lp = window.getAttributes();
            lp.dimAmount = 0.25f;
            window.setAttributes(lp);
        }
    }

    private void handleBlur() {
        Bitmap bp = captureScreen(MainActivity.this);
        bp = blur(bp);                      //对屏幕截图模糊处理
        //将模糊处理后的图恢复到原图尺寸并显示出来
        bp = Bitmap.createScaledBitmap(bp, originalW, originalH, false);
        dialogBg.setImageBitmap(bp);
        dialogBg.setVisibility(View.VISIBLE);
        //防止UI线程阻塞，在子线程中让背景实现淡入效果
        asyncRefresh(true);
    }

    private void asyncRefresh(boolean in) {
        //淡出淡入效果的实现
        if (in) {    //淡入效果
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i <= 255; i += 5) {
                        refreshUI(i);//在UI线程刷新视图
                        try {
                            Thread.sleep(6);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } else {    //淡出效果
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 255; i >= 0; i -= 1) {
                        refreshUI(i);//在UI线程刷新视图
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //当淡出效果完毕后发送消息给mHandler把对话框背景设为不可见
                    mHandler.sendEmptyMessage(0);
                }
            }).start();
        }
    }

    private void refreshUI(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogBg.setImageAlpha(i);
            }
        });
    }

    private void hideBlur() {
        //把对话框背景隐藏
        asyncRefresh(false);
    }

    private Bitmap blur(Bitmap bitmap) {
        //使用RenderScript对图片进行高斯模糊处理
        Bitmap output = Bitmap.createBitmap(bitmap); // 创建输出图片
        RenderScript rs = RenderScript.create(this); // 构建一个RenderScript对象
        ScriptIntrinsicBlur gaussianBlue = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)); //
        // 创建高斯模糊脚本
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap); // 开辟输入内存
        Allocation allOut = Allocation.createFromBitmap(rs, output); // 开辟输出内存
        float radius = 10f;     //设置模糊半径
        gaussianBlue.setRadius(radius); // 设置模糊半径，范围0f<radius<=25f
        gaussianBlue.setInput(allIn); // 设置输入内存
        gaussianBlue.forEach(allOut); // 模糊编码，并将内存填入输出内存
        allOut.copyTo(output); // 将输出内存编码为Bitmap，图片大小必须注意
        rs.destroy();
        //rs.releaseAllContexts(); // 关闭RenderScript对象，API>=23则使用rs.releaseAllContexts()
        return output;
    }

    private void initWordsHashMap() {
        int location = 0;
        while (m.find()) {
            //LogUtils.e("look", location + "");
            wordsHashMap.put(location++, m.group(1) + "|" + m.group(2));
        }
        allWordNum = location;  //将单词总数返回给allWordNum
        hashMapComplete = true;
    }


    /**
     * 发音人选择。
     */
    private void showPersonSelectDialog() {
        handleBlur();
        final BottomSheetDialog bsd = new BottomSheetDialog(this);
        setScreenBgLight(bsd);
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(15, 15, 15, 15);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);
        int childIndex;
        for (childIndex = 0; childIndex < mCloudVoicersEntries.length; childIndex++) {
            layout.addView(new TextView(this), childIndex);
            final TextView tv = (TextView) layout.getChildAt(childIndex);
            tv.setText(mCloudVoicersEntries[childIndex]);
            tv.setTextSize(25f);
            tv.setLayoutParams(params);
            final int finalChildIndex = childIndex;
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    voicer = mCloudVoicersValue[finalChildIndex];
                    selectedNum = finalChildIndex;
                    hideBlur();
                    bsd.dismiss();
                    showTip(getString(R.string.operate_succeed));
                }
            });
        }
        bsd.setContentView(layout);
        bsd.setCancelable(true);
        bsd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                hideBlur();
            }
        });
        bsd.show();
        /*
        new AlertDialog.Builder(this).setTitle("发音人选择")
                .setSingleChoiceItems(mCloudVoicersEntries, // 单选框有几项,各是什么名字
                        selectedNum, // 默认的选项
                        new DialogInterface.OnClickListener() { // 点击单选框后的处理
                            public void onClick(DialogInterface dialog,
                                                int which) { // 点击了哪一项
                                voicer = mCloudVoicersValue[which];
                                selectedNum = which;
                                dialog.dismiss();
                            }
                        }).show();*/
    }

    /**
     * 初始化合成对象
     */
    private void initSpeech() {
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);
    }

    /**
     * 初始化监听。
     */
    private final InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            }
        }
    };

    /**
     * 合成回调监听。
     */
    private final SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            //showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            //showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度

        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度

        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                // 播放完成监听
                //showTip("缓存完成");
                if (!pause)
                    //playChinese();
                    playNextWord();
            } else {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            LogUtils.e(TAG, "TTS Demo onEvent >>>" + eventType);
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                LogUtils.d(TAG, "session id =" + sid);
            }
        }
    };

    private void initSpinner() {
        // 数据源
        final String[] array = {"part 1", "part 2", "part 3", "part 4", "part 5", "part 6", "part 7", "part 8", "part 9", "part 10", "part 11",
                "part 12", "part 13", "part 14", "part 15", "part 16", "part 17", "part 18", "part 19", "part 20", "part 21", "part 22", "part 23",
                "part 24", "part 25", "part 26", "part 27", "part 28", "part 29", "part 30", "part 31", "part 32", "part 33", "part 34", "part 35"};

        spinner = findViewById(R.id.spinner);
        // 添加选中条目的点击事件
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                words = ReadFile.readAssetsFile(getApplicationContext(), array[position].replace(" ", "_"));
                savedSpinnerPos = position;
                m = p.matcher(words);
                wordsHashMap.clear();
                initWordsHashMap();
                if (!reversed)
                    targetLocation = 0;
                else
                    targetLocation = allWordNum - 1;
                mProgressBarHorizontal.setProgress(0);
                allWordTextView.setText(words.replaceAll("\\t", ".   "));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                words = ReadFile.readAssetsFile(getApplicationContext(), "part_1");
                m = p.matcher(words);
                initWordsHashMap();
                if (!reversed)
                    targetLocation = 0;
                else
                    targetLocation = allWordNum - 1;
                mProgressBarHorizontal.setProgress(0);
                //LogUtils.e("look", words);
                allWordTextView.setText(words.replaceAll("\\t", ".   "));
            }
        });
        //创建适配器对象
        SpinnerAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, array);
        //给Spinner设置适配器
        spinner.setAdapter(adapter);
    }

    private void showTip(final String str) {
        LogUtils.e("showTip", str);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                Snackbar.make(rootView, str, Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * 参数设置
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //onEvent回调接口实时返回音频流数据
            //mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, VOICE_SPEED);
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, VOICE_TONE);
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, VOICE_VOL);
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
            /*
              本地合成不设置语速、音调、音量，默认使用语记设置
              开发者如需自定义参数，请参考在线合成参数设置
             */
        }
        //设置播放器音频流类型为3(音乐)
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为false
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, getCacheDir() + "/tts.pcm");
    }

    private void speak(String texts) {
        // 移动数据分析，收集开始合成事件
        FlowerCollector.onEvent(MainActivity.this, "tts_play");
        setParam();
        //String path = getApplicationContext().getCacheDir() + "/tts.ogg";
        //int code = mTts.synthesizeToUri(texts, path, mTtsListener);
        /*int code = */
        mTts.startSpeaking(texts, mTtsListener);
        /*if (code != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + code);
        }*/
        //return code;
    }

    private void initBroadCast() {
        //线控广播接收器
        mediaButtonReceiver = new MediaButtonReceiver();
        registerReceiver(mediaButtonReceiver, new IntentFilter(Intent.ACTION_MEDIA_BUTTON));

        // 耳机拔插广播接收器
        headSetReceiver = new HeadSetReceiver();
        IntentFilter headSetIF = new IntentFilter();
        headSetIF.addAction(Intent.ACTION_HEADSET_PLUG);
        headSetIF.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(headSetReceiver, headSetIF);

        //程序内部广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ok");
        intentFilter.addAction("restart");
        intentFilter.addAction("next");
        intentFilter.addAction("previous");
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    private void playByTts() {
        speak(content[1].replaceAll("[a-zA-z&]+\\.", ""));
    }

    /**
     * @param location 待查找的单词位置
     * @return 单词和释义组成的数组
     */
    private String[] getTargetWord(int location) {
        if (repeat && !reversed && targetLocation > 0) {
            location = --targetLocation;
        } else if (repeat && reversed && targetLocation < allWordNum) {
            location = ++targetLocation;
        }
        LogUtils.i("number is: ", location + "");
        String word = wordsHashMap.get(location);
        if (word == null) {
            return new String[]{"null", "null"};
        }
        final String mWord = word.split("\\|")[0];
        final String mChinese = word.split("\\|")[1];

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wordChangeAnim();   // 显示单词切换动画
                currentWord.setText(mWord);
                currentChinese.setText(mChinese);
                if (openFloatWindow) {
                    mFloatView.textView.setText(String.format("%s  %s", mWord, mChinese));
                }
                showNotification(getApplicationContext(), mWord, mChinese);
            }
        });

        return new String[]{mWord, mChinese};
    }

    /**
     * 播放下一个单词
     */
    private void playNextWord() {
        if (pause) {
            return;
        }
        if (!isPlaying) {
            isPlaying = true;
            speechButton.setText(R.string.pause);
        }
        if (!pause)
            speechButton.setText(R.string.pause);
        updateProgress();
        if (!reversed) {
            if (targetLocation >= 0 && targetLocation < allWordNum) {
                content = getTargetWord(targetLocation++);
                String wordName = content[0];
                //LogUtils.e("content", wordName);
                if (wordName.equals("null")) {
                    showTip("获取单词出错");
                    speechButton.setText(R.string.begin);
                } else {
                    //LogUtils.e("restart", pause + ":pause");
                    if (!pause) play(wordName);
                }
            } else {
                LogUtils.i("targetLocation", targetLocation + "");
                showTip("播放完毕");
                isPlaying = false;
                pause = true;
                speechButton.setText(R.string.restart);
                if (autoRestart) {
                    targetLocation = 0;
                    buttonFunction();
                }
            }
        } else {
            if (targetLocation >= 0 && targetLocation <= allWordNum) {
                content = getTargetWord(targetLocation--);
                //LogUtils.e("content", content[0] + "\n" + content[1]);
                String wordName = content[0];
                if (!pause) play(wordName);
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTip("播放完毕");
                        isPlaying = false;
                        pause = true;
                        speechButton.setText(R.string.restart);
                        if (autoRestart) {
                            targetLocation = allWordNum - 1;
                            buttonFunction();
                        }
                    }
                });
            }
        }
    }

    private void updateProgress() {
        if (!reversed) {
            mProgressBarHorizontal.setProgress((int) ((targetLocation + 1) / (float) (allWordNum + 1) * 100) + 1);
        } else {
            mProgressBarHorizontal.setProgress((int) ((allWordNum + 1 - targetLocation) / (float) (allWordNum + 1) * 100));
        }
    }

    private void wordChangeAnim() {
        if (!repeat) {
            //单词卡片切换动画
            Animation changeAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move_left);
            currentWord.setVisibility(View.VISIBLE);
            currentChinese.setVisibility(View.VISIBLE);
            currentWord.startAnimation(changeAnim);
            currentChinese.startAnimation(changeAnim);
        }
    }

    /**
     * 播放指定文件地址的音频
     *
     * @param word 单词
     */
    private void play(String word) {
        if (mTts.isSpeaking()) {
            mTts.stopSpeaking();
        }
        if (!repeat)
            saveData();
        try {
            if (mediaPlayer == null)
                mediaPlayer = new MediaPlayer();
            //LogUtils.e("word", word);
            //朗读相关
            /*File tempFile = new File(getExternalCacheDir() + "/wordAudios/" + word + ".mp3");
            FileInputStream fis = new FileInputStream(tempFile);
            mediaPlayer.setDataSource(fis.getFD());*/
            mediaPlayer.setDataSource(getExternalCacheDir() + "/wordAudios/" + word + ".mp3");
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();//同步的准备方法。
            //mediaPlayer.prepareAsync();//异步的准备
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (pause) {
                        mediaPlayer.reset();
                        count = 0;
                    } else {
                        mediaPlayer.start();
                        count++;
                    }
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (count < times) {
                        mediaPlayer.start();
                        count++;
                    } else {
                        count = 0;
                        mediaPlayer.reset();
                        if (needPlayChinese && !pause) {
                            playByTts();
                        } else if (!pause) {
                            playNextWord();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            //LogUtils.e("player", "播放失败:" + word);
        }

    }


    /**
     * 暂停当前的声音
     */
    private void pausePlay() {
        count = 0;
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        if (mTts != null && mTts.isSpeaking()) {
            mTts.stopSpeaking();
        }
    }

    /**
     * 继续播放当前单词
     */
    private void continuePlay() {
        if (!reversed) {
            targetLocation--;
        } else {
            targetLocation++;
        }
        pause = false;
        playNextWord();
    }

    public void repeatPlay(View v) {
        if (repeat && !reversed) {
            targetLocation++;
            showTip("顺序播放");
        } else if (!repeat && !reversed) {
            targetLocation--;
            showTip("重复播放");
        } else if (repeat) {
            targetLocation--;
            showTip("顺序播放");
        } else {
            targetLocation++;
            showTip("重复播放");
        }
        repeat = !repeat;
    }

    /**
     * 切换到上一个单词
     */
    public void previous(View v) {
        if (!repeat) {
            if (mediaPlayer != null && mediaPlayer.isPlaying())
                pausePlay();
            if (!reversed) {
                targetLocation -= 2;
            } else {
                targetLocation += 2;
            }
            if (targetLocation < 0) targetLocation = 0;
            if (targetLocation >= allWordNum) targetLocation = allWordNum - 1;
            pause = false;
            playNextWord();
        } else {
            showTip(getString(R.string.cannot_switch));
        }
    }

    /**
     * 切换到下一个单词
     */
    public void next(View v) {
        if (!repeat) {
            if (mediaPlayer != null && mediaPlayer.isPlaying())
                pausePlay();
            if (targetLocation > allWordNum) targetLocation = allWordNum - 1;
            pause = false;
            playNextWord();
        } else {
            showTip(getString(R.string.cannot_switch));
        }
    }

    /**
     * 暂停开始按钮
     */
    public void speech(View v) {
        buttonFunction();
    }

    private void buttonFunction() {
        if (repeat && !reversed && !isPlaying && pause && targetLocation > 1) {
            LogUtils.i("targetLocation", targetLocation + "：目标单词索引");
            targetLocation++;
        } else if (repeat && reversed && !isPlaying && pause && targetLocation < allWordNum - 1) {
            LogUtils.i("targetLocation", targetLocation + "：目标单词索引");
            targetLocation--;
        }
        pause = !pause;
        if (!reversed) {
            if (!isPlaying) { // 没有在播放
                isPlaying = true;
                speechButton.setText(R.string.pause);
                if (targetLocation < allWordNum && targetLocation >= 0) {
                    if (targetLocation == 0) {
                        playNextWord();
                        LogUtils.i("播放下一首");
                    } else {
                        continuePlay();
                        LogUtils.i("继续播放");
                    }
                } else {
                    targetLocation = 0;
                    //LogUtils.e("restart", "重来");
                    pause = false;
                    playNextWord();
                }
            } else {
                // 正在播放的情况
                speechButton.setText(R.string.begin);
                pausePlay();
                isPlaying = !isPlaying;
            }
        } else {//倒放
            if (!isPlaying) { // 没有在播放
                isPlaying = !isPlaying;
                speechButton.setText(R.string.pause);
                if (targetLocation > 0 && targetLocation < allWordNum) {
                    if (targetLocation >= allWordNum - 1)
                        playNextWord();
                    else
                        continuePlay();
                } else {
                    targetLocation = allWordNum - 1;
                    //LogUtils.e("restart", "重来");
                    pause = false;
                    playNextWord();
                }
            } else {
                // 正在播放的情况
                speechButton.setText(R.string.begin);
                pausePlay();
                isPlaying = !isPlaying;
            }
        }
        if (content != null)
            showNotification(getApplicationContext(), content[0], content[1]);

    }

    /**
     * 双击返回键退出
     *
     * @param keyCode 按键码
     * @param event   事件
     * @return 布尔类型
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                isExit = true;
                showTip("再按一次回到桌面");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            isExit = false;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                //finish();
                Intent backHome = new Intent(Intent.ACTION_MAIN);
                backHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                backHome.addCategory(Intent.CATEGORY_HOME);
                startActivity(backHome);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 通知栏通知
     */
    private void showNotification(Context context, String title, String msg) {

        Intent previousIntent = new Intent("previous");
        Intent middleIntent = new Intent("ok");
        Intent nextIntent = new Intent("next");
        PendingIntent preIntent = PendingIntent.getBroadcast(this, 0, previousIntent, 0);
        PendingIntent midIntent = PendingIntent.getBroadcast(this, 0, middleIntent, 0);
        PendingIntent nexIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout); //自定义的布局视图
        remoteViews.setTextViewText(R.id.notify_title, title);
        remoteViews.setTextViewText(R.id.notify_subtitle, msg);
        remoteViews.setImageViewResource(R.id.notify_icon_iv, R.mipmap.ic_launcher);
        remoteViews.setImageViewResource(R.id.notify_next_bt, R.mipmap.next);
        remoteViews.setImageViewResource(R.id.notify_previous_bt, R.mipmap.previous);
        if (pause)
            remoteViews.setImageViewResource(R.id.notify_middle_bt, R.mipmap.play);
        else
            remoteViews.setImageViewResource(R.id.notify_middle_bt, R.mipmap.pause);

        remoteViews.setOnClickPendingIntent(R.id.notify_previous_bt, preIntent);
        remoteViews.setOnClickPendingIntent(R.id.notify_middle_bt, midIntent);
        remoteViews.setOnClickPendingIntent(R.id.notify_next_bt, nexIntent);

        // 适配安卓8.0以下和以上的通知
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationUtil.getNotification(context, title, msg, remoteViews, "0",
                    getString(R.string.notify_channelName));
        } else {
            NotificationUtil.getNotification(context, title, msg, remoteViews);
        }

    }

    /**
     * 悬浮窗相关
     */
    // 如果悬浮窗权限存在则返回true
    private boolean checkPermission() {
        // 权限判断
        if (Build.VERSION.SDK_INT >= 23) {
            return Settings.canDrawOverlays(getApplicationContext());
        }
        return true;
    }

    private void initFloatWindow() {
        if (!checkPermission()) {
            //启动Activity让用户授权
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            LogUtils.i("permission", "权限请求");
        }
        mFloatView = new FloatView(MainActivity.this);
        mFloatView.setLayout(R.layout.float_static);
        if (currentWord != null && currentChinese != null)
            mFloatView.textView.setText(String.format("%s  %s", currentWord.getText(), currentChinese.getText()));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "权限授予失败，无法开启悬浮窗", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    showTip("权限授予成功！");
                    initFloatWindow();
                }
            }
        }
        LogUtils.e("permission", "权限请求:" + requestCode);
    }

    /**********************************线控相关******************************************/

    @Override
    protected void onResume() {
        super.onResume();
        //mAudioManager.registerMediaButtonEventReceiver(mComponentName);
        //registerReceiver(headSetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        //移动数据统计分析
        FlowerCollector.onResume(MainActivity.this);
        FlowerCollector.onPageStart(TAG);

    }

    @Override
    protected void onPause() {
        LogUtils.i("触发onPause");
        //移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(MainActivity.this);
        saveData(); //保存用户数据
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationUtil.cancelNotificationHigh();
        } else {
            NotificationUtil.cancelNotification();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mFloatView != null) {
            if (mFloatView.isShow())
                mFloatView.close();
            mFloatView = null;
        }
        unregisterReceiver(mediaButtonReceiver);
        unregisterReceiver(headSetReceiver);
        unregisterReceiver(myBroadcastReceiver);
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }

        super.onDestroy();
        System.exit(0);
    }

    private class HeadSetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.i("bluetoothReceiver", action);
            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET) && isPlaying && !pause) {
                    //Toast.makeText(context, "Bluetooth headset is now disconnected", Toast.LENGTH_LONG).show();
                    buttonFunction();
                }
            } else if ("android.intent.action.HEADSET_PLUG".equals(action)) {
                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0 && isPlaying && !pause) {
                        //Toast.makeText(context, "headset not connected", Toast.LENGTH_LONG).show();
                        buttonFunction();
                    }
                    /*else if (intent.getIntExtra("state", 0) == 1){
                        Toast.makeText(context, "headset connected", Toast.LENGTH_LONG).show();
                    }*/
                }
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.i("MediaButtonReceiverMain", action);
            if (action == null) return;
            switch (action) {
                case "ok":
                    if (targetLocation < allWordNum - 1 && targetLocation > 0) {
                        buttonFunction();
                    } else if (!reversed && targetLocation >= allWordNum - 1) {
                        targetLocation = 0;
                        isPlaying = false;
                        pause = true;
                        buttonFunction();
                    } else if (reversed && targetLocation <= 0) {
                        targetLocation = allWordNum - 1;
                        isPlaying = false;
                        pause = true;
                        buttonFunction();
                    } else {
                        buttonFunction();
                    }
                    break;
                case "restart":
                    LogUtils.i("MediaButtonReceiverMain", "重启");
                    isPlaying = false;
                    if (!reversed) {
                        targetLocation = 0;
                        pause = true;
                        buttonFunction();
                    } else {
                        targetLocation = allWordNum - 1;
                        pause = true;
                        buttonFunction();
                    }
                    break;
                case "previous":
                    LogUtils.i("MediaButtonReceiverMain", "previousMain");
                    previous(getCurrentFocus());
                    break;
                case "next":
                    next(getCurrentFocus());
                    LogUtils.i("MediaButtonReceiverMain", "NextMain");
                    break;
                default:
                    break;
            }
        }
    }
}
