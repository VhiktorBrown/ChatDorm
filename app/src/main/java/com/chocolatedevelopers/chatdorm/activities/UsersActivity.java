package com.chocolatedevelopers.chatdorm.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chocolatedevelopers.chatdorm.R;
import com.chocolatedevelopers.chatdorm.databinding.ActivityUsersBinding;
import com.chocolatedevelopers.chatdorm.model.User;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {
ActivityUsersBinding binding;
Toolbar toolbar;
DatabaseReference databaseReference;
private FirebaseRecyclerAdapter adapter;
private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.users_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //instantiating Database Reference to gain access to all Users in the database
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        binding.usersRecyclerView.setHasFixedSize(true);
        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //method for fetching the list of users from database.
        fetch();
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private void fetch(){
        Query query = databaseReference;

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(query, new SnapshotParser<User>() {
                    @NonNull
                    @Override
                    public User parseSnapshot(@NonNull DataSnapshot snapshot) {
                        String key = snapshot.getKey();

                            return new User(snapshot.child("name").getValue().toString(),
                                    snapshot.child("image").getValue().toString(),
                                    snapshot.child("status").getValue().toString(),
                                    snapshot.child("thumb_image").getValue().toString());
                    }
                })
                .build();

        adapter = new FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull User model) {
                holder.setDisplayName(model.getName());
                holder.setStatus(model.getStatus());
                holder.setUsersImage(model.getThumbImage(), getApplicationContext());

                String user_id = getRef(position).getKey();

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("hisID", user_id);
                        startActivity(profileIntent);
                    }
                });
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);
                return new UsersViewHolder(view);
            }
        };
        binding.usersRecyclerView.setAdapter(adapter);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {
        View mView;
        CircleImageView usersImage;
        TextView displayName;
        TextView status;
        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            usersImage = itemView.findViewById(R.id.users_image);
            displayName = itemView.findViewById(R.id.display_name);
            status = itemView.findViewById(R.id.status);
        }

        public void setDisplayName(String name){
            displayName.setText(name);
        }
        public void setStatus(String statusMessage){
            status.setText(statusMessage);
        }
        public void setUsersImage(String thumbImage, Context context){
            Picasso.get().load(thumbImage).placeholder(R.drawable.profile).into(usersImage);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.home:
            startActivity(new Intent(UsersActivity.this, MainActivity.class));
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}