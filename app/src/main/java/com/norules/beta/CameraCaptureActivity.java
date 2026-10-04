package com.norules.beta;

import android.app.Activity;
import android.graphics.*;
import android.hardware.Camera;
import android.os.Bundle;
import android.content.Intent;
import android.view.*;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraCaptureActivity extends Activity implements SurfaceHolder.Callback, Camera.FaceDetectionListener {
    private Camera camera;
    private SurfaceView preview;
    private FaceGuideView guide;
    private boolean faceDetectionSupported=false;
    private boolean faceAligned=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FrameLayout root=new FrameLayout(this);
        preview=new SurfaceView(this);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        guide=new FaceGuideView();root.addView(guide,new FrameLayout.LayoutParams(-1,-1));
        preview.getHolder().addCallback(this);setContentView(root);
        guide.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_UP && guide.inShutter(e.getX(),e.getY())){capture();return true;}return true;
        });
    }

    private int findFront(){
        int count=Camera.getNumberOfCameras();Camera.CameraInfo info=new Camera.CameraInfo();
        for(int i=0;i<count;i++){Camera.getCameraInfo(i,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT)return i;}
        return 0;
    }

    @Override public void surfaceCreated(SurfaceHolder h){
        try{
            camera=Camera.open(findFront());camera.setPreviewDisplay(h);camera.setDisplayOrientation(90);
            Camera.Parameters p=camera.getParameters();
            List<Camera.Size> pics=p.getSupportedPictureSizes();if(pics!=null&&!pics.isEmpty()){Camera.Size best=pics.get(0);long bestArea=0;for(Camera.Size s:pics){long area=(long)s.width*s.height;if(area<=3000000&&area>bestArea){best=s;bestArea=area;}}p.setPictureSize(best.width,best.height);}camera.setParameters(p);
            camera.startPreview();
            faceDetectionSupported=p.getMaxNumDetectedFaces()>0;
            if(faceDetectionSupported){camera.setFaceDetectionListener(this);try{camera.startFaceDetection();}catch(Exception ignored){faceDetectionSupported=false;}}
            guide.invalidate();
        }catch(Exception e){setResult(RESULT_CANCELED);finish();}
    }

    @Override public void onFaceDetection(Camera.Face[] faces,Camera c){
        boolean aligned=false;
        if(faces!=null&&faces.length>0){Rect r=faces[0].rect;float cx=(r.left+r.right)/2f,cy=(r.top+r.bottom)/2f;float w=r.width();aligned=Math.abs(cx)<260&&Math.abs(cy)<330&&w>650&&w<1750;}
        faceAligned=aligned;guide.invalidate();
    }

    private void capture(){
        if(camera==null)return;
        if(faceDetectionSupported&&!faceAligned){guide.flashBad();return;}
        try{camera.takePicture(null,null,(data,cam)->{
            try{File f=new File(getCacheDir(),"nr_selfie_"+System.currentTimeMillis()+".jpg");try(FileOutputStream out=new FileOutputStream(f)){out.write(data);}Intent i=new Intent();i.putExtra("photo_path",f.getAbsolutePath());setResult(RESULT_OK,i);}catch(Exception e){setResult(RESULT_CANCELED);}finish();
        });}catch(Exception e){setResult(RESULT_CANCELED);finish();}
    }

    @Override public void surfaceChanged(SurfaceHolder h,int format,int w,int hh){if(camera!=null){try{camera.stopPreview();camera.setPreviewDisplay(h);camera.startPreview();if(faceDetectionSupported)camera.startFaceDetection();}catch(Exception ignored){}}}
    @Override public void surfaceDestroyed(SurfaceHolder h){release();}
    @Override protected void onPause(){release();super.onPause();}
    private void release(){if(camera!=null){try{camera.stopFaceDetection();}catch(Exception ignored){}camera.stopPreview();camera.release();camera=null;}}

    private class FaceGuideView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);long badUntil=0;RectF shutter=new RectF();
        FaceGuideView(){super(CameraCaptureActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        boolean inShutter(float x,float y){return shutter.contains(x,y);}void flashBad(){badUntil=System.currentTimeMillis()+700;invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float W=getWidth(),H=getHeight();boolean bad=System.currentTimeMillis()<badUntil;int cyan=Color.rgb(57,247,255),lime=Color.rgb(223,255,0),pink=Color.rgb(255,46,166);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(bad?pink:(faceAligned?lime:cyan));RectF oval=new RectF(W*.20f,H*.16f,W*.80f,H*.66f);c.drawOval(oval,p);p.setStrokeWidth(4);c.drawLine(W*.32f,H*.37f,W*.68f,H*.37f,p);c.drawLine(W*.50f,H*.40f,W*.50f,H*.48f,p);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(48);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);String msg=faceDetectionSupported?(faceAligned?"READY!":"CENTER YOUR FACE"):"FIT YOUR FACE IN THE GUIDE";c.drawText(msg,W*.5f,H*.73f,p);shutter.set(W*.20f,H*.79f,W*.80f,H*.90f);p.setColor(faceDetectionSupported&&!faceAligned?Color.rgb(70,76,100):lime);c.drawRoundRect(shutter,28,28,p);p.setColor(Color.rgb(8,12,28));p.setTextSize(52);c.drawText("SNAP",W*.5f,H*.86f,p);p.setTextSize(26);p.setColor(Color.WHITE);c.drawText("NO RULES PHOTO BOOTH",W*.5f,H*.08f,p);if(bad){p.setColor(pink);p.setTextSize(34);c.drawText("MOVE CLOSER AND CENTER",W*.5f,H*.77f,p);postInvalidateDelayed(100);}}
    }
}
