package chess.jiruska.chessrecorder.board;

import android.util.Pair;

public class Chessboard {
    //private String[] pieces_string = {"BB", "BE", "BK", "BN", "BP", "BQ", "BR", "WB", "WE", "WK", "WN", "WP", "WQ", "WR"};
    private String[] pieces_string = {"BB", "BK", "BN", "BP", "BQ", "BR", "E", "WB", "WK", "WN", "WP", "WQ", "WR"};
    //private String[] pieces_string = {"BI", "E", "KI", "KN", "PA", "QU", "RO"};
    //private String[] pieces_string = {"E", "N"};

    private int[] board;

    public Chessboard(){
        board = new int[64];
    }

    public void setPosition(int row, int col, int type){
        board[row * 8 + col] = type;
    }

    private int atPosition(int row, int col){
        return board[row * 8 + col];
    }

    public static Pair<Integer, Integer> as2D(int index){
        Pair<Integer, Integer> ret;
        int x = index % 8;
        int y = index / 8;
        ret = new Pair<>(x,y);

        return ret;
    }

    public int[] getBoard(){
        return board;
    }

    @Override
    public String toString(){
        String ret = "";
        int index = 0;
        for (int i = 0; i<8; i++){
            for (int j = 0; j<8; j++){
                ret = ret.concat(pieces_string[board[index]] +" , ");
                index ++;
            }
            ret = ret.concat("\n");
        }
        return ret;
    }

    public boolean equals(Chessboard b){
        for(int i=0; i<64; i++){
            if (board[i] != b.board[i]){
                return false;
            }
        }
        return true;
    }
}
