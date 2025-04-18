#  Text‑to‑Handwriting
################################################################################
# 1. UNZIP THE PROJECT                                                         #
################################################################################
#  download  ➜ extract somewhere convenient:

################################################################################
# 2. BACKEND  (Spring‑Boot + FontForge + Potrace)                               #
################################################################################
# ── prerequisites ─────────────────────────────────────────────────────────────
# • Java 17+  (tested on JDK 18)               • Maven 3.8+
# • Python 3                                   • FontForge for Windows
#   ➜ install to  C:\Program Files (x86)\FontForgeBuilds\fontforge.bat
# • Potrace for Windows
#   ➜ extract to  C:\tools\potrace‑1.16.win64\potrace.exe
#   (If you choose other paths, edit them below.)

# ── edit paths once ───────────────────────────────────────────────────────────
# open  backend/src/main/java/backend/controller/FontProcessingService.java
#   private static final String POTRACE_PATH       = "C:/tools/potrace‑1.16.win64/potrace.exe";
#   private static final String FONTFORGE_BAT_PATH = "C:\\Program Files (x86)\\FontForgeBuilds\\fontforge.bat";
#   private static final String FINAL_STORAGE_DIR  = "C:\\Users\\<you>\\Desktop\\TEMP";
# save & close.

# ── build & run ───────────────────────────────────────────────────────────────
$ cd backend
$ mvn clean package           # downloads jars (OpenCV, Spring Boot, etc.)
$ java -jar target/*.jar      # server starts on port 8080
# leave this terminal window running.

################################################################################
# 3. ANDROID APP                                                               #
################################################################################
# open  android/  folder inside **Android Studio**.

# ── IP address edits (2 files) ────────────────────────────────────────────────
#  (a) DrawingActivity.java       ➜ search  baseUrl("http://...")
#      replace with your PC’s LAN IP that runs the backend.
#
#  (b) res/xml/network_security_config.xml
#      <domain includeSubdomains="true">YOUR IP ADDRESS</domain>
#      (update to exact same IP – enables clear‑text HTTP for that host)
#
# ── OpenAI API key -----------------------------------------------------------
#   NoteEditorActivity.java  ➜  private static final String OPENAI_API_KEY =
#   replace **"sk‑XXXXXXXXXXXXXXXXXXXXXXXX"** with your own key.

# ── build & run on device ----------------------------------------------------
# press “Run” in Android Studio → app launches on phone / emulator.

################################################################################
# 4. WORKFLOW                                                                  #
################################################################################
# ① Tap **Start Drawing** → sketch each character (marker slider, Back, Clear).
# ② Tap **Finish** → type font name → progress bar uploads *.zip to backend.
# ③ Backend returns fully‑cropped TTF; app stores it in:
#      /data/data/<package>/files/fonts/<YourFont>.ttf
#    and shows **After Generation** screen:
#      • Notes         – rich editor with your font + AI formatting (GPT‑4o)
#      • Font Manager  – rename / export / delete stored fonts.

################################################################################
# 5. TROUBLESHOOTING                                                           #
################################################################################
# • Retrofit timeout → verify phone & PC on same Wi‑Fi and that IP/port match.
# • Empty SVGs from Potrace → trimming code auto‑crops extra whitespace already,
#   so draw big.
################################################################################
