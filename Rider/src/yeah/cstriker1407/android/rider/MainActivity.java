package yeah.cstriker1407.android.rider;

import java.lang.ref.WeakReference;
import java.util.Date;

import yeah.cstriker1407.android.rider.storage.Bitmaps;
import yeah.cstriker1407.android.rider.storage.Locations;
import yeah.cstriker1407.android.rider.storage.Locations.SpeedInfo;
import yeah.cstriker1407.android.rider.utils.TimeUtils;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.MyLocationOverlay.LocationMode;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "MainActivity";
	
	private TextView tv_speedx;
	private TextView tv_speedy;
	private TextView tv_speedsel;
	private TextView tv_speedunit;
	private TextView tv_totaldistance;
	private TextView tv_currtime;
	private TextView tv_currloc;
	
	private LocationClient mLocationClient = null;
	private MainActHandler handler = new MainActHandler(this);
	private SpeedSelEnum speedSelEnum = SpeedSelEnum.CUR;
	
	private BMapManager mBMapMan = null;  
	private MapView mMapView = null;  
	private MapController mMapController = null;
	private MyLocationOverlay myLocationOverlay = null; 
	private LocationData locData = null;
	
	
	/* MSG_INDEX */
	private static final int MSG_SPEEDUPDATE = 0;
	private static final int MSG_TIMEUPDATE = 1;
	
	
	
	private BDLocationListener bdLocationListener = new BDLocationListener() {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return;

			Locations.SpeedInfo speedInfo = Locations.getInstance().calcSpeedInfo(
					location.getLatitude(),
					location.getLongitude(),
					location.getAddrStr());
			
			Message msg = new Message();
			msg.what = MSG_SPEEDUPDATE;
			msg.obj = speedInfo;
			handler.sendMessage(msg);
			
			
            locData.latitude = location.getLatitude();
            locData.longitude = location.getLongitude();
            //�������ʾ��λ����Ȧ����accuracy��ֵΪ0����
            locData.accuracy = location.getRadius();
            locData.direction = location.getDerect();
            //���¶�λ����
            myLocationOverlay.setData(locData);
            //����ͼ������ִ��ˢ�º���Ч
            mMapView.refresh();
            //�ƶ�����λ��
            /* �����̺͸���ģʽ�£���ͼ���Զ���animate�����µ�λ�ã�����ͨģʽ�£����ᡣ */
            mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)));
		}

		@Override
		public void onReceivePoi(BDLocation poiLocation) {}
	};

	private MKMapViewListener mKMapViewListener = new MKMapViewListener() {
		@Override
		public void onMapMoveFinish() {}
		
		@Override
		public void onMapLoadFinish() {}
		
		@Override
		public void onMapAnimationFinish() {}
		
		@Override
		public void onGetCurrentMap(Bitmap arg0) 
		{
			Bitmaps.writeBitmapToFile(arg0, "/mnt/sdcard/map.png");
		}
		
		@Override
		public void onClickMapPoi(MapPoi arg0) {}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//ע�⣺��������setContentViewǰ��ʼ��BMapManager���󣬷���ᱨ��  
		mBMapMan=new BMapManager(getApplicationContext());  
		mBMapMan.init("F0488f2ee7d14e2bba215419efb9bff3", null);    
		
		
		setContentView(R.layout.activity_main);
		
		//===
		tv_speedx = (TextView)findViewById(R.id.tv_speedx);
		tv_speedy = (TextView)findViewById(R.id.tv_speedy);
		tv_speedsel = (TextView)findViewById(R.id.tv_speedsel);
		tv_speedunit = (TextView)findViewById(R.id.tv_speedunit);
		tv_currloc = (TextView)findViewById(R.id.tv_currloc);

		tv_speedx.setOnClickListener(this);
		tv_speedy.setOnClickListener(this);
		tv_speedsel.setOnClickListener(this);
		tv_speedunit.setOnClickListener(this);
		
		tv_totaldistance = (TextView)findViewById(R.id.tv_totaldistance);
		tv_currtime = (TextView)findViewById(R.id.tv_currtime);
		
		Typeface lcdTf = Typeface.createFromAsset(getAssets(),"fonts/LCD.ttf");
		tv_speedx.setTypeface(lcdTf);
		tv_speedy.setTypeface(lcdTf);
		tv_speedunit.setTypeface(lcdTf);
		tv_totaldistance.setTypeface(lcdTf);
		//===
		
		
		initBDLocationSettings();
		startBDLocationService();
		
		

		mMapView=(MapView)findViewById(R.id.bdmapview);  
		mMapView.setBuiltInZoomControls(true);  
		//�����������õ����ſؼ�  
		mMapController=mMapView.getController();  
		// �õ�mMapView�Ŀ���Ȩ,�����������ƺ�����ƽ�ƺ�����  
		GeoPoint point =new GeoPoint((int)(39.915* 1E6),(int)(116.404* 1E6));  
		//�ø����ľ�γ�ȹ���һ��GeoPoint����λ��΢�� (�� * 1E6)  
		mMapController.setCenter(point);//���õ�ͼ���ĵ�  
		mMapController.setZoom(12);//���õ�ͼzoom����  
		
		mMapView.regMapViewListener(mBMapMan, mKMapViewListener);
		
		
		
		
        locData = new LocationData();
        //��λͼ���ʼ��
		myLocationOverlay = new MyLocationOverlay(mMapView);
		//���ö�λ����
	    myLocationOverlay.setData(locData);
	    //���Ӷ�λͼ��
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();

		myLocationOverlay.setLocationMode(LocationMode.COMPASS);
		//�޸Ķ�λ���ݺ�ˢ��ͼ����Ч
		mMapView.refresh();
		
		
		handler.sendEmptyMessage(MSG_TIMEUPDATE);
		
	}

	private void startBDLocationService() {
		if (mLocationClient != null)
		{
			mLocationClient.start();
			int result = mLocationClient.requestLocation();
			if (result != 0)
			{
				Log.e(TAG, "BDSDK Init Error:" + result);
			}
			mLocationClient.requestPoi();
		}
	}
	private void stopBDLocationService() {
		if (mLocationClient != null && mLocationClient.isStarted()) {
			mLocationClient.stop();
		}
	}

	private void initBDLocationSettings() {
		mLocationClient = new LocationClient(getApplicationContext()); // ����LocationClient��
		mLocationClient.registerLocationListener(bdLocationListener); // ע���������
		mLocationClient.setAK("F0488f2ee7d14e2bba215419efb9bff3");

		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setAddrType("all");// ���صĶ�λ���������ַ��Ϣ
		option.setCoorType("bd09ll");// ���صĶ�λ����ǰٶȾ�γ��,Ĭ��ֵgcj02
		option.setScanSpan(5000);// ���÷���λ����ļ��ʱ��Ϊ5000ms
		option.disableCache(true);// ��ֹ���û��涨λ

		option.setPoiNumber(10); // ��෵��POI����
		option.setPoiDistance(2000); // poi��ѯ����
		option.setPoiExtraInfo(true); // �Ƿ���ҪPOI�ĵ绰�͵�ַ����ϸ��Ϣ

		option.setPriority(LocationClientOption.NetWorkFirst);
		mLocationClient.setLocOption(option);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override  
	protected void onResume(){  
	        mMapView.onResume();  
	        if(mBMapMan!=null){  
	                mBMapMan.start();  
	        }  
	       super.onResume();  
	}

	@Override  
	protected void onPause(){  
	        mMapView.onPause();  
	        if(mBMapMan!=null){  
	               mBMapMan.stop();  
	        }  
	        super.onPause();  
	}  
	
	@Override
	protected void onDestroy() {
		stopBDLocationService();
		
		 mMapView.destroy();  
	        if(mBMapMan!=null){  
	                mBMapMan.destroy();  
	                mBMapMan=null;  
	        }  
		
		super.onDestroy();
	}
	
	private static enum SpeedSelEnum
	{
		MIN("MIN"),
		MAX("MAX"),
		AVG("AVG"),
		CUR("CUR");
		private String speedDes;
		private SpeedSelEnum(String speedDes)
		{
			this.speedDes = speedDes;
		}
		public String getSpeedDes() {
			return speedDes;
		}
		
		public SpeedSelEnum getNext()
		{
			int len = SpeedSelEnum.values().length;
			int newIdx = (this.ordinal() + 1) % len;
			return SpeedSelEnum.values()[newIdx];
		}
		
	}
	private static class MainActHandler extends Handler 
	{
		private WeakReference<MainActivity> activity = null;

		public MainActHandler(MainActivity act) {
			super();
			this.activity = new WeakReference<MainActivity>(act);
		}

		@Override
		public void handleMessage(Message msg) 
		{
			MainActivity act = activity.get();
			if (null == act) {
				return;
			}
			switch (msg.what) 
			{
				case MSG_TIMEUPDATE:
				{
					act.tv_currtime.setText(TimeUtils.fmtDate2Str(new Date(), " yyyy-MM-dd hh:mm:ss a"));
					this.sendEmptyMessageDelayed(MSG_TIMEUPDATE, 1000);
					break;
				}
			
				case MSG_SPEEDUPDATE:
				{
					Locations.SpeedInfo info = (SpeedInfo) msg.obj;
					if (info != null)
					{
						Log.d(TAG, info.toString());
						
						float speedM = 0.0f;
						switch (act.speedSelEnum) 
						{
							case MIN: 
							{
								speedM = info.minSpeedM * 3.6f;break;
							}
							case MAX: 
							{
								speedM = info.maxSpeedM * 3.6f;break;
							}
							case AVG: 
							{
								speedM = info.averageSpeedM * 3.6f;break;
							}
							case CUR:
							default:
							{
								speedM = info.singleSpeedM * 3.6f;break;
							}
						}
						int x = (int)speedM;
						int y = (int) ((speedM - x)*100);
						act.tv_speedx.setText(String.format("%02d", x));
						act.tv_speedy.setText(String.format("%02d", y));
						
						float totalDiatanceKM = info.totalDistanceM/1000.0f;
						String totalDistance = String.format("%.3fKM/%s", totalDiatanceKM, TimeUtils.fmtMs2Str(info.totalSeconds*1000, "HH:mm:ss"));
						act.tv_totaldistance.setText(totalDistance);
						act.tv_speedsel.setText(act.speedSelEnum.getSpeedDes());//��ǰ�ٶ�����
						act.tv_currloc.setText(info.locDes);
					}
					
					break;
				}
			}
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.tv_speedx:
		case R.id.tv_speedy:
		case R.id.tv_speedsel:
		case R.id.tv_speedunit:
		{
			mMapView.getCurrentMap();
			
			
			speedSelEnum = speedSelEnum.getNext();
			tv_speedsel.setText(speedSelEnum.getSpeedDes());
			break;
		}
		default:
			break;
		}
	}
	
}