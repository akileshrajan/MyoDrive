package com.example.akilesh.myodrive;

/**
 * Created by akilesh on 3/28/2016.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;


public class BackgroundActivity extends Activity {private DeviceListener mListener = new AbstractDeviceListener() {
    boolean volumeMode = false;
    @Override
    public void onConnect(Myo myo, long timestamp) {
        Log.d("pose", "connected");
    }

    @Override
    public void onDisconnect(Myo myo, long timestamp) {
        Log.d("pose", "disconnected");
    }

    @Override
    public void onPose(Myo myo, long timestamp, Pose pose) {
        Log.i("pose", "Pose: " + pose);
        boolean sleep = true;

        switch (pose) {
            case UNKNOWN:
                break;
            case FINGERS_SPREAD:
                //volumeMode = true;
                playPauseMusic();
                break;
            case REST:
                volumeMode = false;
                break;
            case DOUBLE_TAP:
                volumeMode = false;
                myo.unlock(Myo.UnlockType.TIMED);
                break;
            case FIST:
                volumeMode = true;
                sleep = false;
                break;
            case WAVE_IN:
                waveInAction();
                volumeMode = false;
                break;
            case WAVE_OUT:
                waveOutAction();
                volumeMode = false;
                break;
        }
        try {
            if(sleep){
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Log.d("sleep", "sleep failed wtf");
            // nothing to see here, move along
        }
    }

    @Override
    public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
        float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
        float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
        float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
        // Adjust roll and pitch for the orientation of the Myo on the arm.
        if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
            roll *= -1;
            pitch *= -1;
        }
        if(volumeMode){
            Log.i("rotation", Float.toString(roll));
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            int volume = Math.min((int)((roll + 100)*(10.0/80.0)), 14);
            Log.i("volume", Integer.toString(volume));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI); // 15 max, cap at 10/11
            //-60 to 20
        }
    }
};
    ListView list;
    String[] web = {
            "Unlock/Lock",
            "Play/Pause",
            "Previous Song/End Call",
            "Next Song/Accept Call",
            "Volume Control"

    } ;
    Integer[] imageId = {
            R.drawable.unlock,
            R.drawable.spread,
            R.drawable.wave_in,
            R.drawable.wave_out,
            R.drawable.fist
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.background_activity);

        CustomList adapter = new
                CustomList(BackgroundActivity.this, web, imageId);
        list=(ListView)findViewById(R.id.listGestures);
        list.setAdapter(adapter);
      /*  list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(BackgroundActivity.this, "You Clicked at " + web[+position], Toast.LENGTH_SHORT).show();

            }
        });*/
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e("test", "Could not initialize the Hub.");
            finish();
            return;
        }

        hub.addListener(mListener);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void playPauseMusic(){
        Context ctx = getApplicationContext();
        AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        Intent mediaIntent = new Intent("com.android.music.musicservicecommand");
        if (mAudioManager.isMusicActive()) {
            mediaIntent.putExtra("command", "pause");
        } else {
            mediaIntent.putExtra("command", "play");
        }
        ctx.sendBroadcast(mediaIntent);

    }


    public void waveOutAction(){
        Context ctx = getApplicationContext();
        AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        TelephonyManager teleManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        PhoneCallListener  phoneCall=new PhoneCallListener();
        teleManager.listen(phoneCall,PhoneStateListener.LISTEN_CALL_STATE);
        if(teleManager.getCallState()==TelephonyManager.CALL_STATE_RINGING) {
            Toast.makeText(ctx, "Accept call", Toast.LENGTH_LONG);
            Intent intent = new Intent(ctx, AcceptCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            ctx.startActivity(intent);
        }
        else if (mAudioManager.isMusicActive()){
            Intent mediaIntent = new Intent("com.android.music.musicservicecommand");
            mediaIntent.putExtra("command", "next");
            sendBroadcast(mediaIntent);
        }
        else {
            return;
        }
    }

    public void waveInAction(){
        Context ctx = getApplicationContext();
        AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        TelephonyManager teleManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateReceiver phoneListener=new PhoneStateReceiver();
        PhoneCallListener  phoneCall=new PhoneCallListener();
        teleManager.listen(phoneCall,PhoneStateListener.LISTEN_CALL_STATE);
       // String enforcedPerm = "android.permission.CALL_PRIVILEGED";
        if(teleManager.getCallState()==TelephonyManager.CALL_STATE_RINGING){
            Toast.makeText(ctx, "End Call", Toast.LENGTH_LONG);
            phoneListener.killCall(ctx);
           /* Intent rCallIntent = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
                    Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_HEADSETHOOK));
            ctx.sendOrderedBroadcast(rCallIntent, enforcedPerm);
            Intent cCallIntent = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
                    Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_HEADSETHOOK));
            ctx.sendOrderedBroadcast(cCallIntent, enforcedPerm);*/
            //sendBroadcast(cCallIntent);
        }
        else if(teleManager.getCallState()==TelephonyManager.CALL_STATE_OFFHOOK) {
            Toast.makeText(ctx, "End Call", Toast.LENGTH_LONG);
            phoneListener.killCall(ctx);
        }
        else if(mAudioManager.isMusicActive()){
            Intent mediaIntent = new Intent("com.android.music.musicservicecommand");
            mediaIntent.putExtra("command", "previous");
            sendBroadcast(mediaIntent);
        }
        else {
            return;
        }
    }



    public void connect(View view){
        Context context = getApplicationContext();
        Intent intent = new Intent(context, ScanActivity.class);
        startActivity(intent);
        Log.d("test",  "done with scan activity");
    }
    //monitor phone call activities
    private class PhoneCallListener extends PhoneStateListener {

        private boolean isPhoneCalling = false;

        String LOG_TAG = "LOGGING 123";

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (TelephonyManager.CALL_STATE_RINGING == state) {
                // phone ringing
                Log.i(LOG_TAG, "RINGING, number: " + incomingNumber);
            }

            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                // active
                Log.i(LOG_TAG, "OFFHOOK");

                isPhoneCalling = true;
            }

            if (TelephonyManager.CALL_STATE_IDLE == state) {
                // run when class initial and phone call ended,
                // need detect flag from CALL_STATE_OFFHOOK
                Log.i(LOG_TAG, "IDLE");

                if (isPhoneCalling) {

                    Log.i(LOG_TAG, "restart app");

                    // restart app
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(
                                    getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);

                    isPhoneCalling = false;
                }

            }
        }
    }
}
