package com.word.radio.newradio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
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

public class MainActivity extends AppCompatActivity {

    // UI 相关
    private TextView currentWord, currentChinese, allWordTextView;
    private Spinner spinner;
    private Button speechButton;
    private ProgressBar mProgressBarHorizontal;
    private MenuItem menuItem, menuItem1, menuItem2, menuItem3, menuItem4;
    private boolean isExit, openFloatWindow;
    private FloatView mFloatView;
    private ProgressDialog progressDialog;
    private Notification notification;
    private int originalW;
    private int originalH;
    private Handler mHandler;
    private ImageView dialogBg;
    private boolean hashMapComplete;


    //单词相关
    private String words;
    private Pattern p = Pattern.compile("\\d.*?\\t(.+?)：(.*)");  //匹配单词和解释
    private Matcher m;
    private int selectedNum = 0;
    @SuppressLint("UseSparseArrays")
    private Map<Integer, String> wordsHashMap = new HashMap<>();
    private int allWordNum = 0, targetLocation = 0;

    //朗读相关
    private File tempFile;
    private FileInputStream fis;
    private MediaPlayer mediaPlayer;
    int times = 2; //单词播放总次数
    int count = 0; //控制单词当前已经播放次数
    private boolean needPlayChinese = true;
    private boolean isPlaying;
    private String[] content; //[单词, 释义]
    private SharedPreferences sharedPreferences = null;
    private boolean flag, reversed, autoRestart, pause = true;
    private SharedPreferences.Editor editor = null;
    private boolean repeat;
    // 讯飞TTS
    private static String TAG = MainActivity.class.getSimpleName();
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 云端发音人名称列表
    private String[] mCloudVoicersEntries;
    private String[] mCloudVoicersValue;

    private String voicer = "xiaoqi";       // 默认发音人
    final private String VOICE_VOL = "85";  // 音量
    final private String VOICE_TONE = "50"; // 音调
    final private String VOICE_SPEED = "50";// 语速

    //线控相关
    private AudioManager mAudioManager;
    private ComponentName mComponentName;
    private MyBroadcastReceiver myBroadcastReceiver;
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
                    spinner.setSelection(savedSpinnerPos, true);
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

