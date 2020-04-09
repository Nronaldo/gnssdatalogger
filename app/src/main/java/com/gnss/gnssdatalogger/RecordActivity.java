package com.gnss.gnssdatalogger;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GnssClock;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.adam.gpsstatus.GpsStatusProxy;
import com.gnss.gnssdatalogger.Constellations.GpsGalileoBdsGlonassQzssConstellation;
import com.gnss.gnssdatalogger.Constellations.GpsTime;
import com.gnss.gnssdatalogger.Constellations.Satellites.EpochMeasurement;
import com.gnss.gnssdatalogger.Constellations.Satellites.GalileoSatellite;
import com.gnss.gnssdatalogger.Constellations.Satellites.GpsSatellite;
import com.gnss.gnssdatalogger.Constellations.Satellites.QzssSatellite;
import com.gnss.gnssdatalogger.Nav.GpsNavigationConv;
import com.gnss.gnssdatalogger.RinexFileLogger.Rinex;
import com.gnss.gnssdatalogger.RinexFileLogger.RinexHeader;
import com.gnss.gnssdatalogger.RinexFileLogger.RinexNav;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.location.GnssMeasurementsEvent.Callback;

public class RecordActivity extends AppCompatActivity {

    private static final String TAG = RecordActivity.class.getSimpleName();


    private LocationManager mLocationManager;
    private Location mLocation;
    //GNSS信号
    private GpsStatusProxy gnssStatusStrength;


    private TextView textViewWelcome;
    private TextView textViewLatitude;
    private TextView textViewLongtitude;
    private TextView textViewAltitude;
    private TextView textViewApproX;
    private TextView textViewApproY;
    private TextView textViewApproZ;
    private TextView textViewBtnStartStop;
    private TextView textViewBtnScrollDetail;//详情页面
    private TextView textViewLog;


    private TextView textViewGPSTime;
    private TextView textViewGPSDate;

    private Chronometer chronometerTimer;
    private TextView textViewTotalGpsL1;
    private TextView textViewTotalGpsL5;
    private TextView textViewTotalGalileoE1;
    private TextView textViewTotalGalileoE5a;
    private TextView textViewTotalGlonass;
    private TextView textViewTotalQzssL1;
    private TextView textViewTotalQzssL5;
    private TextView textViewTotalBeidou;
    private ScrollView scrollViewLog;
    private LinearLayout linearLayoutGroupMenu;

    private Button BtnUpload;
    private Button BtnFile;
    private Button BtnSetting;

    private int totalGpsL1;
    private int totalGpsL5;
    private int totalGalileoE1;
    private int totalGalileoE5a;
    private int totalGlonass;
    private int totalBeidou;
    private int totalQzssL1;
    private int totalQzssL5;


    private boolean isRecord;
    private boolean isDetailView;

    private Handler handler;

    //观测值数据类
    private GpsGalileoBdsGlonassQzssConstellation sumConstellation;
    //导航电文类
    private GpsNavigationConv gpsNavigationConv;

