package chess.jiruska.chessrecorder.classification;

public interface Classifier {
    String name();

    Classification recognize(final float[] pixels);
}
