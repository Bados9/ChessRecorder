package chess.jiruska.chessrecorder.board;

import android.content.Context;
import android.util.Pair;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import chess.jiruska.chessrecorder.GameWriter;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.StrictMath.max;
import static org.opencv.calib3d.Calib3d.findHomography;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Laplacian;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.warpPerspective;

class BoardLines{
    ArrayList<Pair<Point, Point>> verticalLines;
    ArrayList<Pair<Point, Point>> horizontalLines;
    Mat frame;

    BoardLines(ArrayList<Pair<Point, Point>> verticalLines, ArrayList<Pair<Point, Point>> horizontalLines, Mat frame){
        this.verticalLines = verticalLines;
        this.horizontalLines = horizontalLines;
        this.frame = frame;
    }
}

public class BoardRecognizer {
    private Mat rectified;
    private ChessboardBuilder builder;

    public BoardRecognizer(Context c, ChessboardBuilder b){
        GameWriter writer = new GameWriter(c);
        builder = b;
    }

    private Mat resize(Mat m){
        Mat mResized = new Mat();
        double ratio = m.width()/(double)m.height();
        Imgproc.resize(m, mResized, new Size(1000*ratio, 1000), 0, 0, Imgproc.INTER_AREA);

        return mResized;
    }

    private Mat preRectification(Mat m){
        Point [] srcArray = new Point[4];
        srcArray[0] = new Point(180,880);
        srcArray[1] = new Point(m.width()-180,880);
        srcArray[2] = new Point(330,150);
        srcArray[3] = new Point(m.width()-330,150);

        LinkedList<Point> dstArray = new LinkedList<>();

        dstArray.add(new Point(100,850));
        dstArray.add(new Point(900,850));
        dstArray.add(new Point(100,100));
        dstArray.add(new Point(900,100));

        MatOfPoint2f dst = new MatOfPoint2f();
        dst.fromList(dstArray);

        MatOfPoint2f src = new MatOfPoint2f();
        src.fromArray(srcArray);

        Mat rectified = new Mat();
        Mat homography = findHomography(src, dst);
        warpPerspective(m, rectified, homography, new Size(m.cols(), m.rows()));

        Rect crop = new Rect(new Point(100, 50), new Point(900,850));
        rectified = rectified.submat(crop);

        return rectified;
    }

    private Chessboard processImage(Mat image){
        image = resize(image);

        return preProcess(image);
    }

    private Chessboard preProcess(Mat image) {
        Mat laplace = new Mat();

        image = preRectification(image);

        rectified = image.clone();

        Mat imageForCutting = image.clone();

        Laplacian(image, laplace, 0);

        BoardLines allLines = houghTest(laplace);

        ArrayList<Rect> squares = createSquares(allLines);

        if (squares == null){
            return null;
        }

        ArrayList<Mat> finalCut = cutSquares2(imageForCutting, squares);

        return builder.buildChessboard(finalCut);
    }

    private ArrayList<Mat> cutSquares2(Mat totalOriginal, ArrayList<Rect> rectangleContours) {
        ArrayList<Mat> result = new ArrayList<>();
        int i = 0;
        for(Rect square: rectangleContours){
            i++;
            square.x-=10;
            square.width+=20;
            square.height+=10;

            if (i>32){
                square.y-=square.height*0.8;
                square.height*=1.8;
            } else {
                square.y-=square.height/2;
                square.height*=1.5;
            }

            square.x = max(square.x, 0);
            square.y = max(square.y, 0);
            square.height = min(totalOriginal.height()-square.y, square.height);
            square.width = min(totalOriginal.width()-square.x, square.width);

            Mat squareCut = totalOriginal.submat(square);
            result.add(squareCut);
        }
        return result;
    }

    private ArrayList<Rect> createSquares(BoardLines lines){
        List<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Rect> rectangles = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(lines.frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            Rect r = boundingRect(contours.get(i));
            if (r.width > 200 || r.height > 200){
                continue;
            }
            rectangles.add(r);
        }

        Random rng = new Random(12345);
        for(Rect rect : rectangles){
            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            rectangle(rectified, new Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y+rect.height), color, -1);
        }

