package com.usharik.app.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usharik.app.AppState;
import com.usharik.app.R;
import com.usharik.app.databinding.DeclensionQuizFragmentBinding;
import com.usharik.app.framework.ViewFragment;
import com.usharik.app.widget.CustomDragShadowBuilder;

import javax.inject.Inject;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.usharik.app.fragment.SettingsFragment.SHARED_PREFERENCES;

public class DeclensionQuizFragment extends ViewFragment<DeclensionQuizViewModel> {

    public static final String WORDS_WITH_ERRORS = "WORDS_WITH_ERRORS";

    private static final Set<Integer> wordEditViewSet = buildWordEditViewSet();

    private static Set<Integer> buildWordEditViewSet() {
        HashSet<Integer> res = new HashSet<>();
        res.add(R.id.word1);
        res.add(R.id.word2);
        res.add(R.id.word3);
        res.add(R.id.word4);
        res.add(R.id.word5);
        res.add(R.id.word6);
        res.add(R.id.word7);
        res.add(R.id.word8);
        res.add(R.id.word9);
        res.add(R.id.word10);
        res.add(R.id.word11);
        res.add(R.id.word12);
        res.add(R.id.word13);
        res.add(R.id.word14);
        return Collections.unmodifiableSet(res);
    }

    private DeclensionQuizFragmentBinding binding;

    @Inject
    AppState appState;

    @Inject
    FirebaseAnalytics firebaseAnalytics;

    @Inject
    Gson gson;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.declension_quiz_fragment, container, false);
        binding.setViewModel(getViewModel());
        binding.flow.setOnDragListener(this::onFlowDrag);
        if (appState.wordInfo == null) {
            getViewModel().nextWord(false);
        }
        setListeners();
        setHasOptionsMenu(true);
        return binding.getRoot();
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

        binding.word1.setOnTouchListener(this::onTouch);
        binding.word2.setOnTouchListener(this::onTouch);
        binding.word3.setOnTouchListener(this::onTouch);
        binding.word4.setOnTouchListener(this::onTouch);
        binding.word5.setOnTouchListener(this::onTouch);
        binding.word6.setOnTouchListener(this::onTouch);
        binding.word7.setOnTouchListener(this::onTouch);
        binding.word8.setOnTouchListener(this::onTouch);
        binding.word9.setOnTouchListener(this::onTouch);
        binding.word10.setOnTouchListener(this::onTouch);
        binding.word11.setOnTouchListener(this::onTouch);
        binding.word12.setOnTouchListener(this::onTouch);
        binding.word13.setOnTouchListener(this::onTouch);
        binding.word14.setOnTouchListener(this::onTouch);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_check:
                if (getViewModel().checkAnswers()) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.correct_answer)
                            .setItems(R.array.next_word_dialog, this::nextWordDialogHandler)
                            .setCancelable(false)
                            .show();
                } else {
                    Toast.makeText(getActivity(), R.string.toast_some_errors, Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_next:
                nextWord(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void nextWordDialogHandler(DialogInterface dialogInterface, int i) {
        saveErrorsInfo();
        switch (i) {
            case 0:
                nextWord(false);
                logAction("NEXT");
                return;
            case 1:
                logAction("STAY");
                return;
            case 2:
                nextWord(true);
                logAction("TRY_AGAIN");
                return;
            case 3:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + this.getActivity().getPackageName())));
        }
    }

    private void nextWord(boolean tryAgain) {
        getViewModel().nextWord(tryAgain);
        setListeners();
    }

    private void logAction(String actionName) {
        Bundle bundle = new Bundle();
        bundle.putString("NEXT_WORD_ACTION", actionName);
        firebaseAnalytics.logEvent("NEXT_WORD_ACTION", bundle);
    }

    private void saveErrorsInfo() {
        int errorCount = getViewModel().getErrorCount();
        if (errorCount == 0) {
            appState.removeWordFromErrorMap();
        }
        if (errorCount > 2) {
            appState.putWordToErrorMap(errorCount);
        }
    }

    private boolean onFlowDrag(View v, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            TextView dropped = (TextView) event.getLocalState();
            if (wordEditViewSet.contains(dropped.getId())) {
                return true;
            }
            String[] info = ((String) dropped.getTag()).split("_");
            int numberCode = Integer.valueOf(info[0]);
            int caseNum = Integer.valueOf(info[1]);
            int droppedWordNum = getViewModel().getCaseModels()[numberCode][caseNum];
            getViewModel().updateCaseModel(caseNum, numberCode, -1);
            getViewModel().updateWordTextModel(droppedWordNum, View.VISIBLE);
            dropped.setOnTouchListener(null);
        }
        return true;
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!wordEditViewSet.contains(v.getId())) {
                TextView tv = (TextView) v;
                String[] info1 = ((String) tv.getTag()).split("_");
                int numberCode = Integer.valueOf(info1[0]);
                int caseNum = Integer.valueOf(info1[1]);
                if (getViewModel().getCaseModels()[numberCode][caseNum] == -1) {
                    return false;
                }
            }
            CustomDragShadowBuilder shadowBuilder = new CustomDragShadowBuilder(v, 2f);
            v.startDrag(null, shadowBuilder, v, 0);
            return true;
        }
        return false;
    }

    public boolean onDrag(View v, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            TextView dropped = (TextView) event.getLocalState();
            TextView dropTarget = (TextView) v;
            String[] info = ((String) dropTarget.getTag()).split("_");
            int numberCode = Integer.valueOf(info[0]);
            int caseNum = Integer.valueOf(info[1]);
            if (wordEditViewSet.contains(dropped.getId())) {
                int droppedWordNum = Integer.parseInt(dropped.getTag().toString());
                getViewModel().updateCaseModel(caseNum, numberCode, droppedWordNum);
                getViewModel().updateWordTextModel(droppedWordNum, View.GONE);
                dropTarget.setOnTouchListener(this::onTouch);
            } else {
                String[] info1 = ((String) dropped.getTag()).split("_");
                int numberCode1 = Integer.valueOf(info1[0]);
                int caseNum1 = Integer.valueOf(info1[1]);
                getViewModel().swapCaseModels(caseNum, numberCode, caseNum1, numberCode1);
                dropTarget.setOnTouchListener(this::onTouch);
            }
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit();
        Type type = new TypeToken<HashMap<String, Integer>>() {}.getType();
        editor.putString(WORDS_WITH_ERRORS, gson.toJson(appState.wordsWithErrors, type));
        editor.apply();
    }

    @Override
    protected Class<DeclensionQuizViewModel> getViewModelClass() {
        return DeclensionQuizViewModel.class;
    }
}
