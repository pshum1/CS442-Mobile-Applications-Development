package com.example.inspirationrewards;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity implements View.OnClickListener{

    RecyclerView recyclerView;
    LeaderboardAdapter mAdapter;

    String getProfile_api_response;
    List<AllProfiles> profilesContent;
    String currentUser;
    String passwrd;
    String temp_username;
    String temp_password;
    String currentUser_FirstName;
    String currentUser_LastName;
    ProgressBar progressBar;
    int pointsToAward;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        Toolbar toolbar = findViewById(R.id.toolbar4);
        setSupportActionBar(toolbar);
        profilesContent = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        progressBar = findViewById(R.id.progressBar3);
        progressBar.setVisibility(View.INVISIBLE);

        SharedPreferences sharedPreferences = getSharedPreferences("credentials", Context.MODE_PRIVATE);

        if(getIntent().hasExtra("firstName") && getIntent().hasExtra("lastName")){
            currentUser_FirstName = getIntent().getStringExtra("firstName");
            currentUser_LastName = getIntent().getStringExtra("lastName");
        }
        if (getIntent().hasExtra("username") && getIntent().hasExtra("password") && getIntent().hasExtra("pointsToAward")) {
            currentUser = getIntent().getStringExtra("username");
            passwrd = getIntent().getStringExtra("password");
            pointsToAward = getIntent().getIntExtra("pointsToAward",0);
            new GetAllProfilesAPIAsyncTask(LeaderboardActivity.this).execute(currentUser, passwrd);
        } else if (sharedPreferences != null) {
            temp_username = sharedPreferences.getString("username", "");
            temp_password = sharedPreferences.getString("password", "");
            if (temp_username != null && temp_password != null) {
                currentUser = temp_username;
                passwrd = temp_password;
            }
            new GetAllProfilesAPIAsyncTask(LeaderboardActivity.this).execute(currentUser, passwrd);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error")
                    .setIcon(R.drawable.icon)
                    .setMessage("Something Went Wrong")
                    .setCancelable(false)
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(LeaderboardActivity.this, ProfileActivity.class));
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        /*View arrowLogo = toolbar.getChildAt(0);
        arrowLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LeaderboardActivity.this,ProfileActivity.class)
                        .putExtra("username",currentUser)
                        .putExtra("password",passwrd));
                finish();
            }
        });*/

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void parseJSONData(String getProfile_api_response) {
        try {
            JSONArray jsonArray = new JSONArray(getProfile_api_response);
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject object = jsonArray.getJSONObject(i);
                final AllProfiles profilesData = new AllProfiles();
                profilesData.setStudentID(object.getString("studentId"));
                profilesData.setFirstName(object.getString("firstName"));
                profilesData.setLastName(object.getString("lastName"));
                profilesData.setUsername(object.getString("username"));
                profilesData.setStory(object.getString("story"));
                profilesData.setPosition(object.getString("position"));
                profilesData.setPointsToAward(object.getInt("pointsToAward"));
                profilesData.setAdmin(object.getBoolean("admin"));
                profilesData.setDepartment(object.getString("department"));
                profilesData.setImageByteEncoded(object.getString("imageBytes"));
                profilesData.setLocation(object.getString("location"));

                String val = "null";
                if (object.has("rewards")) {
                    if (object.getString("rewards").startsWith(val)) {
                        String s = object.getString("rewards");
                        String ans = s.replace(val, "");
                    } else {
                        JSONArray objectJSONArray = object.getJSONArray("rewards");
                        for (int j = 0; j < objectJSONArray.length(); j++) {
                            Rewards rewardsContent = new Rewards();
                            JSONObject jsonObject = objectJSONArray.getJSONObject(j);
                            rewardsContent.setUsername(jsonObject.getString("username"));
                            rewardsContent.setName(jsonObject.getString("name"));
                            rewardsContent.setDate(jsonObject.getString("date"));
                            rewardsContent.setNotes(jsonObject.getString("notes"));
                            rewardsContent.setValue(jsonObject.getInt("value"));
                            profilesData.rewardsContents.add(rewardsContent);
                        }
                    }
                }
                profilesContent.add(profilesData);
                Collections.sort(profilesContent, new Comparator<AllProfiles>() {
                    @Override
                    public int compare(AllProfiles o1, AllProfiles o2) {
                        int sum_lhs = 0;
                        for(Rewards content : o1.rewardsContents){
                            sum_lhs += content.getValue();
                        }
                        int sum_rhs = 0;
                        for(Rewards content : o2.rewardsContents){
                            sum_rhs += content.getValue();
                        }
                        return sum_rhs - sum_lhs;
                    }
                });
                mAdapter = new LeaderboardAdapter(LeaderboardActivity.this, profilesContent, currentUser);
                recyclerView.setAdapter(mAdapter);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(View v) {
        int i = recyclerView.getChildLayoutPosition(v);
        AllProfiles data = profilesContent.get(i);
        if (data.getUsername().equals(currentUser)) {
            new CustomToast(LeaderboardActivity.this).showCustomToast("You cannot reward yourself", Color.RED);
            return;
        }
        Intent intent = new Intent(LeaderboardActivity.this, AwardActivity.class);
        intent.putExtra("profile_data", data);
        intent.putExtra("username", currentUser);
        intent.putExtra("password", passwrd);
        intent.putExtra("pointsToAward",pointsToAward);
        intent.putExtra("firstName",currentUser_FirstName);
        intent.putExtra("lastName",currentUser_LastName);
        startActivity(intent);
        finish();
    }

    public void sendResult(String result, String response) {
        String status = "";
        String message = "";
        if (result.toLowerCase().contains("failed")) {
            try {
                JSONObject json = new JSONObject(response);
                String s = json.getString("errordetails");
                JSONObject jsonObject = new JSONObject(s);
                status = jsonObject.getString("status");
                message = jsonObject.getString("message");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new CustomToast(LeaderboardActivity.this).showCustomToast(status+"  " + message, Color.RED);
        } else {
            new CustomToast(LeaderboardActivity.this).showCustomToast("Process: " + result, Color.GREEN);
            getProfile_api_response = response;
            parseJSONData(getProfile_api_response);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(LeaderboardActivity.this,ProfileActivity.class)
                .putExtra("username",currentUser)
                .putExtra("password",passwrd));
        finish();
    }


}
