
package com.example.testvoicescreen;

import com.example.testvoicescreen.utils.PackageControl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * Manages an overlay display window, used for simulating remote playback.
 */
public abstract class QuickLaunchWindow {
    private static final String TAG = "QuickLaunchWindow";
    private static final boolean DEBUG = true;

    private static final float WINDOW_ALPHA = 0.5f;
    private static final float INITIAL_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 1.0f;

    protected final Context mContext;
    protected final String mName;
    protected int mWidth;
    protected int mHeight;
    protected final int mGravity;
    protected OverlayWindowListener mListener;
    protected static QuickLaunchWindow mQlwInstance = null;
    protected static boolean isWindowShown = false;

    protected QuickLaunchWindow(Context context, String name,
            int width, int height, int gravity) {
        mContext = context;
        mName = name;
        mWidth = width;
        mHeight = height;
        mGravity = gravity;
    }

    public static QuickLaunchWindow create(Context context, String name,
            int width, int height, int gravity) {

        if (mQlwInstance == null) {
            mQlwInstance = new createWindowsImpl(context, name, width, height, gravity);
        }
        return mQlwInstance;

    }

    public void setOverlayWindowListener(OverlayWindowListener listener) {
        mListener = listener;
    }

    public Context getContext() {
        return mContext;
    }

    public abstract void show();

    public abstract void dismiss();

    public abstract void updateAspectRatio(int width, int height);

    // Watches for significant changes in the overlay display window lifecycle.
    public interface OverlayWindowListener {
        public void onWindowCreated(Surface surface);

        public void onWindowCreated(SurfaceHolder surfaceHolder);

        public void onWindowDestroyed();
    }

    private static final class createWindowsImpl extends QuickLaunchWindow {
        // When true, disables support for moving and resizing the overlay.
        // The window is made non-touchable, which makes it possible to
        // directly interact with the content underneath.
        private static final boolean DISABLE_MOVE_AND_RESIZE = false;

        private final DisplayManager mDisplayManager;
        private final WindowManager mWindowManager;

        private final Display mDefaultDisplay;
        private final DisplayMetrics mDefaultDisplayMetrics = new DisplayMetrics();

        private View mWindowContent;
        private GridView mGridView;
        private PackcageListAdapter mAdapter;
        private WindowManager.LayoutParams mWindowParams;

        private GestureDetector mGestureDetector;
        // private ScaleGestureDetector mScaleGestureDetector;

        private boolean mWindowVisible;
        private int mWindowX;
        private int mWindowY;
        private float mWindowScale;

        private float mLiveTranslationX;
        private float mLiveTranslationY;

        public createWindowsImpl(Context context, String name,
                int width, int height, int gravity) {

            super(context, name, width, height, gravity);

            mDisplayManager = (DisplayManager) context.getSystemService(
                    Context.DISPLAY_SERVICE);
            mWindowManager = (WindowManager) context.getSystemService(
                    Context.WINDOW_SERVICE);

            mDefaultDisplay = mWindowManager.getDefaultDisplay();

            updateDefaultDisplayInfo();

            createLauncherWindow();
        }

        @Override
        public void show() {
            if (!mWindowVisible) {
                mDisplayManager.registerDisplayListener(mDisplayListener, null);
                if (!updateDefaultDisplayInfo()) {
                    mDisplayManager.unregisterDisplayListener(mDisplayListener);
                    return;
                }

                clearLiveState();
                updateWindowParams();
                mWindowManager.addView(mWindowContent, mWindowParams);
                int size = PackageControl.sCurrentPackageList.size();
                String[] array = (String[]) PackageControl.sCurrentPackageList
                        .toArray(new String[size]);
                mAdapter = new PackcageListAdapter(mContext, R.layout.search_item,
                        array);

                mGridView.setAdapter(mAdapter);
                mWindowVisible = true;
                isWindowShown = true;
            } else {
                // update
                dismiss();
                show();
            }
        }

        @Override
        public void dismiss() {
            if (mWindowVisible) {
                mDisplayManager.unregisterDisplayListener(mDisplayListener);
                mWindowManager.removeView(mWindowContent);
                mWindowVisible = false;
            }
            isWindowShown = false;
        }

        @Override
        public void updateAspectRatio(int width, int height) {

        }

        private boolean updateDefaultDisplayInfo() {
            mDefaultDisplay.getMetrics(mDefaultDisplayMetrics);
            return true;
        }

