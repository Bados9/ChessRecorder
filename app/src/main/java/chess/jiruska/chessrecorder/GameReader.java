package chess.jiruska.chessrecorder;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class GameReader {
    private Context context;

    public String getName() {
        return name;
    }

    String getWhite() {
        return white;
    }

    String getBlack() {
        return black;
    }

    String getDate() { return date; }

    String getResult() {
        return result;
    }

    String getNotes() {
        return notes;
    }

    String getMoves() {
        return moves;
    }

    private String name;
    private String white;
    private String black;
    private String date;
    private String result;
    private String notes;
    private String moves;

    GameReader(Context c){
        context = c;
    }

    void readBasicInfo(String filename){
        if (!filename.contains("game")){
            return;
        }
        String path = context.getFilesDir().getAbsolutePath() + "/" + filename;

        FileInputStream is;
        try {
            is = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String nameLine = br.readLine();
            name = getInfo(nameLine);

            String dateLine = br.readLine();
            String dateR = getInfo(dateLine);
            String[] parts = dateR.split("\\.",3);
            date = parseInt(parts[2]) + ". " + parseInt(parts[1]) + ". " + parts[0];

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void readFile(String filename){
        if (!filename.contains("game")){
            return;
        }
        String path = context.getFilesDir().getAbsolutePath() + "/" + filename;
        try {
            FileInputStream is = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String nameLine = br.readLine();
            name = getInfo(nameLine);

            String dateLine = br.readLine();
            String dateR = getInfo(dateLine);
            String[] parts = dateR.split("\\.",3);
            date = parseInt(parts[2]) + ". " + parseInt(parts[1]) + ". " + parts[0];

            String whiteLine = br.readLine();
            white = getInfo(whiteLine);

            String blackLine = br.readLine();
            black = getInfo(blackLine);

            String resultLine = br.readLine();
            result = getInfo(resultLine);

            StringBuilder notesBuilder = new StringBuilder();
            boolean emptyLine = false;
            while(!emptyLine){
                String a = br.readLine();
                a = a.trim();
                if (a.isEmpty()){
                    emptyLine = true;
                } else {
                    notesBuilder.append(a);
                }
            }
            notesBuilder.deleteCharAt(0);
            notesBuilder.deleteCharAt(notesBuilder.length()-1);
            notes = notesBuilder.toString();

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            moves = builder.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String line){
        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(line);
        if (m.find()){
            return m.group(1);
        } else {
            return "no info";
        }

    }
}
