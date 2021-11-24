package com.chocolatedevelopers.chatdorm.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.chocolatedevelopers.chatdorm.R;
import com.chocolatedevelopers.chatdorm.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceIdReceiver;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    Toolbar toolbar;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;
    DatabaseReference userDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        toolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        binding.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               String email = binding.emailEditText.getText().toString();
               String password = binding.passwordEditText.getText().toString();

               if(!TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)){
                   progressDialog.setTitle("Logging In");
                   progressDialog.setMessage("Please wait, while we check your credentials");
                   progressDialog.setCanceledOnTouchOutside(false);
                   progressDialog.show();
                   loginUser(email, password);
               }
            }
        });
    }

    private void loginUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            // gets token for the purpose of storing it in the database
                            //we'll need this token when trying to send notifications
                            String token = String.valueOf(FirebaseMessaging.getInstance().getToken());
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            userDatabase.child(userId).child("token").setValue(token).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressDialog.dismiss();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            });

                        } else {
                            progressDialog.hide();
                            displayToast(task.getException().toString());
                        }
                    }
                });
    }

    private void displayToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}