                    //getTargetWord(targetLocation++);
                    updateProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasChanged) {
        super.onWindowFocusChanged(hasChanged);
        // 复制MP3数据
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
                    Toast.makeText(getApplicationContext(), R.string.read_chinese, Toast.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean("read", true);
                    needPlayChinese = false;
                    menuItem.setIcon(R.mipmap.not_chinese);
                    menuItem.setTitle(R.string.not_read_chinese);
                    Toast.makeText(getApplicationContext(), R.string.not_read_chinese, Toast.LENGTH_SHORT).show();
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
        // 恢复数据是看是否需要自动重播
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

    private void initMp3() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
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
     */
    public void buildProgressDialog(final int id) {
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
    public void cancelProgressDialog() {
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
        //获取原图尺寸
        originalW = bmp.getWidth();
        originalH = bmp.getHeight();
        //对原图进行缩小，提高下一步高斯模糊的效率
        bmp = Bitmap.createScaledBitmap(bmp, originalW / 4, originalH / 4, false);
        return bmp;
    }

    private void setScreenBgLight(ProgressDialog dialog) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp;
        if (window != null) {
            lp = window.getAttributes();
            lp.dimAmount = 0.1f;
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
                    for (int i = 0; i < 256; i += 5) {
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
                    for (int i = 255; i >= 0; i -= 5) {
                        refreshUI(i);//在UI线程刷新视图
                        try {
                            Thread.sleep(6);
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

        new AlertDialog.Builder(this).setTitle("在线合成发音人选项")
                .setSingleChoiceItems(mCloudVoicersEntries, // 单选框有几项,各是什么名字
                        selectedNum, // 默认的选项
                        new DialogInterface.OnClickListener() { // 点击单选框后的处理
                            public void onClick(DialogInterface dialog,
                                                int which) { // 点击了哪一项
                                voicer = mCloudVoicersValue[which];
                                selectedNum = which;
                                dialog.dismiss();
                            }
                        }).show();
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
    private InitListener mTtsInitListener = new InitListener() {
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
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

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
        String[] array = {"part 1", "part 2", "part 3", "part 4", "part 5", "part 6", "part 7", "part 8", "part 9", "part 10", "part 11",
                "part 12", "part 13", "part 14", "part 15", "part 16", "part 17", "part 18", "part 19", "part 20", "part 21", "part 22", "part 23",
                "part 24", "part 25", "part 26", "part 27", "part 28", "part 29", "part 30", "part 31", "part 32", "part 33", "part 34", "part 35"};

        spinner = findViewById(R.id.spinner);
        // 添加选中条目的点击事件
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                switch (position) {
                    case 0:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_1");
                        break;
                    case 1:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_2");
                        break;
                    case 2:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_3");
                        break;
                    case 3:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_4");
                        break;
                    case 4:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_5");
                        break;
                    case 5:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_6");
                        break;
                    case 6:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_7");
                        break;
                    case 7:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_8");
                        break;
                    case 8:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_9");
                        break;
                    case 9:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_10");
                        break;
                    case 10:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_11");
                        break;
                    case 11:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_12");
                        break;
                    case 12:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_13");
                        break;
                    case 13:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_14");
                        break;
                    case 14:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_15");
                        break;
                    case 15:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_16");
                        break;
                    case 16:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_17");
                        break;
                    case 17:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_18");
                        break;
                    case 18:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_19");
                        break;
                    case 19:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_20");
                        break;
                    case 20:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_21");
                        break;
                    case 21:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_22");
                        break;
                    case 22:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_23");
                        break;
                    case 23:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_24");
                        break;
                    case 24:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_25");
                        break;
                    case 25:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_26");
                        break;
                    case 26:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_27");
                        break;
                    case 27:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_28");
                        break;
                    case 28:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_29");
                        break;
                    case 29:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_30");
                        break;
                    case 30:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_31");
                        break;
                    case 31:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_32");
                        break;
                    case 32:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_33");
                        break;
                    case 33:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_34");
                        break;
                    case 34:
                        words = ReadFile.readAssetsFile(getApplicationContext(), "part_35");
                        break;
                }
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
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
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
            //onevent回调接口实时返回音频流数据
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
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // AudioManager注册一个MediaButton对象
        mComponentName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(mComponentName);
        registerReceiver(headSetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

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
        LogUtils.i("number is: ", location+"");
        String word = wordsHashMap.get(location);
        if(word == null){
            return new String[]{"null", "null"};
        }
        final String mWord = word.split("\\|")[0];
        final String mChinese = word.split("\\|")[1];

        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                currentWord.setText(mWord);
                currentChinese.setText(mChinese);
                if (openFloatWindow) {
                    mFloatView.textView.setText(mWord + "  " + mChinese);
                }
            }
        });
        showNotification(getApplicationContext(), mWord, mChinese);

        return new String[]{mWord, mChinese};
    }

    /**
     * 播放下一个单词
     */
    private void playNextWord() {
        if (!isPlaying) {
            isPlaying = true;
            speechButton.setText(R.string.pause);
        }
        if(!pause)
            speechButton.setText(R.string.pause);
        updateProgress();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!reversed) {
                    if (targetLocation >= 0 && targetLocation < allWordNum) {
                        content = getTargetWord(targetLocation++);
                        String wordName = content[0];
                        //LogUtils.e("content", wordName);
                        if (wordName.equals("null")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "获取单词出错", Toast.LENGTH_SHORT).show();
                                    speechButton.setText(R.string.begin);
                                }
                            });
                        } else {
                            //LogUtils.e("restart", pause + ":pause");
                            if (!pause) play(wordName);
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.i("targetLocation", targetLocation + "");
                                Toast.makeText(getApplicationContext(), "播放完毕", Toast.LENGTH_SHORT).show();
                                isPlaying = false;
                                pause = true;
                                speechButton.setText(R.string.restart);
                                if (autoRestart) {
                                    targetLocation = 0;
                                    buttonFunction();
                                }
                            }
                        });
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
                                Toast.makeText(getApplicationContext(), "播放完毕", Toast.LENGTH_SHORT).show();
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
        }).start();
    }

    private void updateProgress() {
        if (!reversed) {
            mProgressBarHorizontal.setProgress((int) ((targetLocation + 1) / (float) (allWordNum + 1) * 100) + 1);
        } else {
            mProgressBarHorizontal.setProgress((int) ((allWordNum + 1 - targetLocation) / (float) (allWordNum + 1) * 100));
        }
    }


    /**
     * 播放指定文件地址的音频
     *
     * @param word 单词
     */
    public void play(String word) {
        if (mTts.isSpeaking()) {
            mTts.stopSpeaking();
        }
        if (!repeat)
            saveData();
        try {
            if (mediaPlayer == null)
                mediaPlayer = new MediaPlayer();
            //LogUtils.e("word", word);
            tempFile = new File(getExternalCacheDir() + "/wordAudios/" + word + ".mp3");
            fis = new FileInputStream(tempFile);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();//同步的准备方法。
            //mediaPlayer.prepareAsync();//异步的准备
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    count++;
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
                        if (needPlayChinese) {
                            playByTts();
                        } else {
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
        //if (mediaPlayer != null) {
            if (!reversed) {
                targetLocation--;
            } else {
                targetLocation++;
            }
        pause = false;
            playNextWord();
        //}
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
     * @param event 事件
     * @return 布尔类型
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                isExit = true;
                Toast.makeText(getApplicationContext(), "再按一次回到桌面",
                        Toast.LENGTH_SHORT).show();
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
    public void showNotification(Context context, String title, String msg) {

        Intent previousIntent = new Intent("previous");
        Intent middleIntent = new Intent("ok");
        Intent nextIntent = new Intent("next");
        PendingIntent preIntent = PendingIntent.getBroadcast(this, 0, previousIntent, 0);
        PendingIntent midIntent = PendingIntent.getBroadcast(this, 0, middleIntent, 0);
        PendingIntent nexIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout); //自定义的布局视图
        remoteViews.setTextViewText(R.id.notif_title, title);
        remoteViews.setTextViewText(R.id.notif_subtitle, msg);
        remoteViews.setImageViewResource(R.id.notif_icon_iv, R.mipmap.icon);
        remoteViews.setImageViewResource(R.id.notif_next_bt, R.mipmap.next);
        remoteViews.setImageViewResource(R.id.notif_previous_bt, R.mipmap.previous);
        if (pause)
            remoteViews.setImageViewResource(R.id.notif_middle_bt, R.mipmap.play);
        else
            remoteViews.setImageViewResource(R.id.notif_middle_bt, R.mipmap.pause);

        remoteViews.setOnClickPendingIntent(R.id.notif_previous_bt, preIntent);
        remoteViews.setOnClickPendingIntent(R.id.notif_middle_bt, midIntent);
        remoteViews.setOnClickPendingIntent(R.id.notif_next_bt, nexIntent);

        notification = new NotificationCompat.Builder(context)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.icon))
                .setSmallIcon(R.mipmap.icon)
                .setTicker("通知来了")
                .setContentTitle(title)
                .setContentText(msg)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setCustomContentView(remoteViews)
                .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    /**
     * 悬浮窗相关
     */
    //如果悬浮窗权限存在则返回true
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
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "权限授予失败，无法开启悬浮窗", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "权限授予成功！", Toast.LENGTH_SHORT).show();
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
        /*if (textToSpeech != null)
            textToSpeech.shutdown();*/
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (mFloatView != null) {
            if (mFloatView.isShow())
                mFloatView.close();
            mFloatView = null;
        }
        notification = null;
        if (mAudioManager != null)
            mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
        unregisterReceiver(headSetReceiver);
        unregisterReceiver(myBroadcastReceiver);
        mAudioManager = null;
        mComponentName = null;
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        notification = null;
        super.onDestroy();
    }

    private final BroadcastReceiver headSetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_HEADSET_PLUG)) {
                // phone headset plugged
                if (intent.getIntExtra("state", 0) == 1) {
                    // do something
//					LogUtils.d(TAG, "耳机检测：插入");
//					Toast.makeText(context, "耳机检测：插入", Toast.LENGTH_SHORT) .show();
                    mAudioManager.registerMediaButtonEventReceiver(mComponentName);
                    // phone head unplugged
                } else {
                    // do something
//					LogUtils.d(TAG, "耳机检测：没有插入");
//					Toast.makeText(context, "耳机检测：没有插入", Toast.LENGTH_SHORT).show();
                    if (isPlaying) {
                        buttonFunction();
                    }
                    mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
                }
            }
        }
    };

    public class MyBroadcastReceiver extends BroadcastReceiver {
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
            }
        }
    }
}
