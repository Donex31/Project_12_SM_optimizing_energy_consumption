package pl.edu.agh.sm.project12.datacollection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.agh.sm.project12.R;

public class DataCollectionActivity extends AppCompatActivity {
    private static final int RC_START_DATA_COLLECTION = 1;

    private static final String BUNDLE_TASK_IDS = "task_ids";
    private static final String BUNDLE_TASKS = "tasks";
    private static final String PREFS_NAME = "DataCollectionPreferences";

    private final ArrayList<String> taskIds = new ArrayList<>();
    private final HashMap<String, TaskData> tasks = new HashMap<>();
    private DataCollectionTaskListAdapter dataCollectionTaskListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Data collection");
        loadSettings();

        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(this, StartDataCollectionActivity.class);
            startActivityForResult(intent, RC_START_DATA_COLLECTION);
        });

        ListView dataCollectionsTasksListView = findViewById(R.id.dataCollectionsTaskListView);
        dataCollectionTaskListAdapter = new DataCollectionTaskListAdapter();
        dataCollectionsTasksListView.setAdapter(dataCollectionTaskListAdapter);
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings.contains(BUNDLE_TASK_IDS)) {
            ArrayList<?> ids = new Gson().fromJson(settings.getString(BUNDLE_TASK_IDS, "[]"), ArrayList.class);
            ids.forEach(i -> taskIds.add((String) i));
        }
        if (settings.contains(BUNDLE_TASKS)) {
            HashMap<?, ?> savedTasks = new Gson().fromJson(settings.getString(BUNDLE_TASKS, "{}"),
                    new TypeToken<HashMap<String, TaskData>>(){}.getType());
            savedTasks.forEach((k, v) -> tasks.put((String) k, (TaskData) v));
        }
    }

    @Override
    protected void onStop() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(BUNDLE_TASK_IDS, new Gson().toJson(taskIds));
        editor.putString(BUNDLE_TASKS, new Gson().toJson(tasks));
        editor.apply();

        super.onStop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_START_DATA_COLLECTION) {
            if (resultCode == RESULT_OK) {
                String name = data.getStringExtra("name");
                int iterations = data.getIntExtra("iterations", 0);
                boolean useCloud = data.getBooleanExtra("useCloud", false);

                startDataCollection(TaskData.builder()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .iterations(iterations)
                        .useCloud(useCloud)
                        .progress(0d)
                        .build());
            }
        }
    }

    private void startDataCollection(TaskData data) {
        Data inputData = new Data.Builder()
                .putString(DataCollectionWorker.KEY_NAME, data.getName())
                .putInt(DataCollectionWorker.KEY_ITERATIONS, data.getIterations())
                .putBoolean(DataCollectionWorker.KEY_CLOUD, data.useCloud)
                .build();
        WorkContinuation workContinuation = WorkManager.getInstance(getApplicationContext())
                .beginWith(new OneTimeWorkRequest.Builder(DataCollectionWorker.class)
                        .setInputData(inputData)
                        .build());
        workContinuation.enqueue();
        workContinuation.getWorkInfosLiveData()
                .observe(this, workInfos -> {
                    WorkInfo workInfo = workInfos.get(0);
                    boolean finished = workInfo.getState().isFinished();
                    if (finished) {
                        data.setProgress(data.getIterations());
                    } else {
                        int progress = workInfo.getProgress().getInt(DataCollectionWorker.KEY_PROGRESS, 0);
                        data.setProgress(1d * progress / data.getIterations());
                    }
                    dataCollectionTaskListAdapter.notifyDataSetChanged();
                });

        taskIds.add(data.getId());
        tasks.put(data.getId(), data);
        dataCollectionTaskListAdapter.notifyDataSetChanged();
    }

    private class DataCollectionTaskListAdapter extends ArrayAdapter<String> {
        public DataCollectionTaskListAdapter() {
            super(DataCollectionActivity.this, -1, taskIds);
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
    private static class TaskData {
        private String id;
        private String name;
        private int iterations;
        private boolean useCloud;
        private double progress;
    }
}