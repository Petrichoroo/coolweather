package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {  //总的实例类来引用之前创建的各个实体类
    public String status;   //返回的JSON数据中还会包含一项status数据 成功返回ok 失败则会返回具体的原因
    public AQI aqi;
    public Basic basic;
    public Now now;
    public Suggestion suggestion;
    //返回的JSON数据格式中 daily_forecast里包含的是一个数组 其每一项都代表着未来一天的天气信息
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
