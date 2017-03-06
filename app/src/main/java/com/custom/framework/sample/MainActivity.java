package com.custom.framework.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.custom.framework.contants.Intents;
import com.custom.framework.sample.service.IMainService;
import com.custom.framework.sample.service.MainService;
import com.custom.framework.sample.ui.BaseActivity;
import com.custom.framework.service.broadcast.LocalBroadcastManager;

public class MainActivity extends BaseActivity {
    public static final String ACTION_UPDATE = "action_update";
    private MReceiver receiver = new MReceiver();
    private IMainService mainService;
    private TextView tv;

    @Override
    public void onUICreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, ACTION_UPDATE);
        mainService = (IMainService) getService(MainService.class, IMainService.class);
        tv = (TextView) findViewById(R.id.tv);
        findViewById(R.id.bt).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    Log.i("framework", "onClick");
                    mainService.update();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onUIDestroy() {
        super.onUIDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private class MReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_UPDATE.equals(intent.getAction())) {
                tv.setText(intent.getStringExtra(Intents.EXTRAS_SER_RESULT));
            }
        }

    }
}
