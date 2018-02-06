package io.left.meshim.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import io.left.meshim.R;
import io.left.meshim.adapters.UserListAdapter;
import io.left.meshim.adapters.UserMessageListAdapter;
import io.left.meshim.models.Settings;
import io.left.meshim.models.User;

import java.util.ArrayList;

/**
 * Main interface for MeshIM. Displays tabs for viewing online users, conversations, and the
 * user's account.
 */
public class MainTabActivity extends ServiceConnectedActivity {
    // Adapter that populates the online user list with user information from the app service.
    UserListAdapter mUserListAdapter;
    ArrayList<User> mUsers = new ArrayList<>();
    UserMessageListAdapter mUserMessageListAdapter;

    /**
     * Initializes UI elements.
     * @param savedInstanceState passed by Android
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tab);

        configureTabs();
        configureUserList();
        configureMessageList();
        setupSettingTab();
        dummySearchScreen();

    }

    /**
     * Configure the content and UI of the tabs.
     */
    private void configureTabs() {
        TabHost host = findViewById(R.id.tabHost);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Tab One");
        spec.setContent(R.id.tab1);
        spec.setIndicator(getTabIndicator(this,"In Range",R.mipmap.in_range_default));
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator(getTabIndicator(this,"In Range",R.mipmap.messages_default));
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("Tab Three");
        spec.setContent(R.id.tab3);
        spec.setIndicator(getTabIndicator(this,"In Range",R.mipmap.account_default));
        host.addTab(spec);
    }
    private View getTabIndicator(Context context, String title, int icon) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
        ImageView iv = (ImageView) view.findViewById(R.id.imageView);
        iv.setImageResource(icon);
        TextView tv = (TextView) view.findViewById(R.id.tabText);
        tv.setText(title);
        return view;
    }

    /**
     * Configure the user list adapter and click event.
     */
    private void configureUserList() {
        ListView listView = findViewById(R.id.userListView);
        mUserListAdapter = new UserListAdapter(this, mUsers);
        listView.setAdapter(mUserListAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainTabActivity.this, ChatActivity.class);
            intent.putExtra("recipient", mUserListAdapter.getItem(position));
            startActivity(intent);
        });
    }

    private void configureMessageList() {
        ArrayList<User> u = new ArrayList<>();
        u.add(new User("user1",R.mipmap.avatar1));
        u.add(new User("user2",R.mipmap.avatar2));
        u.add(new User("user3",R.mipmap.avatar3));

        ListView listView = findViewById(R.id.multiUserMessageListView);
        mUserMessageListAdapter = new UserMessageListAdapter(this,u);
        listView.setAdapter(mUserMessageListAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainTabActivity.this,ChatActivity.class);
            intent.putExtra("recipient", mUserMessageListAdapter.getItem(position));
            startActivity(intent);
        });
    }

    @Override
    public void updateInterface() {
        runOnUiThread(() -> {
            mUserListAdapter.updateList(mService);
            mUserListAdapter.notifyDataSetChanged();
        });
    }

    /**
     * setup buttons and switches in the setting tab.
     */
    private void setupSettingTab() {
        Settings settings = Settings.fromDisk(this);
        if (settings != null) {
            //turn notification on/off
            Switch notificationSwitch = findViewById(R.id.userSettingNotification);
            notificationSwitch.setChecked(settings.isShowNotifications());
            notificationSwitch.setOnClickListener(view -> {
                if (notificationSwitch.isChecked()) {
                    settings.setShowNotifications(true);
                } else {
                    settings.setShowNotifications(false);
                }
                settings.save(MainTabActivity.this);
            });
        }

        //show rightmesh services
        Button fl = findViewById(R.id.rightmeshSettingButton);
        fl.setOnClickListener(v -> {
            try {
                mService.showRightMeshSettings();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        Button userNameButton = findViewById(R.id.editUsernameButtonSetting);
        userNameButton.setOnClickListener(v -> alertDialog());

        //setup userAvatar

        User user = User.fromDisk(this);
        if (user != null) {
            ImageButton userAvatar = findViewById(R.id.userSettingAvatar);
            userAvatar.setImageResource(user.getAvatar());
            Button button = findViewById(R.id.editUserAvatarButton);
            button.setOnClickListener(v -> {
                Intent avatarChooseIntent = new Intent(MainTabActivity.this,
                        ChooseAvatarActivity.class);
                avatarChooseIntent.setAction("change avatar");
                startActivity(avatarChooseIntent);
            });
        }
    }

    /**
     * creates an alert dialog box to change username.
     */
    private void alertDialog() {
        final  AlertDialog levelDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter username");

        final EditText input = new EditText(MainTabActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        User user = User.fromDisk(this);
        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String username = input.getText().toString();
            if (!username.isEmpty()) {
                if (user != null && username.length() <= 20) {
                    user.setUsername(username);
                    user.save();
                    TextView textView = findViewById(R.id.usernameTextViewSetting);
                    textView.setText(username);
                } else if (username.length() > 20) {
                    Toast.makeText(MainTabActivity.this, "Username bigger than 20"
                            + " characters", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainTabActivity.this, "Empty username not allowed!",
                        Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> { /* Exit. */ });
        levelDialog = builder.create();
        levelDialog.show();
    }

    private void dummySearchScreen(){
        LinearLayout linearLayout = findViewById(R.id.tab1);
        TextView textView = new TextView(this);
        textView.setText("bkvbjhf gkjgkjfg");
        linearLayout.addView(textView);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setupSettingTab();
    }
}