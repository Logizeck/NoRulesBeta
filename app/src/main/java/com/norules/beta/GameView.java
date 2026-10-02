package com.norules.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.hardware.*;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import java.util.*;

public class GameView extends View implements SensorEventListener {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SensorManager sm;
    private final Sensor accel;
    private final AudioManager audio;
    private final SharedPreferences prefs;
    private final float density, scaledDensity;

    private int screen = 0; // 0 home, 1..35 levels, 90 level select, 99 finish
    private long roomStart = System.currentTimeMillis();
    private String toast = "";
    private long toastUntil = 0;
    private int hints = 3;
    private boolean hintOpen = false;
    private static final int TOTAL_LEVELS = 35;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
    // L21 - Snowman face
    private float carrotX, carrotY, stone1X, stone1Y, stone2X, stone2Y;
    private int snowDrag = 0; // 1 carrot, 2/3 stones
    private boolean snowEye1, snowEye2, snowNose, snowSmile, drawingSmile;
    private final ArrayList<PointF> snowSmilePoints = new ArrayList<>();
    private int snowEasterPhase = 0; // 0 normal, 1 warning/blush, 2 melting
    private long snowEasterAt = 0;
    // L22
    private int oppositeChoice = -1;
    // L23
    private boolean gatePressed;
    // L24
    private boolean screenshotCaptured;
    // L26 - rhythm
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
    private boolean rhythmPlaying=false, rhythmReady=false;
    private final ArrayList<Long> rhythmTaps = new ArrayList<>();
    private final int[] rhythmIntervals = {360, 360, 620};
    // L27 - selfie
    private Bitmap selfieBitmap;
    private long selfieCapturedAt=0;
    private boolean selfieRequested=false;

    // visual asset + hidden surprise state
    private Bitmap snowmanArt;
    private int homeSecretTaps=0; private long homeSecretUntil=0;
    private int balloonSecretTaps=0; private long balloonSecretUntil=0;
    private long ninjaSecretUntil=0;

    // L28-L35
    private int deskMask=0; private float deskMugX,deskMugY; private boolean deskMugDrag=false; private long deskBookHold=0; private boolean deskDrawerOpen=false;
    private int toyMask=0; private float toyBallX,toyBallY; private boolean toyBallDrag=false; private int toyBearTaps=0; private int toyShakeStart=0;
    private int kitchenMask=0; private float appleX,appleY; private boolean appleDrag=false; private int toasterTaps=0; private float jarTwist=0,lastMoveX=0;
    private int vaultMask=0; private int vaultShakeStart=0; private long vaultHoldStart=0; private long vaultTiltStart=0;
    private final int[] soundSeq={0,2,1,0}; private final ArrayList<Integer> soundTaps=new ArrayList<>(); private boolean soundPlaying=false,soundReady=false;
    private final int[] logicTarget={0,2,3,1}; private int logicPos=0;
    private float rubProgress=0; private boolean rubbing=false; private String revealCode="";
    private int forgeMask=0; private int forgeShakeStart=0; private int forgeGemTaps=0; private float forgePanelX; private boolean forgePanelDrag=false; private long forgeTwoFingerStart=0; private float forgeKeyX,forgeKeyY; private boolean forgeKeyDrag=false;

    // L25 boss
    private int bossPhase;
    private boolean bossSealRevealed;
    private long bossBothStart;

