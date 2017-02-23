package org.zky.tool.magnetsearch.search;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.zky.tool.magnetsearch.BaseThemeActivity;
import org.zky.tool.magnetsearch.R;
import org.zky.tool.magnetsearch.constants.UrlConstants;
import org.zky.tool.magnetsearch.utils.GetRes;
import org.zky.tool.magnetsearch.utils.recycler.MyAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseThemeActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private static final String TAG = "MainActivity";

    @BindView(R.id.iv_menu)
    ImageView ivMenu;

    @BindView(R.id.card)
    CardView card;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.nav_view)
    NavigationView navView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.pb_loading)
    ProgressBar pbLoading;
    @BindView(R.id.et_search)
    EditText etSearch;
    @BindView(R.id.iv_delete)
    ImageView ivDelete;


    private Retrofit retrofit;

    private List<SearchEntity> list = new ArrayList<>();

    private MyAdapter<SearchEntity> adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();

    }

    private void initView() {
        //drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this);

        ivMenu.setOnClickListener(this);
        ivDelete.setOnClickListener(this);

        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ivDelete.setVisibility(View.VISIBLE);
                    ivMenu.setImageResource(R.drawable.ic_arrow_back_black_24dp);

                } else {
                    ivDelete.setVisibility(View.GONE);
                    GetRes.inputMethodToggle(false);
                }

            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())) {
                    query(v.getText().toString());
                }
                return true;
            }
        });


        recyclerView.setAdapter(adapter = new SearchAdapter(this, list, R.layout.item_recycler_view));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void query(String key) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .client(genericClient())
                    .baseUrl(UrlConstants.BTSO_SEARCH_URL)
                    .addConverterFactory(SearchConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
        }

        Observable<List<SearchEntity>> observable = retrofit.create(SearchSerivce.class).getHtml(key);
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<List<SearchEntity>>() {
                    @Override
                    public void onStart() {
                        list.clear();
                        adapter.notifyDataSetChanged();

                        pbLoading.setVisibility(View.VISIBLE);
                        ivMenu.callOnClick();
                    }

                    @Override
                    public void onCompleted() {
                        pbLoading.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(List<SearchEntity> searchEntities) {
                        Log.i(TAG, "onNext: data\n" + searchEntities);
                        adapter.addDatas(searchEntities);
                    }
                });
    }

    public static OkHttpClient genericClient() {

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request()
                                .newBuilder()
                                .addHeader("host", "btso.pw")
                                .addHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
                                .build();
                        return chain.proceed(request);
                    }

                })
                .build();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Intent intent;
        switch (itemId) {
            case R.id.nav_favorites:

                break;
            case R.id.nav_history:

                break;
            case R.id.nav_settings:

                break;
            case R.id.nav_share:
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain"); //纯文本
                intent.putExtra(Intent.EXTRA_SUBJECT, GetRes.getString(R.string.share));
                intent.putExtra(Intent.EXTRA_TEXT, GetRes.getString(R.string.share_content));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(intent, GetRes.getString(R.string.share)));

                break;
            case R.id.nav_send:
                try {
                    intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + GetRes.getString(R.string.email)));
                    intent.putExtra(Intent.EXTRA_SUBJECT, GetRes.getString(R.string.email_subject));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {

                    Snackbar.make(findViewById(R.id.activity_main), GetRes.getString(R.string.no_email_app), Snackbar.LENGTH_LONG).setAction(GetRes.getString(R.string.i_know), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    }).show();

                }

                break;

        }
        drawerLayout.closeDrawer(Gravity.LEFT);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_delete:
                etSearch.setText("");
                break;
            case R.id.iv_menu:
                if (etSearch.isFocused()) {
                    ivMenu.setImageResource(R.drawable.ic_menu_black_24dp);
                    recyclerView.requestFocus();
                } else {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }

                break;

        }
    }
}
