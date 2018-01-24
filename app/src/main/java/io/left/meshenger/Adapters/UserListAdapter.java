package io.left.meshenger.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

import io.left.meshenger.Models.User;
import io.left.meshenger.R;


public class UserListAdapter extends BaseAdapter {
    private Context myContex;
    private List<User> userList;

    /**
     * constructor for the userlistAdapter
     * @param context context of the activity
     * @param userList list of all the users nearby
     */
    public UserListAdapter(Context context, List userList) {
        this.myContex = context;
        this.userList = userList;
    }

    /**
     * returns the size of the user list.
     * @return
     */
    @Override
    public int getCount() {
        return userList.size();
    }

    /**
     * returns a User on the list.
     * @param i position of the user on the list.
     * @return
     */
    @Override
    public Object getItem(int i) {
        return userList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = View.inflate(myContex, R.layout.user_list, null);
        ImageView userAvatar = (ImageView) v.findViewById(R.id.user_Avatar);
        userAvatar.setImageResource(R.mipmap.avatar_00);
        TextView userName = (TextView) v.findViewById(R.id.userNameText);
        userName.setText(userList.get(i).getUserName());
        return v;
    }
}

