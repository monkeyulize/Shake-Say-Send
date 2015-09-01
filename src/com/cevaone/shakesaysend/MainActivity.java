package com.cevaone.shakesaysend;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {
    
	NotificationManager mNotifyMgr;
	NotificationCompat.Builder mBuilder;
	AlertDialog.Builder email_change_alert;
	AlertDialog.Builder text_number_change_alert;
	AlertDialog.Builder about_alert;
	SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
	boolean isFirstTimeSetup;
	boolean is_service_started = false;
	int selected_option = -1; //0 = email, 1 = text
	String email_address = "somebody@somewhere.com";
	String text_number;
	EditText input;
	Button email_btn;
	Button text_btn;
	Animation animation;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
		setContentView(R.layout.activity_main);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();
        isFirstTimeSetup = sharedPref.getBoolean(getString(R.string.pref_isFirstTime), true);
        selected_option = sharedPref.getInt(getString(R.string.pref_selectedOption), -1);
        
        email_btn = (Button)findViewById(R.id.email_btn);
        text_btn = (Button)findViewById(R.id.text_btn);
        
		animation = new AlphaAnimation(1.0f, 0.5f); // Change alpha from fully visible to invisible
	    animation.setDuration(500); // duration - half a second
	    animation.setInterpolator(new LinearInterpolator()); 
	    animation.setRepeatCount(3); 
	    animation.setRepeatMode(Animation.REVERSE); 
		
        email_change_alert = new AlertDialog.Builder(this);
        email_change_alert.setTitle("Enter an email address");              
        email_change_alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		String value = input.getText().toString();
        	  	email_address = value;
        	  	editor.putString(getString(R.string.pref_email), email_address);
        	  	Toast.makeText(MainActivity.this, email_address, Toast.LENGTH_SHORT).show();
        	  	if(value.length() != 0) {
        	  		selected_option = 0;
        	  	}
        	  	editor.putInt(getString(R.string.pref_selectedOption), selected_option);
        	  	editor.apply();
        	}
        	
        });

        email_change_alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	  public void onClick(DialogInterface dialog, int whichButton) {
        	  		// Canceled.
        	  }
        });
        text_number_change_alert = new AlertDialog.Builder(this);
        text_number_change_alert.setTitle("Enter a phone number");              
        text_number_change_alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        		String value = input.getText().toString();
        	  	text_number = value;
        	  	editor.putString(getString(R.string.pref_text_number), text_number);
        	  	if(value.length() != 0) {
        	  		selected_option = 1;
        	  	}
        	  	editor.putInt(getString(R.string.pref_selectedOption), selected_option);
        	  	editor.apply();

        	}
        	
        });

        text_number_change_alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	  public void onClick(DialogInterface dialog, int whichButton) {
        	  		// Canceled.
        	  }
        });
        
        about_alert = new AlertDialog.Builder(this);
        about_alert.setTitle(R.string.about);
				

	}
	
    @Override
    public void onStop() {
    	super.onStop();
    	editor.putBoolean(getString(R.string.pref_isFirstTime), isFirstTimeSetup);
    	editor.putInt(getString(R.string.pref_selectedOption), selected_option);
    	editor.apply();  
    	finish();
    }
    
    public void select_email(View v) {
    	
		input = new EditText(this);
		email_change_alert.setView(input);  
		email_change_alert.show();
	
    }
    
    public void select_text(View v) {
    	
		input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		text_number_change_alert.setView(input);  
		text_number_change_alert.show();
    	
	
    }

    public void pref_btn(View v){
    	
    }
    
    public void pref_del_btn(View v){
    	editor.remove(getString(R.string.pref_isFirstTime));
    	editor.remove(getString(R.string.pref_selectedOption));
    	editor.apply();
    	selected_option = -1;
    	Toast.makeText(MainActivity.this, "Preference deleted", Toast.LENGTH_SHORT).show();
    	
    }

    public void svc_start(View v){
    	
    	
    	if(selected_option == -1) {
    		email_btn.startAnimation(animation);
    		text_btn.startAnimation(animation);
    		Toast.makeText(MainActivity.this, "Select your preference", Toast.LENGTH_SHORT).show();
    	} else {

        		is_service_started = true;
        		Toast.makeText(MainActivity.this, "Starting listening service", Toast.LENGTH_SHORT).show();
            	startService(new Intent(this, sensorListenerService.class)); 
    		
  	
    	}

    }


    
    public void svc_stop(View v){
		if(is_service_started == false) {
			Toast.makeText(MainActivity.this, "Service is not started", Toast.LENGTH_SHORT).show();
		} else {
			int requestID = (int) System.currentTimeMillis();
			Intent resultIntent = new Intent(MainActivity.this, sensorListenerService.class);
			resultIntent.putExtra(getString(R.string.stop_service), 1);
			PendingIntent resultPendingIntent = PendingIntent.getService(MainActivity.this,  requestID,  resultIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
			try {
				resultPendingIntent.send();
			} catch (CanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}

    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
        	case R.id.action_about:
        		View view = (View) LayoutInflater.from(getApplicationContext()).inflate(R.layout.about, null);
        		about_alert.setView(view);
        		about_alert.show();
        		return true;
        	default:
        		
        		return super.onOptionsItemSelected(item);
        }
        
    }
    
}
