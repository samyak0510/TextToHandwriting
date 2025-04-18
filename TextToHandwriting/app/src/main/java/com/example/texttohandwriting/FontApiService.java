package com.example.texttohandwriting;


import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit service interface for font-related API communication.
 * Defines endpoints for uploading glyph data and receiving generated font files.
 */
public interface FontApiService {
    /**
     * Uploads a zip file containing handwritten glyph images to the server
     * and receives a generated TTF font file in response.
     * 
     * @param fontZip MultipartBody.Part containing the zipped glyph images
     * @return A Call object with ResponseBody containing the TTF font file
     */
    @Multipart
    @POST("api/generateFont")
    Call<ResponseBody> uploadFontZip(@Part MultipartBody.Part fontZip);
}
