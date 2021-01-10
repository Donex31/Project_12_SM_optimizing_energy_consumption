package pl.edu.agh.sm.project12.datacollection;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

import pl.edu.agh.sm.project12.R;

public class StartDataCollectionActivity extends AppCompatActivity {
    private int iterations = 0;
    private EditText nameEditText;
    private Switch useCloud;
    private NumberPicker picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_data_collection);
        setTitle("Start data collection");

        TextView iterationsText = findViewById(R.id.numberOfIterationsText);
        SeekBar iterationsSeekBar = findViewById(R.id.numberOfIterationsSeekBar);
        useCloud = findViewById(R.id.useCloudSwitch);

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

        picker = findViewById(R.id.numberPicker);
        picker.setDisplayedValues( new String[] { "Switch", "Neural network", "Random" } );
        picker.setMinValue(0);
        picker.setMaxValue(2);
    }

    public void start(View view) {
        String name = nameEditText.getText().toString();
        int iterations = this.iterations;
        File imagesDirectory = new File(getApplicationContext().getFilesDir(), "images");
        boolean useCloud = this.useCloud.isChecked();
        int processingMethod  = this.picker.getValue();

        Intent intent = new Intent(this, ResultDataCollectionActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("iterations", iterations);
        intent.putExtra("images_directory", imagesDirectory.getAbsolutePath());
        intent.putExtra("useCloud", useCloud);
        intent.putExtra("processingMethod", processingMethod);
        startActivity(intent);
    }
}