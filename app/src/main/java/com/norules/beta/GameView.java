package com.norules.beta;

import android.content.Context;
import android.graphics.*;
import android.hardware.*;
import android.view.*;
import java.util.*;

public class GameView extends View implements SensorEventListener {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SensorManager sm; private final Sensor accel;
    private int screen=0; // 0 intro, 1..6 rooms, 7 finish
    private long flashUntil=0; private String flash="";
    private float ax=0, ay=0; private long lastFrame=System.nanoTime();
    private long roomStart=System.currentTimeMillis();

    // room 1
    private float keyX,keyY,keyScale=1f,keyDX,keyDY; private boolean keyDrag=false; private float pinchStart=0,keyScaleStart=1;
    // room 2
    private float ballX,ballY,vx,vy;
    // room 3
    private long balloonHold=0; private float balloonR=46; private boolean balloonPopped=false;
    // room 4
    private long calmStart=0; private int calmResets=0;
    // room 5
    private int shadowPos=0; private final int[] shadowTarget={1,3,0,2};

    public GameView(Context c){super(c);setKeepScreenOn(true);sm=(SensorManager)c.getSystemService(Context.SENSOR_SERVICE);accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);setBackgroundColor(Color.rgb(111,199,232));}
    public void onResumeGame(){if(accel!=null)sm.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);lastFrame=System.nanoTime();postInvalidateOnAnimation();}
    public void onPauseGame(){sm.unregisterListener(this);}    
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER){ax=-e.values[0];ay=e.values[1];}}
    @Override public void onAccuracyChanged(Sensor s,int a){}

    private int C(String hex){return Color.parseColor(hex);}    
    private void txt(Canvas c,String s,float x,float y,float size,int col,Paint.Align a){p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(size);p.setColor(col);p.setTextAlign(a);c.drawText(s,x,y,p);}    
    private void rr(Canvas c,float x,float y,float w,float h,float r,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}    
    private void bg(Canvas c){
        Paint q=new Paint();q.setShader(new LinearGradient(0,0,0,getHeight(),C("#78d4ef"),C("#f7d79a"),Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),q);q.setShader(null);
        p.setColor(C("#8ad06a"));p.setStyle(Paint.Style.FILL);Path a=new Path();a.moveTo(0,getHeight()*.78f);a.quadTo(getWidth()*.22f,getHeight()*.69f,getWidth()*.47f,getHeight()*.79f);a.quadTo(getWidth()*.72f,getHeight()*.88f,getWidth(),getHeight()*.73f);a.lineTo(getWidth(),getHeight());a.lineTo(0,getHeight());a.close();c.drawPath(a,p);
        p.setColor(C("#66b653"));Path b=new Path();b.moveTo(0,getHeight()*.88f);b.quadTo(getWidth()*.35f,getHeight()*.76f,getWidth()*.63f,getHeight()*.90f);b.quadTo(getWidth()*.84f,getHeight()*.98f,getWidth(),getHeight()*.84f);b.lineTo(getWidth(),getHeight());b.lineTo(0,getHeight());b.close();c.drawPath(b,p);
    }
    private void title(Canvas c,String a,String b){rr(c,getWidth()*.08f,getHeight()*.035f,getWidth()*.84f,getHeight()*.12f,22,C("#fff8dc"));txt(c,a,getWidth()/2f,getHeight()*.086f,26,C("#3b4058"),Paint.Align.CENTER);txt(c,b,getWidth()/2f,getHeight()*.124f,13,C("#788096"),Paint.Align.CENTER);}    
    private void button(Canvas c,String s,float x,float y,float w,float h){rr(c,x,y,w,h,22,C("#ffcf56"));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(C("#3b4058"));c.drawRoundRect(x,y,x+w,y+h,22,22,p);txt(c,s,x+w/2,y+h*.63f,18,C("#3b4058"),Paint.Align.CENTER);}    
    private void door(Canvas c,float cx,float cy,float w,float h){rr(c,cx-w/2,cy-h/2,w,h,28,C("#7d58c2"));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(C("#51348d"));c.drawRoundRect(cx-w/2,cy-h/2,cx+w/2,cy+h/2,28,28,p);rr(c,cx-w*.39f,cy-h*.39f,w*.78f,h*.78f,20,C("#9873da"));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(C("#3b275f"));c.drawArc(cx-w*.17f-12,cy-h*.07f-12,cx-w*.17f+12,cy-h*.07f+12,0,180,false,p);c.drawArc(cx+w*.17f-12,cy-h*.07f-12,cx+w*.17f+12,cy-h*.07f+12,0,180,false,p);p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy+h*.10f,7,p);p.setColor(C("#f6d35c"));c.drawCircle(cx+w*.29f,cy+h*.08f,8,p);}    
    private void flash(String s){flash=s;flashUntil=System.currentTimeMillis()+700;}
    private void next(){screen++;roomStart=System.currentTimeMillis();flash="";if(screen==1){keyX=getWidth()*.5f;keyY=getHeight()*.63f;keyScale=1f;}if(screen==2)resetBall();if(screen==3){balloonHold=0;balloonR=46;balloonPopped=false;}if(screen==4){calmStart=System.currentTimeMillis();calmResets=0;}if(screen==5)shadowPos=0;invalidate();}
    private void resetBall(){ballX=getWidth()*.20f;ballY=getHeight()*.70f;vx=vy=0;lastFrame=System.nanoTime();}

    @Override protected void onDraw(Canvas c){super.onDraw(c);if(screen==0)intro(c);else if(screen==1)r1(c);else if(screen==2)r2(c);else if(screen==3)r3(c);else if(screen==4)r4(c);else if(screen==5)r5(c);else if(screen==6)r6(c);else finish(c);if(!flash.isEmpty()&&System.currentTimeMillis()<flashUntil){rr(c,getWidth()*.20f,getHeight()*.90f,getWidth()*.60f,42,18,C("#3b4058"));txt(c,flash,getWidth()/2f,getHeight()*.90f+28,15,Color.WHITE,Paint.Align.CENTER);}postInvalidateOnAnimation();}
    private void intro(Canvas c){bg(c);txt(c,"NO RULES",getWidth()/2f,getHeight()*.26f,46,C("#3b4058"),Paint.Align.CENTER);txt(c,"BETA 0.3",getWidth()/2f,getHeight()*.315f,17,C("#ef6b61"),Paint.Align.CENTER);rr(c,getWidth()*.12f,getHeight()*.37f,getWidth()*.76f,getHeight()*.20f,30,C("#fff5d6"));txt(c,"6 CARTOON ROOMS",getWidth()/2f,getHeight()*.435f,20,C("#3b4058"),Paint.Align.CENTER);txt(c,"Touch. Tilt. Wait. Think weird.",getWidth()/2f,getHeight()*.485f,15,C("#6d748a"),Paint.Align.CENTER);button(c,"PLAY",getWidth()*.20f,getHeight()*.66f,getWidth()*.60f,64);}
    private void r1(Canvas c){bg(c);title(c,"ROOM 01","That key looks a little... oversized.");door(c,getWidth()*.5f,getHeight()*.36f,getWidth()*.27f,getHeight()*.30f);p.setColor(C("#392557"));p.setStyle(Paint.Style.FILL);c.drawCircle(getWidth()*.5f,getHeight()*.39f,10,p);c.drawRect(getWidth()*.5f-5,getHeight()*.39f,getWidth()*.5f+5,getHeight()*.39f+24,p);c.save();c.translate(keyX,keyY);c.scale(keyScale,keyScale);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(16);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(C("#ffd45b"));c.drawCircle(-35,0,24,p);c.drawLine(-12,0,62,0,p);c.drawLine(62,0,62,20,p);c.drawLine(38,0,38,15,p);p.setStrokeCap(Paint.Cap.BUTT);c.restore();txt(c,"Pinch it smaller, then drag it to the lock.",getWidth()/2f,getHeight()*.82f,13,C("#536075"),Paint.Align.CENTER);if(keyScale<.48f&&Math.hypot(keyX-getWidth()*.5f,keyY-getHeight()*.39f)<55){flash("PERFECT FIT!");next();}}
    private void r2(Canvas c){bg(c);title(c,"ROOM 02","Gravity is a control too.");float L=getWidth()*.09f,R=getWidth()*.91f,T=getHeight()*.23f,B=getHeight()*.78f;rr(c,L,T,R-L,B-T,28,C("#f7f0cf"));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(C("#536075"));c.drawRoundRect(L,T,R,B,28,28,p);rr(c,getWidth()*.32f,getHeight()*.33f,getWidth()*.10f,getHeight()*.26f,16,C("#ef6b61"));rr(c,getWidth()*.55f,getHeight()*.49f,getWidth()*.12f,getHeight()*.22f,16,C("#5bbca8"));long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*58*dt;vy+=ay*58*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float rad=20;if(ballX<L+rad){ballX=L+rad;vx*=-.6f;}if(ballX>R-rad){ballX=R-rad;vx*=-.6f;}if(ballY<T+rad){ballY=T+rad;vy*=-.6f;}if(ballY>B-rad){ballY=B-rad;vy*=-.6f;}float tx=getWidth()*.78f,ty=getHeight()*.31f;p.setColor(C("#ffd45b"));p.setStyle(Paint.Style.FILL);c.drawCircle(tx,ty,33,p);txt(c,"★",tx,ty+10,27,C("#795b15"),Paint.Align.CENTER);p.setColor(C("#5a476f"));c.drawCircle(ballX,ballY,rad,p);p.setColor(Color.WHITE);c.drawCircle(ballX-6,ballY-4,3,p);c.drawCircle(ballX+6,ballY-4,3,p);txt(c,"Tilt the phone. Keep the screen upright.",getWidth()/2f,getHeight()*.86f,13,C("#536075"),Paint.Align.CENTER);if(Math.hypot(ballX-tx,ballY-ty)<28){flash("NICE TILT!");next();}}
    private void r3(Canvas c){bg(c);title(c,"ROOM 03","Something is hiding in there.");float bx=getWidth()*.5f,by=getHeight()*.54f;if(balloonHold>0&&!balloonPopped){float t=Math.min(1,(System.currentTimeMillis()-balloonHold)/1600f);balloonR=46+40*t;if(t>=1){balloonPopped=true;balloonHold=0;flash("POP!");}}
        if(!balloonPopped){p.setStyle(Paint.Style.FILL);p.setColor(C("#ff6f91"));c.drawOval(bx-balloonR*.86f,by-balloonR,bx+balloonR*.86f,by+balloonR,p);p.setColor(Color.WHITE);c.drawCircle(bx-13,by-8,5,p);c.drawCircle(bx+13,by-8,5,p);txt(c,"KEY",bx,by+13,15,C("#7e3150"),Paint.Align.CENTER);}else{txt(c,"KEY",bx,by+18,38,C("#ffd45b"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>700)next();}
        txt(c,"Tapping only makes it wobble.",getWidth()/2f,getHeight()*.82f,13,C("#536075"),Paint.Align.CENTER);
    }
    private void r4(Canvas c){bg(c);title(c,"ROOM 04","The room is listening to your patience.");door(c,getWidth()*.5f,getHeight()*.48f,getWidth()*.34f,getHeight()*.36f);long e=System.currentTimeMillis()-calmStart;int rem=Math.max(0,7-(int)(e/1000));p.setColor(C("#fff5d6"));p.setStyle(Paint.Style.FILL);c.drawCircle(getWidth()*.5f,getHeight()*.72f,55,p);txt(c,rem>0?String.valueOf(rem):"…",getWidth()*.5f,getHeight()*.735f,36,C("#3b4058"),Paint.Align.CENTER);if(calmResets>0)txt(c,"Oops. Touching restarted the room.",getWidth()/2f,getHeight()*.86f,13,C("#b85d50"),Paint.Align.CENTER);if(e>=7000){flash("PATIENCE WINS!");next();}}
    private void r5(Canvas c){bg(c);title(c,"ROOM 05","The sun knows the order.");float[] xs={.24f,.43f,.62f,.81f};float[] hs={78,48,100,62};int[] cols={C("#ef6b61"),C("#5bbca8"),C("#7c6fe3"),C("#f3a94c")};p.setColor(C("#ffd45b"));p.setStyle(Paint.Style.FILL);c.drawCircle(getWidth()*.11f,getHeight()*.30f,31,p);txt(c,"SUN",getWidth()*.11f,getHeight()*.31f,12,C("#8f6b17"),Paint.Align.CENTER);for(int i=0;i<4;i++){float x=getWidth()*xs[i],base=getHeight()*.68f,h=hs[i],sh=44+h*.78f;p.setColor(Color.argb(50,73,88,88));c.drawOval(x+sh*.15f,base-8,x+sh*1.25f,base+20,p);rr(c,x-27,base-h,54,h,20,cols[i]);p.setColor(Color.WHITE);c.drawCircle(x-9,base-h+23,4,p);c.drawCircle(x+9,base-h+23,4,p);}txt(c,"Tap from the shortest shadow to the longest.",getWidth()/2f,getHeight()*.81f,13,C("#536075"),Paint.Align.CENTER);txt(c,shadowPos+"/4",getWidth()/2f,getHeight()*.87f,14,C("#536075"),Paint.Align.CENTER);}
    private void r6(Canvas c){bg(c);title(c,"ROOM 06","This lock has trust issues.");door(c,getWidth()*.5f,getHeight()*.49f,getWidth()*.46f,getHeight()*.46f);float[][] eyes={{getWidth()*.39f,getHeight()*.43f},{getWidth()*.61f,getHeight()*.43f},{getWidth()*.50f,getHeight()*.57f}};int active=0;for(int i=0;i<3;i++){boolean covered=false;for(int k=0;k<lastPointerCount;k++){if(Math.hypot(lastPX[k]-eyes[i][0],lastPY[k]-eyes[i][1])<42)covered=true;}if(covered)active++;p.setColor(covered?C("#ffd45b"):Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawOval(eyes[i][0]-31,eyes[i][1]-22,eyes[i][0]+31,eyes[i][1]+22,p);p.setColor(C("#3b4058"));c.drawCircle(eyes[i][0],eyes[i][1],covered?5:10,p);}txt(c,"Cover all three eyes at once.",getWidth()/2f,getHeight()*.81f,13,C("#536075"),Paint.Align.CENTER);if(active==3){flash("IT CAN'T SEE YOU!");next();}}
    private void finish(Canvas c){bg(c);rr(c,getWidth()*.09f,getHeight()*.24f,getWidth()*.82f,getHeight()*.45f,32,C("#fff5d6"));txt(c,"BETA COMPLETE!",getWidth()/2f,getHeight()*.34f,34,C("#3b4058"),Paint.Align.CENTER);txt(c,"6 / 6 rooms escaped",getWidth()/2f,getHeight()*.40f,18,C("#ef6b61"),Paint.Align.CENTER);txt(c,"Which room made you smile?",getWidth()/2f,getHeight()*.48f,16,C("#6d748a"),Paint.Align.CENTER);button(c,"PLAY AGAIN",getWidth()*.20f,getHeight()*.58f,getWidth()*.60f,62);}

    private int lastPointerCount=0; private final float[] lastPX=new float[10], lastPY=new float[10];
    private void cachePointers(MotionEvent e){lastPointerCount=Math.min(10,e.getPointerCount());for(int i=0;i<lastPointerCount;i++){lastPX[i]=e.getX(i);lastPY[i]=e.getY(i);}}
    @Override public boolean onTouchEvent(MotionEvent e){cachePointers(e);int a=e.getActionMasked();float x=e.getX(e.getActionIndex()),y=e.getY(e.getActionIndex());
        if(screen==0&&a==MotionEvent.ACTION_DOWN){if(y>getHeight()*.60f)next();return true;}
        if(screen==1){if(e.getPointerCount()>=2){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(a==MotionEvent.ACTION_POINTER_DOWN){pinchStart=d;keyScaleStart=keyScale;}else if(a==MotionEvent.ACTION_MOVE&&pinchStart>0){keyScale=Math.max(.30f,Math.min(1.2f,keyScaleStart*d/pinchStart));}}else{if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-keyX,y-keyY)<120){keyDrag=true;keyDX=x-keyX;keyDY=y-keyY;}if(a==MotionEvent.ACTION_MOVE&&keyDrag){keyX=x-keyDX;keyY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)keyDrag=false;}return true;}
        if(screen==2&&a==MotionEvent.ACTION_DOWN){flash("Use gravity, not your finger.");return true;}
        if(screen==3){float bx=getWidth()*.5f,by=getHeight()*.54f;if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-bx,y-by)<balloonR+35)balloonHold=System.currentTimeMillis();if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(!balloonPopped){balloonHold=0;balloonR=46;flash("Hold it...");}}return true;}
        if(screen==4&&a==MotionEvent.ACTION_DOWN){calmResets++;calmStart=System.currentTimeMillis();flash("RESET");return true;}
        if(screen==5&&a==MotionEvent.ACTION_DOWN){float[] xs={.24f,.43f,.62f,.81f};int hit=-1;for(int i=0;i<4;i++)if(Math.hypot(x-getWidth()*xs[i],y-getHeight()*.61f)<60)hit=i;if(hit>=0){if(hit==shadowTarget[shadowPos]){shadowPos++;flash("OK!");if(shadowPos==4)next();}else{shadowPos=0;flash("TRY AGAIN");}}return true;}
        if(screen==6){invalidate();return true;}
        if(screen==7&&a==MotionEvent.ACTION_DOWN){screen=0;invalidate();return true;}return true;}
}
