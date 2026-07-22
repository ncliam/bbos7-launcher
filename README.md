# BBOS7 Launcher cho Zinwa Q25

Launcher Android tái tạo giao diện BlackBerry OS 7, viết riêng cho Zinwa Q25
(màn hình vuông 720×720, bàn phím QWERTY vật lý, Android 14, chưa root).

## Kiến trúc

Màn hình chính **chính là panel kéo lên**, đúng như BBOS7 — không có lưới icon
riêng ở lớp nền. Phía sau panel chỉ có wallpaper và thanh thông tin.

- Nấc PEEK: panel hé một hàng, hiện tab Yêu thích (app đã ghim) — đây là "màn hình chính".
- Kéo lên HALF/FULL: hiện thanh tab và toàn bộ danh sách của tab đang chọn.
- Gõ chữ trên bàn phím vật lý ở bất kỳ đâu: mở tìm kiếm ngay, không cần bấm gì trước.

## Trạng thái — Đợt 2

Đã có:
- `LauncherActivity` — HOME intent, Back thoát tìm kiếm rồi mới thu gọn panel,
  không bao giờ thoát ra màn hình đen.
- `AppRepository` — quét app, tự cập nhật khi cài/gỡ, Collator tiếng Việt,
  đếm số lần mở app cục bộ (không dùng `UsageStatsManager`).
- `PinnedApps` — app ghim, seed 8 app lõi lần chạy đầu, ghim/gỡ qua menu giữ lâu.
- `TrayLayout` — panel ba nấc PEEK / HALF / FULL, tự viết.
- `AppTab` / `AppClassifier` — 5 tab: Yêu thích, Tất cả, Đa phương tiện, Hay dùng, Đã tải.
  Tab Đa phương tiện lấy theo intent category của hệ thống, không đoán theo tên package.
- `StatusPanelView` — vùng đỉnh: đồng hồ lớn, nút hồ sơ âm thanh, nút tìm kiếm.
  Status bar Android hiện thường trực, layout vẽ tràn ra sau nó nên lớp phủ liền mạch.
- `TabCarouselView` — thanh tab kiểu carousel, trong suốt khi thường và chuyển
  thành dải xanh khi trackpad focus vào. Bấm để gập/mở panel, trái/phải để đổi tab.
- Điều hướng bằng trackpad: khung xanh bao icon đang chọn, tên app hiện ở dòng
  riêng trên lưới, focus không lọt sang tab khác và không chạy xuống icon bị che.
- Tìm kiếm không phân biệt dấu (`ky vuong` → "Kỳ Vương").

**Việc tiếp theo:** bộ icon + wallpaper vector BBOS7 tự vẽ — hiện vẫn dùng icon gốc
của từng app nên nhìn vẫn "Android" chứ chưa ra chất BlackBerry.

Sau đó: sắp xếp lại thứ tự icon đã ghim bằng kéo thả.

## Quyền

Launcher không xin quyền nào cả. Đặc biệt là **không**
đọc SMS hay thông báo — máy này dùng cho ngân hàng, nên đánh đổi là bỏ huy hiệu
đếm tin nhắn chưa đọc của BBOS7 gốc. Hồ sơ âm thanh chỉ đổi Chuông ↔ Rung; chế độ
Im lặng đòi quyền Do Not Disturb nên không làm.

## Build

```sh
./build.sh                 # assembleDebug
./build.sh assembleRelease # cần các biến BBOS7_KEYSTORE* để ký
```

`build.sh` bắt buộc phải dùng: JDK mặc định của máy là 23, còn AGP cần 17
(`/opt/homebrew/opt/openjdk@17`).

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Cài lên máy

Máy phải ở chế độ File Transfer/MTP thì `adb` mới thấy.

```sh
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
$ADB devices                                    # phải thấy máy
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell cmd package set-home-activity com.q25.bbos7/.LauncherActivity
```

Sau mỗi lần `install -r`, hệ thống có thể tạm rơi về launcher cũ dù mặc định vẫn
là BBOS7 — bấm Home một lần nữa là về đúng.

### Quay về launcher cũ

```sh
$ADB shell cmd package set-home-activity <package-launcher-cũ>/<activity>
# hoặc gỡ hẳn: $ADB uninstall com.q25.bbos7
```

Gỡ APK xong, bấm Home sẽ hiện lại hộp thoại chọn launcher.

## Đo thời gian khởi động

```sh
$ADB shell am start -W -n com.q25.bbos7/.LauncherActivity   # mục tiêu TotalTime < 400ms
```
