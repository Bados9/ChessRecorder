package chess.jiruska.chessrecorder.board;

import android.util.Pair;

import java.util.Arrays;
import java.util.Random;

public class MoveRecognizer {
    public static String recognizeMove(Chessboard previousState, Chessboard currentState){
        String move = "";

        int[] ps = previousState.getBoard();
        int[] cs = currentState.getBoard();

        if (Arrays.equals(ps, cs)){
            return "";
        }

        int[] checkStatus = checkSamePositions(ps, cs);


        //TODO typy figurek jsou z neuronky 0-13...potrebuju mit jen 0-7 a volne policko


        //provadeni korekci

        for (int i=0; i<64; i++){
            if (checkStatus[i] == 0){
                if(ps[i] != 0){ //policko nebylo prazdne
                    int pos = findCurrentPosition(ps[i], checkStatus, cs);
                    System.out.println("previous Position = " + i);
                    System.out.println("current Position = " + pos);
                    System.out.println("fig type = " + ps[i]);
                    System.out.println(buildNotation(pos, ps[i]));
                }
            }
        }
        return move;
    }

    private static final String[] pieces = {"-", "", "N", "Q", "B", "K", "R"};
    private static final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h"};
    private static String buildNotation(int dest, int type){
        String notation = "";
        notation = notation.concat(pieces[type]);
        Pair<Integer, Integer> coords = Chessboard.as2D(dest);
        notation = notation.concat(letters[coords.first]);
        notation = notation.concat(Integer.toString(coords.second+1));
        return notation;
    }

    private static int findCurrentPosition(int type, int[] checkStatus, int[] cs){
        for (int i=0; i<64; i++){
            if(checkStatus[i] == 0){
                if (cs[i] == type){
                    return i;
                }
            }
        }
        return -1;
    }

    private static int[] checkSamePositions(int[] ps, int[] cs){
        int[] checkStatus = new int[64];
        Arrays.fill(checkStatus, 0);

        for (int i = 0; i<64; i++) {
            if (cs[i] == ps[i]){
                checkStatus[i] = 1;
                System.out.println("Checked index " + i);
                System.out.println("Current checkStatus: " + Arrays.toString(checkStatus));
                continue;
            }
            System.out.println("some move detected on index " + i);
        }
        return checkStatus;
    }

    private static boolean isMovePossible(int start, int end, int type){
        boolean ret = false;
        Pair<Integer, Integer> startPos = Chessboard.as2D(start);
        Pair<Integer, Integer> endPos = Chessboard.as2D(end);
        switch (type){
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                if (startPos.first.equals(endPos.first) | startPos.second.equals(endPos.second)){
                    ret = true;
                }
                break;
        }
        return ret;
    }

}

