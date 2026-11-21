/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static com.marv42.ebt.newnote.MyOnApplyWindowInsetsListener.getOnApplyWindowInsetsListener;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.scanning.Corrections.LENGTH_THRESHOLD_SERIAL_NUMBER;
import static com.marv42.ebt.newnote.scanning.TextProcessor.NEW_LINE;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.exceptions.NoClipboardManagerException;
import com.marv42.ebt.newnote.preferences.Color;
import com.marv42.ebt.newnote.preferences.MySharedPreferencesListener;
import com.marv42.ebt.newnote.preferences.SettingsActivity;
import com.marv42.ebt.newnote.preferences.SharedPreferenceDataStore;
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;
import com.marv42.ebt.newnote.retry.RetryManager;
import com.marv42.ebt.newnote.scanning.OcrHandlerTask;
import com.marv42.ebt.newnote.scanning.OcrHandlerLocal;
import com.marv42.ebt.newnote.scanning.OcrHandlerOnline;
import com.marv42.ebt.newnote.scanning.OcrNotifier;
import com.marv42.ebt.newnote.scanning.OcrResult;
import com.marv42.ebt.newnote.scanning.OnlineOcr;
import com.marv42.ebt.newnote.ui.ResultsViewModel;
import com.marv42.ebt.newnote.ui.SubmitViewModel;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

