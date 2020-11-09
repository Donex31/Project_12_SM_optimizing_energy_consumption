package com.example.project_12_sm_optimizing_energy_consumption;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
    }

    public void testConnection(View view) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://optimizingenergyconsumption.appspot.com/";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> textView.setText("Response is: "+ response),
                error -> textView.setText("That didn't work!"));
        queue.add(stringRequest);
    }
}