package com.usharik.app.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.usharik.app.BuildConfig;
import com.usharik.app.R;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textView = getView().findViewById(R.id.appVersion);
        textView.setText(getResources().getString(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE));
        getView().findViewById(R.id.rateApp).setOnClickListener(this::onRateAppClick);
    }

    public void onRateAppClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + this.getActivity().getPackageName())));
    }
}
