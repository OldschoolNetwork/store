package com.oldschoolnetwork.store;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import androidx.core.view.ViewCompat;

/** Lightweight pull gesture using existing widgets; child scrolling keeps priority. */
public final class PullRefreshLayout extends FrameLayout implements StoreRepository.CatalogListener {
    private final ProgressBar indicator;
    private float startX, startY;
    private boolean eligible, dragging;
    private Runnable refresh;
    public PullRefreshLayout(Context context) {
        super(context);
        indicator = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        indicator.setIndeterminate(true); indicator.setVisibility(GONE);
    }
    public void setContent(View child, Runnable action) {
        refresh = action; addView(child, new LayoutParams(-1,-1));
        addView(indicator, new LayoutParams(-1, Math.max(3,(int)(4*getResources().getDisplayMetrics().density))));
    }
    private boolean scrolled(View view) {
        if (ViewCompat.canScrollVertically(view,-1)) return true;
        if (view instanceof com.google.android.material.appbar.AppBarLayout && view.getTop()<0) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i).getVisibility()==VISIBLE && scrolled(group.getChildAt(i)))return true;
        }
        return false;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){startX=event.getX();startY=event.getY();eligible=!scrolled(getChildAt(0));dragging=false;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE && eligible && !StoreRepository.getInstance().isRefreshing()){
            float dy=event.getY()-startY,dx=Math.abs(event.getX()-startX);
            if(dy>48*getResources().getDisplayMetrics().density && dy>dx*2){dragging=true;indicator.setVisibility(VISIBLE);return true;}
        }
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(!dragging)return super.onTouchEvent(event);
        if(event.getActionMasked()==MotionEvent.ACTION_UP){dragging=false;if(refresh!=null)refresh.run();if(!StoreRepository.getInstance().isRefreshing())indicator.setVisibility(GONE);}
        if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){dragging=false;indicator.setVisibility(GONE);}
        return true;
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();StoreRepository.getInstance().addListener(this);}
    @Override protected void onDetachedFromWindow(){StoreRepository.getInstance().removeListener(this);super.onDetachedFromWindow();}
    @Override public void onCatalogUpdated(java.util.List<AppItem> apps,java.util.List<BannerItem> banners){indicator.setVisibility(StoreRepository.getInstance().isRefreshing()?VISIBLE:GONE);}
}
