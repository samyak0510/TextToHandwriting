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

## Security Architecture

### Key Management
- **Android Keystore**: Hardware-backed key storage
- **API Key Encryption**: AES-256 encryption for OpenAI keys
- **Certificate Pinning**: HTTPS with certificate validation

### Network Security
```
┌─────────────┐    HTTPS/TLS 1.3    ┌─────────────┐
│ Android App │ ◄─────────────────► │   Backend   │
│             │                     │             │
│ • Key Store │     Encrypted       │ • Config    │
│ • Cert Pin  │     Transport       │ • Validation│
└─────────────┘                     └─────────────┘
```

### Data Protection
- **Input Validation**: Comprehensive sanitization
- **File Size Limits**: 50MB upload maximum
- **Path Traversal Protection**: Secure file handling
- **Memory Management**: Automatic cleanup of temporary files

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

### Resource Management
- **Thread Pools**: Managed by Spring Boot
- **Memory**: Automatic garbage collection
- **File System**: Temporary file cleanup
- **External Processes**: Process lifecycle management

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

### Production Environment
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Load        │    │ Application │    │ External    │
│ Balancer    │◄──►│ Server      │◄──►│ Services    │
│             │    │             │    │             │
│ • SSL Term  │    │ • Spring    │    │ • FontForge │
│ • Routing   │    │ • Java 17   │    │ • Potrace   │
└─────────────┘    └─────────────┘    └─────────────┘
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
- **OpenAI API**: GPT-4 text processing
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
