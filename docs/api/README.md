# API Documentation

## Overview

The TextToHandwriting REST API provides endpoints for font generation and text processing. The API follows RESTful principles and returns JSON responses.

**Base URL**: `http://localhost:8080/api/v1`

## Authentication

Currently, the API does not require authentication for font processing endpoints. However, for AI text formatting features, ensure your OpenAI API key is properly configured in the Android client.

## Endpoints

### Font Processing

#### Upload Font ZIP
Processes handwritten character images and generates a TTF font.

```http
POST /fonts/upload
Content-Type: multipart/form-data
```

**Parameters:**
- `fontZip` (file, required): ZIP file containing PNG images of handwritten characters
  - Image naming convention: `glyph_{ascii_code}.png`
  - Supported characters: A-Z, a-z, 0-9, punctuation
  - Image format: PNG, minimum 200x200px recommended

**Response:**
```http
200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="output_font.ttf"

[Binary TTF file data]
```

**Error Responses:**
```json
{
  "error": "Invalid file format",
  "message": "Only ZIP files are accepted",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Data Models


## Error Handling

The API uses standard HTTP status codes:

- `200 OK`: Successful request
- `400 Bad Request`: Invalid input data
- `413 Payload Too Large`: File size exceeds limit
- `422 Unprocessable Entity`: Valid format but processing failed
- `500 Internal Server Error`: Server-side processing error

## Rate Limiting

Currently, no rate limiting is implemented. For production deployments, consider implementing rate limiting based on IP address or API key.

## Examples

### cURL Example
```bash
curl -X POST http://localhost:8080/api/v1/fonts/upload \
  -F "fontZip=@handwriting_samples.zip" \
  -o generated_font.ttf
```

### Android Retrofit Example
```java
@POST("fonts/upload")
Call<ResponseBody> uploadFontZip(@Part MultipartBody.Part fontZip);
```

## Processing Pipeline

1. **ZIP Extraction**: Extract PNG images from uploaded ZIP
2. **Image Processing**: Crop whitespace and convert to BMP
3. **Vector Conversion**: Use Potrace to convert BMP to SVG
4. **Font Assembly**: Use FontForge to compile SVG files into TTF
5. **Response**: Return generated TTF file

## Performance Considerations

- Processing time scales with number of characters (typically 20-30 seconds for full alphabet)
- Images should be high contrast for best results
- Minimum recommended resolution: 200x200px per character
- Maximum file size: 50MB for ZIP uploads 