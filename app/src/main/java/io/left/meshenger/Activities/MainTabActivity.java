package io.left.meshenger.Activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.ListView;
import android.widget.TabHost;
import io.left.meshenger.Adapters.UserListAdapter;
import io.left.meshenger.R;
import io.left.meshenger.Services.IMeshIMService;
import io.left.meshenger.Services.MeshIMService;

public class MainTabActivity extends Activity {
    // Reference to AIDL interface of app service.
    private IMeshIMService mService = null;

    // Implementation of AIDL interface.
    private IActivity.Stub mCallback = new IActivity.Stub() {
        @Override
        public void updateInterface() throws RemoteException {
            runOnUiThread(() -> {
                mUserListAdapter.updateList(mService);
                mUserListAdapter.notifyDataSetChanged();
            });
        }
    };

    // Handles connecting to service. Registers `mCallback` with the service when the connection
    // is successful.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IMeshIMService.Stub.asInterface(service);
            try {
                mService.registerMainActivityCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    UserListAdapter mUserListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tab);

        Intent serviceIntent = new Intent(this, MeshIMService.class);
        bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);

        configureTabs();
        configureUserList();
    }

    private void configureTabs() {
        TabHost host = findViewById(R.id.tabHost);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Tab One");
        spec.setContent(R.id.tab1);
        spec.setIndicator("In Range");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Messages");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("Tab Three");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Account");
        host.addTab(spec);
    }

    private void configureUserList() {
        ListView listView = findViewById(R.id.userListView);
        mUserListAdapter = new UserListAdapter(this);
        listView.setAdapter(mUserListAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainTabActivity.this, ChatActivity.class);
            startActivity(intent);
        });
    }
}
