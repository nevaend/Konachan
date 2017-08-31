package com.ess.konachan.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ess.konachan.R;
import com.ess.konachan.adapter.RecyclerPostAdapter;
import com.ess.konachan.bean.ImageBean;
import com.ess.konachan.bean.MsgBean;
import com.ess.konachan.bean.ThumbBean;
import com.ess.konachan.global.Constants;
import com.ess.konachan.global.GlideConfig;
import com.ess.konachan.http.OkHttp;
import com.ess.konachan.http.ParseHtml;
import com.ess.konachan.ui.activity.MainActivity;
import com.ess.konachan.ui.activity.SearchActivity;
import com.ess.konachan.utils.UIUtils;
import com.ess.konachan.view.GridDividerItemDecoration;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PostFragment extends Fragment {

    private final static long LOAD_MORE_INTERVAL = 5000;

    private MainActivity mActivity;

    private View mRootView;
    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mRvPosts;
    private GridLayoutManager mLayoutManager;
    private RecyclerPostAdapter mPostAdapter;
    private View mLayoutLoadResult;
    private ImageView mIvLoadFailed;
    private ImageView mIvLoading;

    private int mCurrentPage;
    private ArrayList<String> mCurrentTagList;
    private boolean mIsLoadingMore = false;
    private boolean mLoadMoreAgain = true;

    private Handler mHandler = new Handler();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) context;
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_post, container, false);
        initToolBarLayout();
        initSwipeRefreshLayout();
        initRecyclerView();
        initLoadingView();
        mCurrentPage = 1;
        mCurrentTagList = new ArrayList<>();
        getNewPosts(mCurrentPage);
        return mRootView;
    }

    private long lastClickTime = 0;

    private void initToolBarLayout() {
        Toolbar toolbar = (Toolbar) mRootView.findViewById(R.id.tool_bar);
        mActivity.setSupportActionBar(toolbar);
        DrawerLayout drawerLayout = mActivity.getDrawerLayout();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                mActivity,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toolbar.setNavigationIcon(R.drawable.ic_nav_drawer);

        //双击返回顶部
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastClickTime <= 0) {
                    lastClickTime = System.currentTimeMillis();
                } else {
                    long currentClickTime = System.currentTimeMillis();
                    if (currentClickTime - lastClickTime < 500) {
                        mRvPosts.scrollToPosition(0);
                    } else {
                        lastClickTime = currentClickTime;
                    }
                }
            }
        });

        //搜索
        ImageView ivSearch = (ImageView) mRootView.findViewById(R.id.iv_search);
        ivSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchIntent = new Intent(mActivity, SearchActivity.class);
                startActivityForResult(searchIntent, Constants.SEARCH_CODE);
            }
        });
    }

    private void initSwipeRefreshLayout() {
        mSwipeRefresh = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefresh.setRefreshing(true);
        //下拉刷新
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getNewPosts(1);
                if (mPostAdapter.getThumbList().isEmpty()) {
                    setLoadingGif();
                }
            }
        });
    }

    private void initRecyclerView() {
        mRvPosts = (RecyclerView) mRootView.findViewById(R.id.rv_post);
        mLayoutManager = new GridLayoutManager(mActivity, 2);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == mLayoutManager.getItemCount() - 1 ? 2 : 1;
            }
        });
        mRvPosts.setLayoutManager(mLayoutManager);

        ArrayList<ThumbBean> thumbList = new ArrayList<>();
        mPostAdapter = new RecyclerPostAdapter(mActivity, thumbList);
        mRvPosts.setAdapter(mPostAdapter);

        int spaceHor = UIUtils.dp2px(mActivity, 5);
        int spaceVer = UIUtils.dp2px(mActivity, 10);
        mRvPosts.addItemDecoration(new GridDividerItemDecoration(
                2, GridDividerItemDecoration.VERTICAL, spaceHor, spaceVer, true));

        //滑动加载更多
        mRvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
                int totalCount = mLayoutManager.getItemCount();
                if (totalCount != 1 && lastVisiblePosition >= totalCount - 11
                        && !mIsLoadingMore && mLoadMoreAgain) {
                    mIsLoadingMore = true;
                    mPostAdapter.changeToLoadMoreState();
                    loadMore();
                    Log.i("rrr", "load more");
                }
            }
        });
    }

    private void initLoadingView() {
        mLayoutLoadResult = mRootView.findViewById(R.id.layout_load_result);

        mIvLoading = (ImageView) mRootView.findViewById(R.id.iv_loading);
        GlideConfig.getInstance().loadGif(mActivity, R.drawable.gif_loading, mIvLoading);

        mIvLoadFailed = (ImageView) mRootView.findViewById(R.id.iv_load_failed);
        GlideConfig.getInstance().loadImage(mActivity, R.drawable.img_load_failed, mIvLoadFailed);

        setLoadingGif();
    }

    private void setLoadingGif() {
        mIvLoading.setVisibility(View.VISIBLE);
        mIvLoadFailed.setVisibility(View.GONE);
        mLayoutLoadResult.setVisibility(View.VISIBLE);
    }

    private void setLoadFailedImage() {
        mIvLoading.setVisibility(View.GONE);
        mIvLoadFailed.setVisibility(View.VISIBLE);
        mLayoutLoadResult.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.SEARCH_CODE) {
            if (data != null) {
                mPostAdapter.clear();
                if (!mSwipeRefresh.isRefreshing()) {
                    mSwipeRefresh.setRefreshing(true);
                }
                mCurrentPage = 1;
                mCurrentTagList.clear();
                mIsLoadingMore = false;
                setLoadingGif();

                OkHttp.getInstance().cancelAll();
                String searchTag = data.getStringExtra(Constants.SEARCH_TAG);
                switch (resultCode) {
                    case Constants.SEARCH_CODE_CHINESE:
                        getNameFromBaidu(searchTag);
                        break;
                    case Constants.SEARCH_CODE_TAGS:
                        mCurrentTagList.addAll(Arrays.asList(searchTag.split(",")));
                        getNewPosts(mCurrentPage);
                        break;
                    case Constants.SEARCH_CODE_ID:
                        mCurrentTagList.add("id:" + searchTag);
                        getNewPosts(mCurrentPage);
                }
            }
        }
    }

    private void getNewPosts(int page) {
        String url = OkHttp.getPostUrl(mActivity, page, mCurrentTagList);
        OkHttp.getInstance().connect(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (OkHttp.isNetworkProblem(e)) {
                    checkNetwork();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String html = response.body().string();
                try {
                    ArrayList<ThumbBean> thumbList = ParseHtml.getThumbList(html);
                    refreshThumbList(thumbList);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    getNoData();
                }
                response.close();
            }
        });
    }

    //搜索新内容或下拉刷新完成后刷新界面
    private void refreshThumbList(final ArrayList<ThumbBean> newList) {
        if (!mSwipeRefresh.isRefreshing()) {
            return;
        }

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!newList.isEmpty()) {
                    mLayoutLoadResult.setVisibility(View.GONE);
                    mPostAdapter.refreshDatas(newList);
                    mRvPosts.scrollToPosition(0);
                } else if (mPostAdapter.getThumbList().isEmpty()) {
                    setLoadFailedImage();
                }
                mSwipeRefresh.setRefreshing(false);
            }
        });
    }

    //滑动加载更多
    private void loadMore() {
        String url = OkHttp.getPostUrl(mActivity, ++mCurrentPage, mCurrentTagList);
        OkHttp.getInstance().connect(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (OkHttp.isNetworkProblem(e)) {
                    checkNetwork();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ArrayList<ThumbBean> thumbList = new ArrayList<>();
                try {
                    String html = response.body().string();
                    thumbList.addAll(ParseHtml.getThumbList(html));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } finally {
                    addMoreThumbList(thumbList);
                }
                response.close();
            }
        });
    }

    //加载更多完成后刷新界面
    private void addMoreThumbList(final ArrayList<ThumbBean> newList) {
        if (!mIsLoadingMore) {
            return;
        }

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPostAdapter.loadMoreDatas(newList);
                mPostAdapter.changeToNormalState();
                if (newList.isEmpty()) {
                    Toast.makeText(mActivity, R.string.no_more_load, Toast.LENGTH_SHORT).show();
                } else {
                    mIsLoadingMore = false;
                }
            }
        });
    }

    private void getNameFromBaidu(String searchTag) {
        String url = Constants.BASE_URL_BAIDU + searchTag;
        OkHttp.getInstance().connect(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (OkHttp.isNetworkProblem(e)) {
                    checkNetwork();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String html = response.body().string();
                String name = ParseHtml.getNameFromBaidu(html);
                if (!TextUtils.isEmpty(name)) {
                    String tag1 = "~" + name;
                    mCurrentTagList.add(tag1);

                    String[] split = name.split("_");
                    StringBuilder tag2 = new StringBuilder("~");
                    for (int i = split.length - 1; i >= 0; i--) {
                        tag2.append(split[i]).append("_");
                    }
                    tag2.replace(tag2.length() - 1, tag2.length(), "");
                    mCurrentTagList.add(tag2.toString());
                    getNewPosts(mCurrentPage);
                    Log.i("rrr", "name: " + name);
                } else {
                    getNoData();
                }
                response.close();
            }
        });
    }

    //获取到图片详细信息后收到的通知，obj 为 Json (String)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setImageBean(MsgBean msgBean) {
        if (msgBean.msg.equals(Constants.GET_IMAGE_DETAIL)) {
            String json = (String) msgBean.obj;
            ImageBean imageBean = ImageBean.getImageDetailFromJson(json);
            ArrayList<ThumbBean> thumbList = mPostAdapter.getThumbList();
            for (ThumbBean thumbBean : thumbList) {
                if (thumbBean.thumbUrl.equals(imageBean.posts[0].previewUrl)) {
                    if (thumbBean.imageBean == null) {
                        thumbBean.imageBean = imageBean;
                        Glide.with(mActivity).load(imageBean.posts[0].sampleUrl).submit();
                    }
                    break;
                }
            }
        }
    }

    //更换浏览模式后收到的通知，obj 为 null
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void toggleSearchMode(MsgBean msgBean) {
        if (msgBean.msg.equals(Constants.TOGGLE_SEARCH_MODE)) {
            OkHttp.getInstance().cancelAll();
            mPostAdapter.clear();
            if (!mSwipeRefresh.isRefreshing()) {
                mSwipeRefresh.setRefreshing(true);
            }
            mCurrentPage = 1;
            setLoadingGif();
            getNewPosts(mCurrentPage);
        }
    }

    //百度或K站搜所无结果
    private void getNoData() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPostAdapter.clear();
                mSwipeRefresh.setRefreshing(false);
                setLoadFailedImage();
            }
        });
    }

    //访问网络失败
    private void checkNetwork() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity, R.string.check_network, Toast.LENGTH_SHORT).show();
                mSwipeRefresh.setRefreshing(false);
                mIsLoadingMore = false;
                mLoadMoreAgain = false;
                if (mPostAdapter.getThumbList().isEmpty()) {
                    setLoadFailedImage();
                }

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mLoadMoreAgain = true;
                    }
                }, LOAD_MORE_INTERVAL);
            }
        });
    }

    public static PostFragment newInstance() {
        PostFragment fragment = new PostFragment();
        return fragment;
    }
}
