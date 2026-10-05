using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class NoRulesGame : MonoBehaviour
{
    Canvas canvas;
    RectTransform root;
    RawImage background;
    Text titleText;
    Button menuButton, hintButton;
    GameObject hintPanel;
    Text hintText;
    GameObject levelRoot;
    int level = 0;
    float zenTimer;
    bool zenStarted;
    AudioSource quietAudio;
    float quietCheck;

    // level 1
    RectTransform keyRT;
    RawImage keyImage;
    float keyScale = 1f;
    bool draggingKey;
    Vector2 lastTouch;
    float prevPinchDist;

    // level 2
    RectTransform ballRT;
    Vector2 ballPos;
    Vector2 ballVel;
    readonly List<Rect> mazeWalls = new List<Rect>();

    // level 3
    RawImage[] colorTiles = new RawImage[9];
    int[] tileState = new int[9];
    readonly int[] targetPattern = {1,2,3, 3,2,1, 2,3,1}; // pink, yellow, blue
    int selectedColor = 1;
    float patternPreviewTime;
    bool patternHidden;

    // UI normalized zones matching approved composition
    Rect menuZone => new Rect(Screen.width * 0.04f, Screen.height * 0.77f, Screen.width * 0.29f, Screen.height * 0.09f);
    Rect hintZone => new Rect(Screen.width * 0.67f, Screen.height * 0.77f, Screen.width * 0.29f, Screen.height * 0.09f);

    readonly string[] titles = {"", "KEY PROBLEM", "TINY MAZE", "COLOR SPLASH", "ZEN MODE", "QUIET PLEASE"};
    readonly string[] hints = {
        "",
        "The key is much too large. Try changing its size before using it.",
        "The phone itself can move the ball. Reach the glowing center.",
        "Watch the colors first, then repaint the nine cells from memory.",
        "Sometimes the best move is no move at all.",
        "Make the room completely quiet using the phone itself."
    };

    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    static void Bootstrap()
    {
        if (FindObjectOfType<NoRulesGame>() == null)
        {
            var go = new GameObject("NO RULES Game");
            DontDestroyOnLoad(go);
            go.AddComponent<NoRulesGame>();
        }
    }

    void Start()
    {
        Screen.orientation = ScreenOrientation.Portrait;
        Application.targetFrameRate = 60;
        QualitySettings.vSyncCount = 0;
        BuildUI();
        ShowMenu();
    }

    Font BuiltinFont()
    {
        try { return Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf"); }
        catch { return Resources.GetBuiltinResource<Font>("Arial.ttf"); }
    }

    void BuildUI()
    {
        if (FindObjectOfType<EventSystem>() == null)
        {
            var es = new GameObject("EventSystem", typeof(EventSystem), typeof(StandaloneInputModule));
            DontDestroyOnLoad(es);
        }
        var cgo = new GameObject("Canvas", typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
        canvas = cgo.GetComponent<Canvas>();
        canvas.renderMode = RenderMode.ScreenSpaceOverlay;
        var scaler = cgo.GetComponent<CanvasScaler>();
        scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1080, 1920);
        scaler.matchWidthOrHeight = 0.5f;
        root = cgo.GetComponent<RectTransform>();

        background = MakeRaw("Background", root, Vector2.zero, Vector2.one);
        background.raycastTarget = false;

        // Header visual layer intentionally recreated in Unity so text can later be fully localized.
        var header = MakePanel("Header", root, new Vector2(.045f,.825f), new Vector2(.955f,.985f), new Color(0.02f,0.04f,0.17f,0.98f));
        header.GetComponent<Image>().material = null;
        var outline = header.AddComponent<Outline>(); outline.effectColor = new Color(0.05f,0.95f,1f,1f); outline.effectDistance = new Vector2(4,-4);
        titleText = MakeText("Title", header, new Vector2(.04f,.12f), new Vector2(.96f,.88f), "", 72, TextAnchor.MiddleCenter, Color.white);
        titleText.fontStyle = FontStyle.Bold;

        menuButton = MakeButton("MENU", root, new Vector2(.045f,.755f), new Vector2(.34f,.825f), new Color(0.03f,.9f,1f,1f));
        menuButton.onClick.AddListener(ShowMenu);
        hintButton = MakeButton("HINT", root, new Vector2(.67f,.755f), new Vector2(.955f,.825f), new Color(1f,.25f,.72f,1f));
        hintButton.onClick.AddListener(ToggleHint);
        menuButton.gameObject.SetActive(false); hintButton.gameObject.SetActive(false); header.gameObject.SetActive(false);

        hintPanel = MakePanel("HintPanel", root, new Vector2(.08f,.13f), new Vector2(.92f,.36f), new Color(.02f,.04f,.14f,.97f)).gameObject;
        var hpOutline = hintPanel.AddComponent<Outline>(); hpOutline.effectColor = new Color(0.1f,1f,1f,1f); hpOutline.effectDistance = new Vector2(4,-4);
        hintText = MakeText("HintText", hintPanel.GetComponent<RectTransform>(), new Vector2(.08f,.18f), new Vector2(.92f,.82f), "", 42, TextAnchor.MiddleCenter, Color.white);
        var close = MakeButton("CLOSE", hintPanel.GetComponent<RectTransform>(), new Vector2(.33f,.02f), new Vector2(.67f,.18f), new Color(1f,.25f,.72f,1f));
        close.onClick.AddListener(()=>hintPanel.SetActive(false));
        hintPanel.SetActive(false);
    }

    void ShowMenu()
    {
        level = 0;
        ClearLevel();
        menuButton.gameObject.SetActive(false); hintButton.gameObject.SetActive(false); titleText.transform.parent.gameObject.SetActive(false); hintPanel.SetActive(false);
        background.texture = Resources.Load<Texture2D>("Art/menu_collage");
        background.color = Color.white;

        levelRoot = new GameObject("MenuUI", typeof(RectTransform)).GetComponent<RectTransform>();
        levelRoot.SetParent(root,false); Stretch(levelRoot, Vector2.zero, Vector2.one);
        var panel = MakePanel("LevelPanel", levelRoot, new Vector2(.12f,.08f), new Vector2(.88f,.47f), new Color(.02f,.03f,.13f,.88f));
        var ot = panel.gameObject.AddComponent<Outline>(); ot.effectColor = new Color(0.05f,.95f,1f,1f); ot.effectDistance = new Vector2(4,-4);
        MakeText("Select", panel, new Vector2(.05f,.76f), new Vector2(.95f,.96f), "UNITY PREVIEW — LEVELS 1–5", 44, TextAnchor.MiddleCenter, Color.white);
        for(int i=1;i<=5;i++)
        {
            int idx=i;
            int row=(i-1)/3, col=(i-1)%3;
            float x0=.08f+col*.305f, x1=x0+.255f;
            float y1=.72f-row*.34f, y0=y1-.25f;
            var b=MakeButton(i.ToString(), panel, new Vector2(x0,y0), new Vector2(x1,y1), i%2==0?new Color(1f,.27f,.72f,1):new Color(.05f,.9f,1f,1));
            b.onClick.AddListener(()=>LoadLevel(idx));
        }
    }

    void LoadLevel(int n)
    {
        ClearLevel(); level=n; hintPanel.SetActive(false);
        titleText.transform.parent.gameObject.SetActive(true); menuButton.gameObject.SetActive(true); hintButton.gameObject.SetActive(true);
        titleText.text = $"#{n}   {titles[n]}";
        var tex=Resources.Load<Texture2D>($"Art/level{n:00}_clean");
        background.texture=tex; background.color=Color.white;
        levelRoot=new GameObject($"Level{n}", typeof(RectTransform)).GetComponent<RectTransform>(); levelRoot.SetParent(root,false); Stretch(levelRoot,Vector2.zero,Vector2.one);
        if(n==1) SetupLevel1();
        if(n==2) SetupLevel2();
        if(n==3) SetupLevel3();
        if(n==4) { zenTimer=0; zenStarted=true; }
        if(n==5) SetupLevel5();
    }

    void ClearLevel()
    {
        if(levelRoot) Destroy(levelRoot.gameObject);
        levelRoot=null; keyRT=null; ballRT=null; mazeWalls.Clear();
        if(quietAudio) Destroy(quietAudio.gameObject); quietAudio=null;
    }

    void ToggleHint()
    {
        if(level<1) return;
        hintText.text=hints[level]; hintPanel.SetActive(!hintPanel.activeSelf);
    }

    void SetupLevel1()
    {
        var tex=Resources.Load<Texture2D>("Art/level01_key");
        keyImage=MakeRaw("GoldenKey",levelRoot,new Vector2(.08f,.30f),new Vector2(.67f,.72f)); keyImage.texture=tex; keyImage.color=Color.white; keyImage.preserveAspect=true;
        keyRT=keyImage.rectTransform; keyRT.anchorMin=keyRT.anchorMax=new Vector2(.36f,.51f); keyRT.pivot=new Vector2(.5f,.5f); keyRT.sizeDelta=new Vector2(560,720); keyRT.anchoredPosition=Vector2.zero;
        keyScale=1f; keyRT.localScale=Vector3.one;
    }

    void SetupLevel2()
    {
        var tex=Resources.Load<Texture2D>("Art/level02_ball");
        var ri=MakeRaw("Ball",levelRoot,new Vector2(.17f,.39f),new Vector2(.27f,.45f)); ri.texture=tex; ri.preserveAspect=true;
        ballRT=ri.rectTransform; ballRT.anchorMin=ballRT.anchorMax=Vector2.zero; ballRT.pivot=new Vector2(.5f,.5f); ballRT.sizeDelta=new Vector2(80,80);
        ballPos=new Vector2(235,850); ballVel=Vector2.zero; ballRT.anchoredPosition=ballPos;
        // simplified collision rectangles that track the visual maze corridors in the approved concept.
        mazeWalls.Add(new Rect(165,760,610,28)); mazeWalls.Add(new Rect(165,760,28,540)); mazeWalls.Add(new Rect(747,760,28,540)); mazeWalls.Add(new Rect(165,1272,610,28));
        mazeWalls.Add(new Rect(280,850,28,300)); mazeWalls.Add(new Rect(410,760,28,260)); mazeWalls.Add(new Rect(540,980,28,290)); mazeWalls.Add(new Rect(650,820,28,270));
        mazeWalls.Add(new Rect(305,1120,210,28)); mazeWalls.Add(new Rect(430,900,220,28));
    }

    void SetupLevel3()
    {
        patternPreviewTime=2.5f; patternHidden=false; selectedColor=1;
        for(int i=0;i<9;i++)
        {
            int r=i/3,c=i%3;
            float x=.23f+c*.185f, y=.49f+(2-r)*.105f;
            var tile=MakeRaw("Tile"+i,levelRoot,new Vector2(x,y),new Vector2(x+.15f,y+.095f));
            colorTiles[i]=tile; tile.texture=TileTexture(targetPattern[i]); tile.raycastTarget=false; tileState[i]=0;
        }
        // invisible paint bucket buttons, matching the concept positions.
        AddInvisibleTap(levelRoot,new Vector2(.07f,.17f),new Vector2(.34f,.37f),()=>selectedColor=1);
        AddInvisibleTap(levelRoot,new Vector2(.37f,.17f),new Vector2(.63f,.37f),()=>selectedColor=2);
        AddInvisibleTap(levelRoot,new Vector2(.66f,.17f),new Vector2(.94f,.37f),()=>selectedColor=3);
        for(int i=0;i<9;i++) { int k=i; AddInvisibleTap(levelRoot,colorTiles[i].rectTransform.anchorMin,colorTiles[i].rectTransform.anchorMax,()=>PaintTile(k)); }
    }

    Texture2D TileTexture(int c)
    {
        if(c==1) return Resources.Load<Texture2D>("Art/tile_pink");
        if(c==2) return Resources.Load<Texture2D>("Art/tile_yellow");
        if(c==3) return Resources.Load<Texture2D>("Art/tile_blue");
        return Resources.Load<Texture2D>("Art/tile_cover");
    }

    void PaintTile(int i)
    {
        if(!patternHidden) return;
        tileState[i]=selectedColor; colorTiles[i].texture=TileTexture(selectedColor);
        bool ok=true; for(int k=0;k<9;k++) if(tileState[k]!=targetPattern[k]) ok=false;
        if(ok) CompleteLevel();
    }

    void SetupLevel5()
    {
        var go=new GameObject("Noise",typeof(AudioSource)); quietAudio=go.GetComponent<AudioSource>();
        quietAudio.loop=true; quietAudio.playOnAwake=false; quietAudio.volume=.6f;
        int sr=22050, len=sr*2; var clip=AudioClip.Create("ComicNoise",len,1,sr,false); var data=new float[len];
        for(int i=0;i<len;i++) { float t=i/(float)sr; data[i]=(Mathf.Sin(t*2*Mathf.PI*440)+.45f*Mathf.Sin(t*2*Mathf.PI*670))*.16f; }
        clip.SetData(data,0); quietAudio.clip=clip; quietAudio.Play(); quietCheck=0;
    }

    void Update()
    {
        if(level==1) UpdateLevel1();
        else if(level==2) UpdateLevel2();
        else if(level==3) UpdateLevel3();
        else if(level==4) UpdateLevel4();
        else if(level==5) UpdateLevel5();
    }

    bool TouchIn(Rect r, Vector2 p) => r.Contains(p);
    bool TouchIsUI(Vector2 p) => TouchIn(menuZone,p)||TouchIn(hintZone,p)||hintPanel.activeSelf;

    void UpdateLevel1()
    {
        if(keyRT==null || hintPanel.activeSelf) return;
        if(Input.touchCount>=2)
        {
            Touch a=Input.GetTouch(0), b=Input.GetTouch(1); float d=Vector2.Distance(a.position,b.position);
            if(prevPinchDist>1f) { keyScale*=d/prevPinchDist; keyScale=Mathf.Clamp(keyScale,.35f,1.2f); keyRT.localScale=Vector3.one*keyScale; }
            prevPinchDist=d; draggingKey=false; return;
        }
        prevPinchDist=0;
        if(Input.touchCount==1)
        {
            var t=Input.GetTouch(0); Vector2 lp;
            RectTransformUtility.ScreenPointToLocalPointInRectangle(root,t.position,null,out lp);
            if(t.phase==TouchPhase.Began && RectTransformUtility.RectangleContainsScreenPoint(keyRT,t.position,null)) { draggingKey=true; lastTouch=lp; }
            if(draggingKey && (t.phase==TouchPhase.Moved||t.phase==TouchPhase.Stationary)) { Vector2 delta=lp-lastTouch; keyRT.anchoredPosition+=delta; lastTouch=lp; }
            if(t.phase==TouchPhase.Ended && draggingKey)
            {
                draggingKey=false;
                // door target around the keyhole region of concept art
                Vector2 screen=RectTransformUtility.WorldToScreenPoint(null,keyRT.position);
                bool atDoor=screen.x>Screen.width*.56f && screen.x<Screen.width*.90f && screen.y>Screen.height*.29f && screen.y<Screen.height*.65f;
                if(atDoor && keyScale<.62f) CompleteLevel();
            }
        }
    }

    void UpdateLevel2()
    {
        if(ballRT==null || hintPanel.activeSelf) return;
        Vector3 a=Input.acceleration; if(a.sqrMagnitude<.01f) a=new Vector3(Input.GetAxis("Horizontal"),Input.GetAxis("Vertical"),0);
        ballVel += new Vector2(a.x,a.y)*900f*Time.deltaTime; ballVel*=.985f;
        Vector2 next=ballPos+ballVel*Time.deltaTime;
        next.x=Mathf.Clamp(next.x,145,795); next.y=Mathf.Clamp(next.y,740,1320);
        Rect br=new Rect(next.x-28,next.y-28,56,56);
        foreach(var w in mazeWalls) if(br.Overlaps(w)) { ballVel*=-.35f; next=ballPos; break; }
        ballPos=next; ballRT.anchoredPosition=ballPos;
        if(Vector2.Distance(ballPos,new Vector2(470,1030))<58f) CompleteLevel();
    }

    void UpdateLevel3()
    {
        if(patternHidden) return;
        patternPreviewTime-=Time.deltaTime;
        if(patternPreviewTime<=0)
        {
            patternHidden=true; for(int i=0;i<9;i++) colorTiles[i].texture=TileTexture(0);
        }
    }

    void UpdateLevel4()
    {
        if(hintPanel.activeSelf) return;
        bool touched=false;
        for(int i=0;i<Input.touchCount;i++) if(Input.GetTouch(i).phase==TouchPhase.Began && !TouchIsUI(Input.GetTouch(i).position)) touched=true;
        if(Input.GetMouseButtonDown(0) && !TouchIsUI(Input.mousePosition)) touched=true;
        if(touched) zenTimer=0; else zenTimer+=Time.deltaTime;
        if(zenTimer>=7f) CompleteLevel();
    }

    void UpdateLevel5()
    {
        quietCheck-=Time.deltaTime; if(quietCheck>0) return; quietCheck=.20f;
        if(GetMediaVolume01()<=.001f) CompleteLevel();
    }

    float GetMediaVolume01()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try {
            using(var up=new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using(var activity=up.GetStatic<AndroidJavaObject>("currentActivity"))
            using(var am=activity.Call<AndroidJavaObject>("getSystemService","audio")) {
                int cur=am.Call<int>("getStreamVolume",3); int max=am.Call<int>("getStreamMaxVolume",3); return max>0?cur/(float)max:0;
            }
        } catch { return 1f; }
#else
        return AudioListener.volume;
#endif
    }

    void CompleteLevel()
    {
        if(level<1) return; int done=level;
        level=0; if(quietAudio) quietAudio.Stop();
        var overlay=MakePanel("Complete",root,new Vector2(.08f,.36f),new Vector2(.92f,.64f),new Color(.01f,.03f,.12f,.96f));
        var ol=overlay.gameObject.AddComponent<Outline>(); ol.effectColor=new Color(.1f,1f,.75f,1); ol.effectDistance=new Vector2(5,-5);
        MakeText("Done",overlay,new Vector2(.08f,.55f),new Vector2(.92f,.9f),"SOLVED!",72,TextAnchor.MiddleCenter,new Color(1f,.9f,.2f,1));
        var next=MakeButton(done<5?"NEXT":"MENU",overlay,new Vector2(.2f,.12f),new Vector2(.8f,.44f),new Color(.05f,.9f,1f,1));
        next.onClick.AddListener(()=>{ Destroy(overlay.gameObject); if(done<5) LoadLevel(done+1); else ShowMenu(); });
    }

    RawImage MakeRaw(string name, Transform parent, Vector2 amin, Vector2 amax)
    {
        var go=new GameObject(name,typeof(RectTransform),typeof(RawImage)); var rt=go.GetComponent<RectTransform>(); rt.SetParent(parent,false); Stretch(rt,amin,amax); return go.GetComponent<RawImage>();
    }
    RectTransform MakePanel(string name, Transform parent, Vector2 amin, Vector2 amax, Color c)
    {
        var go=new GameObject(name,typeof(RectTransform),typeof(Image)); var rt=go.GetComponent<RectTransform>(); rt.SetParent(parent,false); Stretch(rt,amin,amax); go.GetComponent<Image>().color=c; return rt;
    }
    Text MakeText(string name, Transform parent, Vector2 amin, Vector2 amax, string value, int size, TextAnchor anchor, Color color)
    {
        var go=new GameObject(name,typeof(RectTransform),typeof(Text)); var rt=go.GetComponent<RectTransform>(); rt.SetParent(parent,false); Stretch(rt,amin,amax); var t=go.GetComponent<Text>(); t.text=value; t.font=BuiltinFont(); t.fontSize=size; t.alignment=anchor; t.color=color; t.resizeTextForBestFit=true; t.resizeTextMinSize=18; t.resizeTextMaxSize=size; return t;
    }
    Button MakeButton(string label, Transform parent, Vector2 amin, Vector2 amax, Color color)
    {
        var rt=MakePanel(label+"Button",parent,amin,amax,color); var img=rt.GetComponent<Image>(); var outline=rt.gameObject.AddComponent<Outline>(); outline.effectColor=new Color(.02f,.02f,.15f,1); outline.effectDistance=new Vector2(5,-5); var b=rt.gameObject.AddComponent<Button>(); b.targetGraphic=img; var txt=MakeText("Label",rt,new Vector2(.05f,.08f),new Vector2(.95f,.92f),label,44,TextAnchor.MiddleCenter,new Color(.02f,.03f,.14f,1)); txt.fontStyle=FontStyle.Bold; return b;
    }
    void AddInvisibleTap(Transform parent, Vector2 amin, Vector2 amax, Action action)
    {
        var rt=MakePanel("TapZone",parent,amin,amax,new Color(0,0,0,0)); var b=rt.gameObject.AddComponent<Button>(); b.targetGraphic=rt.GetComponent<Image>(); b.onClick.AddListener(()=>action());
    }
    void Stretch(RectTransform rt, Vector2 amin, Vector2 amax)
    {
        rt.anchorMin=amin; rt.anchorMax=amax; rt.offsetMin=Vector2.zero; rt.offsetMax=Vector2.zero;
    }
}
