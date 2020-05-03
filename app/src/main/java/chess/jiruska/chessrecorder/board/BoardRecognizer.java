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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import chess.jiruska.chessrecorder.GameWriter;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.opencv.calib3d.Calib3d.findHomography;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.Laplacian;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.warpPerspective;

class BoardLines{
    public ArrayList<Pair<Point, Point>> verticalLines;
    public ArrayList<Pair<Point, Point>> horizontalLines;
    public Mat frame;

    public BoardLines(ArrayList<Pair<Point, Point>> verticalLines, ArrayList<Pair<Point, Point>> horizontalLines, Mat frame){
        this.verticalLines = verticalLines;
        this.horizontalLines = horizontalLines;
        this.frame = frame;
    }
}

public class BoardRecognizer {
    public Mat original;
    public Mat rectified;
    public int iNum;
    public GameWriter writer;
    public Context c;
    public ChessboardBuilder builder;

    public BoardRecognizer(Context c, ChessboardBuilder b){
        writer = new GameWriter(c);
        this.c = c;
        builder = b;
    }

    public Mat resize(Mat m){
        Mat mResized = new Mat();
        double ratio = m.width()/(double)m.height();
        Imgproc.resize(m, mResized, new Size(1000*ratio, 1000), 0, 0, Imgproc.INTER_AREA);

        return mResized;
    }

    private Mat autoCannyDetection(Mat m){
        Mat mCanny = new Mat();
        Mat mDst = new Mat();

        double highThresh = Imgproc.threshold(m, mDst, 0, 255, THRESH_BINARY + THRESH_OTSU);
        double lowThresh = 0.5*highThresh;
        Canny(m, mCanny, lowThresh, highThresh, 3, false);

        Mat mCannyDilated = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        dilate(mCanny, mCannyDilated, kernel);

        return mCanny;
    }

