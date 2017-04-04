package me.omico.currentactivity.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import me.omico.currentactivity.R;
import me.omico.currentactivity.util.Util;
import me.omico.support.widget.floatwindow.FloatWindow;
import me.omico.util.ClipboardUtils;

import static me.omico.currentactivity.Constants.ACTION_STOP;
import static me.omico.currentactivity.Constants.NOTIFICATION_ID;

public final class FloatViewService extends Service {

    private NotificationManager notificationManager;
    private FloatWindow mFloatWindow;
    private TextView mTextView;
    private View view;
    private Handler handler = new Handler();
    private boolean isStop = false;

    @Override
    public void onCreate() {
        notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        startForeground(NOTIFICATION_ID, notificationMethod());
        setFloatViewContent();
        showFloatView();
        setCurrentActivity();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            isStop = true;
            handler.removeCallbacks(runnable);
            stopSelf();
        }

        if (!isStop) mFloatWindow.show();

        handler.postDelayed(runnable, 500);

        updateNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateNotification() {
        if (!isStop) notificationManager.notify(NOTIFICATION_ID, notificationMethod());
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            setCurrentActivity();
            updateNotification();
            handler.postDelayed(this, 500);
        }
    };

    private void setCurrentActivity() {
        mTextView.setText(Util.getCurrentActivity(this));
    }

    private Notification notificationMethod() {
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, FloatViewService.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("当前应用包名，点击查看详细")
                .setContentText(Util.getCurrentActivity(this))
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher);

        Intent exitIntent = new Intent(this, FloatViewService.class).setAction(ACTION_STOP);
        builder.addAction(android.R.drawable.ic_delete, "Exit", PendingIntent.getService(this, 0, exitIntent, 0));

        return builder.build();
    }

    /*Example:
    private WindowManager.LayoutParams setLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.TOP;
        return layoutParams;
    }
    */

    private FloatWindow showFloatView() {
        mFloatWindow = new FloatWindow(this);

        /*Example:
        mFloatWindow.init(view, setLayoutParams()).attach();

        mFloatWindow.init(view, setLayoutParams(), Gravity.TOP).attach();

        mFloatWindow.init(view).setLayoutParams(setLayoutParams()).attach();

        mFloatWindow.init(view).setLayoutParams(setLayoutParams(), Gravity.TOP).attach();

        mFloatWindow.init(view, setLayoutParams()).setLayoutParamsGravity(Gravity.BOTTOM).attach();

        mFloatWindow
                .init(view)
                .setLayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        getType(),
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT)
                .setLayoutParamsGravity(Gravity.TOP)
                .attach();
        */

        mFloatWindow
                .init(view)
                .setLayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        getType(),
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT,
                        Gravity.TOP)
                .attach();

        mFloatWindow.setOnFloatWindowClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFloatWindow.hide();
            }
        });

        mFloatWindow.setOnFloatWindowLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardUtils.copyToClipboard(getApplicationContext(), mTextView.getText().toString());
                return true;
            }
        });

        return mFloatWindow;
    }

    private int getType() {
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            } else {
                type = WindowManager.LayoutParams.TYPE_TOAST;
            }
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return type;
    }

    private void setFloatViewContent() {
        view = View.inflate(this, R.layout.pop_view, null);
        mTextView = (TextView) view.findViewById(R.id.pop_view_text);
        view.setSystemUiVisibility(view.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStop = true;
        stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
        mFloatWindow.detach();
    }
}