public class EbtNewNote extends DaggerAppCompatActivity
        implements SubmitFragment.Callback, ResultsFragmentData.Callback, CommentSuggestionTask.Callback,
        OcrHandlerTask.Callback, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String FRAGMENT_TYPE = "fragment_type";
    public static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 3;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 4;
    public static final int NOTIFICATIONS_PERMISSION_REQUEST_CODE = 5;
    static final int SUBMIT_FRAGMENT_INDEX = 0;
    private static final int RESULTS_FRAGMENT_INDEX = 1;
    private static final int DATA_RESULTS_FRAGMENT_INDEX = 2;
    private static final int VIBRATION_MS = 150;
    private static final CharSequence CLIPBOARD_LABEL = "overwritten EBT data";
    private static final String TAG = EbtNewNote.class.getSimpleName();
    @Inject
    SharedPreferenceDataStore sharedPreferenceDataStore;
    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    MySharedPreferencesListener sharedPreferencesListener;
    @Inject
    ViewModelProvider viewModelProvider;
    @Inject
    AllResults allResults;
    @Inject
    RetryManager retryManager;
    @Inject
    OnlineOcr onlineOcr;
    private SubmitFragment submitFragment = null;
    private ResultsFragment resultsFragment = null;
    private boolean isResultsEmpty = true;
    private String[] commentSuggestionsUntilFragmentAdded;
    private String ocrResult = "";
    private List<String> remainingOcrResults;
    private String appliedColor;
    private OcrHandlerTask ocrHandlerTask;
    private final RetryManager.Listener retryListener = this::onRetryFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout();
        setupFragmentsInitially();
        setupResultsObserver();
        requestNotificationsPermissions();
        sharedPreferencesListener.register();
        retryManager.init();
        retryManager.addListener(retryListener);
        deliverCompletedOcr();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String color = getColor();
        if (!color.equals(appliedColor)) {
            appliedColor = color;
            recreate();
        }
    }

    @NonNull
    private String getColor() {
        return sharedPreferenceDataStore.get(R.string.pref_settings_color_key, Color.DEFAULT.getKey());
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // do not call super.onSaveInstanceState, or we will end up with multiple fragments (https://stackoverflow.com/a/14578709/542235)
    }

    private void requestNotificationsPermissions() {
        if (PermissionChecker.checkSelfPermission(this, NOTIFICATION_SERVICE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, NOTIFICATIONS_PERMISSION_REQUEST_CODE);
        }
    }

    private void setLayout() {
        applyStyle();
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.main);
        View view = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(view, getOnApplyWindowInsetsListener());
    }

    private void applyStyle() {
        appliedColor = getColor();
        Color.fromKey(appliedColor).applyTo(this);
    }

    private boolean isDualPane() {
        return getViewPager() == null;
    }

    private ViewPager2 getViewPager() {
        return findViewById(R.id.view_pager);
    }

    private void setupFragmentsInitially() {
        createFragments();
        if (isDualPane())
            setupFragmentsInitiallyDualPane();
        else
            setupFragmentsInitiallyNoDualPane();
    }

    private void createFragments() {
        if (submitFragment == null)
            submitFragment = new SubmitFragment();
        if (resultsFragment == null)
            resultsFragment = new ResultsFragmentEmpty();
    }

    private void setupFragmentsInitiallyDualPane() {
        addSubmitFragment();
        addResultsFragment();
    }

    private String getFragmentTag(int fragmentIndex) {
        return "f" + fragmentIndex;
    }

    private void addSubmitFragment() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.submit_fragment_container_view);
        if (fragment != null)
            return;
        final String tag = getFragmentTag(SUBMIT_FRAGMENT_INDEX);
        addFragment(R.id.submit_fragment_container_view, SubmitFragment.class, tag);
    }

    private void addResultsFragment() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.results_fragment_container_view);
        if (fragment != null)
            return;
        final String tag = getFragmentTag(RESULTS_FRAGMENT_INDEX);
        Class<? extends ResultsFragment> resultsFragmentClass;
        if (isResultsEmpty)
            resultsFragmentClass = ResultsFragmentEmpty.class;
        else
            resultsFragmentClass = ResultsFragmentData.class;
        addFragment(R.id.results_fragment_container_view, resultsFragmentClass, tag);
    }

    private void addFragment(int containerViewId, Class<? extends Fragment> fragmentClass, String tag) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(containerViewId, fragmentClass, null, tag)
                .commit();
    }

    private void setupFragmentsInitiallyNoDualPane() {
        setupViewPager();
        setupTabLayout();
    }

    private void setupViewPager() {
        MyFragmentStateAdapter adapter = new MyFragmentStateAdapter();
        ViewPager2 viewPager = getViewPager();
        if (viewPager == null)
            return;
        viewPager.setAdapter(adapter);
    }

    private void setupTabLayout() {
        new TabLayoutMediator(getTabLayout(), getViewPager(), (tab, position) -> {
            switch (position) {
                case SUBMIT_FRAGMENT_INDEX -> tab.setText(getString(R.string.submit_fragment_title));
                case RESULTS_FRAGMENT_INDEX -> tab.setText(getString(R.string.results_fragment_title));
                default -> throw new IllegalArgumentException("position");
            }
        }).attach();
    }

    private TabLayout getTabLayout() {
        return findViewById(R.id.tab_layout);
    }

    private void setupResultsObserver() {
        ResultsViewModel viewModel = viewModelProvider.get(ResultsViewModel.class);
        viewModel.getResults().observe(this, resultsObserver -> {
            if (! resultsObserver.isEmpty()) {
                replaceEmptyResultsFragment();
                isResultsEmpty = false;
            }
        });
    }

    private void replaceEmptyResultsFragment() {
        if (resultsFragment instanceof ResultsFragmentData)
            return;
        if (isDualPane())
            getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.results_fragment_container_view, ResultsFragmentData.class, null)
                .commit();
        else
            resultsFragment = new ResultsFragmentData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (isDualPane())
            return;
        if (shouldSwitchToResults(intent))
            switchFragment(RESULTS_FRAGMENT_INDEX);
        else if (shouldSwitchToSubmit(intent))
            switchFragment(SUBMIT_FRAGMENT_INDEX);
    }

    private boolean shouldSwitchToResults(Intent intent) {
        final String extra = intent.getStringExtra(FRAGMENT_TYPE);
        return extra != null && extra.equals(ResultsFragment.class.getSimpleName());
    }

    private boolean shouldSwitchToSubmit(Intent intent) {
        final String extra = intent.getStringExtra(FRAGMENT_TYPE);
        return extra != null && extra.equals(SubmitFragment.class.getSimpleName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void inflateMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.findItem(R.id.settings).setIntent(new Intent(this, SettingsActivity.class));
        menu.findItem(R.id.licenses).setIntent(new Intent(this, LicensesActivity.class));
        menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE)
            if (resultCode == RESULT_OK)
                processPhoto();
    }

    private void processPhoto() {
        Toast.makeText(this, R.string.processing, LENGTH_LONG).show();
        retryManager.cancelPendingOcr();
        cancelOcr();
        String photoPath = sharedPreferencesHandler.get(R.string.pref_photo_path_key, "");
        Uri photoUri = Uri.parse(sharedPreferencesHandler.get(R.string.pref_photo_uri_key, ""));
        if (sharedPreferenceDataStore.get(R.string.pref_settings_ocr_online_key, false))
            ocrHandlerTask = new OcrHandlerOnline(this, photoPath, photoUri, getContentResolver(), onlineOcr);
        else
            ocrHandlerTask = new OcrHandlerLocal(this, this, photoPath, photoUri, getContentResolver());
        ocrHandlerTask.execute();
    }

    private void cancelOcr() {
        if (ocrHandlerTask != null) {
            ocrHandlerTask.cancel();
            ocrHandlerTask = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.no_permission, LENGTH_LONG).show();
            return;
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, R.string.permission_location, LENGTH_LONG).show();
            submitFragment.locationButtonClicked();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, R.string.permission_camera, LENGTH_LONG).show();
            submitFragment.takePhoto();
        }
    }

    @Override
    public void onSubmitFragmentAdded() {
        if (commentSuggestionsUntilFragmentAdded != null && commentSuggestionsUntilFragmentAdded.length > 0) {
            submitFragment.setCommentsAdapter(commentSuggestionsUntilFragmentAdded);
            commentSuggestionsUntilFragmentAdded = null;
        }
    }

    @Override
    public void onSubmitButtonClicked() {
        if (remainingOcrResults == null || remainingOcrResults.isEmpty())
            return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.ocr_multiple_results)
                .setMessage(R.string.remaining_results)
                .setPositiveButton(getString(android.R.string.ok),
                        (dialog, which) ->
                                letUserChoose(remainingOcrResults.toArray(new String[0])))
                .create()
                .show();
    }

    @Override
    public void switchFragment(int fragmentIndex) {
        if (isDualPane())
            return;
        ViewPager2 viewPager = getViewPager();
        viewPager.setCurrentItem(fragmentIndex);
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        if (submitFragment != null && submitFragment.isAdded())
            submitFragment.setCommentsAdapter(suggestions);
        else
            commentSuggestionsUntilFragmentAdded = suggestions;
    }

    @Override
    protected void onDestroy() {
        retryManager.removeListener(retryListener);
        cancelOcr();
        sharedPreferencesListener.unregister();
        super.onDestroy();
    }

    private void onRetryFinished() {
        allResults.mergeCompletedResults();
        deliverCompletedOcr();
    }

    private void deliverCompletedOcr() {
        for (String text : retryManager.drainCompletedOcr())
            onOcrResult(new OcrResult(text));
    }

    private boolean isOnlineOcr() {
        return sharedPreferenceDataStore.get(R.string.pref_settings_ocr_online_key, false);
    }

    private void scheduleOcr() {
        String photoPath = sharedPreferencesHandler.get(R.string.pref_photo_path_key, "");
        String photoUri = sharedPreferencesHandler.get(R.string.pref_photo_uri_key, "");
        retryManager.scheduleOcr(photoPath, photoUri);
    }

    @Override
    public void onOcrResult(@NonNull OcrResult result) {
        ocrHandlerTask = null;
        final String text = result.text;
        if (text.isEmpty()) {
            OcrNotifier.showDialog(this, getString(R.string.ocr_dialog_empty));
            return;
        }
        if (text.startsWith(ERROR)) {
            new ErrorMessage(this).showErrorToasts(text);
            if (result.retryableConnectionFailure && isOnlineOcr()) {
                scheduleOcr();
                Toast.makeText(this, R.string.sending_later, Toast.LENGTH_SHORT).show();
                return;
            }
            final String errorMessage = new ErrorMessage(this).getErrorMessage(text);
            OcrNotifier.showDialog(this, errorMessage);
            return;
        }
        checkMultipleOcrResults(text);
    }

    private void checkMultipleOcrResults(@NonNull String result) {
        if (result.contains(NEW_LINE)) {
            String[] allResults = result.split(NEW_LINE);
            letUserChoose(allResults);
            return;
        }
        ocrResult = result;
        vibrate();
        replaceShortCodeOrSerialNumber();
        Toast.makeText(this, R.string.ocr_return, LENGTH_LONG).show();
    }

    private void letUserChoose(String[] ocrResults) {
        ocrResult = "";
        new AlertDialog.Builder(this)
                .setTitle(R.string.ocr_multiple_results)
//                .setMessage(R.string.ocr_multiple_results)  // https://developer.android.com/develop/ui/views/components/dialogs#AddAList
                .setItems(ocrResults, (dialog, item) -> {
                    setOcrResults(ocrResults, item);
                    replaceShortCodeOrSerialNumber();
                })
                .setCancelable(false)
                .setNegativeButton(getString(android.R.string.cancel), null)
                .create()
                .show();
    }

    private void setOcrResults(String[] ocrResults, int item) {
        ocrResult = ocrResults[item];
        remainingOcrResults = new LinkedList<>(Arrays.asList(ocrResults));
        remainingOcrResults.remove(ocrResult);
    }

    private void replaceShortCodeOrSerialNumber() {
        checkClipboardManager();
        setSerialNumberOrShortCode(ocrResult);
    }

    private boolean isSerialNumberNotShortCode(String ocrResult) {
        return ocrResult.length() >= LENGTH_THRESHOLD_SERIAL_NUMBER;
    }

    private void setSerialNumberOrShortCode(String ocrResult) {
        final boolean serialNumberNotShortCode = isSerialNumberNotShortCode(ocrResult);
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        if (serialNumberNotShortCode)
            viewModel.setSerialNumber(ocrResult);
        else
            viewModel.setShortCode(ocrResult);
    }

    void checkClipboardManager() {
        try {
            putToClipboard();
        } catch (NoClipboardManagerException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    private void putToClipboard() throws NoClipboardManagerException {
        ClipboardManager manager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (manager == null)
            throw new NoClipboardManagerException();
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        final boolean serialNumberNotShortCode = isSerialNumberNotShortCode(ocrResult);
        String text = viewModel.getSerialNumber().getValue();
        if (!serialNumberNotShortCode)
            text = viewModel.getShortCode().getValue();
        if (text != null && !text.isEmpty()) {
            ClipData data = ClipData.newPlainText(CLIPBOARD_LABEL, text);
            manager.setPrimaryClip(data);
            Toast.makeText(this, R.string.content_in_clipboard, LENGTH_LONG).show();
        }
    }

    private void vibrate() {
        VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
        Vibrator v = manager.getDefaultVibrator();
        v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, DEFAULT_AMPLITUDE));
    }

    private class MyFragmentStateAdapter extends FragmentStateAdapter {
        private static final int NUM_TABS = 2;

        MyFragmentStateAdapter() {
            super(getSupportFragmentManager(), EbtNewNote.this.getLifecycle());
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }

        @Override
        public long getItemId(int position) {
            if (position == RESULTS_FRAGMENT_INDEX && ! isResultsEmpty)
                position = DATA_RESULTS_FRAGMENT_INDEX; // This triggers a createFragment call for a ResultsFragmentData
            return position;
        }

        @Override
        public boolean containsItem(long itemId) {
            return switch ((int) itemId) {
                case SUBMIT_FRAGMENT_INDEX -> submitFragment != null;
                case RESULTS_FRAGMENT_INDEX -> resultsFragment != null;
                case DATA_RESULTS_FRAGMENT_INDEX -> ! isResultsEmpty;
                default -> throw new IllegalArgumentException("itemId");
            };
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case SUBMIT_FRAGMENT_INDEX -> { return submitFragment; }
                case RESULTS_FRAGMENT_INDEX -> { return resultsFragment; }
                default -> throw new IllegalArgumentException("position");
            }
        }
    }
}
