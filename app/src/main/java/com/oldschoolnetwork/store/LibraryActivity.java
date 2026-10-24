package com.oldschoolnetwork.store;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
public class LibraryActivity extends AppCompatActivity {
 protected void onCreate(Bundle state) {
  super.onCreate(state); setContentView(R.layout.activity_library);
  findViewById(R.id.btnBackLibrary).setOnClickListener(v -> finish());
  if (state == null) getSupportFragmentManager().beginTransaction().replace(R.id.libraryContent, (getIntent().getBooleanExtra("favorites", false) ? CollectionFragment.favorites() : new CollectionFragment())).commit();
  getSupportFragmentManager().addOnBackStackChangedListener(() -> findViewById(R.id.libraryDetails).setVisibility(getSupportFragmentManager().getBackStackEntryCount() > 0 ? View.VISIBLE : View.GONE));
  if (getSupportFragmentManager().getBackStackEntryCount() > 0) findViewById(R.id.libraryDetails).setVisibility(View.VISIBLE);
 }
 public void openDetails(AppItem app) {
  AppDetailsFragment details = new AppDetailsFragment(); Bundle args = new Bundle(); args.putSerializable("APP_ITEM", app); details.setArguments(args);
  findViewById(R.id.libraryDetails).setVisibility(View.VISIBLE);
  getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left,R.anim.slide_in_left,R.anim.slide_out_right).replace(R.id.libraryDetails, details).addToBackStack(null).commit();
 }
}
