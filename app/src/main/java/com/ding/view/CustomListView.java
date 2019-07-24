package com.ding.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jindingwei on 2019/7/22.
 */

public class CustomListView extends AdapterView {

    private Adapter mAdapter;
    private int mLastItemPosition = -1;
    private int mFirstItemPosition;
    private int mStartDownY;
    private int mStartDownX;
    private boolean isSliding;
    private int mLastMoveY;
    private int mFlingY;
    private List<View> mScrapView = new LinkedList<>();
    private View[] mActiveViews;
    private OnItemClickListener mOnItemClickListener;
    private VelocityTracker mVelocityTracker;
    private FlingRunnable mFlingRunnable;
    private DataSetObserver mDataChangeObserver;
    private boolean mDataChange;

    public CustomListView(Context context) {
        super(context);
        init();
    }

    public CustomListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mFlingRunnable = new FlingRunnable();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getChildCount() == 0) {
            fillDown(0, 0);
        } else {
            mLastItemPosition = mFirstItemPosition - 1;
            int topViewOffset = getChildAt(0).getTop();
            if (mDataChange || changed) {
                recycleAllViewToScrapViews();
                removeAllViewsInLayout();
                mDataChange = false;
            } else {
                recycleToActiveViews();
                removeAllViewsInLayout();
            }
            fillDown(mFirstItemPosition, topViewOffset);
            invalidate();
        }
    }

    private void fillDown(int pos, int startOffset) {
        while (startOffset < getHeight() && pos < mAdapter.getCount()) {
            View childView = makeAndAddView(pos, startOffset, true);
            startOffset += childView.getMeasuredHeight();
            pos++;
            mLastItemPosition++;
        }
    }

    private void fillUp(int pos, int startOffset) {
        while (startOffset > 0 && pos >= 0) {
            View child = makeAndAddView(pos, startOffset, false);
            startOffset -= child.getMeasuredHeight();
            pos--;
            mFirstItemPosition--;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAdapter != null) {
            if (mDataChangeObserver == null) {
                mDataChangeObserver = new CustomListDataSetObserver();
            }
            mAdapter.registerDataSetObserver(mDataChangeObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null && mDataChangeObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataChangeObserver);
        }
        mFlingRunnable.endFling();
    }

    private View makeAndAddView(int pos, int startOffset, boolean topToDown) {
        boolean isAttachedToWindow = false;
        boolean needMeasure = true;
        View childView = getViewFromActiveViews(pos);
        if (childView != null) {
            needMeasure = false;
        }
        if (childView == null) {
            childView = getScrapView();
        }
        if (childView != null) {
            isAttachedToWindow = true;
        }
        childView = mAdapter.getView(pos, childView, this);
        LayoutParams params = childView.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        if (isAttachedToWindow) {
            attachViewToParent(childView, topToDown ? -1 : 0, params);
        } else {
            addViewInLayout(childView, -1, params, true);
        }
        if (needMeasure) {
            int measureWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
            childView.measure(measureWidth, MeasureSpec.UNSPECIFIED);
        }

        int width = childView.getMeasuredWidth();
        int height = childView.getMeasuredHeight();
        int bottom = 0;
        int top = 0;
        if (topToDown) {
            top = startOffset;
            bottom = startOffset + height;
        } else {
            top = startOffset - height;
            bottom = startOffset;
        }
        childView.layout(0, top, width, bottom);

        return childView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mFlingRunnable.isFling()) {
                    mFlingRunnable.endFling();
                }
                mStartDownX = x;
                mStartDownY = y;
                mLastMoveY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isSliding) {
                    if (isSlideGesture(x, y)) {
                        isSliding = true;
                    }
                } else {
                    int incrementalY = y - mLastMoveY;
                    if (incrementalY != 0) {
                        fling(incrementalY);
                        mLastMoveY = y;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!isSliding) {
                    clickItem(event);
                } else {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    int initYVelocity = (int) mVelocityTracker.getYVelocity();
                    if (Math.abs(initYVelocity) > ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity()) {
                        mFlingRunnable.startFling(initYVelocity);
                    }
                }
                isSliding = false;
                mStartDownY = 0;
                mStartDownX = 0;
                mLastMoveY = 0;
                break;
        }
        return true;
    }

    private class FlingRunnable implements Runnable {
        private Scroller scroller;

        public FlingRunnable() {
            if (scroller == null) {
                scroller = new Scroller(getContext());
            }
        }

        public void startFling(int initVelocity) {
            int startY = initVelocity < 0 ? Integer.MAX_VALUE : 0;
            mFlingY = startY;
            scroller.fling(0, startY, 0, initVelocity,
                    0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            postOnAnimation(mFlingRunnable);
        }

        public boolean isFling() {
            return !scroller.isFinished();
        }

        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                int curY = scroller.getCurrY();
                int incrementalY = curY - mFlingY;
                if (incrementalY != 0) {
                    fling(incrementalY);
                }
                mFlingY = curY;
                postOnAnimation(FlingRunnable.this);
            }
        }

        public void endFling() {
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
        }
    }

    private void clickItem(MotionEvent event) {
        if (mOnItemClickListener != null) {
            int index = getClickViewIndex(event);
            if (index >= 0) {
                int position = mFirstItemPosition + index;
                mOnItemClickListener.onItemClick(this, getChildAt(index), position, mAdapter.getItemId(position));
            }
        }
    }

    private int getClickViewIndex(MotionEvent event) {
        int y = (int) event.getY();
        for (int i = getChildCount() - 1;i >= 0;i--) {
            View view = getChildAt(i);
            if (y > view.getTop()) {
                return i;
            }
        }
        return -1;
    }

    private void fling(int incrementalY) {
        if (getChildCount() <= 0) {
            return;
        }
        boolean down = incrementalY < 0 ? true : false;
        if (down) {
            if (mLastItemPosition >= mAdapter.getCount() - 1) {
                View child = getChildAt(getChildCount() - 1);
                if (child.getBottom() <= getHeight()) {
                    if (mFlingRunnable.isFling()) {
                        mFlingRunnable.endFling();
                    }
                    return;
                }
                if (child.getBottom() - getHeight() < Math.abs(incrementalY)) {
                    incrementalY = -(child.getBottom() - getHeight());
                }
            }
        } else {
            if (mFirstItemPosition <= 0) {
                View child = getChildAt(0);
                if (child.getTop() >= 0) {
                    if (mFlingRunnable.isFling()) {
                        mFlingRunnable.endFling();
                    }
                    return;
                }
                if (Math.abs(child.getTop()) < incrementalY) {
                    incrementalY = Math.abs(child.getTop());
                }
            }
        }
        removeLoseView(down, incrementalY);
        if (down) {
            offsetChilder(incrementalY);
        } else {
            offsetChilder(incrementalY);
        }
        fillGap(down);
        invalidate();
    }

    private void removeLoseView(boolean down, int incrementalY) {
        int count = 0;
        int start = 0;
        int absDiffY = Math.abs(incrementalY);
        if (down) {
            for (int i = 0;i < getChildCount();i++) {
                View child = getChildAt(i);
                if (child.getBottom() - absDiffY > 0) {
                    break;
                } else {
                    addScrapView(child);
                    count++;
                    mFirstItemPosition++;
                }
            }
        } else {
            for (int i = getChildCount() - 1;i >= 0;i--) {
                View child = getChildAt(i);
                if (child.getTop() + absDiffY <= getHeight()) {
                    break;
                } else {
                    addScrapView(child);
                    start = i;
                    count++;
                    mLastItemPosition--;
                }
            }
        }
        if (count > 0) {
            detachViewsFromParent(start, count);
        }
    }

    private void recycleToActiveViews() {
        int count = getChildCount();
        if (count <= 0) {
            return;
        }
        mActiveViews = new View[count];
        for (int i = 0;i < count;i++) {
            mActiveViews[i] = getChildAt(i);
        }
    }

    private void recycleAllViewToScrapViews() {
        int count = getChildCount();
        if (count <= 0) {
            return;
        }
        for (int i = 0;i < count;i++) {
            mScrapView.add(getChildAt(i));
        }
    }

    private View getViewFromActiveViews(int pos) {
        if (pos >= 0 && mActiveViews != null && pos < mActiveViews.length) {
            View child = mActiveViews[pos];
            mActiveViews[pos] = null;
            return child;
        }
        return null;
    }

    private void addScrapView(View view) {
        mScrapView.add(view);
    }

    private View getScrapView() {
        if (mScrapView.size() > 0) {
            return mScrapView.remove(0);
        }
        return null;
    }

    private void fillGap(boolean down) {
        int count = getChildCount();
        if (down) {
            int startOffset = count > 0 ? getChildAt(count - 1).getBottom() : 0;
            fillDown(mLastItemPosition + 1, startOffset);
        } else {
            int startOffset = count > 0 ? getChildAt(0).getTop() : 0;
            fillUp(mFirstItemPosition - 1, startOffset);
        }
    }

    private void offsetChilder(int offset) {
        for (int i = 0;i < getChildCount();i++) {
            View child = getChildAt(i);
            child.offsetTopAndBottom(offset);
        }
    }

    private boolean isSlideGesture(int curX, int curY) {
        int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        int diffX = Math.abs(mStartDownX - curX);
        int diffY = Math.abs(mStartDownY - curY);
        if (diffX > touchSlop || diffY > touchSlop) {
            return true;
        }
        return false;
    }

    class CustomListDataSetObserver extends DataSetObserver {

        public CustomListDataSetObserver() {
            super();
        }

        @Override
        public void onChanged() {
            super.onChanged();
            mDataChange = true;
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter == null) {
            return;
        }
        mAdapter = adapter;
        removeAllViewsInLayout();
        requestLayout();
    }

    @Override
    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setSelection(int position) {

    }
}
