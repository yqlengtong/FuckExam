package com.lengtong.fuckexam;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.app.Activity;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class ScreenCaptureService extends Service {

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler handler = new Handler(Looper.getMainLooper());

    private static final String NOTIFICATION_CHANNEL_ID = "ScreenCapture";
    private static final int NOTIFICATION_ID = 1;

    public static Intent data;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals("CAPTURE")) {
            if (mediaProjection != null && imageReader != null) {
                handler.postDelayed(this::captureAndRecognize, 100);
                return START_STICKY;
            }
        }

        if (data == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // 必须设置小图标
                .setContentTitle("科技已启动") // 标题留空
                .setContentText("正在识别中...") // 内容留空
                .setPriority(NotificationCompat.PRIORITY_MIN) // 设置为最低优先级
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
        data = null; // Consume the data to prevent reuse

        if (mediaProjection == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                }
                if (imageReader != null) {
                    imageReader.close();
                }
                mediaProjection = null;
            }
        }, handler);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);

        handler.postDelayed(this::captureAndRecognize, 1000);

        sharedPreferences = getSharedPreferences("AIConfig", MODE_PRIVATE);

        return START_STICKY;
    }

    private SharedPreferences sharedPreferences;
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private void queryAI(String questionText, String model, String apiKey, Callback callback) {
        String url = getApiUrl(model, apiKey);
        String requestBodyStr = buildRequestBody(model, questionText);

        if (url == null || requestBodyStr == null) {
            callback.onFailure(null, new IOException("Invalid AI model or failed to build request body."));
            return;
        }

        RequestBody body = RequestBody.create(requestBodyStr, JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json");

        if (model.equals("通义千问") || model.equals("Kimi") || model.equals("DeepSeek")) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }

        client.newCall(requestBuilder.build()).enqueue(callback);
    }

    private String getApiUrl(String model, String apiKey) {
        switch (model) {
            case "Kimi":
                return "https://api.moonshot.cn/v1/chat/completions";
            case "DeepSeek":
                return "https://api.deepseek.com/v1/chat/completions";
            case "通义千问":
                return "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
            case "文心一言":
                return "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=" + apiKey;
            case "Gemini":
                return "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;
            default:
                return null;
        }
    }

    private String buildRequestBody(String model, String questionText) {
        try {
            JSONObject requestJson = new JSONObject();
            String prompt = "你是一个答题助手，你需要从我下面发送的内容中正确解析出题目并给出正确答案，优先输出正确答案，然后再输出题目信息。内容如下： " + questionText;

            switch (model) {
                case "Kimi":
                    requestJson.put("model", "moonshot-v1-8k")
                               .put("messages", createMessagesPayload("system", "You are a helpful assistant.", "user", prompt));
                    break;
                case "DeepSeek":
                    requestJson.put("model", "deepseek-chat")
                               .put("messages", createMessagesPayload("system", "You are a helpful assistant.", "user", prompt));
                    break;
                case "通义千问":
                    requestJson.put("model", "qwen-turbo")
                            .put("input", new JSONObject().put("messages", createMessagesPayload("system", "You are a helpful assistant.", "user", prompt)))
                            .put("parameters", new JSONObject().put("result_format", "message"));
                    break;
                case "文心一言":
                    requestJson.put("messages", createMessagesPayload(null, null, "user", prompt));
                    break;
                case "Gemini":
                    JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
                    JSONObject content = new JSONObject().put("parts", parts);
                    requestJson.put("contents", new JSONArray().put(content));
                    break;
                default:
                    return null;
            }
            return requestJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONArray createMessagesPayload(String systemRole, String systemContent, String userRole, String userContent) throws Exception {
        JSONArray messages = new JSONArray();
        if (systemRole != null && systemContent != null) {
            messages.put(new JSONObject().put("role", systemRole).put("content", systemContent));
        }
        messages.put(new JSONObject().put("role", userRole).put("content", userContent));
        return messages;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (imageReader != null) {
            imageReader.close();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }

    private void captureAndRecognize() {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            return;
        }

        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());

        recognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    List<Text.TextBlock> blocks = new ArrayList<>(visionText.getTextBlocks());
                    blocks.sort((o1, o2) -> {
                        int top1 = o1.getBoundingBox().top;
                        int top2 = o2.getBoundingBox().top;
                        if (Math.abs(top1 - top2) > 10) { // 容忍一定的高度差
                            return Integer.compare(top1, top2);
                        } else {
                            return Integer.compare(o1.getBoundingBox().left, o2.getBoundingBox().left);
                        }
                    });

                    StringBuilder sortedText = new StringBuilder();
                    for (Text.TextBlock block : blocks) {
                        sortedText.append(block.getText()).append("\n");
                    }

                    String resultText = sortedText.toString();
                    Log.i("OCR_RESULT", "Recognized Text: " + resultText);

                    String selectedAI = sharedPreferences.getString("selected_ai", "");
                    String apiKey = sharedPreferences.getString("api_key", "");
                    if (selectedAI.isEmpty() || apiKey.isEmpty()) {
                        sendNotification("请先配置AI模型和API Key");
                        return;
                    }

                    queryAI(resultText, selectedAI, apiKey, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            sendNotification("AI查询失败: " + e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                sendNotification("AI查询错误: " + response.code());
                                return;
                            }
                            String responseBody = response.body().string();
                            String answer = "";
                            // 解析响应，根据不同模型
                            try {
                                JSONObject json = new JSONObject(responseBody);
                                if (selectedAI.equals("Gemini")) {
                                    answer = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                                } else if (selectedAI.equals("文心一言")) {
                                    answer = json.getString("result");
                                } else {
                                    answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                                }
                            } catch (Exception e) {
                                answer = "解析错误";
                            }
                            Log.i("AI_ANSWER", "AI Answer: " + answer);
                            sendNotification("答案: " + answer);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Text recognition failed", e);
                    sendNotification("识别失败");
                });
    }

    private void sendNotification(String text) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationManager.notify(2, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Screen Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


}