    public Mat preRectification(Mat m){
        Point [] srcArray = new Point[4];
        srcArray[0] = new Point(180,880);
        srcArray[1] = new Point(m.width()-180,880);
        srcArray[2] = new Point(330,150);
        srcArray[3] = new Point(m.width()-330,150);

        //Imgproc.circle(m, new Point(180,880), 20, new Scalar(255,0,0), -1);
        //Imgproc.circle(m, new Point(m.width()-180,880), 20, new Scalar(255,0,0), -1);
        //Imgproc.circle(m, new Point(330,150), 20, new Scalar(255,0,0), -1);
        //Imgproc.circle(m, new Point(m.width()-330,150), 20, new Scalar(255,0,0), -1);

        //show("m",m);

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

    public BoardLines detectLines(Mat m) {
        Mat mHough = m.clone();
        Imgproc.cvtColor(m, mHough, Imgproc.COLOR_GRAY2BGR);

        Mat lines = new Mat(); // will hold the results of the detection
        Imgproc.HoughLines(m, lines, 1, Math.PI / 180, 150); // runs the actual detection
        // Draw the lines

        ArrayList<Point> controllPointsH = new ArrayList<>();
        ArrayList<Point> controllPointsV = new ArrayList<>();
        ArrayList<Pair<Point, Point>> horizontalLines = new ArrayList<>();
        ArrayList<Pair<Point, Point>> verticalLines = new ArrayList<>();

        for (int x = 0; x < lines.rows(); x++) {
            double rho = lines.get(x, 0)[0],
                    theta = lines.get(x, 0)[1];
            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a * rho, y0 = b * rho;
            Point pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
            Point pt2 = new Point(Math.round(x0 - 1500 * (-b)), Math.round(y0 - 1500 * (a)));
            Imgproc.line(mHough, pt1, pt2, new Scalar(0,0,255), 1, Imgproc.LINE_AA, 0);

            //TODO udelat lepsi rozpoznavani jedinecnych car - clustering, prumerovani
            if (Math.abs(pt1.y - pt2.y) < 300){ //horizontalni
                Point controll = new Point(Math.round(x0 - 700*(-b)), Math.round(y0 - 700*(a)));

                boolean duplicate = false;
                for (Point c: controllPointsH){
                    if ((Math.abs(c.y - controll.y) < 30) & (Math.abs(c.x - controll.x) < 30)){
                        duplicate = true;
                    }
                }

                if (!duplicate) {
                    controllPointsH.add(controll);
                    horizontalLines.add(new Pair<>(pt1, pt2));
                    Imgproc.line(mHough, pt1, pt2, new Scalar(0,255,255), 2, Imgproc.LINE_AA, 0);
                } else {

                }
            } else if (Math.abs(pt1.x - pt2.x) < 1200) { //vertikalni
                Point controll = new Point(Math.round(x0 - 500*(-b)), Math.round(y0 - 450*(a)));

                boolean duplicate = false;
                for (Point c: controllPointsV){
                    if ((Math.abs(c.y - controll.y) < 30) & (Math.abs(c.x - controll.x) < 30)){
                        duplicate = true;
                    }
                }

                if (!duplicate) {
                    controllPointsV.add(controll);
                    verticalLines.add(new Pair<>(pt1, pt2));
                    Imgproc.line(mHough, pt1, pt2, new Scalar(0, 0, 255), 2, Imgproc.LINE_AA, 0);
                } else {
                }
            }
        }

        horizontalLines.sort((l1,l2) -> {
            if (l1.first.y == l2.first.y) return 0;
            return l1.first.y > l2.first.y ? 1 : -1;
        });

        verticalLines.sort((l1,l2) -> { //musi se vybrat vzdy jen spodni bod
            Point p1 = l1.first.y > l1.second.y ? l1.first : l1.second;
            Point p2 = l2.first.y > l2.second.y ? l2.first : l2.second;
            if (p1.x == p2.x) return 0;
            return p1.x > p2.x ? 1 : -1;
        });

        BoardLines ret = new BoardLines(verticalLines, horizontalLines, mHough);

        //show("lines", mHough, false);
        return ret;
    }

    public BoardLines selectLines(BoardLines object){
        ArrayList<Pair<Point, Point>> verticalLines = object.verticalLines;
        ArrayList<Pair<Point, Point>> horizontalLines = object.horizontalLines;
        Mat m = object.frame;
        Mat rectifiedWLines = rectified.clone();
        /*detekce 9x9 car*/
        double avgSquareSize = 0;
        double count = 0;
        for(int i = 2; i < verticalLines.size()-3; i++){
            Pair<Point, Point> line1 = verticalLines.get(i);
            Pair<Point, Point> line2 = verticalLines.get(i+1);
            Point p1 = line1.first.y > line1.second.y ? line1.first : line1.second;
            Point p2 = line2.first.y > line2.second.y ? line2.first : line2.second;
            double dist = Math.abs(p1.x-p2.x);
            if (dist > 40){
                avgSquareSize += dist;
                //System.out.println(Math.abs(p1.x-p2.x));
                count++;
            }
        }
        avgSquareSize /= count;
        //System.out.println("average= "+avgSquareSize);

        //hledani nejblizsi cary ke stredu
        double minDist = 10000;
        int minIndex = 0;
        int index = 0;
        for(Pair<Point, Point> line: verticalLines){

            Point p = line.first.y > line.second.y ? line.first : line.second;
            if (Math.abs(p.x-m.width()/2.0) < minDist){
                minDist = Math.abs(p.x-m.width()/2.0);
                minIndex = index;
            }
            //System.out.println(p.x);
            index++;
        }
        //System.out.println("nejbliz je index "+minIndex+" a dist je "+minDist);

        Pair<Point, Point> centerLineVertical = verticalLines.get(minIndex);
        ArrayList<Pair<Point, Point>> finalVerticalLines = new ArrayList<>();
        for (int q=0; q<9; q++){
            finalVerticalLines.add(null);
        }
        finalVerticalLines.set(4, centerLineVertical);
        Point pVC = centerLineVertical.first.y > centerLineVertical.second.y ? centerLineVertical.first : centerLineVertical.second;
        for (int i=0; i<4; i++){
            boolean found = false;

            for (int j=0; j<minIndex; j++){
                Pair<Point, Point> line = verticalLines.get(j);
                Point p1 = line.first.y > line.second.y ? line.first : line.second;
                //Point p2 = centerLineVertical.getKey().y > centerLineVertical.getValue().y ? centerLineVertical.getKey() : centerLineVertical.getValue();
                //System.out.println("porovnavam " +Math.abs(p1.x-p2.x)+" s "+avgSquareSize*(i+1)+20);
                if (Math.abs(Math.abs(p1.x-pVC.x) - avgSquareSize*(i+1)) < 20){
                    finalVerticalLines.set(3-i, line);
                    //System.out.println("Cara na indexu "+j+" je na pozici "+(3-i));
                    found = true;
                    break;
                }
            }
            if (!found){
                //System.out.println("Doplnena cara na index "+(3-i));
                Pair<Point, Point> newLine = new Pair(new Point(pVC.x-avgSquareSize*(i+1),1000), new Point(pVC.x-avgSquareSize*(i+1),0));
                finalVerticalLines.set(3-i, newLine);
            }
        }

        for (int i=0; i<4; i++){
            boolean found = false;

            for (int j=minIndex+1; j<verticalLines.size()-1; j++){
                Pair<Point, Point> line = verticalLines.get(j);
                Point p1 = line.first.y > line.second.y ? line.first : line.second;
                //Point p2 = centerLineVertical.getKey().y > centerLineVertical.getValue().y ? centerLineVertical.getKey() : centerLineVertical.getValue();
                //System.out.println("porovnavam " +Math.abs(p1.x-p2.x)+" s "+avgSquareSize*(i+1)+20);
                if (Math.abs(Math.abs(p1.x-pVC.x) - avgSquareSize*(i+1)) < 20){
                    finalVerticalLines.set(5+i, line);
                    //System.out.println("Cara na indexu "+j+" je na pozici "+(5+i));
                    found = true;
                    break;
                }
            }
            if (!found){
                //System.out.println("Doplnena cara na index "+(5+i));
                Pair<Point, Point> newLine = new Pair(new Point(pVC.x+avgSquareSize*(i+1),1000), new Point(pVC.x+avgSquareSize*(i+1),0));
                finalVerticalLines.set(5+i, newLine);
            }
        }

        for (Pair<Point, Point> line: finalVerticalLines){
            Imgproc.line(rectifiedWLines, line.first, line.second, new Scalar(0,255,0), 4);
        }

        //-----------horizontalni cast

        double avgSquareSizeH = 0;
        double countH = 0;
        for(int i = 2; i < horizontalLines.size()-3; i++){
            Pair<Point, Point> line1 = horizontalLines.get(i);
            Pair<Point, Point> line2 = horizontalLines.get(i+1);
            Point p1 = line1.first;//.y > line1.getValue().y ? line1.getKey() : line1.getValue();
            Point p2 = line2.first;//.y > line2.getValue().y ? line2.getKey() : line2.getValue();
            double dist = Math.abs(p1.y-p2.y);
            if (dist > 40){
                avgSquareSizeH += dist;
                //System.out.println(Math.abs(p1.y-p2.y));
                countH++;
            }
        }
        avgSquareSizeH /= countH;
        //System.out.println("average= "+avgSquareSizeH);

        //hledani nejblizsi cary ke stredu
        double minDistH = 10000;
        int minIndexH = 0;
        int indexH = 0;
        for(Pair<Point, Point> line: horizontalLines){

            Point p = line.first; // > line.getValue().y ? line.getKey() : line.getValue();
            if (Math.abs(p.y-475) < minDistH){ //TODO nejak zparametrizovat podle rektifikace
                minDistH = Math.abs(p.y-475);
                minIndexH = indexH;
            }
            //System.out.println(p.y);
            indexH++;
        }
        //System.out.println("nejbliz je index "+minIndexH+" a dist je "+minDistH);

        Pair<Point, Point> centerLineHorizontal = horizontalLines.get(minIndexH);
        ArrayList<Pair<Point, Point>> finalHorizontalLines = new ArrayList<>();
        for (int q=0; q<9; q++){
            finalHorizontalLines.add(null);
        }
        finalHorizontalLines.set(4, centerLineHorizontal);
        Point pHC = centerLineHorizontal.first; //.y > centerLineVertical.getValue().y ? centerLineVertical.getKey() : centerLineVertical.getValue();
        for (int i=0; i<4; i++){
            boolean found = false;

            for (int j=0; j<minIndexH; j++){
                Pair<Point, Point> line = horizontalLines.get(j);
                Point p1 = line.first; //> line.getValue().y ? line.getKey() : line.getValue();
                //Point p2 = centerLineVertical.getKey().y > centerLineVertical.getValue().y ? centerLineVertical.getKey() : centerLineVertical.getValue();
                //System.out.println("porovnavam " +Math.abs(p1.y-pHC.y)+" s "+avgSquareSizeH*(i+1)+20);
                if (Math.abs(Math.abs(p1.y-pHC.y) - avgSquareSizeH*(i+1)) < 20){
                    finalHorizontalLines.set(3-i, line);
                    //System.out.println("Cara na indexu "+j+" je na pozici "+(3-i));
                    found = true;
                    break;
                }
            }
            if (!found){
                //System.out.println("Doplnena cara na index "+(3-i));
                Pair<Point, Point> newLine = new Pair(new Point(1000, pHC.y-avgSquareSizeH*(i+1)), new Point(0, pHC.y-avgSquareSizeH*(i+1)));
                finalHorizontalLines.set(3-i, newLine);
            }
        }

        for (int i=0; i<4; i++){
            boolean found = false;

            for (int j=minIndexH+1; j<horizontalLines.size()-1; j++){
                Pair<Point, Point> line = horizontalLines.get(j);
                Point p1 = line.first;//.y > line.getValue().y ? line.getKey() : line.getValue();
                //Point p2 = centerLineVertical.getKey().y > centerLineVertical.getValue().y ? centerLineVertical.getKey() : centerLineVertical.getValue();
                //System.out.println("porovnavam " +Math.abs(p1.x-p2.x)+" s "+avgSquareSize*(i+1)+20);
                if (Math.abs(Math.abs(p1.y-pHC.y) - avgSquareSizeH*(i+1)) < 20){
                    finalHorizontalLines.set(5+i, line);
                    //System.out.println("Cara na indexu "+j+" je na pozici "+(5+i));
                    found = true;
                    break;
                }
            }
            if (!found){
                //System.out.println("Doplnena cara na index "+(5+i));
                Pair<Point, Point> newLine = new Pair(new Point(0,pHC.y+avgSquareSizeH*(i+1)), new Point(1000,pHC.y+avgSquareSizeH*(i+1)));
                finalHorizontalLines.set(5+i, newLine);
            }
        }

        for (Pair<Point, Point> line: finalHorizontalLines){
            Imgproc.line(rectifiedWLines, line.first, line.second, new Scalar(0,255,0), 4);
        }


        //show("test", rectifiedWLines, false);
        BoardLines ret = new BoardLines(finalVerticalLines, finalHorizontalLines, rectifiedWLines);

        return ret;
    }

    public ArrayList<Rect> findSquares(Mat totalOriginal, BoardLines selectedLines, boolean show){
        Mat original = totalOriginal.clone();
        List<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Rect> rectangles = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(selectedLines.frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        double areaSum = 0.0;
        int q = 0;
        for (int i = 0; i < contours.size(); i++) {
            Rect r = boundingRect(contours.get(i));
            if (Math.abs(r.width/(double)r.height - 1.0) < 0.8){ //trideni podle pomeru vysky a sirky - pouze ctverce
                rectangles.add(r);
                areaSum += r.area();
            }
        }

        //spocita se prumerna plocha a potom se vyrazne odchylky odstrani
        //ve stejnem cyklu se kontroluje pozice ctverce, pokud neni na sachovnici, ale na kraji, je odstranen
        double minLeft, maxRight, minTop, maxBottom;
        minLeft = (selectedLines.verticalLines.get(0).first.x + selectedLines.verticalLines.get(0).second.x)/2.0;
        maxRight = (selectedLines.verticalLines.get(8).first.x + selectedLines.verticalLines.get(8).second.x)/2.0;
        minTop = (selectedLines.horizontalLines.get(0).first.y + selectedLines.horizontalLines.get(0).second.y)/2.0;
        maxBottom = (selectedLines.horizontalLines.get(8).first.y + selectedLines.horizontalLines.get(8).second.y)/2.0;
        //System.out.println("TOP" +minTop+ " BOTTOM "+maxBottom+" LEFT "+minLeft+" RIGHT "+maxRight);
        double averageArea = areaSum/rectangles.size();
        List<Rect> toRemove = new ArrayList<>();
        for (Rect r: rectangles){
            if((r.area()-averageArea) > 2000){
                toRemove.add(r);
            }
            if (r.x < minLeft-20 || r.x+r.width > maxRight+20){
                toRemove.add(r);
            }
            if (r.y < minTop-20 || r.y+r.height > maxBottom+20){
                toRemove.add(r);
            }
        }
        rectangles.removeAll(toRemove);


        //System.out.println("POCET CTVERCU JE " + rectangles.size());
        Random rng = new Random(12345);
        for(Rect rect : rectangles){
            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            rectangle(original, new Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y+rect.height), color, -1);
        }


        if(show){
            //show("Contours", original, false);
        }
        return rectangles;
    }

    /*predpoklada 64 ctvercu - srovna je zleva zespodu po radach*/
    private static ArrayList<List<Rect>> createChessboard(ArrayList<Rect> rectangles) {
        if (rectangles.size() < 64){
            return null;
        }
        //seradi ctverce odspodu nahoru
        rectangles.sort((r1, r2) -> {
            if (r1.y == r2.y) return 0;
            return r1.y > r2.y ? -1 : 1;
        });
        ArrayList<List<Rect>> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++){
            List<Rect> row = rectangles.subList(i*8, i*8+8);
            //seradi ctverce v rade zleva doprava
            row.sort((r1, r2) -> {
                if (r1.x == r2.x) return 0;
                return r1.x < r2.x ? -1 : 1;
            });
            rows.add(row);
        }
        return rows;
    }

