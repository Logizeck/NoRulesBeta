package com.norules.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.hardware.*;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.ToneGenerator;
import android.media.FaceDetector;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import java.util.*;

public class GameView extends View implements SensorEventListener {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SensorManager sm;
    private final Sensor accel;
    private final Sensor gyro;
    private final AudioManager audio;
    private final SharedPreferences prefs;
    private final float density, scaledDensity;

    private int screen = -1; // -1 language, 0 home, 1..50 levels, 90 level select, 95 simulated ad, 99 finish
    private long roomStart = System.currentTimeMillis();
    private String toast = "";
    private long toastUntil = 0;
    private int hints = 3;
    private boolean hintOpen = false;
    private static final int TOTAL_LEVELS = 50;
    private static final int LEVELS_PER_PAGE = 12;
    private int levelPage = 0;
    private static final int AREA_COUNT = (TOTAL_LEVELS + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE;
    private String languageCode = "en";
    private final int[] hintTier = new int[TOTAL_LEVELS+1];
    private long lastFakeAdAt = System.currentTimeMillis();
    private int levelsSinceFakeAd = 0;
    private long fakeAdShownAt = 0;
    private int pendingScreenAfterAd = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // sensors
    private float ax, ay, az;
    private float gx, gy, gz;
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
    private int snowEasterPhase = 0; // 0 normal, 1 warning, 2 eyes-pop, 3 melt, 4 fail
    private long snowEasterAt = 0;
    private float snowEye1VX=0, snowEye1VY=0, snowEye2VX=0, snowEye2VY=0;
    private boolean snowScreamPlayed=false;
    private MediaPlayer snowScreamPlayer;
    private boolean snowMeltdownAdShown=false;
    private boolean returnToSnowFailAfterAd=false;
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
    // replacements for former music puzzles
    private int assembleMask=0, assembleDrag=0;
    private final float[] assembleX=new float[3], assembleY=new float[3];
    private final int[] pinPos={0,0,0,0};
    private final int[] pinTarget={2,0,1,2};
    // L27 - selfie
    private Bitmap selfieBitmap;
    private long selfieCapturedAt=0;
    private boolean selfieRequested=false;
    private boolean selfieFaceFound=false;
    private float selfieFaceCx=.5f, selfieFaceCy=.43f, selfieEyeDist=.22f;

    // reusable drag-to-assemble key state for object-search rooms
    private final float[] foundPieceX={0,0,0}, foundPieceY={0,0,0};
    private int foundPlacedMask=0, foundDrag=0;
    private float foundDX, foundDY;
    private boolean finalKeyDrag=false;
    private float finalKeyX, finalKeyY;
    private float forgePullY; private boolean forgePullDrag=false;

    // visual asset + hidden surprise state
    private Bitmap snowmanArt;
    private int homeSecretTaps=0; private long homeSecretUntil=0;
    private int balloonSecretTaps=0; private long balloonSecretUntil=0;
    private long ninjaSecretUntil=0;

    // L28-L35 reworked interaction state
    private float deskMugX,deskMugY,deskBookX,deskBookY,deskDrawerX; private int deskDrag=0; private String deskCode="";
    private float mazeBallX,mazeBallY,mazeVX,mazeVY;
    private float pinataAngle=0,pinataAV=0; private int pinataHits=0; private long pinataFlashUntil=0;
    private float perspectiveX,perspectiveY; private boolean perspectiveDrag=false;
    private final float[] pinY={0,0,0,0}; private int pinDrag=-1;
    private final int[] soundSeq={0,2,1,0}; private final ArrayList<Integer> soundTaps=new ArrayList<>(); private boolean soundPlaying=false,soundReady=false;
    private final int[] logicTarget={1,3,0,2}; private int logicPos=0;
    private float rubProgress=0; private boolean rubbing=false; private String revealCode="";
    private int forgeMask=0; private int forgeShakeStart=0; private int forgeGemTaps=0; private float forgePanelX; private boolean forgePanelDrag=false; private long forgeTwoFingerStart=0; private float forgeKeyX,forgeKeyY; private boolean forgeKeyDrag=false;

    // L36-L50 new puzzle state
    private int neonSwitchStep=0;
    private float dialAngle=0,lastDialAngle=0; private boolean dialDrag=false;
    private final int[] circuitRot={0,0,0,0}; private final int[] circuitTarget={1,0,1,0};
    private float magnetX,magnetY,metalX,metalY; private boolean magnetDrag=false;
    private final int[] tileRot={0,0,0,0};
    private float flashlightX,flashlightY; private boolean flashlightDrag=false; private boolean hiddenMarkFound=false;
    private int balanceLeft=0,balanceRight=0;
    private int colorSeqPos=0; private final int[] colorSeq={2,0,3,1};
    private int mirrorStep=0; private final int[] mirrorSeq={1,3,0,2};
    private int wireMask=0;
    private String safeCode="";
    private int peelLayers=0;
    private int gridMemPos=0; private long gridShownAt=0; private boolean gridReady=false; private final int[] gridSeq={0,3,5,2};
    private long dualHoldStart=0;
    private int shadowChoice=-1;
    private int finalStage=0, finalTapCount=0; private float finalLeverY=0; private boolean finalLeverDrag=false;

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
            "KEY ASSEMBLY", "SELFIE TROUBLE", "MESSY DESK", "TILT MAZE", "PIÑATA PANIC", "PERSPECTIVE TRICK",
            "COLOR PISTONS", "ANGLE ORDER", "RUB IT OUT", "MASTER KEY ROOM",
            "NEON SWITCHES", "CIRCUIT LIGHT", "MAGNET HEIST", "ROTATE THE PATH", "FLASHLIGHT",
            "BALANCE IT", "COLOR ORDER", "MIRROR PANEL", "BROKEN CIRCUIT", "SECRET CODE",
            "PEEL THE POSTER", "MEMORY GRID", "TWO-HAND LOCK", "SHADOW DOORS", "CHAOS CORE"
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
            "Count first, then apply the tiny operation printed next to each row.",
            "Build his face: two stone eyes, carrot nose, then draw a curved smile with your finger.",
            "He always chooses the wrong door. Do the opposite of his choice.",
            "One hand opens the gate. The other controls gravity.",
            "Do not touch the key. Capture this moment with the phone itself.",
            "The boss combines things you already learned.",
            "The three fragments are parts of one key. Drag each one into its matching silhouette.",
            "The guard wants proof that you can look ridiculous. The camera may be more useful than the door.",
            "Move the objects to reveal three key pieces. Then drag every piece into its matching assembly slot.",
            "The pieces are under the toys. Move or provoke the toys, then drag the revealed pieces into the key outline.",
            "Each breakfast object hides a real piece. Reveal it, then physically drag it into the matching key slot.",
            "The two shapes belong together. Move the foreground piece until the perspective makes one clean symbol.",
            "Each colored piston moves directly under your finger. Drag every piston to its matching notch.",
            "Order the shapes by how many interior angles they have, from fewest to most.",
            "Rub the grime away until the hidden code is readable, then enter it.",
            "Slide the panel, crack the box, and pull the cyan tab down. Drag all three revealed pieces into the key outline, then use that same rebuilt key.",
            "The three switches flash in a sequence. Copy that exact order.",
            "Drag around the circular dial. Stop when the pointer aligns with the glowing notch.",
            "Move the magnet, not the metal piece. Bring the magnet close enough to pull it to the exit.",
            "Tap each tile to rotate it until the glowing path becomes continuous from left to right.",
            "Drag the flashlight. Something useful is hidden in the darkness.",
            "Tap the weights until both sides show the same total.",
            "Watch the colored lamps, then tap them back in the same order.",
            "The panel is mirrored. Read the arrows as their reflected directions.",
            "Activate all three broken circuit nodes. Each node must connect to the center.",
            "The shapes tell you three digits. Enter them on the keypad in left-to-right order.",
            "Swipe the poster repeatedly. There are several layers hiding something underneath.",
            "Memorize the four lit squares, then tap those squares in the same order.",
            "Keep both pads pressed at the same time until the lock finishes charging.",
            "The real door casts the only shadow that matches its frame. Tap that door.",
            "Open the latch, pull the lever, then hit the core three times. Every step is visible."
    };

