package io.left.meshenger.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;

import io.left.meshenger.Models.User;
import io.left.meshenger.R;

public class ChooseAvatarActivity extends Activity {
    User mUser;
    //used for the table layout
    private final int ROWS = 9;
    private final int COLUMNS = 3;

    private int userAvatarID=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_avatar);
        mUser = User.fromDisk(this);
        setupAvatars();
        userAvatarID = R.mipmap.account_default;
        saveAvatar(userAvatarID);

    }

    /**
     * The function finds the scrollview in the layout and creates a dynamic
     * table layout of avatars user can choose from.
     */
    private void setupAvatars() {
       //keep track of the avatar
        int avatarNum = 1;

        mUser = User.fromDisk(this);
        ScrollView scrollView = findViewById(R.id.avatarScrollView);
        TableLayout tableLayout = new TableLayout(this);

        for (int r = 0; r < ROWS; r++) {
            final int curRow = r;
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                            TableLayout.LayoutParams.MATCH_PARENT, 1.0f));
            for (int c = 0; c < COLUMNS; c++) {
                int curCol = c;
                final ImageButton imageButton = new ImageButton(this);

                //setting the avatar
                int id = getResources().getIdentifier("avatar"+avatarNum
                       , "mipmap", getPackageName());
                imageButton.setImageResource(id);
                imageButton.setBackgroundResource(R.color.white);
                imageButton.setLayoutParams(
                        new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.MATCH_PARENT, 1.0f));
                imageButton.setPadding(0, 0, 0, 0);
                tableRow.addView(imageButton);
                int finalAvatarNum = avatarNum;
                imageButton.setOnClickListener(
                        new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        userAvatarID = getResources().getIdentifier(
                                "avatar"+ finalAvatarNum, "mipmap", getPackageName());
                        ImageButton button = findViewById(R.id.selectedAvatar);
                        button.setImageResource(userAvatarID);
                    }
                });
                avatarNum++;
            }
            tableLayout.addView(tableRow);
        }
        scrollView.addView(tableLayout);
    }

    /**
     * Function saves the avatar chosen by the user.
     * function takes user to the main screen
     * @param id the id of the avatar user chose.
     */
    private void saveAvatar(int id) {
        Button saveButton = findViewById(R.id.saveUserAvatarButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUser.setUserAvatar(id);
                mUser.save();
                Intent intent = new Intent(ChooseAvatarActivity.this, MainTabActivity.class);
                startActivity(intent);
            }
        });
    }
}
