package com.chocolatedevelopers.chatdorm.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.chocolatedevelopers.chatdorm.R;
import com.chocolatedevelopers.chatdorm.databinding.ActivityStatusBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {
    DatabaseReference databaseReference;
    FirebaseUser currentUser;
    ActivityStatusBinding binding;
    String uid, status;
    Toolbar toolbar;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        status = intent.getStringExtra("status");

        toolbar = findViewById(R.id.status_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Status Update");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        binding.changeStatusEditText.setText(status);

        binding.changeStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = new ProgressDialog(StatusActivity.this);
                progressDialog.setTitle("Saving Changes");
                progressDialog.setMessage("Please wait, while we save the changes.");
                progressDialog.show();

                databaseReference.child("status").setValue(binding.changeStatusEditText.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            displayToast("Some error occurred. Please, try again.");
                        }
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void displayToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}