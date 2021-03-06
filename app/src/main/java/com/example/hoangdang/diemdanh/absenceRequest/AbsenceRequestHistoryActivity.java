package com.example.hoangdang.diemdanh.absenceRequest;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.hoangdang.diemdanh.R;
import com.example.hoangdang.diemdanh.SupportClass.AbsenceRequest;
import com.example.hoangdang.diemdanh.SupportClass.AppVariable;
import com.example.hoangdang.diemdanh.SupportClass.Network;
import com.example.hoangdang.diemdanh.SupportClass.SecurePreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AbsenceRequestHistoryActivity extends AppCompatActivity {

    @BindView(R.id.absence_request_history_listView)
    ListView _listview;

    ProgressDialog progressDialog;
    ArrayList<AbsenceRequest> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absence_request_history);

        ButterKnife.bind(this);

        this.setTitle("HISTORY");

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressDialog = new ProgressDialog(this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(false);

        SharedPreferences pref = new SecurePreferences(this);
        new GetSentAbsenceRequestListTask().execute(
                pref.getString(AppVariable.USER_TOKEN, null),
                pref.getString(AppVariable.USER_ID, null));

    }

    private void setListViewAdapter() {
        _listview.setAdapter(new ListAbsenceRequestAdapter(list, this));

        _listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AbsenceRequest ar = list.get(position);

                new AlertDialog.Builder(AbsenceRequestHistoryActivity.this)
                        .setTitle(ar.fromDate + " to " + ar.toDate)
                        .setMessage(ar.reason)
                        .setNegativeButton("Close", null).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class GetSentAbsenceRequestListTask extends AsyncTask<String, Void, Integer> {
        private Exception exception;
        private String strJsonResponse;

        @Override
        protected void onPreExecute(){
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            try {
                URL url = new URL(Network.API_GET_SENT_ABSENCE_REQUEST_LIST);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    //prepare json data
                    JSONObject jsonUserData = new JSONObject();
                    jsonUserData.put("token", params[0]);
                    jsonUserData.put("id", params[1]);

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //write
                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(jsonUserData.toString());
                    writer.flush();

                    //check http response code
                    int status = connection.getResponseCode();
                    switch (status){
                        case HttpURLConnection.HTTP_OK:
                            //read response
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                            StringBuilder sb = new StringBuilder();
                            String line;

                            while ((line = bufferedReader.readLine()) != null) {
                                sb.append(line).append("\n");
                            }

                            bufferedReader.close();
                            strJsonResponse = sb.toString();

                            return HttpURLConnection.HTTP_OK;
                        default:
                            exception = new Exception(connection.getResponseMessage());
                            return 0;
                    }
                }
                finally{
                    connection.disconnect();
                }
            }
            catch(Exception e) {
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            if (status != HttpURLConnection.HTTP_OK){
                progressDialog.dismiss();
            }
            else {
                try{
                    JSONObject jsonObject = new JSONObject(strJsonResponse);
                    String result = jsonObject.getString("result");
                    if (result.equals("failure")){
                        progressDialog.dismiss();
                        return;
                    }

                    int length = Integer.valueOf(jsonObject.getString("total_items"));
                    JSONArray coursesJson = jsonObject.getJSONArray("absence_requests");

                    list = new ArrayList<>();
                    for (int i = 0; i < length; i++){
                        JSONObject c = coursesJson.getJSONObject(i);

                        list.add(new AbsenceRequest(
                                c.getInt("id"),
                                c.getString("start_date").substring(0,10),
                                c.getString("end_date").substring(0,10),
                                c.getString("reason"),
                                c.getInt("status"),
                                //c.getString("created_at")
                                c.getString("created_at").substring(0,10)
                        ));
                   }

                    setListViewAdapter();
                    progressDialog.dismiss();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
