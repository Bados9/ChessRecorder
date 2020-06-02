package chess.jiruska.chessrecorder.board;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.resize;

public class ChessboardBuilder {

    private final int IMG_SIZE = 100;

    private Interpreter tflite;

    private Interpreter emptyOrNot;

    private List<String> labels;

    public ChessboardBuilder (Context c) {
        loadModel(c);
        try {
            labels = readLabels(c.getAssets(), "labels.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Chessboard buildChessboard(ArrayList<Mat> squares){
        Chessboard board = new Chessboard();

        for(int x = 0; x<squares.size(); x++){
            Mat testSquare = squares.get(x).clone();
            resize(testSquare, testSquare, new Size(IMG_SIZE,IMG_SIZE));
            Mat p = new Mat(IMG_SIZE,IMG_SIZE,CvType.CV_32FC3);
            testSquare.convertTo(p, CvType.CV_32FC3, 1/255.0);

            float[] pixels = new float[IMG_SIZE*IMG_SIZE];
            p.get(0,0, pixels);
            float[][][][] pixels2D = new float[1][IMG_SIZE][IMG_SIZE][1];

            for(int i=0; i<IMG_SIZE;i++) {
                for (int j = 0; j < IMG_SIZE; j++) {
                    pixels2D[0][i][j][0] = pixels[(j * IMG_SIZE) + i];
                }
            }

            float[][] outputFirst = new float[1][2];
            float[][] outputSecond = new float[1][12];

            emptyOrNot.run(pixels2D, outputFirst);
            int resF = getIndexOfLargest(outputFirst[0]);
            if (resF == 1){
                tflite.run(pixels2D, outputSecond);

                int res = getIndexOfLargest(outputSecond[0]);
                String piece_type = labels.get(res);
                board.setPosition(7-x/8, 7-x%8, Integer.parseInt(piece_type));
            } else {
                board.setPosition(7-x/8, 7-x%8, Integer.parseInt("6"));
            }
        }

        return board;
    }

    public int getIndexOfLargest( float[] array ) {
        if ( array == null || array.length == 0 ) return -1;

        int largest = 0;
        for ( int i = 1; i < array.length; i++ )
        {
            if ( array[i] > array[largest] ) largest = i;
        }
        return largest;
    }

    private static List<String> readLabels(AssetManager am, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null){
            labels.add(line);
        }

        br.close();
        return labels;
    }

    private MappedByteBuffer loadModelFile(Context c, String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = c.getAssets().openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadModel(Context c){
        new Thread(() -> {
            try {
                tflite = new Interpreter(loadModelFile(c, "of-final.tflite"));
                emptyOrNot = new Interpreter(loadModelFile(c, "emptyNotempty-2.tflite"));
                System.out.println("models loaded!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ).start();
    }
}