    private ArrayList<Mat> cutSquares(Mat totalOriginal, ArrayList<Rect> rectangleContours) {
        //TODO az pro rozpoznavani, pro tvorbu datasetu neni potřeba
        //ArrayList<List<Rect>> rows = createChessboard(rectangleContours);
        ArrayList<Mat> result = new ArrayList<>();
        int i = 0;
        for(Rect square: rectangleContours){
            i++;
            int heightOld = square.height;
            square.height = (int) (heightOld *2.5);
            int diff = square.height - heightOld;
            square.y = square.y - diff + 5;
            square.x -= 10;
            square.width += 20;
            //square.height = (int) (square.height*1.5);

            Mat squareCut = totalOriginal.submat(square);
            result.add(squareCut);
            //show("square_"+i, squareCut, false);
            int index = Objects.requireNonNull(new File("D:/School/2MIT/DP/data/test").list()).length;
            //imwrite("D:/School/2MIT/DP/data/test/square_"+index+".jpg",squareCut);
        }
        /*for (List<Rect> row : rows) {
            for(Rect square : row){
                i++;
                Mat squareCut = totalOriginal.submat(square);
                result.add(squareCut);
                show("square_"+i, squareCut);
            }
        }*/
        return result;
    }

    public Chessboard processImage(Mat image){
        image = resize(image);
        original = image;

        //show("original",original, false);

        Chessboard chessboard = preProcess(image);
        if (chessboard == null){
            return null;
        }

        return chessboard;

        /*image = preRectification(image);
        rectified = image;

        show("rectified",rectified, false);

        image = autoCannyDetection(image);

        show("canny", image, false);

        BoardLines allLines = detectLines(image);
        show("firstMethod", allLines.frame, false);*/
/*
        BoardLines selectedLines = selectLines(allLines);

        show("BoardDetected",selectedLines.frame, false);

        ArrayList<Rect> squares = findSquares(rectified, selectedLines,true);

        cutSquares(rectified, squares);
*/
    }

