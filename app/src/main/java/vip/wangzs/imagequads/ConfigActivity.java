package vip.wangzs.imagequads;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import vip.wangzs.imagequads.tools.QuadsUtil;
import vip.wangzs.imagequads.tools.SpConfigUtil;
import vip.wangzs.imagequads.tools.WindowUtil;

public class ConfigActivity extends AppCompatActivity {

    int bgColor;
    ImageView choseColorIv;
    RadioGroup choseModeRg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowUtil.setStatusBarTransparent(this, false);

        setContentView(R.layout.activity_config);
        initData();
        initView();
        setListener();
    }

    private void initData() {
        bgColor = (int) SpConfigUtil.get(this, SpConfigUtil.QUAD_BG_COLOR, Color.BLACK);
    }

    private void initView() {
        choseColorIv = findViewById(R.id.chose_bg_color_iv);
        choseColorIv.setBackgroundColor(bgColor);

        choseModeRg = findViewById(R.id.mode_chose_rg);
        choseModeRg.check(getCheckId());
    }

    private void setListener() {
        choseColorIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChoseColorDlg();
            }
        });

        choseModeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rect_rb:
                        saveMode(QuadsUtil.MODE_RECT);
                        break;
                    case R.id.circle_rb:
                        saveMode(QuadsUtil.MODE_CIRCLE);
                        break;
                    case R.id.oval_rb:
                        saveMode(QuadsUtil.MODE_OVAL);
                        break;
                    case R.id.round_rect_rb:
                        saveMode(QuadsUtil.MODE_ROUND_RECT);
                        break;
                    case R.id.hex_rb:
                        saveMode(QuadsUtil.MODE_HEX);
                        break;
                    default:
                        saveMode(QuadsUtil.MODE_ROUND_RECT);
                        break;
                }
            }
        });
    }


    private void saveMode(int mode) {
        Log.d("TAG", "chose mode: " + mode);
        SpConfigUtil.put(this, SpConfigUtil.SHAPE_MODE, mode);
    }

    private int getCheckId() {
        int mode = (int) SpConfigUtil.get(this, SpConfigUtil.SHAPE_MODE, QuadsUtil.MODE_CIRCLE);
        switch (mode) {
            case QuadsUtil.MODE_RECT:
                return R.id.rect_rb;
            case QuadsUtil.MODE_CIRCLE:
                return R.id.circle_rb;
            case QuadsUtil.MODE_OVAL:
                return R.id.oval_rb;
            case QuadsUtil.MODE_ROUND_RECT:
                return R.id.round_rect_rb;
            case QuadsUtil.MODE_HEX:
                return R.id.hex_rb;
            default:
                return R.id.round_rect_rb;
        }
    }

    private void setSelectedColor(int selectedColor) {
        bgColor = selectedColor;
        choseColorIv.setBackgroundColor(bgColor);
        SpConfigUtil.put(this, SpConfigUtil.QUAD_BG_COLOR, bgColor);
    }


    private void showChoseColorDlg() {
        ColorPickerDialogBuilder
                .with(this, R.style.AppDialogTheme)
                .setTitle("Choose color")
                .initialColor(bgColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {

                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        setSelectedColor(selectedColor);
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }
}
