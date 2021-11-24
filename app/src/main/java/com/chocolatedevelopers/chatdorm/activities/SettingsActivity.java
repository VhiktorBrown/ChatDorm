package com.chocolatedevelopers.chatdorm.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.chocolatedevelopers.chatdorm.R;
import com.chocolatedevelopers.chatdorm.databinding.ActivitySettingsBinding;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    DatabaseReference databaseReference;
    ActivitySettingsBinding binding;
    FirebaseUser currentUser;
    String uid;
    private static final int GALLERY_PICK = 1;
    StorageReference imageStorage;
    ProgressDialog progressDialog;
    byte[] thumb_byte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = currentUser.getUid();

        // initializing Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        databaseReference.keepSynced(true);

        //initializing Firebase Storage
        imageStorage = FirebaseStorage.getInstance().getReference().child("profile_images");

        //Here, I'm getting the user's name and status and displaying it in text view
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                String image = snapshot.child("image").getValue().toString();
                String thumb_image = snapshot.child("thumb_image").getValue().toString();

                binding.settingsUserName.setText(name);
                binding.statusText.setText(status);
                if(!image.equals("default")) {
                    Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.profile).into(binding.settingsUserImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(image).placeholder(R.drawable.profile).into(binding.settingsUserImage);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.changeStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, StatusActivity.class);
                intent.putExtra("status", binding.statusText.getText().toString());
                startActivity(intent);
            }
        });

        binding.changeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK){
            Uri imageUri = data.getData();

            CropImage.activity(imageUri)
                    .setAspectRatio(1, 1)
                    .setMinCropWindowSize(500, 500)
                    .start(this);
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK){
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Uploading image");
                progressDialog.setMessage("Please wait, while we save your image");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                Uri resultUri = result.getUri();
                File thumb_filePath = new File(resultUri.getPath());
                try {
                    Bitmap compressedImage = new Compressor(this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_filePath);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    thumb_byte = baos.toByteArray();

                } catch (IOException e) {
                    e.printStackTrace();
                }



                StorageReference filePath = imageStorage.child( currentUser.getUid() + ".jpg");
                StorageReference thumb_path = imageStorage.child("thumb_files").child(currentUser.getUid() + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            getDownloadUrl(filePath, thumb_path, thumb_byte);
                            displayToast("Image was successfully saved");
                        } else {
                            progressDialog.dismiss();
                            displayToast("Something happened oh! It didn't save");
                        }
                    }
                });

            } else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error = result.getError();
            }
        }
    }

    private void getDownloadUrl(StorageReference filePath, StorageReference thumb_filePath, byte[] thumb_path){
        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //gets the download url after storing the image in database
                String download_Url = uri.toString();
                displayToast(uri.toString());

                //this saves the thumbPath to the database
                UploadTask uploadTask = thumb_filePath.putBytes(thumb_path);

                //this checks if the byte file was successfully saved or not
                uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            thumb_filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //get the download url after uploading the path for the thumbnail
                                    String thumb_downloadUrl = uri.toString();

                                    //create hashMap that would store more than one set of data
                                   Map updateHashMap = new HashMap();
                                   updateHashMap.put("image", download_Url);
                                   updateHashMap.put("thumb_image", thumb_downloadUrl);

                                    databaseReference.updateChildren(updateHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                progressDialog.dismiss();
                                                displayToast("Successfully Uploaded");
                                            } else {
                                                progressDialog.dismiss();
                                                displayToast("It wasn't successfully uploaded");
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                });

            }
        });
    }

    private String random(){
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(10);
        char tempChar;
        for(int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96 ) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    private void displayToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}