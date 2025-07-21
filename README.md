# 📱 Dahmer - Digital Scale App

**Transform your Android phone into a precision digital scale using advanced touch pressure algorithms.**

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

</div>

## 🎯 **About**

Dahmer is an innovative Android application that leverages your device's touch pressure sensors and accelerometer to create a functional digital scale. Using advanced physics algorithms and machine learning techniques, it can accurately measure small objects up to 300 grams.

### ✨ **Key Features**

- **🔬 Physics-Based Measurement**: Implements proper Force = Pressure × Area calculations
- **⚖️ Dual Calibration System**: Zero-point and weight-based calibration for accuracy
- **📊 Real-time Visualization**: Live force index and weight display with smooth animations
- **🛡️ Safety Warnings**: Automatic alerts for weights that could damage your screen
- **🎨 Modern UI**: Beautiful Material Design 3 interface with landscape optimization
- **📱 Multi-Device Support**: Works on phones and tablets with pressure sensors
- **💾 Persistent Calibration**: Saves your calibration settings between sessions
- **🔄 Unit Conversion**: Switch between grams and ounces

## 🚀 **How It Works**

### Physics Algorithm
```
Force Index = Average Touch Pressure × Contact Area
Weight (grams) = Force Index ÷ Calibration Factor
```

The app uses a sophisticated two-step calibration process:

1. **Zero Calibration**: Establishes baseline when screen is empty
2. **Weight Calibration**: Uses known reference weight to calculate conversion factor

### Supported Weight Range
- **Minimum**: 0.5 grams (detection threshold)
- **Recommended**: Up to 200 grams
- **Maximum Safe**: 300 grams (with warnings)

## 📋 **Requirements**

- **Android 7.0** (API level 24) or higher
- **Touch Pressure Sensor** (most modern Android devices)
- **Accelerometer** (standard on all Android devices)
- **4GB RAM** recommended for smooth operation

### Device Compatibility
✅ Samsung Galaxy series (S8+, Note 8+)  
✅ Google Pixel series  
✅ OnePlus devices  
✅ Most flagship Android phones from 2017+  
❓ Check app compatibility before installation

## 🛠️ **Installation**

### Option 1: Download APK
1. Go to [Releases](../../releases)
2. Download the latest APK file
3. Enable "Install from Unknown Sources" in Android settings
4. Install the APK

### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/EXELANCE-LLC/Dahmer-Weight-VT.git
cd Dahmer-Weight-VT

# Open in Android Studio
# Build and run on your device
```

## 📖 **Usage Guide**

### Initial Setup
1. **Launch the app** and grant necessary permissions
2. **Check compatibility** - app will notify if sensors are unavailable
3. **Start scale** by tapping the START SCALE button

### Calibration Process
1. **Zero Calibration**:
   - Ensure screen is completely clean and empty
   - Tap "Calibration" → "Zero Cal."
   - Wait for confirmation

2. **Weight Calibration**:
   - Place a known-weight object (recommended: 5-20g coin)
   - Enter the exact weight in the dialog
   - Tap "Weight Cal."
   - Calibration complete! ✅

### Measuring Objects
1. **Clean the screen** thoroughly
2. **Place object** gently in the center of the weighing area
3. **Apply gentle pressure** for stable reading
4. **Read the measurement** in the main display

### Best Practices
- 🧽 **Keep screen clean** for accurate readings
- 🪶 **Use light pressure** - excessive force can damage screen
- 📐 **Center objects** on the weighing area
- ⏱️ **Wait for stabilization** (2-3 seconds)
- 🔄 **Re-calibrate periodically** for best accuracy

## ⚠️ **Safety Guidelines**

### Important Warnings
- **Maximum 300g**: Exceeding this may damage your screen
- **No sharp objects**: Use protective layer if needed
- **Gentle pressure only**: Let object's weight do the measuring
- **Clean surface**: Dust/debris affects accuracy
- **Room temperature**: Extreme temperatures affect sensors

### Not Suitable For
❌ Heavy objects (>300g)  
❌ Sharp or pointed items  
❌ Wet or liquid materials  
❌ Professional/commercial weighing  
❌ Medical dosage measurements  

## 🏗️ **Technical Architecture**

### Core Components
- **TouchPressureManager**: Sensor data processing and physics calculations
- **ScaleViewModel**: Business logic and state management
- **ScaleScreen**: Modern Compose UI with real-time updates
- **PreferencesManager**: Calibration data persistence

### Technologies Used
- **Kotlin**: 100% Kotlin codebase
- **Jetpack Compose**: Modern declarative UI
- **Coroutines**: Asynchronous programming
- **StateFlow**: Reactive state management
- **Material Design 3**: Latest design system
- **Android Sensors API**: Hardware sensor access

## 🤝 **Contributing**

We welcome contributions! Here's how you can help:

### Development Setup
```bash
# Fork the repository
# Clone your fork
git clone https://github.com/EXELANCE-LLC/Dahmer-Weight-VT.git

# Create feature branch
git checkout -b feature/amazing-feature

# Make your changes
# Test thoroughly on real devices

# Commit and push
git commit -m "Add amazing feature"
git push origin feature/amazing-feature

# Create Pull Request
```

### Areas for Contribution
- 🐛 **Bug fixes** and stability improvements
- 📱 **Device compatibility** testing and fixes
- 🎨 **UI/UX enhancements** and animations
- 🔬 **Algorithm improvements** and accuracy tuning
- 🌍 **Internationalization** and translations
- 📚 **Documentation** and tutorials

## 📊 **Accuracy & Limitations**

### Expected Accuracy
- ±2-5 grams for objects 10-100g
- ±5-10 grams for objects 100-300g
- Better accuracy with proper calibration

### Factors Affecting Accuracy
- **Device quality**: Premium phones have better sensors
- **Calibration**: Regular re-calibration improves accuracy
- **Environment**: Temperature, humidity, vibrations
- **Object properties**: Shape, material, surface contact

### This is NOT a Professional Scale
⚖️ This app is designed for **approximate measurements** and **educational purposes**. For precision weighing needs (jewelry, medication, etc.), please use a professional digital scale.

## 📝 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 **Authors**

**EXELANCE LLC**
- 📧 Email: [contact@webusta.org](mailto:contact@webusta.org)
- 🌐 Website: [www.webusta.org](https://www.webusta.org)
- 💼 LinkedIn: [EXELANCE LLC](https://linkedin.com/company/white-hat)

## 🙏 **Acknowledgments**

- Android Sensors API documentation and community
- Material Design 3 guidelines and components
- Physics algorithms and calibration techniques research
- Beta testers and community feedback

## 📱 **Screenshots**

<div align="center">

| Main Scale Interface | Calibration System | Settings & Controls |
|:---:|:---:|:---:|
| ![Main](screenshots/main_scale.png) | ![Calibration](screenshots/calibration.png) | ![Settings](screenshots/settings.png) |

*Screenshots showing the modern Material Design 3 interface*

</div>

## 🔄 **Version History**

### v1.0.0 (Latest)
- ✅ Initial release with core functionality
- ✅ Physics-based measurement algorithm
- ✅ Dual calibration system
- ✅ Material Design 3 UI
- ✅ Safety warnings and limits
- ✅ Landscape mode optimization

### Upcoming Features
- 🔜 **Multi-language support**
- 🔜 **Measurement history and export**
- 🔜 **Advanced statistics and analytics**
- 🔜 **Cloud sync for calibration data**
- 🔜 **Voice commands and accessibility**

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ by EXELANCE LLC

</div>
