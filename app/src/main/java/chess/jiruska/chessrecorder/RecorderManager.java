package chess.jiruska.chessrecorder;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import chess.jiruska.chessrecorder.board.BoardRecognizer;
import chess.jiruska.chessrecorder.board.Chessboard;
import chess.jiruska.chessrecorder.board.ChessboardBuilder;
import chess.jiruska.chessrecorder.board.MoveRecognizer;

class RecorderManager extends Thread{

    private Chessboard previousState;
    private Chessboard currentState;
    private BoardRecognizer recognizer;
    private boolean boardDetected;

    RecorderManager(Context c){
        ChessboardBuilder builder = new ChessboardBuilder(c);
        recognizer = new BoardRecognizer(c, builder);
        previousState = null;
        currentState = null;
        boardDetected = false;
    }

    void newState(byte[] image){
        Mat mat = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.IMREAD_UNCHANGED);
        if (currentState != null){
            previousState = currentState;
        }
        currentState = recognizer.processFrame(mat);
        if (currentState != null){
            boardDetected = true;
            //Log.d("newState", currentState.toString());
        } else {
            boardDetected = false;
        }
    }

    String getMove(){
        if (previousState == null || currentState == null){
            return "";
        }
        return MoveRecognizer.recognizeMove(previousState, currentState);
    }


    boolean isBoardDetected(){
        return boardDetected;
    }
}
