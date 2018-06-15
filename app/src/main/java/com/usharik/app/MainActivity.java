package com.usharik.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.usharik.app.dao.DatabaseManager;
import com.usharik.app.framework.ViewActivity;
import com.usharik.app.databinding.ActivityMainBinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.subjects.PublishSubject;

public class MainActivity extends ViewActivity<MainViewModel> {

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

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private PublishSubject<Boolean> permissionRequestSubject;

    private ActivityMainBinding binding;

    @Inject
    DatabaseManager databaseManager;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        getViewModel().setText(getResources().getString(R.string.title_home));
                        return true;
                    case R.id.navigation_dashboard:
                        getViewModel().setText(getResources().getString(R.string.title_dashboard));
                        return true;
                    case R.id.navigation_notifications:
                        getViewModel().setText(getResources().getString(R.string.title_notifications));
                        return true;
                }
                return false;
            };

    @Override
    protected void onResume() {
        super.onResume();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setViewModel(getViewModel());
        binding.flow.setOnDragListener(this::onFlowDrag);
        binding.button.setOnClickListener(this::onButtonClick);

        //checkPermissionAndExecute(databaseManager::restore);

        loadNewWord();
    }

    private void loadNewWord() {
        getViewModel().getWordDeclension();

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

    private void onButtonClick(View view) {
        getViewModel().checkAnswers();
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
        if (event.getAction()==MotionEvent.ACTION_DOWN)
        {
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
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
                if (getViewModel().getCaseModels()[numberCode][caseNum] == -1) {
                    dropped.setOnTouchListener(null);
                }
                getViewModel().swapCaseModels(caseNum, numberCode, caseNum1, numberCode1);
                dropTarget.setOnTouchListener(this::onTouch);
            }
        }
        return true;
    }

    private void checkPermissionAndExecute(io.reactivex.functions.Action action) {
        if (isExternalStoragePermitted()) {
            try {
                action.run();
                return;
            } catch (Exception ex) {
                onError(ex);
                return;
            }
        }
        permissionRequestSubject = PublishSubject.create();
        permissionRequestSubject.subscribe(
                allowed -> {
                    if (allowed) {
                        action.run();
                    }
                },
                this::onError);
        requestStoragePermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            for (int permission : grants) {
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    permissionRequestSubject.onNext(false);
                    permissionRequestSubject.onComplete();
                    return;
                }
            }
            permissionRequestSubject.onNext(true);
            permissionRequestSubject.onComplete();
        }
    }

    private boolean isExternalStoragePermitted() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        );
    }

    public void onError(Throwable thr) {
        Log.e(getClass().getName(), thr.getLocalizedMessage(), thr);
    }

    @Override
    protected Class<MainViewModel> getViewModelClass() {
        return MainViewModel.class;
    }
}