    private Chessboard preProcess(Mat image) {
        Mat laplace = new Mat();

        image = preRectification(image);

        rectified = image.clone();

        Mat imageForCutting = image.clone();

        Laplacian(image, laplace, 0);

        //show("laplace", laplace, false);

        BoardLines allLines = houghTest(laplace);

        ArrayList<Rect> squares = createSquares(allLines);

        if (squares == null){
            //System.out.println("malo ctvercu");
            return null;
        }

        ArrayList<Mat> finalCut = cutSquares2(imageForCutting, squares);

        Chessboard chessboard = builder.buildChessboard(finalCut);

        return chessboard;
    }

    private ArrayList<Mat> cutSquares2(Mat totalOriginal, ArrayList<Rect> rectangleContours) {
        ArrayList<Mat> result = new ArrayList<>();
        int i = 0;
        for(Rect square: rectangleContours){
            i++;
            square.x-=5;
            square.width+=10;

            Mat squareCut = totalOriginal.submat(square);
            result.add(squareCut);
            //show("square_"+i, squareCut, false);
        }
        return result;
    }

    public ArrayList<Rect> createSquares(BoardLines lines){
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

        //show("rectangles new", rectified, false);

        if(rectangles.size() == 64){
            return rectangles;
        } else {
            return null; //todo asi pres vyjimky
        }

    }

