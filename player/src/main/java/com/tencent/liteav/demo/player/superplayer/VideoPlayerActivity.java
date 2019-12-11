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
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.demo.player.R;
import com.tencent.liteav.demo.player.common.utils.TCConstants;
import com.tencent.liteav.demo.player.server.GetVideoInfoListListener;
import com.tencent.liteav.demo.player.server.VideoDataMgr;
import com.tencent.liteav.demo.player.server.VideoInfo;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.liteav.demo.play.SuperPlayerConst;
import com.tencent.liteav.demo.play.SuperPlayerGlobalConfig;
import com.tencent.liteav.demo.play.SuperPlayerModel;
import com.tencent.liteav.demo.play.SuperPlayerView;
import com.tencent.liteav.demo.play.v3.SuperPlayerVideoId;
import com.tencent.rtmp.TXLiveBase;
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
        SuperVodListLoader.OnVodInfoLoadListener,
        SuperPlayerView.OnSuperPlayerViewCallback,
        TCVodPlayerListAdapter.OnItemClickLitener,
        SwipeRefreshLayout.OnRefreshListener {
    // 新手引导的标记
    private static final String SHARE_PREFERENCE_NAME = "tx_super_player_guide_setting";
    private static final String KEY_GUIDE_ONE = "is_guide_one_finish";
    private static final String KEY_GUIDE_TWO = "is_guide_two_finish";

    private static final String TAG = "VideoPlayerActivity";
    private static final int LIST_TYPE_LIVE = 0;
    private static final int LIST_TYPE_VOD = 1;

    private Context mContext;
    //超级播放器View
    private SuperPlayerView mSuperPlayerView;
    private TCVodPlayerListAdapter mVodPlayerListAdapter;
    //进入默认播放的视频
    private int DEFAULT_APPID = 1252463788;
    //获取点播信息接口
    private SuperVodListLoader mSuperVodListLoader;

    //上传文件列表
    private boolean mDefaultVideo;
    private String mVideoId;
    private GetVideoInfoListListener mGetVideoInfoListListener;

    private ArrayList<VideoModel> mLiveList;
    private ArrayList<VideoModel> mVodList;
    private int mDataType = LIST_TYPE_LIVE;
    private int mVideoCount;
    private boolean mVideoHasPlay;

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

        mDataType = mDefaultVideo ? LIST_TYPE_LIVE : LIST_TYPE_VOD;
        updateList(mDataType);
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

        mVodPlayerListAdapter = new TCVodPlayerListAdapter(this);
        mVodPlayerListAdapter.setOnItemClickLitener(this);

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
        mLiveList = new ArrayList<>();
        mVodList = new ArrayList<>();
        mDefaultVideo = getIntent().getBooleanExtra(TCConstants.PLAYER_DEFAULT_VIDEO, true);
        mSuperVodListLoader = new SuperVodListLoader();
        mSuperVodListLoader.setOnVodInfoLoadListener(this);

        initSuperVodGlobalSetting();

        mVideoHasPlay = false;

        mVideoCount = 0;

        TXLiveBase.setAppID("1253131631");
    }

    private void updateLiveList() {
        mLiveList.clear();
        mSuperVodListLoader.getLiveList(new SuperVodListLoader.OnListLoadListener() {
            @Override
            public void onSuccess(final ArrayList<VideoModel> superPlayerModelList) {

                VideoPlayerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mDataType != LIST_TYPE_LIVE) return;
                        mVodPlayerListAdapter.clear();
                        for (VideoModel videoModel :
                                superPlayerModelList) {
                            mVodPlayerListAdapter.addSuperPlayerModel(videoModel);
                            mLiveList.add(videoModel);
                        }
                        if (!mVideoHasPlay && !mLiveList.isEmpty()) {
                            if (mLiveList.get(0).appid > 0) {
                                TXLiveBase.setAppID("" + mLiveList.get(0).appid);
                            }
                            playVideoModel(mLiveList.get(0));
                            mVideoHasPlay = true;
                        }
                        mVodPlayerListAdapter.notifyDataSetChanged();

                    }
                });
            }

            @Override
            public void onFail(int errCode) {
                TXCLog.e(TAG, "updateLiveList error");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        });
        mVodPlayerListAdapter.notifyDataSetChanged();

    }

    private void updateVodList() {
        if (mDefaultVideo) {
            mVodList.clear();
            ArrayList<VideoModel> superPlayerModels = mSuperVodListLoader.loadDefaultVodList();
            mSuperVodListLoader.getVodInfoOneByOne(superPlayerModels);

        } else {
            mVideoId = getIntent().getStringExtra(TCConstants.PLAYER_VIDEO_ID);
            if (!TextUtils.isEmpty(mVideoId)) {
                playDefaultVideo(TCConstants.VOD_APPID, mVideoId);
                mVideoHasPlay = true;
            }

            mGetVideoInfoListListener = new GetVideoInfoListListener() {
                @Override
                public void onGetVideoInfoList(final List<VideoInfo> videoInfoList) {
                    if (mDataType != LIST_TYPE_VOD) return;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mVodPlayerListAdapter.clear();
                            mVodPlayerListAdapter.notifyDataSetChanged();
                            ArrayList<VideoModel> videoModels = VideoDataMgr.getInstance().loadVideoInfoList(videoInfoList);
                            if (videoModels != null && videoModels.size() != 0) {
                                mSuperVodListLoader.getVodInfoOneByOne(videoModels);
                            }
                        }
                    });
                }

                @Override
                public void onFail(int errCode) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "获取已上传的视频列表失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };

            mVodList.clear();
            VideoDataMgr.getInstance().setGetVideoInfoListListener(mGetVideoInfoListListener);
            VideoDataMgr.getInstance().getVideoList();
        }
    }

    private void playDefaultVideo(int appId, String fileId) {
        VideoModel videoModel = new VideoModel();
        videoModel.appid = appId;
        videoModel.fileid = fileId;
        videoModel.title = "小视频-特效剪辑";
        if (videoModel.appid > 0) {
            TXLiveBase.setAppID("" + videoModel.appid);
        }
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
        prefs.renderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
        prefs.playShiftDomain = "playtimeshift.live.myqcloud.com";//需要修改为自己的时移域名
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
        VideoDataMgr.getInstance().setGetVideoInfoListListener(null);
    }

    /**
     * 获取点播信息成功
     */
    @Override
    public void onSuccess(final VideoModel videoModel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mDataType != LIST_TYPE_VOD) return;
                mVodPlayerListAdapter.addSuperPlayerModel(videoModel);
                mVodList.add(videoModel);
            }
        });
    }

    /**
     * 获取点播信息失败
     *
     * @param errCode errCode
     */
    @Override
    public void onFail(int errCode) {
        TXCLog.i(TAG, "onFail errCode:" + errCode);
    }

    @Override
    public void onItemClick(int position, final VideoModel videoModel) {
        if (videoModel.appid > 0) {
            TXLiveBase.setAppID("" + videoModel.appid);
        }
        playVideoModel(videoModel);
    }

    private void playVideoModel(VideoModel videoModel) {
        final SuperPlayerModel superPlayerModelV3 = new SuperPlayerModel();
        superPlayerModelV3.appId = videoModel.appid;

        if (!TextUtils.isEmpty(videoModel.videoURL)) {
            superPlayerModelV3.title = videoModel.title;
            superPlayerModelV3.url = videoModel.videoURL;
            superPlayerModelV3.qualityName = "原画";

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

    private void updateList(int dataType) {

        mVodPlayerListAdapter.clear();
        switch (mDataType) {
            case LIST_TYPE_LIVE:
                if (mLiveList.isEmpty()) {
                    updateLiveList();
                } else {
                    for (VideoModel videoModel :
                            mLiveList) {
                        mVodPlayerListAdapter.addSuperPlayerModel(videoModel);
                    }
                }
                break;
            case LIST_TYPE_VOD:
                if (mVodList.isEmpty()) {
                    updateVodList();
                } else {
                    for (VideoModel videoModel :
                            mVodList) {
                        mVodPlayerListAdapter.addSuperPlayerModel(videoModel);
                    }
                }
                break;
        }

        mVodPlayerListAdapter.notifyDataSetChanged();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null || data.getExtras() == null || TextUtils.isEmpty(data.getExtras().getString("result"))) {
            return;
        }
        String result = data.getExtras().getString("result");
        if (requestCode == 200) {
        } else if (requestCode == 100) {
            // 二维码播放视频
            playNewVideo(result);
        }
    }

    private boolean isLivePlay(VideoModel videoModel) {
        String videoURL = videoModel.videoURL;
        if (TextUtils.isEmpty(videoModel.videoURL)) {
            return false;
        }
        if (videoURL.startsWith("rtmp://")) {
            return true;
        } else if ((videoURL.startsWith("http://") || videoURL.startsWith("https://")) && videoURL.contains(".flv")) {
            return true;
        } else {
            return false;
        }
    }

    private void playNewVideo(String result) {
        mVideoCount++;
        VideoModel videoModel = new VideoModel();
        videoModel.title = "测试视频" + mVideoCount;
        videoModel.videoURL = result;
        videoModel.placeholderImage = "http://xiaozhibo-10055601.file.myqcloud.com/coverImg.jpg";
        videoModel.appid = DEFAULT_APPID;
        if (!TextUtils.isEmpty(videoModel.videoURL) && videoModel.videoURL.contains("5815.liveplay.myqcloud.com")) {
            videoModel.appid = 1253131631;
            TXLiveBase.setAppID("1253131631");
            videoModel.multiVideoURLs = new ArrayList<>(3);
            videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("超清", videoModel.videoURL));
            videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("高清", videoModel.videoURL.replace(".flv", "_900.flv")));
            videoModel.multiVideoURLs.add(new VideoModel.VideoPlayerURL("标清", videoModel.videoURL.replace(".flv", "_550.flv")));
        }
        if (!TextUtils.isEmpty(videoModel.videoURL) && videoModel.videoURL.contains("3891.liveplay.myqcloud.com")) {
            videoModel.appid = 1252463788;
            TXLiveBase.setAppID("1252463788");
        }
        playVideoModel(videoModel);

        boolean needRefreshList;
        if (isLivePlay(videoModel)) {
            mLiveList.add(videoModel);
            needRefreshList = mDataType == LIST_TYPE_LIVE;
        } else {
            mVodList.add(videoModel);
            needRefreshList = mDataType == LIST_TYPE_VOD;
        }
        if (needRefreshList) {
            mVodPlayerListAdapter.addSuperPlayerModel(videoModel);
            mVodPlayerListAdapter.notifyDataSetChanged();
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

    @Override
    public void onRefresh() {
        if (mDefaultVideo) {
            return;
        }
        if (mDataType == LIST_TYPE_VOD) {
            mVodList.clear();
            VideoDataMgr.getInstance().getVideoList();
        } else {
            updateLiveList();
        }
    }
}
