package com.chocolatedevelopers.chatdorm.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.chocolatedevelopers.chatdorm.R;
import com.chocolatedevelopers.chatdorm.adapters.SectionsPagerAdapter;
import com.chocolatedevelopers.chatdorm.databinding.ActivityMainBinding;
import com.chocolatedevelopers.chatdorm.databinding.AppBarLayoutBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    ActivityMainBinding binding;
    Toolbar toolbar;
    ViewPager viewPager;
    SectionsPagerAdapter sectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.main_toolbar);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser  = firebaseAuth.getCurrentUser();
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chat Dorm");
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        binding.mainViewPager.setAdapter(sectionsPagerAdapter);

        binding.mainTabLayout.setupWithViewPager(binding.mainViewPager);


    }

    @Override
    protected void onStart() {
        super.onStart();
        if(firebaseUser == null){
            sendToStart();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.logout_menu){
            FirebaseAuth.getInstance().signOut();
            sendToStart();
        } else if(item.getItemId() == R.id.main_settings){
            sendToAccount();
        }

        if(item.getItemId() == R.id.main_all_users){
            startActivity(new Intent(MainActivity.this, UsersActivity.class));
        }
        return true;
    }

    private void sendToStart(){
        startActivity(new Intent(MainActivity.this, StartActivity.class));
        finish();
    }

    private void sendToAccount(){
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }
}