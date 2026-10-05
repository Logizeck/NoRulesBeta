package com.norules.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import java.util.*;

public class GameView extends View implements SensorEventListener {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SensorManager sm;
    private final Sensor accel;
    private final AudioManager audio;
    private final SharedPreferences prefs;
    private final Bitmap[] bg = new Bitmap[11];
    private Bitmap menuBg;
    private int screen = -1; // -1 language, 0 menu, 90 select, 1..10 levels
    private String lang = "en";
    private boolean hintOpen = false;
    private long levelStart, lastFrame;
    private float ax, ay;
    private long lastShake;
    private int shakeCount;
    private boolean solvedFlash;
    private long solvedAt;
    private float keyScale=1f, keyX=.43f, keyY=.58f, pinchStartDist=0, pinchStartScale=1f;
    private boolean draggingKey=false;
    private float ballX=.22f, ballY=.58f;
    private int selectedColor=0;
    private final int[] splash = new int[9];
    private final int[] targetSplash = {1,2,3,3,2,1,2,3,1};
    private boolean splashCovered=true;
    private long splashRevealUntil;
    private long zenStart;
    private boolean lockAwake=false;
    private final boolean[] snacks = new boolean[3];
    private int mirrorA=0, mirrorB=0;
    private final int[] circuit = {0,0,0,0};
    private float balloonLift=0f;
    private float downX, downY;
    private boolean touchActive=false;
    private final ArrayList<Particle> particles = new ArrayList<>();

    private static class Particle {
        float x,y,vx,vy,life;
        Particle(float x,float y,float vx,float vy,float life){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;}
    }

