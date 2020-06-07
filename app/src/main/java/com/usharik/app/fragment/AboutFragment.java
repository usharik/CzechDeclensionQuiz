package com.usharik.app.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;

import java.text.DateFormat;
import java.util.Date;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvVersion = getView().findViewById(R.id.appVersion);
        TextView tvDateOfBuild = getView().findViewById(R.id.appDateOfBuild);
        tvVersion.setText(getResources().getString(R.string.version,
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE));
        String buildDateStr = DateFormat.getInstance().format(new Date(BuildConfig.TIMESTAMP));
        tvDateOfBuild.setText(getResources().getString(R.string.date_of_build, buildDateStr));
        getView().findViewById(R.id.rateApp).setOnClickListener(this::onRateAppClick);
        getView().findViewById(R.id.privacyPolicy).setOnClickListener(this::onPrivacyPolicyClick);
        getView().findViewById(R.id.donate).setOnClickListener(this::onDonateClick);
    }

    private void onDonateClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.me/usharik")));
    }

    public void onRateAppClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + this.getActivity().getPackageName())));
    }

    private void onPrivacyPolicyClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/usharik/CzechDeclensionQuiz/blob/master/privacy_policy.md")));
    }
}
