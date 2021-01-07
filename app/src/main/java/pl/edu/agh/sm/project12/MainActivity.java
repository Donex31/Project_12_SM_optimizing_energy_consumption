package pl.edu.agh.sm.project12;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import pl.edu.agh.sm.project12.datacollection.DataCollectionActivity;
import pl.edu.agh.sm.project12.ocr.FaceDetectionOcr;
import pl.edu.agh.sm.project12.ocr.TextRecognitionOcr;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.text.Text;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_CODE_PICK_ACCOUNT = 101;
    static final int REQUEST_ACCOUNT_AUTHORIZATION = 102;
    static final int REQUEST_PERMISSIONS = 13;

    public static String accessToken;

    private Account mAccount;
    private ProgressDialog mProgressDialog;
    private TextView textView;
    private TextRecognitionOcr recognizer = new TextRecognitionOcr();
    private FaceDetectionOcr detector = new FaceDetectionOcr();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(this);
        textView = findViewById(R.id.textView);
        Button textRecognitionCloudButton = findViewById(R.id.textRecognitionCloudButton);

        if(Power.isConnected(getBaseContext())){
            textView.setText("Unplug the charger!!");
        }

        textRecognitionCloudButton.setOnClickListener(v -> ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_PERMISSIONS));
    }


    public void recognizeText(View view) {
        textView.setText("Recognition started...");

        InputImage inputImage = recognizer.getInputImage(getResources(), R.drawable.germany);

        Instant start = Instant.now();

        Task<Text> process = recognizer.process(inputImage);

        process.addOnSuccessListener(visionText -> {
            Instant finish = Instant.now();
            Duration between = Duration.between(start, finish);
            textView.setText("Elapsed time: " + between.toString());
        }).addOnFailureListener(e -> {
            textView.setText("Error");
        });
    }

    public void detectFaces(View view) {
        textView.setText("Detection started...");

        InputImage inputImage = detector.getInputImage(getResources(), R.drawable.passat);

        Instant start = Instant.now();

        Task<List<Face>> process = detector.process(inputImage);

        process.addOnSuccessListener(visionText -> {
            Instant finish = Instant.now();
            Duration between = Duration.between(start, finish);
            textView.setText("Elapsed time: " + between.toString());
        }).addOnFailureListener(e -> {
            textView.setText("Error");
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAuthToken();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AccountManager am = AccountManager.get(this);
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                for (Account account : accounts) {
                    if (account.name.equals(email)) {
                        mAccount = account;
                        break;
                    }
                }
                getAuthToken();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "No Account Selected", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == REQUEST_ACCOUNT_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                Bundle extra = data.getExtras();
                onTokenReceived(extra.getString("authtoken"));
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Authorization Failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    private void getAuthToken() {
        String SCOPE = "oauth2:https://www.googleapis.com/auth/cloud-platform";
        if (mAccount == null) {
            pickUserAccount();
        } else {
            new GetOAuthToken(MainActivity.this, mAccount, SCOPE, REQUEST_ACCOUNT_AUTHORIZATION)
                    .execute();
        }
    }

    public void onTokenReceived(String token) {
        accessToken = token;
        textView.setText("Authorization was successful!!!");
    }

    public void openDataCollectionActivity(View view) {
        Intent intent = new Intent(this, DataCollectionActivity.class);
        startActivity(intent);
    }
}