    //rinex观测值问件的获取
    private Rinex rinex;
    //主要用来获取setting的信息，或者没有加载setting之后，获取之前初始化的信息，在constants里面有定义
    private SharedPreferences sharedPreferences;
    //GPS时间
    private GpsTime gpsTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);


        //经度纬度高  XYZ
        textViewLatitude = findViewById(R.id.record_latitude);
        textViewLongtitude = findViewById(R.id.record_longtitude);
        textViewAltitude = findViewById(R.id.record_altitude);
        textViewApproX = findViewById(R.id.record_approX);
        textViewApproY = findViewById(R.id.record_approY);
        textViewApproZ = findViewById(R.id.record_approZ);


        //欢迎使用
        textViewWelcome = findViewById(R.id.record_welcome);
        //开启或者结束
        textViewBtnStartStop = findViewById(R.id.record_btnStartStop);
        //计时
        chronometerTimer = findViewById(R.id.record_timer);
        chronometerTimer.setText(R.string.zero_time);
        //详情界面，即获取GNSS观测值文件的详细情况
        textViewBtnScrollDetail = findViewById(R.id.record_btnScrollDetail);

        //点击详细界面之后，可展示各个时刻出现的卫星数和卫星列表
        scrollViewLog = findViewById(R.id.record_logScroll);
        textViewLog = findViewById(R.id.record_log);



        textViewGPSTime = findViewById(R.id.record_time);//GPS时间时分秒
        textViewGPSDate = findViewById(R.id.record_date);//GPS时间年月日


        //文件
        BtnFile = findViewById(R.id.record_btnFile);
        //设置
        BtnSetting = findViewById(R.id.record_btnSetting);
        //上传设置，主要是将位置信息上传至服务器
        BtnUpload = findViewById(R.id.record_btnUpload);

        //各个系统卫星个数界面
        linearLayoutGroupMenu = findViewById(R.id.record_groupMenu);
        textViewTotalGpsL1 = findViewById(R.id.record_totalGpsL1);
        textViewTotalGpsL5 = findViewById(R.id.record_totalGpsL5);
        textViewTotalGalileoE1 = findViewById(R.id.record_totalGalileoE1);
        textViewTotalGalileoE5a = findViewById(R.id.record_totalGalileoE5a);
        textViewTotalGlonass = findViewById(R.id.record_totalGlonass);
        textViewTotalQzssL1 = findViewById(R.id.record_totalQzssL1);
        textViewTotalQzssL5 = findViewById(R.id.record_totalQzssL5);
        textViewTotalBeidou = findViewById(R.id.record_totalBeidou);


        //GNSS信号展示
        gnssStatusStrength = GpsStatusProxy.getInstance(this);
        gnssStatusStrength.register();

        //验证GNSS权限
        verifyGnssPermissions(this);
        //验证文件权限
        verifyStoragePermissions(this);


        //判断是否在记录rinex文件
        isRecord = false;
        //判断是否点击了详情界面
        isDetailView = false;
        //
        handler = new Handler();

        //这个是获取头文件信息
        sharedPreferences = getSharedPreferences(Constants.FILE_SETTING, 0);

        //初始化卫星个数
        initTotalSatellite();
        //点击事件
        BtnFile.setOnClickListener(new onClickEvent());
        BtnSetting.setOnClickListener(new onClickEvent());
        BtnUpload.setOnClickListener(new onClickEvent());
        textViewBtnStartStop.setOnClickListener(new onClickEvent());
        textViewBtnScrollDetail.setOnClickListener(new onClickEvent());

        //初始化观测值数据观测文件
        sumConstellation = new GpsGalileoBdsGlonassQzssConstellation();
        //初始化导航电文类
        gpsNavigationConv=new GpsNavigationConv(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        registerLocation();
        registerGnssMeasurements();
        registerGnssNavigationMessage();
    }

    private Callback gnssMeasurementsEvent = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);

            initTotalSatellite();

            GnssClock clock = eventArgs.getClock();

            gpsTime =new GpsTime(clock);

            sumConstellation.updateMeasurements(eventArgs);

            EpochMeasurement epochMeasurement = sumConstellation.getEpochMeasurement();

            //如果表明正在记录文件，则需要执行更新文件
            if(isRecord)
            {
                rinex.writeBody(epochMeasurement);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewLog.append("\n");
                        textViewLog.append(textViewGPSTime.getText() + "->>");
                        textViewLog.append(epochMeasurement.getSatellitePrnList().size() + ">>");
                        for(String prn :epochMeasurement.getSatellitePrnList())
                            textViewLog.append(prn);

                    }
                });
            }

            for (GpsSatellite gpsSatellite : epochMeasurement.getGpsSatelliteList()) {
                if (gpsSatellite.isHasC1())
                    totalGpsL1++;
                if (gpsSatellite.isHasC5())
                    totalGpsL5++;
            }
            for (QzssSatellite qzssSatellite : epochMeasurement.getQzssSatelliteList()) {
                if (qzssSatellite.isHasC1())
                    totalQzssL1++;
                if (qzssSatellite.isHasC5())
                    totalQzssL5++;
            }
            for (GalileoSatellite galileoSatellite : epochMeasurement.getGalileoSatelliteList()) {
                if (galileoSatellite.isHasC1())
                    totalGalileoE1++;
                if (galileoSatellite.isHasC5())
                    totalGalileoE5a++;
            }
            totalBeidou = epochMeasurement.getBdsSatelliteList().size();
            totalGlonass = epochMeasurement.getGlonassSatelliteList().size();

            handler.post(new Runnable() {
                @SuppressLint("DefaultLocale")
                @Override
                public void run() {
                    textViewTotalGpsL1.setText(String.valueOf(totalGpsL1));
                    textViewTotalGpsL5.setText(String.valueOf(totalGpsL5));
                    textViewTotalGalileoE1.setText(String.valueOf(totalGalileoE1));
                    textViewTotalGalileoE5a.setText(String.valueOf(totalGalileoE5a));
                    textViewTotalGlonass.setText(String.valueOf(totalGlonass));

                    textViewTotalQzssL1.setText(String.valueOf(totalQzssL1));
                    textViewTotalQzssL5.setText(String.valueOf(totalQzssL5));
                    textViewTotalBeidou.setText(String.valueOf(totalBeidou));
                    textViewGPSTime.setText(String.format("%02d:%02d:%02.2f", gpsTime.getHour(),gpsTime.getMinute(),gpsTime.getSecond()));
                    textViewGPSDate.setText(String.format("%02d/%02d/%02d", gpsTime.getYearSimplify(),gpsTime.getMonth(),gpsTime.getDay()));
                }
            });

        }

        @Override
        public void onStatusChanged(int status) {
            super.onStatusChanged(status);
        }
    };
    private GnssNavigationMessage.Callback gnssNavigationMessage = new GnssNavigationMessage.Callback() {
        @Override
        public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
            super.onGnssNavigationMessageReceived(event);

            if (isRecord&&sharedPreferences.getInt(Constants.KEY_NAV, Constants.DEF_RINEX_NAV)==Constants.Nav_Yes) {
                //卫星号
                int svid = event.getSvid();
                //原始数据
                byte[] rawData = event.getData();
                //
                int messageId = event.getMessageId();
                //
                int submessageId = event.getSubmessageId();
                int type = event.getType();

                gpsNavigationConv.onGpsNavMessageReported(svid, type, submessageId, rawData);

            }
        }

        @Override
        public void onStatusChanged(int status) {
            super.onStatusChanged(status);
        }
    };

    private LocationListener mLocationListener = new LocationListener() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(Location location) {

            if (location != null) {

                mLocation=location;
                try {
                    textViewLatitude.setText(String.format("%.5f", location.getLatitude()));

                    textViewLongtitude.setText(String.format("%.5f", location.getLongitude()));

                    if (location.hasAltitude()) {
                        textViewAltitude.setText(String.format("%.3f", location.getAltitude()));
                    }
                    //坐标转换
                    double[] xyz = Coordinate.WGS84LLAtoXYZ(location.getLatitude(), location.getLongitude(), location.getAltitude());

                    textViewApproX.setText(String.format("%.4f", xyz[0]));
                    textViewApproY.setText(String.format("%.4f", xyz[1]));
                    textViewApproZ.setText(String.format("%.4f", xyz[2]));
                } catch (Exception e) {
                    Log.d(TAG, "避免出现Location不为空值，但纬度经度为空值的情况");
                }

                try {
                    gnssStatusStrength.notifyLocation(location);
                } catch (Exception e) {
                    Log.d(TAG, "未获得定位结果");
                }
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void registerGnssMeasurements() {
        //Check Permission
        if (ActivityCompat.checkSelfPermission(RecordActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }
        mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEvent);
        Log.i(TAG, "Register callback -> measurementsEvent");
    }

    private void registerGnssNavigationMessage() {
        //Check Permission
        if (ActivityCompat.checkSelfPermission(RecordActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }
        mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessage);
        Log.i(TAG, "Register callback -> GnssNavigationMessage");
    }

    private void registerLocation() {
        //Check Permission
        if (ActivityCompat.checkSelfPermission(RecordActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }
        // 为获取地理位置信息时设置查询条件
        String bestProvider = mLocationManager.getBestProvider(getCriteria(), true);

        // 获取位置信息

        // 如果不设置查询要求，getLastKnownLocation方法传人的参数为LocationManager.GPS_PROVIDER

        assert bestProvider != null;
        mLocation = mLocationManager.getLastKnownLocation(bestProvider);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener);
    }

    private static Criteria getCriteria() {

        Criteria criteria = new Criteria();

        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细

        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // 设置是否要求速度

        criteria.setSpeedRequired(true);

        // 设置是否允许运营商收费

        criteria.setCostAllowed(false);

        // 设置是否需要方位信息

        criteria.setBearingRequired(true);

        // 设置是否需要海拔信息

        criteria.setAltitudeRequired(true);

        // 设置对电源的需求

        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        return criteria;

    }


    private void initTotalSatellite() {
        totalGpsL1 = 0;
        totalGpsL5 = 0;
        totalGalileoE1 = 0;
        totalGalileoE5a = 0;
        totalGlonass = 0;
        totalQzssL1 = 0;
        totalQzssL5 = 0;
        totalBeidou = 0;
    }

    /**
     * 在对sd卡进行读写操作之前调用这个方法 * Checks if the app has permission to write to device storage * If the app does not has permission then the user will be prompted to grant permissions
     */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static void verifyStoragePermissions(Activity activity) {    // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {        // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    private static final int REQUEST_ACCESS_FINE_LOCATION = 2;

    private void verifyGnssPermissions(Activity activity) {
        //看GNSS权限是否开启
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);

        }
    }


    private class onClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {

                case R.id.record_btnFile:
                        Log.i(TAG, "Click -> File (RinexFile)");
                        startActivity(
                                new Intent(RecordActivity.this, FileActivity.class)
                        );

                    break;
                case R.id.record_btnScrollDetail:

                    if (!isDetailView) {
                        textViewBtnScrollDetail.setTextColor(getColor(R.color.colorOrange));
                        scrollViewLog.setVisibility(View.VISIBLE);
                        linearLayoutGroupMenu.setVisibility(View.INVISIBLE);
                        isDetailView = true;
                        ObjectAnimator animatorLog = ObjectAnimator.ofFloat(scrollViewLog, View.ALPHA, 1f);
                        animatorLog.setDuration(1000);
                        animatorLog.setStartDelay(200);
                        animatorLog.start();
                    } else {
                        textViewBtnScrollDetail.setTextColor(getColor(R.color.colorGray));

                        isDetailView = false;
                        //动画效果
                        ObjectAnimator animatorLog = ObjectAnimator.ofFloat(scrollViewLog, View.ALPHA, 0f);
                        animatorLog.setDuration(200);
                        animatorLog.start();
                        animatorLog.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                scrollViewLog.setVisibility(View.INVISIBLE);
                                linearLayoutGroupMenu.setVisibility(View.VISIBLE);
                            }
                        });

                    }
                    break;
                case R.id.record_btnSetting:
                    Log.i(TAG, "Click -> Setting (RinexHeader)");
                    startActivity(
                            new Intent(RecordActivity.this, SettingActivity.class)
                    );
                    break;
                case R.id.record_btnStartStop:

                    if (!isRecord) {
                        animationClickStart();


                        startRecordRinex();

                        if(sharedPreferences.getInt(Constants.KEY_NAV, Constants.DEF_RINEX_NAV)==Constants.Nav_Yes)
                        {
                            startRecordRinexNav();
                        }
                            isRecord = true;

                    } else {
                        stopRecordRinex();
                        if(sharedPreferences.getInt(Constants.KEY_NAV, Constants.DEF_RINEX_NAV)==Constants.Nav_Yes)
                        {
                            stopRecordRinexNav();
                        }
                        animationClickStop();
                        isRecord = false;
                    }
                case R.id.record_btnUpload:
                    break;
            }
        }

    }

    private void animationClickStart() {
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textViewBtnStartStop.setBackgroundResource(R.drawable.bg_btn_red);
        textViewBtnStartStop.setText(R.string.stop);

        textViewLog.setText(R.string.searching);
        // 将计时器清零
        chronometerTimer.setBase(SystemClock.elapsedRealtime());
        //开始计时
        chronometerTimer.start();
        chronometerTimer.setText(R.string.zero_time);
        //记录时间的秒数
        final int[] recordSecond = {0};

        chronometerTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                recordSecond[0] = recordSecond[0] + 1;
                chronometer.setText(FormatMiss(recordSecond[0]));
            }
        });
    }

    private void animationClickStop() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textViewBtnStartStop.setBackgroundResource(R.drawable.bg_btn_lightgreen);
        textViewBtnStartStop.setText(R.string.start);
        chronometerTimer.stop();

    }

    //用来转换计时器的时间
    public static String FormatMiss(int miss) {
        String hh = miss / 3600 > 9 ? miss / 3600 + "" : "0" + miss / 3600;
        String mm = (miss % 3600) / 60 > 9 ? (miss % 3600) / 60 + "" : "0" + (miss % 3600) / 60;
        String ss = (miss % 3600) % 60 > 9 ? (miss % 3600) % 60 + "" : "0" + (miss % 3600) % 60;
        return hh + ":" + mm + ":" + ss;
    }


    private void startRecordRinex() {
        rinex = new Rinex(getApplicationContext(), sharedPreferences.getInt(Constants.KEY_RINEX_VER, Constants.DEF_RINEX_VER));
        rinex.writeHeader(new RinexHeader(
                sharedPreferences.getString(Constants.KEY_MARK_NAME, Constants.DEF_MARK_NAME),
                sharedPreferences.getString(Constants.KEY_MARK_TYPE, Constants.DEF_MARK_TYPE),
                sharedPreferences.getString(Constants.KEY_OBSERVER_NAME, Constants.DEF_OBSERVER_NAME),
                sharedPreferences.getString(Constants.KEY_OBSERVER_AGENCY_NAME, Constants.DEF_OBSERVER_AGENCY_NAME),
                sharedPreferences.getString(Constants.KEY_RECEIVER_NUMBER, Constants.DEF_RECEIVER_NUMBER),
                sharedPreferences.getString(Constants.KEY_RECEIVER_TYPE, Constants.DEF_RECEIVER_TYPE),
                sharedPreferences.getString(Constants.KEY_RECEIVER_VERSION, Constants.DEF_RECEIVER_VERSION),
                sharedPreferences.getString(Constants.KEY_ANTENNA_NUMBER, Constants.DEF_ANTENNA_NUMBER),
                sharedPreferences.getString(Constants.KEY_ANTENNA_TYPE, Constants.DEF_ANTENNA_TYPE),
                Double.parseDouble(sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_EAST, Constants.DEF_ANTENNA_ECCENTRICITY_EAST)),
                Double.parseDouble(sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_NORTH, Constants.DEF_ANTENNA_ECCENTRICITY_NORTH)),
                Double.parseDouble(sharedPreferences.getString(Constants.KEY_ANTENNA_HEIGHT, Constants.DEF_ANTENNA_HEIGHT)),
                textViewApproX.getText().toString(),
                textViewApproY.getText().toString(),
                textViewApproZ.getText().toString(),gpsTime
        ));
    }

    private RinexNav rinexNav;

    private void startRecordRinexNav()
    {
        rinexNav=new RinexNav(getApplicationContext(), sharedPreferences.getInt(Constants.KEY_RINEX_VER,Constants.DEF_RINEX_VER));
        //需要更新数据库
    }

    private void stopRecordRinexNav()
    {
        rinexNav.writeHeader(gpsNavigationConv.sqliteManager);
        rinexNav.writeBody(gpsNavigationConv.sqliteManager);
        rinexNav.closeFile();
    }

    private void stopRecordRinex() {
        rinex.closeFile();
    }
}



