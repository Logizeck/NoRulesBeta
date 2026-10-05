#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

[InitializeOnLoad]
public static class NoRulesProjectSetup
{
    static NoRulesProjectSetup()
    {
        EditorApplication.delayCall += Configure;
    }

    [MenuItem("NO RULES/Configure Android Preview")]
    public static void Configure()
    {
        PlayerSettings.productName = "NO RULES — Unity Preview";
        PlayerSettings.companyName = "NO RULES";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.norules.unitypreview");
        PlayerSettings.defaultInterfaceOrientation = UIOrientation.Portrait;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel23;
        PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64 | AndroidArchitecture.ARMv7;
        EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene("Assets/Scenes/Main.unity", true) };
        AssetDatabase.SaveAssets();
    }
}
#endif