        if(rectangles.size() == 64){
            return rectangles;
        } else {
            return null;
        }

    }

    private BoardLines houghTest(Mat image){
        Mat dst = image.clone(); Mat cdst = new Mat();
        Mat lines = new Mat();

        Imgproc.Canny(dst, dst, 50, 200, 3, false);
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        Mat linesP = new Mat();
        Imgproc.HoughLinesP(dst, linesP, 0.85, Math.PI/280, 190, 150, 100);

        ArrayList<Pair<Point, Point>> verticalLines = new ArrayList<>();
        ArrayList<Pair<Point, Point>> horizontalLines = new ArrayList<>();

        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            double tx = l[2] - l[0];
            double ty = l[3] - l[1];
            double vecLength = sqrt(pow(tx,2)+pow(ty,2));
            tx /= vecLength;
            ty /= vecLength;
            if (abs(abs(tx)-abs(ty))<0.5){
            } else if (abs(tx) > abs(ty)) {
                horizontalLines.add(new Pair<>(new Point(l[0],l[1]), new Point(l[2],l[3])));
            } else {
                verticalLines.add(new Pair<>(new Point(l[0],l[1]), new Point(l[2],l[3])));
            }
        }

        horizontalLines = removeDuplicates(horizontalLines, 0);
        verticalLines = removeDuplicates(verticalLines, 1);

        horizontalLines = correctLines(horizontalLines, 0);
        verticalLines = correctLines(verticalLines, 1);

        for (Pair<Point, Point> line: horizontalLines) {
            horizontalLines.set(horizontalLines.indexOf(line),extendLine(line, 0));
        }

        for (Pair<Point, Point> line: verticalLines) {
            verticalLines.set(verticalLines.indexOf(line),extendLine(line, 1));
        }

        Mat ret = new Mat(cdst.rows(), cdst.cols(), CvType.CV_8U);
        ret.setTo(new Scalar(0,0,0));

        for (Pair<Point,Point> line: horizontalLines){
            Imgproc.line(cdst, line.first, line.second, new Scalar(0, 255, 0), 3, Imgproc.LINE_AA, 0);
            Imgproc.line(ret, line.first, line.second, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

        }

        for (Pair<Point,Point> line: verticalLines){
            Imgproc.line(cdst, line.first, line.second, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
            Imgproc.line(ret, line.first, line.second, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

        }

        return new BoardLines(verticalLines,horizontalLines,ret);
    }

    private ArrayList<Pair<Point, Point>> correctLines(ArrayList<Pair<Point, Point>> array, int direction){
        if (array.size() < 6){
            return array;
        }
        if (direction == 0){ //horizontalni
            ArrayList<Integer> forRemoval = new ArrayList<>();

            for (int i = 0; i<array.size()-1; i++){
                double distance = array.get(i+1).first.y - array.get(i).first.y;
                if (i < array.size()/2 && distance < 50){
                    forRemoval.add(i);
                } else if (i >= array.size()/2 && distance <50){
                    forRemoval.add(i+1);
                }
            }
            for(int i = forRemoval.size()-1; i>=0; i--){
                array.remove(forRemoval.get(i).intValue());
            }

            for (int i = 0; i<8; i++){
                double expectedPositionY = array.get(array.size()-1-i).first.y - 80;
                if (array.size()-2-i < 0){
                    Point point1 = new Point(array.get(0).first.x, expectedPositionY);
                    Point point2 = new Point(array.get(0).second.x, expectedPositionY);
                    Pair<Point, Point> line = new Pair<>(point1, point2);
                    array.add(0, line);
                    i=0;
                } else {
                    if (expectedPositionY - array.get(array.size() - 2 - i).first.y > 30) {
                        double wantedPosition = array.get(array.size() - 2 - i).first.y + array.get(array.size() - 1 - i).first.y;
                        wantedPosition /= 2;
                        Point point1 = new Point(array.get(0).first.x, wantedPosition);
                        Point point2 = new Point(array.get(0).second.x, wantedPosition);
                        Pair<Point, Point> line = new Pair<>(point1, point2);
                        array.add(array.size()-1-i, line);
                        i = 0;
                    }
                }
            }

        } else { //vertikalni
            ArrayList<Integer> forRemoval = new ArrayList<>();

            for (int i = 0; i<array.size()-1; i++){
                double distance = array.get(i+1).first.x - array.get(i).first.x;
                if (i < array.size()/2 && distance < 50){
                    forRemoval.add(i);
                } else if (i >= array.size()/2 && distance <50){
                    forRemoval.add(i+1);
                }
            }
            for(int i = forRemoval.size()-1; i>=0; i--){
                array.remove(forRemoval.get(i).intValue());
            }

            int middleIndex = 4;
            for (int i = 0; i<array.size(); i++){
                if (abs(array.get(i).first.x - 400) < 40){
                    middleIndex = i;
                }
            }

            double expectedPositionX;
            for (int i = 0; i<4; i++){ //leva strana
                expectedPositionX = (array.get(middleIndex-i).first.y < 500) ? (array.get(middleIndex-i).first.x - 80) : (array.get(middleIndex-i).second.x -80);
                if (middleIndex-i-1 < 0){
                    Point point1 = new Point(expectedPositionX, array.get(middleIndex).first.y);
                    Point point2 = new Point(expectedPositionX, array.get(middleIndex).second.y);
                    Pair<Point, Point> line = new Pair<>(point1, point2);
                    array.add(0, line);
                    middleIndex +=1;
                    i=0;
                }
            }

            for (int i = 0; i<4; i++){ //prava strana
                expectedPositionX = (array.get(middleIndex-i).first.y < 500) ? (array.get(middleIndex+i).first.x + 80) : (array.get(middleIndex+i).second.x + 80);
                if (middleIndex+i+1 > array.size()-1){
                    Point point1 = new Point(expectedPositionX, array.get(middleIndex).first.y);
                    Point point2 = new Point(expectedPositionX+5, array.get(middleIndex).second.y);
                    Pair<Point, Point> line = new Pair<>(point1, point2);
                    array.add(array.size(), line);
                    i=0;
                }
            }
        }

        return array;
    }

    private Pair<Point, Point> extendLine(Pair<Point, Point> line, int direction){ //horizontální 0-800, vertikální 100-800
        Point referencePoint = line.second;
        Pair<Point, Point> finalLine = new Pair<>(new Point(), new Point());

        double tx = line.first.x- line.second.x;
        double ty = line.first.y - line.second.y;
        double vecLength = sqrt(pow(tx,2)+pow(ty,2));
        tx /= vecLength;
        ty /= vecLength;
        if (direction == 0){ //horizontal
            double valY = referencePoint.x*ty;
            finalLine.first.x = 0;
            finalLine.first.y = referencePoint.y + valY;

            valY = (800-referencePoint.x)*ty;
            finalLine.second.x = 800;
            finalLine.second.y = referencePoint.y + valY;
        } else { //vertical
            double valX = referencePoint.y*tx;
            finalLine.first.x = referencePoint.x + valX;
            finalLine.first.y = 100;

            valX = (800-referencePoint.y)*tx;
            finalLine.second.x = referencePoint.x+valX;
            finalLine.second.y = 800;
        }
        return finalLine;
    }

    private ArrayList<Pair<Point, Point>> removeDuplicates(ArrayList<Pair<Point, Point>> array, int direction){
        if (direction == 0){ //horizontalni
            array.sort((l1,l2) -> {
                if (l1.first.y == l2.first.y) return 0;
                return l1.first.y > l2.first.y ? 1 : -1;
            });
            ArrayList<Integer> forRemoval = new ArrayList<>();

            for (int i = 0; i<array.size()-1; i++){
                if (abs(array.get(i).first.y - array.get(i+1).first.y) < 10){
                    forRemoval.add(i+1);
                }
            }
            for(int i = forRemoval.size()-1; i>=0; i--){
                array.remove(forRemoval.get(i).intValue());
            }
        } else { //vertikalni
            array.sort((l1,l2) -> {
                if (l1.first.x == l2.first.x) return 0;
                return l1.first.x > l2.first.x ? 1 : -1;
            });
            ArrayList<Integer> forRemoval = new ArrayList<>();
            for (int i = 0; i<array.size()-1; i++){
                if (abs(array.get(i).first.x - array.get(i+1).first.x) < 10){
                    forRemoval.add(i+1);
                }
            }
            for(int i = forRemoval.size()-1; i>=0; i--){
                array.remove(forRemoval.get(i).intValue());
            }
        }
        return array;
    }

    public Chessboard processFrame(Mat frame){
        cvtColor(frame, frame, COLOR_BGR2GRAY);
        return processImage(frame);
    }
}
