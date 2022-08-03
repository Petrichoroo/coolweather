package com.coolweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;    //省列表
    private List<City> cityList;    //市列表
    private List<County> countyList;    //县列表
    private Province selectedProvince;  //选中的省份
    private City selectedCity;  //选中的城市
    private int currentLevel;   //当前选中的级别

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //第一次调用queryProvinces()方法 开始加载各个省份的数据并将其显示到界面上
        queryProvinces();
        //用户点击了某个省份 进入ListView的点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * 点击了某个省的时候会进入到ListView的onItemClick()方法中
             * */
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //判断当前所处的级别 即省或市 再分别去查询其下属区域
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(i); //得到当前选中的省份所对应的Province对象 用于其后的查询
                    queryCities();  //去查询市级数据
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(i); //得到当前选中的市级所对应的City对象 用于其后的查询
                    queryCounties();    //去查询县级数据
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            /**
             * 在返回按钮的点击事件里，会对当前ListView的列表级别进行判断
             * 如果当前是县级列表，那么就返回到市级列表 如果当前是市级列表，那么就返回到省级表列表
             * 当返回到省级列表时，返回按钮会自动隐藏
             */
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryProvinces() {
        //queryProvinces()方法中首先会将头布局的标题设置成中国，将返回按钮隐藏起来，因为省级列表无法再返回
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        /*  调用LitePal的查询接口来从数据库中读取省级数据，如果读取到了就直接将数据显示到界面上
            若没有读取到则组装出一个请求地址，然后调用queryFromServer()方法来从服务器上查询数据*/
        provinceList = LitePal.findAll(Province.class);

        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);   //将第1个item显示在listView的最上面一项
            currentLevel = LEVEL_PROVINCE;
        } else {
            /**
             * Android P全面禁止了非安全的http连接 若要使用非加密连接 需要配置network security config.xml*/
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询全国所有的市，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());  //将头布局的标题改为当前所处的市级
        backButton.setVisibility(View.VISIBLE); //使返回按钮可见
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged(); //通知ListView页面发生变化 即将市级单位呈现出来
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;  //修改当前所处级别
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询全国所有的县，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县的数据
     * queryFromServer()方法中会调用HttpUtil的sendOkHttpRequest()方法来向服务器发送请求，
     * 响应的数据会回调到onResponse()方法中，然后去调用Utility的handleProvincesResponse()方法来解析和处理服务器返回的数据，并存储到数据库中
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                /**
                 * 在解析和处理完数据之后，再次调用了queryProvinces()方法来重新加载省级数据
                 * 由于queryProvinces()方法牵扯到了UI操作，因此必须要在主线程中调用
                 * 这里借助了runOnUiThread()方法来实现从子线程切换到主线程
                 * 现在数据库中已经存在了数据，因此调用queryProvinces()就会直接将数据显示到界面上了
                 */
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 显示进度条对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);    //点击对话框外部区域是否允许对话框消失
        }
        progressDialog.show();
    }

    /**
     * 关闭进度条
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
