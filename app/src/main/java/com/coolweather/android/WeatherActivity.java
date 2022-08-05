package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    public SwipeRefreshLayout swipeRefresh;
    public DrawerLayout drawerLayout;
    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //实现背景图和状态栏融合在一起的功能
        if (Build.VERSION.SDK_INT >= 21) {  //版本号大于或等于21 即5.0及以上系统才支持此功能
            View decorView = getWindow().getDecorView();    //得到当前活动的DecorView(activity窗口的根视图)
            //活动布局会显示在状态栏上
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);   //将状态栏设置成透明色
        }   //系统状态栏已经成为我们布局中的一部分(之前并没有为它留出空间)

        //初始化各种控件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.design_default_color_primary); //设置下拉刷新进度条的颜色
        drawerLayout = findViewById(R.id.drawer_layout);    //滑动菜单
        navButton = findViewById(R.id.nav_button);
        //尝试从本地缓存中读取数据(第一次肯定是没有缓存的)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);    //天气数据
        String bingPic = prefs.getString("bing_pic", null);   //必应每日一图url
        final String weatherId;
        //有缓存时直接解析天气数据
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //取出要查询的地方的weatherId
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.GONE); //将ScrollView隐藏 不然是一个空数据界面
            requestWeather(weatherId);
        }
        //有缓存时直接加载图片
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();  //没有缓存 调用loadBingPic()方法请求今日的必应背景图
        }
        //设置下拉刷新监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
        //点击navButton打开滑动菜单
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * 根据天气id请求城市天气数据
     */
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=097845e7558a4077953bd84a11022a0a";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);  //刷新事件结束 隐藏刷新进度条
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);    //将JSON数据转换成Weather对象
                runOnUiThread(new Runnable() {  //切换到主线程 进行UI操作
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {   //判断请求天气是否成功
                            //将返回的数据缓存到SharedPreferences中
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                                    (WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);   //显示内容
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);  //刷新事件结束 隐藏刷新进度条
                    }
                });
            }
        });
        loadBingPic();  //每次请求天气信息的时候同时也会刷新背景图片
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];  //2016-08-08 21:58 分割" "得到具体时间
        //Log.d("WeatherActivity", "updateTime = " + updateTime);
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);    //当前城市名
        titleUpdateTime.setText(updateTime);    //当前时间
        degreeText.setText(degree);     //实时温度
        weatherInfoText.setText(weatherInfo);   //天气状态
        forecastLayout.removeAllViews();    //移除其下的所有子视图 刷新
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout,
                    false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);    //时间
            infoText.setText(forecast.more.info);   //天气状态
            maxText.setText(forecast.temperature.max);  //最高温度
            minText.setText(forecast.temperature.min);  //最低温度
            forecastLayout.addView(view);   //动态添加子View
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议:" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);  //使ScrollView重新可见
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String address = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();    //得到今日的必应背景图链接
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                        (WeatherActivity.this).edit();  //得到.editor对象
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {  //回到主线程更新UI
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}