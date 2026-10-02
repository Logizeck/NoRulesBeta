package com.norules.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.hardware.*;
import android.media.AudioManager;
import android.view.*;
import java.util.*;

public class GameView extends View implements SensorEventListener {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SensorManager sm;
    private final Sensor accel;
    private final AudioManager audio;
    private final SharedPreferences prefs;
    private final float density, scaledDensity;

    private int screen = 0; // 0 home, 1..25 levels, 90 level select, 99 finish
    private long roomStart = System.currentTimeMillis();
    private String toast = "";
    private long toastUntil = 0;
    private int hints = 3;

    // sensors
    private float ax, ay, az;
    private float lastAx, lastAy, lastAz;
    private long lastFrame = System.nanoTime();
    private float stillScore = 0;
    private int shakeCount = 0;
    private long lastShake = 0;

    // generic touches
    private int pointerCount = 0;
    private final float[] px = new float[10], py = new float[10];
    private float downX, downY;
    private long downAt;

    // L1 pinch key
    private float keyX, keyY, keyScale = 1f, keyDX, keyDY, pinchStart, keyScaleStart;
    private boolean keyDrag;
    // L2 / L23 ball
    private float ballX, ballY, vx, vy;
    // L3
    private long balloonHold;
    private float balloonR = 48;
    private boolean balloonPopped;
    // L4
    private long calmStart;
    // L5
    private int liarChoice = -1;
    // L7
    private boolean shakeDropped;
    // L8
    private float fallY;
    // L9
    private int knockCount;
    private long lastKnock;
    // L10
    private long bothHoldStart;
    // L11
    private float stickerX, stickerY;
    private boolean stickerDrag;
    // L12
    private boolean slashDone;
    // L14
    private long stillStart;
    // L15
    private int rotatePhase;
    // L16
    private float doorScale = 1f;
    private float doorPinchStart, doorScaleStart;
    // L17
    private float bubbleX, bubbleY;
    private boolean bubbleDrag;
    // L18
    private float blueX, blueY, yellowX, yellowY;
    private int orbDrag = 0;
    private boolean greenMade;
    // L19
    private int memoryPhase;
    private int memoryPos;
    private long memoryShownAt;
    private final int[] memorySeq = {2,0,3,1};
    // L20
    private String code = "";
    // L21
    private float titleCardX, titleCardY;
    private boolean titleDrag;
    // L22
    private int oppositeChoice = -1;
    // L23
    private boolean gatePressed;
    // L24
    private boolean mirrorSolved;
    // L25 boss
    private int bossPhase;
    private boolean bossSealRevealed;
    private long bossBothStart;

    private final String[] levelNames = {
            "", "KEY PROBLEM", "GRAVITY STAR", "POP!", "ZEN DOOR", "LIAR CHIBI",
            "THREE EYES", "SHAKE IT", "UPSIDE DOWN", "KNOCK KNOCK", "TWO SEALS",
            "PEEL IT", "SAMURAI SLASH", "SHHH!", "STATUE MODE", "LEFT / RIGHT",
            "BIG DOOR", "MOVE THE BUBBLE", "COLOR FUSION", "MEMORY FACES", "COUNT IT",
            "BREAK THE UI", "OPPOSITE DAY", "TILT + HOLD", "MIRROR TRICK", "BOSS ROOM"
    };

    private final String[] hintsText = {
            "",
            "The key is not the wrong shape. It is the wrong size.",
            "Your finger is not the controller. Gravity is.",
            "A quick tap is not enough. Be persistent.",
            "Maybe doing nothing is still doing something.",
            "Ignore what he says. Look at where he is looking.",
            "One finger cannot hide three eyes.",
            "Treat the phone like a stubborn vending machine.",
            "What if the whole room had to turn over?",
            "The sound effect is also an instruction.",
            "Two seals. Same moment.",
            "That corner looks suspiciously removable.",
            "Follow the manga speed line with one clean slash.",
            "The room asks for silence. Your phone has a volume button.",
            "A statue does not move.",
            "The screen can stay portrait while the phone leans sideways.",
            "Try changing the door, not the key.",
            "Speech bubbles can block more than dialogue.",
            "Blue + yellow has a useful result.",
            "Watch first. Tap later.",
            "Count cats, stars, then swords.",
            "Not every piece of the interface has to stay in the interface.",
            "He tells you he lies. Believe that part.",
            "One hand opens the gate. The other controls gravity.",
            "A mirror reverses directions.",
            "The boss combines things you already learned."
    };

    public GameView(Context c) {
        super(c);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        sm = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        audio = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        prefs = c.getSharedPreferences("no_rules_beta", Context.MODE_PRIVATE);
        hints = prefs.getInt("hints", 3);
        setKeepScreenOn(true);
        setBackgroundColor(C("#F7E8C6"));
    }

