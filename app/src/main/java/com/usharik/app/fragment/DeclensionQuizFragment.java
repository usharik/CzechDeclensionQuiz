package com.usharik.app.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.lifecycle.Lifecycle;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.adapter.WordDragAdapter;
import com.usharik.app.ads.AdManager;
import com.usharik.app.databinding.DeclensionQuizFragmentBinding;
import com.usharik.app.DeclensionQuizState.WordTextModel;
import com.usharik.app.framework.ViewFragment;
import com.usharik.app.service.FirebaseAnalyticsService;
import com.usharik.app.utils.HapticFeedback;
import com.usharik.app.widget.CustomDragShadowBuilder;

import javax.inject.Inject;

import java.lang.reflect.Type;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static com.usharik.app.fragment.SettingsFragment.SHARED_PREFERENCES;

public class DeclensionQuizFragment extends ViewFragment<DeclensionQuizViewModel> {

    public static final String WORDS_WITH_ERRORS = "WORDS_WITH_ERRORS";
    private static final int WRONG_ATTEMPTS_BEFORE_AD = 5;

    private AdView adView;
    private WordDragAdapter wordDragAdapter;
    private Observable.OnPropertyChangedCallback wordModelCallback;

    private DeclensionQuizFragmentBinding binding;

    @Inject AppState appState;
    @Inject FirebaseAnalyticsService analyticsService;
    @Inject Gson gson;
    @Inject AdManager adManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.declension_quiz_fragment, container, false);
        binding.setViewModel(getViewModel());
        getViewModel().nextWord(false);
        setupWordDragRecyclerView();
        setListeners();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenu();
        adManager.loadAd(getActivity());
        setupBannerAd();

        // Keep the RecyclerView in sync whenever the ViewModel updates wordTextModels.
        wordModelCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == BR.wordTextModels) {
                    WordTextModel[] items = getViewModel().getWordTextModels();
                    if (items != null && wordDragAdapter != null) {
                        wordDragAdapter.updateItems(items);
                    }
                }
            }
        };
        getViewModel().addOnPropertyChangedCallback(wordModelCallback);
    }

    @Override
    public void onDestroyView() {
        if (wordModelCallback != null) {
            getViewModel().removeOnPropertyChangedCallback(wordModelCallback);
            wordModelCallback = null;
        }
        super.onDestroyView();
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    private void setupWordDragRecyclerView() {
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(requireContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);

        wordDragAdapter = new WordDragAdapter(this::onTouch);

        binding.wordsRecyclerView.setLayoutManager(layoutManager);
        binding.wordsRecyclerView.setHasFixedSize(false);
        binding.wordsRecyclerView.setAdapter(wordDragAdapter);
        // Returning a word from a case cell by dropping onto the pool area.
        binding.wordsRecyclerView.setOnDragListener(this::onRecyclerViewDrag);

        WordTextModel[] existing = getViewModel().getWordTextModels();
        if (existing != null) {
            wordDragAdapter.updateItems(existing);
        }
    }

    private void setupBannerAd() {
        adView = new AdView(requireContext());
        adView.setAdUnitId(BuildConfig.ADMOB_BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);
        binding.adViewContainer.removeAllViews();
        binding.adViewContainer.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                return handleMenuItemSelected(item);
            }
        }, getViewLifecycleOwner(), Lifecycle.State.CREATED);
        requireActivity().invalidateMenu();
    }

    private void setListeners() {
        getViewModel().update();

        binding.case1.caseSingular.setOnDragListener(this::onDrag);
        binding.case1.casePlural.setOnDragListener(this::onDrag);
        binding.case2.caseSingular.setOnDragListener(this::onDrag);
        binding.case2.casePlural.setOnDragListener(this::onDrag);
        binding.case3.caseSingular.setOnDragListener(this::onDrag);
        binding.case3.casePlural.setOnDragListener(this::onDrag);
        binding.case4.caseSingular.setOnDragListener(this::onDrag);
        binding.case4.casePlural.setOnDragListener(this::onDrag);
        binding.case5.caseSingular.setOnDragListener(this::onDrag);
        binding.case5.casePlural.setOnDragListener(this::onDrag);
        binding.case6.caseSingular.setOnDragListener(this::onDrag);
        binding.case6.casePlural.setOnDragListener(this::onDrag);
        binding.case7.caseSingular.setOnDragListener(this::onDrag);
        binding.case7.casePlural.setOnDragListener(this::onDrag);

        // Add observer for error counter changes to animate it
        getViewModel().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == com.usharik.app.BR.wrongAttemptsCounter) {
                    animateErrorCounter();
                }
            }
        });
    }

    // ─── Menu ────────────────────────────────────────────────────────────────

    private boolean handleMenuItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_next) {
            checkAndShowAdThenNextWord(false);
            return true;
        }
        return false;
    }

    private void showCorrectAnswerDialog() {
        android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_correct_answer, null, false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        saveErrorsInfo();

        dialogView.findViewById(R.id.btnNextWord).setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            dialog.dismiss();
            checkAndShowAdThenNextWord(false);
            analyticsService.logNextWordAction("NEXT");
        });
        dialogView.findViewById(R.id.btnStayHere).setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            dialog.dismiss();
            analyticsService.logNextWordAction("STAY");
        });
        dialogView.findViewById(R.id.btnTryAgain).setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            dialog.dismiss();
            nextWord(true);
            analyticsService.logNextWordAction("TRY_AGAIN");
        });
        dialogView.findViewById(R.id.btnRateApp).setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            dialog.dismiss();
            String packageName = requireActivity().getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + packageName)));
            } catch (android.content.ActivityNotFoundException e) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(requireContext(), R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    // ─── Word flow / navigation ───────────────────────────────────────────────

    private void checkAndShowAdThenNextWord(boolean tryAgain) {
        appState.incrementWordsCountSinceLastAd();
        if (appState.getWordsCountSinceLastAd() >= 10) {
            appState.resetWordsCountSinceLastAd();
            showAdThenNextWord(tryAgain);
        } else {
            nextWord(tryAgain);
        }
    }

    private void showAdThenNextWord(boolean tryAgain) {
        adManager.showAd(getActivity(), () -> nextWord(tryAgain));
    }

    private void nextWord(boolean tryAgain) {
        getViewModel().nextWord(tryAgain);
        setListeners();
    }

    private void saveErrorsInfo() {
        int errorCount = getViewModel().getErrorCount();
        String currentWord = getViewModel().getWord();
        if (errorCount == 0) appState.removeWordFromErrorMap(currentWord);
        if (errorCount > 2)  appState.putWordToErrorMap(currentWord, errorCount);
    }

    // ─── Drag-and-drop helpers ────────────────────────────────────────────────

    /**
     * Returns true when {@code v} is a word-pool item (tag contains no underscore),
     * false when it is a case cell (tag is "numberCode_caseNum").
     */
    private boolean isWordPoolItem(View v) {
        Object tag = v.getTag();
        return tag instanceof String && !((String) tag).contains("_");
    }

    // ─── Drag-and-drop handlers ───────────────────────────────────────────────

    /**
     * Touch handler shared by word-pool items (via WordDragAdapter) and case cells
     * (set by onDrag after a word is placed).
     *
     * Word-pool items always start a drag.
     * Case cells only start a drag when they contain a word (index != -1).
     */
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isWordPoolItem(v)) {
                // Case-cell path: only draggable when occupied.
                String[] info = ((String) v.getTag()).split("_");
                int numberCode = Integer.parseInt(info[0]);
                int caseNum    = Integer.parseInt(info[1]);
                if (getViewModel().getCaseModels()[numberCode][caseNum] == -1) {
                    return false;
                }
            }
            // Light haptic feedback when picking up a word
            HapticFeedback.medium(v.getContext());
            v.startDragAndDrop(null, new CustomDragShadowBuilder(v, 2f), v, 0);
            return true;
        }
        return false;
    }

    /**
     * Drag listener on the RecyclerView itself.
     * Handles the "return a word to the pool" case when the user drops a case-cell
     * word onto the word-pool area. Drops originating from word-pool items are ignored.
     */
    private boolean onRecyclerViewDrag(View v, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            View dropped = (View) event.getLocalState();
            if (isWordPoolItem(dropped)) {
                return true; // already in pool — no-op
            }
            String[] info = ((String) dropped.getTag()).split("_");
            int numberCode   = Integer.parseInt(info[0]);
            int caseNum      = Integer.parseInt(info[1]);
            int droppedWordNum = getViewModel().getCaseModels()[numberCode][caseNum];
            getViewModel().updateCaseModel(caseNum, numberCode, -1);
            getViewModel().updateWordTextModel(droppedWordNum, View.VISIBLE);
            dropped.setOnTouchListener(null);
        }
        return true;
    }

    /**
     * Drag listener on each case cell (caseSingular / casePlural in every row_case).
     * Handles:
     *   - word-pool item → case cell  (place word)
     *   - case cell      → case cell  (swap words)
     * Now with instant validation, animations, and vibration feedback
     */
    public boolean onDrag(View v, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            View dropped     = (View) event.getLocalState();
            TextView dropTarget = (TextView) v;
            String[] info    = ((String) dropTarget.getTag()).split("_");
            int numberCode   = Integer.parseInt(info[0]);
            int caseNum      = Integer.parseInt(info[1]);

            if (isWordPoolItem(dropped)) {
                // Place a word from the pool into this cell.
                int droppedWordNum = Integer.parseInt(dropped.getTag().toString());
                getViewModel().updateCaseModel(caseNum, numberCode, droppedWordNum);
                getViewModel().updateWordTextModel(droppedWordNum, View.GONE);
                dropTarget.setOnTouchListener(this::onTouch);

                // Check if the answer is correct
                boolean isCorrect = getViewModel().checkSingleAnswer(caseNum, numberCode, droppedWordNum);

                if (isCorrect) {
                    // Correct answer: bounce animation + single vibration
                    animateBounce(dropTarget);
                    vibrateSuccess();

                    // Check if quiz is complete
                    if (getViewModel().isQuizComplete()) {
                        // Show success dialog after a short delay to let animation finish
                        dropTarget.postDelayed(this::showCorrectAnswerDialog, 300);
                    }
                } else {
                    // Wrong answer: shake animation + triple vibration + red color
                    animateShake(dropTarget);
                    vibrateError();
                    getViewModel().incrementWrongAttempts();

                    // Return word to pool
                    dropTarget.postDelayed(() -> {
                        getViewModel().updateCaseModel(caseNum, numberCode, -1);
                        getViewModel().updateWordTextModel(droppedWordNum, View.VISIBLE);
                        dropTarget.setOnTouchListener(null);
                    }, 500);

                    // Check if we need to show ad after 5 wrong attempts
                    if (getViewModel().getWrongAttempts() >= WRONG_ATTEMPTS_BEFORE_AD) {
                        getViewModel().resetWrongAttempts();
                        dropTarget.postDelayed(() -> {
                            adManager.showAd(getActivity(), () -> {
                                // Ad closed, continue
                            });
                        }, 600);
                    }
                }
            } else {
                // Swap two case cells.
                String[] info1  = ((String) dropped.getTag()).split("_");
                int numberCode1 = Integer.parseInt(info1[0]);
                int caseNum1    = Integer.parseInt(info1[1]);
                getViewModel().swapCaseModels(caseNum, numberCode, caseNum1, numberCode1);
                dropTarget.setOnTouchListener(this::onTouch);

                // Check both swapped positions
                int wordIx1 = getViewModel().getCaseModels()[numberCode][caseNum];
                int wordIx2 = getViewModel().getCaseModels()[numberCode1][caseNum1];

                boolean isCorrect1 = wordIx1 != -1 && getViewModel().checkSingleAnswer(caseNum, numberCode, wordIx1);
                boolean isCorrect2 = wordIx2 != -1 && getViewModel().checkSingleAnswer(caseNum1, numberCode1, wordIx2);

                if (isCorrect1 && isCorrect2) {
                    // Both correct after swap
                    animateBounce(dropTarget);
                    vibrateSuccess();

                    if (getViewModel().isQuizComplete()) {
                        dropTarget.postDelayed(this::showCorrectAnswerDialog, 300);
                    }
                } else if (!isCorrect1 || !isCorrect2) {
                    // At least one is wrong - swap back
                    animateShake(dropTarget);
                    vibrateError();
                    getViewModel().incrementWrongAttempts();

                    dropTarget.postDelayed(() -> {
                        // Swap back
                        getViewModel().swapCaseModels(caseNum, numberCode, caseNum1, numberCode1);
                    }, 500);

                    if (getViewModel().getWrongAttempts() >= WRONG_ATTEMPTS_BEFORE_AD) {
                        getViewModel().resetWrongAttempts();
                        dropTarget.postDelayed(() -> {
                            adManager.showAd(getActivity(), () -> {});
                        }, 600);
                    }
                }
            }
        }
        return true;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) adView.pause();
        SharedPreferences.Editor editor =
                getActivity().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit();
        Type type = new TypeToken<HashMap<String, Integer>>() {}.getType();
        editor.putString(WORDS_WITH_ERRORS, gson.toJson(appState.getWordsWithErrors(), type));
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
            if (binding != null && binding.adViewContainer != null && adView.getParent() == null) {
                binding.adViewContainer.removeAllViews();
                binding.adViewContainer.addView(adView);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    @Override
    protected Class<DeclensionQuizViewModel> getViewModelClass() {
        return DeclensionQuizViewModel.class;
    }

    // ─── Animation and Vibration ──────────────────────────────────────────────

    /**
     * Bounce animation for correct answer
     * Animates scale from 1.0 -> 1.2 -> 1.0
     */
    private void animateBounce(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.2f, 1.0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    /**
     * Shake animation for wrong answer
     * Animates horizontal translation with 3 cycles
     */
    private void animateShake(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f);
        shake.setDuration(500);
        shake.start();
    }

    /**
     * Single short vibration for correct answer
     */
    private void vibrateSuccess() {
        HapticFeedback.success(requireContext());
    }

    /**
     * Triple vibration for wrong answer
     */
    private void vibrateError() {
        HapticFeedback.error(requireContext());
    }

    /**
     * Animate error counter with a small bounce effect when it changes
     */
    private void animateErrorCounter() {
        if (binding.errorCounter != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.errorCounter, "scaleX", 1.0f, 1.3f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.errorCounter, "scaleY", 1.0f, 1.3f, 1.0f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(250);
            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
            animatorSet.start();
        }
    }
}
