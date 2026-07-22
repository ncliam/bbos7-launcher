# BBOS7 Launcher

A BlackBerry OS 7–style launcher for the **Unihertz / BlackBerry Zinwa Q25** — square 720×720 screen, physical QWERTY keyboard, Android 14, no root required.

[![Latest release](https://img.shields.io/github/v/release/ncliam/bbos7-launcher)](https://github.com/ncliam/bbos7-launcher/releases/latest)

**🇬🇧 English** · [🇻🇳 Tiếng Việt](#tiếng-việt)

---

## English

### What it is

The home screen **is a pull-up tray**, exactly like BBOS7 — no separate icon grid on the wallpaper. Peek shows your pinned favorites; pull up for tabs and the full app list. Just start typing on the physical keyboard anywhere to search instantly.

### Requirements

- Unihertz / BlackBerry Zinwa **Q25** (best experience). Runs on Android 8.0+ but the layout is tuned for the Q25's square screen.
- "Install unknown apps" enabled for your browser or file manager.

### Install

1. Download **`BBOS7-Launcher-x.y.z.apk`** from the [latest release](https://github.com/ncliam/bbos7-launcher/releases/latest).
2. Open the APK and install it (allow "unknown apps" if prompted).
3. Set it as your home app: press **Home**, then pick **BBOS7** → *Always*. (Or *Settings → Apps → Default apps → Home app*.)

No adb, no special permissions, no root.

### Using it

- **Peek** (home): a row of pinned favorites.
- **Pull up** (half / full): tabs + the full app list.
- **Type on the keyboard**: search opens instantly, accent-insensitive (`ky vuong` finds "Kỳ Vương").
- **Long-press an app**: pin/unpin, sort into a tab, assign an Alt-shortcut, app info.
- **Trackpad**: moves the selection; **Alt + a letter** opens the app you mapped to it.

### Build from source

```
git clone https://github.com/ncliam/bbos7-launcher.git
cd bbos7-launcher
./gradlew :app:assembleDebug        # JDK 17, Android SDK 35
```
The APK lands in `app/build/outputs/apk/debug/`. Releases are built and published automatically by GitHub Actions on any `v*` tag.

### License

Not yet decided — see the maintainer before reusing the code.

---

## Tiếng Việt

### Giới thiệu

Màn hình chính **chính là panel kéo lên**, đúng như BBOS7 — không có lưới icon riêng trên wallpaper. Nấc hé (peek) hiện các app yêu thích đã ghim; kéo lên để thấy thanh tab và toàn bộ danh sách app. Gõ chữ trên bàn phím vật lý ở bất kỳ đâu là mở tìm kiếm ngay.

### Yêu cầu

- Unihertz / BlackBerry Zinwa **Q25** (trải nghiệm tốt nhất). Chạy được trên Android 8.0+ nhưng giao diện tối ưu cho màn hình vuông của Q25.
- Đã bật "Cài ứng dụng không rõ nguồn gốc" cho trình duyệt hoặc trình quản lý tệp.

### Cài đặt

1. Tải **`BBOS7-Launcher-x.y.z.apk`** từ [bản phát hành mới nhất](https://github.com/ncliam/bbos7-launcher/releases/latest).
2. Mở file APK và cài (cho phép "nguồn không rõ" nếu được hỏi).
3. Đặt làm màn hình chính: bấm **Home**, chọn **BBOS7** → *Luôn dùng*. (Hoặc *Cài đặt → Ứng dụng → Ứng dụng mặc định → Ứng dụng màn hình chính*.)

Không cần adb, không cần quyền đặc biệt, không cần root.

### Cách dùng

- **Nấc hé** (màn hình chính): một hàng app yêu thích đã ghim.
- **Kéo lên** (nửa / đầy): thanh tab + toàn bộ danh sách app.
- **Gõ bàn phím**: mở tìm kiếm ngay, không phân biệt dấu (`ky vuong` ra "Kỳ Vương").
- **Giữ lâu một app**: ghim/bỏ ghim, sắp vào tab, gán phím tắt Alt, thông tin ứng dụng.
- **Trackpad**: di chuyển vùng chọn; **Alt + một phím chữ** mở app bạn đã gán.

### Tự build từ mã nguồn

```
git clone https://github.com/ncliam/bbos7-launcher.git
cd bbos7-launcher
./gradlew :app:assembleDebug        # JDK 17, Android SDK 35
```
APK nằm ở `app/build/outputs/apk/debug/`. Các bản release được GitHub Actions tự động build và phát hành khi đẩy tag `v*`.

### Giấy phép

Chưa quyết định — liên hệ maintainer trước khi sử dụng lại mã nguồn.
