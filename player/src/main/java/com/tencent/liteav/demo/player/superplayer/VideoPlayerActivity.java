package com.tencent.liteav.demo.player.superplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.liteav.demo.player.R;
import com.tencent.liteav.demo.play.SuperPlayerConst;
import com.tencent.liteav.demo.play.SuperPlayerGlobalConfig;
import com.tencent.liteav.demo.play.SuperPlayerModel;
import com.tencent.liteav.demo.play.SuperPlayerView;
import com.tencent.liteav.demo.play.v3.SuperPlayerVideoId;
import com.tencent.rtmp.TXLiveConstants;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by liyuejiao on 2018/7/3.
 * 超级播放器主Activity
 */
public class VideoPlayerActivity extends Activity implements
        SuperPlayerView.OnSuperPlayerViewCallback {
    // 新手引导的标记
    private static final String SHARE_PREFERENCE_NAME = "tx_super_player_guide_setting";
    private static final String KEY_GUIDE_ONE = "is_guide_one_finish";
    private static final String KEY_GUIDE_TWO = "is_guide_two_finish";
    private static final String TAG = "VideoPlayerActivity";

    private Context mContext;
    //超级播放器View
    private SuperPlayerView mSuperPlayerView;
    //进入默认播放的视频
    private int DEFAULT_APP_ID = 1252463788;
    private RelativeLayout mRlMaskOne, mRlMaskTwo;
    private TextView mTvBtnOne, mTvBtnTwo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        mContext = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkPermission();
        initView();
        initData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 100);
            }
        }
    }

    private void initView() {
        mSuperPlayerView = (SuperPlayerView) findViewById(R.id.p_superVodPlayerView);
        mSuperPlayerView.setPlayerViewCallback(this);
        initNewGuideLayout();
    }

    /**
     * 初始化新手引导布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initNewGuideLayout() {
        mRlMaskOne = (RelativeLayout) findViewById(R.id.p_small_rl_mask_one);
        mRlMaskOne.setOnTouchListener(new View.OnTouchListener() { // 拦截事件
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mRlMaskTwo = (RelativeLayout) findViewById(R.id.p_small_rl_mask_two);
        mRlMaskTwo.setOnTouchListener(new View.OnTouchListener() { // 拦截事件
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mTvBtnOne = (TextView) findViewById(R.id.p_small_tv_btn1);
        mTvBtnTwo = (TextView) findViewById(R.id.p_small_tv_btn2);

        final SharedPreferences s = SharePreferenceUtils.newInstance(this, SHARE_PREFERENCE_NAME);
        boolean isFinishOne = SharePreferenceUtils.getBoolean(s, KEY_GUIDE_ONE);
        boolean isFinishTwo = SharePreferenceUtils.getBoolean(s, KEY_GUIDE_TWO);

        if (isFinishOne) {
            mRlMaskOne.setVisibility(GONE);
            if (isFinishTwo) {
                //ignore
            } else {
                mRlMaskTwo.setVisibility(VISIBLE);
            }
        } else {
            mRlMaskOne.setVisibility(VISIBLE);
            mRlMaskTwo.setVisibility(GONE);
        }

        mTvBtnOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRlMaskOne.setVisibility(GONE);
                mRlMaskTwo.setVisibility(VISIBLE);
                SharePreferenceUtils.putBoolean(s, KEY_GUIDE_ONE, true);
                SharePreferenceUtils.putBoolean(s, KEY_GUIDE_TWO, false);
            }
        });
        mTvBtnTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRlMaskOne.setVisibility(GONE);
                mRlMaskTwo.setVisibility(GONE);
                SharePreferenceUtils.putBoolean(s, KEY_GUIDE_ONE, true);
                SharePreferenceUtils.putBoolean(s, KEY_GUIDE_TWO, true);
            }
        });
    }

    private void initData() {
        initSuperVodGlobalSetting();
        VideoModel videoModel = new VideoModel();
        videoModel.title = "测试视频";
        videoModel.videoURL = "http://aliyuncdnct.inter.iqiyi.com/videos/v0/20191129/15/91/dee0bd77af731e45c3eafbb5fb580534.f4v?key=0c81e23151f619148602a6af95d61c348&dis_k=f9f1cf3bb16e91eeff340a1cfe1b6a25&dis_t=1575982378&dis_dz=CT-ShanDong_QingDao&dis_st=103&src=iqiyi.com&dis_hit=0&uuid=3a38a81e-5def952a-fe&qd_vipres=0&qd_stert=0&qd_vipdyn=0&qd_p=3a38a81e&qd_tvid=9669691800&qd_src=01012001010000000000&qd_aid=248443601&qd_index=1&qd_vip=0&qd_ip=3a38a81e&qd_uid=0&qd_k=3e3f9bcafe273ff2f4b6f280d10be795&qd_tm=1575982378666";
        videoModel.multiVideoURLs = new ArrayList<>();
        videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("蓝光", "http://aliyuncdnct.inter.iqiyi.com/videos/v0/20191129/15/91/dee0bd77af731e45c3eafbb5fb580534.f4v?key=0c81e23151f619148602a6af95d61c348&dis_k=f9f1cf3bb16e91eeff340a1cfe1b6a25&dis_t=1575982378&dis_dz=CT-ShanDong_QingDao&dis_st=103&src=iqiyi.com&dis_hit=0&uuid=3a38a81e-5def952a-fe&qd_vipres=0&qd_stert=0&qd_vipdyn=0&qd_p=3a38a81e&qd_tvid=9669691800&qd_src=01012001010000000000&qd_aid=248443601&qd_index=1&qd_vip=0&qd_ip=3a38a81e&qd_uid=0&qd_k=3e3f9bcafe273ff2f4b6f280d10be795&qd_tm=1575982378666"));
        videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("超清", "http://jcloudcdnct.inter.iqiyi.com/videos/v0/20191129/b7/ae/60697bba7c71c60f02a2ff4ac098e593.f4v?key=0431d0dcb570c3346602a6af95d61c348&dis_k=2e4dd4424532e9a9b1750c54447e05d9&dis_t=1575982378&dis_dz=CT-ShanDong_QingDao&dis_st=103&src=iqiyi.com&dis_hit=0&uuid=3a38a81e-5def952a-fa&qd_vipres=0&qd_stert=0&qd_vipdyn=0&qd_p=3a38a81e&qd_tvid=9669691800&qd_src=01012001010000000000&qd_aid=248443601&qd_index=1&qd_vip=0&qd_ip=3a38a81e&qd_uid=0&qd_k=3e3f9bcafe273ff2f4b6f280d10be795&qd_tm=1575982378666"));
        videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("高清", "http://baiducdnct.inter.iqiyi.com/videos/v0/20191129/5e/ac/bb373f5a4a1ee4bef6bf35690a2148e9.f4v?key=04b98c12268a34985b6db646164158326&dis_k=1362daffadf079b9ef39f2915d048139&dis_t=1575982379&dis_dz=CT-ShanDong_QingDao&dis_st=103&src=iqiyi.com&dis_hit=0&uuid=3a38a81e-5def952b-fb&qd_vipres=0&qd_stert=0&qd_vipdyn=0&qd_p=3a38a81e&qd_tvid=9669691800&qd_src=01012001010000000000&qd_aid=248443601&qd_index=1&qd_vip=0&qd_ip=3a38a81e&qd_uid=0&qd_k=3e3f9bcafe273ff2f4b6f280d10be795&qd_tm=1575982378666"));
        videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("流畅", "http://jcloudcdnct.inter.iqiyi.com/videos/v0/20191129/bb/7c/86ffa1afad1cc88856ed1e440e08b869.f4v?key=01d6e9cfc6908caf644a3e03430c674cf&dis_k=e09da488d57fa4e29b2f3b2dc8eb76df&dis_t=1575982380&dis_dz=CT-ShanDong_QingDao&dis_st=103&src=iqiyi.com&dis_hit=0&uuid=3a38a81e-5def952c-fc&qd_vipres=0&qd_stert=0&qd_vipdyn=0&qd_p=3a38a81e&qd_tvid=9669691800&qd_src=01012001010000000000&qd _aid=248443601&qd_index=1&qd_vip=0&qd_ip=3a38a81e&qd_uid=0&qd_k=3e3f9bcafe273ff2f4b6f280d10be795&qd_tm=1575982378666"));
        playVideoModel(videoModel);
    }

    /**
     * 初始化超级播放器全局配置
     */
    private void initSuperVodGlobalSetting() {
        SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
        // 开启悬浮窗播放
        prefs.enableFloatWindow = true;
        // 设置悬浮窗的初始位置和宽高
        SuperPlayerGlobalConfig.TXRect rect = new SuperPlayerGlobalConfig.TXRect();
        rect.x = 0;
        rect.y = 0;
        rect.width = 810;
        rect.height = 540;
        prefs.floatViewRect = rect;
        // 播放器默认缓存个数
        prefs.maxCacheItem = 5;
        // 设置播放器渲染模式
        prefs.enableHWAcceleration = true;
        prefs.renderMode = TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSuperPlayerView.getPlayState() == SuperPlayerConst.PLAYSTATE_PLAY) {
            Log.i(TAG, "onResume state :" + mSuperPlayerView.getPlayState());
            mSuperPlayerView.onResume();
            if (mSuperPlayerView.getPlayMode() == SuperPlayerConst.PLAYMODE_FLOAT) {
                mSuperPlayerView.requestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause state :" + mSuperPlayerView.getPlayState());
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSuperPlayerView.release();
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.resetPlayer();
        }
    }

    private void playVideoModel(VideoModel videoModel) {
        final SuperPlayerModel superPlayerModelV3 = new SuperPlayerModel();
        superPlayerModelV3.appId = videoModel.appid;

        if (!TextUtils.isEmpty(videoModel.videoURL)) {
            superPlayerModelV3.title = videoModel.title;

            superPlayerModelV3.multiURLs = new ArrayList<>();
            if (videoModel.multiVideoURLs != null) {
                for (VideoModel.VideoPlayerURL modelURL : videoModel.multiVideoURLs) {
                    superPlayerModelV3.multiURLs.add(new SuperPlayerModel.SuperPlayerURL(modelURL.url, modelURL.title));
                }
            }
        } else if (!TextUtils.isEmpty(videoModel.fileid)) {
            superPlayerModelV3.videoId = new SuperPlayerVideoId();
            superPlayerModelV3.videoId.fileId = videoModel.fileid;
        }
        mSuperPlayerView.playWithModel(superPlayerModelV3);
    }

    /**
     * 悬浮窗播放
     */
    private void showFloatWindow() {
        if (mSuperPlayerView.getPlayState() == SuperPlayerConst.PLAYSTATE_PLAY) {
            mSuperPlayerView.requestPlayMode(SuperPlayerConst.PLAYMODE_FLOAT);
        } else {
            mSuperPlayerView.resetPlayer();
            finish();
        }
    }

    @Override
    public void onStartFullScreenPlay() {
        // 隐藏其他元素实现全屏
    }

    @Override
    public void onStopFullScreenPlay() {
        // 恢复原有元素
    }

    @Override
    public void onClickFloatCloseBtn() {
        // 点击悬浮窗关闭按钮，那么结束整个播放
        mSuperPlayerView.resetPlayer();
        finish();
    }

    @Override
    public void onClickSmallReturnBtn() {
        // 点击小窗模式下返回按钮，开始悬浮播放
        showFloatWindow();
    }

    @Override
    public void onStartFloatWindowPlay() {
        // 开始悬浮播放后，直接返回到桌面，进行悬浮播放
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }
}