    public void onResumeGame(){ if(accel!=null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME); lastFrame=System.nanoTime(); invalidate(); }
    public void onPauseGame(){ sm.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()!=Sensor.TYPE_ACCELEROMETER) return;
        lastAx=ax; lastAy=ay; lastAz=az;
        ax=-e.values[0]; ay=e.values[1]; az=e.values[2];
        float delta=Math.abs(ax-lastAx)+Math.abs(ay-lastAy)+Math.abs(az-lastAz);
        if(delta < 0.45f) stillScore=Math.min(10,stillScore+0.08f); else stillScore=Math.max(0,stillScore-0.35f);
        float mag=(float)Math.sqrt(ax*ax+ay*ay+az*az);
        long now=System.currentTimeMillis();
        if(Math.abs(mag-9.81f)>4.5f && now-lastShake>280){ shakeCount++; lastShake=now; }
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}

    private int C(String h){ return Color.parseColor(h); }
    private float sp(float v){ return v*scaledDensity; }
    private float dp(float v){ return v*density; }

    private void txt(Canvas c,String s,float x,float y,float size,int col,Paint.Align align){
        p.setStyle(Paint.Style.FILL); p.setTypeface(Typeface.create("sans",Typeface.BOLD)); p.setTextSize(sp(size)); p.setColor(col); p.setTextAlign(align); c.drawText(s,x,y,p);
    }
    private void mangaTxt(Canvas c,String s,float x,float y,float size,int col,Paint.Align align){
        p.setStyle(Paint.Style.FILL); p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD)); p.setTextSize(sp(size)); p.setColor(col); p.setTextAlign(align); c.drawText(s,x,y,p);
    }
    private void rr(Canvas c,float x,float y,float w,float h,float r,int col){ p.setStyle(Paint.Style.FILL); p.setColor(col); c.drawRoundRect(x,y,x+w,y+h,r,r,p); }
    private boolean in(float x,float y,float l,float t,float r,float b){ return x>=l&&x<=r&&y>=t&&y<=b; }

    private void wrap(Canvas c,String text,float cx,float top,float maxW,float size,int col){
        p.setTypeface(Typeface.create("sans",Typeface.BOLD)); p.setTextSize(sp(size)); p.setTextAlign(Paint.Align.CENTER); p.setColor(col);
        String[] words=text.split(" "); String line=""; float y=top;
        for(String w:words){ String test=line.isEmpty()?w:line+" "+w; if(p.measureText(test)>maxW && !line.isEmpty()){ c.drawText(line,cx,y,p); y+=sp(size*1.32f); line=w; } else line=test; }
        if(!line.isEmpty()) c.drawText(line,cx,y,p);
    }

    private void speedBg(Canvas c){
        c.drawColor(C("#FFF3D8"));
        p.setColor(C("#F6C95C")); p.setStrokeWidth(dp(2)); p.setStyle(Paint.Style.STROKE);
        float cx=getWidth()/2f, cy=getHeight()*.53f;
        for(int i=0;i<28;i++){ double a=i*Math.PI*2/28.0; float x1=cx+(float)Math.cos(a)*getWidth()*.28f; float y1=cy+(float)Math.sin(a)*getHeight()*.18f; float x2=cx+(float)Math.cos(a)*getWidth()*.72f; float y2=cy+(float)Math.sin(a)*getHeight()*.52f; c.drawLine(x1,y1,x2,y2,p); }
        p.setStyle(Paint.Style.FILL); p.setColor(C("#F8E6B9")); c.drawCircle(cx,cy,getWidth()*.34f,p);
    }

    private void header(Canvas c,String subtitle){
        rr(c,getWidth()*.04f,getHeight()*.025f,getWidth()*.92f,getHeight()*.13f,dp(18),Color.WHITE);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#24243A")); c.drawRoundRect(getWidth()*.04f,getHeight()*.025f,getWidth()*.96f,getHeight()*.155f,dp(18),dp(18),p);
        mangaTxt(c,"#"+screen+"  "+levelNames[screen],getWidth()*.075f,getHeight()*.077f,22,C("#24243A"),Paint.Align.LEFT);
        wrap(c,subtitle,getWidth()/2f,getHeight()*.122f,getWidth()*.79f,16,C("#55566D"));
        // hint button
        rr(c,getWidth()*.79f,getHeight()*.17f,getWidth()*.17f,dp(44),dp(14),C("#FF6C7A"));
        mangaTxt(c,"HINT "+hints,getWidth()*.875f,getHeight()*.17f+dp(29),14,Color.WHITE,Paint.Align.CENTER);
    }

    private void chibi(Canvas c,float x,float y,float scale,boolean lookLeft,boolean angry){
        p.setStyle(Paint.Style.FILL); p.setColor(C("#F7C7A7")); c.drawCircle(x,y,dp(42)*scale,p);
        p.setColor(C("#25223A")); c.drawArc(x-dp(45)*scale,y-dp(48)*scale,x+dp(45)*scale,y+dp(12)*scale,180,180,true,p);
        float eyeY=y-dp(4)*scale; float off=lookLeft?-dp(6)*scale:dp(6)*scale;
        p.setColor(Color.WHITE); c.drawOval(x-dp(25)*scale,eyeY-dp(10)*scale,x-dp(3)*scale,eyeY+dp(10)*scale,p); c.drawOval(x+dp(3)*scale,eyeY-dp(10)*scale,x+dp(25)*scale,eyeY+dp(10)*scale,p);
        p.setColor(C("#25223A")); c.drawCircle(x-dp(14)*scale+off,eyeY,dp(5)*scale,p); c.drawCircle(x+dp(14)*scale+off,eyeY,dp(5)*scale,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)*scale); p.setColor(C("#7B3145")); if(angry) c.drawLine(x-dp(14)*scale,y+dp(18)*scale,x+dp(14)*scale,y+dp(9)*scale,p); else c.drawArc(x-dp(15)*scale,y+dp(5)*scale,x+dp(15)*scale,y+dp(25)*scale,0,180,false,p);
    }

    private void speech(Canvas c,String s,float x,float y,float w,float h){
        rr(c,x,y,w,h,dp(20),Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#24243A")); c.drawRoundRect(x,y,x+w,y+h,dp(20),dp(20),p);
        Path t=new Path(); t.moveTo(x+w*.32f,y+h); t.lineTo(x+w*.43f,y+h); t.lineTo(x+w*.35f,y+h+dp(22)); t.close(); p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); c.drawPath(t,p); p.setStyle(Paint.Style.STROKE); p.setColor(C("#24243A")); c.drawPath(t,p);
        wrap(c,s,x+w/2,y+dp(31),w-dp(24),16,C("#24243A"));
    }

    private void showToast(String s){ toast=s; toastUntil=System.currentTimeMillis()+900; }
    private void useHint(){
        if(screen<1||screen>25) return;
        if(hints<=0){ showToast("No hints left — shop comes later!"); return; }
        hints--; prefs.edit().putInt("hints",hints).apply(); showToast(hintsText[screen]);
    }

    private void gotoLevel(int n){
        screen=n; roomStart=System.currentTimeMillis(); toast=""; shakeCount=0; stillScore=0; resetLevel(); invalidate();
    }
    private void solved(){
        prefs.edit().putBoolean("level_"+screen,true).putLong("time_"+screen,System.currentTimeMillis()-roomStart).apply();
        showToast("CLEAR!  ✦");
        int n=screen+1; if(n>25){ screen=99; } else { screen=n; roomStart=System.currentTimeMillis(); resetLevel(); }
    }

    private void resetLevel(){
        pointerCount=0; downAt=0;
        keyX=getWidth()*.50f; keyY=getHeight()*.66f; keyScale=1f; keyDrag=false; pinchStart=0;
        resetBall();
        balloonHold=0; balloonR=dp(48); balloonPopped=false;
        calmStart=System.currentTimeMillis(); liarChoice=-1; shakeDropped=false; fallY=getHeight()*.30f;
        knockCount=0; lastKnock=0; bothHoldStart=0;
        stickerX=getWidth()*.63f; stickerY=getHeight()*.46f; stickerDrag=false;
        slashDone=false; stillStart=0; rotatePhase=0; doorScale=1; doorPinchStart=0;
        bubbleX=getWidth()*.24f; bubbleY=getHeight()*.38f; bubbleDrag=false;
        blueX=getWidth()*.30f; blueY=getHeight()*.62f; yellowX=getWidth()*.70f; yellowY=getHeight()*.62f; orbDrag=0; greenMade=false;
        memoryPhase=0; memoryPos=0; memoryShownAt=System.currentTimeMillis(); code="";
        titleCardX=getWidth()*.50f; titleCardY=getHeight()*.24f; titleDrag=false;
        oppositeChoice=-1; gatePressed=false; mirrorSolved=false;
        bossPhase=0; bossSealRevealed=false; bossBothStart=0;
    }
    private void resetBall(){ ballX=getWidth()*.18f; ballY=getHeight()*.70f; vx=vy=0; lastFrame=System.nanoTime(); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(screen==0) drawHome(c); else if(screen==90) drawLevelSelect(c); else if(screen==99) drawFinish(c); else drawLevel(c);
        if(!toast.isEmpty() && System.currentTimeMillis()<toastUntil){ rr(c,getWidth()*.08f,getHeight()*.86f,getWidth()*.84f,dp(72),dp(18),C("#24243A")); wrap(c,toast,getWidth()/2f,getHeight()*.86f+dp(30),getWidth()*.76f,16,Color.WHITE); }
        postInvalidateOnAnimation();
    }

    private void drawHome(Canvas c){
        speedBg(c);
        mangaTxt(c,"NO RULES!",getWidth()/2f,getHeight()*.20f,44,C("#24243A"),Paint.Align.CENTER);
        mangaTxt(c,"MANGA TEST BUILD",getWidth()/2f,getHeight()*.255f,19,C("#E94C67"),Paint.Align.CENTER);
        chibi(c,getWidth()*.50f,getHeight()*.41f,1.4f,false,false);
        speech(c,"25 rooms. The phone is part of the puzzle.",getWidth()*.12f,getHeight()*.52f,getWidth()*.76f,dp(105));
        button(c,"PLAY",getWidth()*.18f,getHeight()*.70f,getWidth()*.64f,dp(66),C("#FFCA4B"));
        button(c,"LEVEL SELECT",getWidth()*.18f,getHeight()*.79f,getWidth()*.64f,dp(60),C("#7CD6C1"));
    }
    private void button(Canvas c,String s,float x,float y,float w,float h,int col){ rr(c,x,y,w,h,dp(18),col); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#24243A")); c.drawRoundRect(x,y,x+w,y+h,dp(18),dp(18),p); mangaTxt(c,s,x+w/2,y+h*.64f,18,C("#24243A"),Paint.Align.CENTER); }

    private void drawLevelSelect(Canvas c){
        c.drawColor(C("#FFF3D8")); mangaTxt(c,"TEST LEVELS",getWidth()/2f,getHeight()*.07f,28,C("#24243A"),Paint.Align.CENTER);
        int cols=5; float gap=dp(8); float cell=(getWidth()-dp(32)-gap*(cols-1))/cols; float top=getHeight()*.12f;
        for(int i=1;i<=25;i++){ int row=(i-1)/cols,col=(i-1)%cols; float x=dp(16)+col*(cell+gap), y=top+row*(cell+gap); int color=prefs.getBoolean("level_"+i,false)?C("#7CD6C1"):Color.WHITE; rr(c,x,y,cell,cell,dp(14),color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(C("#24243A")); c.drawRoundRect(x,y,x+cell,y+cell,dp(14),dp(14),p); mangaTxt(c,String.valueOf(i),x+cell/2,y+cell*.60f,18,C("#24243A"),Paint.Align.CENTER); }
        button(c,"BACK",getWidth()*.30f,getHeight()*.86f,getWidth()*.40f,dp(58),C("#FFCA4B"));
    }

    private void drawFinish(Canvas c){
        speedBg(c); mangaTxt(c,"25 / 25!",getWidth()/2f,getHeight()*.25f,42,C("#24243A"),Paint.Align.CENTER); mangaTxt(c,"TEST COMPLETE",getWidth()/2f,getHeight()*.32f,25,C("#E94C67"),Paint.Align.CENTER); chibi(c,getWidth()*.50f,getHeight()*.49f,1.5f,false,false); wrap(c,"Now we test: which rooms are fun, confusing, too easy, or impossible?",getWidth()/2f,getHeight()*.64f,getWidth()*.78f,18,C("#55566D")); button(c,"LEVEL SELECT",getWidth()*.18f,getHeight()*.79f,getWidth()*.64f,dp(62),C("#7CD6C1"));
    }

    private void drawLevel(Canvas c){
        speedBg(c);
        switch(screen){
            case 1: l1(c); break; case 2:l2(c);break; case 3:l3(c);break; case 4:l4(c);break; case 5:l5(c);break;
            case 6:l6(c);break; case 7:l7(c);break; case 8:l8(c);break; case 9:l9(c);break; case 10:l10(c);break;
            case 11:l11(c);break; case 12:l12(c);break; case 13:l13(c);break; case 14:l14(c);break; case 15:l15(c);break;
            case 16:l16(c);break; case 17:l17(c);break; case 18:l18(c);break; case 19:l19(c);break; case 20:l20(c);break;
            case 21:l21(c);break; case 22:l22(c);break; case 23:l23(c);break; case 24:l24(c);break; case 25:l25(c);break;
        }
    }

    private void door(Canvas c,float x,float y,float w,float h){ rr(c,x-w/2,y-h/2,w,h,dp(18),C("#7556C9")); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(4)); p.setColor(C("#24243A")); c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,dp(18),dp(18),p); p.setStyle(Paint.Style.FILL); p.setColor(C("#FFCA4B")); c.drawCircle(x+w*.30f,y,dp(8),p); }

    private void l1(Canvas c){ header(c,"That key is ridiculously dramatic."); door(c,getWidth()*.5f,getHeight()*.42f,getWidth()*.30f,getHeight()*.28f); c.save(); c.translate(keyX,keyY); c.scale(keyScale,keyScale); p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(16));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(C("#FFCA4B"));c.drawCircle(-dp(34),0,dp(23),p);c.drawLine(-dp(10),0,dp(64),0,p);c.drawLine(dp(64),0,dp(64),dp(19),p);p.setStrokeCap(Paint.Cap.BUTT);c.restore(); wrap(c,"Pinch + drag",getWidth()/2f,getHeight()*.82f,getWidth()*.75f,18,C("#55566D")); if(keyScale<.48f&&Math.hypot(keyX-getWidth()*.5f,keyY-getHeight()*.42f)<dp(55)) solved(); }

    private void l2(Canvas c){ header(c,"The floor is listening to gravity."); float L=getWidth()*.09f,R=getWidth()*.91f,T=getHeight()*.28f,B=getHeight()*.76f; rr(c,L,T,R-L,B-T,dp(22),Color.WHITE); long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*55*dt;vy+=ay*55*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float rad=dp(17);if(ballX<L+rad){ballX=L+rad;vx*=-.55f;}if(ballX>R-rad){ballX=R-rad;vx*=-.55f;}if(ballY<T+rad){ballY=T+rad;vy*=-.55f;}if(ballY>B-rad){ballY=B-rad;vy*=-.55f;}float tx=getWidth()*.76f,ty=getHeight()*.35f;p.setColor(C("#FFCA4B"));p.setStyle(Paint.Style.FILL);c.drawCircle(tx,ty,dp(28),p);mangaTxt(c,"★",tx,ty+sp(9),24,C("#7D5A00"),Paint.Align.CENTER);p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,rad,p); if(Math.hypot(ballX-tx,ballY-ty)<dp(26)) solved(); }

    private void l3(Canvas c){ header(c,"The key is trapped inside a stubborn balloon."); float x=getWidth()*.5f,y=getHeight()*.53f;if(balloonHold>0&&!balloonPopped){float t=Math.min(1,(System.currentTimeMillis()-balloonHold)/1500f);balloonR=dp(48+38*t);if(t>=1){balloonPopped=true;showToast("BANG!!");}} if(!balloonPopped){p.setColor(C("#FF6C9B"));p.setStyle(Paint.Style.FILL);c.drawOval(x-balloonR*.82f,y-balloonR,x+balloonR*.82f,y+balloonR,p);mangaTxt(c,"KEY",x,y+sp(7),16,C("#772942"),Paint.Align.CENTER);}else{mangaTxt(c,"🔑",x,y+sp(20),44,C("#24243A"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>700) solved();} mangaTxt(c,"PRESS...",getWidth()/2f,getHeight()*.79f,22,C("#E94C67"),Paint.Align.CENTER); }

    private void l4(Canvas c){ header(c,"The door hates needy players."); door(c,getWidth()*.5f,getHeight()*.49f,getWidth()*.38f,getHeight()*.36f); long e=System.currentTimeMillis()-calmStart; int rem=Math.max(0,7-(int)(e/1000)); mangaTxt(c,rem>0?String.valueOf(rem):"...",getWidth()/2f,getHeight()*.75f,38,C("#24243A"),Paint.Align.CENTER); if(e>=7000) solved(); }

    private void l5(Canvas c){ header(c,"This little guy says: 'LEFT! Definitely LEFT!'"); chibi(c,getWidth()*.5f,getHeight()*.39f,1.3f,false,true); speech(c,"LEFT! TRUST ME!",getWidth()*.19f,getHeight()*.50f,getWidth()*.62f,dp(92)); button(c,"LEFT",getWidth()*.10f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#FF8D7C")); button(c,"RIGHT",getWidth()*.56f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#7CD6C1")); wrap(c,"His eyes might be more honest than his mouth.",getWidth()/2f,getHeight()*.84f,getWidth()*.82f,16,C("#55566D")); if(liarChoice==1) solved(); }

    private void l6(Canvas c){ header(c,"Three eyes. Zero privacy."); float[][] eyes={{.30f,.48f},{.50f,.58f},{.70f,.48f}};int active=0;for(int i=0;i<3;i++){float ex=getWidth()*eyes[i][0],ey=getHeight()*eyes[i][1];boolean covered=false;for(int k=0;k<pointerCount;k++)if(Math.hypot(px[k]-ex,py[k]-ey)<dp(48))covered=true;if(covered)active++;p.setColor(covered?C("#FFCA4B"):Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawOval(ex-dp(42),ey-dp(26),ex+dp(42),ey+dp(26),p);p.setColor(C("#24243A"));c.drawCircle(ex,ey,covered?dp(5):dp(12),p);} mangaTxt(c,"HIDE THEM",getWidth()/2f,getHeight()*.76f,24,C("#E94C67"),Paint.Align.CENTER); if(active==3) solved(); }

    private void l7(Canvas c){ header(c,"The vending machine ate your key."); rr(c,getWidth()*.27f,getHeight()*.30f,getWidth()*.46f,getHeight()*.42f,dp(24),C("#77A7DD")); mangaTxt(c,"KEY",getWidth()*.5f,getHeight()*.42f,28,C("#FFCA4B"),Paint.Align.CENTER); mangaTxt(c,"ガタガタ!",getWidth()*.5f,getHeight()*.79f,28,C("#E94C67"),Paint.Align.CENTER); if(shakeCount>=3){shakeDropped=true;} if(shakeDropped){ mangaTxt(c,"🔑",getWidth()*.5f,getHeight()*.72f,38,C("#24243A"),Paint.Align.CENTER); if(System.currentTimeMillis()-roomStart>600) solved();} }

    private void l8(Canvas c){ header(c,"The key is stuck to the ceiling."); door(c,getWidth()*.5f,getHeight()*.70f,getWidth()*.32f,getHeight()*.23f); mangaTxt(c,"🔑",getWidth()*.5f,fallY,38,C("#24243A"),Paint.Align.CENTER); boolean inverted=ay<-6.2f; if(inverted) fallY=Math.min(getHeight()*.68f,fallY+dp(8)); if(fallY>=getHeight()*.65f) solved(); }

    private void l9(Canvas c){ header(c,"A sleeping ninja guards the door."); chibi(c,getWidth()*.5f,getHeight()*.48f,1.25f,false,false); mangaTxt(c,"Z Z Z",getWidth()*.68f,getHeight()*.36f,28,C("#6B65B5"),Paint.Align.CENTER); mangaTxt(c,"TOK  TOK  TOK",getWidth()/2f,getHeight()*.72f,26,C("#E94C67"),Paint.Align.CENTER); mangaTxt(c,knockCount+" / 3",getWidth()/2f,getHeight()*.80f,20,C("#24243A"),Paint.Align.CENTER); if(knockCount>=3) solved(); }

    private void l10(Canvas c){ header(c,"The seals only trust teamwork."); float y=getHeight()*.52f;seal(c,getWidth()*.32f,y,"A");seal(c,getWidth()*.68f,y,"B");boolean a=false,b=false;for(int i=0;i<pointerCount;i++){if(Math.hypot(px[i]-getWidth()*.32f,py[i]-y)<dp(60))a=true;if(Math.hypot(px[i]-getWidth()*.68f,py[i]-y)<dp(60))b=true;}if(a&&b){if(bothHoldStart==0)bothHoldStart=System.currentTimeMillis();if(System.currentTimeMillis()-bothHoldStart>850)solved();}else bothHoldStart=0;mangaTxt(c,"HOLD BOTH",getWidth()/2f,getHeight()*.75f,24,C("#E94C67"),Paint.Align.CENTER); }
    private void seal(Canvas c,float x,float y,String s){p.setStyle(Paint.Style.FILL);p.setColor(C("#E84D61"));c.drawCircle(x,y,dp(50),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#24243A"));c.drawCircle(x,y,dp(50),p);mangaTxt(c,s,x,y+sp(9),25,Color.WHITE,Paint.Align.CENTER);}

    private void l11(Canvas c){ header(c,"Someone put a sticker over the important part."); door(c,getWidth()*.5f,getHeight()*.50f,getWidth()*.42f,getHeight()*.38f); mangaTxt(c,"OPEN",getWidth()*.5f,getHeight()*.51f,22,Color.WHITE,Paint.Align.CENTER); rr(c,stickerX-dp(75),stickerY-dp(50),dp(150),dp(100),dp(10),C("#FFCA4B")); mangaTxt(c,"SALE!",stickerX,stickerY+sp(9),24,C("#24243A"),Paint.Align.CENTER); if(stickerX<0||stickerX>getWidth()||stickerY<0||stickerY>getHeight()) solved(); }

    private void l12(Canvas c){ header(c,"One clean samurai slash."); chibi(c,getWidth()*.28f,getHeight()*.55f,1.0f,false,false);p.setColor(C("#24243A"));p.setStrokeWidth(dp(6));p.setStyle(Paint.Style.STROKE);c.drawLine(getWidth()*.38f,getHeight()*.68f,getWidth()*.76f,getHeight()*.36f,p);mangaTxt(c,"シュッ!",getWidth()*.70f,getHeight()*.68f,28,C("#E94C67"),Paint.Align.CENTER); if(slashDone) solved(); }

    private void l13(Canvas c){ header(c,"The manga librarian demands TOTAL SILENCE."); chibi(c,getWidth()*.5f,getHeight()*.46f,1.25f,false,true); mangaTxt(c,"SHHH!!",getWidth()/2f,getHeight()*.66f,34,C("#E94C67"),Paint.Align.CENTER); int vol=audio.getStreamVolume(AudioManager.STREAM_MUSIC); int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC); wrap(c,"Media volume: "+vol+" / "+max,getWidth()/2f,getHeight()*.77f,getWidth()*.80f,18,C("#24243A")); if(vol==0) solved(); }

    private void l14(Canvas c){ header(c,"Become a statue. Seriously."); chibi(c,getWidth()*.5f,getHeight()*.48f,1.45f,false,false); if(stillScore>2.5f){if(stillStart==0)stillStart=System.currentTimeMillis();}else stillStart=0; long t=stillStart==0?0:System.currentTimeMillis()-stillStart; mangaTxt(c,(t/1000)+" / 4",getWidth()/2f,getHeight()*.76f,28,C("#E94C67"),Paint.Align.CENTER); if(t>4000) solved(); }

    private void l15(Canvas c){ header(c,"Lean LEFT... then RIGHT. Screen stays portrait."); mangaTxt(c,rotatePhase==0?"← LEFT":"RIGHT →",getWidth()/2f,getHeight()*.52f,38,C("#24243A"),Paint.Align.CENTER); if(rotatePhase==0&&ax<-6.0f)rotatePhase=1; if(rotatePhase==1&&ax>6.0f)solved(); }

    private void l16(Canvas c){ header(c,"The door is too small for your ego."); c.save();c.translate(getWidth()*.5f,getHeight()*.52f);c.scale(doorScale,doorScale);door(c,0,0,getWidth()*.22f,getHeight()*.20f);c.restore();mangaTxt(c,"MAKE IT BIG",getWidth()/2f,getHeight()*.78f,24,C("#E94C67"),Paint.Align.CENTER); if(doorScale>2.25f) solved(); }

    private void l17(Canvas c){ header(c,"Dialogue is blocking the evidence."); mangaTxt(c,"🔑",getWidth()*.50f,getHeight()*.48f,42,C("#24243A"),Paint.Align.CENTER); speech(c,"I am extremely important dialogue!",bubbleX,bubbleY,getWidth()*.56f,dp(120)); if(bubbleX>getWidth()*.78f||bubbleX+getWidth()*.56f<getWidth()*.20f||bubbleY>getHeight()*.72f) solved(); }

    private void l18(Canvas c){ header(c,"The lock wants GREEN. You only have two colors."); orb(c,blueX,blueY,C("#4F8FEA"));orb(c,yellowX,yellowY,C("#FFCA4B"));float tx=getWidth()*.5f,ty=getHeight()*.38f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(C("#4BAA78"));c.drawCircle(tx,ty,dp(42),p);if(Math.hypot(blueX-yellowX,blueY-yellowY)<dp(65)){greenMade=true;blueX=yellowX=(blueX+yellowX)/2;blueY=yellowY=(blueY+yellowY)/2;}if(greenMade){orb(c,blueX,blueY,C("#55B979"));if(Math.hypot(blueX-tx,blueY-ty)<dp(52))solved();}}
    private void orb(Canvas c,float x,float y,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawCircle(x,y,dp(42),p);p.setColor(Color.WHITE);c.drawCircle(x-dp(11),y-dp(10),dp(7),p);}

    private void l19(Canvas c){ header(c,"Watch the faces. Then repeat them."); long now=System.currentTimeMillis(); if(memoryPhase==0&&now-memoryShownAt>2400)memoryPhase=1; float[] xs={.18f,.39f,.61f,.82f}; for(int i=0;i<4;i++){float x=getWidth()*xs[i],y=getHeight()*.55f;int col=C("#F7C7A7");p.setColor(col);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,dp(34),p);mangaTxt(c,new String[]{"😠","😴","😎","😱"}[i],x,y+sp(12),28,C("#24243A"),Paint.Align.CENTER);} if(memoryPhase==0){ mangaTxt(c,""+(memorySeq[0]+1)+"  "+(memorySeq[1]+1)+"  "+(memorySeq[2]+1)+"  "+(memorySeq[3]+1),getWidth()/2f,getHeight()*.72f,28,C("#E94C67"),Paint.Align.CENTER);}else mangaTxt(c,"YOUR TURN  "+memoryPos+"/4",getWidth()/2f,getHeight()*.72f,22,C("#E94C67"),Paint.Align.CENTER); }

    private void l20(Canvas c){ header(c,"Cats, stars, swords. In that order."); mangaTxt(c,"🐱 🐱 🐱",getWidth()/2f,getHeight()*.30f,28,C("#24243A"),Paint.Align.CENTER);mangaTxt(c,"★ ★ ★ ★",getWidth()/2f,getHeight()*.38f,28,C("#FFB300"),Paint.Align.CENTER);mangaTxt(c,"⚔ ⚔",getWidth()/2f,getHeight()*.46f,28,C("#24243A"),Paint.Align.CENTER); drawKeypad(c);mangaTxt(c,code.isEmpty()?"_ _ _":code,getWidth()/2f,getHeight()*.56f,28,C("#E94C67"),Paint.Align.CENTER);if(code.equals("342"))solved();if(code.length()>=3&&!code.equals("342")){showToast("NOPE");code="";}}
    private void drawKeypad(Canvas c){float top=getHeight()*.62f;int n=1;for(int r=0;r<3;r++)for(int col=0;col<3;col++){float x=getWidth()*.22f+col*getWidth()*.28f,y=top+r*dp(62);rr(c,x-dp(28),y-dp(24),dp(56),dp(48),dp(12),Color.WHITE);mangaTxt(c,String.valueOf(n++),x,y+sp(7),18,C("#24243A"),Paint.Align.CENTER);}}

    private void l21(Canvas c){ header(c,"The interface thinks it is untouchable."); door(c,getWidth()*.5f,getHeight()*.62f,getWidth()*.42f,getHeight()*.30f);rr(c,titleCardX-getWidth()*.20f,titleCardY-dp(38),getWidth()*.40f,dp(76),dp(14),C("#FFCA4B"));mangaTxt(c,"ROOM 21",titleCardX,titleCardY+sp(8),20,C("#24243A"),Paint.Align.CENTER);wrap(c,"Drag the label somewhere useful.",getWidth()/2f,getHeight()*.82f,getWidth()*.80f,18,C("#55566D")); if(titleCardY>getHeight()*.58f&&Math.abs(titleCardX-getWidth()*.5f)<getWidth()*.18f)solved(); }

    private void l22(Canvas c){ header(c,"He says: 'I ALWAYS LIE. Choose LEFT.'");chibi(c,getWidth()*.5f,getHeight()*.42f,1.25f,true,true);button(c,"LEFT",getWidth()*.10f,getHeight()*.68f,getWidth()*.34f,dp(64),C("#FF8D7C"));button(c,"RIGHT",getWidth()*.56f,getHeight()*.68f,getWidth()*.34f,dp(64),C("#7CD6C1"));if(oppositeChoice==1)solved(); }

    private void l23(Canvas c){ header(c,"One finger opens the gate. Gravity does the rest.");float L=getWidth()*.08f,R=getWidth()*.92f,T=getHeight()*.30f,B=getHeight()*.74f;rr(c,L,T,R-L,B-T,dp(20),Color.WHITE);float gateX=getWidth()*.55f;p.setColor(C("#E94C67"));p.setStyle(Paint.Style.FILL);if(!gatePressed)c.drawRect(gateX,T,gateX+dp(14),B,p);button(c,"HOLD",getWidth()*.08f,getHeight()*.78f,getWidth()*.28f,dp(58),C("#FFCA4B"));long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*50*dt;vy+=ay*50*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float r=dp(16);if(ballX<L+r)ballX=L+r;if(ballX>R-r)ballX=R-r;if(ballY<T+r)ballY=T+r;if(ballY>B-r)ballY=B-r;if(!gatePressed&&ballX>gateX-r&&ballX<gateX+dp(26)&&ballY>T&&ballY<B){ballX=gateX-r;vx*=-.5f;}p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,r,p);float tx=getWidth()*.82f,ty=getHeight()*.38f;p.setColor(C("#55B979"));c.drawCircle(tx,ty,dp(25),p);if(Math.hypot(ballX-tx,ballY-ty)<dp(28))solved(); }

    private void l24(Canvas c){ header(c,"The mirror shows →. What does reality do?");rr(c,getWidth()*.17f,getHeight()*.32f,getWidth()*.66f,getHeight()*.26f,dp(20),C("#DCEBFA"));mangaTxt(c,"→",getWidth()/2f,getHeight()*.50f,54,C("#24243A"),Paint.Align.CENTER);mangaTxt(c,"MIRROR",getWidth()/2f,getHeight()*.66f,22,C("#6B65B5"),Paint.Align.CENTER);if(mirrorSolved)solved(); }

    private void l25(Canvas c){ header(c,"FINAL BOSS: three lessons, one room."); if(bossPhase==0){rr(c,getWidth()*.27f,getHeight()*.34f,getWidth()*.46f,getHeight()*.30f,dp(20),C("#77A7DD"));mangaTxt(c,"SHAKE",getWidth()/2f,getHeight()*.52f,30,C("#24243A"),Paint.Align.CENTER);if(shakeCount>=3){bossSealRevealed=true;bossPhase=1;showToast("SEALS REVEALED!");}} else if(bossPhase==1){float y=getHeight()*.50f;seal(c,getWidth()*.34f,y,"1");seal(c,getWidth()*.66f,y,"2");boolean a=false,b=false;for(int i=0;i<pointerCount;i++){if(Math.hypot(px[i]-getWidth()*.34f,py[i]-y)<dp(60))a=true;if(Math.hypot(px[i]-getWidth()*.66f,py[i]-y)<dp(60))b=true;}if(a&&b){if(bossBothStart==0)bossBothStart=System.currentTimeMillis();if(System.currentTimeMillis()-bossBothStart>800){bossPhase=2;resetBall();showToast("FINAL ORB!");}}else bossBothStart=0;} else {float tx=getWidth()*.5f,ty=getHeight()*.40f;long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*55*dt;vy+=ay*55*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;ballX=Math.max(dp(20),Math.min(getWidth()-dp(20),ballX));ballY=Math.max(getHeight()*.28f,Math.min(getHeight()*.78f,ballY));p.setColor(C("#FFCA4B"));c.drawCircle(tx,ty,dp(32),p);p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,dp(18),p);if(Math.hypot(ballX-tx,ballY-ty)<dp(30))solved();} mangaTxt(c,"PHASE "+(bossPhase+1)+" / 3",getWidth()/2f,getHeight()*.78f,22,C("#E94C67"),Paint.Align.CENTER); }

    private void cache(MotionEvent e){ pointerCount=Math.min(10,e.getPointerCount()); for(int i=0;i<pointerCount;i++){px[i]=e.getX(i);py[i]=e.getY(i);} }

    @Override public boolean onTouchEvent(MotionEvent e){
        cache(e); int a=e.getActionMasked(); int idx=e.getActionIndex(); float x=e.getX(idx),y=e.getY(idx);
        if(a==MotionEvent.ACTION_DOWN){downX=x;downY=y;downAt=System.currentTimeMillis();}
        if(screen==0){ if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.67f,getWidth()*.82f,getHeight()*.78f))gotoLevel(1); else if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.78f,getWidth()*.82f,getHeight()*.88f))screen=90; invalidate(); return true; }
        if(screen==90){ if(a==MotionEvent.ACTION_DOWN){int cols=5;float gap=dp(8),cell=(getWidth()-dp(32)-gap*4)/5,top=getHeight()*.12f;for(int i=1;i<=25;i++){int row=(i-1)/5,col=(i-1)%5;float lx=dp(16)+col*(cell+gap),ty=top+row*(cell+gap);if(in(x,y,lx,ty,lx+cell,ty+cell)){gotoLevel(i);return true;}}if(y>getHeight()*.84f)screen=0;}invalidate();return true; }
        if(screen==99){ if(a==MotionEvent.ACTION_DOWN){screen=90;invalidate();}return true; }
        if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.77f,getHeight()*.15f,getWidth(),getHeight()*.24f)){useHint();return true;}

        switch(screen){
            case 1:
                if(e.getPointerCount()>=2){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(a==MotionEvent.ACTION_POINTER_DOWN){pinchStart=d;keyScaleStart=keyScale;}else if(a==MotionEvent.ACTION_MOVE&&pinchStart>0)keyScale=Math.max(.28f,Math.min(1.25f,keyScaleStart*d/pinchStart));}else{if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-keyX,y-keyY)<dp(130)){keyDrag=true;keyDX=x-keyX;keyDY=y-keyY;}if(a==MotionEvent.ACTION_MOVE&&keyDrag){keyX=x-keyDX;keyY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)keyDrag=false;}break;
            case 2: if(a==MotionEvent.ACTION_DOWN)showToast("Hands off. Tilt!");break;
            case 3: if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-getWidth()*.5f,y-getHeight()*.53f)<dp(90))balloonHold=System.currentTimeMillis();if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(!balloonPopped){balloonHold=0;balloonR=dp(48);showToast("Hold longer!");}}break;
            case 4: if(a==MotionEvent.ACTION_DOWN){calmStart=System.currentTimeMillis();showToast("RESET! Stop touching.");}break;
            case 5: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.66f){liarChoice=x>getWidth()*.5f?1:0;if(liarChoice==0)showToast("He fooled you 😏");}break;
            case 6: break;
            case 7: break;
            case 8: break;
            case 9: if(a==MotionEvent.ACTION_DOWN){long now=System.currentTimeMillis();if(lastKnock==0||now-lastKnock<700){knockCount++;lastKnock=now;}else{knockCount=1;lastKnock=now;}}break;
            case 10: break;
            case 11: if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-stickerX,y-stickerY)<dp(100)){stickerDrag=true;keyDX=x-stickerX;keyDY=y-stickerY;}if(a==MotionEvent.ACTION_MOVE&&stickerDrag){stickerX=x-keyDX;stickerY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)stickerDrag=false;break;
            case 12: if(a==MotionEvent.ACTION_UP){float dx=x-downX,dy=y-downY;if(dx>getWidth()*.28f&&dy<-getHeight()*.18f)slashDone=true;}break;
            case 13: break;
            case 14: if(a==MotionEvent.ACTION_DOWN)showToast("Even your finger moved it!");break;
            case 15: break;
            case 16: if(e.getPointerCount()>=2){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(a==MotionEvent.ACTION_POINTER_DOWN){doorPinchStart=d;doorScaleStart=doorScale;}else if(a==MotionEvent.ACTION_MOVE&&doorPinchStart>0)doorScale=Math.max(.5f,Math.min(3f,doorScaleStart*d/doorPinchStart));}break;
            case 17: float bw=getWidth()*.56f,bh=dp(120);if(a==MotionEvent.ACTION_DOWN&&in(x,y,bubbleX,bubbleY,bubbleX+bw,bubbleY+bh)){bubbleDrag=true;keyDX=x-bubbleX;keyDY=y-bubbleY;}if(a==MotionEvent.ACTION_MOVE&&bubbleDrag){bubbleX=x-keyDX;bubbleY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)bubbleDrag=false;break;
            case 18: if(a==MotionEvent.ACTION_DOWN){if(Math.hypot(x-blueX,y-blueY)<dp(60))orbDrag=1;else if(Math.hypot(x-yellowX,y-yellowY)<dp(60))orbDrag=2;}if(a==MotionEvent.ACTION_MOVE){if(orbDrag==1){blueX=x;blueY=y;}if(orbDrag==2){yellowX=x;yellowY=y;}}if(a==MotionEvent.ACTION_UP)orbDrag=0;break;
            case 19: if(memoryPhase==1&&a==MotionEvent.ACTION_DOWN){float[] xs={.18f,.39f,.61f,.82f};int hit=-1;for(int i=0;i<4;i++)if(Math.hypot(x-getWidth()*xs[i],y-getHeight()*.55f)<dp(48))hit=i;if(hit>=0){if(hit==memorySeq[memoryPos]){memoryPos++;if(memoryPos==4)solved();}else{showToast("Wrong face!");memoryPos=0;}}}break;
            case 20: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.58f){float top=getHeight()*.62f;for(int r=0;r<3;r++)for(int col=0;col<3;col++){float kx=getWidth()*.22f+col*getWidth()*.28f,ky=top+r*dp(62);if(Math.hypot(x-kx,y-ky)<dp(34)&&code.length()<3)code+=String.valueOf(r*3+col+1);}}break;
            case 21: if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-titleCardX,y-titleCardY)<dp(110)){titleDrag=true;keyDX=x-titleCardX;keyDY=y-titleCardY;}if(a==MotionEvent.ACTION_MOVE&&titleDrag){titleCardX=x-keyDX;titleCardY=y-keyDY;}if(a==MotionEvent.ACTION_UP)titleDrag=false;break;
            case 22: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.64f){oppositeChoice=x>getWidth()*.5f?1:0;if(oppositeChoice==0)showToast("He said he lies!");}break;
            case 23: gatePressed=false;for(int i=0;i<pointerCount;i++)if(in(px[i],py[i],getWidth()*.06f,getHeight()*.75f,getWidth()*.40f,getHeight()*.88f))gatePressed=true;break;
            case 24: if(a==MotionEvent.ACTION_UP){float dx=x-downX;if(dx<-getWidth()*.25f)mirrorSolved=true;else if(Math.abs(dx)>getWidth()*.20f)showToast("Mirror it.");}break;
            case 25: break;
        }
        invalidate(); return true;
    }
}