    public GameView(Context c) {
        super(c);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        sm = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        audio = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        prefs = c.getSharedPreferences("no_rules_beta", Context.MODE_PRIVATE);
        hints = prefs.getInt("hints", 3);
        languageCode = prefs.getString("language", "");
        screen = languageCode.isEmpty() ? -1 : 0;
        snowmanArt = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("snowman_3d", "drawable", c.getPackageName()));
        int screamId=getResources().getIdentifier("snow_scream","raw",c.getPackageName());
        if(screamId!=0){try{snowScreamPlayer=MediaPlayer.create(c,screamId);}catch(Exception ignored){snowScreamPlayer=null;}}
        setKeepScreenOn(true);
        setBackgroundColor(C("#F7E8C6"));
    }

    public void onResumeGame(){ if(accel!=null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME); if(gyro!=null) sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME); lastFrame=System.nanoTime(); invalidate(); }
    public void onPauseGame(){ sm.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()==Sensor.TYPE_GYROSCOPE){gx=e.values[0];gy=e.values[1];gz=e.values[2];return;}
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
        p.setStyle(Paint.Style.FILL); p.setStrokeWidth(dp(1)); p.setTypeface(Typeface.create("sans-serif-rounded",Typeface.NORMAL)); p.setTextSize(sp(Math.max(size,20))); p.setColor(col); p.setTextAlign(align); c.drawText(s,x,y,p);
    }
    private void mangaTxt(Canvas c,String s,float x,float y,float size,int col,Paint.Align align){
        p.setStyle(Paint.Style.FILL); p.setStrokeWidth(dp(1)); p.setTypeface(Typeface.create("sans-serif-rounded",Typeface.BOLD)); p.setTextSize(sp(Math.max(size,21))); p.setColor(col); p.setTextAlign(align); c.drawText(s,x,y,p);
    }
    private void rr(Canvas c,float x,float y,float w,float h,float r,int col){ p.setStyle(Paint.Style.FILL); p.setColor(col); c.drawRoundRect(x,y,x+w,y+h,r,r,p); }
    private boolean in(float x,float y,float l,float t,float r,float b){ return x>=l&&x<=r&&y>=t&&y<=b; }

    private void wrap(Canvas c,String text,float cx,float top,float maxW,float size,int col){
        p.setStyle(Paint.Style.FILL); p.setStrokeWidth(dp(1)); p.setTypeface(Typeface.create("sans-serif-rounded",Typeface.NORMAL)); float readable=Math.max(size,20); p.setTextSize(sp(readable)); p.setTextAlign(Paint.Align.CENTER); p.setColor(col);
        String[] words=text.split(" "); String line=""; float y=top;
        for(String w:words){ String test=line.isEmpty()?w:line+" "+w; if(p.measureText(test)>maxW && !line.isEmpty()){ c.drawText(line,cx,y,p); y+=dp(readable*1.35f); line=w; } else line=test; }
        if(!line.isEmpty()) c.drawText(line,cx,y,p);
    }

    private Path cutPanel(float x,float y,float w,float h,float cut){
        Path q=new Path(); q.moveTo(x+cut,y); q.lineTo(x+w-cut,y); q.lineTo(x+w,y+cut); q.lineTo(x+w-cut,y+h); q.lineTo(x+cut,y+h); q.lineTo(x,y+h-cut); q.lineTo(x,y+cut); q.close(); return q;
    }
    private void neonPanel(Canvas c,float x,float y,float w,float h,int fill,int edge){
        Path sh=cutPanel(x+dp(6),y+dp(8),w,h,dp(16));p.setStyle(Paint.Style.FILL);p.setColor(C("#02040F"));c.drawPath(sh,p);
        Path q=cutPanel(x,y,w,h,dp(16));p.setColor(fill);c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(edge);c.drawPath(q,p);
        p.setStrokeWidth(dp(1));p.setColor(Color.argb(120,255,255,255));c.drawLine(x+dp(24),y+dp(7),x+w-dp(36),y+dp(7),p);
    }
    private void neonChip(Canvas c,float x,float y,float w,float h,int fill,int edge){
        Path q=cutPanel(x,y,w,h,dp(10));p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(edge);c.drawPath(q,p);
    }

    private void instruction(Canvas c,String text){
        float x=getWidth()*.085f,y=getHeight()*.785f,w=getWidth()*.83f,h=dp(74);
        rr(c,x+dp(6),y+dp(8),w,h,dp(20),C("#0A0F22"));
        rr(c,x,y,w,h,dp(20),C("#11183B"));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#39F7FF"));c.drawRoundRect(x,y,x+w,y+h,dp(20),dp(20),p);
        p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));c.drawCircle(x+dp(31),y+h/2,dp(18),p);mangaTxt(c,"!",x+dp(31),y+h/2+sp(7),19,C("#10142B"),Paint.Align.CENTER);
        wrap(c,text,x+w*.58f,y+dp(43),w-dp(90),19,Color.WHITE);
    }

    private void speedBg(Canvas c){
        c.drawColor(C("#050817"));
        float W=getWidth(),H=getHeight();
        // perspective arcade grid
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.argb(130,57,247,255));
        float horizon=H*.54f; for(int i=-7;i<=7;i++){float bx=W*.5f+i*W*.13f;c.drawLine(W*.5f,horizon,bx,H,p);}
        for(int i=0;i<8;i++){float t=i/8f;float yy=horizon+(H-horizon)*(t*t);c.drawLine(0,yy,W,yy,p);}
        // asymmetrical neon shards instead of old radial sunburst
        int[] cc={C("#FF2EA6"),C("#39F7FF"),C("#FFE94D"),C("#9D6CFF")};
        for(int i=0;i<14;i++){float x=((i*83)%100)/100f*W;float y=((i*47)%100)/100f*H*.72f;float len=dp(16+(i%4)*10);p.setColor(cc[i%4]);p.setStrokeWidth(dp(4));c.drawLine(x,y,x+((i%2==0)?len:-len*.6f),y-len*.55f,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(70,255,46,166));c.drawCircle(W*.50f,H*.52f,W*.23f,p);
        p.setColor(C("#39F7FF"));c.drawCircle(W*.92f,H*.33f,dp(8),p);p.setColor(C("#FF2EA6"));c.drawCircle(W*.07f,H*.62f,dp(10),p);
    }

    private void drawAreaBackground(Canvas c,int page){
        if(page==0) bgDojo(c); else if(page==1) bgWorkshop(c); else if(page==2) bgDigital(c); else if(page==3) bgArcade(c); else bgLab(c);
    }

    private void drawThemeBackground(Canvas c,int level){
        if(level==21) bgWinter(c);
        else if(level==27) bgPhotoBooth(c);
        else if(level==9||level==12||level==13||level==14||level==15||level==22) bgDojo(c);
        else if(level==1||level==3||level==7||level==8||level==11||level==16||level==17||level==24||level==26||level==28||level==29||level==30||level==35||level==40||level==44||level==46) bgWorkshop(c);
        else if(level>=36&&level<=43) bgArcade(c);
        else if(level>=45) bgLab(c);
        else bgDigital(c);
    }

    private void bgDigital(Canvas c){
        c.drawColor(C("#050817")); float W=getWidth(),H=getHeight();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.argb(120,57,247,255));for(int i=0;i<9;i++){float y=H*.30f+i*dp(62);c.drawLine(0,y,W,y,p);}for(int i=0;i<7;i++){float x=W*(i/6f);c.drawLine(x,H*.28f,W*.5f,H,p);}p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(60,255,46,166));c.drawCircle(W*.74f,H*.56f,W*.24f,p);p.setColor(C("#DFFF00"));c.drawCircle(W*.11f,H*.68f,dp(9),p);
    }
    private void bgWorkshop(Canvas c){
        c.drawColor(C("#0A0B18")); float W=getWidth(),H=getHeight();
        p.setStyle(Paint.Style.FILL);p.setColor(C("#16162A"));c.drawRect(0,H*.28f,W,H,p);p.setColor(C("#22213A"));for(int r=0;r<7;r++)for(int col=0;col<5;col++){float x=col*W*.24f+(r%2)*W*.12f,y=H*.30f+r*dp(74);c.drawRect(x,y,x+W*.20f,y+dp(56),p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#FF2EA6"));c.drawLine(0,H*.72f,W,H*.63f,p);p.setColor(C("#39F7FF"));c.drawLine(0,H*.79f,W,H*.70f,p);p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));c.drawCircle(W*.88f,H*.56f,dp(7),p);
    }
    private void bgWinter(Canvas c){
        c.drawColor(C("#07172B")); float W=getWidth(),H=getHeight();
        p.setStyle(Paint.Style.FILL);p.setColor(C("#153D66"));c.drawRect(0,H*.26f,W,H,p);p.setColor(C("#E9FBFF"));c.drawOval(-W*.1f,H*.67f,W*.55f,H*.93f,p);c.drawOval(W*.35f,H*.70f,W*1.1f,H*.96f,p);p.setColor(C("#39F7FF"));for(int i=0;i<24;i++){float x=((i*37)%100)/100f*W,y=H*.28f+((i*61)%100)/100f*H*.46f;c.drawCircle(x,y,dp(2+(i%3)),p);}p.setColor(C("#FF2EA6"));c.drawCircle(W*.88f,H*.38f,dp(8),p);
    }
    private void bgPhotoBooth(Canvas c){
        c.drawColor(C("#16071E")); float W=getWidth(),H=getHeight();
        int[] bars={C("#FF2EA6"),C("#39F7FF"),C("#FFE94D"),C("#9D6CFF")};p.setStyle(Paint.Style.FILL);for(int i=0;i<8;i++){p.setColor(Color.argb(80,Color.red(bars[i%4]),Color.green(bars[i%4]),Color.blue(bars[i%4])));c.drawRect(i*W/8f,H*.27f,(i+1)*W/8f,H,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#39F7FF"));c.drawRoundRect(W*.13f,H*.31f,W*.87f,H*.72f,dp(24),dp(24),p);p.setColor(C("#FFE94D"));c.drawCircle(W*.50f,H*.31f,dp(12),p);
    }
    private void bgDojo(Canvas c){
        c.drawColor(C("#0B0A13")); float W=getWidth(),H=getHeight();
        p.setStyle(Paint.Style.FILL);p.setColor(C("#EEE9DF"));c.drawRect(0,H*.26f,W,H,p);p.setColor(C("#E02E3D"));c.drawCircle(W*.77f,H*.48f,W*.20f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#111111"));for(int i=0;i<10;i++){float y=H*.33f+i*dp(65);c.drawLine(0,y,W,y-dp(35),p);}p.setColor(C("#FF2EA6"));c.drawLine(W*.08f,H*.65f,W*.34f,H*.54f,p);p.setColor(C("#39F7FF"));c.drawLine(W*.66f,H*.76f,W*.92f,H*.61f,p);
    }


    private void bgArcade(Canvas c){c.drawColor(C("#10031D"));float W=getWidth(),H=getHeight();p.setStyle(Paint.Style.FILL);for(int i=0;i<10;i++){p.setColor(i%2==0?C("#23104A"):C("#090D1E"));c.drawRect(0,H*.27f+i*dp(58),W,H*.27f+(i+1)*dp(58),p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#DFFF00"));c.drawCircle(W*.18f,H*.58f,dp(70),p);p.setColor(C("#FF2EA6"));c.drawCircle(W*.82f,H*.68f,dp(95),p);p.setColor(C("#39F7FF"));c.drawLine(0,H*.48f,W,H*.73f,p);}
    private void bgLab(Canvas c){c.drawColor(C("#03151A"));float W=getWidth(),H=getHeight();p.setStyle(Paint.Style.FILL);p.setColor(C("#082A31"));c.drawRect(0,H*.28f,W,H,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));for(int i=0;i<8;i++){p.setColor(i%2==0?C("#39F7FF"):C("#DFFF00"));float x=W*(.08f+i*.12f);c.drawLine(x,H*.31f,x-dp(30),H*.92f,p);}p.setColor(C("#FF2EA6"));c.drawRoundRect(W*.18f,H*.48f,W*.82f,H*.70f,dp(28),dp(28),p);}
    private void header(Canvas c,String subtitle){
        float x=getWidth()*.04f,y=getHeight()*.018f,w=getWidth()*.92f,h=getHeight()*.145f;
        neonPanel(c,x,y,w,h,C("#111936"),C("#39F7FF"));
        c.save();c.rotate(-4f,x+dp(52),y+dp(22));neonChip(c,x+dp(12),y+dp(7),dp(86),dp(31),C("#FF2EA6"),C("#FFE94D"));c.restore();
        mangaTxt(c,"#"+screen,x+dp(55),y+dp(30),15,Color.WHITE,Paint.Align.CENTER);
        mangaTxt(c,levelNames[screen],getWidth()/2f,y+dp(57),24,Color.WHITE,Paint.Align.CENTER);
        wrap(c,subtitle,getWidth()/2f,y+dp(94),w-dp(44),18,C("#DDE6FF"));
        button(c,T("MENU"),getWidth()*.045f,getHeight()*.177f,getWidth()*.225f,dp(50),C("#39F7FF"));
        button(c,T("HINT")+" "+hints,getWidth()*.730f,getHeight()*.177f,getWidth()*.225f,dp(50),C("#FF5BAA"));
    }

    private void chibi(Canvas c,float x,float y,float scale,boolean lookLeft,boolean angry){
        float r=dp(43)*scale;
        Path head=new Path();head.moveTo(x-r*.75f,y-r*.95f);head.quadTo(x-r*1.08f,y-r*.15f,x-r*.72f,y+r*.78f);head.quadTo(x,y+r*1.06f,x+r*.72f,y+r*.78f);head.quadTo(x+r*1.08f,y-r*.15f,x+r*.75f,y-r*.95f);head.quadTo(x,y-r*1.15f,x-r*.75f,y-r*.95f);head.close();
        p.setStyle(Paint.Style.FILL);p.setColor(C("#39F7FF"));c.drawPath(head,p);c.save();c.clipPath(head);p.setColor(C("#FF2EA6"));c.drawRect(x,y-r*1.2f,x+r*1.2f,y+r*1.2f,p);c.restore();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4)*scale);p.setColor(C("#090D1E"));c.drawPath(head,p);
        float eyeY=y-dp(5)*scale,off=lookLeft?-dp(5)*scale:dp(5)*scale;
        p.setStyle(Paint.Style.FILL);p.setColor(C("#F7F8FF"));c.drawOval(x-dp(27)*scale,eyeY-dp(12)*scale,x-dp(3)*scale,eyeY+dp(12)*scale,p);c.drawOval(x+dp(3)*scale,eyeY-dp(12)*scale,x+dp(27)*scale,eyeY+dp(12)*scale,p);
        p.setColor(C("#090D1E"));c.drawCircle(x-dp(15)*scale+off,eyeY,dp(6)*scale,p);c.drawCircle(x+dp(15)*scale+off,eyeY,dp(6)*scale,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4)*scale);p.setColor(C("#FFE94D"));if(angry){c.drawLine(x-dp(18)*scale,y+dp(21)*scale,x+dp(18)*scale,y+dp(8)*scale,p);}else{c.drawArc(x-dp(17)*scale,y+dp(5)*scale,x+dp(17)*scale,y+dp(27)*scale,0,180,false,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(C("#DFFF00"));c.drawCircle(x-r*.83f,y+r*.18f,dp(5)*scale,p);
    }

    private void speech(Canvas c,String s,float x,float y,float w,float h){
        rr(c,x+dp(5),y+dp(6),w,h,dp(22),C("#060A19"));rr(c,x,y,w,h,dp(22),C("#F7F7FF"));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#39F7FF"));c.drawRoundRect(x,y,x+w,y+h,dp(22),dp(22),p);
        Path t=new Path();t.moveTo(x+w*.29f,y+h-dp(1));t.lineTo(x+w*.43f,y+h-dp(1));t.lineTo(x+w*.34f,y+h+dp(24));t.close();p.setStyle(Paint.Style.FILL);p.setColor(C("#F7F7FF"));c.drawPath(t,p);p.setStyle(Paint.Style.STROKE);p.setColor(C("#39F7FF"));p.setStrokeWidth(dp(3));c.drawPath(t,p);
        wrap(c,s,x+w/2,y+dp(34),w-dp(28),17,C("#11183B"));
    }

    private void showToast(String s){ toast=s; toastUntil=System.currentTimeMillis()+900; }
    private void useHint(){
        if(screen<1||screen>TOTAL_LEVELS) return;
        if(hintOpen) return;
        if(hints<=0){ showToast("No hints left — shop comes later!"); return; }
        hints--; prefs.edit().putInt("hints",hints).apply(); hintTier[screen]=Math.min(3,hintTier[screen]+1); hintOpen=true; invalidate();
    }


    private String activeHint(){
        int tier=(screen>=0&&screen<hintTier.length)?Math.max(1,hintTier[screen]):1;
        String base=hintsText[screen];
        if(tier==1)return base;
        if(tier==2)return base+" Focus on the object or gesture named in the clue; decorative elements do not block the solution.";
        return base+" This is the direct hint: perform exactly that interaction on the visible target, then watch for the success feedback.";
    }
    private void drawHintOverlay(Canvas c){
        if(!hintOpen || screen<1 || screen>TOTAL_LEVELS) return;
        p.setColor(Color.argb(185,4,6,18)); p.setStyle(Paint.Style.FILL); c.drawRect(0,0,getWidth(),getHeight(),p);
        float x=getWidth()*.07f, y=getHeight()*.24f, w=getWidth()*.86f, h=getHeight()*.38f;
        rr(c,x+dp(6),y+dp(8),w,h,dp(24),C("#040716"));
        rr(c,x,y,w,h,dp(24),C("#11183B"));
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(4)); p.setColor(C("#39F7FF")); c.drawRoundRect(x,y,x+w,y+h,dp(24),dp(24),p);
        mangaTxt(c,"HINT",x+dp(24),y+dp(48),24,C("#FFE94D"),Paint.Align.LEFT);
        wrap(c,activeHint(),x+w/2,y+dp(100),w-dp(48),21,Color.WHITE);
        rr(c,x+w-dp(62),y+dp(16),dp(46),dp(46),dp(14),C("#FF2EA6"));
        mangaTxt(c,"×",x+w-dp(39),y+dp(49),26,Color.WHITE,Paint.Align.CENTER);
        button(c,"CLOSE",x+w*.25f,y+h-dp(72),w*.50f,dp(52),C("#FFE94D"));
    }

    private void gotoLevel(int n){
        screen=n; roomStart=System.currentTimeMillis(); toast=""; hintOpen=false; shakeCount=0; stillScore=0; resetLevel(); ((MainActivity)getContext()).syncMusicForScreen(screen); invalidate();
    }
    private void solved(){
        int completed=screen;
        prefs.edit().putBoolean("level_"+completed,true).putLong("time_"+completed,System.currentTimeMillis()-roomStart).apply();
        levelsSinceFakeAd++;
        int next=completed+1; if(next>TOTAL_LEVELS) next=99;
        long now=System.currentTimeMillis();
        boolean adDue = levelsSinceFakeAd>=5 || now-lastFakeAdAt>=180000L;
        if(adDue){
            pendingScreenAfterAd=next; screen=95; fakeAdShownAt=now; lastFakeAdAt=now; levelsSinceFakeAd=0; toast=""; hintOpen=false; ((MainActivity)getContext()).syncMusicForScreen(95); invalidate();
        }else{
            showToast("CLEAR!  ✦"); screen=next; if(screen>=1&&screen<=TOTAL_LEVELS){roomStart=now;resetLevel();} ((MainActivity)getContext()).syncMusicForScreen(screen);
        }
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
        snowDrag=0; snowEye1=false; snowEye2=false; snowNose=false; snowSmile=false; drawingSmile=false; snowSmilePoints.clear(); snowEasterPhase=0; snowEasterAt=0; snowEye1VX=snowEye1VY=snowEye2VX=snowEye2VY=0; snowScreamPlayed=false;
        assembleMask=0; assembleDrag=0; assembleX[0]=getWidth()*.20f; assembleY[0]=getHeight()*.68f; assembleX[1]=getWidth()*.48f; assembleY[1]=getHeight()*.76f; assembleX[2]=getWidth()*.77f; assembleY[2]=getHeight()*.68f; for(int i=0;i<4;i++)pinPos[i]=0;
        oppositeChoice=-1; gatePressed=false; screenshotCaptured=false;
        bossPhase=0; bossSealRevealed=false; bossBothStart=0;
        rhythmPlaying=false; rhythmReady=false; rhythmTaps.clear(); selfieBitmap=null; selfieCapturedAt=0; selfieRequested=false;
        deskMugX=getWidth()*.62f; deskMugY=getHeight()*.49f; deskBookX=getWidth()*.76f; deskBookY=getHeight()*.39f; deskDrawerX=getWidth()*.14f; deskDrag=0; deskCode="";
        mazeBallX=getWidth()*.16f; mazeBallY=getHeight()*.68f; mazeVX=mazeVY=0;
        pinataAngle=0;pinataAV=0;pinataHits=0;pinataFlashUntil=0;
        perspectiveX=getWidth()*.72f;perspectiveY=getHeight()*.62f;perspectiveDrag=false;
        pinDrag=-1; for(int i=0;i<4;i++)pinY[i]=getHeight()*.65f;
        lastMoveX=0;
        soundTaps.clear(); soundPlaying=false; soundReady=false; logicPos=0; rubProgress=0; rubbing=false; revealCode="";
        forgeMask=0; forgeShakeStart=shakeCount; forgeGemTaps=0; forgePanelX=getWidth()*.56f; forgePanelDrag=false; forgeTwoFingerStart=0; forgeKeyX=getWidth()*.5f; forgeKeyY=getHeight()*.70f; forgeKeyDrag=false; forgePullY=getHeight()*.43f; forgePullDrag=false;
        foundPlacedMask=0;foundDrag=0;finalKeyDrag=false;finalKeyX=getWidth()*.50f;finalKeyY=keySlotY();for(int i=0;i<3;i++){foundPieceX[i]=getWidth()*.5f;foundPieceY[i]=getHeight()*.5f;}
        neonSwitchStep=0;dialAngle=0;dialDrag=false;for(int i=0;i<4;i++)circuitRot[i]=(i+1)%2;magnetX=getWidth()*.25f;magnetY=getHeight()*.65f;metalX=getWidth()*.72f;metalY=getHeight()*.48f;magnetDrag=false;for(int i=0;i<4;i++)tileRot[i]=i%3;flashlightX=getWidth()*.28f;flashlightY=getHeight()*.65f;flashlightDrag=false;hiddenMarkFound=false;balanceLeft=0;balanceRight=0;colorSeqPos=0;mirrorStep=0;wireMask=0;safeCode="";peelLayers=0;gridMemPos=0;gridShownAt=System.currentTimeMillis();gridReady=false;dualHoldStart=0;shadowChoice=-1;finalStage=0;finalTapCount=0;finalLeverY=getHeight()*.44f;finalLeverDrag=false;
    }
    private void resetBall(){ ballX=getWidth()*.18f; ballY=getHeight()*.70f; vx=vy=0; lastFrame=System.nanoTime(); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(screen==-1) drawLanguageSelect(c); else if(screen==0) drawHome(c); else if(screen==90) drawLevelSelect(c); else if(screen==95) drawFakeAd(c); else if(screen==99) drawFinish(c); else drawLevel(c);
        drawHintOverlay(c);
        if(!toast.isEmpty() && System.currentTimeMillis()<toastUntil){ rr(c,getWidth()*.08f,getHeight()*.86f,getWidth()*.84f,dp(72),dp(18),C("#111936")); wrap(c,toast,getWidth()/2f,getHeight()*.86f+dp(30),getWidth()*.76f,16,Color.WHITE); }
        postInvalidateOnAnimation();
    }


    private String T(String k){
        String l=languageCode==null?"en":languageCode;
        if(k.equals("PLAY")){if(l.equals("de"))return "SPIELEN";if(l.equals("it"))return "GIOCA";if(l.equals("es"))return "JUGAR";if(l.equals("pt"))return "JOGAR";if(l.equals("fr"))return "JOUER";if(l.equals("zh"))return "开始";if(l.equals("ja"))return "プレイ";if(l.equals("ru"))return "ИГРАТЬ";}
        if(k.equals("LEVEL_SELECT")){if(l.equals("de"))return "LEVELAUSWAHL";if(l.equals("it"))return "SELEZIONE LIVELLO";if(l.equals("es"))return "NIVELES";if(l.equals("pt"))return "NÍVEIS";if(l.equals("fr"))return "NIVEAUX";if(l.equals("zh"))return "关卡选择";if(l.equals("ja"))return "レベル選択";if(l.equals("ru"))return "ВЫБОР УРОВНЯ";}
        if(k.equals("MENU")){if(l.equals("zh"))return "菜单";if(l.equals("ja"))return "メニュー";if(l.equals("ru"))return "МЕНЮ";return "MENU";}
        if(k.equals("HINT")){if(l.equals("de"))return "TIPP";if(l.equals("it"))return "AIUTO";if(l.equals("es"))return "PISTA";if(l.equals("pt"))return "DICA";if(l.equals("fr"))return "INDICE";if(l.equals("zh"))return "提示";if(l.equals("ja"))return "ヒント";if(l.equals("ru"))return "ПОДСКАЗКА";}
        if(k.equals("BACK")){if(l.equals("de"))return "ZURÜCK";if(l.equals("it"))return "INDIETRO";if(l.equals("es"))return "ATRÁS";if(l.equals("pt"))return "VOLTAR";if(l.equals("fr"))return "RETOUR";if(l.equals("zh"))return "返回";if(l.equals("ja"))return "戻る";if(l.equals("ru"))return "НАЗАД";}
        if(k.equals("RETRY")){if(l.equals("de"))return "NOCHMAL";if(l.equals("it"))return "RIPROVA";if(l.equals("es"))return "REINTENTAR";if(l.equals("pt"))return "TENTAR DE NOVO";if(l.equals("fr"))return "RÉESSAYER";if(l.equals("zh"))return "重试";if(l.equals("ja"))return "もう一度";if(l.equals("ru"))return "ЕЩЁ РАЗ";}
        if(k.equals("SNOW_NO")){if(l.equals("de"))return "Nein! Bloß nicht da!";if(l.equals("it"))return "No! Lì proprio no!";if(l.equals("es"))return "¡No! ¡Ahí no!";if(l.equals("pt"))return "Não! Aí não!";if(l.equals("fr"))return "Non ! Surtout pas là !";if(l.equals("zh"))return "不行！那里绝对不行！";if(l.equals("ja"))return "ダメ！そこは絶対ダメ！";if(l.equals("ru"))return "Нет! Только не туда!";return "No! Definitely not there!";}
        if(k.equals("CHOOSE_LANGUAGE")){if(l.equals("it"))return "SCEGLI LA LINGUA";return "CHOOSE LANGUAGE";}
        return k;
    }

    private void drawLanguageSelect(Canvas c){
        bgDigital(c);float W=getWidth(),H=getHeight();
        mangaTxt(c,"NO RULES",W*.5f,H*.105f,42,C("#FFE94D"),Paint.Align.CENTER);
        mangaTxt(c,"CHOOSE LANGUAGE",W*.5f,H*.165f,25,Color.WHITE,Paint.Align.CENTER);
        String[] labels={"English","Deutsch","Italiano","Español","Português","Français","中文","日本語","Русский"};
        String[] codes={"en","de","it","es","pt","fr","zh","ja","ru"};
        int cols=2;float bw=W*.39f,bh=dp(62),gap=dp(14),sx=W*.075f,sy=H*.23f;
        for(int i=0;i<labels.length;i++){int row=i/cols,col=i%cols;float x=sx+col*(bw+W*.07f),y=sy+row*(bh+gap);button(c,labels[i],x,y,bw,bh,(i%3==0)?C("#39F7FF"):(i%3==1)?C("#FF5BAA"):C("#FFE94D"));}
        wrap(c,"Language can be changed later in settings.",W*.5f,H*.84f,W*.78f,16,C("#DDE6FF"));
    }

    private void drawHome(Canvas c){
        speedBg(c); float W=getWidth(),H=getHeight();
        // logo built from offset neon slabs, intentionally irregular
        c.save();c.rotate(-5f,W*.5f,H*.16f);neonChip(c,W*.13f,H*.085f,W*.33f,dp(86),C("#39F7FF"),C("#FF2EA6"));c.restore();
        c.save();c.rotate(4f,W*.5f,H*.16f);neonChip(c,W*.42f,H*.10f,W*.45f,dp(90),C("#FF2EA6"),C("#FFE94D"));c.restore();
        if(System.currentTimeMillis()<homeSecretUntil){c.save();float wig=(float)Math.sin(System.currentTimeMillis()/55.0)*dp(7);c.translate(wig,0);mangaTxt(c,"NO  RUL—HEY!",W*.5f,H*.175f,39,Color.WHITE,Paint.Align.CENTER);c.restore();}else mangaTxt(c,"NO RULES",W*.5f,H*.175f,46,Color.WHITE,Paint.Align.CENTER);
        mangaTxt(c,"THINK WEIRD.",W*.5f,H*.245f,20,C("#FFE94D"),Paint.Align.CENTER);
        // weird floating key + door instead of old generic chibi/speech composition
        door(c,W*.53f,H*.43f,W*.30f,H*.25f);
        c.save();c.rotate(-17f,W*.29f,H*.46f);drawCompleteKey(c,W*.29f,H*.46f,.72f);c.restore();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#FF2EA6"));c.drawCircle(W*.82f,H*.39f,dp(28),p);p.setColor(C("#39F7FF"));c.drawLine(W*.79f,H*.39f,W*.85f,H*.39f,p);c.drawLine(W*.82f,H*.36f,W*.82f,H*.42f,p);
        mangaTxt(c,"?",W*.82f,H*.405f,31,C("#FFE94D"),Paint.Align.CENTER);
        button(c,T("PLAY"),W*.17f,H*.68f,W*.66f,dp(70),C("#DFFF00"));
        button(c,T("LEVEL_SELECT"),W*.17f,H*.79f,W*.66f,dp(62),C("#39F7FF"));
    }

    private void button(Canvas c,String s,float x,float y,float w,float h,int col){
        Path shadow=cutPanel(x+dp(6),y+dp(7),w,h,dp(14));p.setStyle(Paint.Style.FILL);p.setColor(C("#02040F"));c.drawPath(shadow,p);
        Path q=cutPanel(x,y,w,h,dp(14));p.setColor(col);c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#0B1030"));c.drawPath(q,p);
        p.setStrokeWidth(dp(2));p.setColor(Color.argb(120,255,255,255));c.drawLine(x+dp(20),y+dp(9),x+w-dp(24),y+dp(9),p);
        mangaTxt(c,s,x+w/2,y+h*.65f,20,C("#090D1E"),Paint.Align.CENTER);
    }

    private void drawLevelSelect(Canvas c){
        drawAreaBackground(c,levelPage);
        String[] names={"STARTER MAYHEM","SNEAKY OBJECTS","WEIRD EXPERIMENTS","NEON ARCADE","CHAOS LAB"};
        String[] subs={"Hands, gravity, timing & trouble","Hidden clues, phone tricks & misdirection","Multi-step rooms, camera chaos & locks","Patterns, dials, magnets & visual tricks","Layered logic, memory & multi-step finales"};
        mangaTxt(c,names[levelPage],getWidth()/2f,getHeight()*.060f,29,Color.WHITE,Paint.Align.CENTER);
        wrap(c,subs[levelPage],getWidth()/2f,getHeight()*.095f,getWidth()*.80f,16,C("#D7E8FF"));
        neonChip(c,getWidth()*.39f,getHeight()*.125f,getWidth()*.22f,dp(30),C("#111936"),C("#FFE94D"));
        mangaTxt(c,"AREA "+(levelPage+1)+" / "+AREA_COUNT,getWidth()/2f,getHeight()*.145f,14,C("#FFE94D"),Paint.Align.CENTER);
        int startLevel=levelPage*LEVELS_PER_PAGE+1;
        int cols=3, rows=4; float gap=dp(12); float cell=(getWidth()-dp(48)-gap*(cols-1))/cols; float top=getHeight()*.19f;
        for(int slot=0;slot<LEVELS_PER_PAGE;slot++){
            int level=startLevel+slot; int row=slot/cols,col=slot%cols; float x=dp(24)+col*(cell+gap), y=top+row*(cell+gap*1.08f);
            if(level>TOTAL_LEVELS){Path empty=cutPanel(x,y,cell,cell,dp(12));p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(70,20,25,58));c.drawPath(empty,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.argb(100,255,46,166));c.drawPath(empty,p);mangaTxt(c,"?",x+cell/2,y+cell*.60f,20,C("#68709A"),Paint.Align.CENTER);continue;}
            boolean done=prefs.getBoolean("level_"+level,false);float wob=((slot%3)-1)*dp(2);Path q=cutPanel(x,y+wob,cell,cell,dp(12));p.setStyle(Paint.Style.FILL);p.setColor(done?C("#DFFF00"):C("#111936"));c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(done?C("#39F7FF"):C("#FF2EA6"));c.drawPath(q,p);mangaTxt(c,String.valueOf(level),x+cell/2,y+wob+cell*.61f,20,done?C("#080B18"):Color.WHITE,Paint.Align.CENTER);
        }
        if(levelPage>0) button(c,"‹",getWidth()*.06f,getHeight()*.82f,getWidth()*.18f,dp(56),C("#39F7FF"));
        if(levelPage<AREA_COUNT-1) button(c,"›",getWidth()*.76f,getHeight()*.82f,getWidth()*.18f,dp(56),C("#FF5BAA"));
        button(c,T("BACK"),getWidth()*.30f,getHeight()*.90f,getWidth()*.40f,dp(56),C("#FFE94D"));
    }

    private void drawFakeAd(Canvas c){
        c.drawColor(C("#070A18")); float W=getWidth(),H=getHeight();
        p.setStyle(Paint.Style.FILL);p.setColor(C("#21114A"));c.drawCircle(W*.5f,H*.42f,W*.42f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#39F7FF"));for(int i=0;i<10;i++){float yy=H*.20f+i*dp(30);c.drawLine(W*.10f,yy,W*.90f,yy,p);}
        neonChip(c,W*.09f,H*.055f,W*.82f,dp(74),C("#FF2EA6"),C("#FFE94D"));
        mangaTxt(c,"SIMULATED AD",W*.5f,H*.102f,28,Color.WHITE,Paint.Align.CENTER);
        mangaTxt(c,"BANANA INSURANCE™",W*.5f,H*.245f,31,C("#FFE94D"),Paint.Align.CENTER);
        mangaTxt(c,"FOR WHEN LIFE GETS SLIPPERY",W*.5f,H*.29f,17,C("#D7E8FF"),Paint.Align.CENTER);
        p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));Path banana=new Path();banana.moveTo(W*.30f,H*.50f);banana.quadTo(W*.50f,H*.63f,W*.73f,H*.44f);banana.quadTo(W*.56f,H*.52f,W*.36f,H*.42f);banana.close();c.drawPath(banana,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#FF2EA6"));c.drawPath(banana,p);
        mangaTxt(c,"100% FAKE AD — TEST ONLY",W*.5f,H*.67f,17,C("#39F7FF"),Paint.Align.CENTER);
        long left=Math.max(0,3000L-(System.currentTimeMillis()-fakeAdShownAt));
        if(left>0){neonChip(c,W*.25f,H*.77f,W*.50f,dp(60),C("#111936"),C("#6670A1"));mangaTxt(c,"CLOSE IN "+((left+999)/1000),W*.5f,H*.81f,20,C("#AAB4D7"),Paint.Align.CENTER);}else{button(c,"CLOSE ×",W*.25f,H*.77f,W*.50f,dp(60),C("#39F7FF"));}
    }

    private void drawFinish(Canvas c){
        speedBg(c); mangaTxt(c,"TEST COMPLETE!",getWidth()/2f,getHeight()*.25f,40,Color.WHITE,Paint.Align.CENTER); mangaTxt(c,"YOU SURVIVED THIS BUILD",getWidth()/2f,getHeight()*.32f,23,C("#FFE94D"),Paint.Align.CENTER); chibi(c,getWidth()*.50f,getHeight()*.49f,1.5f,false,false); wrap(c,"Now we test: which rooms are fun, confusing, too easy, or impossible?",getWidth()/2f,getHeight()*.64f,getWidth()*.78f,18,C("#D7E8FF")); button(c,"LEVEL SELECT",getWidth()*.18f,getHeight()*.79f,getWidth()*.64f,dp(62),C("#39F7FF"));
    }

    private void drawLevel(Canvas c){
        drawThemeBackground(c,screen);
        switch(screen){
            case 1: l1(c); break; case 2:l2(c);break; case 3:l3(c);break; case 4:l4(c);break; case 5:l5(c);break;
            case 6:l6(c);break; case 7:l7(c);break; case 8:l8(c);break; case 9:l9(c);break; case 10:l10(c);break;
            case 11:l11(c);break; case 12:l12(c);break; case 13:l13(c);break; case 14:l14(c);break; case 15:l15(c);break;
            case 16:l16(c);break; case 17:l17(c);break; case 18:l18(c);break; case 19:l19(c);break; case 20:l20(c);break;
            case 21:l21(c);break; case 22:l22(c);break; case 23:l23(c);break; case 24:l24(c);break; case 25:l25(c);break;
            case 26:l26(c);break; case 27:l27(c);break; case 28:l28(c);break; case 29:l29(c);break; case 30:l30(c);break;
            case 31:l31(c);break; case 32:l32(c);break; case 33:l33(c);break; case 34:l34(c);break; case 35:l35(c);break;
            case 36:l36(c);break; case 37:l37(c);break; case 38:l38(c);break; case 39:l39(c);break; case 40:l40(c);break;
            case 41:l41(c);break; case 42:l42(c);break; case 43:l43(c);break; case 44:l44(c);break; case 45:l45(c);break;
            case 46:l46(c);break; case 47:l47(c);break; case 48:l48(c);break; case 49:l49(c);break; case 50:l50(c);break;
        }
    }

    private void door(Canvas c,float x,float y,float w,float h){
        float l=x-w/2,t=y-h/2;Path sh=cutPanel(l+dp(7),t+dp(9),w,h,dp(18));p.setStyle(Paint.Style.FILL);p.setColor(C("#01030B"));c.drawPath(sh,p);
        Path q=cutPanel(l,t,w,h,dp(18));p.setColor(C("#151D45"));c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#FF2EA6"));c.drawPath(q,p);
        p.setStrokeWidth(dp(3));p.setColor(C("#39F7FF"));c.drawLine(l+w*.24f,t+dp(18),l+w*.24f,t+h-dp(18),p);c.drawLine(l+w*.72f,t+dp(18),l+w*.72f,t+h-dp(18),p);
        p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));c.drawCircle(l+w*.78f,t+h*.52f,dp(11),p);p.setColor(C("#070B1D"));c.drawCircle(l+w*.78f,t+h*.52f,dp(4),p);c.drawRect(l+w*.78f-dp(2),t+h*.52f,l+w*.78f+dp(2),t+h*.52f+dp(12),p);
        mangaTxt(c,"NR",l+w*.48f,t+h*.24f,14,C("#39F7FF"),Paint.Align.CENTER);
    }

    private void l1(Canvas c){ header(c,"That key is ridiculously dramatic."); door(c,getWidth()*.5f,getHeight()*.42f,getWidth()*.30f,getHeight()*.28f); c.save(); c.translate(keyX,keyY); c.scale(keyScale,keyScale); drawCompleteKey(c,0,0,.82f); c.restore(); instruction(c,"Pinch smaller, then drag the key into the door."); if(keyScale<.48f&&Math.hypot(keyX-getWidth()*.5f,keyY-getHeight()*.42f)<dp(55)) solved(); }

    private void l2(Canvas c){ header(c,"The floor is listening to gravity."); float L=getWidth()*.09f,R=getWidth()*.91f,T=getHeight()*.28f,B=getHeight()*.76f; rr(c,L,T,R-L,B-T,dp(22),Color.WHITE); long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*55*dt;vy+=ay*55*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float rad=dp(17);if(ballX<L+rad){ballX=L+rad;vx*=-.55f;}if(ballX>R-rad){ballX=R-rad;vx*=-.55f;}if(ballY<T+rad){ballY=T+rad;vy*=-.55f;}if(ballY>B-rad){ballY=B-rad;vy*=-.55f;}float tx=getWidth()*.76f,ty=getHeight()*.35f;p.setColor(C("#FFE94D"));p.setStyle(Paint.Style.FILL);c.drawCircle(tx,ty,dp(28),p);mangaTxt(c,"★",tx,ty+sp(9),24,C("#7D5A00"),Paint.Align.CENTER);p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,rad,p); if(Math.hypot(ballX-tx,ballY-ty)<dp(26)) solved(); }

    private void l3(Canvas c){ header(c,"The key is trapped inside a stubborn balloon."); float x=getWidth()*.5f,y=getHeight()*.53f;if(balloonHold>0&&!balloonPopped){float t=Math.min(1,(System.currentTimeMillis()-balloonHold)/1500f);balloonR=dp(48+38*t);if(t>=1){balloonPopped=true;showToast("BANG!!");}} if(!balloonPopped){p.setColor(C("#FF6C9B"));p.setStyle(Paint.Style.FILL);c.drawOval(x-balloonR*.82f,y-balloonR,x+balloonR*.82f,y+balloonR,p);mangaTxt(c,"KEY",x,y+sp(7),16,C("#772942"),Paint.Align.CENTER);if(System.currentTimeMillis()<balloonSecretUntil)mangaTxt(c,"ಠ_ಠ",x,y-sp(20),22,C("#EAF0FF"),Paint.Align.CENTER);}else{drawCompleteKey(c,x,y,.58f);if(System.currentTimeMillis()-roomStart>700) solved();} mangaTxt(c,"PRESS...",getWidth()/2f,getHeight()*.79f,22,C("#FF5BAA"),Paint.Align.CENTER); }

    private void l4(Canvas c){ header(c,"The door hates needy players."); door(c,getWidth()*.5f,getHeight()*.49f,getWidth()*.38f,getHeight()*.36f); long e=System.currentTimeMillis()-calmStart; int rem=Math.max(0,7-(int)(e/1000)); mangaTxt(c,rem>0?String.valueOf(rem):"...",getWidth()/2f,getHeight()*.75f,38,C("#EAF0FF"),Paint.Align.CENTER); if(e>=7000) solved(); }

    private void l5(Canvas c){ header(c,"This little guy says: 'LEFT! Definitely LEFT!'"); chibi(c,getWidth()*.5f,getHeight()*.39f,1.3f,false,true); speech(c,"LEFT! TRUST ME!",getWidth()*.19f,getHeight()*.50f,getWidth()*.62f,dp(92)); button(c,"LEFT",getWidth()*.10f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#FF8D7C")); button(c,"RIGHT",getWidth()*.56f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#39F7FF")); wrap(c,"His eyes might be more honest than his mouth.",getWidth()/2f,getHeight()*.84f,getWidth()*.82f,16,C("#CBD4EA")); if(liarChoice==1) solved(); }

    private void l6(Canvas c){ header(c,"Three eyes. Zero privacy."); float[][] eyes={{.30f,.48f},{.50f,.58f},{.70f,.48f}};int active=0;for(int i=0;i<3;i++){float ex=getWidth()*eyes[i][0],ey=getHeight()*eyes[i][1];boolean covered=false;for(int k=0;k<pointerCount;k++)if(Math.hypot(px[k]-ex,py[k]-ey)<dp(48))covered=true;if(covered)active++;p.setColor(covered?C("#FFE94D"):Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawOval(ex-dp(42),ey-dp(26),ex+dp(42),ey+dp(26),p);p.setColor(C("#EAF0FF"));c.drawCircle(ex,ey,covered?dp(5):dp(12),p);} mangaTxt(c,"HIDE THEM",getWidth()/2f,getHeight()*.76f,24,C("#FF5BAA"),Paint.Align.CENTER); if(active==3) solved(); }

    private void l7(Canvas c){ header(c,"The vending machine ate your key."); rr(c,getWidth()*.27f,getHeight()*.30f,getWidth()*.46f,getHeight()*.42f,dp(24),C("#77A7DD")); mangaTxt(c,"KEY",getWidth()*.5f,getHeight()*.42f,28,C("#FFE94D"),Paint.Align.CENTER); mangaTxt(c,"ガタガタ!",getWidth()*.5f,getHeight()*.79f,28,C("#FF5BAA"),Paint.Align.CENTER); if(shakeCount>=3){shakeDropped=true;} if(shakeDropped){ drawCompleteKey(c,getWidth()*.5f,getHeight()*.72f,.52f); if(System.currentTimeMillis()-roomStart>600) solved();} }

    private void l8(Canvas c){ header(c,"The key is stuck to the ceiling."); door(c,getWidth()*.5f,getHeight()*.70f,getWidth()*.32f,getHeight()*.23f); drawCompleteKey(c,getWidth()*.5f,fallY,.52f); boolean inverted=ay<-6.2f; if(inverted) fallY=Math.min(getHeight()*.68f,fallY+dp(8)); if(fallY>=getHeight()*.65f) solved(); }

    private void l9(Canvas c){ header(c,"A sleeping ninja guards the door."); chibi(c,getWidth()*.5f,getHeight()*.48f,1.25f,false,System.currentTimeMillis()<ninjaSecretUntil); mangaTxt(c,"Z Z Z",getWidth()*.68f,getHeight()*.36f,28,C("#6B65B5"),Paint.Align.CENTER); mangaTxt(c,"TOK  TOK  TOK",getWidth()/2f,getHeight()*.72f,26,C("#FF5BAA"),Paint.Align.CENTER); mangaTxt(c,knockCount+" / 3",getWidth()/2f,getHeight()*.80f,20,C("#EAF0FF"),Paint.Align.CENTER); if(knockCount>=3) solved(); }

    private void l10(Canvas c){ header(c,"The seals only trust teamwork."); float y=getHeight()*.52f;seal(c,getWidth()*.32f,y,"A");seal(c,getWidth()*.68f,y,"B");boolean a=false,b=false;for(int i=0;i<pointerCount;i++){if(Math.hypot(px[i]-getWidth()*.32f,py[i]-y)<dp(60))a=true;if(Math.hypot(px[i]-getWidth()*.68f,py[i]-y)<dp(60))b=true;}if(a&&b){if(bothHoldStart==0)bothHoldStart=System.currentTimeMillis();if(System.currentTimeMillis()-bothHoldStart>850)solved();}else bothHoldStart=0;mangaTxt(c,"HOLD BOTH",getWidth()/2f,getHeight()*.75f,24,C("#FF5BAA"),Paint.Align.CENTER); }
    private void seal(Canvas c,float x,float y,String s){p.setStyle(Paint.Style.FILL);p.setColor(C("#E84D61"));c.drawCircle(x,y,dp(50),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#EAF0FF"));c.drawCircle(x,y,dp(50),p);mangaTxt(c,s,x,y+sp(9),25,Color.WHITE,Paint.Align.CENTER);}

    private void l11(Canvas c){ header(c,"Someone put a sticker over the important part."); door(c,getWidth()*.5f,getHeight()*.50f,getWidth()*.42f,getHeight()*.38f); mangaTxt(c,"OPEN",getWidth()*.5f,getHeight()*.51f,22,Color.WHITE,Paint.Align.CENTER); rr(c,stickerX-dp(75),stickerY-dp(50),dp(150),dp(100),dp(10),C("#FFE94D")); mangaTxt(c,"SALE!",stickerX,stickerY+sp(9),24,C("#EAF0FF"),Paint.Align.CENTER); if(stickerX<0||stickerX>getWidth()||stickerY<0||stickerY>getHeight()) solved(); }

    private void l12(Canvas c){ header(c,"One clean samurai slash."); chibi(c,getWidth()*.28f,getHeight()*.55f,1.0f,false,false);p.setColor(C("#EAF0FF"));p.setStrokeWidth(dp(6));p.setStyle(Paint.Style.STROKE);c.drawLine(getWidth()*.38f,getHeight()*.68f,getWidth()*.76f,getHeight()*.36f,p);mangaTxt(c,"シュッ!",getWidth()*.70f,getHeight()*.68f,28,C("#FF5BAA"),Paint.Align.CENTER); if(slashDone) solved(); }

    private void l13(Canvas c){ header(c,"The manga librarian demands TOTAL SILENCE."); chibi(c,getWidth()*.5f,getHeight()*.46f,1.25f,false,true); mangaTxt(c,"SHHH!!",getWidth()/2f,getHeight()*.66f,34,C("#FF5BAA"),Paint.Align.CENTER); int vol=audio.getStreamVolume(AudioManager.STREAM_MUSIC); int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC); wrap(c,"Media volume: "+vol+" / "+max,getWidth()/2f,getHeight()*.77f,getWidth()*.80f,18,C("#EAF0FF")); if(vol==0) solved(); }

    private void l14(Canvas c){ header(c,"Become a statue. Seriously."); chibi(c,getWidth()*.5f,getHeight()*.48f,1.45f,false,false); if(stillScore>2.5f){if(stillStart==0)stillStart=System.currentTimeMillis();}else stillStart=0; long t=stillStart==0?0:System.currentTimeMillis()-stillStart; mangaTxt(c,(t/1000)+" / 4",getWidth()/2f,getHeight()*.76f,28,C("#FF5BAA"),Paint.Align.CENTER); if(t>4000) solved(); }

    private void l15(Canvas c){ header(c,"Lean LEFT... then RIGHT. Screen stays portrait."); mangaTxt(c,rotatePhase==0?"← LEFT":"RIGHT →",getWidth()/2f,getHeight()*.52f,38,C("#EAF0FF"),Paint.Align.CENTER); if(rotatePhase==0&&ax<-6.0f)rotatePhase=1; if(rotatePhase==1&&ax>6.0f)solved(); }

    private void l16(Canvas c){ header(c,"The door is too small for your ego."); c.save();c.translate(getWidth()*.5f,getHeight()*.52f);c.scale(doorScale,doorScale);door(c,0,0,getWidth()*.22f,getHeight()*.20f);c.restore();mangaTxt(c,"MAKE IT BIG",getWidth()/2f,getHeight()*.78f,24,C("#FF5BAA"),Paint.Align.CENTER); if(doorScale>2.25f) solved(); }

    private void l17(Canvas c){ header(c,"Dialogue is blocking the evidence."); drawCompleteKey(c,getWidth()*.50f,getHeight()*.48f,.56f); speech(c,"I am extremely important dialogue!",bubbleX,bubbleY,getWidth()*.56f,dp(120)); if(bubbleX>getWidth()*.78f||bubbleX+getWidth()*.56f<getWidth()*.20f||bubbleY>getHeight()*.72f) solved(); }

    private void l18(Canvas c){ header(c,"The lock wants GREEN. You only have two colors."); orb(c,blueX,blueY,C("#4F8FEA"));orb(c,yellowX,yellowY,C("#FFE94D"));float tx=getWidth()*.5f,ty=getHeight()*.38f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(C("#4BAA78"));c.drawCircle(tx,ty,dp(42),p);if(Math.hypot(blueX-yellowX,blueY-yellowY)<dp(65)){greenMade=true;blueX=yellowX=(blueX+yellowX)/2;blueY=yellowY=(blueY+yellowY)/2;}if(greenMade){orb(c,blueX,blueY,C("#55B979"));if(Math.hypot(blueX-tx,blueY-ty)<dp(52))solved();}}
    private void orb(Canvas c,float x,float y,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawCircle(x,y,dp(42),p);p.setColor(Color.WHITE);c.drawCircle(x-dp(11),y-dp(10),dp(7),p);}

    private void l19(Canvas c){ header(c,"Watch the faces. Then repeat them."); long now=System.currentTimeMillis(); if(memoryPhase==0&&now-memoryShownAt>2400)memoryPhase=1; float[] xs={.18f,.39f,.61f,.82f}; for(int i=0;i<4;i++){float x=getWidth()*xs[i],y=getHeight()*.55f;int col=C("#F7C7A7");p.setColor(col);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,dp(34),p);mangaTxt(c,new String[]{"😠","😴","😎","😱"}[i],x,y+sp(12),28,C("#EAF0FF"),Paint.Align.CENTER);} if(memoryPhase==0){ mangaTxt(c,""+(memorySeq[0]+1)+"  "+(memorySeq[1]+1)+"  "+(memorySeq[2]+1)+"  "+(memorySeq[3]+1),getWidth()/2f,getHeight()*.72f,28,C("#FF5BAA"),Paint.Align.CENTER);}else mangaTxt(c,"YOUR TURN  "+memoryPos+"/4",getWidth()/2f,getHeight()*.72f,22,C("#FF5BAA"),Paint.Align.CENTER); }

    private void l20(Canvas c){
        header(c,"Count them, then decode them.");
        float W=getWidth(),H=getHeight();
        mangaTxt(c,"🐱  🐱  🐱",W*.42f,H*.33f,28,C("#EAF0FF"),Paint.Align.CENTER);mangaTxt(c,"+ 1",W*.77f,H*.33f,21,C("#39F7FF"),Paint.Align.CENTER);
        mangaTxt(c,"★  ★  ★  ★",W*.42f,H*.40f,28,C("#FFE94D"),Paint.Align.CENTER);mangaTxt(c,"× 2",W*.77f,H*.40f,21,C("#FF5BAA"),Paint.Align.CENTER);
        mangaTxt(c,"⚔  ⚔",W*.42f,H*.47f,28,C("#EAF0FF"),Paint.Align.CENTER);mangaTxt(c,"− 1",W*.77f,H*.47f,21,C("#DFFF00"),Paint.Align.CENTER);
        mangaTxt(c,"APPLY EACH RULE AFTER COUNTING",W*.50f,H*.545f,16,C("#D7E8FF"),Paint.Align.CENTER);
        drawKeypad(c);mangaTxt(c,code.isEmpty()?"_ _ _":code,W*.50f,H*.60f,27,C("#FF5BAA"),Paint.Align.CENTER);
        if(code.equals("481"))solved();if(code.length()>=3&&!code.equals("481")){showToast("NOPE");code="";}
    }
    private void drawKeypad(Canvas c){drawKeypadAt(c,getHeight()*.62f);}
    private void drawKeypadAt(Canvas c,float top){int n=1;for(int r=0;r<3;r++)for(int col=0;col<3;col++){float x=getWidth()*.22f+col*getWidth()*.28f,y=top+r*dp(62);neonChip(c,x-dp(30),y-dp(25),dp(60),dp(50),C("#111936"),C("#39F7FF"));mangaTxt(c,String.valueOf(n++),x,y+sp(7),18,Color.WHITE,Paint.Align.CENTER);}}

    private void drawCarrot(Canvas c,float x,float y,float sc){
        c.save();c.translate(x,y);Path body=new Path();body.moveTo(-dp(34)*sc,-dp(15)*sc);body.quadTo(dp(7)*sc,-dp(20)*sc,dp(50)*sc,0);body.quadTo(dp(7)*sc,dp(20)*sc,-dp(34)*sc,dp(15)*sc);body.close();p.setStyle(Paint.Style.FILL);p.setColor(C("#FF7A18"));c.drawPath(body,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3)*sc);p.setColor(C("#FFB13B"));c.drawLine(-dp(15)*sc,-dp(9)*sc,dp(3)*sc,-dp(5)*sc,p);c.drawLine(-dp(8)*sc,dp(7)*sc,dp(12)*sc,dp(4)*sc,p);p.setStyle(Paint.Style.FILL);p.setColor(C("#39F77A"));for(int i=-1;i<=1;i++){Path leaf=new Path();leaf.moveTo(-dp(34)*sc,0);leaf.quadTo(-dp(52)*sc,dp(11*i)*sc,-dp(58)*sc,dp(25*i)*sc);leaf.quadTo(-dp(42)*sc,dp(14*i)*sc,-dp(31)*sc,dp(5*i)*sc);leaf.close();c.drawPath(leaf,p);}c.restore();
    }
    private void playSnowScream(){
        if(snowScreamPlayer==null||snowScreamPlayed) return;
        snowScreamPlayed=true;
        try{snowScreamPlayer.seekTo(0);snowScreamPlayer.start();}catch(Exception ignored){}
    }

    private void l21(Canvas c){
        header(c,"Build his face. Try not to traumatize him.");
        float left=getWidth()*.18f,top=getHeight()*.30f,right=getWidth()*.82f,bottom=getHeight()*.75f,cx=(left+right)/2f,h=bottom-top,headY=top+h*.30f;
        long now=System.currentTimeMillis();
        float t=(now-snowEasterAt)/1000f;

        // base snowman. During the melt the body collapses vertically into one puddle.
        if(snowEasterPhase<3){
            c.save();
            if(snowEasterPhase==1||snowEasterPhase==2)c.translate((float)Math.sin((now-snowEasterAt)/28.0)*dp(4),0);
            if(snowmanArt!=null)c.drawBitmap(snowmanArt,new Rect(0,0,snowmanArt.getWidth(),snowmanArt.getHeight()),new RectF(left,top,right,bottom),p);
            c.restore();
        }else if(snowEasterPhase==3){
            float mt=Math.min(1f,(now-snowEasterAt)/1500f);
            if(snowmanArt!=null){
                c.save();
                c.translate(0,dp(118)*mt);
                c.scale(1f+.06f*mt,Math.max(.08f,1f-.88f*mt),cx,bottom);
                p.setAlpha((int)(255*(1f-.42f*mt)));
                c.drawBitmap(snowmanArt,new Rect(0,0,snowmanArt.getWidth(),snowmanArt.getHeight()),new RectF(left,top,right,bottom),p);
                p.setAlpha(255);
                c.restore();
            }
            p.setStyle(Paint.Style.FILL);p.setColor(C("#D9F7FF"));
            c.drawOval(cx-dp(70)-dp(95)*mt,bottom-dp(4),cx+dp(70)+dp(95)*mt,bottom+dp(28)+dp(22)*mt,p);
            // props fall naturally rather than deforming with the body
            c.save();c.rotate(38*mt,cx+dp(58),bottom-dp(36));drawCarrot(c,cx+dp(45)+dp(42)*mt,bottom-dp(78)+dp(92)*mt,.58f);c.restore();
            p.setColor(C("#201D31"));c.drawOval(cx-dp(72),bottom-dp(13)+dp(42)*mt,cx-dp(18),bottom+dp(13)+dp(42)*mt,p);
        }

        // face pieces: the stones stay exactly where the player placed them inside the legal eye zones.
        if(snowEasterPhase<2){
            if(snowEye1){p.setStyle(Paint.Style.FILL);p.setColor(C("#1B1B25"));c.drawCircle(stone1X,stone1Y,dp(13),p);p.setColor(Color.WHITE);c.drawCircle(stone1X-dp(4),stone1Y-dp(4),dp(3),p);}
            if(snowEye2){p.setStyle(Paint.Style.FILL);p.setColor(C("#1B1B25"));c.drawCircle(stone2X,stone2Y,dp(13),p);p.setColor(Color.WHITE);c.drawCircle(stone2X-dp(4),stone2Y-dp(4),dp(3),p);}
        }else if(snowEasterPhase==2){
            float et=Math.min(1f,(now-snowEasterAt)/520f);
            float e1x=stone1X+snowEye1VX*et, e1y=stone1Y+snowEye1VY*et+dp(42)*et*et;
            float e2x=stone2X+snowEye2VX*et, e2y=stone2Y+snowEye2VY*et+dp(42)*et*et;
            p.setStyle(Paint.Style.FILL);p.setColor(C("#1B1B25"));c.drawCircle(e1x,e1y,dp(13)*(1f-.15f*et),p);c.drawCircle(e2x,e2y,dp(13)*(1f-.15f*et),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#FFE94D"));c.drawLine(stone1X,stone1Y,e1x,e1y,p);c.drawLine(stone2X,stone2Y,e2x,e2y,p);
        }

        if(snowNose&&snowEasterPhase<3)drawCarrot(c,carrotX,carrotY,.60f);
        if(!snowSmilePoints.isEmpty()&&snowEasterPhase<3){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(C("#151522"));Path sm=new Path();PointF f=snowSmilePoints.get(0);sm.moveTo(f.x,f.y);for(int i=1;i<snowSmilePoints.size();i++){PointF q=snowSmilePoints.get(i);sm.lineTo(q.x,q.y);}c.drawPath(sm,p);p.setStrokeCap(Paint.Cap.BUTT);}

        if(snowEasterPhase==1){
            float bt=Math.min(1f,(now-snowEasterAt)/650f);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb((int)(90+150*bt),255,45,95));
            c.drawCircle(cx-dp(46),headY+dp(20),dp(10)+dp(12)*bt,p);c.drawCircle(cx+dp(46),headY+dp(20),dp(10)+dp(12)*bt,p);
            speech(c,T("SNOW_NO"),getWidth()*.12f,getHeight()*.255f,getWidth()*.76f,dp(92));
            if(t>.58f){playSnowScream();snowEasterPhase=2;snowEasterAt=now;snowEye1VX=-dp(190);snowEye1VY=-dp(150);snowEye2VX=dp(210);snowEye2VY=-dp(165);}
        }else if(snowEasterPhase==2){
            if(now-snowEasterAt>560){snowEasterPhase=3;snowEasterAt=now;}
        }else if(snowEasterPhase==3){
            if(now-snowEasterAt>1550){
                snowEasterPhase=4;snowEasterAt=now;
                if(!snowMeltdownAdShown){
                    snowMeltdownAdShown=true;returnToSnowFailAfterAd=true;pendingScreenAfterAd=21;screen=95;fakeAdShownAt=now;lastFakeAdAt=now;levelsSinceFakeAd=0;((MainActivity)getContext()).syncMusicForScreen(95);return;
                }
            }
        }

        if(snowEasterPhase==0){
            if(!snowNose)drawCarrot(c,carrotX,carrotY,.72f);
            if(!snowEye1){p.setStyle(Paint.Style.FILL);p.setColor(C("#25242D"));c.drawCircle(stone1X,stone1Y,dp(16),p);}
            if(!snowEye2){p.setColor(C("#25242D"));c.drawCircle(stone2X,stone2Y,dp(16),p);}
            mangaTxt(c,"MAKE HIM A FACE",getWidth()/2f,getHeight()*.875f,19,Color.WHITE,Paint.Align.CENTER);
        }
        if(snowEasterPhase==4){
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(225,4,6,18));c.drawRect(0,0,getWidth(),getHeight(),p);
            mangaTxt(c,"TOTAL MELTDOWN",getWidth()/2f,getHeight()*.37f,35,C("#FF2EA6"),Paint.Align.CENTER);
            wrap(c,"That was definitely the wrong end.",getWidth()/2f,getHeight()*.45f,getWidth()*.78f,21,Color.WHITE);
            button(c,T("RETRY"),getWidth()*.18f,getHeight()*.58f,getWidth()*.64f,dp(66),C("#DFFF00"));button(c,T("MENU"),getWidth()*.18f,getHeight()*.68f,getWidth()*.64f,dp(60),C("#39F7FF"));
        }
        if(snowEye1&&snowEye2&&snowNose&&snowSmile&&snowEasterPhase==0)solved();
    }
    private void l22(Canvas c){ header(c,"He always chooses the WRONG door."); chibi(c,getWidth()*.5f,getHeight()*.40f,1.25f,true,true); speech(c,"I PICK LEFT!",getWidth()*.22f,getHeight()*.49f,getWidth()*.56f,dp(95)); button(c,"LEFT",getWidth()*.10f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#FF8D7C")); button(c,"RIGHT",getWidth()*.56f,getHeight()*.70f,getWidth()*.34f,dp(64),C("#39F7FF")); wrap(c,"If his choice is always wrong, which door is safe?",getWidth()/2f,getHeight()*.84f,getWidth()*.82f,19,C("#3F4156")); if(oppositeChoice==1)solved(); }

    private void l23(Canvas c){ header(c,"One finger opens the gate. Gravity does the rest.");float L=getWidth()*.08f,R=getWidth()*.92f,T=getHeight()*.30f,B=getHeight()*.74f;rr(c,L,T,R-L,B-T,dp(20),Color.WHITE);float gateX=getWidth()*.55f;p.setColor(C("#FF5BAA"));p.setStyle(Paint.Style.FILL);if(!gatePressed)c.drawRect(gateX,T,gateX+dp(14),B,p);button(c,"HOLD",getWidth()*.08f,getHeight()*.78f,getWidth()*.28f,dp(58),C("#FFE94D"));long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*50*dt;vy+=ay*50*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;float r=dp(16);if(ballX<L+r)ballX=L+r;if(ballX>R-r)ballX=R-r;if(ballY<T+r)ballY=T+r;if(ballY>B-r)ballY=B-r;if(!gatePressed&&ballX>gateX-r&&ballX<gateX+dp(26)&&ballY>T&&ballY<B){ballX=gateX-r;vx*=-.5f;}p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,r,p);float tx=getWidth()*.82f,ty=getHeight()*.38f;p.setColor(C("#55B979"));c.drawCircle(tx,ty,dp(25),p);if(Math.hypot(ballX-tx,ballY-ty)<dp(28))solved(); }

    private void l24(Canvas c){
        header(c,"This key refuses to leave the screen.");
        drawCompleteKey(c,getWidth()*.50f,getHeight()*.47f,.70f);
        speedBurst(c,getWidth()*.50f,getHeight()*.47f,dp(105));
        speech(c,"YOU CAN LOOK... BUT DON'T TOUCH!",getWidth()*.12f,getHeight()*.59f,getWidth()*.76f,dp(110));
        mangaTxt(c,"TAKE IT WITH YOU",getWidth()/2f,getHeight()*.77f,25,C("#FF5BAA"),Paint.Align.CENTER);
        if(android.os.Build.VERSION.SDK_INT < 34){
            wrap(c,"Screenshot detection needs Android 14+ in this test build.",getWidth()/2f,getHeight()*.84f,getWidth()*.80f,18,C("#CBD4EA"));
        }
        if(screenshotCaptured) solved();
    }

    private void l25(Canvas c){ header(c,"FINAL BOSS: three lessons, one room."); if(bossPhase==0){rr(c,getWidth()*.27f,getHeight()*.34f,getWidth()*.46f,getHeight()*.30f,dp(20),C("#77A7DD"));mangaTxt(c,"SHAKE",getWidth()/2f,getHeight()*.52f,30,C("#EAF0FF"),Paint.Align.CENTER);if(shakeCount>=3){bossSealRevealed=true;bossPhase=1;showToast("SEALS REVEALED!");}} else if(bossPhase==1){float y=getHeight()*.50f;seal(c,getWidth()*.34f,y,"1");seal(c,getWidth()*.66f,y,"2");boolean a=false,b=false;for(int i=0;i<pointerCount;i++){if(Math.hypot(px[i]-getWidth()*.34f,py[i]-y)<dp(60))a=true;if(Math.hypot(px[i]-getWidth()*.66f,py[i]-y)<dp(60))b=true;}if(a&&b){if(bossBothStart==0)bossBothStart=System.currentTimeMillis();if(System.currentTimeMillis()-bossBothStart>800){bossPhase=2;resetBall();showToast("FINAL ORB!");}}else bossBothStart=0;} else {float tx=getWidth()*.5f,ty=getHeight()*.40f;long n=System.nanoTime();float dt=Math.min(.033f,(n-lastFrame)/1_000_000_000f);lastFrame=n;vx+=ax*55*dt;vy+=ay*55*dt;vx*=.992f;vy*=.992f;ballX+=vx;ballY+=vy;ballX=Math.max(dp(20),Math.min(getWidth()-dp(20),ballX));ballY=Math.max(getHeight()*.28f,Math.min(getHeight()*.78f,ballY));p.setColor(C("#FFE94D"));c.drawCircle(tx,ty,dp(32),p);p.setColor(C("#42405F"));c.drawCircle(ballX,ballY,dp(18),p);if(Math.hypot(ballX-tx,ballY-ty)<dp(30))solved();} mangaTxt(c,"PHASE "+(bossPhase+1)+" / 3",getWidth()/2f,getHeight()*.78f,22,C("#FF5BAA"),Paint.Align.CENTER); }

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
        header(c,"Three physical fragments. One exact silhouette.");float ty=getHeight()*.50f;float[] tx={getWidth()*.34f,getWidth()*.50f,getWidth()*.66f};
        if(assembleMask!=7){for(int i=0;i<3;i++){drawKeyFragment(c,tx[i],ty,i,1f,true);if((assembleMask&(1<<i))==0){if(Math.hypot(assembleX[i]-tx[i],assembleY[i]-ty)<dp(90)){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#DFFF00"));c.drawCircle(tx[i],ty,dp(42),p);}drawKeyFragment(c,assembleX[i],assembleY[i],i,1f,false);}else drawKeyFragment(c,tx[i],ty,i,1f,false);}mangaTxt(c,"DRAG EACH PIECE INTO THE MATCHING CUTOUT",getWidth()/2f,getHeight()*.36f,17,Color.WHITE,Paint.Align.CENTER);}else{drawCompleteKey(c,getWidth()*.50f,ty,.85f);door(c,getWidth()*.82f,getHeight()*.67f,getWidth()*.18f,getHeight()*.19f);mangaTxt(c,"ONE KEY. NO DUPLICATES.",getWidth()/2f,getHeight()*.78f,18,C("#DFFF00"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>850)solved();}
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
        header(c,"The guard only respects ridiculous faces.");float cx=getWidth()/2f;
        if(selfieBitmap==null){
            // framing guide before external camera opens
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#39F7FF"));c.drawOval(getWidth()*.27f,getHeight()*.31f,getWidth()*.73f,getHeight()*.60f,p);
            p.setColor(C("#FF2EA6"));c.drawLine(getWidth()*.35f,getHeight()*.44f,getWidth()*.65f,getHeight()*.44f,p);
            mangaTxt(c,"CENTER YOUR FACE",cx,getHeight()*.64f,17,C("#FFE94D"),Paint.Align.CENTER);
            button(c,selfieRequested?"OPENING CAMERA...":"TAKE A SELFIE",getWidth()*.20f,getHeight()*.70f,getWidth()*.60f,dp(66),C("#DFFF00"));
        }else{
            float left=getWidth()*.14f,top=getHeight()*.27f,right=getWidth()*.86f,bottom=getHeight()*.70f;neonPanel(c,left-dp(8),top-dp(8),right-left+dp(16),bottom-top+dp(16),C("#11183B"),C("#39F7FF"));Rect src=new Rect(0,0,selfieBitmap.getWidth(),selfieBitmap.getHeight());RectF dst=new RectF(left,top,right,bottom);c.drawBitmap(selfieBitmap,src,dst,p);
            float fx=left+(right-left)*selfieFaceCx, fy=top+(bottom-top)*selfieFaceCy, ed=(right-left)*selfieEyeDist;
            if(!selfieFaceFound){fx=cx;fy=top+(bottom-top)*.42f;ed=(right-left)*.21f;}
            // deliberately ridiculous party glasses: star lens + spiral lens, aligned to detected eyes
            float gy=fy,gr=ed*.52f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setStrokeJoin(Paint.Join.ROUND);
            Path star=new Path();for(int k=0;k<10;k++){double aa=-Math.PI/2+k*Math.PI/5;float rr=(k%2==0?gr:gr*.45f);float sx=fx-ed+(float)Math.cos(aa)*rr,sy=gy+(float)Math.sin(aa)*rr;if(k==0)star.moveTo(sx,sy);else star.lineTo(sx,sy);}star.close();p.setColor(C("#DFFF00"));c.drawPath(star,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(95,57,247,255));c.drawPath(star,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(C("#FF2EA6"));p.setStrokeWidth(dp(8));c.drawCircle(fx+ed,gy,gr,p);Path spiral=new Path();for(int k=0;k<30;k++){float tt=k/29f;double aa=tt*Math.PI*4;float rr=gr*(1f-tt*.82f);float sx=fx+ed+(float)Math.cos(aa)*rr,sy=gy+(float)Math.sin(aa)*rr;if(k==0)spiral.moveTo(sx,sy);else spiral.lineTo(sx,sy);}c.drawPath(spiral,p);p.setColor(C("#39F7FF"));c.drawLine(fx-ed*.35f,gy,fx+ed*.35f,gy,p);p.setStrokeJoin(Paint.Join.MITER);
            // curled cartoon moustache under the detected nose
            float my=fy+ed*.98f;p.setStyle(Paint.Style.FILL);p.setColor(C("#7B2CFF"));Path m=new Path();m.moveTo(fx,my);m.cubicTo(fx-ed*.22f,my-ed*.38f,fx-ed*.88f,my-ed*.30f,fx-ed*1.12f,my+ed*.10f);m.cubicTo(fx-ed*.86f,my+ed*.07f,fx-ed*.52f,my+ed*.42f,fx,my+ed*.13f);m.cubicTo(fx+ed*.52f,my+ed*.42f,fx+ed*.86f,my+ed*.07f,fx+ed*1.12f,my+ed*.10f);m.cubicTo(fx+ed*.88f,my-ed*.30f,fx+ed*.22f,my-ed*.38f,fx,my);c.drawPath(m,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#FFE94D"));c.drawPath(m,p);
            mangaTxt(c,selfieFaceFound?"FACE LOCKED. STYLE DESTROYED.":"CLOSE ENOUGH. CHAOS WINS.",cx,getHeight()*.76f,18,C("#FFE94D"),Paint.Align.CENTER);if(selfieCapturedAt>0&&System.currentTimeMillis()-selfieCapturedAt>2800)solved();
        }
    }

    private void detectSelfieFace(Bitmap src){
        selfieFaceFound=false;if(src==null)return;try{Bitmap b=src.copy(Bitmap.Config.RGB_565,true);FaceDetector fd=new FaceDetector(b.getWidth(),b.getHeight(),1);FaceDetector.Face[] faces=new FaceDetector.Face[1];int n=fd.findFaces(b,faces);if(n>0&&faces[0]!=null){PointF mid=new PointF();faces[0].getMidPoint(mid);selfieFaceCx=mid.x/b.getWidth();selfieFaceCy=mid.y/b.getHeight();selfieEyeDist=Math.max(.09f,Math.min(.34f,faces[0].eyesDistance()/b.getWidth()));selfieFaceFound=true;}}catch(Exception ignored){}
    }

    public void onSelfieCaptured(Bitmap bitmap){
        selfieRequested=false;
        if(screen==27 && bitmap!=null){selfieBitmap=bitmap;detectSelfieFace(bitmap);selfieCapturedAt=System.currentTimeMillis();showToast("STYLE UPGRADE!");invalidate();}
    }

    public void onSelfieCancelled(){ selfieRequested=false; if(screen==27){showToast("CAMERA CANCELLED");invalidate();} }


    private void keyPiece(Canvas c,float x,float y,int idx){drawKeyFragment(c,x,y,idx,1f,false);}    
    private void drawKeyFragment(Canvas c,float x,float y,int idx,float sc,boolean ghost){
        int fill=ghost?C("#273052"):C("#FFE94D"),edge=ghost?C("#5B668B"):C("#0A0F22"),hi=ghost?C("#455174"):C("#39F7FF");
        p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        if(idx==0){
            // angular bow: unmistakably the head of the same key
            Path q=new Path();float r=dp(27)*sc;q.moveTo(x-r,y);q.lineTo(x-r*.45f,y-r*.90f);q.lineTo(x+r*.45f,y-r*.90f);q.lineTo(x+r,y);q.lineTo(x+r*.45f,y+r*.90f);q.lineTo(x-r*.45f,y+r*.90f);q.close();p.setStyle(Paint.Style.FILL);p.setColor(edge);c.drawPath(q,p);c.save();c.scale(.82f,.82f,x,y);p.setColor(fill);c.drawPath(q,p);c.restore();p.setColor(C("#090D1E"));c.drawCircle(x,y,dp(10)*sc,p);
        } else if(idx==1){
            p.setStyle(Paint.Style.FILL);p.setColor(edge);c.drawRoundRect(x-dp(36)*sc,y-dp(12)*sc,x+dp(36)*sc,y+dp(12)*sc,dp(6)*sc,dp(6)*sc,p);p.setColor(fill);c.drawRoundRect(x-dp(32)*sc,y-dp(7)*sc,x+dp(32)*sc,y+dp(7)*sc,dp(4)*sc,dp(4)*sc,p);p.setColor(hi);c.drawRoundRect(x-dp(27)*sc,y-dp(5)*sc,x+dp(24)*sc,y-dp(2)*sc,dp(2)*sc,dp(2)*sc,p);
        } else {
            Path q=new Path();q.moveTo(x-dp(34)*sc,y-dp(11)*sc);q.lineTo(x+dp(8)*sc,y-dp(11)*sc);q.lineTo(x+dp(8)*sc,y-dp(2)*sc);q.lineTo(x+dp(20)*sc,y-dp(2)*sc);q.lineTo(x+dp(20)*sc,y+dp(17)*sc);q.lineTo(x+dp(9)*sc,y+dp(17)*sc);q.lineTo(x+dp(9)*sc,y+dp(9)*sc);q.lineTo(x-dp(4)*sc,y+dp(9)*sc);q.lineTo(x-dp(4)*sc,y+dp(17)*sc);q.lineTo(x-dp(17)*sc,y+dp(17)*sc);q.lineTo(x-dp(17)*sc,y+dp(8)*sc);q.lineTo(x-dp(34)*sc,y+dp(8)*sc);q.close();p.setStyle(Paint.Style.FILL);p.setColor(edge);c.drawPath(q,p);c.save();c.scale(.90f,.82f,x,y);p.setColor(fill);c.drawPath(q,p);c.restore();
        }
        p.setStrokeJoin(Paint.Join.MITER);p.setStrokeCap(Paint.Cap.BUTT);
    }
    private void drawCompleteKey(Canvas c,float x,float y,float sc){
        // draw as one continuous visual object; fragments touch with no overlap/duplication
        drawKeyFragment(c,x-dp(61)*sc,y,0,sc,false);drawKeyFragment(c,x-dp(8)*sc,y,1,sc,false);drawKeyFragment(c,x+dp(54)*sc,y,2,sc,false);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2)*sc);p.setColor(C("#FF2EA6"));c.drawLine(x-dp(31)*sc,y+dp(10)*sc,x+dp(70)*sc,y+dp(10)*sc,p);
    }
    private float keySlotX(int i){return getWidth()*(.36f+i*.14f);}
    private float keySlotY(){return getHeight()*.72f;}
    private void drawAssemblySlots(Canvas c){
        mangaTxt(c,"REBUILD",getWidth()/2f,getHeight()*.655f,16,C("#FFE94D"),Paint.Align.CENTER);
        for(int i=0;i<3;i++)drawKeyFragment(c,keySlotX(i),keySlotY(),i,.78f,true);
    }
    private void drawFoundPiece(Canvas c,int revealMask,int idx){if((revealMask&(1<<idx))!=0&&(foundPlacedMask&(1<<idx))==0)drawKeyFragment(c,foundPieceX[idx],foundPieceY[idx],idx,.82f,false);}
    private void drawPlacedKey(Canvas c){
        if(foundPlacedMask==7){drawCompleteKey(c,finalKeyX,finalKeyY,.68f);mangaTxt(c,"KEY COMPLETE",getWidth()/2f,getHeight()*.82f,16,C("#DFFF00"),Paint.Align.CENTER);}
        else {drawAssemblySlots(c);for(int i=0;i<3;i++)if((foundPlacedMask&(1<<i))!=0)drawKeyFragment(c,keySlotX(i),keySlotY(),i,.78f,false);}
    }
    private boolean beginFoundPieceDrag(float x,float y,int revealMask){for(int i=0;i<3;i++){if((revealMask&(1<<i))!=0&&(foundPlacedMask&(1<<i))==0&&Math.hypot(x-foundPieceX[i],y-foundPieceY[i])<dp(48)){foundDrag=i+1;foundDX=x-foundPieceX[i];foundDY=y-foundPieceY[i];return true;}}return false;}
    private void moveFoundPiece(float x,float y){if(foundDrag>0){int i=foundDrag-1;foundPieceX[i]=x-foundDX;foundPieceY[i]=y-foundDY;}}
    private void endFoundPieceDrag(){if(foundDrag>0){int i=foundDrag-1;if(Math.hypot(foundPieceX[i]-keySlotX(i),foundPieceY[i]-keySlotY())<dp(55)){foundPlacedMask|=1<<i;foundPieceX[i]=keySlotX(i);foundPieceY[i]=keySlotY();if(foundPlacedMask==7){finalKeyX=getWidth()*.50f;finalKeyY=keySlotY();}}foundDrag=0;}}

    private void keyProgress(Canvas c,int mask,int total){mangaTxt(c,"FOUND "+Integer.bitCount(mask)+" / "+total,getWidth()/2f,getHeight()*.86f,17,C("#FF5BAA"),Paint.Align.CENTER);}
    private void l28(Canvas c){
        header(c,"Three digits are hidden under the clutter.");float W=getWidth(),H=getHeight();
        // desk surface
        neonPanel(c,W*.07f,H*.32f,W*.86f,H*.25f,C("#3C2147"),C("#FF5BAA"));
        // fixed hidden clues, always present below the movable objects
        mangaTxt(c,"7",W*.25f,H*.43f,34,C("#FFE94D"),Paint.Align.CENTER);
        mangaTxt(c,"2",W*.62f,H*.49f,34,C("#39F7FF"),Paint.Align.CENTER);
        mangaTxt(c,"9",W*.76f,H*.39f,34,C("#DFFF00"),Paint.Align.CENTER);
        // sliding drawer over digit 7
        float dx=deskDrawerX;neonPanel(c,dx,H*.355f,W*.28f,dp(95),C("#9A5434"),C("#FFE94D"));
        p.setStyle(Paint.Style.FILL);p.setColor(C("#111936"));c.drawRoundRect(dx+W*.11f,H*.38f,dx+W*.17f,H*.397f,dp(6),dp(6),p);
        // mug over digit 2
        p.setStyle(Paint.Style.FILL);p.setColor(C("#39F7FF"));c.drawCircle(deskMugX,deskMugY,dp(38),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(9));p.setColor(C("#39F7FF"));c.drawArc(deskMugX+dp(16),deskMugY-dp(20),deskMugX+dp(58),deskMugY+dp(20),-80,160,false,p);
        // book over digit 9
        c.save();c.rotate(-8,deskBookX,deskBookY);neonChip(c,deskBookX-dp(62),deskBookY-dp(37),dp(124),dp(74),C("#8E2B67"),C("#DFFF00"));mangaTxt(c,"NOTES",deskBookX,deskBookY+sp(6),15,Color.WHITE,Paint.Align.CENTER);c.restore();
        mangaTxt(c,"DRAG THE CLUTTER AWAY",W*.5f,H*.595f,17,C("#D7E8FF"),Paint.Align.CENTER);
        drawKeypadAt(c,H*.665f);mangaTxt(c,deskCode.isEmpty()?"_ _ _":deskCode,W*.5f,H*.635f,23,C("#FFE94D"),Paint.Align.CENTER);
        if(deskCode.equals("729"))solved();if(deskCode.length()>=3&&!deskCode.equals("729")){showToast("WRONG CODE");deskCode="";}
    }
    private void l29(Canvas c){
        header(c,"Roll the ball into the center.");float W=getWidth(),H=getHeight();float L=W*.08f,R=W*.92f,T=H*.31f,B=H*.78f;
        neonPanel(c,L,T,R-L,B-T,C("#0E1634"),C("#39F7FF"));
        // concentric maze walls with alternating gaps
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(12));p.setStrokeCap(Paint.Cap.ROUND);int[] cols={C("#FF2EA6"),C("#FFE94D"),C("#9D6CFF")};
        float cx=W*.5f,cy=H*.54f;for(int i=0;i<3;i++){float rad=dp(72+i*48);p.setColor(cols[i]);float gap=(i%2==0)?35:205;c.drawArc(cx-rad,cy-rad,cx+rad,cy+rad,gap+28,304,false,p);}p.setStrokeCap(Paint.Cap.BUTT);
        long n=System.nanoTime();float dt=Math.min(.035f,(n-lastFrame)/1_000_000_000f);lastFrame=n;mazeVX+=ax*42*dt;mazeVY+=ay*42*dt;mazeVX*=.985f;mazeVY*=.985f;float ox=mazeBallX,oy=mazeBallY;mazeBallX+=mazeVX;mazeBallY+=mazeVY;float br=dp(14);if(mazeBallX<L+br||mazeBallX>R-br||mazeBallY<T+br||mazeBallY>B-br){mazeBallX=ox;mazeBallY=oy;mazeVX*=-.45f;mazeVY*=-.45f;}
        // approximate circular wall collision with gaps
        for(int i=0;i<3;i++){float rad=dp(72+i*48);float d=(float)Math.hypot(mazeBallX-cx,mazeBallY-cy);if(Math.abs(d-rad)<dp(11)){double ang=(Math.toDegrees(Math.atan2(mazeBallY-cy,mazeBallX-cx))+360)%360;double gap=(i%2==0)?35:205;double diff=Math.abs(((ang-gap+540)%360)-180);if(diff>34){mazeBallX=ox;mazeBallY=oy;mazeVX*=-.58f;mazeVY*=-.58f;}}}
        p.setStyle(Paint.Style.FILL);p.setColor(C("#DFFF00"));c.drawCircle(cx,cy,dp(28),p);p.setColor(C("#090D1E"));c.drawCircle(cx,cy,dp(14),p);p.setColor(C("#FFE94D"));c.drawCircle(mazeBallX,mazeBallY,br,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#FF2EA6"));c.drawCircle(mazeBallX,mazeBallY,br,p);
        if(Math.hypot(mazeBallX-cx,mazeBallY-cy)<dp(25))solved();
    }
    private void l30(Canvas c){
        header(c,"Smash the moving piñata until the prize drops.");float W=getWidth(),H=getHeight();
        long n=System.nanoTime();float dt=Math.min(.035f,(n-lastFrame)/1_000_000_000f);lastFrame=n;
        pinataAV += (-2.3f*(float)Math.sin(pinataAngle)+ax*.13f+gz*.35f)*dt;pinataAV*=.992f;pinataAngle+=pinataAV*dt;pinataAngle=Math.max(-1.05f,Math.min(1.05f,pinataAngle));
        float anchorX=W*.5f,anchorY=H*.31f,len=dp(210);float px=anchorX+(float)Math.sin(pinataAngle)*len,py=anchorY+(float)Math.cos(pinataAngle)*len;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#EAF0FF"));c.drawLine(anchorX,anchorY,px,py-dp(48),p);
        c.save();c.translate(px,py);c.rotate((float)Math.toDegrees(pinataAngle));float pulse=System.currentTimeMillis()<pinataFlashUntil?1.13f:1f;c.scale(pulse,pulse);p.setStyle(Paint.Style.FILL);p.setColor(C("#FF2EA6"));Path body=new Path();body.moveTo(-dp(58),-dp(25));body.lineTo(-dp(30),-dp(55));body.lineTo(dp(44),-dp(45));body.lineTo(dp(62),0);body.lineTo(dp(35),dp(52));body.lineTo(-dp(42),dp(46));body.close();c.drawPath(body,p);p.setColor(C("#39F7FF"));c.drawRect(-dp(45),-dp(10),dp(50),dp(5),p);p.setColor(C("#FFE94D"));c.drawRect(-dp(38),dp(15),dp(42),dp(29),p);p.setColor(C("#DFFF00"));c.drawCircle(-dp(28),-dp(39),dp(8),p);c.restore();
        mangaTxt(c,"HITS  "+pinataHits+" / 8",W*.5f,H*.80f,19,C("#FFE94D"),Paint.Align.CENTER);
        if(pinataHits>=8){for(int i=0;i<9;i++){float a=(float)(i*Math.PI*2/9);p.setColor(new int[]{C("#39F7FF"),C("#FF2EA6"),C("#FFE94D")}[i%3]);c.drawCircle(px+(float)Math.cos(a)*dp(70),py+(float)Math.sin(a)*dp(60),dp(7),p);}mangaTxt(c,"★ PRIZE! ★",W*.5f,H*.69f,27,C("#DFFF00"),Paint.Align.CENTER);if(System.currentTimeMillis()-pinataFlashUntil>650)solved();}
    }
    private void l31(Canvas c){
        header(c,"Make the two layers become one symbol.");float W=getWidth(),H=getHeight();float tx=W*.50f,ty=H*.54f;
        // rear layer
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(16));p.setColor(C("#FF2EA6"));c.drawArc(tx-dp(80),ty-dp(80),tx+dp(80),ty+dp(80),35,230,false,p);
        // draggable foreground slash
        p.setStrokeWidth(dp(18));p.setColor(C("#39F7FF"));c.drawLine(perspectiveX-dp(50),perspectiveY+dp(55),perspectiveX+dp(50),perspectiveY-dp(55),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(C("#5E6A99"));c.drawCircle(tx,ty,dp(110),p);
        mangaTxt(c,"ALIGN THE LAYERS",W*.5f,H*.76f,19,C("#D7E8FF"),Paint.Align.CENTER);
        if(Math.hypot(perspectiveX-tx,perspectiveY-ty)<dp(24)){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(70,223,255,0));c.drawCircle(tx,ty,dp(120),p);mangaTxt(c,"SNAP!",tx,ty+dp(150),22,C("#DFFF00"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>500)solved();}
    }
    private void playSoundSeq(){if(soundPlaying)return;soundPlaying=true;soundReady=false;soundTaps.clear();int[] at={0,380,760,1140};int[] tones={ToneGenerator.TONE_DTMF_1,ToneGenerator.TONE_DTMF_3,ToneGenerator.TONE_DTMF_2,ToneGenerator.TONE_DTMF_1};for(int i=0;i<4;i++){final int k=i;handler.postDelayed(()->{if(screen==32){tone.startTone(tones[k],170);invalidate();}},at[i]);}handler.postDelayed(()->{if(screen==32){soundPlaying=false;soundReady=true;showToast("REPEAT THE PITCHES!");invalidate();}},1500);}
    private void l32(Canvas c){
        header(c,"Drag every colored piston to its matching notch.");float W=getWidth(),H=getHeight();float[] xs={.18f,.39f,.61f,.82f};float[] target={H*.48f,H*.57f,H*.43f,H*.62f};int[] col={C("#39F7FF"),C("#FF2EA6"),C("#FFE94D"),C("#DFFF00")};boolean ok=true;
        for(int i=0;i<4;i++){float x=W*xs[i];neonPanel(c,x-dp(30),H*.36f,dp(60),H*.34f,C("#101734"),col[i]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(col[i]);c.drawLine(x-dp(24),target[i],x+dp(24),target[i],p);p.setStyle(Paint.Style.FILL);p.setColor(col[i]);c.drawRoundRect(x-dp(23),pinY[i]-dp(18),x+dp(23),pinY[i]+dp(18),dp(9),dp(9),p);p.setColor(C("#0A0F22"));c.drawCircle(x,pinY[i],dp(7),p);if(Math.abs(pinY[i]-target[i])>dp(12))ok=false;}
        mangaTxt(c,ok?"ALL FOUR LOCKED":"DRAG — DON'T TAP",W*.5f,H*.80f,19,ok?C("#DFFF00"):C("#D7E8FF"),Paint.Align.CENTER);if(ok)solved();
    }
    private void checkSoundSeq(){if(soundTaps.size()<4)return;boolean ok=true;for(int i=0;i<4;i++)if(soundTaps.get(i)!=soundSeq[i])ok=false;if(ok)solved();else{showToast("WRONG MELODY");soundReady=false;soundTaps.clear();}}
    private void l33(Canvas c){
        header(c,"Fewest interior angles to most.");float W=getWidth(),H=getHeight();float[] xs={.18f,.40f,.62f,.82f};int[] sides={6,3,8,5};int[] cols={C("#39F7FF"),C("#FFE94D"),C("#FF2EA6"),C("#DFFF00")};
        for(int i=0;i<4;i++){drawPoly(c,W*xs[i],H*.54f,dp(43),sides[i],cols[i]);}
        mangaTxt(c,"STEP "+logicPos+" / 4",W*.5f,H*.73f,20,C("#EAF0FF"),Paint.Align.CENTER);
    }
    private void drawPoly(Canvas c,float cx,float cy,float r,int sides,int col){Path q=new Path();for(int i=0;i<sides;i++){double a=-Math.PI/2+i*Math.PI*2/sides;float x=cx+(float)Math.cos(a)*r,y=cy+(float)Math.sin(a)*r;if(i==0)q.moveTo(x,y);else q.lineTo(x,y);}q.close();p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawPath(q,p);}
    private void l34(Canvas c){header(c,"Someone covered the code with grime.");float l=getWidth()*.14f,t=getHeight()*.36f,r=getWidth()*.86f,b=getHeight()*.58f;rr(c,l,t,r-l,b-t,dp(20),C("#5D5A63"));if(rubProgress>20){float aa=Math.min(1f,rubProgress/120f);p.setColor(Color.argb((int)(255*aa),255,255,255));p.setTextSize(sp(44));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText("5 7 3",getWidth()/2f,getHeight()*.49f,p);}mangaTxt(c,"RUBBED  "+Math.min(100,(int)(rubProgress/1.2f))+"%",getWidth()/2f,getHeight()*.63f,19,C("#FF5BAA"),Paint.Align.CENTER);if(rubProgress>=100){drawKeypad(c);mangaTxt(c,revealCode.isEmpty()?"_ _ _":revealCode,getWidth()/2f,getHeight()*.68f,26,C("#EAF0FF"),Paint.Align.CENTER);if(revealCode.equals("573"))solved();if(revealCode.length()>=3&&!revealCode.equals("573")){showToast("WRONG CODE");revealCode="";}}}
    private void l35(Canvas c){
        header(c,"Three mechanisms hide three matching parts.");float W=getWidth(),H=getHeight(),y=H*.45f;drawAssemblySlots(c);
        // left sliding hatch: no label, physical handle and rails
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#39F7FF"));c.drawLine(W*.08f,y-dp(55),W*.35f,y-dp(55),p);c.drawLine(W*.08f,y+dp(55),W*.35f,y+dp(55),p);neonPanel(c,forgePanelX-dp(53),y-dp(48),dp(106),dp(96),C("#202A59"),C("#FF2EA6"));p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));c.drawRoundRect(forgePanelX-dp(17),y-dp(5),forgePanelX+dp(17),y+dp(5),dp(4),dp(4),p);
        if(Math.abs(forgePanelX-W*.24f)>dp(75)&&(forgeMask&1)==0){forgeMask|=1;foundPieceX[0]=W*.17f;foundPieceY[0]=y;}
        // center brittle cover progressively cracks
        neonPanel(c,W*.43f,y-dp(48),W*.23f,dp(96),C("#44331B"),C("#FFE94D"));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(C("#FF5BAA"));for(int i=0;i<Math.min(4,forgeGemTaps+1);i++){float cx=W*.545f+(i-1.5f)*dp(8);c.drawLine(cx,y-dp(25),cx-dp(12),y+dp(25),p);}if(forgeGemTaps>=4&&(forgeMask&2)==0){forgeMask|=2;foundPieceX[1]=W*.545f;foundPieceY[1]=y+dp(58);}
        // right hanging release on visible cable
        float px=W*.82f;p.setColor(C("#D7E8FF"));p.setStrokeWidth(dp(4));c.drawLine(px,H*.34f,px,forgePullY-dp(35),p);neonChip(c,px-dp(35),forgePullY-dp(35),dp(70),dp(70),C("#39F7FF"),C("#DFFF00"));p.setStyle(Paint.Style.FILL);p.setColor(C("#0A0F22"));Path ar=new Path();ar.moveTo(px-dp(12),forgePullY-dp(6));ar.lineTo(px+dp(12),forgePullY-dp(6));ar.lineTo(px,forgePullY+dp(14));ar.close();c.drawPath(ar,p);if(forgePullY>H*.59f&&(forgeMask&4)==0){forgeMask|=4;foundPieceX[2]=px;foundPieceY[2]=H*.62f;}
        for(int i=0;i<3;i++)drawFoundPiece(c,forgeMask,i);drawPlacedKey(c);keyProgress(c,forgeMask&7,3);
        if(foundPlacedMask==7){mangaTxt(c,"ASSEMBLY COMPLETE",W*.5f,H*.84f,17,C("#DFFF00"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>900)solved();}
    }

    private void l36(Canvas c){header(c,"Copy the flashing switch order.");float[] xs={.25f,.50f,.75f};int[] cols={C("#39F7FF"),C("#FF2EA6"),C("#FFE94D")};for(int i=0;i<3;i++){p.setStyle(Paint.Style.FILL);p.setColor(cols[i]);c.drawCircle(getWidth()*xs[i],getHeight()*.53f,dp(44),p);mangaTxt(c,String.valueOf(i+1),getWidth()*xs[i],getHeight()*.54f+sp(8),21,C("#091020"),Paint.Align.CENTER);}mangaTxt(c,"2  →  1  →  3",getWidth()/2f,getHeight()*.70f,25,Color.WHITE,Paint.Align.CENTER);if(neonSwitchStep>=3)solved();}
    private void l37(Canvas c){
        header(c,"Complete the circuit and light the lamp.");float W=getWidth(),H=getHeight(),cy=H*.55f;float bx=W*.10f,lx=W*.90f;
        // battery
        neonPanel(c,bx-dp(35),cy-dp(55),dp(70),dp(110),C("#121B3F"),C("#39F7FF"));p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));c.drawRect(bx-dp(18),cy-dp(30),bx+dp(18),cy+dp(30),p);mangaTxt(c,"+",bx,cy-dp(10),18,C("#090D1E"),Paint.Align.CENTER);mangaTxt(c,"−",bx,cy+dp(24),18,C("#090D1E"),Paint.Align.CENTER);
        // lamp
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(6));boolean ok=true;for(int i=0;i<4;i++)if(circuitRot[i]!=circuitTarget[i])ok=false;p.setColor(ok?C("#FFE94D"):C("#6670A1"));c.drawCircle(lx,cy-dp(8),dp(28),p);c.drawLine(lx-dp(16),cy+dp(26),lx+dp(16),cy+dp(26),p);c.drawLine(lx-dp(12),cy+dp(34),lx+dp(12),cy+dp(34),p);
        // wire baseline and 4 modules
        p.setStrokeWidth(dp(7));p.setColor(C("#5E6A99"));c.drawLine(bx+dp(36),cy,lx-dp(35),cy,p);float[] xs={.27f,.43f,.59f,.75f};for(int i=0;i<4;i++){float x=W*xs[i];neonPanel(c,x-dp(34),cy-dp(34),dp(68),dp(68),C("#111936"),new int[]{C("#39F7FF"),C("#FF2EA6"),C("#FFE94D"),C("#DFFF00")}[i]);c.save();c.rotate(circuitRot[i]*90,x,cy);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(Color.WHITE);c.drawLine(x-dp(26),cy,x+dp(26),cy,p);c.restore();}
        if(ok){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(C("#FFE94D"));c.drawLine(bx+dp(36),cy,lx-dp(35),cy,p);for(int r=1;r<=3;r++){p.setColor(Color.argb(130/r,255,233,77));c.drawCircle(lx,cy-dp(8),dp(28+r*12),p);}mangaTxt(c,"POWER!",W*.5f,H*.72f,24,C("#DFFF00"),Paint.Align.CENTER);if(System.currentTimeMillis()-roomStart>450)solved();}
    }
    private void l38(Canvas c){header(c,"Use the magnet to steal the metal puck.");p.setStyle(Paint.Style.FILL);p.setColor(C("#9EA6B6"));c.drawCircle(metalX,metalY,dp(24),p);p.setColor(C("#FF2EA6"));c.drawRoundRect(magnetX-dp(38),magnetY-dp(24),magnetX+dp(38),magnetY+dp(24),dp(16),dp(16),p);mangaTxt(c,"MAG",magnetX,magnetY+sp(6),15,Color.WHITE,Paint.Align.CENTER);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(C("#DFFF00"));c.drawCircle(getWidth()*.18f,getHeight()*.40f,dp(34),p);if(Math.hypot(magnetX-metalX,magnetY-metalY)<dp(95)){metalX+=(magnetX-metalX)*.10f;metalY+=(magnetY-metalY)*.10f;}if(Math.hypot(metalX-getWidth()*.18f,metalY-getHeight()*.40f)<dp(34))solved();}
    private void l39(Canvas c){header(c,"Rotate the tiles until the path is continuous.");float startX=getWidth()*.18f,y=getHeight()*.53f,sz=dp(74);boolean ok=true;for(int i=0;i<4;i++){float x=startX+i*getWidth()*.21f;neonChip(c,x-sz/2,y-sz/2,sz,sz,C("#111936"),C("#39F7FF"));c.save();c.rotate(tileRot[i]*90,x,y);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(10));p.setColor(C("#FFE94D"));c.drawLine(x-sz*.34f,y,x+sz*.34f,y,p);c.restore();if(tileRot[i]%2!=0)ok=false;}if(ok)solved();}
    private void l40(Canvas c){header(c,"Search the room with the flashlight.");p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(205,0,0,0));c.drawRect(0,getHeight()*.26f,getWidth(),getHeight(),p);Path cone=new Path();cone.moveTo(flashlightX,flashlightY);cone.lineTo(flashlightX-dp(110),flashlightY-dp(180));cone.lineTo(flashlightX+dp(110),flashlightY-dp(180));cone.close();p.setColor(Color.argb(150,255,233,77));c.drawPath(cone,p);mangaTxt(c,"★",getWidth()*.72f,getHeight()*.45f,34,C("#DFFF00"),Paint.Align.CENTER);if(Math.hypot(flashlightX-getWidth()*.72f,flashlightY-getHeight()*.63f)<dp(110)){hiddenMarkFound=true;}if(hiddenMarkFound){mangaTxt(c,"TAP THE STAR",getWidth()/2f,getHeight()*.76f,20,Color.WHITE,Paint.Align.CENTER);}}
    private void l41(Canvas c){header(c,"Make both sides weigh the same.");mangaTxt(c,"LEFT  "+balanceLeft,getWidth()*.28f,getHeight()*.48f,25,C("#39F7FF"),Paint.Align.CENTER);mangaTxt(c,"RIGHT  "+balanceRight,getWidth()*.72f,getHeight()*.48f,25,C("#FF2EA6"),Paint.Align.CENTER);button(c,"+1",getWidth()*.12f,getHeight()*.60f,getWidth()*.30f,dp(60),C("#39F7FF"));button(c,"+2",getWidth()*.58f,getHeight()*.60f,getWidth()*.30f,dp(60),C("#FF5BAA"));if(balanceLeft==balanceRight&&balanceLeft>=4)solved();}
    private void l42(Canvas c){header(c,"Repeat the color order.");int[] cols={C("#39F7FF"),C("#FF2EA6"),C("#FFE94D"),C("#DFFF00")};float[] xs={.18f,.39f,.61f,.82f};for(int i=0;i<4;i++){p.setColor(cols[i]);p.setStyle(Paint.Style.FILL);c.drawCircle(getWidth()*xs[i],getHeight()*.55f,dp(34),p);}mangaTxt(c,"YELLOW → CYAN → LIME → PINK",getWidth()/2f,getHeight()*.72f,17,Color.WHITE,Paint.Align.CENTER);if(colorSeqPos>=4)solved();}
    private void l43(Canvas c){header(c,"The panel is mirrored.");String[] arr={"←","↑","→","↓"};float[] xs={.18f,.39f,.61f,.82f};for(int i=0;i<4;i++)button(c,arr[i],getWidth()*xs[i]-dp(32),getHeight()*.52f,dp(64),dp(64),i%2==0?C("#39F7FF"):C("#FF5BAA"));mangaTxt(c,"MIRROR:  →  ↓  ←  ↑",getWidth()/2f,getHeight()*.72f,19,C("#FFE94D"),Paint.Align.CENTER);if(mirrorStep>=4)solved();}
    private void l44(Canvas c){header(c,"Reconnect the broken circuit.");float cx=getWidth()*.5f,cy=getHeight()*.55f;p.setStyle(Paint.Style.FILL);p.setColor(C("#FFE94D"));c.drawCircle(cx,cy,dp(28),p);float[][] n={{.22f,.43f},{.78f,.43f},{.50f,.72f}};for(int i=0;i<3;i++){float x=getWidth()*n[i][0],y=getHeight()*n[i][1];p.setColor((wireMask&(1<<i))!=0?C("#DFFF00"):C("#394064"));c.drawCircle(x,y,dp(30),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor((wireMask&(1<<i))!=0?C("#39F7FF"):C("#59617F"));c.drawLine(x,y,cx,cy,p);p.setStyle(Paint.Style.FILL);}if(wireMask==7)solved();}
    private void l45(Canvas c){header(c,"Read the shapes, then enter the code.");mangaTxt(c,"▲   ●●●●●   ■■■",getWidth()/2f,getHeight()*.39f,26,Color.WHITE,Paint.Align.CENTER);mangaTxt(c,safeCode.isEmpty()?"_ _ _":safeCode,getWidth()/2f,getHeight()*.50f,28,C("#FFE94D"),Paint.Align.CENTER);drawKeypad(c);if(safeCode.equals("153"))solved();if(safeCode.length()>=3&&!safeCode.equals("153")){safeCode="";showToast("TRY AGAIN");}}
    private void l46(Canvas c){header(c,"Peel away every layer.");int[] cols={C("#FF2EA6"),C("#39F7FF"),C("#FFE94D"),C("#DFFF00")};for(int i=peelLayers;i<4;i++){float off=(i-peelLayers)*dp(7);neonChip(c,getWidth()*.18f+off,getHeight()*.38f+off,getWidth()*.64f,dp(220),cols[i],C("#111936"));}mangaTxt(c,peelLayers<4?"SWIPE IT AWAY":"FOUND IT!",getWidth()/2f,getHeight()*.72f,23,Color.WHITE,Paint.Align.CENTER);if(peelLayers>=4){drawCompleteKey(c,getWidth()*.5f,getHeight()*.56f,.7f);if(System.currentTimeMillis()-roomStart>500)solved();}}
    private void l47(Canvas c){header(c,"Watch the grid. Then repeat it.");float sx=getWidth()*.28f,sy=getHeight()*.39f,cell=dp(68),gap=dp(12);long now=System.currentTimeMillis();if(!gridReady&&now-gridShownAt>2600)gridReady=true;for(int i=0;i<6;i++){int row=i/3,col=i%3;float x=sx+col*(cell+gap),y=sy+row*(cell+gap);boolean lit=!gridReady&&(i==0||i==3||i==5||i==2);neonChip(c,x,y,cell,cell,lit?C("#FFE94D"):C("#111936"),lit?C("#FF2EA6"):C("#39F7FF"));}mangaTxt(c,gridReady?"YOUR TURN":"MEMORIZE",getWidth()/2f,getHeight()*.72f,22,Color.WHITE,Paint.Align.CENTER);if(gridMemPos>=4)solved();}
    private void l48(Canvas c){header(c,"Hold both pads together.");float y=getHeight()*.55f;button(c,"HOLD",getWidth()*.08f,y,getWidth()*.34f,dp(80),C("#39F7FF"));button(c,"HOLD",getWidth()*.58f,y,getWidth()*.34f,dp(80),C("#FF5BAA"));boolean left=false,right=false;for(int i=0;i<pointerCount;i++){if(in(px[i],py[i],getWidth()*.08f,y,getWidth()*.42f,y+dp(80)))left=true;if(in(px[i],py[i],getWidth()*.58f,y,getWidth()*.92f,y+dp(80)))right=true;}if(left&&right){if(dualHoldStart==0)dualHoldStart=System.currentTimeMillis();}else dualHoldStart=0;long held=dualHoldStart==0?0:System.currentTimeMillis()-dualHoldStart;mangaTxt(c,(held/100)/10f+" / 1.5",getWidth()/2f,getHeight()*.72f,20,C("#FFE94D"),Paint.Align.CENTER);if(held>1500)solved();}
    private void l49(Canvas c){header(c,"Only one shadow matches its door.");float y=getHeight()*.54f;for(int i=0;i<3;i++){float x=getWidth()*(.24f+i*.26f);door(c,x,y,getWidth()*.18f,getHeight()*.20f);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(100,0,0,0));float skew=(i==1?0:(i==0?-28:30));Path sh=new Path();sh.moveTo(x-dp(34),y+dp(78));sh.lineTo(x+dp(34),y+dp(78));sh.lineTo(x+dp(34)+skew,y+dp(115));sh.lineTo(x-dp(34)+skew,y+dp(115));sh.close();c.drawPath(sh,p);}if(shadowChoice==1)solved();}
    private void l50(Canvas c){header(c,"Three visible steps. No guessing.");float W=getWidth(),H=getHeight();if(finalStage==0){neonChip(c,W*.28f,H*.43f,W*.44f,dp(95),C("#FF2EA6"),C("#39F7FF"));mangaTxt(c,"OPEN LATCH",W*.5f,H*.485f,20,Color.WHITE,Paint.Align.CENTER);}else if(finalStage==1){neonChip(c,W*.43f,finalLeverY,dp(90),dp(110),C("#39F7FF"),C("#FFE94D"));mangaTxt(c,"PULL",W*.5f,finalLeverY+dp(62),18,C("#07101A"),Paint.Align.CENTER);}else{p.setStyle(Paint.Style.FILL);p.setColor(C("#FF2EA6"));c.drawCircle(W*.5f,H*.54f,dp(72),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(C("#DFFF00"));c.drawCircle(W*.5f,H*.54f,dp(88),p);mangaTxt(c,"CORE "+finalTapCount+"/3",W*.5f,H*.55f+sp(7),20,Color.WHITE,Paint.Align.CENTER);if(finalTapCount>=3)solved();}}

    private void speedBurst(Canvas c,float cx,float cy,float radius){
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(C("#EAF0FF"));
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
        if(screen==-1){if(a==MotionEvent.ACTION_DOWN){String[] codes={"en","de","it","es","pt","fr","zh","ja","ru"};int cols=2;float bw=getWidth()*.39f,bh=dp(62),gap=dp(14),sx=getWidth()*.075f,sy=getHeight()*.23f;for(int i=0;i<codes.length;i++){int row=i/cols,col=i%cols;float bx=sx+col*(bw+getWidth()*.07f),by=sy+row*(bh+gap);if(in(x,y,bx,by,bx+bw,by+bh)){languageCode=codes[i];prefs.edit().putString("language",languageCode).apply();screen=0;((MainActivity)getContext()).requestStartupCameraPermission();invalidate();return true;}}}return true;}
        if(screen==0){ if(a==MotionEvent.ACTION_DOWN&&y<getHeight()*.30f){homeSecretTaps++;if(homeSecretTaps>=7){homeSecretUntil=System.currentTimeMillis()+2600;homeSecretTaps=0;}} if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.67f,getWidth()*.82f,getHeight()*.78f))gotoLevel(1); else if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.78f,getWidth()*.82f,getHeight()*.88f)){screen=90;levelPage=0;((MainActivity)getContext()).syncMusicForScreen(90);} invalidate(); return true; }
        if(screen==90){
            if(a==MotionEvent.ACTION_DOWN){int cols=3;float gap=dp(12),cell=(getWidth()-dp(48)-gap*2)/3,top=getHeight()*.19f;int startLevel=levelPage*LEVELS_PER_PAGE+1;for(int slot=0;slot<LEVELS_PER_PAGE;slot++){int level=startLevel+slot;if(level>TOTAL_LEVELS)continue;int row=slot/cols,col=slot%cols;float lx=dp(24)+col*(cell+gap),ty=top+row*(cell+gap*1.08f);if(in(x,y,lx,ty,lx+cell,ty+cell)){gotoLevel(level);return true;}}
                if(levelPage>0&&in(x,y,getWidth()*.04f,getHeight()*.80f,getWidth()*.26f,getHeight()*.88f)){levelPage--;invalidate();return true;}
                if(levelPage<AREA_COUNT-1&&in(x,y,getWidth()*.74f,getHeight()*.80f,getWidth()*.96f,getHeight()*.88f)){levelPage++;invalidate();return true;}
                if(y>getHeight()*.885f){screen=0;invalidate();return true;}}
            return true;
        }
        if(screen==95){if(a==MotionEvent.ACTION_DOWN&&System.currentTimeMillis()-fakeAdShownAt>=3000L&&in(x,y,getWidth()*.22f,getHeight()*.74f,getWidth()*.78f,getHeight()*.86f)){int next=pendingScreenAfterAd;pendingScreenAfterAd=-1;screen=next;if(returnToSnowFailAfterAd&&screen==21){returnToSnowFailAfterAd=false;snowEasterPhase=4;snowEasterAt=System.currentTimeMillis();}else if(screen>=1&&screen<=TOTAL_LEVELS){roomStart=System.currentTimeMillis();resetLevel();}((MainActivity)getContext()).syncMusicForScreen(screen);invalidate();}return true;}
        if(screen==99){ if(a==MotionEvent.ACTION_DOWN){screen=90;levelPage=AREA_COUNT-1;invalidate();}return true; }
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
                float scx=getWidth()*.50f,sheadY=getHeight()*.435f,snoseX=scx,snoseY=sheadY+dp(16),sbuttX=getWidth()*.69f,sbuttY=getHeight()*.655f;
                if(snowEasterPhase==4){if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.56f,getWidth()*.82f,getHeight()*.66f)){roomStart=System.currentTimeMillis();resetLevel();}else if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.18f,getHeight()*.66f,getWidth()*.82f,getHeight()*.78f)){screen=0;resetLevel();}break;}if(snowEasterPhase>0)break;
                if(a==MotionEvent.ACTION_DOWN){if(!snowNose&&Math.hypot(x-carrotX,y-carrotY)<dp(75)){snowDrag=1;keyDX=x-carrotX;keyDY=y-carrotY;}else if(!snowEye1&&Math.hypot(x-stone1X,y-stone1Y)<dp(46)){snowDrag=2;keyDX=x-stone1X;keyDY=y-stone1Y;}else if(!snowEye2&&Math.hypot(x-stone2X,y-stone2Y)<dp(46)){snowDrag=3;keyDX=x-stone2X;keyDY=y-stone2Y;}else if(y>sheadY+dp(18)&&y<sheadY+dp(75)&&Math.abs(x-scx)<dp(78)){drawingSmile=true;snowSmilePoints.clear();snowSmilePoints.add(new PointF(x,y));}}
                if(a==MotionEvent.ACTION_MOVE){if(snowDrag==1){carrotX=x-keyDX;carrotY=y-keyDY;if(snowEye1&&snowEye2&&Math.hypot(carrotX-sbuttX,carrotY-sbuttY)<dp(72)){snowEasterPhase=1;snowEasterAt=System.currentTimeMillis();snowDrag=0;}}else if(snowDrag==2){stone1X=x-keyDX;stone1Y=y-keyDY;}else if(snowDrag==3){stone2X=x-keyDX;stone2Y=y-keyDY;}if(drawingSmile&&snowSmilePoints.size()<100)snowSmilePoints.add(new PointF(x,y));}
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(snowDrag==1){if(Math.abs(carrotX-snoseX)<dp(48)&&Math.abs(carrotY-snoseY)<dp(38)){snowNose=true;}else{carrotX=getWidth()*.20f;carrotY=getHeight()*.77f;}}if(snowDrag==2){if(stone1X>scx-dp(70)&&stone1X<scx-dp(4)&&stone1Y>sheadY-dp(48)&&stone1Y<sheadY+dp(18)){snowEye1=true;}else{stone1X=getWidth()*.43f;stone1Y=getHeight()*.80f;}}if(snowDrag==3){if(stone2X>scx+dp(4)&&stone2X<scx+dp(70)&&stone2Y>sheadY-dp(48)&&stone2Y<sheadY+dp(18)){snowEye2=true;}else{stone2X=getWidth()*.58f;stone2Y=getHeight()*.80f;}}snowDrag=0;if(drawingSmile){drawingSmile=false;if(snowSmilePoints.size()>=5){PointF f=snowSmilePoints.get(0),l=snowSmilePoints.get(snowSmilePoints.size()-1),m=snowSmilePoints.get(snowSmilePoints.size()/2);float ends=(f.y+l.y)/2f;if(f.x<scx-dp(18)&&l.x>scx+dp(18)&&m.y>ends+dp(4))snowSmile=true;else{snowSmilePoints.clear();showToast("DRAW A CURVED SMILE");}}}}break;
            case 22: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.64f){oppositeChoice=x>getWidth()*.5f?1:0;if(oppositeChoice==0)showToast("That is the door he picked — so it is wrong.");}break;
            case 23:
                if(a==MotionEvent.ACTION_DOWN && in(x,y,getWidth()*.06f,getHeight()*.75f,getWidth()*.40f,getHeight()*.88f))gatePressed=true;
                if(a==MotionEvent.ACTION_MOVE){gatePressed=false;for(int i=0;i<e.getPointerCount();i++)if(in(e.getX(i),e.getY(i),getWidth()*.06f,getHeight()*.75f,getWidth()*.40f,getHeight()*.88f))gatePressed=true;}
                if(a==MotionEvent.ACTION_POINTER_UP){gatePressed=false;for(int i=0;i<e.getPointerCount();i++)if(i!=idx&&in(e.getX(i),e.getY(i),getWidth()*.06f,getHeight()*.75f,getWidth()*.40f,getHeight()*.88f))gatePressed=true;}
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)gatePressed=false;
                break;
            case 24: if(a==MotionEvent.ACTION_DOWN)showToast("Touching will not take it with you.");break;
            case 25: break;
            case 26:
                float aty=getHeight()*.50f;float[] atx={getWidth()*.34f,getWidth()*.50f,getWidth()*.66f};if(a==MotionEvent.ACTION_DOWN){for(int i=0;i<3;i++)if((assembleMask&(1<<i))==0&&Math.hypot(x-assembleX[i],y-assembleY[i])<dp(55)){assembleDrag=i+1;keyDX=x-assembleX[i];keyDY=y-assembleY[i];break;}}if(a==MotionEvent.ACTION_MOVE&&assembleDrag>0){int i=assembleDrag-1;assembleX[i]=x-keyDX;assembleY[i]=y-keyDY;}if((a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)&&assembleDrag>0){int i=assembleDrag-1;if(Math.hypot(assembleX[i]-atx[i],assembleY[i]-aty)<dp(58)){assembleMask|=(1<<i);assembleX[i]=atx[i];assembleY[i]=aty;}assembleDrag=0;}break;
            case 27:
                if(a==MotionEvent.ACTION_DOWN && selfieBitmap==null && !selfieRequested && in(x,y,getWidth()*.16f,getHeight()*.66f,getWidth()*.84f,getHeight()*.80f)){selfieRequested=true;((MainActivity)getContext()).startSelfieCapture();invalidate();}
                break;
            case 28:
                if(a==MotionEvent.ACTION_DOWN){
                    if(in(x,y,deskDrawerX,getHeight()*.355f,deskDrawerX+getWidth()*.28f,getHeight()*.355f+dp(95))){deskDrag=1;keyDX=x-deskDrawerX;}
                    else if(Math.hypot(x-deskMugX,y-deskMugY)<dp(58)){deskDrag=2;keyDX=x-deskMugX;keyDY=y-deskMugY;}
                    else if(Math.hypot(x-deskBookX,y-deskBookY)<dp(75)){deskDrag=3;keyDX=x-deskBookX;keyDY=y-deskBookY;}
                    else if(y>getHeight()*.61f){float top28=getHeight()*.665f;for(int rr=0;rr<3;rr++)for(int cc=0;cc<3;cc++){float kx=getWidth()*.22f+cc*getWidth()*.28f,ky=top28+rr*dp(62);if(Math.hypot(x-kx,y-ky)<dp(34)&&deskCode.length()<3)deskCode+=String.valueOf(rr*3+cc+1);}}
                }
                if(a==MotionEvent.ACTION_MOVE){if(deskDrag==1)deskDrawerX=Math.max(-getWidth()*.12f,Math.min(getWidth()*.60f,x-keyDX));else if(deskDrag==2){deskMugX=x-keyDX;deskMugY=y-keyDY;}else if(deskDrag==3){deskBookX=x-keyDX;deskBookY=y-keyDY;}}
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)deskDrag=0;
                break;
            case 29: break;
            case 30:
                if(a==MotionEvent.ACTION_DOWN&&pinataHits<8){float anchorX=getWidth()*.5f,anchorY=getHeight()*.31f,len=dp(210);float pxx=anchorX+(float)Math.sin(pinataAngle)*len,pyy=anchorY+(float)Math.cos(pinataAngle)*len;if(Math.hypot(x-pxx,y-pyy)<dp(75)){pinataHits++;pinataFlashUntil=System.currentTimeMillis()+500;pinataAV+=(x<pxx?1:-1)*1.7f;showToast(pinataHits<8?"WHACK!":"CRACK!!");}}
                break;
            case 31:
                if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-perspectiveX,y-perspectiveY)<dp(90)){perspectiveDrag=true;keyDX=x-perspectiveX;keyDY=y-perspectiveY;}
                if(a==MotionEvent.ACTION_MOVE&&perspectiveDrag){perspectiveX=x-keyDX;perspectiveY=y-keyDY;}
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)perspectiveDrag=false;
                break;
            case 32:
                if(a==MotionEvent.ACTION_DOWN){float[] pxx={.18f,.39f,.61f,.82f};for(int i=0;i<4;i++)if(Math.abs(x-getWidth()*pxx[i])<dp(36)&&Math.abs(y-pinY[i])<dp(45)){pinDrag=i;break;}}
                if(a==MotionEvent.ACTION_MOVE&&pinDrag>=0)pinY[pinDrag]=Math.max(getHeight()*.39f,Math.min(getHeight()*.68f,y));
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(pinDrag>=0){float[] target={getHeight()*.48f,getHeight()*.57f,getHeight()*.43f,getHeight()*.62f};if(Math.abs(pinY[pinDrag]-target[pinDrag])<dp(18))pinY[pinDrag]=target[pinDrag];}pinDrag=-1;}
                break;
            case 33:
                if(a==MotionEvent.ACTION_DOWN){float[] lx={.18f,.40f,.62f,.82f};int logicHit=-1;for(int i=0;i<4;i++)if(Math.hypot(x-getWidth()*lx[i],y-getHeight()*.54f)<dp(60))logicHit=i;if(logicHit>=0){if(logicHit==logicTarget[logicPos]){logicPos++;if(logicPos==4)solved();}else{logicPos=0;showToast("ORDER RESET");}}}
                break;
            case 34:
                if(a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.14f,getHeight()*.36f,getWidth()*.86f,getHeight()*.58f)){rubbing=true;lastMoveX=x;}if(a==MotionEvent.ACTION_MOVE&&rubbing){rubProgress=Math.min(120,rubProgress+Math.abs(x-lastMoveX)*.08f+2);lastMoveX=x;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)rubbing=false;if(rubProgress>=100&&a==MotionEvent.ACTION_DOWN&&y>getHeight()*.58f){float rubTop=getHeight()*.62f;for(int rr=0;rr<3;rr++)for(int cc=0;cc<3;cc++){float kx=getWidth()*.22f+cc*getWidth()*.28f,ky=rubTop+rr*dp(62);if(Math.hypot(x-kx,y-ky)<dp(34)&&revealCode.length()<3)revealCode+=String.valueOf(rr*3+cc+1);}}break;
            case 35:
                if(a==MotionEvent.ACTION_DOWN){if(beginFoundPieceDrag(x,y,forgeMask)){}else if(Math.hypot(x-forgePanelX,y-getHeight()*.45f)<dp(75)){forgePanelDrag=true;keyDX=x-forgePanelX;}else if(in(x,y,getWidth()*.43f,getHeight()*.39f,getWidth()*.66f,getHeight()*.51f)){forgeGemTaps++;}else if(Math.abs(x-getWidth()*.82f)<dp(55)&&Math.abs(y-forgePullY)<dp(60)){forgePullDrag=true;keyDY=y-forgePullY;}}
                if(a==MotionEvent.ACTION_MOVE){if(foundDrag>0)moveFoundPiece(x,y);else if(forgePanelDrag)forgePanelX=x-keyDX;else if(forgePullDrag)forgePullY=Math.max(getHeight()*.38f,Math.min(getHeight()*.64f,y-keyDY));}
                if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){endFoundPieceDrag();forgePanelDrag=false;forgePullDrag=false;}
                break;
            case 36: if(a==MotionEvent.ACTION_DOWN){float[] xs36={.25f,.50f,.75f};int[] tgt36={1,0,2};for(int i=0;i<3;i++)if(Math.hypot(x-getWidth()*xs36[i],y-getHeight()*.53f)<dp(55)){if(neonSwitchStep<3&&i==tgt36[neonSwitchStep])neonSwitchStep++;else{neonSwitchStep=0;showToast("RESET");}break;}}break;
            case 37:
                if(a==MotionEvent.ACTION_DOWN){float[] xs37={.27f,.43f,.59f,.75f};for(int i=0;i<4;i++)if(Math.abs(x-getWidth()*xs37[i])<dp(43)&&Math.abs(y-getHeight()*.55f)<dp(48)){circuitRot[i]=(circuitRot[i]+1)%2;break;}}
                break;
            case 38: if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-magnetX,y-magnetY)<dp(60)){magnetDrag=true;keyDX=x-magnetX;keyDY=y-magnetY;}if(a==MotionEvent.ACTION_MOVE&&magnetDrag){magnetX=x-keyDX;magnetY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)magnetDrag=false;break;
            case 39: if(a==MotionEvent.ACTION_DOWN){float startX=getWidth()*.18f,y39=getHeight()*.53f;for(int i=0;i<4;i++){float xx=startX+i*getWidth()*.21f;if(Math.abs(x-xx)<dp(42)&&Math.abs(y-y39)<dp(42)){tileRot[i]=(tileRot[i]+1)%4;break;}}}break;
            case 40: if(a==MotionEvent.ACTION_DOWN&&Math.hypot(x-flashlightX,y-flashlightY)<dp(70)){flashlightDrag=true;keyDX=x-flashlightX;keyDY=y-flashlightY;}if(a==MotionEvent.ACTION_MOVE&&flashlightDrag){flashlightX=x-keyDX;flashlightY=y-keyDY;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)flashlightDrag=false;if(hiddenMarkFound&&a==MotionEvent.ACTION_DOWN&&Math.hypot(x-getWidth()*.72f,y-getHeight()*.45f)<dp(50))solved();break;
            case 41: if(a==MotionEvent.ACTION_DOWN){if(in(x,y,getWidth()*.12f,getHeight()*.60f,getWidth()*.42f,getHeight()*.60f+dp(60)))balanceLeft++;else if(in(x,y,getWidth()*.58f,getHeight()*.60f,getWidth()*.88f,getHeight()*.60f+dp(60)))balanceRight+=2;if(balanceLeft>8)balanceLeft=0;if(balanceRight>8)balanceRight=0;}break;
            case 42: if(a==MotionEvent.ACTION_DOWN){float[] xs42={.18f,.39f,.61f,.82f};int hit=-1;for(int i=0;i<4;i++)if(Math.hypot(x-getWidth()*xs42[i],y-getHeight()*.55f)<dp(45))hit=i;if(hit>=0){if(colorSeqPos<4&&hit==colorSeq[colorSeqPos])colorSeqPos++;else{colorSeqPos=0;showToast("RESET");}}}break;
            case 43: if(a==MotionEvent.ACTION_DOWN){float[] xs43={.18f,.39f,.61f,.82f};int hit=-1;for(int i=0;i<4;i++)if(Math.abs(x-getWidth()*xs43[i])<dp(45)&&Math.abs(y-getHeight()*.55f)<dp(55))hit=i;if(hit>=0){if(mirrorStep<4&&hit==mirrorSeq[mirrorStep])mirrorStep++;else{mirrorStep=0;showToast("RESET");}}}break;
            case 44: if(a==MotionEvent.ACTION_DOWN){float[][] n44={{.22f,.43f},{.78f,.43f},{.50f,.72f}};for(int i=0;i<3;i++)if(Math.hypot(x-getWidth()*n44[i][0],y-getHeight()*n44[i][1])<dp(45)){wireMask|=(1<<i);break;}}break;
            case 45: if(a==MotionEvent.ACTION_DOWN&&y>getHeight()*.58f){float top45=getHeight()*.62f;for(int rr=0;rr<3;rr++)for(int cc=0;cc<3;cc++){float kx=getWidth()*.22f+cc*getWidth()*.28f,ky=top45+rr*dp(62);if(Math.hypot(x-kx,y-ky)<dp(34)&&safeCode.length()<3)safeCode+=String.valueOf(rr*3+cc+1);}}break;
            case 46: if(a==MotionEvent.ACTION_UP&&Math.hypot(x-downX,y-downY)>dp(70))peelLayers=Math.min(4,peelLayers+1);break;
            case 47: if(gridReady&&a==MotionEvent.ACTION_DOWN){float sx47=getWidth()*.28f,sy47=getHeight()*.39f,cell47=dp(68),gap47=dp(12);int hit=-1;for(int i=0;i<6;i++){int rr=i/3,cc=i%3;float bx=sx47+cc*(cell47+gap47),by=sy47+rr*(cell47+gap47);if(in(x,y,bx,by,bx+cell47,by+cell47))hit=i;}if(hit>=0){if(gridMemPos<4&&hit==gridSeq[gridMemPos])gridMemPos++;else{gridMemPos=0;showToast("RESET");}}}break;
            case 48: break;
            case 49: if(a==MotionEvent.ACTION_DOWN){float yy=getHeight()*.54f;for(int i=0;i<3;i++){float xx=getWidth()*(.24f+i*.26f);if(Math.hypot(x-xx,y-yy)<dp(60)){shadowChoice=i;break;}}}break;
            case 50: if(finalStage==0&&a==MotionEvent.ACTION_DOWN&&in(x,y,getWidth()*.28f,getHeight()*.43f,getWidth()*.72f,getHeight()*.43f+dp(95)))finalStage=1;else if(finalStage==1){if(a==MotionEvent.ACTION_DOWN&&Math.abs(x-getWidth()*.5f)<dp(60)&&Math.abs(y-finalLeverY)<dp(80)){finalLeverDrag=true;keyDY=y-finalLeverY;}if(a==MotionEvent.ACTION_MOVE&&finalLeverDrag)finalLeverY=Math.max(getHeight()*.42f,Math.min(getHeight()*.64f,y-keyDY));if((a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)&&finalLeverDrag){finalLeverDrag=false;if(finalLeverY>getHeight()*.58f)finalStage=2;}}else if(finalStage==2&&a==MotionEvent.ACTION_DOWN&&Math.hypot(x-getWidth()*.5f,y-getHeight()*.54f)<dp(95))finalTapCount++;break;

        }
        invalidate(); return true;
    }
}
