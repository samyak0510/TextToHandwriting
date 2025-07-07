# Architecture Overview

## System Architecture

TextToHandwriting follows a distributed microservice architecture with clear separation between frontend, backend, and external processing services.

### High-Level Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Android App   │◄──►│   Spring Boot    │◄──►│  External Tools │
│                 │    │     Backend      │    │                 │
│  • Drawing UI   │    │                  │    │  • FontForge    │
│  • Font Manager │    │  • REST API      │    │  • Potrace      │
│  • Text Editor  │    │  • Image Proc.   │    │  • OpenAI API   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Component Architecture

### Android Application

```
┌─────────────────────────────────────────────────────────────┐
│                     Android App Layer                       │
├─────────────────────────────────────────────────────────────┤
│  UI Layer                                                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │ MainActivity│ │DrawingActivity│ │ NoteEditorActivity      │ │
│  │             │ │             │ │                         │ │
│  │ • Font List │ │ • Canvas    │ │ • Rich Text Editor      │ │
│  │ • Navigation│ │ • Controls  │ │ • AI Integration        │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Business Logic Layer                                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │ GlyphMap    │ │ FileUtil    │ │ NetworkConfig           │ │
│  │             │ │             │ │                         │ │
│  │ • Char Data │ │ • File I/O  │ │ • Auto IP Detection     │ │
│  │ • Bitmap    │ │ • ZIP Utils │ │ • Retrofit Setup        │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Security Layer                                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │ Keystore    │ │ Network Sec │ │ Input Validation        │ │
│  │             │ │             │ │                         │ │
│  │ • API Keys  │ │ • TLS Config│ │ • Data Sanitization     │ │
│  │ • Encryption│ │ • Cert Pin  │ │ • Size Limits           │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Backend Service

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                    │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer                                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │FontController│ │HealthCheck │ │ ErrorHandler            │ │
│  │             │ │             │ │                         │ │
│  │ • Upload    │ │ • Status    │ │ • Global Exception      │ │
│  │ • Process   │ │ • Monitor   │ │ • Error Responses       │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │FontProcessor│ │ PathResolver│ │ ConfigManager           │ │
│  │             │ │             │ │                         │ │
│  │ • Image Proc│ │ • Tool Paths│ │ • Auto-Configuration    │ │
│  │ • Pipeline  │ │ • Detection │ │ • Environment Setup     │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │ File System │ │ Process Mgr │ │ Async Processing        │ │
│  │             │ │             │ │                         │ │
│  │ • Storage   │ │ • External  │ │ • CompletableFuture     │ │
│  │ • Cleanup   │ │ • Commands  │ │ • Thread Pools          │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### Font Generation Pipeline

```
1. User Drawing Phase
   ┌─────────────┐
   │ User draws  │
   │ characters  │
   │ on canvas   │
   └─────┬───────┘
         │
         ▼
2. Client Processing
   ┌─────────────┐
   │ Capture     │
   │ bitmap data │
   │ per glyph   │
   └─────┬───────┘
         │
         ▼
3. ZIP Creation
   ┌─────────────┐
   │ Package     │
   │ PNG files   │
   │ into ZIP    │
   └─────┬───────┘
         │
         ▼
4. Server Upload
   ┌─────────────┐
   │ HTTP POST   │
   │ multipart   │
   │ form data   │
   └─────┬───────┘
         │
         ▼
5. Backend Processing
   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
   │ Extract ZIP │ -> │ Process     │ -> │ Vector      │
   │             │    │ images      │    │ conversion  │
   └─────────────┘    └─────────────┘    └─────┬───────┘
                                              │
                                              ▼
6. Font Assembly
   ┌─────────────┐    ┌─────────────┐
   │ FontForge   │ -> │ TTF         │
   │ processing  │    │ generation  │
   └─────────────┘    └─────┬───────┘
                           │
                           ▼
7. Response
   ┌─────────────┐
   │ Return TTF  │
   │ binary data │
   │ to client   │
   └─────────────┘
```

## Performance Architecture

### Asynchronous Processing
```
┌─────────────┐
│ HTTP Request│
│   Thread    │
└─────┬───────┘
      │
      ▼
┌─────────────┐
│ Controller  │
│  (Fast)     │
└─────┬───────┘
      │
      ▼
┌─────────────┐    ┌─────────────┐
│ Async Task  │    │ Background  │
│ Queue       │ -> │ Processing  │
└─────────────┘    └─────┬───────┘
                         │
                         ▼
                   ┌─────────────┐
                   │ Response    │
                   │ Future      │
                   └─────────────┘
```

## Deployment Architecture

### Development Environment
```
┌─────────────┐    ┌─────────────┐
│ Developer   │    │ Local       │
│ Machine     │    │ Android     │
│             │    │ Device/Emu  │
│ • Backend   │◄──►│             │
│ • Tools     │    │ • App Debug │
└─────────────┘    └─────────────┘
```

## Technology Stack

### Frontend (Android)
- **Language**: Java 11+
- **UI Framework**: Android Views (traditional)
- **HTTP Client**: Retrofit 2.9+
- **Security**: Android Keystore System
- **Build System**: Gradle

### Backend (Spring Boot)
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Web**: Spring MVC
- **Async Processing**: CompletableFuture
- **Build System**: Maven

### External Dependencies
- **FontForge**: Font creation and editing
- **Potrace**: Bitmap to vector conversion
- **OpenAI API**: GPT-4o text processing
- **Python**: FontForge scripting runtime

## Scalability Considerations

### Horizontal Scaling
- **Stateless Design**: No server-side sessions
- **Load Balancing**: Multiple backend instances
- **Shared Storage**: Network-attached storage for fonts

### Performance Optimization
- **Caching**: Redis for frequently accessed data
- **CDN**: Static asset delivery
- **Database**: PostgreSQL for metadata
- **Monitoring**: Prometheus + Grafana
