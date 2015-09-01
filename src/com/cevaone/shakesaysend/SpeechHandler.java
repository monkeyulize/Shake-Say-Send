package com.cevaone.shakesaysend;

import java.util.ArrayList;


import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.PreferenceManager;

public class SpeechHandler extends Activity {

	
	AlertDialog.Builder email_change_alert;
	AlertDialog.Builder save_file_alert;
	EditText input;
	TextView prompt_text;
	Button email_btn, text_btn, save_btn;
	ArrayList<String> tts_text;
	String email_address = "somebody@somewhere.com";
	String text_number;
	String filename;
	SharedPreferences sharedPref;
	SharedPreferences.Editor editor;
	int selected_option; //0 = email, 1 = text
	private final String TAG = "vr_status";
	protected static final int RESULT_SPEECH = 1;
	Intent speechIntent;
	SpeechRecognizer sr;
	String text = "";
	boolean stop_listening;
	boolean is_first_speech;
	
	static final int MSG_START_LISTEN = 0;
	static final int MSG_STOP_LISTEN = 1;
	static final int MSG_CAN_SENSE = 2;
	static final int MSG_CANT_SENSE = 3;
	
	private ProgressBar progressBar;
	Messenger mService = null;
	boolean mIsBound;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
    
	static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
                super.handleMessage(msg);
        }
	}
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, sensorListenerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
        public void onServiceDisconnected(ComponentName className) {          
            mService = null;
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speech_handler);
		
		
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setIndeterminate(true);
		progressBar.setVisibility(View.INVISIBLE);
		stop_listening = getIntent().getExtras().getBoolean(getString(R.string.extra_stopListen));
		is_first_speech = true;
		doBindService();
		send_message(MSG_START_LISTEN);
 
      	prompt_text = (TextView)findViewById(R.id.prompt_text);
		sr = getSpeechRecognizer();
    	speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    	speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
    	speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, SpeechHandler.this.getPackageName());
    	speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		tts_text = new ArrayList<String>();
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = sharedPref.edit();
		
		text = sharedPref.getString(getString(R.string.pref_ttsText), "");
		email_address = sharedPref.getString(getString(R.string.pref_email), getString(R.string.email_not_set));
		selected_option = sharedPref.getInt(getString(R.string.pref_selectedOption), -1);
		text_number = sharedPref.getString(getString(R.string.pref_text_number), "0000000000");
		
        if(stop_listening == false) {
        	sr.startListening(speechIntent);
        } else {
        	sr.stopListening();
        	sr.cancel();
        	
        	switch(selected_option) {
    		case 0:
    			send_email();
    			break;
    		case 1:
    			finish();
    			send_text();
    			break;
    		}
        	text = "";
        	editor.remove(getString(R.string.pref_ttsText));
        	editor.apply();
        	finish();
        }
	} //onCreate
	
    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
        case SpeechRecognizer.ERROR_AUDIO:
            message = "Audio recording error";
            break;
        case SpeechRecognizer.ERROR_CLIENT:
            message = "Client side error";
            break;
        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
            message = "Insufficient permissions";
            break;
        case SpeechRecognizer.ERROR_NETWORK:
            message = "Network error";
            break;
        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            message = "Network timeout";
            break;
        case SpeechRecognizer.ERROR_NO_MATCH:
            message = "No match";
            break;
        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            message = "RecognitionService busy";
            break;
        case SpeechRecognizer.ERROR_SERVER:
            message = "error from server";
            break;
        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            message = "No speech input";
            break;
        default:
            message = "Didn't understand, please try again.";
            break;
        }
        return message;
    } //getErrorText
    
	public void send_email() {
		if(text.length() == 0) {
    		Toast.makeText(SpeechHandler.this, "Note hasn't been recorded!", Toast.LENGTH_SHORT).show();	
    	} else {
    		
    		Intent i = new Intent(Intent.ACTION_SEND);
        	i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_EMAIL, new String[]{email_address});
			i.putExtra(Intent.EXTRA_SUBJECT, "recorded memo");
			i.putExtra(Intent.EXTRA_TEXT, text);
			try {
				
				startActivity(i);
				send_message(MSG_CAN_SENSE);
				
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(SpeechHandler.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();	
			}        		
    	}
	}
    
    public void send_text() {
		if(text.length() == 0) {
    		Toast.makeText(SpeechHandler.this, "Note hasn't been recorded!", Toast.LENGTH_SHORT).show();	
    	} else {
    		Intent i = new Intent(Intent.ACTION_VIEW);
    		i.setType("vnd.android-dir/mms-sms");
    		i.putExtra("sms_body", text);
    		i.setData(Uri.parse("smsto:" + Long.parseLong(text_number)));
    		try {			
    			send_message(MSG_START_LISTEN);
    			startActivity(i);
    			send_message(MSG_CAN_SENSE);
    		} catch (android.content.ActivityNotFoundException ex) {
    			Toast.makeText(SpeechHandler.this, "There are no texting clients installed.", Toast.LENGTH_SHORT).show();			
    		}  
    	}
    }

	public void mExitListener(View v) {     	
		sr.cancel();
		sr.destroy();	
		finish();
		
	}
	
	public void mStopListener(View v) {     	
    	sr.stopListening();
    	sr.cancel();
    	
    	switch(selected_option) {
		case 0:
			finish();
			send_email();
			break;
		case 1:
			finish();
			send_text();
			break;
		}
    	text = "";
    	editor.remove(getString(R.string.pref_ttsText));
    	editor.apply();
    	
	}

	private SpeechRecognizer getSpeechRecognizer() {
		if(sr == null) {
			sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
			sr.setRecognitionListener(new listener());
		}

		return sr;
		
	}
	
	class listener implements RecognitionListener {
		@Override
		public void onBeginningOfSpeech() {
		    Log.d(TAG, "onBeginningOfSpeech()");  
		    progressBar.setIndeterminate(false);
		    progressBar.setMax(10);
		}
			
		@Override
		public void onBufferReceived(byte[] buffer) {
		    Log.d(TAG, "onBufferReceived()");
		}
			
		@Override
		public void onEndOfSpeech() {
		    Log.d(TAG, "onEndOfSpeech()");
		    progressBar.setIndeterminate(true);
		    prompt_text.setText(getString(R.string.wait));
		}
			
		@Override
		public void onError(int error) {
		    Log.d(TAG, "onError()");
		    String errorMessage = getErrorText(error);
		    Log.d(TAG, "FAILED " + errorMessage);
		    sr.startListening(speechIntent);
		}
			
		@Override
		public void onEvent(int eventType, Bundle params) { ; }
			
		@Override
		public void onReadyForSpeech(Bundle params) {
		    Log.d(TAG, "onReadyForSpeech()");
		    progressBar.setVisibility(View.VISIBLE);
		    progressBar.setIndeterminate(false);

		    prompt_text.setText(getString(R.string.speak_now));

		    
		}
			
		@Override
		public void onResults(Bundle results) {
			Log.d(TAG, "onResults()");
			ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);  	        	
			text = text + " " + matches.get(0);
			editor.putString(getString(R.string.pref_ttsText), text);
			editor.apply();
			sr.startListening(speechIntent);
			progressBar.setIndeterminate(false);
			send_message(MSG_STOP_LISTEN);
			send_message(MSG_CAN_SENSE);
			prompt_text.setText(getString(R.string.processing));
			is_first_speech = false;
			
			
		}
			
		@Override
		public void onPartialResults(Bundle partialResults) {
		    Log.d(TAG, "onPartialResults()");
		}
		
		@Override
		public void onRmsChanged(float rmsdB) {
			progressBar.setProgress((int) rmsdB);
		} 
	} //class listener
	
	@Override
	protected void onResume() {
		sr = getSpeechRecognizer();
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		if(sr != null) {
			sr.stopListening();
			sr.cancel();
			sr.destroy();
		}
		editor.commit();
		super.onPause();
	}
	  
	@Override
	protected void onStop() {
		editor.commit();		
		super.onStop();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		sr.cancel();
		sr.destroy(); 
		
		/** if something goes wrong, make sure service knows to listen **/
		send_message(MSG_START_LISTEN);
		send_message(MSG_CAN_SENSE);
		
		doUnbindService();	
	}
	
    void doBindService() {
        bindService(new Intent(this, sensorListenerService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

    }
    void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, sensorListenerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
            unbindService(mConnection);
            mIsBound = false;

        }
    }
    
    
    public void send_message(int which) {
      	if(mIsBound) {
      		if(mService != null) {
      			try {
      				Message msg = null;
      				switch(which) {
      				case MSG_START_LISTEN:
      					msg = Message.obtain(null, sensorListenerService.MSG_START_LISTEN, 1);
      					break;
      				case MSG_STOP_LISTEN:
      					msg = Message.obtain(null, sensorListenerService.MSG_STOP_LISTEN, 1);
      					break;
      				case MSG_CAN_SENSE:
      					msg = Message.obtain(null, sensorListenerService.MSG_CAN_SENSE, 1);
      					break;
      				case MSG_CANT_SENSE:
      					msg = Message.obtain(null, sensorListenerService.MSG_CANT_SENSE, 1);
      					break;
      				
      				}
      				msg.replyTo = mMessenger;
      				mService.send(msg);
      			} catch(RemoteException e) {
      				
      			}
      			
      		}	
      	}
      		    	
    }
    
   
	
	
}