    public GameView(Context c) {
        super(c);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        prefs = c.getSharedPreferences("nr_b10", Context.MODE_PRIVATE);
        lang = prefs.getString("lang", "");
        screen = lang.isEmpty() ? -1 : 0;
        sm = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        audio = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        loadBitmaps(c);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(5f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        lastFrame = SystemClock.uptimeMillis();
    }

    private void loadBitmaps(Context c){
        menuBg = BitmapFactory.decodeResource(getResources(), R.drawable.menu_collage);
        int[] ids = {0,R.drawable.level01,R.drawable.level02,R.drawable.level03,R.drawable.level04,R.drawable.level05,R.drawable.level06,R.drawable.level07,R.drawable.level08,R.drawable.level09,R.drawable.level10};
        for(int i=1;i<=10;i++) bg[i]=BitmapFactory.decodeResource(getResources(),ids[i]);
    }

    public void onResumeGame(){ if(accel!=null) sm.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME); }
    public void onPauseGame(){ sm.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()!=Sensor.TYPE_ACCELEROMETER) return;
        ax=e.values[0]; ay=e.values[1];
        float mag=(float)Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);
        long now=SystemClock.uptimeMillis();
        if(mag>17f && now-lastShake>450){ lastShake=now; shakeCount++; if(screen==6) lockAwake=true; }
    }
    @Override public void onAccuracyChanged(Sensor s,int a){}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=SystemClock.uptimeMillis();
        float dt=Math.min(.033f,(now-lastFrame)/1000f); lastFrame=now;
        if(screen==-1) drawLanguage(c);
        else if(screen==0) drawMenu(c);
        else if(screen==90) drawSelect(c);
        else if(screen>=1&&screen<=10) drawLevel(c,screen,now,dt);
        updateParticles(dt);
        drawParticles(c);
        if(hintOpen) drawHint(c);
        if(solvedFlash) drawSolved(c,now);
        postInvalidateOnAnimation();
    }

    private void drawBitmapFill(Canvas c, Bitmap b){
        if(b==null){ c.drawColor(Color.rgb(2,7,35)); return; }
        Rect src=new Rect(0,0,b.getWidth(),b.getHeight());
        RectF dst=new RectF(0,0,getWidth(),getHeight());
        p.setAlpha(255); c.drawBitmap(b,src,dst,p);
    }

    private void drawLanguage(Canvas c){
        c.drawColor(Color.rgb(4,7,34));
        float w=getWidth(), h=getHeight();
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.CYAN); p.setTextSize(w*.085f); c.drawText("NO RULES",w/2,h*.12f,p);
        p.setColor(Color.WHITE); p.setTextSize(w*.045f); c.drawText("CHOOSE LANGUAGE",w/2,h*.18f,p);
        String[] codes={"en","de","it","es","pt","fr","zh","ja","ru"};
        String[] names={"ENGLISH","DEUTSCH","ITALIANO","ESPAÑOL","PORTUGUÊS","FRANÇAIS","中文","日本語","РУССКИЙ"};
        for(int i=0;i<9;i++){
            int col=i%2,row=i/2; float x=w*(col==0?.27f:.73f), y=h*(.28f+row*.115f);
            if(i==8){x=w*.5f; y=h*.28f+4*.115f;}
            neonButton(c,x-w*.19f,y-h*.035f,x+w*.19f,y+h*.035f, i%2==0?Color.rgb(22,220,240):Color.rgb(255,63,185));
            p.setColor(Color.rgb(10,12,45)); p.setTextSize(w*.034f); c.drawText(names[i],x,y+h*.012f,p);
        }
    }

    private void drawMenu(Canvas c){
        drawBitmapFill(c,menuBg); float w=getWidth(),h=getHeight();
        // cover/replace small tagline and button labels to follow selected language
        p.setColor(Color.argb(205,4,7,22)); c.drawRoundRect(w*.30f,h*.24f,w*.70f,h*.29f,24,24,p);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.042f); p.setColor(Color.WHITE); c.drawText(tr("tag"),w*.5f,h*.275f,p);
        drawMenuLabel(c,w*.5f,h*.49f,tr("play"),Color.rgb(225,255,0));
        drawMenuLabel(c,w*.5f,h*.62f,tr("levels"),Color.rgb(30,223,239));
        drawMenuLabel(c,w*.5f,h*.72f,tr("settings"),Color.rgb(255,50,182));
    }
    private void drawMenuLabel(Canvas c,float x,float y,String s,int color){
        float w=getWidth(),h=getHeight(); p.setColor(color); c.drawRoundRect(x-w*.30f,y-h*.036f,x+w*.30f,y+h*.036f,30,30,p);
        p.setColor(Color.rgb(8,9,38)); p.setTextSize(w*.05f); p.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y+h*.016f,p);
    }

    private void drawSelect(Canvas c){
        c.drawColor(Color.rgb(3,6,28)); float w=getWidth(),h=getHeight();
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(w*.065f); c.drawText(tr("levels"),w/2,h*.10f,p);
        for(int i=1;i<=10;i++){
            int col=(i-1)%2,row=(i-1)/2; float x=w*(col==0?.28f:.72f), y=h*(.22f+row*.12f);
            int colr=(i%2==0)?Color.rgb(255,66,184):Color.rgb(30,225,241);
            neonButton(c,x-w*.16f,y-h*.042f,x+w*.16f,y+h*.042f,colr);
            p.setColor(Color.rgb(7,8,35)); p.setTextSize(w*.055f); c.drawText(String.valueOf(i),x,y+h*.018f,p);
        }
        neonButton(c,w*.25f,h*.88f,w*.75f,h*.95f,Color.rgb(255,225,60)); p.setColor(Color.rgb(10,10,40)); p.setTextSize(w*.045f); c.drawText(tr("back"),w*.5f,h*.925f,p);
    }

    private void drawLevel(Canvas c,int n,long now,float dt){
        drawBitmapFill(c,bg[n]);
        drawLocalizedChrome(c,n);
        switch(n){
            case 1: drawLevel1(c); break;
            case 2: drawLevel2(c,dt); break;
            case 3: drawLevel3(c,now); break;
            case 4: drawLevel4(c,now); break;
            case 5: drawLevel5(c); break;
            case 6: drawLevel6(c); break;
            case 7: drawLevel7(c); break;
            case 8: drawLevel8(c); break;
            case 9: drawLevel9(c); break;
            case 10: drawLevel10(c); break;
        }
    }

    private void drawLocalizedChrome(Canvas c,int n){
        if("en".equals(lang)) return;
        float w=getWidth(),h=getHeight();
        p.setColor(Color.rgb(5,10,50)); c.drawRoundRect(w*.10f,h*.045f,w*.90f,h*.18f,34,34,p);
        stroke.setColor(Color.CYAN); stroke.setStrokeWidth(5); c.drawRoundRect(w*.10f,h*.045f,w*.90f,h*.18f,34,34,stroke);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.060f); p.setColor(Color.WHITE); c.drawText(title(n),w*.5f,h*.135f,p);
        drawChromeButton(c,w*.08f,h*.195f,w*.34f,h*.255f,tr("menu"),Color.rgb(35,223,240));
        drawChromeButton(c,w*.66f,h*.195f,w*.92f,h*.255f,tr("hint"),Color.rgb(255,213,40));
    }

    private void drawChromeButton(Canvas c,float l,float t,float r,float b,String s,int color){
        p.setColor(color); c.drawRoundRect(getWidth()*l/getWidth(),0,0,0,0,0,p); // noop to keep overload ambiguity away
        float w=getWidth(),h=getHeight(); RectF rf=new RectF(l,t,r,b); p.setColor(color); c.drawRoundRect(rf,26,26,p); stroke.setColor(Color.rgb(20,15,70)); stroke.setStrokeWidth(6); c.drawRoundRect(rf,26,26,stroke);
        p.setColor(Color.rgb(12,13,45)); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.040f); c.drawText(s,(l+r)/2,(t+b)/2+h*.014f,p);
    }

    private void neonButton(Canvas c,float l,float t,float r,float b,int color){
        p.setColor(Color.argb(65, colorRed(color),colorGreen(color),colorBlue(color))); c.drawRoundRect(l-10,t-10,r+10,b+10,28,28,p);
        p.setColor(color); c.drawRoundRect(l,t,r,b,24,24,p);
        stroke.setColor(Color.WHITE); stroke.setStrokeWidth(3); c.drawRoundRect(l+5,t+5,r-5,b-5,20,20,stroke);
    }
    private int colorRed(int c){return (c>>16)&255;} private int colorGreen(int c){return (c>>8)&255;} private int colorBlue(int c){return c&255;}

    private void drawLevel1(Canvas c){
        float w=getWidth(),h=getHeight();
        stroke.setColor(Color.argb(180,255,230,40)); stroke.setStrokeWidth(8);
        float kx=w*keyX, ky=h*keyY, rr=w*.12f*keyScale;
        c.drawCircle(kx,ky,rr,stroke);
        if(keyScale<.58f){ p.setColor(Color.argb(130,70,255,210)); c.drawCircle(w*.72f,h*.55f,w*.06f,p); }
    }

    private void drawLevel2(Canvas c,float dt){
        float w=getWidth(),h=getHeight();
        ballX += (-ax)*dt*.035f; ballY += ay*dt*.030f;
        ballX=Math.max(.12f,Math.min(.86f,ballX)); ballY=Math.max(.36f,Math.min(.76f,ballY));
        // soft maze collision bands roughly matching the visual route
        if(ballY<.50f && ballX>.38f && ballX<.74f) ballY=.50f;
        if(ballY>.57f && ballX>.24f && ballX<.52f) ballX=.24f;
        p.setColor(Color.rgb(190,225,255)); c.drawCircle(w*ballX,h*ballY,w*.027f,p); stroke.setColor(Color.CYAN); stroke.setStrokeWidth(4); c.drawCircle(w*ballX,h*ballY,w*.027f,stroke);
        float dx=ballX-.52f,dy=ballY-.55f; if(dx*dx+dy*dy<.0055f) solve();
    }

    private void drawLevel3(Canvas c,long now){
        float w=getWidth(),h=getHeight();
        float gx=w*.24f, gy=h*.39f, cw=w*.17f, ch=h*.095f;
        if(now<splashRevealUntil) return;
        if(splashCovered){
            p.setColor(Color.argb(205,20,20,45));
            for(int r=0;r<3;r++) for(int col=0;col<3;col++) c.drawRoundRect(gx+col*cw,gy+r*ch,gx+col*cw+cw*.86f,gy+r*ch+ch*.82f,18,18,p);
        } else {
            for(int i=0;i<9;i++) if(splash[i]!=0){
                int r=i/3,col=i%3; p.setColor(splash[i]==1?Color.MAGENTA:(splash[i]==2?Color.YELLOW:Color.rgb(0,155,255)));
                c.drawCircle(gx+col*cw+cw*.43f,gy+r*ch+ch*.41f,Math.min(cw,ch)*.25f,p);
            }
        }
    }

    private void drawLevel4(Canvas c,long now){
        float w=getWidth(),h=getHeight(); if(zenStart==0) zenStart=now; long left=Math.max(0,7000-(now-zenStart));
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(w*.075f); c.drawText(String.valueOf((left+999)/1000),w*.5f,h*.35f,p);
        if(left<=0) solve();
    }

    private void drawLevel5(Canvas c){
        if(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0) solve();
        float w=getWidth(),h=getHeight(); int vol=audio.getStreamVolume(AudioManager.STREAM_MUSIC),max=Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        p.setColor(Color.argb(180,255,55,170)); c.drawRoundRect(w*.15f,h*.83f,w*(.15f+.7f*vol/max),h*.86f,18,18,p);
    }

    private void drawLevel6(Canvas c){
        float w=getWidth(),h=getHeight(); if(lockAwake){ stroke.setColor(Color.rgb(255,235,50)); stroke.setStrokeWidth(12); c.drawCircle(w*.51f,h*.59f,w*.20f,stroke); }
    }

    private void drawLevel7(Canvas c){
        float w=getWidth(),h=getHeight();
        for(int i=0;i<3;i++) if(snacks[i]){ p.setColor(Color.argb(180,0,255,210)); c.drawCircle(w*(.29f+i*.20f),h*.63f,w*.03f,p); }
        if(snacks[0]&&snacks[1]&&snacks[2]) solve();
    }

    private void drawLevel8(Canvas c){
        float w=getWidth(),h=getHeight();
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.042f); p.setColor(Color.WHITE);
        c.drawText("↻"+mirrorA,w*.31f,h*.50f,p); c.drawText("↻"+mirrorB,w*.72f,h*.66f,p);
        if(mirrorA==1&&mirrorB==3){ stroke.setColor(Color.YELLOW); stroke.setStrokeWidth(9); Path path=new Path(); path.moveTo(w*.18f,h*.70f); path.lineTo(w*.72f,h*.62f); path.lineTo(w*.31f,h*.46f); path.lineTo(w*.75f,h*.42f); c.drawPath(path,stroke); solve(); }
    }

    private void drawLevel9(Canvas c){
        float w=getWidth(),h=getHeight();
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.034f); p.setColor(Color.WHITE);
        for(int i=0;i<4;i++){ float x=w*(.35f+(i%2)*.27f), y=h*(.52f+(i/2)*.14f); c.drawText("↻"+circuit[i],x,y,p); }
        if(circuit[0]==1&&circuit[1]==2&&circuit[2]==3&&circuit[3]==1){
            p.setColor(Color.argb(120,255,245,80)); c.drawCircle(w*.84f,h*.69f,w*.11f,p); solve();
        }
    }

    private void drawLevel10(Canvas c){
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(100,40,240,255)); c.drawRoundRect(w*.42f,h*(.77f-balloonLift*.25f),w*.58f,h*(.87f-balloonLift*.25f),30,30,p);
        if(balloonLift>=1f) solve();
    }

    private void drawHint(Canvas c){
        float w=getWidth(),h=getHeight(); p.setColor(Color.argb(190,0,0,20)); c.drawRect(0,0,w,h,p);
        RectF box=new RectF(w*.08f,h*.31f,w*.92f,h*.68f); p.setColor(Color.rgb(247,250,255)); c.drawRoundRect(box,34,34,p); stroke.setColor(Color.CYAN); stroke.setStrokeWidth(7); c.drawRoundRect(box,34,34,stroke);
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.rgb(8,8,45)); p.setTextSize(w*.055f); c.drawText(tr("hint"),w*.5f,h*.39f,p);
        p.setTextSize(w*.038f); drawWrapped(c,hintFor(screen),w*.5f,h*.47f,w*.72f,w*.038f*1.35f);
        neonButton(c,w*.32f,h*.60f,w*.68f,h*.655f,Color.rgb(255,220,50)); p.setColor(Color.rgb(8,8,45)); p.setTextSize(w*.036f); c.drawText(tr("close"),w*.5f,h*.638f,p);
    }

    private void drawWrapped(Canvas c,String text,float cx,float y,float maxW,float lineH){
        String[] words=text.split(" "); String line=""; for(String word:words){ String test=line.isEmpty()?word:line+" "+word; if(p.measureText(test)>maxW&&!line.isEmpty()){ c.drawText(line,cx,y,p); y+=lineH; line=word;} else line=test;} if(!line.isEmpty())c.drawText(line,cx,y,p);
    }

    private void drawSolved(Canvas c,long now){
        float w=getWidth(),h=getHeight(); float t=Math.min(1f,(now-solvedAt)/700f); p.setColor(Color.argb((int)(170*(1-t*.25f)),0,0,20)); c.drawRect(0,0,w,h,p);
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.YELLOW); p.setTextSize(w*(.09f+.02f*(float)Math.sin(t*8))); c.drawText(tr("solved"),w*.5f,h*.53f,p);
        if(now-solvedAt>900){ solvedFlash=false; int next=screen+1; if(next<=10) gotoLevel(next); else screen=90; }
    }

    private void updateParticles(float dt){ Iterator<Particle> it=particles.iterator(); while(it.hasNext()){Particle q=it.next(); q.x+=q.vx*dt; q.y+=q.vy*dt; q.vy+=.18f*dt; q.life-=dt; if(q.life<=0)it.remove();}}
    private void drawParticles(Canvas c){float w=getWidth(),h=getHeight(); for(Particle q:particles){ p.setColor(Color.argb((int)(255*Math.min(1,q.life)),255,230,50)); c.drawCircle(w*q.x,h*q.y,w*.008f,p); }}
    private void burst(float x,float y){ for(int i=0;i<18;i++){ double a=Math.PI*2*i/18.0; particles.add(new Particle(x,y,(float)Math.cos(a)*.35f,(float)Math.sin(a)*.35f,.7f)); }}

    private void solve(){ if(solvedFlash) return; solvedFlash=true; solvedAt=SystemClock.uptimeMillis(); prefs.edit().putBoolean("done_"+screen,true).apply(); burst(.5f,.55f); }
    private void gotoLevel(int n){ screen=n; hintOpen=false; solvedFlash=false; levelStart=SystemClock.uptimeMillis(); zenStart=0; shakeCount=0; lockAwake=false; keyScale=1;keyX=.43f;keyY=.58f;ballX=.22f;ballY=.58f;selectedColor=0;Arrays.fill(splash,0);splashCovered=true;splashRevealUntil=levelStart+1800;Arrays.fill(snacks,false);mirrorA=mirrorB=0;Arrays.fill(circuit,0);balloonLift=0; }

    @Override public boolean onTouchEvent(MotionEvent e){
        float w=getWidth(),h=getHeight(); float x=e.getX()/w,y=e.getY()/h; int a=e.getActionMasked();
        if(screen==-1 && a==MotionEvent.ACTION_UP){
            int idx=-1; if(y>.245f&&y<.36f) idx=x<.5f?0:1; else if(y>.36f&&y<.475f)idx=x<.5f?2:3; else if(y>.475f&&y<.59f)idx=x<.5f?4:5; else if(y>.59f&&y<.705f)idx=x<.5f?6:7; else if(y>.70f&&y<.82f)idx=8;
            if(idx>=0){ String[] codes={"en","de","it","es","pt","fr","zh","ja","ru"};lang=codes[idx];prefs.edit().putString("lang",lang).apply();screen=0;invalidate(); } return true;
        }
        if(hintOpen){ if(a==MotionEvent.ACTION_UP && y>.55f){hintOpen=false;invalidate();} return true; }
        if(screen==0 && a==MotionEvent.ACTION_UP){ if(y>.44f&&y<.55f){gotoLevel(1);} else if(y>.56f&&y<.68f)screen=90; else if(y>.68f&&y<.79f)screen=-1; invalidate(); return true; }
        if(screen==90 && a==MotionEvent.ACTION_UP){
            if(y>.84f){screen=0;return true;} for(int i=1;i<=10;i++){int col=(i-1)%2,row=(i-1)/2;float cx=col==0?.28f:.72f,cy=.22f+row*.12f;if(Math.abs(x-cx)<.18f&&Math.abs(y-cy)<.055f){gotoLevel(i);return true;}}
        }
        if(screen>=1&&screen<=10){
            if(a==MotionEvent.ACTION_UP && y>.18f&&y<.30f&&x<.40f){screen=0;return true;}
            if(a==MotionEvent.ACTION_UP && y>.18f&&y<.30f&&x>.60f){hintOpen=true;return true;}
            if(screen==4 && (a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE)) zenStart=SystemClock.uptimeMillis();
            handleLevelTouch(e,x,y);
        }
        return true;
    }

    private void handleLevelTouch(MotionEvent e,float x,float y){ int a=e.getActionMasked();
        if(a==MotionEvent.ACTION_DOWN){downX=x;downY=y;touchActive=true;}
        if(screen==1){
            if(e.getPointerCount()>=2){ float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.sqrt(dx*dx+dy*dy); if(pinchStartDist==0){pinchStartDist=d;pinchStartScale=keyScale;} keyScale=Math.max(.35f,Math.min(1.4f,pinchStartScale*d/pinchStartDist)); }
            else if(a==MotionEvent.ACTION_DOWN&&dist(x,y,keyX,keyY)<.18f)draggingKey=true;
            else if(a==MotionEvent.ACTION_MOVE&&draggingKey&&keyScale<.65f){keyX=x;keyY=y;}
            if(a==MotionEvent.ACTION_UP){pinchStartDist=0;draggingKey=false;if(keyScale<.65f&&dist(keyX,keyY,.72f,.55f)<.14f)solve();}
        } else if(screen==3 && a==MotionEvent.ACTION_UP){
            if(y>.70f){ selectedColor=x<.34f?1:(x<.66f?2:3); splashCovered=false; }
            else { float gx=.24f,gy=.39f,cw=.17f,ch=.095f;int col=(int)((x-gx)/cw),row=(int)((y-gy)/ch);if(selectedColor>0&&col>=0&&col<3&&row>=0&&row<3){splash[row*3+col]=selectedColor; boolean ok=true;for(int i=0;i<9;i++)if(splash[i]!=targetSplash[i])ok=false;if(ok)solve();}}
        } else if(screen==6 && a==MotionEvent.ACTION_UP){ if(lockAwake&&x>.25f&&x<.78f&&y>.38f&&y<.78f)solve(); }
        else if(screen==7 && a==MotionEvent.ACTION_UP){
            // three obvious snack zones from upper/middle/lower tray; tapping feeds them
            if(y>.47f&&y<.83f){ int idx=x<.36f?0:(x<.68f?1:2); if(!snacks[idx]){snacks[idx]=true;burst(x,y);} }
        } else if(screen==8 && a==MotionEvent.ACTION_UP){ if(x<.5f){mirrorA=(mirrorA+1)%4;}else{mirrorB=(mirrorB+1)%4;} }
        else if(screen==9 && a==MotionEvent.ACTION_UP){ if(y>.42f&&y<.80f){int col=x<.5f?0:1,row=y<.62f?0:1,idx=row*2+col;circuit[idx]=(circuit[idx]+1)%4;} }
        else if(screen==10 && a==MotionEvent.ACTION_UP){ float dy=y-downY;if(dy<-.08f && downY>.55f){balloonLift=Math.min(1f,balloonLift+.22f);burst(.5f,.72f-balloonLift*.2f);} }
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){touchActive=false;pinchStartDist=0;}
    }

    private float dist(float a,float b,float c,float d){float x=a-c,y=b-d;return (float)Math.sqrt(x*x+y*y);}

    private String title(int n){ String[] en={"","KEY PROBLEM","TINY MAZE","COLOR SPLASH","ZEN MODE","QUIET PLEASE","WAKE THE LOCK","MONSTER SNACK","MIRROR BEAM","BRIGHT CIRCUIT","BALLOON LIFT"}; if("en".equals(lang))return en[n]; String[][] m={
        {"it","","PROBLEMA DELLA CHIAVE","PICCOLO LABIRINTO","ESPLOSIONE DI COLORI","MODALITÀ ZEN","SILENZIO, PER FAVORE","SVEGLIA IL LUCCHETTO","SPUNTINO DEL MOSTRO","RAGGIO SPECCHIATO","CIRCUITO LUMINOSO","ASCENSORE DI PALLONCINI"},
        {"de","","SCHLÜSSELPROBLEM","MINI-LABYRINTH","FARBKLECKS","ZEN-MODUS","BITTE LEISE","WECK DAS SCHLOSS","MONSTER-SNACK","SPIEGELSTRAHL","LICHTSCHALTUNG","BALLON-AUFZUG"},
        {"es","","PROBLEMA DE LA LLAVE","MINI LABERINTO","EXPLOSIÓN DE COLOR","MODO ZEN","SILENCIO, POR FAVOR","DESPIERTA EL CANDADO","BOLSA DEL MONSTRUO","RAYO ESPEJO","CIRCUITO BRILLANTE","ASCENSOR DE GLOBOS"},
        {"pt","","PROBLEMA DA CHAVE","MINI LABIRINTO","EXPLOSÃO DE CORES","MODO ZEN","SILÊNCIO, POR FAVOR","ACORDE O CADEADO","LANCHE DO MONSTRO","RAIO ESPELHO","CIRCUITO BRILHANTE","ELEVADOR DE BALÕES"},
        {"fr","","PROBLÈME DE CLÉ","MINI LABYRINTHE","ÉCLABOUSSURE DE COULEURS","MODE ZEN","SILENCE, S'IL VOUS PLAÎT","RÉVEILLE LE CADENAS","GOÛTER DU MONSTRE","RAYON MIROIR","CIRCUIT LUMINEUX","ASCENSEUR À BALLONS"},
        {"zh","","钥匙难题","迷你迷宫","色彩飞溅","禅模式","请安静","叫醒锁","怪兽零食","镜面光束","明亮电路","气球升降机"},
        {"ja","","鍵の問題","ミニ迷路","カラースプラッシュ","禅モード","お静かに","ロックを起こせ","モンスタースナック","ミラービーム","ライト回路","バルーンリフト"},
        {"ru","","ПРОБЛЕМА С КЛЮЧОМ","МИНИ-ЛАБИРИНТ","ВСПЛЕСК ЦВЕТА","РЕЖИМ ДЗЕН","ТИШИНА, ПОЖАЛУЙСТА","РАЗБУДИ ЗАМОК","ПЕРЕКУС МОНСТРА","ЗЕРКАЛЬНЫЙ ЛУЧ","ЯРКАЯ ЦЕПЬ","ПОДЪЁМ НА ШАРАХ"}
    }; for(String[] row:m)if(row[0].equals(lang))return row[n+1]; return en[n]; }

    private String tr(String k){
        Map<String,String[]> map=new HashMap<>();
        map.put("menu",new String[]{"MENU","MENÜ","MENU","MENÚ","MENU","MENU","菜单","メニュー","МЕНЮ"});
        map.put("hint",new String[]{"HINT","HILFE","AIUTO","AYUDA","AJUDA","AIDE","提示","ヒント","ПОДСКАЗКА"});
        map.put("play",new String[]{"PLAY","SPIELEN","GIOCA","JUGAR","JOGAR","JOUER","开始","プレイ","ИГРАТЬ"});
        map.put("levels",new String[]{"LEVEL SELECT","LEVELAUSWAHL","SELEZIONE LIVELLI","SELECCIÓN DE NIVEL","SELEÇÃO DE NÍVEL","CHOIX DU NIVEAU","关卡选择","レベル選択","ВЫБОР УРОВНЯ"});
        map.put("settings",new String[]{"SETTINGS","EINSTELLUNGEN","IMPOSTAZIONI","AJUSTES","CONFIGURAÇÕES","PARAMÈTRES","设置","設定","НАСТРОЙКИ"});
        map.put("back",new String[]{"BACK","ZURÜCK","INDIETRO","ATRÁS","VOLTAR","RETOUR","返回","戻る","НАЗАД"});
        map.put("close",new String[]{"CLOSE","SCHLIESSEN","CHIUDI","CERRAR","FECHAR","FERMER","关闭","閉じる","ЗАКРЫТЬ"});
        map.put("solved",new String[]{"SOLVED!","GESCHAFFT!","RISOLTO!","¡RESUELTO!","RESOLVIDO!","RÉUSSI !","完成！","クリア！","РЕШЕНО!"});
        map.put("tag",new String[]{"THINK WEIRD.","DENK SELTSAM.","PENSA STRANO.","PIENSA RARO.","PENSE ESTRANHO.","PENSE BIZARRE.","怪一点。","ヘンに考えよう。","ДУМАЙ СТРАННО."});
        int idx=langIndex(); String[] a=map.get(k); return a==null?k:a[idx];
    }
    private int langIndex(){ String[] c={"en","de","it","es","pt","fr","zh","ja","ru"}; for(int i=0;i<c.length;i++)if(c[i].equals(lang))return i; return 0; }

    private String hintFor(int n){
        String[][] it={
            {"Riduci la chiave con due dita e poi trascinala verso la serratura."},
            {"Inclina il telefono con delicatezza e guida la pallina verso il centro."},
            {"Memorizza il motivo iniziale, poi usa i tre colori per ricrearlo."},
            {"Non fare nulla. Qualsiasi tocco azzera il conto."},
            {"Il vero comando non è sullo schermo: abbassa completamente il volume multimediale."},
            {"Prima scuoti il telefono. Poi tocca il lucchetto quando si sveglia."},
            {"Dai da mangiare al mostro tre snack dal vassoio."},
            {"Ruota entrambi gli specchi finché il raggio raggiunge il bersaglio."},
            {"Ruota i quattro moduli finché l'energia arriva alla lampadina."},
            {"Spingi l'aria verso l'alto con swipe rapidi dal basso."}
        };
        String[][] en={
            {"Pinch the key smaller, then drag it toward the lock."},{"Tilt the phone gently and guide the ball to the center."},{"Memorize the opening pattern, then repaint it with the three colors."},{"Do nothing. Any touch resets the countdown."},{"The real control is outside the game: turn media volume all the way down."},{"Shake the phone first, then tap the lock when it wakes up."},{"Feed the monster three snacks from the tray."},{"Rotate both mirrors until the beam reaches the target."},{"Rotate the four modules until power reaches the bulb."},{"Swipe upward from the bottom to lift the balloons."}
        };
        if("it".equals(lang))return it[n-1][0]; if("en".equals(lang))return en[n-1][0];
        // concise fallback in English for non-Italian preview locales
        return en[n-1][0];
    }
}
