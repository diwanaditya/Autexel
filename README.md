# Autexel 
> **Scan - Digitize - Export** --- 100% on---device AI. No API. No internet.

---

## Quick Start (Build in 5 minutes)

### Step 1 --- Open in Android Studio
1. Open **Android Studio** ---> **Open** ---> select `Autexel` folder
2. Wait for Gradle sync (~3---5 min, needs internet first time only)

### Step 2 --- Run on Device
1. Enable **USB Debugging** on your Android phone
2. Connect via USB ---> press > **Run**

### Step 3 --- Build Release APK / AAB
```
Build ---> Generate Signed Bundle/APK
```

---

## Play Store --- Complete Checklist

### [OK] Already Done (in this project)
--- [x] Proper `applicationId`, `versionCode`, `versionName`
--- [x] `targetSdk 34` (meets Google's 2024 requirement)
--- [x] `minSdk 26` (~95% device coverage)
--- [x] All permissions declared correctly
--- [x] FileProvider configured
--- [x] ProGuard + resource shrinking enabled
--- [x] AAB bundle splits configured
--- [x] Network security config
--- [x] 512x512 Play Store icon (`playstore_icon_512.png`)
--- [x] 1024x500 Feature Graphic (`playstore_feature_graphic_1024x500.png`)
--- [x] Privacy Policy HTML (`playstore_assets/privacy_policy.html`)
--- [x] Store listing text (`playstore_assets/store_listing.txt`)

### [ ] You Need To Do

#### 1. Generate Keystore (one---time, ~2 min)
```bash
cd playstore_assets
chmod +x generate_keystore.sh
./generate_keystore.sh
```
[!] **SAVE the .jks file and passwords forever** --- losing it === can't update app

#### 2. Add Keystore to build.gradle
In `app/build.gradle`, uncomment and fill the `signingConfigs.release` block:
```groovy
signingConfigs {
 release {
 storeFile file("../autexel_release_key.jks")
 storePassword "your_password"
 keyAlias "autexel_key"
 keyPassword "your_key_password"
 }
}
```
Also uncomment `signingConfig signingConfigs.release` in `buildTypes.release`.

#### 3. Host Privacy Policy (free, ~5 min)
**Option A --- GitHub Pages (recommended):**
1. Create a GitHub repo named `autexel---privacy`
2. Upload `playstore_assets/privacy_policy.html` as `index.html`
3. Go to repo Settings ---> Pages ---> Enable
4. Your URL: `https://YOUR_USERNAME.github.io/autexel---privacy`

**Option B --- Google Sites:**
1. Go to sites.google.com ---> New Site
2. Paste privacy policy text ---> Publish

#### 4. Create Play Console Account
1. Go to **play.google.com/console**
2. Pay **one---time $25** registration fee
3. Complete identity verification (1---3 days)

#### 5. Create App & Upload
1. Play Console ---> **Create app**
2. Fill store listing using `playstore_assets/store_listing.txt`
3. Upload `playstore_icon_512.png` as High---res icon
4. Upload `playstore_feature_graphic_1024x500.png`
5. Take 4+ screenshots from your phone
6. Add your privacy policy URL
7. Build signed AAB ---> upload to **Production**
8. Submit for review (3---7 days for first app)

---

## Project Structure
```
Autexel/
--- app/src/main/
--- --- java/com/autexel/app/
--- --- --- ui/splash/ ---> SplashActivity.kt
--- --- --- ui/home/ ---> HomeActivity.kt
--- --- --- ui/camera/ ---> CameraActivity.kt (ML Kit OCR)
--- --- --- ui/excel/ ---> ExcelActivity.kt (table editor)
--- --- --- ui/invoice/ ---> InvoiceActivity.kt (invoice editor)
--- --- --- parser/ ---> TextParser.kt (on---device AI)
--- --- --- utils/ ---> ExcelExporter, PdfGenerator, FileHelper
--- --- res/ ---> Layouts, icons, themes, colors
--- playstore_assets/
--- --- privacy_policy.html < Host this online
--- --- store_listing.txt < Copy---paste to Play Console
--- --- generate_keystore.sh < Run to create signing key
--- --- playstore_icon_512.png < Upload to Play Console
--- --- playstore_feature_graphic.png < Upload to Play Console
--- gradle/wrapper/ ---> Gradle wrapper config
--- gradlew / gradlew.bat ---> Build scripts
--- README.md
```

---

## Timeline to Go Live
| Step | Time |
|---|---|
| Build + test on device | 1---2 hours |
| Generate keystore + sign | 15 minutes |
| Host privacy policy | 10 minutes |
| Create Play Console account | 15 min + **1---3 days** (verification) |
| Prepare store listing + screenshots | 1---2 hours |
| Google review after submission | **3---7 days** |
| **Total** | **~1 week** |

---

## Troubleshooting
| Problem | Fix |
|---|---|
| Gradle sync fails | Check internet; `File ---> Invalidate Caches ---> Restart` |
| "SDK not found" | `File ---> Project Structure ---> SDK Location` |
| Camera black screen | Use physical device (emulator camera limited) |
| Build fails (POI conflict) | `packagingOptions` excludes already in build.gradle |
| APK not installing | Enable "Install from Unknown Sources" on device |

---

*Built with Kotlin, CameraX, ML Kit, Apache POI, iTextPDF, Material3*
