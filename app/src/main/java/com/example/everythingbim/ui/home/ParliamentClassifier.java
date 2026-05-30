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
    private static final float CONFIDENCE_THRESHOLD = 0.80f;
    private static final float UNCERTAIN_THRESHOLD = 0.45f;
    private static final int OUTPUT_CLASS_COUNT = 3;
    private static final int INDEX_KENSINGTON_OVAL = 0;
    private static final int INDEX_OTHER = 1;
    private static final int INDEX_PARLIAMENT = 2;

    public static final String LABEL_PARLIAMENT = "parliament";
    public static final String LABEL_KENSINGTON_OVAL = "kensington_oval";
    public static final String LABEL_OTHER = "other";
    public static final String LABEL_UNCERTAIN = "uncertain";

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

        float[][] output = new float[1][OUTPUT_CLASS_COUNT];
        interpreter.run(inputBuffer, output);

        float kensingtonProbability = clampProbability(output[0][INDEX_KENSINGTON_OVAL]);
        float otherProbability = clampProbability(output[0][INDEX_OTHER]);
        float parliamentProbability = clampProbability(output[0][INDEX_PARLIAMENT]);
        float topProbability = Math.max(parliamentProbability, Math.max(kensingtonProbability, otherProbability));

        String label;
        if (parliamentProbability >= PARLIAMENT_THRESHOLD) {
            label = LABEL_PARLIAMENT;
        } else if (kensingtonProbability >= CONFIDENCE_THRESHOLD) {
            label = LABEL_KENSINGTON_OVAL;
        } else if (otherProbability >= CONFIDENCE_THRESHOLD) {
            label = LABEL_OTHER;
        } else if (topProbability >= UNCERTAIN_THRESHOLD) {
            label = LABEL_UNCERTAIN;
        } else {
            label = LABEL_OTHER;
        }
        return new Result(label, parliamentProbability, kensingtonProbability, otherProbability, topProbability);
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

    private float clampProbability(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static class Result {
        private final String label;
        private final float parliamentProbability;
        private final float kensingtonProbability;
        private final float otherProbability;
        private final float topProbability;

        public Result(@NonNull String label,
                      float parliamentProbability,
                      float kensingtonProbability,
                      float otherProbability,
                      float topProbability) {
            this.label = label;
            this.parliamentProbability = parliamentProbability;
            this.kensingtonProbability = kensingtonProbability;
            this.otherProbability = otherProbability;
            this.topProbability = topProbability;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        public float getParliamentProbability() {
            return parliamentProbability;
        }

        public float getKensingtonProbability() {
            return kensingtonProbability;
        }

        public float getOtherProbability() {
            return otherProbability;
        }

        public float getTopProbability() {
            return topProbability;
        }
    }
}
