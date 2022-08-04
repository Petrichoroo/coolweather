package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    //JSON中的一些字段可能不太适合直接作为Java字段来命名 使用@SerializedName注解的方式来让JSON字段和Java字段之间建立映射关系
    @SerializedName("city")
    public String cityName; //城市名
    @SerializedName("id")
    public String weatherId;
    public Update update;   //天气的更新时间

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}
