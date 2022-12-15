package com.example.codiistestapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.codiistestapplication.databinding.SignatureBinding;
import com.google.android.material.slider.RangeSlider;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import yuku.ambilwarna.AmbilWarnaDialog;

public class CaptureSignature extends Activity {

    public static String tempDir;
    public String current = null;
    SignatureBinding binding;
    signature mSignature;
    View mView;
    File mypath;
    private String uniqueId;

    @SuppressLint("ResourceAsColor")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = SignatureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tempDir = Environment.getExternalStorageDirectory() + "/" + getResources().getString(R.string.external_dir) + "/";
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir(getResources().getString(R.string.external_dir), Context.MODE_PRIVATE);

        prepareDirectory();
        uniqueId = getTodaysDate() + "_" + getCurrentTime() + "_" + Math.random();
        current = uniqueId + ".png";
        mypath = new File(directory, current);

        mSignature = new signature(this, null);
        mSignature.setBackgroundColor(R.color.primary_sign);
        binding.linearLayout.addView(mSignature, LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        binding.saveSign.setEnabled(false);
        mView = binding.linearLayout;

        binding.undoSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignature.onClickUndo();
            }
        });

        binding.redoSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignature.onClickRedo();
            }
        });

        binding.penColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignature.changeColor();
            }
        });
        binding.penType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignature.changePenType();
            }
        });
        binding.clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("log_tag", "Panel Cleared");
                mSignature.clear();
                binding.saveSign.setEnabled(false);
            }
        });
        binding.export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignature.export(mView);
            }
        });
        binding.saveSign.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("log_tag", "Panel Saved");
                boolean error = captureSignature();
                if (!error) {
                    mSignature.save(mView);

                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        Log.w("GetSignature", "onDestory");
        super.onDestroy();
    }

    private boolean captureSignature() {

        boolean error = false;
        String errorMessage = "";

        if (error) {
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 105, 50);
            toast.show();
        }

        return error;
    }

    private String getTodaysDate() {

        final Calendar c = Calendar.getInstance();
        int todaysDate = (c.get(Calendar.YEAR) * 10000) +
                ((c.get(Calendar.MONTH) + 1) * 100) +
                (c.get(Calendar.DAY_OF_MONTH));
        Log.w("DATE:", String.valueOf(todaysDate));
        return (String.valueOf(todaysDate));

    }

    private String getCurrentTime() {

        final Calendar c = Calendar.getInstance();
        int currentTime = (c.get(Calendar.HOUR_OF_DAY) * 10000) +
                (c.get(Calendar.MINUTE) * 100) +
                (c.get(Calendar.SECOND));
        Log.w("TIME:", String.valueOf(currentTime));
        return (String.valueOf(currentTime));

    }


    private boolean prepareDirectory() {
        try {
            return makedirs();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not initiate File System.. Is Sdcard mounted properly?", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean makedirs() {
        File tempdir = new File(tempDir);
        if (!tempdir.exists())
            tempdir.mkdirs();

        if (tempdir.isDirectory()) {
            File[] files = tempdir.listFiles();
            for (File file : files) {
                if (!file.delete()) {
                    System.out.println("Failed to delete " + file);
                }
            }
        }
        return (tempdir.isDirectory());
    }

    public class signature extends View  {
        private static final float STROKE_WIDTH = 5f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
        private static final float TOUCH_TOLERANCE = 4;
        private final RectF dirtyRect = new RectF();
        private final Paint paint = new Paint();
        private final ArrayList<Path> paths = new ArrayList<Path>();
        private final ArrayList<Path> undonePaths = new ArrayList<Path>();
        Canvas canvas = new Canvas();
        private Path path = new Path();
        /* private float lastTouchX;
         private float lastTouchY;*/
        private float mX, mY;
        private RangeSlider rangeSlider;

        public signature(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
        }

        public void save(View v) {
            mView.setDrawingCacheEnabled(true);
            String imgSaved = MediaStore.Images.Media.insertImage(
                    getContentResolver(), mView.getDrawingCache(), UUID.randomUUID()
                            .toString() + ".png", "drawing");

            if (imgSaved != null) {
                Toast savedToast = Toast.makeText(getApplicationContext(),
                        "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                savedToast.show();
            } else {
                Toast unsavedToast = Toast.makeText(getApplicationContext(),
                        "Oops! Image could not be saved.", Toast.LENGTH_SHORT);

                unsavedToast.show();
            }
            mView.destroyDrawingCache();

        }

        public void export(View v) {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);

            intentShareFile.setType("application/png");

            intentShareFile.putExtra(Intent.EXTRA_STREAM, MediaStore.Images.Media.insertImage(
                    getContentResolver(), mView.getDrawingCache(), UUID.randomUUID()
                            .toString() + ".png", "drawing"));

            startActivity(Intent.createChooser(intentShareFile, "Share File"));

        }

        public void clear() {
            undonePaths.clear();
            paths.clear();
            path.reset();
            invalidate();
        }


        public void changePenType() {
            if (paint.getStyle() == Paint.Style.FILL) {
                paint.setStyle(Paint.Style.STROKE);
                Toast.makeText(CaptureSignature.this, "Selected : Stroke", Toast.LENGTH_SHORT).show();
            } else if (paint.getStyle() == Paint.Style.STROKE) {
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                Toast.makeText(CaptureSignature.this, "Selected : Fill & Stroke", Toast.LENGTH_SHORT).show();
            } else if (paint.getStyle() == Paint.Style.FILL_AND_STROKE) {
                paint.setStyle(Paint.Style.FILL);
                Toast.makeText(CaptureSignature.this, "Selected : Fill", Toast.LENGTH_SHORT).show();

            }

        }

        public void changeColor() {
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(CaptureSignature.this, paint.getColor(), true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    paint.setColor(color);
                    binding.penColor.setBackgroundColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                    Toast.makeText(getApplicationContext(), "Action canceled!", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        }


        @Override
        protected void onDraw(Canvas canvas) {
            for (Path p : paths) {
                canvas.drawPath(p, paint);
            }
            canvas.drawPath(path, paint);
            if (undonePaths.size() < 0) {
                undonePaths.clear();
            }

        }

        private void touch_start(float x, float y) {
            undonePaths.clear();
            path.reset();
            path.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
        }

        private void touch_up() {
            path.lineTo(mX, mY);
            paths.add(path);
            path = new Path();
        }

        public void onClickUndo() {
            if (paths.size() > 0) {
                undonePaths.add(paths.remove(paths.size() - 1));
                invalidate();
            } else {

            }
        }

        public void onClickRedo() {
            if (undonePaths.size() > 0) {
                paths.add(undonePaths.remove(undonePaths.size() - 1));
                invalidate();
            } else {

            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            binding.saveSign.setEnabled(true);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(eventX, eventY);
                    invalidate();
                    break;

                case MotionEvent.ACTION_MOVE:
                    touch_move(eventX, eventY);
                    invalidate();
                    break;

                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;

                default:
                    debug("Ignored touch event: " + event);
                    return false;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            mX = eventX;
            mY = eventY;
            return true;
        }

        private void debug(String string) {
        }

        private void expandDirtyRect(float historicalX, float historicalY) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY) {
            dirtyRect.left = Math.min(mX, eventX);
            dirtyRect.right = Math.max(mX, eventX);
            dirtyRect.top = Math.min(mY, eventY);
            dirtyRect.bottom = Math.max(mY, eventY);
        }

    }
}
