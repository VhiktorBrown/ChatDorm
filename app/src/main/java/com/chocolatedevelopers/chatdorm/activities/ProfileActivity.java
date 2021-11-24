package com.chocolatedevelopers.chatdorm.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.chocolatedevelopers.chatdorm.R;
import com.chocolatedevelopers.chatdorm.databinding.ActivityProfileBinding;
import com.chocolatedevelopers.chatdorm.model.Constants;
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
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileBinding binding;
    DatabaseReference databaseReference;
    DatabaseReference friendDatabaseReference;
    DatabaseReference friendsList;
    DatabaseReference notificationReference;
    private ProgressDialog progressDialog;
    private String current_state;
    private FirebaseUser currentUser;
    private String userName;
    String userImage;
    String receiversMessage;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = getIntent().getStringExtra("hisID");

        progressDialog = new ProgressDialog(this);

        //initializing Firebase database references
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        friendDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Friend_request");
        friendsList = FirebaseDatabase.getInstance().getReference().child("Friends");
        notificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");


        //initializing Firebase currentUser
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //setting the default value that 'current_state' will have
        current_state = "not_friends";

        //whenever user opens this activity, the 'decline Request' button should be invisible
        //it would the be made visible the user has actually been sent a request
        binding.declineFriendRequestBtn.setVisibility(View.INVISIBLE);

        //once activity is started, this progress dialog is shown to the user
        //while I get the user's information from the database
        progressDialog.setTitle("Loading user's data");
        progressDialog.setMessage("Please, wait while we load this user's data");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String displayName = snapshot.child("name").getValue().toString();
                String status = snapshot.child("status").getValue().toString();
                String image = snapshot.child("status").getValue().toString();
                String thumbImage = snapshot.child("thumb_image").getValue().toString();
                userName = displayName;

                binding.profileDisplayName.setText(displayName);
                binding.profileStatus.setText(status);
                Picasso.get().load(image).placeholder(R.drawable.profile).into(binding.profileImage);

                //here, we'll go the the logged in user's friend request records
                friendDatabaseReference.child(currentUser.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                //the snap shot will return the list of friend requests
                                if(snapshot.hasChild(userId)){

                                    /*we'll then check to see if this particular user whose
                                    profile is open is on the list.
                                    if the user is on the list, we'll check to get the request type
                                     ****/
                                    String request_type = snapshot.child(userId).child("request_type").getValue().toString();

                                    //we'll then check to see if the request type is sent or received
                                    if(request_type.equals("received")){
                                        current_state = "req_received";
                                        binding.sendFriendRequestBtn.setText(R.string.accept_friend_request);

                                        //if this user sent a friend request, we want to make visible the 'Decline Friend' button visible
                                        binding.declineFriendRequestBtn.setVisibility(View.VISIBLE);
                                        binding.declineFriendRequestBtn.setEnabled(true);
                                    } else if(request_type.equals("sent")){
                                        current_state = "req_sent";
                                        binding.sendFriendRequestBtn.setText(R.string.cancel_friend_request);

                                        //if we sent the friend request ourself to this user, we don't want to make the
                                        //'Decline Friend Request' button visible because we are the ones that did the sending
                                        //of request
                                        binding.declineFriendRequestBtn.setVisibility(View.INVISIBLE);
                                        binding.declineFriendRequestBtn.setEnabled(true);
                                    }
                                } else {

                                    /** here, we'll check the friend list in the database
                                     if this particular user is already friends with the logged in user
                                     then, we would do the needful
                                     */
                                    friendsList.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(snapshot.hasChild(userId)){
                                                current_state = "friends";
                                                binding.sendFriendRequestBtn.setText("Unfriend " + userName);
                                                binding.declineFriendRequestBtn.setVisibility(View.GONE);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.sendFriendRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // -----------THIS IS FOR SENDING FRIEND REQUEST----------

                if(current_state.equals("not_friends")) {
                    //once user presses button, I want to disable the button so that the user cannot click it again
                    binding.sendFriendRequestBtn.setEnabled(false);

                    //here, I'm saving the request sent to the particular user with their id's
                    friendDatabaseReference.child(currentUser.getUid()).child(userId).child("request_type")
                            .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                friendDatabaseReference.child(userId).child(currentUser.getUid()).child("request_type")
                                        .setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            HashMap<String, String> notification = new HashMap<>();
                                            notification.put("from", userId);
                                            notification.put("type", "request_type");

                                            databaseReference.child("Users").child(userId).addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                   String name = snapshot.child("name").getValue().toString();
                                                    receiversMessage =  name + " sent you a friend request";
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                            getToken(receiversMessage, "Friend Request", userId, userImage);

                                            notificationReference.child(userId).push().setValue(notification).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    current_state = "req_sent";
                                                    binding.sendFriendRequestBtn.setText(R.string.cancel_friend_request);

                                                    binding.declineFriendRequestBtn.setVisibility(View.INVISIBLE);
                                                    binding.declineFriendRequestBtn.setEnabled(true);
                                                }
                                            });

                                            displayToast("Friend request successfully sent");
                                        } else {
                                            displayToast("Request did not send");
                                        }
                                    }
                                });
                            } else {
                                displayToast("Something went wrong. Just couldn't send request");
                            }
                            binding.sendFriendRequestBtn.setEnabled(true);
                        }
                    });
                }


                // --------THIS PART IS FOR UN_SENDING FRIEND REQUEST------------
                if(current_state.equals("req_sent")){
                    friendDatabaseReference.child(currentUser.getUid()).child(userId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            friendDatabaseReference.child(userId).child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    displayToast("Successfully canceled request");

                                    current_state = "not_friends";

                                    //because user has unsent the friend request, I have set
                                    //the text to 'Send Friend request' again
                                    binding.sendFriendRequestBtn.setText(R.string.send_friend_request);
                                }
                            });
                        }
                    });

                    //here, I set activated the button again because
                    //it was deactivated at the click of this button.
                    binding.sendFriendRequestBtn.setEnabled(true);
                }

                if(current_state.equals("req_received")){
                    /** here is the send Friend request area. Once it has been confirmed
                     * that the user received a friend request from this person,
                     * when he or she presses this button to accept the request, it would delete the user's id
                     * from the friend request table and add this user to the friend's table
                     */
                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    friendsList.child(currentUser.getUid()).child(userId).child("friends_since")
                            .setValue(currentDate).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                               friendsList.child(userId).child(currentUser.getUid()).child("friends_since")
                                       .setValue(currentDate).addOnCompleteListener(new OnCompleteListener<Void>() {
                                   @Override
                                   public void onComplete(@NonNull Task<Void> task) {
                                       if(task.isSuccessful()){

                                           friendDatabaseReference.child(currentUser.getUid()).child(userId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                               @Override
                                               public void onSuccess(Void aVoid) {
                                                   friendDatabaseReference.child(userId).child(currentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                       @Override
                                                       public void onSuccess(Void aVoid) {

                                                           //here, I set activated the button again because
                                                           //it was deactivated at the click of this button.
                                                           binding.sendFriendRequestBtn.setEnabled(true);
                                                           current_state = "friends";

                                                           //because user has unsent the friend request, I have set
                                                           //the text to 'Send Friend request' again
                                                           binding.sendFriendRequestBtn.setText("UnFriend "+ userName);
                                                           binding.declineFriendRequestBtn.setVisibility(View.INVISIBLE);
                                                           binding.declineFriendRequestBtn.setEnabled(false);
                                                       }
                                                   });
                                               }
                                           });
                                       }
                                   }
                               });
                            }
                        }
                    });
                }
            }
        });

        binding.declineFriendRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    private void getToken(String message, String title, String hisID, String hisImage){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(hisID);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String token = snapshot.child("token").getValue().toString();
                String name = snapshot.child("name").getValue().toString();

                JSONObject to = new JSONObject();
                JSONObject data = new JSONObject();

                try{
                    data.put("title", title);
                    data.put("message", message);
                    data.put("hisID", hisID);
                    data.put("hisImage", hisImage);

                    to.put("to", token);
                    to.put("data", data);

                    sendNotification(to);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendNotification(JSONObject to) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Constants.NOTIFICATION_URL,
                to, response -> {
            Log.d("notification", "send Notification" + response);
        }, error -> {
            Log.d("notification", "send Notification" + error);
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> map = new HashMap<>();
                map.put("Authorization", "key " + Constants.NOTIFICATION_URL);
                map.put("content-Type", "application/json");
                return map;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(ProfileActivity.this);
        request.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);

    }

    private void displayToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}