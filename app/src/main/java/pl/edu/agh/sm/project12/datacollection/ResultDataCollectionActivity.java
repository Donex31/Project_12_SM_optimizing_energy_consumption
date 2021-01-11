package pl.edu.agh.sm.project12.datacollection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.agh.sm.project12.R;

public class ResultDataCollectionActivity extends AppCompatActivity {
    private static final String TAG = ResultDataCollectionActivity.class.getSimpleName();

    private static final int RC_START_DATA_COLLECTION = 1;

    private final ArrayList<String> taskIds = new ArrayList<>();
    private final HashMap<String, TaskData> tasks = new HashMap<>();
    private DataCollectionTaskListAdapter dataCollectionTaskListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Data collection");

        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView dataCollectionsTasksListView = findViewById(R.id.dataCollectionsTaskListView);
        dataCollectionTaskListAdapter = new DataCollectionTaskListAdapter();
        dataCollectionsTasksListView.setAdapter(dataCollectionTaskListAdapter);

        Intent data = getIntent();
        String name = data.getStringExtra("name");
        int iterations = data.getIntExtra("iterations", 0);
        String imagesDirPath = data.getStringExtra("images_directory");
        boolean useCloud = data.getBooleanExtra("useCloud", false);
        int processingMethod = data.getIntExtra("processingMethod", 0);
        boolean isWiFiConnected = data.getBooleanExtra("isWiFiConnected", false);

        startDataCollection(TaskData.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .iterations(iterations)
                .useCloud(useCloud)
                .isWiFiConnected(isWiFiConnected)
                .processingMethod(processingMethod)
                .progress(0d)
                .imagesDirPath(imagesDirPath)
                .build());
    }

    @Override
    protected void onStop() {
        super.onStop();
        throw new RuntimeException("Not supported");
    }

    private void startDataCollection(TaskData data) {
        int fileCount = new File(data.getImagesDirPath()).listFiles().length;
        new DataCollectionWorker(getApplicationContext(), progress -> {
            data.setProgress(1d * progress / fileCount / data.iterations);
        }).execute(data);

        taskIds.add(data.getId());
        tasks.put(data.getId(), data);
        dataCollectionTaskListAdapter.notifyDataSetChanged();
    }

    private class DataCollectionTaskListAdapter extends ArrayAdapter<String> {
        public DataCollectionTaskListAdapter() {
            super(ResultDataCollectionActivity.this, -1, taskIds);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.data_collection_task_item, parent, false);
            TextView nameTextView = rowView.findViewById(R.id.taskNameTextView);
            ProgressBar taskProgressBar = rowView.findViewById(R.id.taskProgressBar);
            ImageView imageView = rowView.findViewById(R.id.taskIcon);

            String id = taskIds.get(position);
            TaskData data = tasks.get(id);
            nameTextView.setText(data.getName());
            taskProgressBar.setMax(1000);
            taskProgressBar.setProgress((int) (data.progress * 1000));

            imageView.setImageResource(R.drawable.common_google_signin_btn_icon_light_focused);

            return rowView;
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskData {
        private String id;
        private String name;
        private int iterations;
        private boolean useCloud;
        private double progress;
        private String imagesDirPath;
        private int processingMethod;
        private boolean isWiFiConnected;
    }
}