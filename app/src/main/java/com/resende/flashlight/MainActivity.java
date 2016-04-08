package com.resende.flashlight;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    public boolean flashIsOn = false;

    public Camera cam;
    public Camera.Parameters camParams;
    public int frequency;
    public StroboRunner stroboClass;
    public Thread t;
    public ImageButton flashSwitch;
    BroadcastReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        IntentFilter filter = new IntentFilter("android.intent.CLOSE_ACTIVITY");
        registerReceiver(mReceiver, filter);

        getCam();

        flashSwitch = (ImageButton) findViewById(R.id.flashlight_button);
        flashSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFlash();
                turnOnOff();

                if (!flashIsOn) {
                    flashSwitch.setImageResource(R.drawable.flash_button_off);
                } else {
                    flashSwitch.setImageResource(R.drawable.flash_button_on);
                }
            }
        });

        SeekBar skBar = (SeekBar) findViewById(R.id.seekBar);
        skBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (frequency != progress) {
                    frequency = progress;
                    if (t == null || frequency == 0) {
                        turnOnOff();
                    }
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.aboutSnack, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public void getCam() {
        cam = Camera.open();
        camParams = cam.getParameters();
        cam.startPreview();
    }

    public void updateFlash() {
        if (flashIsOn) {
            flashIsOn = false;
            dismissNotification();
        } else if (!flashIsOn) {
            flashIsOn = true;
            createNotification();
        }
    }

    public void turnOnOff() {
        if (flashIsOn) {
            if (frequency != 0 && t == null) {
                stroboClass = new StroboRunner();
                stroboClass.stopRunning = false;
                t = new Thread(stroboClass);
                t.start();
            } else if (frequency == 0){
                if (t != null) {
                    stroboClass.stopRunning = true;
                }
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
        } else {
            if (t != null) {
                stroboClass.stopRunning = true;
            } else if (camParams.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)){
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }

        cam.setParameters(camParams);
        cam.startPreview();
    }

    public void createNotification() {
        NotificationManager notifyMgr = (NotificationManager) getSystemService(MainActivity.NOTIFICATION_SERVICE);
        String packageName = this.getPackageName();

        Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent launchPintent = PendingIntent.getActivity(this, 0, launchIntent, 0);

        Intent finishIntent = new Intent("android.intent.CLOSE_ACTIVITY");
        PendingIntent finishPintent = PendingIntent.getBroadcast(this, 0, finishIntent, 0);

        NotificationCompat.Builder notifyFlash = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Flashlight")
                .setContentText("is on!")
                .setContentIntent(launchPintent)
                .addAction(Color.TRANSPARENT, "Turn off", finishPintent);

        notifyMgr.notify(33094095, notifyFlash.build());
    }

    public void dismissNotification() {
        NotificationManager notifyMgr = (NotificationManager) getSystemService(MainActivity.NOTIFICATION_SERVICE);
        notifyMgr.cancel(33094095);
    }


/**************************************************************************************************/

    private class StroboRunner implements Runnable {

        int frequency;
        boolean stopRunning;

        @Override
        public void run() {
            Camera.Parameters parametersOn = MainActivity.this.cam.getParameters();
            Camera.Parameters parametersOff = MainActivity.this.camParams;

            parametersOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            parametersOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

            try {
                while (!stopRunning) {
                    frequency = MainActivity.this.frequency;

                    MainActivity.this.cam.setParameters(parametersOn);
                    MainActivity.this.cam.startPreview();
                    Thread.sleep(100 - ( (frequency * 4 ) / 5) );

                    MainActivity.this.cam.setParameters(parametersOff);
                    MainActivity.this.cam.stopPreview();
                    Thread.sleep(100 - ( (frequency * 3) / 5) );
                }
                t = null;
            } catch (Throwable t) {
                /*****/
            }
        }
    }

/**************************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            AlertDialog about = new AlertDialog.Builder(this).create();
            about.setTitle(getString(R.string.app_name));
            about.setMessage(getString(R.string.about));
            about.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            about.show();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onDestroy() {
        super.onDestroy();

        dismissNotification();
        if (cam != null) {
            cam.release();
        }
    }
}
