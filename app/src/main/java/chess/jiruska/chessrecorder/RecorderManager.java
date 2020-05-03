package chess.jiruska.chessrecorder;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Random;

import chess.jiruska.chessrecorder.board.BoardRecognizer;
import chess.jiruska.chessrecorder.board.Chessboard;
import chess.jiruska.chessrecorder.board.ChessboardBuilder;
import chess.jiruska.chessrecorder.board.MoveRecognizer;

public class RecorderManager extends Thread{
    public Handler mHandler;

    private Chessboard previousState;
    private Chessboard currentState;
    private Context context;
    private BoardRecognizer recognizer;
    private ChessboardBuilder builder;
    private boolean boardDetected;

    public RecorderManager(Context c){
        builder = new ChessboardBuilder(c);
        recognizer = new BoardRecognizer(c, builder);
        previousState = null;
        currentState = null;
        context = c;
        boardDetected = false;
    }

    public void newState(byte[] image){
        Mat mat = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.IMREAD_UNCHANGED);
        //Log.d("manager", "prijat obrazek");
        GameWriter.saveImage(context, image);
        if (currentState != null){
            previousState = currentState;
        }
        //System.out.println("newState ... vlakno id = " + Thread.currentThread().getId());
        currentState = recognizer.processFrame(context, mat);
        if (currentState != null){
            boardDetected = true;
        } else {
            boardDetected = false; //TODO udelat ze musi byt null 2x v rade a pak to failne
        }
    }

    public String getMove(){
        if (previousState == null){
            return "";
        }
        String move = MoveRecognizer.recognizeMove(previousState, currentState);
        return move;
    }


    public boolean isBoardDetected(){
        return boardDetected;
    }
}
