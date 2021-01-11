package pl.edu.agh.sm.project12;

import android.accounts.Account;
import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;

class GetOAuthToken extends AsyncTask<Void, Void, Void> {
    Activity mActivity;
    Account mAccount;
    String mScope;

    GetOAuthToken(Activity activity, Account account, String scope) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mAccount = account;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            String token = fetchToken();
            if (token != null) {
                MainActivity.accessToken = token;
                mActivity.runOnUiThread(() -> Toast.makeText(mActivity,
                        "Token: " + MainActivity.accessToken, Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String fetchToken() throws IOException {
        String accessToken;
        try {
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope);
            GoogleAuthUtil.clearToken(mActivity, accessToken);
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope);
            return accessToken;
        } catch (GoogleAuthException e) {
            throw new RuntimeException(e);
        }
    }
}