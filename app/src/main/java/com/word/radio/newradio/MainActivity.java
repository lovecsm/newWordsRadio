package com.word.radio.newradio;

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
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
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
    private MenuItem menuItem, menuItem1, menuItem2, menuItem3, menuItem4, menuItem5;
    private boolean isExit, openFloatWindow;
    private FloatView mFloatView;
    private ProgressDialog progressDialog;
    private boolean inBg;
    private Notification notification;


    //单词相关
    private String words;
    private Pattern p = Pattern.compile("\\d.*?\\t(.+?)：(.*)");  //匹配单词和解释
    private Matcher m;
    private Map<String, Integer> wordsHashMap = new HashMap<>();
    private int allWordNum = 0, targetLocation = 0;

    //朗读相关
    private File tempFile;
    private FileInputStream fis;
    private MediaPlayer mediaPlayer;
    int times = 2; //控制单词播放次数
    int count = 0; //控制单词播放次数
    private boolean needPlayChinese = true;
    //private File file;
    private boolean isPlaying;
    private String[] content; //[单词, 释义]
    //final HashMap ttsOptions = new HashMap<>();
    private SharedPreferences sharedPreferences = null;
    private boolean flag, reversed, autoRestart, pause = true;
    private SharedPreferences.Editor editor = null;
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
    private Toast mToast;

    //线控相关
    private AudioManager mAudioManager;
    private ComponentName mComponentName;
    private MyBroadcastReceiver myBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化界面并读取单词数据
        initView();
        initSpinner();
        initSpeech();
        initBroadCast();
    }

    @Override
    public void onWindowFocusChanged(boolean hasChanged) {
        super.onWindowFocusChanged(hasChanged);
        // TODO: 复制MP3数据
        File file = new File(getExternalCacheDir() + "/wordAudios");
        if (!file.exists()) {
            initMp3();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menuItem = menu.findItem(R.id.readChinese); // 朗读中文
        menuItem1 = menu.findItem(R.id.readTimes);  // 朗读次数
        menuItem2 = menu.findItem(R.id.reversed);   // 倒序播放
        menuItem3 = menu.findItem(R.id.autoRestart);// 自动重播
        menuItem4 = menu.findItem(R.id.floatWindow);// 悬浮窗开关
        menuItem5 = menu.findItem(R.id.chooseVoice);// 选择发音人
        sharedPreferences = getSharedPreferences("read", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();      //获取编辑器
        flag = sharedPreferences.getBoolean("read", false);
        if (flag == true) {
            menuItem.setIcon(R.mipmap.not_chinese);
            menuItem.setTitle(R.string.not_read_chinese);
        } else {
            menuItem.setIcon(R.mipmap.chinese);
            menuItem.setTitle(R.string.read_chinese);
        }
        needPlayChinese = !flag;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.readChinese:
                flag = sharedPreferences.getBoolean("read", false);
                if (flag == true) {
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

    private void initView() {
        currentWord = findViewById(R.id.current_word);
        currentChinese = findViewById(R.id.current_chinese);
        allWordTextView = findViewById(R.id.allWords);
        mProgressBarHorizontal = findViewById(R.id.progressBarHorizontal);
        speechButton = findViewById(R.id.speech);
        mCloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
        mCloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);
    }

    private void initMp3() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buildProgressDialog(R.string.loading);
                    ReadFile.unZip(MainActivity.this, "wordAudios.zip", getExternalCacheDir() + "/wordAudios", true);
                    cancelProgressDialog();
                    showTip(getString(R.string.init_complete));
                } catch (Exception e) {
                    e.printStackTrace();
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
                    }
            }
        });
    }

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
        SpinnerAdapter adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, array);
        //给Spinner设置适配器
        spinner.setAdapter(adapter);
    }

    private void initWordsHashMap() {
        int location = 0;
        while (m.find()) {
            //LogUtils.e("look", location + "");
            wordsHashMap.put(m.group(1) + "|" + m.group(2), location++);
        }
        allWordNum = location;  //将单词总数返回给allWordNum
    }

    private int selectedNum = 0;

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
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里

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
                // TODO: 播放完成监听
                //showTip("缓存完成");
                if (!pause)
                    //playChinese();
                    playNextWord();
            } else if (error != null) {
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
     *
     * @return
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
            /**
             * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
             * 开发者如需自定义参数，请参考在线合成参数设置
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

    private int speak(String texts) {
        // 移动数据分析，收集开始合成事件
        FlowerCollector.onEvent(MainActivity.this, "tts_play");
        setParam();
        String path = getApplicationContext().getCacheDir() + "/tts.ogg";
        //int code = mTts.synthesizeToUri(texts, path, mTtsListener);
        int code = mTts.startSpeaking(texts, mTtsListener);
        /*if (code != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + code);
        }*/
        return code;
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
        speak(content[1].replaceAll("[a-zA-z\\&]+\\.", ""));
    }

    /**
     * 查找单词函数
     * 参数：待查找的单词位置
     * 返回值：单词的json字符串和单词释义组成的String数组
     *
     * @param location
     * @return
     */
    private String[] getTargetWord(int location) {
        Object word = null, count;
        Iterator iter = wordsHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            word = entry.getKey();
            count = entry.getValue();
            if (Integer.parseInt(count.toString()) == location) break;
        }
        final String mWord = word.toString().split("\\|")[0];
        final String mChinese = word.toString().split("\\|")[1];
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentWord.setText(mWord);
                currentChinese.setText(mChinese);
                if (openFloatWindow) {
                    FloatView.textView.setText(mWord + "  " + mChinese);
                }
            }
        });
        if(inBg)
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
        if (!reversed) {
            mProgressBarHorizontal.setProgress((int) (targetLocation / 100.0 * 100) + 1);
        } else {
            mProgressBarHorizontal.setProgress((int) ((100 - targetLocation) / 100.0 * 100) + 1);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!reversed) {
                    if (targetLocation < allWordNum) {
                        content = getTargetWord(targetLocation++);
                        String wordName = content[0];
                        //LogUtils.e("content", wordName);
                        if (wordName.equals("null")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "无法获取单词读音链接，请检查网络", Toast.LENGTH_SHORT).show();
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
                    if (targetLocation >= 0) {
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


    /**
     * 播放指定文件地址的音频
     *
     * @param word
     */
    public void play(String word) {
        if (mTts.isSpeaking()) {
            mTts.stopSpeaking();
        }
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
                    } else if (count >= times) {
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
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        if (mTts.isSpeaking()) {
            mTts.stopSpeaking();
        }
    }

    /**
     * 继续播放当前单词
     */
    private void continuePlay() {
        if (mediaPlayer != null) {
            if (!reversed) {
                targetLocation--;
            } else {
                targetLocation++;
            }
            playNextWord();
        }
    }

    /**
     * 切换到上一个单词
     */
    public void previous(View v) {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            pausePlay();
        if (!reversed) {
            targetLocation -= 2;
        } else {
            targetLocation += 2;
        }
        if (targetLocation < 0) targetLocation = 0;
        pause = false;
        playNextWord();
    }

    /**
     * 切换到下一个单词
     */
    public void next(View v) {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            pausePlay();
        if (targetLocation > allWordNum) targetLocation = allWordNum;
        pause = false;
        playNextWord();
    }

    /**
     * 暂停开始按钮
     */
    public void speech(View v) {
        buttonFunction();
    }

    private void buttonFunction() {
        pause = !pause;
        if (!reversed) {
            if (!isPlaying) { // 没有在播放
                isPlaying = true;
                speechButton.setText(R.string.pause);
                if (targetLocation < allWordNum) {
                    if (targetLocation <= 0)
                        playNextWord();
                    else
                        continuePlay();
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
                if (targetLocation > 0) {
                    if (targetLocation >= allWordNum - 1)
                        playNextWord();
                    else
                        continuePlay();
                } else {
                    targetLocation = allWordNum;
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
    }

    /**
     * 双击返回键退出
     *
     * @param keyCode
     * @param event
     * @return
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
        notification = new NotificationCompat.Builder(context)
                /**设置通知左边的大图标**/
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.icon))
                /**设置通知右边的小图标**/
                .setSmallIcon(R.mipmap.icon)
                /**通知首次出现在通知栏，带上升动画效果的**/
                .setTicker("通知来了")
                /**设置通知的标题**/
                .setContentTitle(title)
                /**设置通知的内容**/
                .setContentText(msg)
                /**通知产生的时间，会在通知信息里显示**/
                .setWhen(System.currentTimeMillis())
                /**设置该通知优先级**/
                .setPriority(Notification.PRIORITY_HIGH)
                /**设置这个标志当用户单击面板就可以让通知将自动取消**/
                .setAutoCancel(true)
                /**设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)**/
                .setOngoing(true)
                /**向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：**/
                //.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        /**发起通知**/
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
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            LogUtils.e("permission", "权限请求");
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
        inBg = false;
        //移动数据统计分析
        FlowerCollector.onResume(MainActivity.this);
        FlowerCollector.onPageStart(TAG);

    }

    @Override
    protected void onPause() {
        inBg = true;
        //移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(MainActivity.this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        /*if (textToSpeech != null)
            textToSpeech.shutdown();*/
        if (mediaPlayer != null) {
            //mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (mFloatView != null) {
            if (mFloatView.isShow())
                mFloatView.close();
            mFloatView = null;
        }
        notification = null;
        FloatView.textView = null;
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
        super.onDestroy();
    }

    private final BroadcastReceiver headSetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
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
            if (action.equals("ok")) {
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
            } else if (action.equals("restart")) {
                LogUtils.i("MediaButtonReceiverMain","重启");
                isPlaying = false;
                if (!reversed) {
                    targetLocation = 0;
                    isPlaying = false;
                    pause = true;
                    buttonFunction();
                } else {
                    targetLocation = allWordNum - 1;
                    isPlaying = false;
                    pause = true;
                    buttonFunction();
                }
            } else if (action.equals("previous")) {
                LogUtils.i("MediaButtonReceiverMain", "previousMain");
                previous(getCurrentFocus());
            } else if (action.equals("next")) {
                next(getCurrentFocus());
                LogUtils.i("MediaButtonReceiverMain", "NextMain");
            }
        }
    }
}
