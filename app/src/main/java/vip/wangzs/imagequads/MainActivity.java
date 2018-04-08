package vip.wangzs.imagequads;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vip.wangzs.imagequads.tools.FileUtil;
import vip.wangzs.imagequads.tools.PermissionUtil;
import vip.wangzs.imagequads.tools.QuadsUtil;
import vip.wangzs.imagequads.tools.UriUtil;
import vip.wangzs.imagequads.tools.WindowUtil;
import vip.wangzs.imagequads.view.QuadView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CHOSE_IMAGE = 2;

    private Uri selectedImgUri;
    private ImageView cropImgView;
    private Button createQuadBtn;
    private QuadView quadView;

    private View quadOpRect;
    private Button controlBtn;
    private Button saveBtn;
    private Button reelectBtn;
    private TextView tipsTxtView;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowUtil.setStatusBarTransparent(this, false);

        setContentView(R.layout.activity_main);

        PermissionUtil.verifyPermissions(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE);

        handler = new Handler();
        initView();
        setListener();
    }

    private void initView() {
        cropImgView = findViewById(R.id.select_image_iv);
        createQuadBtn = findViewById(R.id.create_quad_btn);
        quadView = findViewById(R.id.quad_view);

        quadOpRect = findViewById(R.id.quad_operator_rect);
        controlBtn = findViewById(R.id.control_btn);
        saveBtn = findViewById(R.id.save_btn);
        reelectBtn = findViewById(R.id.reelect_btn);
        tipsTxtView = findViewById(R.id.tips_txt);
    }

    private void setListener() {
        cropImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtil.openFileChooser(MainActivity.this,
                        getString(R.string.pic_chose_title),
                        "image/*",
                        REQUEST_CHOSE_IMAGE);
            }
        });

        cropImgView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (selectedImgUri != null) {
                    startUCrop(selectedImgUri);
                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.long_click_err_tip,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        createQuadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createQuadBtn.setVisibility(View.GONE);
                cropImgView.setVisibility(View.GONE);
                quadView.setVisibility(View.VISIBLE);
                quadOpRect.setVisibility(View.VISIBLE);
                setControlBtn(QuadView.STATUS_RUN);

                Log.d("MainActivity", "input quad image w:" + cropImgView.getWidth() + " h:" + cropImgView.getHeight());

                QuadsUtil.Model quadModel = new QuadsUtil.Model().init(
                        UriUtil.getRealFilePath(MainActivity.this, selectedImgUri),
                        cropImgView.getWidth(),
                        cropImgView.getHeight()
                );
                quadView.startInit(quadModel, QuadsUtil.MODE_CIRCLE, new QuadView.OnStatusChange() {
                    @Override
                    public void onMaxSplitStop() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setControlBtn(QuadView.STATUS_STOP);
                            }
                        });
                    }

                    @Override
                    public void onSplit(final int cnt) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (cnt > 0) {
                                    tipsTxtView.setText(
                                            String.format(Locale.getDefault(),
                                                    getString(R.string.split_cnt_format), cnt)
                                    );
                                }
                            }
                        });
                    }
                });
            }
        });

        controlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int status = quadView.startOrPause();
                setControlBtn(status);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                    File file = new File(FileUtil.getWangzsDir(), "quad_" + sf.format(new Date()) + ".jpg");
                    Toast.makeText(MainActivity.this, file.getAbsolutePath() + "已保存", Toast.LENGTH_LONG).show();
                    quadView.saveToFile(file);
                    saveBtn.setEnabled(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        reelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quadView.stop();
                selectedImgUri = null;

                tipsTxtView.setText(R.string.help_tip);
                cropImgView.setVisibility(View.VISIBLE);
                quadView.setVisibility(View.GONE);
                quadOpRect.setVisibility(View.GONE);
                cropImgView.setImageResource(R.mipmap.chose_image_bg);
            }
        });

        findViewById(R.id.config_iv)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: 打开配置页
                    }
                });
    }

    private void setControlBtn(int status) {
        int txtId = R.string.continue_txt;
        int txtColorId = R.color.white;
        boolean enable = true;
        boolean saveEnable = status != QuadView.STATUS_RUN;

        if (status == QuadView.STATUS_RUN) {
            txtId = R.string.pause_txt;
            txtColorId = R.color.light_blue;
        } else if (status == QuadView.STATUS_STOP) {
            txtId = R.string.split_finish_txt;
            txtColorId = R.color.light_blue;
            enable = false;
        }
        saveBtn.setEnabled(saveEnable);
        controlBtn.setEnabled(enable);
        controlBtn.setText(txtId);
        controlBtn.setTextColor(ContextCompat.getColor(this, txtColorId));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CHOSE_IMAGE) {
                selectedImgUri = data.getData();
                String realFilePath = UriUtil.getRealFilePath(this, selectedImgUri);
                Log.d("MainActivity", "Chose image: " + realFilePath);
                cropImgView.setImageURI(selectedImgUri);
                createQuadBtn.setVisibility(View.INVISIBLE);
            } else if (requestCode == UCrop.REQUEST_CROP) {
                selectedImgUri = UCrop.getOutput(data);
                cropImgView.setImageURI(selectedImgUri);
                createQuadBtn.setVisibility(View.VISIBLE);
            } else if (requestCode == UCrop.RESULT_ERROR) {
                Throwable error = UCrop.getError(data);
                if (error != null) {
                    error.printStackTrace();
                }
            }
        }
    }

    /**
     * 图片裁剪
     */
    public void startUCrop(Uri imageUri) {
        File outFile = new File(FileUtil.getWangzsDir(), "tmp.jpg");
        if (outFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outFile.delete();
        }
        Uri destinationUri = Uri.fromFile(outFile);

        // 初始化，第一个参数：需要裁剪的图片；第二个参数：裁剪后图片
        UCrop uCrop = UCrop.of(imageUri, destinationUri);
        // 初始化UCrop配置
        UCrop.Options options = new UCrop.Options();
        // 设置裁剪图片可操作的手势
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        // 是否隐藏底部容器，默认显示
        //options.setHideBottomControls(false);
        // 设置toolbar颜色
        options.setToolbarColor(ActivityCompat.getColor(this, R.color.colorPrimary));
        // 设置状态栏颜色
        options.setStatusBarColor(ActivityCompat.getColor(this, R.color.colorPrimary));
        // 是否能调整裁剪框
        options.setFreeStyleCropEnabled(true);
        // UCrop配置
        uCrop.withOptions(options);
        // 设置裁剪图片的宽高比，比如16：9
        uCrop.withAspectRatio(16, 16);
        // uCrop.useSourceImageAspectRatio();
        // 跳转裁剪页面
        uCrop.start(this, UCrop.REQUEST_CROP);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                finish();
            }
        }
    }
}