    private final String[] levelNames = {
            "", "KEY PROBLEM", "GRAVITY STAR", "POP!", "ZEN DOOR", "LIAR CHIBI",
            "THREE EYES", "SHAKE IT", "UPSIDE DOWN", "KNOCK KNOCK", "TWO SEALS",
            "PEEL IT", "SAMURAI SLASH", "SHHH!", "STATUE MODE", "LEFT / RIGHT",
            "BIG DOOR", "MOVE THE BUBBLE", "COLOR FUSION", "MEMORY FACES", "COUNT IT",
            "SNOWMAN FACE", "WRONG DOOR", "TILT + HOLD", "SCREENSHOT!", "BOSS ROOM",
            "BEAT COPY", "SELFIE TROUBLE", "MESSY DESK", "TOY BOX", "KITCHEN CHAOS", "SENSOR VAULT",
            "SOUND SEQUENCE", "ODD LOGIC", "RUB IT OUT", "MASTER KEY FORGE"
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
            "Build his face: two stone eyes, carrot nose, then draw a curved smile with your finger.",
            "He always chooses the wrong door. Do the opposite of his choice.",
            "One hand opens the gate. The other controls gravity.",
            "Do not touch the key. Capture this moment with the phone itself.",
            "The boss combines things you already learned.",
            "Listen to the spacing between the four tones, then reproduce the same rhythm on the red button.",
            "The guard wants proof that you can look ridiculous. The camera may be more useful than the door.",
            "Three pieces. Move the mug, open the drawer, and hold the book.",
            "Move the ball, double-tap the bear, and shake the robot loose.",
            "The apple moves, the toaster reacts to persistence, and the jar lid twists.",
            "Each compartment wants a different phone action: shake, hold, and tilt.",
            "Listen to the four pitches, then repeat them using LOW, MID, and HIGH.",
            "Odd shapes low-to-high first. Then even shapes high-to-low.",
            "Rub the grime away until the hidden code is readable, then enter it.",
            "Four fragments, four lessons: shake, multitouch, persistence, and moving the blocker."
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
        snowmanArt = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("snowman_3d", "drawable", c.getPackageName()));
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
        p.setStyle(Paint.Style.FILL); p.setStrokeWidth(dp(1)); p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); p.setTextSize(sp(Math.max(size,20))); p.setColor(col); p.setTextAlign(align); c.drawText(s,x,y,p);
    }
    private void mangaTxt(Canvas c,String s,float x,float y,float size,int col,Paint.Align align){
        p.setStyle(Paint.Style.FILL); p.setStrokeWidth(dp(1)); p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); p.setTextSize(sp(Math.max(size,21))); p.setColor(col); p.setTextAlign(align); c.drawText(s,x,y,p);
    }
    private void rr(Canvas c,float x,float y,float w,float h,float r,int col){ p.setStyle(Paint.Style.FILL); p.setColor(col); c.drawRoundRect(x,y,x+w,y+h,r,r,p); }
    private boolean in(float x,float y,float l,float t,float r,float b){ return x>=l&&x<=r&&y>=t&&y<=b; }

    private void wrap(Canvas c,String text,float cx,float top,float maxW,float size,int col){
        p.setStyle(Paint.Style.FILL); p.setStrokeWidth(dp(1)); p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); float readable=Math.max(size,20); p.setTextSize(sp(readable)); p.setTextAlign(Paint.Align.CENTER); p.setColor(col);
        String[] words=text.split(" "); String line=""; float y=top;
        for(String w:words){ String test=line.isEmpty()?w:line+" "+w; if(p.measureText(test)>maxW && !line.isEmpty()){ c.drawText(line,cx,y,p); y+=dp(readable*1.35f); line=w; } else line=test; }
        if(!line.isEmpty()) c.drawText(line,cx,y,p);
    }

    private void instruction(Canvas c,String text){
        float x=getWidth()*.10f, y=getHeight()*.785f, w=getWidth()*.80f, h=dp(64);
        rr(c,x,y,w,h,dp(16),Color.WHITE);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(C("#24243A"));
        c.drawRoundRect(x,y,x+w,y+h,dp(16),dp(16),p);
        wrap(c,text,getWidth()/2f,y+dp(38),w-dp(24),19,C("#343548"));
    }

    private void speedBg(Canvas c){
        c.drawColor(C("#FFF3D8"));
        p.setColor(C("#F6C95C")); p.setStrokeWidth(dp(2)); p.setStyle(Paint.Style.STROKE);
        float cx=getWidth()/2f, cy=getHeight()*.53f;
        for(int i=0;i<28;i++){ double a=i*Math.PI*2/28.0; float x1=cx+(float)Math.cos(a)*getWidth()*.28f; float y1=cy+(float)Math.sin(a)*getHeight()*.18f; float x2=cx+(float)Math.cos(a)*getWidth()*.72f; float y2=cy+(float)Math.sin(a)*getHeight()*.52f; c.drawLine(x1,y1,x2,y2,p); }
        p.setStyle(Paint.Style.FILL); p.setColor(C("#F8E6B9")); c.drawCircle(cx,cy,getWidth()*.34f,p);
    }

    private void header(Canvas c,String subtitle){
        rr(c,getWidth()*.035f,getHeight()*.018f,getWidth()*.93f,getHeight()*.155f,dp(18),Color.WHITE);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#24243A"));
        c.drawRoundRect(getWidth()*.035f,getHeight()*.018f,getWidth()*.965f,getHeight()*.173f,dp(18),dp(18),p);
        mangaTxt(c,"#"+screen+"  "+levelNames[screen],getWidth()/2f,getHeight()*.067f,23,C("#24243A"),Paint.Align.CENTER);
        wrap(c,subtitle,getWidth()/2f,getHeight()*.112f,getWidth()*.82f,20,C("#3F4156"));
        button(c,"MENU",getWidth()*.045f,getHeight()*.185f,getWidth()*.22f,dp(48),C("#7CD6C1"));
        button(c,"HINT "+hints,getWidth()*.735f,getHeight()*.185f,getWidth()*.22f,dp(48),C("#FF7A89"));
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
        if(screen<1||screen>TOTAL_LEVELS) return;
        if(hintOpen) return;
        if(hints<=0){ showToast("No hints left — shop comes later!"); return; }
        hints--; prefs.edit().putInt("hints",hints).apply(); hintOpen=true; invalidate();
    }

    private void drawHintOverlay(Canvas c){
        if(!hintOpen || screen<1 || screen>TOTAL_LEVELS) return;
        p.setColor(Color.argb(165,20,20,35)); p.setStyle(Paint.Style.FILL); c.drawRect(0,0,getWidth(),getHeight(),p);
        float x=getWidth()*.07f, y=getHeight()*.24f, w=getWidth()*.86f, h=getHeight()*.38f;
        rr(c,x,y,w,h,dp(24),Color.WHITE);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(4)); p.setColor(C("#24243A")); c.drawRoundRect(x,y,x+w,y+h,dp(24),dp(24),p);
        mangaTxt(c,"HINT",x+dp(24),y+dp(48),24,C("#E94C67"),Paint.Align.LEFT);
        wrap(c,hintsText[screen],x+w/2,y+dp(100),w-dp(48),21,C("#24243A"));
        rr(c,x+w-dp(62),y+dp(16),dp(46),dp(46),dp(14),C("#FF7A89"));
        mangaTxt(c,"×",x+w-dp(39),y+dp(49),26,Color.WHITE,Paint.Align.CENTER);
        button(c,"CLOSE",x+w*.25f,y+h-dp(72),w*.50f,dp(52),C("#FFCA4B"));
    }

    private void gotoLevel(int n){
        screen=n; roomStart=System.currentTimeMillis(); toast=""; hintOpen=false; shakeCount=0; stillScore=0; resetLevel(); ((MainActivity)getContext()).syncMusicForScreen(screen); invalidate();
    }
    private void solved(){
        prefs.edit().putBoolean("level_"+screen,true).putLong("time_"+screen,System.currentTimeMillis()-roomStart).apply();
        showToast("CLEAR!  ✦");
        int n=screen+1; if(n>TOTAL_LEVELS){ screen=99; } else { screen=n; roomStart=System.currentTimeMillis(); resetLevel(); } ((MainActivity)getContext()).syncMusicForScreen(screen);
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
        carrotX=getWidth()*.20f; carrotY=getHeight()*.77f; stone1X=getWidth()*.43f; stone1Y=getHeight()*.80f; stone2X=getWidth()*.58f; stone2Y=getHeight()*.80f;
        snowDrag=0; snowEye1=false; snowEye2=false; snowNose=false; snowSmile=false; drawingSmile=false; snowSmilePoints.clear(); snowEasterPhase=0; snowEasterAt=0;
        oppositeChoice=-1; gatePressed=false; screenshotCaptured=false;
        bossPhase=0; bossSealRevealed=false; bossBothStart=0;
        rhythmPlaying=false; rhythmReady=false; rhythmTaps.clear(); selfieBitmap=null; selfieCapturedAt=0; selfieRequested=false;
        deskMask=0; deskMugX=getWidth()*.63f; deskMugY=getHeight()*.55f; deskMugDrag=false; deskBookHold=0; deskDrawerOpen=false;
        toyMask=0; toyBallX=getWidth()*.70f; toyBallY=getHeight()*.60f; toyBallDrag=false; toyBearTaps=0; toyShakeStart=shakeCount;
        kitchenMask=0; appleX=getWidth()*.30f; appleY=getHeight()*.61f; appleDrag=false; toasterTaps=0; jarTwist=0; lastMoveX=0;
        vaultMask=0; vaultShakeStart=shakeCount; vaultHoldStart=0; vaultTiltStart=0;
        soundTaps.clear(); soundPlaying=false; soundReady=false; logicPos=0; rubProgress=0; rubbing=false; revealCode="";
        forgeMask=0; forgeShakeStart=shakeCount; forgeGemTaps=0; forgePanelX=getWidth()*.56f; forgePanelDrag=false; forgeTwoFingerStart=0; forgeKeyX=getWidth()*.5f; forgeKeyY=getHeight()*.70f; forgeKeyDrag=false;
    }
    private void resetBall(){ ballX=getWidth()*.18f; ballY=getHeight()*.70f; vx=vy=0; lastFrame=System.nanoTime(); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(screen==0) drawHome(c); else if(screen==90) drawLevelSelect(c); else if(screen==99) drawFinish(c); else drawLevel(c);
        drawHintOverlay(c);
        if(!toast.isEmpty() && System.currentTimeMillis()<toastUntil){ rr(c,getWidth()*.08f,getHeight()*.86f,getWidth()*.84f,dp(72),dp(18),C("#24243A")); wrap(c,toast,getWidth()/2f,getHeight()*.86f+dp(30),getWidth()*.76f,16,Color.WHITE); }
        postInvalidateOnAnimation();
    }

    private void drawHome(Canvas c){
        speedBg(c);
        if(System.currentTimeMillis()<homeSecretUntil){
            c.save(); float wig=(float)Math.sin(System.currentTimeMillis()/55.0)*dp(7); c.translate(wig,0); mangaTxt(c,"NO  RUL—HEY!",getWidth()/2f,getHeight()*.20f,39,C("#E94C67"),Paint.Align.CENTER); c.restore();
        } else mangaTxt(c,"NO RULES!",getWidth()/2f,getHeight()*.20f,44,C("#24243A"),Paint.Align.CENTER);
        mangaTxt(c,"MANGA TEST BUILD",getWidth()/2f,getHeight()*.255f,19,C("#E94C67"),Paint.Align.CENTER);
        chibi(c,getWidth()*.50f,getHeight()*.41f,1.4f,false,false);
        speech(c,"THE PHONE IS PART OF THE PUZZLE.",getWidth()*.12f,getHeight()*.52f,getWidth()*.76f,dp(105));
        button(c,"PLAY",getWidth()*.18f,getHeight()*.70f,getWidth()*.64f,dp(66),C("#FFCA4B"));
        button(c,"LEVEL SELECT",getWidth()*.18f,getHeight()*.79f,getWidth()*.64f,dp(60),C("#7CD6C1"));
    }
    private void button(Canvas c,String s,float x,float y,float w,float h,int col){ rr(c,x,y,w,h,dp(18),col); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#24243A")); c.drawRoundRect(x,y,x+w,y+h,dp(18),dp(18),p); mangaTxt(c,s,x+w/2,y+h*.64f,20,C("#24243A"),Paint.Align.CENTER); }

    private void drawLevelSelect(Canvas c){
        c.drawColor(C("#FFF3D8")); mangaTxt(c,"TEST LEVELS",getWidth()/2f,getHeight()*.07f,28,C("#24243A"),Paint.Align.CENTER);
        int cols=5; float gap=dp(8); float cell=(getWidth()-dp(32)-gap*(cols-1))/cols; float top=getHeight()*.12f;
        for(int i=1;i<=TOTAL_LEVELS;i++){ int row=(i-1)/cols,col=(i-1)%cols; float x=dp(16)+col*(cell+gap), y=top+row*(cell+gap); int color=prefs.getBoolean("level_"+i,false)?C("#7CD6C1"):Color.WHITE; rr(c,x,y,cell,cell,dp(14),color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(C("#24243A")); c.drawRoundRect(x,y,x+cell,y+cell,dp(14),dp(14),p); mangaTxt(c,String.valueOf(i),x+cell/2,y+cell*.60f,18,C("#24243A"),Paint.Align.CENTER); }
        button(c,"BACK",getWidth()*.30f,getHeight()*.86f,getWidth()*.40f,dp(58),C("#FFCA4B"));
    }

    private void drawFinish(Canvas c){
        speedBg(c); mangaTxt(c,"TEST COMPLETE!",getWidth()/2f,getHeight()*.25f,40,C("#24243A"),Paint.Align.CENTER); mangaTxt(c,"YOU SURVIVED THIS BUILD",getWidth()/2f,getHeight()*.32f,23,C("#E94C67"),Paint.Align.CENTER); chibi(c,getWidth()*.50f,getHeight()*.49f,1.5f,false,false); wrap(c,"Now we test: which rooms are fun, confusing, too easy, or impossible?",getWidth()/2f,getHeight()*.64f,getWidth()*.78f,18,C("#55566D")); button(c,"LEVEL SELECT",getWidth()*.18f,getHeight()*.79f,getWidth()*.64f,dp(62),C("#7CD6C1"));
    }

    private void drawLevel(Canvas c){
        speedBg(c);
        switch(screen){
            case 1: l1(c); break; case 2:l2(c);break; case 3:l3(c);break; case 4:l4(c);break; case 5:l5(c);break;
            case 6:l6(c);break; case 7:l7(c);break; case 8:l8(c);break; case 9:l9(c);break; case 10:l10(c);break;
            case 11:l11(c);break; case 12:l12(c);break; case 13:l13(c);break; case 14:l14(c);break; case 15:l15(c);break;
            case 16:l16(c);break; case 17:l17(c);break; case 18:l18(c);break; case 19:l19(c);break; case 20:l20(c);break;
            case 21:l21(c);break; case 22:l22(c);break; case 23:l23(c);break; case 24:l24(c);break; case 25:l25(c);break;
            case 26:l26(c);break; case 27:l27(c);break; case 28:l28(c);break; case 29:l29(c);break; case 30:l30(c);break;
            case 31:l31(c);break; case 32:l32(c);break; case 33:l33(c);break; case 34:l34(c);break; case 35:l35(c);break;
        }
    }

    private void door(Canvas c,float x,float y,float w,float h){ rr(c,x-w/2,y-h/2,w,h,dp(18),C("#7556C9")); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(4)); p.setColor(C("#24243A")); c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,dp(18),dp(18),p); p.setStyle(Paint.Style.FILL); p.setColor(C("#FFCA4B")); c.drawCircle(x+w*.30f,y,dp(8),p); }

    private void l1(Canvas c){ header(c,"That key is ridiculously dramatic."); door(c,getWidth()*.5f,getHeight()*.42f,getWidth()*.30f,getHeight()*.28f); c.save(); c.translate(keyX,keyY); c.scale(keyScale,keyScale); p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(16));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(C("#FFCA4B"));c.drawCircle(-dp(34),0,dp(23),p);c.drawLine(-dp(10),0,dp(64),0,p);c.drawLine(dp(64),0,dp(64),dp(19),p);p.setStrokeCap(Paint.Cap.BUTT);c.restore(); instruction(c,"Pinch smaller, then drag the key into the door."); if(keyScale<.48f&&Math.hypot(keyX-getWidth()*.5f,keyY-getHeight()*.42f)<dp(55)) solved(); }

    private void l2(Canvas c){ header(c,"The floor is listening to gravity."); float L=getWidth()*.09f,R=getWidth()*.91f,T=getHeight()*.28f,B=getHeight()*.76f; rr(c,L,T,R-L,B-T,dp(22),Color.WHITE); long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*55*dt;vy+=ay*55*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float rad=dp(17);if(ballX<L+rad){ballX=L+rad;vx*=-.55f;}if(ballX>R-rad){ballX=R-rad;vx*=-.55f;}if(ballY<T+rad){ballY=T+rad;vy*=-.55f;}if(ballY>B-rad){ballY=B-rad;vy*=-.55f;}float tx=getWidth()*.76f,ty=getHeight()*.35f;p.setColor(C("#FFCA4B"));p.setStyle(Paint.Style.FILL);c.drawCircle(tx,ty,dp(28),p);mangaTxt(c,"★",tx,ty+sp(9),24,C("#7D5A00"),Paint.Align.CENTER);p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,rad,p); if(Math.hypot(ballX-tx,ballY-ty)<dp(26)) solved(); }

    private void l3(Canvas c){ header(c,"The key is trapped inside a stubborn balloon."); float x=getWidth()*.5f,y=getHeight()*.53f;if(balloonHold>0&&!balloonPopped){float t=Math.min(1,(System.currentTimeMillis()-balloonHold)/1500f);balloonR=dp(48+38*t);if(t>=1){balloonPopped=true;showToast("BANG!!");}} if(!balloonPopped){p.setColor(C("#FF6C9B"));p.setStyle(Paint.Style.FILL);c.drawOval(x-balloonR*.82f,y-balloonR,x+balloonR*.82f,y+balloonR,p);mangaTxt(c,"KEY",x,y+sp(7),16,C("#772942"),Paint.Align.CENTER);if(System.currentTimeMillis()<balloonSecretUntil)mangaTxt(c,"ಠ_ಠ",x,y-sp(20),22,C("#24243A"),Paint.Align.CENTER);}else{mangaTxt(c,"🔑",x,y+sp(20),44,C("#24243A"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>700) solved();} mangaTxt(c,"PRESS...",getWidth()/2f,getHeight()*.79f,22,C("#E94C67"),Paint.Align.CENTER); }

    private void l4(Canvas c){ header(c,"The door hates needy players."); door(c,getWidth()*.5f,getHeight()*.49f,getWidth()*.38f,getHeight()*.36f); long e=System.currentTimeMillis()-calmStart; int rem=Math.max(0,7-(int)(e/1000)); mangaTxt(c,rem>0?String.valueOf(rem):"...",getWidth()/2f,getHeight()*.75f,38,C("#24243A"),Paint.Align.CENTER); if(e>=7000) solved(); }

    private void l5(Canvas c){ header(c,"This little guy says: 'LEFT! Definitely LEFT!'"); chibi(c,getWidth()*.5f,getHeight()*.39f,1.3f,false,true); speech(c,"LEFT! TRUST ME!",getWidth()*.19f,getHeight()*.50f,getWidth()*.62f,dp(92)); button(c,"LEFT",getWidth()*.10f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#FF8D7C")); button(c,"RIGHT",getWidth()*.56f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#7CD6C1")); wrap(c,"His eyes might be more honest than his mouth.",getWidth()/2f,getHeight()*.84f,getWidth()*.82f,16,C("#55566D")); if(liarChoice==1) solved(); }

    private void l6(Canvas c){ header(c,"Three eyes. Zero privacy."); float[][] eyes={{.30f,.48f},{.50f,.58f},{.70f,.48f}};int active=0;for(int i=0;i<3;i++){float ex=getWidth()*eyes[i][0],ey=getHeight()*eyes[i][1];boolean covered=false;for(int k=0;k<pointerCount;k++)if(Math.hypot(px[k]-ex,py[k]-ey)<dp(48))covered=true;if(covered)active++;p.setColor(covered?C("#FFCA4B"):Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawOval(ex-dp(42),ey-dp(26),ex+dp(42),ey+dp(26),p);p.setColor(C("#24243A"));c.drawCircle(ex,ey,covered?dp(5):dp(12),p);} mangaTxt(c,"HIDE THEM",getWidth()/2f,getHeight()*.76f,24,C("#E94C67"),Paint.Align.CENTER); if(active==3) solved(); }

    private void l7(Canvas c){ header(c,"The vending machine ate your key."); rr(c,getWidth()*.27f,getHeight()*.30f,getWidth()*.46f,getHeight()*.42f,dp(24),C("#77A7DD")); mangaTxt(c,"KEY",getWidth()*.5f,getHeight()*.42f,28,C("#FFCA4B"),Paint.Align.CENTER); mangaTxt(c,"ガタガタ!",getWidth()*.5f,getHeight()*.79f,28,C("#E94C67"),Paint.Align.CENTER); if(shakeCount>=3){shakeDropped=true;} if(shakeDropped){ mangaTxt(c,"🔑",getWidth()*.5f,getHeight()*.72f,38,C("#24243A"),Paint.Align.CENTER); if(System.currentTimeMillis()-roomStart>600) solved();} }

    private void l8(Canvas c){ header(c,"The key is stuck to the ceiling."); door(c,getWidth()*.5f,getHeight()*.70f,getWidth()*.32f,getHeight()*.23f); mangaTxt(c,"🔑",getWidth()*.5f,fallY,38,C("#24243A"),Paint.Align.CENTER); boolean inverted=ay<-6.2f; if(inverted) fallY=Math.min(getHeight()*.68f,fallY+dp(8)); if(fallY>=getHeight()*.65f) solved(); }

    private void l9(Canvas c){ header(c,"A sleeping ninja guards the door."); chibi(c,getWidth()*.5f,getHeight()*.48f,1.25f,false,System.currentTimeMillis()<ninjaSecretUntil); mangaTxt(c,"Z Z Z",getWidth()*.68f,getHeight()*.36f,28,C("#6B65B5"),Paint.Align.CENTER); mangaTxt(c,"TOK  TOK  TOK",getWidth()/2f,getHeight()*.72f,26,C("#E94C67"),Paint.Align.CENTER); mangaTxt(c,knockCount+" / 3",getWidth()/2f,getHeight()*.80f,20,C("#24243A"),Paint.Align.CENTER); if(knockCount>=3) solved(); }

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

    private void l21(Canvas c){
        header(c,"Build his face. Try not to traumatize him.");
        float left=getWidth()*.16f, top=getHeight()*.29f, right=getWidth()*.84f, bottom=getHeight()*.74f;
        float cx=(left+right)/2f, headY=top+(bottom-top)*.31f, bodyY=top+(bottom-top)*.67f;
        float eyeLX=cx-dp(26), eyeRX=cx+dp(26), eyeY=headY-dp(10), noseX=cx, noseY=headY+dp(16);
        long now=System.currentTimeMillis(); float meltT=snowEasterPhase==2?Math.min(1f,(now-snowEasterAt)/1500f):0f;
        if(snowmanArt!=null){
            c.save(); if(snowEasterPhase==2){c.translate(0,(bottom-top)*.34f*meltT);c.scale(1f,Math.max(.08f,1f-.86f*meltT),cx,bottom);}
            c.drawBitmap(snowmanArt,new Rect(0,0,snowmanArt.getWidth(),snowmanArt.getHeight()),new RectF(left,top,right,bottom),p);
            Paint fp=new Paint(Paint.ANTI_ALIAS_FLAG);fp.setShader(new RadialGradient(cx-dp(12),headY-dp(18),dp(92),Color.WHITE,C("#DCEBFA"),Shader.TileMode.CLAMP));c.drawOval(cx-dp(70),headY-dp(58),cx+dp(70),headY+dp(62),fp);c.restore();
        }
        if(snowEasterPhase==2){p.setStyle(Paint.Style.FILL);p.setColor(C("#BDE7F4"));c.drawOval(cx-dp(115)*meltT,bottom-dp(18),cx+dp(115)*meltT,bottom+dp(32)*meltT,p);}
        if(snowEasterPhase>0&&snowEasterPhase<3){p.setStyle(Paint.Style.FILL);p.setColor(C("#FF6F86"));c.drawCircle(cx-dp(44),headY+dp(18),dp(18),p);c.drawCircle(cx+dp(44),headY+dp(18),dp(18),p);}
        if(snowEasterPhase==0){
            p.setStyle(Paint.Style.FILL);p.setColor(C("#24243A"));if(snowEye1)c.drawCircle(eyeLX,eyeY,dp(12),p);if(snowEye2)c.drawCircle(eyeRX,eyeY,dp(12),p);
            if(snowNose){p.setColor(C("#F28C28"));Path n=new Path();n.moveTo(noseX-dp(7),noseY-dp(8));n.lineTo(noseX+dp(62),noseY);n.lineTo(noseX-dp(7),noseY+dp(8));n.close();c.drawPath(n,p);}
            if(!snowSmilePoints.isEmpty()){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(C("#24243A"));Path sm=new Path();PointF f=snowSmilePoints.get(0);sm.moveTo(f.x,f.y);for(int i=1;i<snowSmilePoints.size();i++){PointF q=snowSmilePoints.get(i);sm.lineTo(q.x,q.y);}c.drawPath(sm,p);p.setStrokeCap(Paint.Cap.BUTT);}
            if(!snowNose){c.save();c.translate(carrotX,carrotY);p.setStyle(Paint.Style.FILL);p.setColor(C("#F28C28"));Path n=new Path();n.moveTo(-dp(36),-dp(11));n.lineTo(dp(42),0);n.lineTo(-dp(36),dp(11));n.close();c.drawPath(n,p);p.setColor(C("#55A868"));c.drawRect(-dp(43),-dp(16),-dp(32),dp(16),p);c.restore();}
            p.setColor(C("#32313B"));p.setStyle(Paint.Style.FILL);if(!snowEye1)c.drawCircle(stone1X,stone1Y,dp(16),p);if(!snowEye2)c.drawCircle(stone2X,stone2Y,dp(16),p);
            wrap(c,"2 STONES  •  CARROT  •  DRAW A SMILE",getWidth()/2f,getHeight()*.875f,getWidth()*.88f,20,C("#24243A"));
        }
        if(snowEasterPhase==1){speech(c,"No! Lì proprio no!",getWidth()*.12f,getHeight()*.255f,getWidth()*.76f,dp(92));if(now-snowEasterAt>1150){snowEasterPhase=2;snowEasterAt=now;}}
        else if(snowEasterPhase==2&&meltT>=1f){snowEasterPhase=3;snowEasterAt=now;}
        if(snowEasterPhase==3){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(205,25,23,35));c.drawRect(0,0,getWidth(),getHeight(),p);mangaTxt(c,"MELTDOWN!",getWidth()/2f,getHeight()*.38f,38,Color.WHITE,Paint.Align.CENTER);wrap(c,"You have emotionally and physically destroyed the snowman.",getWidth()/2f,getHeight()*.46f,getWidth()*.78f,21,Color.WHITE);button(c,"RETRY",getWidth()*.18f,getHeight()*.58f,getWidth()*.64f,dp(66),C("#FFCA4B"));button(c,"MENU",getWidth()*.18f,getHeight()*.68f,getWidth()*.64f,dp(60),C("#7CD6C1"));}
        if(snowEye1&&snowEye2&&snowNose&&snowSmile&&snowEasterPhase==0)solved();
    }

    private void l22(Canvas c){ header(c,"He always chooses the WRONG door."); chibi(c,getWidth()*.5f,getHeight()*.40f,1.25f,true,true); speech(c,"I PICK LEFT!",getWidth()*.22f,getHeight()*.49f,getWidth()*.56f,dp(95)); button(c,"LEFT",getWidth()*.10f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#FF8D7C")); button(c,"RIGHT",getWidth()*.56f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#7CD6C1")); wrap(c,"If his choice is always wrong, which door is safe?",getWidth()/2f,getHeight()*.84f,getWidth()*.82f,19,C("#3F4156")); if(oppositeChoice==1)solved(); }

    private void l23(Canvas c){ header(c,"One finger opens the gate. Gravity does the rest.");float L=getWidth()*.08f,R=getWidth()*.92f,T=getHeight()*.30f,B=getHeight()*.74f;rr(c,L,T,R-L,B-T,dp(20),Color.WHITE);float gateX=getWidth()*.55f;p.setColor(C("#E94C67"));p.setStyle(Paint.Style.FILL);if(!gatePressed)c.drawRect(gateX,T,gateX+dp(14),B,p);button(c,"HOLD",getWidth()*.08f,getHeight()*.78f,getWidth()*.28f,dp(58),C("#FFCA4B"));long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*50*dt;vy+=ay*50*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float r=dp(16);if(ballX<L+r)ballX=L+r;if(ballX>R-r)ballX=R-r;if(ballY<T+r)ballY=T+r;if(ballY>B-r)ballY=B-r;if(!gatePressed&&ballX>gateX-r&&ballX<gateX+dp(26)&&ballY>T&&ballY<B){ballX=gateX-r;vx*=-.5f;}p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,r,p);float tx=getWidth()*.82f,ty=getHeight()*.38f;p.setColor(C("#55B979"));c.drawCircle(tx,ty,dp(25),p);if(Math.hypot(ballX-tx,ballY-ty)<dp(28))solved(); }

    private void l24(Canvas c){
        header(c,"This key refuses to leave the screen.");
        mangaTxt(c,"🔑",getWidth()*.50f,getHeight()*.47f,58,C("#24243A"),Paint.Align.CENTER);
        speedBurst(c,getWidth()*.50f,getHeight()*.47f,dp(105));
        speech(c,"YOU CAN LOOK... BUT DON'T TOUCH!",getWidth()*.12f,getHeight()*.59f,getWidth()*.76f,dp(110));
        mangaTxt(c,"TAKE IT WITH YOU",getWidth()/2f,getHeight()*.77f,25,C("#E94C67"),Paint.Align.CENTER);
        if(android.os.Build.VERSION.SDK_INT < 34){
            wrap(c,"Screenshot detection needs Android 14+ in this test build.",getWidth()/2f,getHeight()*.84f,getWidth()*.80f,18,C("#55566D"));
        }
        if(screenshotCaptured) solved();
    }

    private void l25(Canvas c){ header(c,"FINAL BOSS: three lessons, one room."); if(bossPhase==0){rr(c,getWidth()*.27f,getHeight()*.34f,getWidth()*.46f,getHeight()*.30f,dp(20),C("#77A7DD"));mangaTxt(c,"SHAKE",getWidth()/2f,getHeight()*.52f,30,C("#24243A"),Paint.Align.CENTER);if(shakeCount>=3){bossSealRevealed=true;bossPhase=1;showToast("SEALS REVEALED!");}} else if(bossPhase==1){float y=getHeight()*.50f;seal(c,getWidth()*.34f,y,"1");seal(c,getWidth()*.66f,y,"2");boolean a=false,b=false;for(int i=0;i<pointerCount;i++){if(Math.hypot(px[i]-getWidth()*.34f,py[i]-y)<dp(60))a=true;if(Math.hypot(px[i]-getWidth()*.66f,py[i]-y)<dp(60))b=true;}if(a&&b){if(bossBothStart==0)bossBothStart=System.currentTimeMillis();if(System.currentTimeMillis()-bossBothStart>800){bossPhase=2;resetBall();showToast("FINAL ORB!");}}else bossBothStart=0;} else {float tx=getWidth()*.5f,ty=getHeight()*.40f;long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*55*dt;vy+=ay*55*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;ballX=Math.max(dp(20),Math.min(getWidth()-dp(20),ballX));ballY=Math.max(getHeight()*.28f,Math.min(getHeight()*.78f,ballY));p.setColor(C("#FFCA4B"));c.drawCircle(tx,ty,dp(32),p);p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,dp(18),p);if(Math.hypot(ballX-tx,ballY-ty)<dp(30))solved();} mangaTxt(c,"PHASE "+(bossPhase+1)+" / 3",getWidth()/2f,getHeight()*.78f,22,C("#E94C67"),Paint.Align.CENTER); }

    private void playRhythm(){
        if(rhythmPlaying) return;
        rhythmPlaying=true; rhythmReady=false; rhythmTaps.clear();
        final int[] at={0,360,720,1340};
        for(int i=0;i<at.length;i++){
            final int idx=i;
            handler.postDelayed(() -> {
                if(screen!=26) return;
                tone.startTone(idx==3?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,120);
                invalidate();
            },at[i]);
        }
        handler.postDelayed(() -> { if(screen==26){rhythmPlaying=false;rhythmReady=true;showToast("YOUR TURN!");invalidate();} },1700);
    }

    private void l26(Canvas c){
        header(c,"Listen first. Then copy the rhythm.");
        float cx=getWidth()/2f;
        p.setStyle(Paint.Style.FILL); p.setColor(C("#24243A")); c.drawCircle(cx,getHeight()*.43f,dp(72),p);
        mangaTxt(c,"♪",cx,getHeight()*.455f,54,Color.WHITE,Paint.Align.CENTER);
        if(!rhythmReady && !rhythmPlaying) button(c,"PLAY RHYTHM",getWidth()*.22f,getHeight()*.57f,getWidth()*.56f,dp(62),C("#7CD6C1"));
        else if(rhythmPlaying) mangaTxt(c,"LISTEN...",cx,getHeight()*.62f,24,C("#E94C67"),Paint.Align.CENTER);
        else {
            button(c,"TAP",getWidth()*.30f,getHeight()*.56f,getWidth()*.40f,dp(90),C("#E94C67"));
            mangaTxt(c,rhythmTaps.size()+" / 4",cx,getHeight()*.70f,22,C("#24243A"),Paint.Align.CENTER);
        }
        instruction(c,"Four tones. Match the timing between them.");
    }

    private void checkRhythm(){
        if(rhythmTaps.size()<4) return;
        boolean ok=true;
        for(int i=0;i<3;i++){
            long d=rhythmTaps.get(i+1)-rhythmTaps.get(i);
            if(Math.abs(d-rhythmIntervals[i])>180) ok=false;
        }
        if(ok){showToast("ON BEAT!"); solved();}
        else {showToast("OFF BEAT — LISTEN AGAIN"); rhythmReady=false; rhythmTaps.clear();}
    }

    private void l27(Canvas c){
        header(c,"The guard only respects ridiculous faces.");
        float cx=getWidth()/2f;
        if(selfieBitmap==null){
            chibi(c,cx,getHeight()*.45f,1.25f,false,true);
            speech(c,"PROVE YOU'RE FUNNY.",getWidth()*.18f,getHeight()*.56f,getWidth()*.64f,dp(94));
            button(c,selfieRequested?"OPENING CAMERA...":"TAKE A SELFIE",getWidth()*.20f,getHeight()*.70f,getWidth()*.60f,dp(66),C("#FFCA4B"));
        }else{
            float left=getWidth()*.17f, top=getHeight()*.29f, right=getWidth()*.83f, bottom=getHeight()*.70f;
            p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); c.drawRoundRect(left-dp(8),top-dp(8),right+dp(8),bottom+dp(8),dp(20),dp(20),p);
            Rect src=new Rect(0,0,selfieBitmap.getWidth(),selfieBitmap.getHeight()); RectF dst=new RectF(left,top,right,bottom); c.drawBitmap(selfieBitmap,src,dst,p);
            // oversized cartoon glasses
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(8)); p.setColor(Color.BLACK);
            float gy=top+(bottom-top)*.37f; float gw=(right-left)*.24f, gh=(bottom-top)*.13f;
            c.drawOval(cx-gw-dp(8),gy-gh,cx-dp(8),gy+gh,p); c.drawOval(cx+dp(8),gy-gh,cx+gw+dp(8),gy+gh,p); c.drawLine(cx-dp(8),gy,cx+dp(8),gy,p);
            // moustache
            p.setStyle(Paint.Style.FILL); Path m=new Path(); float my=top+(bottom-top)*.63f;
            m.moveTo(cx,my); m.cubicTo(cx-dp(10),my-dp(18),cx-dp(65),my-dp(20),cx-dp(72),my+dp(12)); m.cubicTo(cx-dp(45),my+dp(3),cx-dp(22),my+dp(23),cx,my+dp(6));
            m.cubicTo(cx+dp(22),my+dp(23),cx+dp(45),my+dp(3),cx+dp(72),my+dp(12)); m.cubicTo(cx+dp(65),my-dp(20),cx+dp(10),my-dp(18),cx,my); c.drawPath(m,p);
            mangaTxt(c,"PERFECT. ABSOLUTELY TERRIBLE.",cx,getHeight()*.76f,20,C("#E94C67"),Paint.Align.CENTER);
            if(selfieCapturedAt>0 && System.currentTimeMillis()-selfieCapturedAt>2600) solved();
        }
    }

    public void onSelfieCaptured(Bitmap bitmap){
        selfieRequested=false;
        if(screen==27 && bitmap!=null){selfieBitmap=bitmap;selfieCapturedAt=System.currentTimeMillis();showToast("STYLE UPGRADE!");invalidate();}
    }

    public void onSelfieCancelled(){ selfieRequested=false; if(screen==27){showToast("CAMERA CANCELLED");invalidate();} }


    private void keyPiece(Canvas c,float x,float y,int idx){p.setStyle(Paint.Style.FILL);p.setColor(C("#FFCA4B"));if(idx==0)c.drawRoundRect(x-dp(30),y-dp(12),x+dp(8),y+dp(12),dp(8),dp(8),p);else if(idx==1)c.drawCircle(x,y,dp(18),p);else{Path q=new Path();q.moveTo(x-dp(8),y-dp(16));q.lineTo(x+dp(28),y);q.lineTo(x-dp(8),y+dp(16));q.close();c.drawPath(q,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(C("#8B6810"));c.drawCircle(x,y,dp(22),p);}
    private void keyProgress(Canvas c,int mask,int total){mangaTxt(c,"KEY PIECES  "+Integer.bitCount(mask)+" / "+total,getWidth()/2f,getHeight()*.79f,22,C("#E94C67"),Paint.Align.CENTER);}
    private void l28(Canvas c){header(c,"Three key pieces are hiding in plain sight.");float dt=getHeight()*.44f;rr(c,getWidth()*.08f,dt,getWidth()*.84f,dp(26),dp(10),C("#A96F45"));rr(c,getWidth()*.12f,dt+dp(28),getWidth()*.30f,dp(100),dp(12),C("#D49A68"));mangaTxt(c,deskDrawerOpen?"OPEN":"DRAWER",getWidth()*.27f,dt+dp(88),18,C("#24243A"),Paint.Align.CENTER);if(deskDrawerOpen&&(deskMask&1)==0)keyPiece(c,getWidth()*.27f,dt+dp(60),0);p.setStyle(Paint.Style.FILL);p.setColor(C("#7CD6C1"));c.drawCircle(deskMugX,deskMugY,dp(34),p);mangaTxt(c,"MUG",deskMugX,deskMugY+sp(6),14,C("#24243A"),Paint.Align.CENTER);if(Math.abs(deskMugX-getWidth()*.63f)>dp(80)&&(deskMask&2)==0)keyPiece(c,getWidth()*.63f,getHeight()*.59f,1);rr(c,getWidth()*.66f,getHeight()*.42f,getWidth()*.22f,dp(72),dp(8),C("#E94C67"));mangaTxt(c,"BOOK",getWidth()*.77f,getHeight()*.46f,17,Color.WHITE,Paint.Align.CENTER);if(deskBookHold>0&&System.currentTimeMillis()-deskBookHold>1100&&(deskMask&4)==0)keyPiece(c,getWidth()*.77f,getHeight()*.50f,2);if(deskMask==7){mangaTxt(c,"🔑",getWidth()/2f,getHeight()*.68f,48,C("#24243A"),Paint.Align.CENTER);door(c,getWidth()*.82f,getHeight()*.66f,getWidth()*.18f,getHeight()*.19f);}keyProgress(c,deskMask,3);}
    private void l29(Canvas c){header(c,"The toy box has terrible secrets.");rr(c,getWidth()*.08f,getHeight()*.36f,getWidth()*.84f,getHeight()*.34f,dp(24),C("#C88EE8"));mangaTxt(c,"🧸",getWidth()*.28f,getHeight()*.51f,50,C("#24243A"),Paint.Align.CENTER);if(toyBearTaps>=2&&(toyMask&1)==0)keyPiece(c,getWidth()*.28f,getHeight()*.58f,0);p.setStyle(Paint.Style.FILL);p.setColor(C("#FF7A89"));c.drawCircle(toyBallX,toyBallY,dp(38),p);if(Math.abs(toyBallX-getWidth()*.70f)>dp(100)&&(toyMask&2)==0)keyPiece(c,getWidth()*.70f,getHeight()*.60f,1);mangaTxt(c,"🤖",getWidth()*.50f,getHeight()*.61f,48,C("#24243A"),Paint.Align.CENTER);if(shakeCount-toyShakeStart>=2&&(toyMask&4)==0)keyPiece(c,getWidth()*.50f,getHeight()*.67f,2);if(toyMask==7){door(c,getWidth()*.82f,getHeight()*.66f,getWidth()*.18f,getHeight()*.19f);mangaTxt(c,"🔑",getWidth()*.58f,getHeight()*.72f,46,C("#24243A"),Paint.Align.CENTER);}keyProgress(c,toyMask,3);}
    private void l30(Canvas c){header(c,"Breakfast stole the key. Naturally.");p.setStyle(Paint.Style.FILL);p.setColor(C("#E94C67"));c.drawCircle(appleX,appleY,dp(34),p);mangaTxt(c,"🍎",appleX,appleY+sp(10),28,C("#24243A"),Paint.Align.CENTER);if(Math.abs(appleX-getWidth()*.30f)>dp(90)&&(kitchenMask&1)==0)keyPiece(c,getWidth()*.30f,getHeight()*.61f,0);rr(c,getWidth()*.48f,getHeight()*.46f,getWidth()*.28f,dp(95),dp(15),C("#9EA6B6"));mangaTxt(c,"TOASTER",getWidth()*.62f,getHeight()*.51f,16,C("#24243A"),Paint.Align.CENTER);if(toasterTaps>=3&&(kitchenMask&2)==0)keyPiece(c,getWidth()*.62f,getHeight()*.58f,1);p.setStyle(Paint.Style.FILL);p.setColor(C("#F1D18A"));c.drawCircle(getWidth()*.50f,getHeight()*.69f,dp(45),p);mangaTxt(c,"JAR",getWidth()*.50f,getHeight()*.70f,17,C("#24243A"),Paint.Align.CENTER);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#24243A"));c.drawArc(getWidth()*.50f-dp(55),getHeight()*.69f-dp(55),getWidth()*.50f+dp(55),getHeight()*.69f+dp(55),-90,Math.min(300,jarTwist),false,p);if(jarTwist>260&&(kitchenMask&4)==0)keyPiece(c,getWidth()*.50f,getHeight()*.76f,2);keyProgress(c,kitchenMask,3);if(kitchenMask==7)mangaTxt(c,"TAP THE COMPLETE KEY",getWidth()/2f,getHeight()*.84f,21,C("#55A868"),Paint.Align.CENTER);}
    private void l31(Canvas c){header(c,"Three compartments. Three different phone tricks.");float[] xs={.22f,.50f,.78f};String[] labs={"SHAKE","HOLD","TILT"};for(int i=0;i<3;i++){rr(c,getWidth()*xs[i]-dp(62),getHeight()*.42f,dp(124),dp(145),dp(18),Color.WHITE);mangaTxt(c,labs[i],getWidth()*xs[i],getHeight()*.49f,17,C("#24243A"),Paint.Align.CENTER);if((vaultMask&(1<<i))!=0)keyPiece(c,getWidth()*xs[i],getHeight()*.57f,i);}if(shakeCount-vaultShakeStart>=2)vaultMask|=1;if(ax>6f){if(vaultTiltStart==0)vaultTiltStart=System.currentTimeMillis();if(System.currentTimeMillis()-vaultTiltStart>800)vaultMask|=4;}else vaultTiltStart=0;keyProgress(c,vaultMask,3);if(vaultMask==7){door(c,getWidth()*.5f,getHeight()*.72f,getWidth()*.22f,getHeight()*.18f);if(System.currentTimeMillis()-roomStart>900)solved();}}
    private void playSoundSeq(){if(soundPlaying)return;soundPlaying=true;soundReady=false;soundTaps.clear();int[] at={0,380,760,1140};int[] tones={ToneGenerator.TONE_DTMF_1,ToneGenerator.TONE_DTMF_3,ToneGenerator.TONE_DTMF_2,ToneGenerator.TONE_DTMF_1};for(int i=0;i<4;i++){final int k=i;handler.postDelayed(()->{if(screen==32){tone.startTone(tones[k],170);invalidate();}},at[i]);}handler.postDelayed(()->{if(screen==32){soundPlaying=false;soundReady=true;showToast("REPEAT THE PITCHES!");invalidate();}},1500);}
    private void l32(Canvas c){header(c,"Repeat the PITCH sequence.");float cx=getWidth()/2f;mangaTxt(c,"♫  ?  ?  ?  ?",cx,getHeight()*.39f,34,C("#24243A"),Paint.Align.CENTER);if(!soundPlaying&&!soundReady)button(c,"LISTEN",getWidth()*.26f,getHeight()*.48f,getWidth()*.48f,dp(62),C("#7CD6C1"));if(soundReady){int[] cols={C("#7CD6C1"),C("#FFCA4B"),C("#FF7A89")};String[] labs={"LOW","MID","HIGH"};for(int i=0;i<3;i++)button(c,labs[i],getWidth()*.08f+i*getWidth()*.30f,getHeight()*.57f,getWidth()*.24f,dp(70),cols[i]);mangaTxt(c,soundTaps.size()+" / 4",cx,getHeight()*.72f,22,C("#E94C67"),Paint.Align.CENTER);}instruction(c,"Listen to pitch order, then reproduce it.");}
    private void checkSoundSeq(){if(soundTaps.size()<4)return;boolean ok=true;for(int i=0;i<4;i++)if(soundTaps.get(i)!=soundSeq[i])ok=false;if(ok)solved();else{showToast("WRONG MELODY");soundReady=false;soundTaps.clear();}}
    private void l33(Canvas c){header(c,"Odd sides first. Then even sides. Reverse the second group.");float[] xs={.18f,.39f,.61f,.82f};int[] sides={3,4,5,6};for(int i=0;i<4;i++){drawPoly(c,getWidth()*xs[i],getHeight()*.54f,dp(45),sides[i],new int[]{C("#E94C67"),C("#7CD6C1"),C("#FFCA4B"),C("#8C77D9")}[i]);mangaTxt(c,String.valueOf(sides[i]),getWidth()*xs[i],getHeight()*.67f,18,C("#24243A"),Paint.Align.CENTER);}mangaTxt(c,"STEP "+logicPos+" / 4",getWidth()/2f,getHeight()*.76f,21,C("#E94C67"),Paint.Align.CENTER);}
    private void drawPoly(Canvas c,float cx,float cy,float r,int sides,int col){Path q=new Path();for(int i=0;i<sides;i++){double a=-Math.PI/2+i*Math.PI*2/sides;float x=cx+(float)Math.cos(a)*r,y=cy+(float)Math.sin(a)*r;if(i==0)q.moveTo(x,y);else q.lineTo(x,y);}q.close();p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawPath(q,p);}
    private void l34(Canvas c){header(c,"Someone covered the code with grime.");float l=getWidth()*.14f,t=getHeight()*.36f,r=getWidth()*.86f,b=getHeight()*.58f;rr(c,l,t,r-l,b-t,dp(20),C("#5D5A63"));if(rubProgress>20){float aa=Math.min(1f,rubProgress/120f);p.setColor(Color.argb((int)(255*aa),255,255,255));p.setTextSize(sp(44));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText("5 7 3",getWidth()/2f,getHeight()*.49f,p);}mangaTxt(c,"RUBBED  "+Math.min(100,(int)(rubProgress/1.2f))+"%",getWidth()/2f,getHeight()*.63f,19,C("#E94C67"),Paint.Align.CENTER);if(rubProgress>=100){drawKeypad(c);mangaTxt(c,revealCode.isEmpty()?"_ _ _":revealCode,getWidth()/2f,getHeight()*.68f,26,C("#24243A"),Paint.Align.CENTER);if(revealCode.equals("573"))solved();if(revealCode.length()>=3&&!revealCode.equals("573")){showToast("WRONG CODE");revealCode="";}}}
    private void l35(Canvas c){header(c,"MASTER KEY FORGE: four fragments, four tricks.");rr(c,getWidth()*.10f,getHeight()*.37f,getWidth()*.80f,getHeight()*.30f,dp(22),C("#3E3B54"));mangaTxt(c,"◆",getWidth()*.26f,getHeight()*.50f,36,C("#FFCA4B"),Paint.Align.CENTER);mangaTxt(c,"GEM",getWidth()*.26f,getHeight()*.57f,15,Color.WHITE,Paint.Align.CENTER);rr(c,forgePanelX-dp(48),getHeight()*.43f,dp(96),dp(112),dp(12),C("#E94C67"));mangaTxt(c,"MOVE",forgePanelX,getHeight()*.50f,15,Color.WHITE,Paint.Align.CENTER);if(shakeCount-forgeShakeStart>=3)forgeMask|=1;if(forgeGemTaps>=4)forgeMask|=2;if(Math.abs(forgePanelX-getWidth()*.56f)>dp(110))forgeMask|=4;boolean aa=false,bb=false;for(int i=0;i<pointerCount;i++){if(Math.hypot(px[i]-getWidth()*.74f,py[i]-getHeight()*.50f)<dp(55))aa=true;if(Math.hypot(px[i]-getWidth()*.82f,py[i]-getHeight()*.60f)<dp(55))bb=true;}if(aa&&bb){if(forgeTwoFingerStart==0)forgeTwoFingerStart=System.currentTimeMillis();if(System.currentTimeMillis()-forgeTwoFingerStart>700)forgeMask|=8;}else forgeTwoFingerStart=0;for(int i=0;i<4;i++)if((forgeMask&(1<<i))!=0)keyPiece(c,getWidth()*(.24f+i*.17f),getHeight()*.72f,i%3);mangaTxt(c,"FRAGMENTS "+Integer.bitCount(forgeMask)+" / 4",getWidth()/2f,getHeight()*.79f,22,C("#E94C67"),Paint.Align.CENTER);if(forgeMask==15){mangaTxt(c,"🔑",forgeKeyX,forgeKeyY,48,C("#24243A"),Paint.Align.CENTER);door(c,getWidth()*.84f,getHeight()*.72f,getWidth()*.18f,getHeight()*.18f);if(Math.hypot(forgeKeyX-getWidth()*.84f,forgeKeyY-getHeight()*.72f)<dp(60))solved();}}

    private void speedBurst(Canvas c,float cx,float cy,float radius){
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#24243A"));
        for(int i=0;i<18;i++){
            double a=(Math.PI*2*i)/18.0;
            float x1=cx+(float)Math.cos(a)*radius*.62f, y1=cy+(float)Math.sin(a)*radius*.62f;
            float x2=cx+(float)Math.cos(a)*radius, y2=cy+(float)Math.sin(a)*radius;
            c.drawLine(x1,y1,x2,y2,p);
        }
    }

    public void onScreenshotDetected(){
        if(screen==24 && !hintOpen){
            screenshotCaptured=true;
            showToast("SNAP! YOU CAUGHT IT! 📸");
            invalidate();
        }
    }

    private void cache(MotionEvent e){ pointerCount=Math.min(10,e.getPointerCount()); for(int i=0;i<pointerCount;i++){px[i]=e.getX(i);py[i]=e.getY(i);} }

    @Override public boolean onTouchEvent(MotionEvent e){
        cache(e); int a=e.getActionMasked(); int idx=e.getActionIndex(); float x=e.getX(idx),y=e.getY(idx);
        if(a==MotionEvent.ACTION_DOWN){downX=x;downY=y;downAt=System.currentTimeMillis();}
        if(screen==0){ if(a==MotionEvent.ACTION_DOWN&&y<getHeight()*.30f){homeSecretTaps++;if(homeSecretTaps>=7){homeSecretUntil=System.currentTimeMillis()+2600;homeSecretTaps=0;}} if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.67f,getWidth()*.82f,getHeight()*.78f))gotoLevel(1); else if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.78f,getWidth()*.82f,getHeight()*.88f)){screen=90;((MainActivity)getContext()).syncMusicForScreen(90);} invalidate(); return true; }
        if(screen==90){ if(a==MotionEvent.ACTION_DOWN){int cols=5;float gap=dp(8),cell=(getWidth()-dp(32)-gap*4)/5,top=getHeight()*.12f;for(int i=1;i<=TOTAL_LEVELS;i++){int row=(i-1)/5,col=(i-1)%5;float lx=dp(16)+col*(cell+gap),ty=top+row*(cell+gap);if(in(x,y,lx,ty,lx+cell,ty+cell)){gotoLevel(i);return true;}}if(y>getHeight()*.84f)screen=0;}invalidate();return true; }
        if(screen==99){ if(a==MotionEvent.ACTION_DOWN){screen=90;invalidate();}return true; }
        if(hintOpen){
            if(a==MotionEvent.ACTION_DOWN){float hx=getWidth()*.07f,hy=getHeight()*.24f,hw=getWidth()*.86f,hh=getHeight()*.38f;if(in(x,y,hx+hw-dp(78),hy,hx+hw,hy+dp(78))||in(x,y,hx+hw*.24f,hy+hh-dp(90),hx+hw*.76f,hy+hh)){hintOpen=false;invalidate();}}
            return true;
        }
        if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.035f,getHeight()*.175f,getWidth()*.285f,getHeight()*.265f)){screen=0;hintOpen=false;((MainActivity)getContext()).syncMusicForScreen(0);invalidate();return true;}
        if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.715f,getHeight()*.175f,getWidth()*.98f,getHeight()*.265f)){useHint();return true;}

        switch(screen){
            case 1:
                if(e.getPointerCount()>=2){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(a==MotionEvent.ACTION_POINTER_DOWN){pinchStart=d;keyScaleStart=keyScale;}else if(a==MotionEvent.ACTION_MOVE&&pinchStart>0)keyScale=Math.max(.28f,Math.min(1.25f,keyScaleStart*d/pinchStart));}else{if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-keyX,y-keyY)<dp(130)){keyDrag=true;keyDX=x-keyX;keyDY=y-keyY;}if(a==MotionEvent.ACTION_MOVE&&keyDrag){keyX=x-keyDX;keyY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)keyDrag=false;}break;
            case 2: if(a==MotionEvent.ACTION_DOWN)showToast("Hands off. Tilt!");break;
            case 3: if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-getWidth()*.5f,y-getHeight()*.53f)<dp(90)){balloonHold=System.currentTimeMillis();balloonSecretTaps++;if(balloonSecretTaps>=5){balloonSecretUntil=System.currentTimeMillis()+1800;balloonSecretTaps=0;showToast("THE BALLOON IS JUDGING YOU 👀");}}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(!balloonPopped){balloonHold=0;balloonR=dp(48);showToast("Hold longer!");}}break;
            case 4: if(a==MotionEvent.ACTION_DOWN){calmStart=System.currentTimeMillis();showToast("RESET! Stop touching.");}break;
            case 5: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.66f){liarChoice=x>getWidth()*.5f?1:0;if(liarChoice==0)showToast("He fooled you 😏");}break;
            case 6: break;
            case 7: break;
            case 8: break;
            case 9: if(a==MotionEvent.ACTION_DOWN){long now=System.currentTimeMillis();if(lastKnock==0||now-lastKnock<700){knockCount++;lastKnock=now;}else{knockCount=1;lastKnock=now;}if(knockCount>=6){ninjaSecretUntil=now+2200;showToast("I SAID THREE KNOCKS! 😡");knockCount=0;}}break;
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
            case 21:
                float scx=getWidth()*.50f, sheadY=getHeight()*.43f; float seyeLX=scx-dp(26),seyeRX=scx+dp(26),seyeY=sheadY-dp(10),snoseX=scx,snoseY=sheadY+dp(16);float sbuttX=getWidth()*.78f,sbuttY=getHeight()*.67f;
                if(snowEasterPhase==3){if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.56f,getWidth()*.82f,getHeight()*.66f)){roomStart=System.currentTimeMillis();resetLevel();}else if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.66f,getWidth()*.82f,getHeight()*.78f)){screen=0;resetLevel();}break;}
                if(snowEasterPhase>0)break;
                if(a==MotionEvent.ACTION_DOWN){if(!snowNose&&Math.hypot(x-carrotX,y-carrotY)<dp(72)){snowDrag=1;keyDX=x-carrotX;keyDY=y-carrotY;}else if(!snowEye1&&Math.hypot(x-stone1X,y-stone1Y)<dp(45)){snowDrag=2;keyDX=x-stone1X;keyDY=y-stone1Y;}else if(!snowEye2&&Math.hypot(x-stone2X,y-stone2Y)<dp(45)){snowDrag=3;keyDX=x-stone2X;keyDY=y-stone2Y;}else if(y>sheadY+dp(18)&&y<sheadY+dp(72)&&Math.abs(x-scx)<dp(72)){drawingSmile=true;snowSmilePoints.clear();snowSmilePoints.add(new PointF(x,y));}}
                if(a==MotionEvent.ACTION_MOVE){if(snowDrag==1){carrotX=x-keyDX;carrotY=y-keyDY;if(Math.hypot(carrotX-sbuttX,carrotY-sbuttY)<dp(78)){snowEasterPhase=1;snowEasterAt=System.currentTimeMillis();snowDrag=0;}}if(snowDrag==2){stone1X=x-keyDX;stone1Y=y-keyDY;}if(snowDrag==3){stone2X=x-keyDX;stone2Y=y-keyDY;}if(drawingSmile&&snowSmilePoints.size()<100)snowSmilePoints.add(new PointF(x,y));}
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(snowDrag==1){if(Math.hypot(carrotX-snoseX,carrotY-snoseY)<dp(58))snowNose=true;else{carrotX=getWidth()*.20f;carrotY=getHeight()*.77f;}}if(snowDrag==2){if(Math.hypot(stone1X-seyeLX,stone1Y-seyeY)<dp(44))snowEye1=true;else{stone1X=getWidth()*.43f;stone1Y=getHeight()*.80f;}}if(snowDrag==3){if(Math.hypot(stone2X-seyeRX,stone2Y-seyeY)<dp(44))snowEye2=true;else{stone2X=getWidth()*.58f;stone2Y=getHeight()*.80f;}}snowDrag=0;if(drawingSmile){drawingSmile=false;if(snowSmilePoints.size()>=5){PointF f=snowSmilePoints.get(0),l=snowSmilePoints.get(snowSmilePoints.size()-1),m=snowSmilePoints.get(snowSmilePoints.size()/2);float ends=(f.y+l.y)/2f;if(f.x<scx-dp(18)&&l.x>scx+dp(18)&&m.y>ends+dp(4))snowSmile=true;else{snowSmilePoints.clear();showToast("DRAW A CURVED SMILE ☺");}}}}
                break;
            case 22: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.64f){oppositeChoice=x>getWidth()*.5f?1:0;if(oppositeChoice==0)showToast("That is the door he picked — so it is wrong.");}break;
            case 23: gatePressed=false;for(int i=0;i<pointerCount;i++)if(in(px[i],py[i],getWidth()*.06f,getHeight()*.75f,getWidth()*.40f,getHeight()*.88f))gatePressed=true;break;
            case 24: if(a==MotionEvent.ACTION_DOWN)showToast("Touching will not take it with you.");break;
            case 25: break;
            case 26:
                if(a==MotionEvent.ACTION_DOWN){
                    if(!rhythmReady && !rhythmPlaying && in(x,y,getWidth()*.20f,getHeight()*.53f,getWidth()*.80f,getHeight()*.67f)){ playRhythm(); }
                    else if(rhythmReady && in(x,y,getWidth()*.27f,getHeight()*.52f,getWidth()*.73f,getHeight()*.68f)){
                        rhythmTaps.add(System.currentTimeMillis()); tone.startTone(ToneGenerator.TONE_PROP_BEEP,70); checkRhythm();
                    }
                }
                break;
            case 27:
                if(a==MotionEvent.ACTION_DOWN && selfieBitmap==null && !selfieRequested && in(x,y,getWidth()*.16f,getHeight()*.66f,getWidth()*.84f,getHeight()*.80f)){selfieRequested=true;((MainActivity)getContext()).startSelfieCapture();invalidate();}
                break;
            case 28:
                if(a==MotionEvent.ACTION_DOWN){if(in(x,y,getWidth()*.12f,getHeight()*.44f+dp(28),getWidth()*.42f,getHeight()*.44f+dp(128))){deskDrawerOpen=true;deskMask|=1;}else if(Math.hypot(x-deskMugX,y-deskMugY)<dp(55)){deskMugDrag=true;keyDX=x-deskMugX;keyDY=y-deskMugY;}else if(in(x,y,getWidth()*.64f,getHeight()*.39f,getWidth()*.90f,getHeight()*.54f))deskBookHold=System.currentTimeMillis();if(deskMask==7&&Math.hypot(x-getWidth()*.5f,y-getHeight()*.68f)<dp(70))solved();}
                if(a==MotionEvent.ACTION_MOVE&&deskMugDrag){deskMugX=x-keyDX;deskMugY=y-keyDY;if(Math.abs(deskMugX-getWidth()*.63f)>dp(80))deskMask|=2;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){deskMugDrag=false;if(deskBookHold>0&&System.currentTimeMillis()-deskBookHold>1050)deskMask|=4;deskBookHold=0;}break;
            case 29:
                if(a==MotionEvent.ACTION_DOWN){if(Math.hypot(x-getWidth()*.28f,y-getHeight()*.51f)<dp(60)){toyBearTaps++;if(toyBearTaps>=2)toyMask|=1;}else if(Math.hypot(x-toyBallX,y-toyBallY)<dp(55)){toyBallDrag=true;keyDX=x-toyBallX;keyDY=y-toyBallY;}if(toyMask==7&&x>getWidth()*.52f&&y>getHeight()*.64f)solved();}if(a==MotionEvent.ACTION_MOVE&&toyBallDrag){toyBallX=x-keyDX;toyBallY=y-keyDY;if(Math.abs(toyBallX-getWidth()*.70f)>dp(100))toyMask|=2;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)toyBallDrag=false;if(shakeCount-toyShakeStart>=2)toyMask|=4;break;
            case 30:
                if(a==MotionEvent.ACTION_DOWN){lastMoveX=x;if(Math.hypot(x-appleX,y-appleY)<dp(55)){appleDrag=true;keyDX=x-appleX;keyDY=y-appleY;}else if(in(x,y,getWidth()*.47f,getHeight()*.43f,getWidth()*.78f,getHeight()*.60f)){toasterTaps++;if(toasterTaps>=3)kitchenMask|=2;}}if(a==MotionEvent.ACTION_MOVE){if(appleDrag){appleX=x-keyDX;appleY=y-keyDY;if(Math.abs(appleX-getWidth()*.30f)>dp(90))kitchenMask|=1;}if(y>getHeight()*.62f&&Math.abs(x-getWidth()*.50f)<dp(85)){jarTwist+=Math.abs(x-lastMoveX)*.9f;lastMoveX=x;if(jarTwist>260)kitchenMask|=4;}}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)appleDrag=false;if(kitchenMask==7&&a==MotionEvent.ACTION_DOWN&&y>getHeight()*.78f)solved();break;
            case 31:
                if(a==MotionEvent.ACTION_DOWN&&Math.abs(x-getWidth()*.50f)<dp(70)&&y>getHeight()*.40f&&y<getHeight()*.64f)vaultHoldStart=System.currentTimeMillis();if((a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)&&vaultHoldStart>0){if(System.currentTimeMillis()-vaultHoldStart>1100)vaultMask|=2;vaultHoldStart=0;}break;
            case 32:
                if(a==MotionEvent.ACTION_DOWN){if(!soundReady&&!soundPlaying&&in(x,y,getWidth()*.26f,getHeight()*.46f,getWidth()*.74f,getHeight()*.57f))playSoundSeq();else if(soundReady&&y>getHeight()*.55f&&y<getHeight()*.68f){int soundHit=(int)(x/(getWidth()/3f));soundHit=Math.max(0,Math.min(2,soundHit));soundTaps.add(soundHit);tone.startTone(new int[]{ToneGenerator.TONE_DTMF_1,ToneGenerator.TONE_DTMF_2,ToneGenerator.TONE_DTMF_3}[soundHit],120);checkSoundSeq();}}break;
            case 33:
                if(a==MotionEvent.ACTION_DOWN){float[] lx={.18f,.39f,.61f,.82f};int logicHit=-1;for(int i=0;i<4;i++)if(Math.hypot(x-getWidth()*lx[i],y-getHeight()*.54f)<dp(62))logicHit=i;if(logicHit>=0){if(logicHit==logicTarget[logicPos]){logicPos++;if(logicPos==4)solved();}else{logicPos=0;showToast("ORDER RESET");}}}break;
            case 34:
                if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.14f,getHeight()*.36f,getWidth()*.86f,getHeight()*.58f)){rubbing=true;lastMoveX=x;}if(a==MotionEvent.ACTION_MOVE&&rubbing){rubProgress=Math.min(120,rubProgress+Math.abs(x-lastMoveX)*.08f+2);lastMoveX=x;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)rubbing=false;if(rubProgress>=100&&a==MotionEvent.ACTION_DOWN&&y>getHeight()*.58f){float rubTop=getHeight()*.62f;for(int rr=0;rr<3;rr++)for(int cc=0;cc<3;cc++){float kx=getWidth()*.22f+cc*getWidth()*.28f,ky=rubTop+rr*dp(62);if(Math.hypot(x-kx,y-ky)<dp(34)&&revealCode.length()<3)revealCode+=String.valueOf(rr*3+cc+1);}}break;
            case 35:
                if(a==MotionEvent.ACTION_DOWN){if(Math.hypot(x-getWidth()*.26f,y-getHeight()*.50f)<dp(65)){forgeGemTaps++;if(forgeGemTaps>=4)forgeMask|=2;}else if(Math.hypot(x-forgePanelX,y-getHeight()*.50f)<dp(75)){forgePanelDrag=true;keyDX=x-forgePanelX;}else if(forgeMask==15&&Math.hypot(x-forgeKeyX,y-forgeKeyY)<dp(75)){forgeKeyDrag=true;keyDX=x-forgeKeyX;keyDY=y-forgeKeyY;}}if(a==MotionEvent.ACTION_MOVE){if(forgePanelDrag){forgePanelX=x-keyDX;if(Math.abs(forgePanelX-getWidth()*.56f)>dp(110))forgeMask|=4;}if(forgeKeyDrag){forgeKeyX=x-keyDX;forgeKeyY=y-keyDY;}}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){forgePanelDrag=false;forgeKeyDrag=false;}break;
        }
        invalidate(); return true;
    }
}
