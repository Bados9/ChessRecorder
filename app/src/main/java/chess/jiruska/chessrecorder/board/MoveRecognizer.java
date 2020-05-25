package chess.jiruska.chessrecorder.board;

import android.util.Pair;

import java.util.Arrays;

public class MoveRecognizer {
    public static String recognizeMove(Chessboard previousState, Chessboard currentState){
        String move = "";

        int[] ps = previousState.getBoard();
        int[] cs = currentState.getBoard();

        if (Arrays.equals(ps, cs)){
            return "";
        }

        int[] checkStatus = checkSamePositions(ps, cs);

        //provadeni korekci

        for (int i=0; i<64; i++){
            if (checkStatus[i] == 0){
                if(ps[i] != 6){ //policko nebylo prazdne
                    int pos = findCurrentPosition(ps[i], checkStatus, cs);
                    if (pos == -1){
                        continue;
                    }
                    //System.out.println("previous Position = " + i);
                    //System.out.println("current Position = " + pos);
                    //System.out.println("fig type = " + ps[i]);
                    if (isMovePossible(i, pos, ps[i])) {
                        //System.out.println(buildNotation(pos, ps[i]));
                        if (ps[pos] != 6){
                            return buildNotation(pos, ps[i], true);
                        } else {
                            return buildNotation(pos, ps[i], false);
                        }
                    }
                }
            }
        }
        return move;
    }

    private static final String[] pieces = {"B", "K", "N", "P", "Q", "R", "-", "B", "K", "N", "P", "Q", "R"};
    private static final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h"};

    private static String buildNotation(int dest, int type, boolean take){
        String notation = "";
        notation = notation.concat(pieces[type]);
        Pair<Integer, Integer> coords = Chessboard.as2D(dest);
        if (take){
            notation = notation.concat("x");
        }
        notation = notation.concat(letters[coords.second]);
        notation = notation.concat(Integer.toString(coords.first+1));

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
                //System.out.println("Checked index " + i);
                //System.out.println("Current checkStatus: " + Arrays.toString(checkStatus));
                continue;
            }
            //System.out.println("some move detected on index " + i);
        }
        return checkStatus;
    }

    private static boolean isMovePossible(int start, int end, int type){
        boolean ret = false;
        Pair<Integer, Integer> startPos = Chessboard.as2D(start);
        Pair<Integer, Integer> endPos = Chessboard.as2D(end);

        int dFirst = Math.abs(startPos.first - endPos.first);
        int dSecond = Math.abs(startPos.second - endPos.second);

        switch (type){
            case 0: case 7:
                if (dFirst == dSecond){
                    ret = true;
                }
                break;
            case 1: case 8:
                if (dFirst <= 1 && dSecond <= 1){
                    ret = true;
                }
                break;
            case 2: case 9:
                if ((dFirst == 1 && dSecond == 2) || (dFirst == 2 && dSecond == 1)){
                    ret = true;
                }
                break;
            case 3:
                if (((startPos.first > endPos.first) && dFirst <= 2) || dSecond <= 1){
                    ret = true;
                }
                break;
            case 10:
                if (((startPos.first < endPos.first) && dFirst <= 2) || dSecond <=1){
                    ret = true;
                }
                break;
            case 4: case 11:
                if (dFirst == dSecond || startPos.first.equals(endPos.first) || startPos.second.equals(endPos.second)){
                    ret = true;
                }
                break;
            case 5: case 12:
                if (startPos.first.equals(endPos.first) || startPos.second.equals(endPos.second)){
                    ret = true;
                }
                break;
            default:
                ret = false;
        }
        return ret;
    }

}

