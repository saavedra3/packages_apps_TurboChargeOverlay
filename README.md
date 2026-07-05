# ⚡ Turbocharge Overlay

A system app designed to display a charging visual effect (overlay) when the device detects fast charging exceeding **10W**.

## 🚀 Description
This app is specifically designed to be compiled as a **system app (priv-app)** within a custom ROM (AOSP/LineageOS/etc.).
Upon detecting that the charger is delivering more than 10W, it displays an animated "TurboPower" effect.

## ⚠️ Integration Requirements (IMPORTANT!)
For the app to function correctly and access system permissions (SELinux), you must follow these steps during the ROM build process:

### 1. Remember add package to Device.mk
Go to `device/<vendor>/<device>/device.mk` and add line:
```makefile
# Set build indication
PRODUCT_PACKAGES += \TurboChargeOverlay
```

### 2. SELinux Configuration
The app requires specific security policies. To ensure the compiler does not ignore them, you must declare them in your `BoardConfig.mk` file (usually located at `device/<brand>/<model>/BoardConfig.mk`):

```makefile
# Add support for TurboChargeOverlay policies
BOARD_SEPOLICY_DIRS += packages/apps/TurboChargeOverlay/sepolicy
