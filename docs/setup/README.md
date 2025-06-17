# Setup Guide

This guide will walk you through setting up the TextToHandwriting development environment from scratch.

## Development Environment Setup

### 1. Prerequisites Installation

#### Java Development Kit (JDK)
```bash
# Install OpenJDK 17 (recommended)
# Windows (using Chocolatey)
choco install openjdk17

# macOS (using Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Verify installation
java -version
javac -version
```

#### Android Development
1. **Download Android Studio**: [https://developer.android.com/studio](https://developer.android.com/studio)
2. **Install Android SDK**: API Level 24+ (Android 7.0)
3. **Configure Android Virtual Device (AVD)** for testing

#### Maven
```bash
# Windows (using Chocolatey)
choco install maven

# macOS (using Homebrew) 
brew install maven

# Ubuntu/Debian
sudo apt install maven

# Verify installation
mvn -version
```

### 2. External Dependencies

#### FontForge Installation

**Windows:**
1. Download from [FontForge Windows Builds](https://fontforge.org/en-US/downloads/windows/)
2. Install to default location: `C:\Program Files (x86)\FontForgeBuilds\`
3. Add to PATH or note installation directory

**macOS:**
```bash
brew install fontforge
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install fontforge
```

#### Potrace Installation

**Windows:**
1. Download from [Potrace Downloads](http://potrace.sourceforge.net/#downloading)
2. Extract to `C:\tools\potrace-1.16.win64\`
3. Note the path to `potrace.exe`

**macOS:**
```bash
brew install potrace
```

**Ubuntu/Debian:**
```bash
sudo apt install potrace
```

### 3. Project Setup

#### Clone and Initialize
```bash
git clone https://github.com/samyak0510/TextToHandwriting.git
cd TextToHandwriting

# Create necessary directories if they don't exist
mkdir -p assets/images docs/architecture tests/integration
```

#### Backend Configuration
1. **Create application.properties**:
```bash
cd src/backend/src/main/resources
cp application.properties.template application.properties
```

2. **Edit configuration** (`application.properties`):
```properties
# Auto-detection will be attempted first
fontforge.path=${FONTFORGE_PATH:auto}
potrace.path=${POTRACE_PATH:auto}

# Override if auto-detection fails
# fontforge.path=C:\\Program Files (x86)\\FontForgeBuilds\\fontforge.bat
# potrace.path=C:/tools/potrace-1.16.win64/potrace.exe

# Storage configuration
storage.directory=${HOME}/Desktop/TextToHandwriting/fonts
server.port=8080

# File upload limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

#### Android Configuration

1. **Open Android Project**:
   - Launch Android Studio
   - Open `src/android/` directory
   - Wait for Gradle sync to complete

2. **Configure Network Security** (for development):
   The app will auto-detect your development server IP address.

3. **Set up OpenAI API Key** (securely):
   - The app uses Android Keystore for secure key storage
   - You'll need to provide your OpenAI API key during first run
   - No hardcoded keys in source code

### 4. Build and Run

#### Backend Service
```bash
cd src/backend

# Clean build
mvn clean compile

# Run tests
mvn test

# Start development server
mvn spring-boot:run

# Server will start at http://localhost:8080
```

#### Android Application
1. **Connect Android Device** or start **Android Emulator**
2. **In Android Studio**:
   - Click "Run" button
   - Select target device
   - Wait for build and installation

### 5. Environment Variables

Create a `.env` file for local development:
```bash
# Backend
FONTFORGE_PATH=/usr/local/bin/fontforge
POTRACE_PATH=/usr/local/bin/potrace
STORAGE_DIR=/home/user/fonts
SERVER_PORT=8080

# Android (for build scripts)
ANDROID_SDK_ROOT=/Users/user/Library/Android/sdk
ANDROID_HOME=/Users/user/Library/Android/sdk
```

## Next Steps

- Read the [Architecture Overview](../architecture/README.md)
- Check the [API Documentation](../api/README.md)
- Review the [Contributing Guide](../../CONTRIBUTING.md)
- Set up your development branch following our Git workflow 