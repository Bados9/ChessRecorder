package chess.jiruska.chessrecorder;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class GameWriter implements Serializable {

    private FileOutputStream file;
    private String moves;
    private Context context;
    private int currentMoveNumber;

    public GameWriter(Context context){
        moves = "";
        this.context = context;
        currentMoveNumber = 2;
    }

    GameWriter(Context context, String moves){
        this.context = context;
        this.moves = moves;
    }

    void writeMove(String move){
        if (currentMoveNumber % 2 == 0){
            moves = moves.concat(" " + currentMoveNumber/2 + ". " + move);
        } else {
            moves = moves.concat(" " + move);
        }
        currentMoveNumber += 1;
        Log.d("writer", "move " + move + " written");
    }

    void saveGame(String title, String white, String black, String winner, String notes){
        UUID identifier = UUID.randomUUID();
        try {
            file = context.openFileOutput("game"+ identifier.toString()+".chg", Context.MODE_PRIVATE | Context.MODE_APPEND);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String date = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date());
        try {
            file.write(("[Event \"" + title + "\"]\n" +
                    "[Date \"" + date + "\"]\n" +
                    "[White \"" + white + "\"]\n" +
                    "[Black \"" + black + "\"]\n" +
                    "[Result \"" + winner + "\"]\n" +
                    "{" + notes + "}\n\n" +
                    moves).getBytes());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public static void saveImage(Context context, byte[] img){

        try {
            FileOutputStream iFile = context.openFileOutput("image"+UUID.randomUUID().toString()+".jpg", Context.MODE_PRIVATE | Context.MODE_APPEND);
            iFile.write(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    /*public static void saveOpenCVImage(Context context, Mat subimg, String text){
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(subimg.cols(), subimg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(subimg, bmp);
        } catch (CvException e) {
            Log.d(TAG, Objects.requireNonNull(e.getMessage()));
        }

        subimg.release();
        FileOutputStream out = null;

        try {
            out = context.openFileOutput(text+"image"+UUID.randomUUID().toString()+".jpg", Context.MODE_PRIVATE | Context.MODE_APPEND);
            assert bmp != null;
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, Objects.requireNonNull(e.getMessage()));
        } finally {
            try {
                if (out != null) {
                    out.close();
                    Log.d(TAG, "OK!!");
                }
            } catch (IOException e) {
                Log.d(TAG, e.getMessage() + "Error");
                e.printStackTrace();
            }
        }
    }*/

    String getMoves(){
        return moves;
    }
}