        private void createLauncherWindow() {
            LayoutInflater inflater = LayoutInflater.from(mContext);

            mWindowContent = inflater.inflate(
                    R.layout.package_view, null);
            mWindowContent.setOnTouchListener(mOnTouchListener);

            mGridView = (GridView) mWindowContent.findViewById(R.id.gridView);

            mWindowParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mWindowParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

            if (DISABLE_MOVE_AND_RESIZE) {
                mWindowParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            mWindowParams.alpha = WINDOW_ALPHA;
            mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
            mWindowParams.setTitle(mName);

            // mGestureDetector = new GestureDetector(mContext, mOnGestureListener);
            // mScaleGestureDetector = new ScaleGestureDetector(mContext, mOnScaleGestureListener);

            // Set the initial position and scale.
            // The position and scale will be clamped when the display is first shown.
            /*
             * mWindowX = (mGravity & Gravity.LEFT) == Gravity.LEFT ? 0 :
             * mDefaultDisplayMetrics.widthPixels;
             */
            mWindowX = mDefaultDisplayMetrics.widthPixels;
            mWindowY = (mGravity & Gravity.TOP) == Gravity.TOP ?
                    0 : mDefaultDisplayMetrics.heightPixels / 3;
            Log.d(TAG, mDefaultDisplayMetrics.toString());
            mWindowScale = INITIAL_SCALE;

            mGridView = (GridView) mWindowContent.findViewById(R.id.gridView);

            // calculate and save initial settings
            updateWindowParams();
            saveWindowParams();
        }

        private void updateAdapter() {

            int size = PackageControl.sCurrentPackageList.size();
            String[] array = (String[]) PackageControl.sCurrentPackageList
                    .toArray(new String[size]);
            mAdapter = new PackcageListAdapter(mContext, R.layout.search_item,
                    array);

            mGridView.setAdapter(mAdapter);

            Log.d(TAG, "updateAdapter size is " + size + " " + array[0]);
        }

        private void updateWindowParams() {
            float scale = mWindowScale;
            scale = Math.min(scale, (float) mDefaultDisplayMetrics.widthPixels / mWidth);
            scale = Math.min(scale, (float) mDefaultDisplayMetrics.heightPixels / mHeight);
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));

            // float offsetScale = (scale / mWindowScale - 1.0f) * 0.5f;
            float offsetScale = 0;
            mWidth = mDefaultDisplayMetrics.widthPixels / 6;
            mHeight = (int) (mDefaultDisplayMetrics.heightPixels * 3 / 5);
            int width = (int) (mWidth * scale);
            int height = (int) (mHeight * scale);
            // int x = (int) (mWindowX + mLiveTranslationX - width * offsetScale);
            // int y = (int) (mWindowY + mLiveTranslationY - height * offsetScale);

            int x = (int) (mWindowX + mLiveTranslationX);
            int y = (int) (mWindowY + mLiveTranslationY);
            x = Math.max(0, Math.min(x, mDefaultDisplayMetrics.widthPixels - width));
            y = Math.max(0, Math.min(y, mDefaultDisplayMetrics.heightPixels - height));

            if (DEBUG) {
                Log.d(TAG, "updateWindowParams: scale=" + scale
                        + ", offsetScale=" + offsetScale
                        + ", x=" + x + ", y=" + y
                        + ", width=" + width + ", height=" + height);
            }

            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowParams.width = width;
            mWindowParams.height = height;

        }

        private void saveWindowParams() {
            mWindowX = mWindowParams.x;
            mWindowY = mWindowParams.y;
            clearLiveState();
        }

        private void clearLiveState() {
            mLiveTranslationX = 0f;
            mLiveTranslationY = 0f;
        }

        private final DisplayManager.DisplayListener mDisplayListener =
                new DisplayManager.DisplayListener() {
                    @Override
                    public void onDisplayAdded(int displayId) {
                    }

                    @Override
                    public void onDisplayChanged(int displayId) {
                        if (displayId == mDefaultDisplay.getDisplayId()) {
                            if (updateDefaultDisplayInfo()) {
                            } else {
                                dismiss();
                            }
                        }
                    }

                    @Override
                    public void onDisplayRemoved(int displayId) {
                        if (displayId == mDefaultDisplay.getDisplayId()) {
                            dismiss();
                        }
                    }
                };

        private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Work in screen coordinates.
                final float oldX = event.getX();
                final float oldY = event.getY();
                event.setLocation(event.getRawX(), event.getRawY());

                mGestureDetector.onTouchEvent(event);
                // mScaleGestureDetector.onTouchEvent(event);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        saveWindowParams();
                        break;
                }

                // Revert to window coordinates.
                event.setLocation(oldX, oldY);

                return true;
            }
        };

        private final GestureDetector.OnGestureListener mOnGestureListener =
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {
                        mLiveTranslationX -= distanceX;
                        mLiveTranslationY -= distanceY;

                        return true;
                    }
                };

        public class PackcageListAdapter extends ArrayAdapter<String> {
            private Context mContext;

            private String[] mPackageNameList;

            public PackcageListAdapter(Context context, int resource, String[] src) {
                super(context, resource, src);
                mContext = context;
                mPackageNameList = src;
               
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                convertView = View.inflate(mContext, R.layout.search_item, null);
                TextView textView = (TextView) convertView.findViewById(R.id.search_pack_name);
                ImageView icon = (ImageView) convertView.findViewById(R.id.search_pack_icon);

                try {
                    ApplicationInfo info = mContext.getPackageManager().
                            getApplicationInfo(mPackageNameList[position],
                                    PackageManager.GET_META_DATA);

                    Resources packageRes = mContext.getPackageManager()
                            .getResourcesForApplication(mPackageNameList[position]);

                    String resName = packageRes.getString(info.labelRes);

                    final Intent packageIntent = mContext.getPackageManager()
                            .getLaunchIntentForPackage(mPackageNameList[position]);
                    packageIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                    icon.setBackground(mContext.getPackageManager().getApplicationIcon(
                            mPackageNameList[position]));

                    icon.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            if (packageIntent != null) {
                                Log.d(TAG, "onclick icon");
                                mContext.startActivity(packageIntent);
                                dismiss();
                                Intent i = new Intent(getContext(), UIService.class);
                                getContext().stopService(i);
                            }

                        }

                    });
                    textView.setText(resName);
                } catch (NameNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return convertView;
            }

        }
    }
}
