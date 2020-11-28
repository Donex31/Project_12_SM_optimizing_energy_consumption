package pl.edu.agh.sm.project12.datacollection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import pl.edu.agh.sm.project12.R;

public class StartDataCollectionActivity extends AppCompatActivity {
    private int iterations = 0;
    private EditText nameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_data_collection);
        setTitle("Start data collection");

        TextView iterationsText = findViewById(R.id.numberOfIterationsText);
        SeekBar iterationsSeekBar = findViewById(R.id.numberOfIterationsSeekBar);
        iterationsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                iterationsText.setText(progress + " iterations");
                StartDataCollectionActivity.this.iterations = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        nameEditText = findViewById(R.id.nameEditText);
    }

    public void start(View view) {
        String name = nameEditText.getText().toString();
        int iterations = this.iterations;

        Intent intent = new Intent();
        intent.putExtra("name", name);
        intent.putExtra("iterations", iterations);
        setResult(RESULT_OK, intent);
        finish();
    }
}