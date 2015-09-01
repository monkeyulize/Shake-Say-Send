package com.cevaone.shakesaysend;

import java.util.ArrayList;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class sensorListenerService extends Service implements SensorEventListener  {
	NotificationManager mNotifyMgr;
	NotificationCompat.Builder mBuilder;
	private SensorManager sensorManager;
	private Sensor mAccelerometer;
	long timeFilter = 200;
	float x, y, z;
	float lastX, lastY, lastZ;
	float deltaX, deltaY, deltaZ;
	long currentTime, lastTime;
	long diffTime;
	boolean mInitialized = false;
	float values[];
	int i = 0;
	int numShakes = 0;
	int whichDirection = -1; //0 = x, 1 = y, 2 = z
	static boolean can_listen = true;
	static boolean can_sense = true;
	
	float mAccel, mAccelCurrent, mAccelLast;
	float[] currentAccel = new float[3];
	float[] lastAccel = new float[3];	
	KeyguardManager kgMgr;
	PowerManager pMgr;
	static ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	int mValue = 0;	
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;	
	static final int MSG_START_LISTEN = 3;
	static final int MSG_STOP_LISTEN = 4;
	static final int MSG_CAN_SENSE = 5;
	static final int MSG_CANT_SENSE = 6;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	
	
	static class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
			case MSG_REGISTER_CLIENT:
				Log.d("can_listen", "received MSG_REGISTER_CLIENT");
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				Log.d("can_listen", "received MSG_UNREGISTER_CLIENT");
				mClients.remove(msg.replyTo);
				break;
			case MSG_START_LISTEN:
				Log.d("can_listen", "received MSG_START_LISTEN");
				can_listen = true;
				break;
			case MSG_STOP_LISTEN:
				Log.d("can_listen", "received MSG_STOP_LISTEN");
				can_listen = false;
				break;
			case MSG_CAN_SENSE:
				Log.d("can_listen", "received MSG_CAN_SENSE");
				can_sense = true;
				break;
			case MSG_CANT_SENSE:
				Log.d("can_listen", "received MSG_CANT_SENSE");
				can_sense = false;
				break;
			default:
				Log.d("can_listen", "received default");
				super.handleMessage(msg);
			
			}
			Log.d("can_listen", Boolean.valueOf(can_listen).toString());
			Log.d("can_sense", Boolean.valueOf(can_sense).toString());
			
		}
	}
	
	
	
	private final SensorEventListener mSensorListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent se) {
			float x = se.values[0];
			float y = se.values[1];
			float z = se.values[2];
			mAccelLast = mAccelCurrent;
			mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
			float delta = mAccelCurrent - mAccelLast;
			mAccel = mAccel * 0.09f + delta;
			//Log.d("mAccel", Float.valueOf(mAccel).toString());
			if(mAccel > 11) {				
				Log.d("shaken", "device was shaken");
				Intent i = new Intent(sensorListenerService.this, SpeechHandler.class);
				if(can_sense == true && is_screen_on()) { //ensure the program is in a state where it should accept a shake action
					if(can_listen == false) {	//toggle between starting a new speech handler or ending one							
						can_sense = false;
						//Toast.makeText(getApplicationContext(), "Stopping...please wait", Toast.LENGTH_SHORT).show();
/*						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
						i.putExtra(getString(R.string.extra_stopListen), true); //flag determining whether to start or stop speech handler activity
						startActivity(i);*/
					} else {	
						can_sense = false;
						Toast.makeText(getApplicationContext(), "Starting...please wait", Toast.LENGTH_SHORT).show();
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.putExtra(getString(R.string.extra_stopListen), false); //flag determining whether to start or stop speech handler activity
						startActivity(i);				
					}
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
		}
		
		
		
	};
	

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent e) {
	}

	private boolean is_screen_on() {
		if(pMgr.isScreenOn() && !kgMgr.inKeyguardRestrictedInputMode()) {
			return true;
		} else {
			return false;
		}
	}
	
	private void showNotification() {
		int requestID = (int) System.currentTimeMillis();
		
		mBuilder = new NotificationCompat.Builder(sensorListenerService.this)
		.setSmallIcon(R.drawable.ic_launcher_2)
		.setContentTitle("Stop listening")
		.setOngoing(true);
		
		Intent resultIntent = new Intent(this, sensorListenerService.class);
		resultIntent.putExtra(getString(R.string.stop_service), 1);
		PendingIntent resultPendingIntent = PendingIntent.getService(sensorListenerService.this,  requestID,  resultIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		int mNotificationId = 001;
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
		
	}
	
	
    @Override
    public void onCreate() {
    	Log.d("service_status", "service was created");
    	mAccel = 0.00f;
    	mAccelCurrent = SensorManager.GRAVITY_EARTH;
    	mAccelLast = SensorManager.GRAVITY_EARTH;
    	sensorManager  = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		showNotification();
		
		pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
		kgMgr = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    }
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	sensorManager.unregisterListener(mSensorListener, mAccelerometer);
    	Toast.makeText(getApplicationContext(), "Stopping listening service", Toast.LENGTH_SHORT).show();
    	Log.d("service_status", "service was destroyed");
    }
    

    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d("service_status", "service was started");
    	Log.d("service_status", "stop_service = " + Integer.valueOf(intent.getIntExtra(getString(R.string.stop_service), -1)).toString());
    	if(intent != null) {
        	if(intent.getIntExtra(getString(R.string.stop_service), -1) == 1) {
        		Log.d("service_status", "trying to stop");
        		sensorManager.unregisterListener(mSensorListener, mAccelerometer);
        		mNotifyMgr.cancelAll();
        		stopSelf();
        	}   		
    	}

    	
    	return android.app.Service.START_REDELIVER_INTENT ;   	
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}
	


}
