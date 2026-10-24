package com.oldschoolnetwork.store;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
public class NotificationsFragment extends Fragment implements ONAccountSessionManager.SessionListener {
 private View root; private ONAccountSessionManager session; private int generation;
 public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) { return inflater.inflate(R.layout.fragment_store_notifications, container, false); }
 public void onViewCreated(View view, Bundle state) { root = view; session = ONAccountSessionManager.getInstance(requireContext()); session.addListener(this); root.findViewById(R.id.notificationStatus).setOnClickListener(v -> load()); load(); }
 public void onResume() { super.onResume(); if (root != null) load(); }
 public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (!hidden && root != null) load(); }
 public void onSessionUpdated() { if (root != null && !isHidden()) load(); }
 private void load() {
  int version = ++generation; TextView status = root.findViewById(R.id.notificationStatus); LinearLayout rows = root.findViewById(R.id.notificationRows); rows.removeAllViews();
  if (!session.isLoggedIn()) { status.setText("Sign in to view your Store notifications."); return; } status.setText("Loading…");
  StoreRepository.getInstance().requestAccountAction("store_notifications", new JSONObject(), session, (result, error) -> {
   if (root == null || version != generation) return;
   if (error != null || !result.has("notifications")) { status.setText("Could not load notifications. Tap to retry."); return; }
   JSONArray messages = result.optJSONArray("notifications"); status.setText(messages == null || messages.length() == 0 ? "No new notifications" : "");
   if (messages != null) for (int i = 0; i < messages.length(); i++) {
    JSONObject item = messages.optJSONObject(i); if (item == null) continue;
    View row = getLayoutInflater().inflate(R.layout.item_review, rows, false);
    ((TextView) row.findViewById(R.id.reviewAuthor)).setText(item.optString("subject"));
    ((TextView) row.findViewById(R.id.reviewRating)).setText(item.optString("updated_at"));
    ((TextView) row.findViewById(R.id.reviewText)).setText(item.optString("message")); rows.addView(row);
   }
  });
 }
 public void onDestroyView() { generation++; if (session != null) session.removeListener(this); root = null; super.onDestroyView(); }
}
