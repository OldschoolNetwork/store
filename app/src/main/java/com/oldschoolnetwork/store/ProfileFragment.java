package com.oldschoolnetwork.store;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

public class ProfileFragment extends Fragment implements ONAccountSessionManager.SessionListener {

    private boolean installationNoticeDismissed;
    private ImageView ivProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileUsername;
    private TextView tvManageAccountText;
    private TextView tvManageAccountDesc;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_profile, container, false);
        view.findViewById(R.id.installationNotice).setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://oldschoolnetwork.online/downloads/docs/full-install/"))); }
            catch (android.content.ActivityNotFoundException ignored) { Toast.makeText(requireContext(), "Open oldschoolnetwork.online/downloads/docs/full-install/", Toast.LENGTH_LONG).show(); }
        });

        view.findViewById(R.id.closeInstallationNotice).setOnClickListener(v->{installationNoticeDismissed=true;view.findViewById(R.id.installationNoticePanel).setVisibility(View.GONE);});
        ((android.widget.CheckBox)view.findViewById(R.id.hideInstallationNotice)).setOnCheckedChangeListener((button,checked)->{
            requireContext().getSharedPreferences("ONStore",0).edit().putBoolean("hide_installation_notice",checked).apply();
        });
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        ImageView btnProfileSettings = view.findViewById(R.id.btnProfileSettings);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileUsername = view.findViewById(R.id.tvProfileUsername);
        tvManageAccountText = view.findViewById(R.id.tvManageAccountText);
        tvManageAccountDesc = view.findViewById(R.id.tvManageAccountDesc);

        View btnWallet = view.findViewById(R.id.btnWallet);
        if (btnWallet != null) btnWallet.setVisibility(View.GONE);

        View btnGameHome = view.findViewById(R.id.btnGameHome);
        if (btnGameHome != null) btnGameHome.setVisibility(View.GONE);

        if (btnProfileSettings != null) {
            btnProfileSettings.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SettingsActivity.class)));
        }

        View btnManageAccount = view.findViewById(R.id.btnManageAccount);
        if (btnManageAccount != null) {
            btnManageAccount.setOnClickListener(v -> {
                ONAccountSessionManager session = ONAccountSessionManager.getInstance(requireContext());
                if (session.isLoggedIn()) {
                    openDashboardService();
                } else {
                    openOnboardingService();
                }
            });
        }

        View btnChangeAccount = view.findViewById(R.id.btnChangeAccount);
        if (btnChangeAccount != null) {
            btnChangeAccount.setOnClickListener(v -> openQuickAuthService());
        }

        view.findViewById(R.id.btnFavorites).setOnClickListener(v -> startActivity(new Intent(requireContext(), LibraryActivity.class).putExtra("favorites", true)));

        View btnLibrary = view.findViewById(R.id.btnLibrary);
        if (btnLibrary != null) {
            btnLibrary.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), LibraryActivity.class)));
        }

        ONAccountSessionManager.getInstance(requireContext()).addListener(this);
        ONAccountSessionManager.getInstance(requireContext()).bindCoreService();
        refreshProfileUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            ONAccountSessionManager.getInstance(getContext()).syncFromCoreService();
            refreshProfileUI();
        }
    }

    @Override
    public void onDestroyView() {
        if (getContext() != null) {
            ONAccountSessionManager.getInstance(getContext()).removeListener(this);
        }
        super.onDestroyView();
    }

    @Override
    public void onSessionUpdated() {
        refreshProfileUI();
    }

    public void refreshProfileUI() {
        if (!isAdded() || getContext() == null) return;
        View notice = getView() != null ? getView().findViewById(R.id.installationNoticePanel) : null;
        if (notice != null) notice.setVisibility(InstallationStatus.complete(getContext()) || installationNoticeDismissed || getContext().getSharedPreferences("ONStore",0).getBoolean("hide_installation_notice",false) ? View.GONE : View.VISIBLE);
        ONAccountSessionManager session = ONAccountSessionManager.getInstance(getContext());
        int avatarRadius = Math.max(1, Math.round(getResources().getDimension(R.dimen.profile_avatar_corner_radius)));

        if (!session.isLoggedIn()) {
            if (tvProfileName != null) tvProfileName.setText("Not logged in");
            if (tvProfileUsername != null) tvProfileUsername.setVisibility(View.GONE);
            if (tvManageAccountText != null) tvManageAccountText.setText("Log in to ON");
            if (tvManageAccountDesc != null) tvManageAccountDesc.setText("Connect your core account");
            if (ivProfileAvatar != null) {
                Glide.with(this)
                        .load(R.drawable.ic_profile_placeholder)
                        .transform(new CenterCrop(), new RoundedCorners(avatarRadius))
                        .into(ivProfileAvatar);
            }
        } else {
            if (tvProfileName != null) tvProfileName.setText(session.getFullName());
            if (tvProfileUsername != null) {
                String uname = session.getUsername();
                String onId = session.getOnId();
                StringBuilder sub = new StringBuilder();
                if (!uname.isEmpty()) sub.append("@").append(uname);
                else if (!session.getEmail().isEmpty()) sub.append(session.getEmail());
                if (!onId.isEmpty()) {
                    if (sub.length() > 0) sub.append(" · ");
                    sub.append(onId);
                }
                if (sub.length() > 0) {
                    tvProfileUsername.setText(sub.toString());
                    tvProfileUsername.setVisibility(View.VISIBLE);
                } else {
                    tvProfileUsername.setVisibility(View.GONE);
                }
            }
            if (tvManageAccountText != null) tvManageAccountText.setText("Manage account");
            if (tvManageAccountDesc != null) tvManageAccountDesc.setText("Account Manager");

            if (ivProfileAvatar != null) {
                Bitmap bmp = session.getAvatarBitmap();
                if (bmp != null) {
                    Glide.with(this)
                            .load(bmp)
                            .transform(new CenterCrop(), new RoundedCorners(avatarRadius))
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(ivProfileAvatar);
                } else {
                    Glide.with(this)
                            .load(R.drawable.ic_profile_placeholder)
                            .transform(new CenterCrop(), new RoundedCorners(avatarRadius))
                            .into(ivProfileAvatar);
                }
            }
        }
    }

    private void openDashboardService() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.oldschoolnetwork.core", "com.oldschoolnetwork.core.DashboardActivity");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "ON Core Services are missing!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openOnboardingService() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.oldschoolnetwork.core", "com.oldschoolnetwork.core.OnboardingActivity");
            startActivityForResult(intent, ONAccountSessionManager.REQ_ONBOARDING);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "ON Core Services are missing!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openQuickAuthService() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.oldschoolnetwork.core", "com.oldschoolnetwork.core.QuickAuthActivity");
            startActivityForResult(intent, ONAccountSessionManager.REQ_QUICK_AUTH);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "ON Core Services are missing!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ONAccountSessionManager.REQ_QUICK_AUTH || requestCode == ONAccountSessionManager.REQ_ONBOARDING)
                && resultCode == Activity.RESULT_OK && getContext() != null) {
            ONAccountSessionManager.getInstance(getContext()).updateFromQuickAuthIntent(data, this::refreshProfileUI);
        } else if (getContext() != null) {
            ONAccountSessionManager.getInstance(getContext()).syncFromCoreService();
            refreshProfileUI();
        }
    }
}
