package com.atlassian.mobilekit.module.feedback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.atlassian.mobilekit.module.core.DeviceInfo;
import com.atlassian.mobilekit.module.core.utils.SystemUtils;
import com.atlassian.mobilekit.module.feedback.commands.Result;

public class FeedbackActivity extends AppCompatActivity
        implements ProgressDialogActions, FinishAction, SendFeedbackListener {

    private EditText feedbackEt;
    private EditText feedbackEmailEt;
    private MenuItem sendMenuItem = null;
    private DeviceInfo deviceInfo = null;

    public static Intent getIntent(Context src) {
        Intent intent = new Intent(src, FeedbackActivity.class);
        if (!(src instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TO make sure scroll works with editTexts
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        feedbackEt = (EditText) findViewById(R.id.feedbackIssueDescriptionEditText);
        feedbackEt.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }
        });
        feedbackEmailEt = (EditText) findViewById(R.id.feedbackIssueEmailEditText);
        feedbackEmailEt.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }
        });
        feedbackEmailEt.setOnEditorActionListener((v, actionId, event) -> {
            onOptionsItemSelected(sendMenuItem);
            return true;
        });

        View immediateParentView = findViewById(R.id.feedback_content_parent);
        if (immediateParentView != null) {
            immediateParentView.setOnClickListener(view -> {
                // Show keyboard when user clicks on
                // Large white area on the screen beside the screenshot
                focusOnFeedbackEditText();
            });
        }

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setOnClickListener(view -> {
                // Show keyboard when user clicks on
                // Large white area on the screen below the screenshot
                focusOnFeedbackEditText();
            });
        }

        if (null == savedInstanceState) {
            focusOnFeedbackEditText();
        }

        deviceInfo = new DeviceInfo(getApplicationContext());

        FeedbackModule.registerSendFeedbackListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FeedbackModule.unregisterSendFeedbackListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feedback, menu);
        sendMenuItem = menu.findItem(R.id.action_send);
        sendMenuItem.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_send) {
            String msg = feedbackEt.getText().toString().trim();
            if (TextUtils.isEmpty(msg)) {
                Toast.makeText(this, R.string.mk_fb_feedback_empty, Toast.LENGTH_SHORT).show();
                return true;
            }

            String email = feedbackEmailEt.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, R.string.mk_fb_invalid_email_address, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (!deviceInfo.hasConnectivity()) {
                Toast.makeText(this, R.string.mk_fb_device_offline, Toast.LENGTH_SHORT).show();
                return true;
            }

            sendMenuItem = item;
            SystemUtils.hideSoftKeyboard(feedbackEt);
            SystemUtils.hideSoftKeyboard(feedbackEmailEt);
            sendFeedback(msg, email);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSendButtonState() {
        String feedback = feedbackEt.getText().toString();
        String email = feedbackEmailEt.getText().toString();
        if (!TextUtils.isEmpty(feedback) && Patterns.EMAIL_ADDRESS.matcher(email).matches() && sendMenuItem != null) {
            sendMenuItem.setEnabled(true);
        } else if (sendMenuItem != null) {
            sendMenuItem.setEnabled(false);
        }
    }

    public void showProgressDialog() {
        final ProgressDialogFragment progressDialog = new ProgressDialogFragment();
        progressDialog.show(getSupportFragmentManager(), ProgressDialogFragment.class.getSimpleName());
    }

    public void dismissProgressDialog() {
        Fragment dialogFragment = getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.class.getSimpleName());
        if (dialogFragment != null
                && dialogFragment instanceof ProgressDialogFragment) {
            ((ProgressDialogFragment) dialogFragment).dismiss();
        }
    }

    @Override
    public void doFinish() {
        finish();
    }

    private void focusOnFeedbackEditText() {
        feedbackEt.requestFocus();
        showKeyboard();
    }

    private void sendFeedback(final String msg, final String email) {
        FeedbackModule.sendFeedback(msg, email);
        showProgressDialog();
    }

    @Override
    public void onSendCompleted(Result result) {
        if (Result.SUCCESS == result) {

            // Don't allow any more changes
            if (sendMenuItem != null) {
                sendMenuItem.setEnabled(false);
            }
            feedbackEt.setEnabled(false);
        }

        boolean isPaused = !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);

        if (isPaused) {
            finish(); // Cannot show any notification to user. So just finish.
        }
    }

    private void showKeyboard() {
        feedbackEt.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(feedbackEt, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 300);
    }

    private abstract class TextWatcherAdapter implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // unused
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // unused
        }

        @Override
        public void afterTextChanged(Editable s) {
            // unused
        }
    }
}
