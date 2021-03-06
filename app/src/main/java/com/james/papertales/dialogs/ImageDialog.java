package com.james.papertales.dialogs;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.james.papertales.R;
import com.james.papertales.Supplier;
import com.james.papertales.adapters.ImagePagerAdapter;
import com.james.papertales.data.WallData;
import com.james.papertales.utils.ImageUtils;
import com.james.papertales.views.PageIndicator;

import java.io.IOException;

public class ImageDialog extends AppCompatDialog {

    private Activity activity;
    private Supplier supplier;
    private DownloadReceiver downloadReceiver;

    private Integer image;
    private WallData data;

    private Toolbar toolbar;
    private ViewPager viewPager;

    public ImageDialog(Activity activity) {
        super(activity, R.style.AppTheme_Dialog);
        this.activity = activity;
        supplier = (Supplier) activity.getApplicationContext();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_image);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        if (data != null) toolbar.setTitle(data.name);

        toolbar.inflateMenu(R.menu.menu_image);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_fullscreen:
                        dismiss();
                        break;
                }
                return false;
            }
        });

        viewPager.setAdapter(new ImagePagerAdapter(activity, data, false));
        ((PageIndicator) findViewById(R.id.indicator)).setViewPager(viewPager);
        if (image != null) viewPager.setCurrentItem(image);

        downloadReceiver = new DownloadReceiver(getContext());

        findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (data == null || image == null) return;

                downloadReceiver.register();

                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    supplier.downloadWallpaper(getContext(), data.name, data.images.get(image));
                else
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 8027);
            }
        });

        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (data != null && image != null)
                    supplier.shareWallpaper(getContext(), data.images.get(image));
            }
        });

        findViewById(R.id.apply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (data == null || image == null) return;

                Glide.with(getContext()).load(data.images.get(image)).into(new SimpleTarget<GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SET_WALLPAPER) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                WallpaperManager.getInstance(getContext()).setBitmap(ImageUtils.drawableToBitmap(resource));
                                Toast.makeText(getContext(), R.string.set_wallpaper_success, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), R.string.set_wallpaper_failed, Toast.LENGTH_SHORT).show();
                            }
                        } else
                            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.SET_WALLPAPER}, 9274);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        Toast.makeText(getContext(), R.string.download_failed, Toast.LENGTH_SHORT).show();
                    }
                });

                if (isShowing()) dismiss();
            }
        });
    }

    public ImageDialog setImage(int image) {
        this.image = image;
        if (viewPager != null) viewPager.setCurrentItem(image);
        return this;
    }

    public ImageDialog setWallpaper(WallData data) {
        this.data = data;
        if (toolbar != null) toolbar.setTitle(data.name);
        return this;
    }

    @Override
    public void onStop() {
        if (downloadReceiver != null && downloadReceiver.isRegistered()) downloadReceiver.unregister();
        super.onStop();
    }

    private static class DownloadReceiver extends BroadcastReceiver {

        private Context context;
        private boolean isRegistered;

        public DownloadReceiver(Context context) {
            this.context = context;
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            ((Supplier) context.getApplicationContext()).getDownloadedDialog(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                }
            }).show();

            unregister();
        }

        public void register() {
            context.registerReceiver(this, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            isRegistered = true;
        }

        public void unregister() {
            context.unregisterReceiver(this);
            isRegistered = false;
        }

        public boolean isRegistered() {
            return isRegistered;
        }
    }
}
