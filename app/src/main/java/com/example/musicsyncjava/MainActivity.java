/*
Developer: Sandeep Singh Sandha
Contact: sandha.iitr@gmail.com
This file contains the code which can be used to synchronize the Android smartphones.

The code uses NTP client to query the NTP server and captues the offset of the android device monotonic time with respect to the server
 */

package com.example.musicsyncjava;

import android.Manifest;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    //stores the offset value
    long offset =0L;

    String ntpHost = "17.253.26.253";//"time.apple.com";

    boolean synced =false;

    MediaPlayer mp = new MediaPlayer();

    private final String [] permissions = {
            Manifest.permission.INTERNET
    };

    private static final int REQUEST_PERMISSION = 200;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //thread to calculate offset with respect to the android device monotonic time
        Thread calculate_offsets = new Thread(Timer);
        Thread waiting = new Thread(Wait_for_sync);

        calculate_offsets.setPriority(10);
        waiting.setPriority(8);
        calculate_offsets.start();
        waiting.start();

        //Wait_for_sync_manager.start();
    }//end onCreate

    public void OnClick_play(View v){
        mp.start();

        final Button btn_back = findViewById(R.id.button_back);
        final Button btn_next = findViewById(R.id.button_next);

        btn_back.setEnabled(true);
        btn_next.setEnabled(true);
    }

    public void OnClick_change_music_position(View v){
        final Button btn_back = findViewById(R.id.button_back);
        final Button btn_next = findViewById(R.id.button_next);

        if (v.getId() == btn_back.getId())
            mp.seekTo(mp.getCurrentPosition() - 10);
        else if(v.getId() == btn_next.getId())
            mp.seekTo(mp.getCurrentPosition() + 10);
    }

    void set_music(){
        mp = MediaPlayer.create(getApplicationContext(), R.raw.zelda);
    }

    long get_ntp_offset()
    {
        SntpDsense client = null;

        int timeout = 3000;
        boolean SntpSuceeded=false;

        long curr_ntp_offset = 0;
        long curr_ntp_monotonic_time=0;
        long curr_ntp_sys_time=0;

        client = new SntpDsense();

        //System.out.println("get_ntp_offset is running");
        SntpSuceeded = client.requestTime(ntpHost, timeout);

        if(SntpSuceeded)
        {
            curr_ntp_offset = client.getNtp_clockoffset();
            curr_ntp_monotonic_time = client.get_ntp_update_monotonic_time();
            curr_ntp_sys_time=client.get_ntp_update_sys_time();

            offset = curr_ntp_offset+curr_ntp_sys_time - curr_ntp_monotonic_time;
            System.out.println("curr_ntp_offset is: "+curr_ntp_offset);
            System.out.println("curr_ntp_sys_time is: "+curr_ntp_sys_time);
            System.out.println("curr_ntp_monotonic_time is: "+curr_ntp_monotonic_time);
            System.out.println("offset is: "+offset);
        }
        return curr_ntp_offset;
    }//end get_ntp_offset

    //running system time clock
    Runnable Timer = new Runnable() {
        @Override
        public void run() {
            offset = get_ntp_offset();
            while(true) {
                SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss.SSS");
                String dateString = formatter.format(new Date(System.currentTimeMillis() + offset));
                final TextView textViewToChange = (TextView) findViewById(R.id.textView);
                textViewToChange.setText(dateString);
                synced = true;
            }
        }//end  while (run_sys_time)
    };//end Timer

    Runnable Wait_for_sync = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (synced) {
                    set_music();
                    System.out.println("hejo");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final Button button = (Button) findViewById(R.id.button_play);
                            button.setEnabled(true);
                        }
                    });
                    break;
                }
            }    //end while true
        }//end run
    }; //end Wait_for_sync
}//end MainActivity
