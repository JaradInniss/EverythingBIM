package com.example.everythingbim.ui.home;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ParliamentClassifier implements AutoCloseable {
    private static final String MODEL_ASSET_NAME = "parliament_classifier.tflite";
    private static final int INPUT_SIZE = 224;
    private static final int CHANNEL_COUNT = 3;
    private static final float PARLIAMENT_THRESHOLD = 0.80f;

    private final Interpreter interpreter;

    public ParliamentClassifier(@NonNull Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModelFile(context), options);
    }

    @NonNull
    public Result predict(@NonNull Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * CHANNEL_COUNT)
                .order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        for (int pixel : pixels) {
            inputBuffer.putFloat((pixel >> 16) & 0xFF);
            inputBuffer.putFloat((pixel >> 8) & 0xFF);
            inputBuffer.putFloat(pixel & 0xFF);
        }
        inputBuffer.rewind();

        float[][] output = new float[1][1];
        interpreter.run(inputBuffer, output);

        float parliamentProbability = Math.max(0f, Math.min(1f, output[0][0]));
        float otherProbability = 1f - parliamentProbability;
        String label = parliamentProbability >= PARLIAMENT_THRESHOLD ? "parliament" : "other";
        return new Result(label, parliamentProbability, otherProbability);
    }

    @Override
    public void close() {
        interpreter.close();
    }

    @NonNull
    private MappedByteBuffer loadModelFile(@NonNull Context context) throws IOException {
        try (android.content.res.AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_ASSET_NAME);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel channel = inputStream.getChannel();
            return channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getDeclaredLength()
            );
        }
    }

    public static class Result {
        private final String label;
        private final float parliamentProbability;
        private final float otherProbability;

        public Result(@NonNull String label, float parliamentProbability, float otherProbability) {
            this.label = label;
            this.parliamentProbability = parliamentProbability;
            this.otherProbability = otherProbability;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        public float getParliamentProbability() {
            return parliamentProbability;
        }

        public float getOtherProbability() {
            return otherProbability;
        }
    }
}