    public BoardLines houghTest(Mat image){
        Mat dst = image.clone(); Mat cdst = new Mat();
        Mat lines = new Mat(); // will hold the results of the detection

        Imgproc.Canny(dst, dst, 50, 200, 3, false);
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        Mat linesP = new Mat(); // will hold the results of the detection
        //Imgproc.HoughLinesP(dst, linesP, 0.7, Math.PI/280, 200, 150, 100); // runs the actual detection
        Imgproc.HoughLinesP(dst, linesP, 0.85, Math.PI/280, 190, 150, 100); // runs the actual detection

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
                //System.out.println("vektor = ("+ tx + "," + ty +") -- je křivá");
            } else if (abs(tx) > abs(ty)) {
                //System.out.println("vektor = ("+ tx + "," + ty +") -- je horizontální");
                horizontalLines.add(new Pair<>(new Point(l[0],l[1]), new Point(l[2],l[3])));
            } else {
                //System.out.println("vektor = ("+ tx + "," + ty +") -- je vertikální");
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

        //show("secondMethod" , cdst, false);
        //writer.saveOpenCVImage(c ,cdst, "lines");
        //show ("blackandwhite", ret, false);
        return new BoardLines(verticalLines,horizontalLines,ret);
    }

    public ArrayList<Pair<Point, Point>> correctLines(ArrayList<Pair<Point, Point>> array, int direction){
        if (direction == 0){ //horizontalni
            ArrayList<Double> distances = new ArrayList<>();
            ArrayList<Integer> forRemoval = new ArrayList<>();

            for (int i = 0; i<array.size()-1; i++){
                double distance = array.get(i+1).first.y - array.get(i).first.y;
                distances.add(distance);
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
            ArrayList<Double> distances = new ArrayList<>();
            ArrayList<Integer> forRemoval = new ArrayList<>();

            for (int i = 0; i<array.size()-1; i++){
                double distance = array.get(i+1).first.x - array.get(i).first.x;
                distances.add(distance);
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

            double expectedPositionX = 0.0;
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

    public Pair<Point, Point> extendLine(Pair<Point, Point> line, int direction){ //horizontální 0-800, vertikální 100-800
        //TODO možná pomůže že nebudou až do kraje
        Point referencePoint = line.second;
        Pair<Point, Point> finalLine = new Pair<>(new Point(), new Point());

        double tx = line.first.x- line.second.x;
        double ty = line.first.y - line.second.y;
        double vecLength = sqrt(pow(tx,2)+pow(ty,2));
        tx /= vecLength;
        ty /= vecLength;
        //System.out.println("vektor: " + tx + " " + ty);
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

    public ArrayList<Pair<Point, Point>> removeDuplicates(ArrayList<Pair<Point, Point>> array, int direction){
        if (direction == 0){ //horizontalni
            array.sort((l1,l2) -> {
                if (l1.first.y == l2.first.y) return 0;
                return l1.first.y > l2.first.y ? 1 : -1;
            });
            ArrayList<Integer> forRemoval = new ArrayList<>();
            //System.out.println("mame " + array.size() + " horizontalnich car");

            for (int i = 0; i<array.size()-1; i++){
                if (abs(array.get(i).first.y - array.get(i+1).first.y) < 10){
                    //System.out.println("cara na indexu " + (i+1) + "presunuta k vymazani");
                    forRemoval.add(i+1);
                }
            }
            for(int i = forRemoval.size()-1; i>=0; i--){
                array.remove(forRemoval.get(i).intValue());
            }

            for (Pair<Point, Point> line: array) {
                //System.out.println("Y = "+line.getKey().y);
            }
        } else { //vertikalni
            array.sort((l1,l2) -> {
                if (l1.first.x == l2.first.x) return 0;
                return l1.first.x > l2.first.x ? 1 : -1;
            });
            ArrayList<Integer> forRemoval = new ArrayList<>();
            //System.out.println("mame " + array.size() + " vertikalnich car");
            for (int i = 0; i<array.size()-1; i++){
                if (abs(array.get(i).first.x - array.get(i+1).first.x) < 10){
                    //System.out.println("cara na indexu " + (i+1) + "presunuta k vymazani");
                    forRemoval.add(i+1);
                }
            }
            for(int i = forRemoval.size()-1; i>=0; i--){
                array.remove(forRemoval.get(i).intValue());
            }
            for (Pair<Point, Point> line: array) {
                //System.out.println("X = "+line.getKey().x);
            }
        }
        //System.out.println("zbylo nam " + array.size() + " car");


        return array;
    }

    public Chessboard processFrame(Context c, Mat frame){
        //System.out.println("processFrame ... vlakno id = " + Thread.currentThread().getId());

        cvtColor(frame, frame, COLOR_BGR2GRAY);
        return processImage(frame);
    }
}
