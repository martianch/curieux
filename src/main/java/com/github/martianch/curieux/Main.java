//#!/usr/bin/java --source 11
// Note: the above shebang can work only if the file extension IS NOT .java

package com.github.martianch.curieux;

/*
Curious: an X3D Viewer
Designed to view the Curiosity Rover images from Mars in X3D, but can be used to view any stereo pairs (both LR and X3D).
Opens images from Internet or local drive, supports drag-and-drop, for example, you can drag-n-drop the red DOWNLOAD
button from the raw images index on the NASA site.
This software is Public Domain
*/


import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.Element;
import javax.swing.text.NumberFormatter;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.Image.SCALE_SMOOTH;

/** The app runner class */
public class Main {

    public static final String CURIOSITY_RAW_IMAGES_URL =
        "https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+desc%2C+date_taken+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc&per_page=100&page=0&mission=msl";
    public static final String PERSEVERANCE_RAW_IMAGES_URL =
        "https://mars.nasa.gov/mars2020/multimedia/raw-images/";

    public static final String[] PREFERRED_FONTS = {
            "Verdana"
            //,"Ubuntu"
            //,"DejaVu Sans Mono"
    };

    static final ParallelImageLoading PARALLEL_IMAGE_LOADING = ParallelImageLoading.DELAYED;
    public enum ParallelImageLoading {PARALLEL,DELAYED,SEQUENTIAL}

    public static void main(String[] args) throws Exception {
        System.out.println("args: "+ Arrays.toString(args));
        List<String> paths;
        switch (args.length) {
            case 0:
            {
                //paths = Arrays.asList("","");
                paths = Arrays.asList(
                    "https://mars.jpl.nasa.gov/msl-raw-images/msss/02667/mhli/2667MH0001630001001643R00_DXXX.jpg",
                    "https://mars.jpl.nasa.gov/msl-raw-images/msss/02667/mhli/2667MH0001630001001645R00_DXXX.jpg"
                );
            }
            break;
            case 1: {
                paths = FileLocations.twoPaths(HttpLocations.unPage(args[0]));
            }
            break;
            default:
            case 2: {
                paths = FileLocations.twoPaths(HttpLocations.unPage(args[0]), HttpLocations.unPage(args[1]));
            }
            break;
        }

        var rd0 = RawData.createInProgress(paths.get(0), paths.get(1));
        var dp = new DisplayParameters();
        var xv = new X3DViewer();
        var ms = new MeasurementStatus();
        var lrn = new LRNavigator();
        var so = new SaveOptions();
        var uic = new UiController(xv, lrn, rd0, dp, ms, so);
        javax.swing.SwingUtilities.invokeLater(
                () -> uic.createAndShowViews()
        );
        paths = uic.unThumbnailIfNecessary(paths);
        NasaReader.cleanupReading(); // this is black magic. we read favicon
        uic.updateRawDataAsync(paths.get(0), paths.get(1));
    }
}

interface UiEventListener {
    void zoomChanged(double newZoom);
    void lZoomChanged(double newLZoom);
    void rZoomChanged(double newRZoom);
    void angleChanged(double newAngle);
    void lAngleChanged(double newLAngle);
    void rAngleChanged(double newRAngle);
    void lDebayerModeChanged(DebayerMode newLDebayerMode);
    void rDebayerModeChanged(DebayerMode newRDebayerMode);
    void lImageResamplingModeChanged(ImageResamplingMode newImageResamplingModeL);
    void rImageResamplingModeChanged(ImageResamplingMode newImageResamplingModeR);
    void lColorCorrectionChanged(ColorCorrection colorCorrection);
    void rColorCorrectionChanged(ColorCorrection colorCorrection);
    void xOffsetChanged(int newXOff);
    void yOffsetChanged(int newYOff);
    void swapImages();
    void swapImageUrls();
    void dndImport(String s, boolean isRight, OneOrBothPanes oneOrBoth);
    void dndSingleToBothChanged(boolean newValue);
    void unthumbnailChanged(boolean newValue);
    void copyUrl(boolean isRight);
    void copyUrls();
    void loadMatchOfOther(boolean isRight);
    void loadCopyOfOther(boolean isRight);
    void reload(boolean isRight);
    void gotoImage(GoToImageOptions goToImageOptions, boolean isRight, Optional<Integer> sol);
    void newWindow();
    void setShowUrls(boolean visible);
    void resetToDefaults();
    void saveScreenshot();
    void navigate(boolean isRight, boolean isLeft, boolean forwardInTime, int byOneOrTwo);
    void openInBrowser(SiteOpenCommand command, boolean isRight, WhichRover whichRover);
    void markPointWithMousePress(boolean isRight, MouseEvent e);
    void setWaitingForPoint(int forPointNumber);
    void markedPointChanged(int coordId, double lrXy12);
    void clearAllMarks();
    void setSubpixelPrecisionMarks(boolean precise);
    void stereoCameraChanged(StereoPairParameters v);
    void markShapeChanged(MeasurementPointMark v);
    void measurementShownChanged(boolean newIsShown);
    void adjustOffsets();
    void escapePressed();
    Optional<Integer> getSol(boolean isRight, WhichRover whichRover);
    MeasurementStatus getMeasurementStatus();
    DisplayParameters getDisplayParameters();
    ColorRange getViewportColorRange(boolean isRight);
    CustomStretchRgbParameters getCurrentCustomStretchRgbParameters(boolean isRight);
    void setCustomStretchRgbParameters(CustomStretchRgbParameters customStretchRgbParameters, boolean isRight); //???
    void setSaveOptions(boolean saveGif, boolean saveLeftRIght);
}

enum GoToImageOptions {
    CURIOSITY_FIRST_OF_SOL,
    CURIOSITY_LATEST,
    PERSEVERANCE_FIRST_OF_SOL,
    PERSEVERANCE_LATEST;
}
enum WhichRover {
    CURIOSITY,
    PERSEVERANCE;
}

class ColorRange {
    int minR, minG, minB, maxR, maxG, maxB;

    private ColorRange(int minR, int minG, int minB, int maxR, int maxG, int maxB) {
        this.minR = minR;
        this.minG = minG;
        this.minB = minB;
        this.maxR = maxR;
        this.maxG = maxG;
        this.maxB = maxB;
    }
    public ColorRange copy() {
        return new ColorRange(minR, minG, minB, maxR, maxG, maxB);
    }

    public static ColorRange newFullRange() {
        var res = new ColorRange(0, 0, 0, 255, 255, 255);
        return res;
    }
    public static ColorRange newEmptyRange() {
        var res = new ColorRange(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0);
        return res;
    }
    boolean isEmpty() {
        return minR >= maxR || minG >= maxG || minB >= maxB;
    }
    boolean isFullRange() {
        return minR == 0 && maxR == 255
            && minG == 0 && maxG == 255
            && minB == 0 && maxB == 255;
    }
    @Override
    public String toString() {
        return "ColorRange{" +
                minR + ".." + maxR + ", " +
                minG + ".." + maxG + ", " +
                minB + ".." + maxB +
                (isEmpty() ? " (empty)" : "") +
                (isFullRange() ? " (full-range)" : "") +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorRange that = (ColorRange) o;
        return minR == that.minR &&
                minG == that.minG &&
                minB == that.minB &&
                maxR == that.maxR &&
                maxG == that.maxG &&
                maxB == that.maxB;
    }
    @Override
    public int hashCode() {
        return Objects.hash(minR, minG, minB, maxR, maxG, maxB);
    }
}
class CustomStretchRgbParameters {
    ColorRange colorRange;
    boolean isPerChannel;
    boolean isSaturated;

    public CustomStretchRgbParameters(ColorRange colorRange, boolean isPerChannel, boolean isSaturated) {
        this.colorRange = colorRange.copy();
        this.isPerChannel = isPerChannel;
        this.isSaturated = isSaturated;
    }
    public CustomStretchRgbParameters copy() {
        return new CustomStretchRgbParameters(colorRange, isPerChannel, isSaturated);
    }
    public static CustomStretchRgbParameters newEmpty() {
        return new CustomStretchRgbParameters(ColorRange.newEmptyRange(), true, true);
    }
    public static CustomStretchRgbParameters newFullRange() {
        return new CustomStretchRgbParameters(ColorRange.newFullRange(), true, true);
    }
    @Override
    public String toString() {
        return "CustomStretchRgbParameters{" +
                "colorRange=" + colorRange +
                ", isPerChannel=" + isPerChannel +
                ", isSaturated=" + isSaturated +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomStretchRgbParameters that = (CustomStretchRgbParameters) o;
        return isPerChannel == that.isPerChannel &&
                isSaturated == that.isSaturated &&
                Objects.equals(colorRange, that.colorRange);
    }
    @Override
    public int hashCode() {
        return Objects.hash(colorRange, isPerChannel, isSaturated);
    }
}
class SaveOptions {
    boolean saveGif = true;
    boolean saveLeftRightImages = true;
}
class DisplayParameters {
    double zoom, zoomL, zoomR;
    int offsetX, offsetY;
    double angle, angleR, angleL;
    DebayerMode debayerL, debayerR;
    ImageResamplingMode imageResamplingModeL, imageResamplingModeR;
    ColorCorrection lColorCorrection, rColorCorrection;
    boolean measurementShown = true;

    public DisplayParameters() {
        setDefaults();
    }
    void setDefaults() {
        zoom = zoomL = zoomR = 1.;
        offsetX = offsetY = 0;
        angle = angleL = angleR = 0.;
        debayerL = debayerR = DebayerMode.getUiDefault();
        imageResamplingModeL = imageResamplingModeR = ImageResamplingMode.getUiDefault();
        lColorCorrection = rColorCorrection = new ColorCorrection(Collections.EMPTY_LIST, CustomStretchRgbParameters.newFullRange());
        measurementShown = true;
    }
    void setDefaultsMrMl() {
        setDefaults();
        zoomR = 3.;
        offsetX = 700;//240;
        offsetY = 450;//72;
    }
    private DisplayParameters(double zoom, double zoomL, double zoomR, int offsetX, int offsetY, double angle, double angleL, double angleR, DebayerMode debayerL, DebayerMode debayerR, ImageResamplingMode imageResamplingModeL, ImageResamplingMode imageResamplingModeR, ColorCorrection lColorCorrection, ColorCorrection rColorCorrection, boolean measurementShown) {
        this.zoom = zoom;
        this.zoomL = zoomL;
        this.zoomR = zoomR;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.angle = angle;
        this.angleL = angleL;
        this.angleR = angleR;
        this.debayerL = debayerL;
        this.debayerR = debayerR;
        this.imageResamplingModeL = imageResamplingModeL;
        this.imageResamplingModeR = imageResamplingModeR;
        this.lColorCorrection = lColorCorrection;
        this.rColorCorrection = rColorCorrection;
        this.measurementShown = measurementShown;
    }
    public DisplayParameters swapped() {
        return new DisplayParameters(zoom, zoomR, zoomL, -offsetX, -offsetY, angle, angleR, angleL, debayerR, debayerL, imageResamplingModeR, imageResamplingModeL, rColorCorrection, lColorCorrection, measurementShown);
    }
    public DisplayParameters withColorCorrection(ColorCorrection lColorCorrection, ColorCorrection rColorCorrection) {
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, measurementShown);
    }
    public DisplayParameters withColorCorrection(boolean isRight, ColorCorrection cc) {
        ColorCorrection lColorCorrection = isRight ? this.lColorCorrection : cc;
        ColorCorrection rColorCorrection = isRight ? cc : this.rColorCorrection;
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, measurementShown);
    }
    public DisplayParameters withMeasurementShown(boolean measurementShown) {
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, measurementShown);
    }
    ColorCorrection getColorCorrection(boolean isRight) {
        return isRight ? rColorCorrection : lColorCorrection;
    }
}
class ImageAndPath {
    public static final String IN_PROGRESS_PATH = "..."; // pseudo-path, means "download in progress"
    public static final String NO_PATH = "-"; // pseudo-path, means "no path"
    public static final String ERROR_PATH = ""; // pseudo-path, means "error while loading path"
    public static final int DUMMY_SIZE = 12;
    final BufferedImage image;
    final String path;
    final String pathToLoad;

    public ImageAndPath(BufferedImage image, String path, String pathToLoad) {
        this.image = image;
        this.path = path;
        this.pathToLoad = pathToLoad;
    }
    public boolean isPathEqual(String otherPath) {
        boolean isThisPathSpecial = isSpecialPath(path);
        boolean isOtherPathSpecial = isSpecialPath(otherPath);
        if (isThisPathSpecial && isOtherPathSpecial) {
            return path.equals(otherPath);
        }
        if (!isThisPathSpecial && !isOtherPathSpecial) {
            return path.equals(otherPath) && !isDummyImage(image);
        }
        return false;
    }

    static BufferedImage dummyImage(Color color) {
        return _dummyImage(color, DUMMY_SIZE, DUMMY_SIZE);
    }
    static boolean isDummyImage(BufferedImage img) {
        return img.getWidth() == DUMMY_SIZE && img.getHeight() == DUMMY_SIZE;
    }
    static BufferedImage _dummyImage(Color color, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setPaint(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        return image;
    }
    static ImageAndPath imageIoRead(String path, String pathToLoad) throws IOException {
        BufferedImage res;
        if(isSpecialPath(path)) {
            Color color = IN_PROGRESS_PATH.equals(path)
                        ? new Color(0, 128, 255)
                        : NO_PATH.equals(path)
                        ? new Color(0, 0, 0)
                        : new Color(80, 20, 20);
            res = dummyImage(color);
        } else if(FileLocations.isCuriousUrn(path)) {
            String path1 = FileLocations.uncuriousUri(path);
            String imageid1 = FileLocations.getFileNameNoExt(path1);
            String ext1 = FileLocations.getFileExt(path1);
            Optional<String> match = MastcamPairFinder.findMrlMatch(imageid1).map(fname -> FileLocations.replaceFileName(path1, fname+ext1));
            String newPath = match.orElse("");
            return imageIoRead(newPath, newPath);
        } else if(FileLocations.isUrl(path)) {
            System.out.println("downloading "+path+ " ...");
            try {
                URLConnection uc = new URL(path).openConnection();
                NasaReader.setHttpHeaders(uc);
                try {
                    res = ImageIO.read(uc.getInputStream());
                    res.getWidth(); // throw an exception if null
                    System.out.println("downloaded " + path);
                } catch (IOException e) {
                    if (uc instanceof HttpURLConnection) {
                        HttpURLConnection httpUc = (HttpURLConnection) uc;
                        // Oracle recommends to read the error stream, but in practice
                        // if uc.getInputStream() failed with "java.net.ConnectException: Connection timed out",
                        // httpUc.getErrorStream() will also fail
                        try (
                                InputStream es = httpUc.getErrorStream()
                        ) {
                            int respCode = httpUc.getResponseCode();
                            System.out.println("Response code:" + respCode);
                            String error_text = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))
                                    .lines()
                                    .collect(Collectors.joining("\n"));
                            System.out.println("Error stream:");
                            System.out.println(error_text);
                        } catch (IOException ex) {
                            // deal with the exception
                            System.out.println("Error while printing the error stream:");
                            ex.printStackTrace();
                            System.out.println("DISCONNECTING URLConnection...");
                            httpUc.disconnect();
                        }
                    }
                    e.printStackTrace();
                    System.out.println("could not download "+path);
                    res = dummyImage(new Color(80,20,20));
                    NasaReader.cleanupReading();
                }
            } catch (Throwable t) {
                t.printStackTrace();
                System.out.println("could not download "+path);
                res = dummyImage(new Color(80,20,20));
            }
        } else {
            try {
                res = ImageIO.read(new File(path));
                res.getWidth(); // throw an exception if null
            } catch (Throwable t) {
                System.out.println("could not read file "+path);
                res = dummyImage(new Color(80,20,20));
            }
        }
        return new ImageAndPath(res, path, pathToLoad);
    }

    public static boolean isSpecialPath(String path) {
        return NO_PATH.equals(path) || IN_PROGRESS_PATH.equals(path) || ERROR_PATH.equals(path);
    }

    static ImageAndPath imageIoReadNoExc(String path, String pathToLoad) {
        try {
            return imageIoRead(path, pathToLoad);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "ImageAndPath{" +
                "image=" + (image == null
                           ? null
                           : ""+image.hashCode()+
                             "("+image.getWidth()+
                             "x"+image.getHeight()+")"
                           ) +
                ", path='" + path + '\'' +
                '}';
    }
}
// MVC Model
class RawData {
    final ImageAndPath left;
    final ImageAndPath right;

    public static RawData createInProgress(String leftPathToLoad, String rightPathToLoad) throws IOException {
        var res = new RawData(
            ImageAndPath.imageIoRead(ImageAndPath.IN_PROGRESS_PATH, leftPathToLoad),
            ImageAndPath.imageIoRead(ImageAndPath.IN_PROGRESS_PATH, rightPathToLoad)
        );
        return res;
    }
    public RawData(ImageAndPath left, ImageAndPath right) {
        this.left = left;
        this.right = right;
    }

    RawData swapped() {
        return new RawData(right, left);
    }

    @Override
    public String toString() {
        return "RawData{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
class MeasurementStatus {
    PanelMeasurementStatus left, right;
    MeasurementPointMark measurementPointMark;
    StereoPairParameters stereoPairParameters;
    boolean isWaitingForPoint;
    int pointIsWaitingFor;
    boolean isSubpixelPrecision;
    public MeasurementStatus(PanelMeasurementStatus left, PanelMeasurementStatus right) {
        this.left = left;
        this.right = right;
        isWaitingForPoint = false;
        pointIsWaitingFor = 0;
        isSubpixelPrecision = false;
        measurementPointMark = MeasurementPointMark.getUiDefault();
        stereoPairParameters = StereoPairParameters.getUiDefault(); // TODO: find out from file name ???
    }
    public MeasurementStatus() {
        this(new PanelMeasurementStatus(), new PanelMeasurementStatus());
    }
    public void setWaitingForPoint(int forPoint) {
        isWaitingForPoint = true;
        pointIsWaitingFor = forPoint;
    }
    public boolean isWaitingForPoint() {
        return isWaitingForPoint;
    }
    public int pointWaitingFor() {
        return pointIsWaitingFor;
    }
    public void clearWaitingForPoint() {
        isWaitingForPoint = false;
    }
    public MeasurementStatus swapped() {
        var res = new MeasurementStatus(right.copy(), left.copy());
        res.measurementPointMark = this.measurementPointMark;
        res.stereoPairParameters = this.stereoPairParameters;
        res.isWaitingForPoint = this.isWaitingForPoint;
        res.pointIsWaitingFor = this.pointIsWaitingFor;
        res.isSubpixelPrecision = this.isSubpixelPrecision;
        return res;
    }
    public MeasurementStatus copy() {
        var res = new MeasurementStatus(left.copy(), right.copy());
        res.measurementPointMark = this.measurementPointMark;
        res.stereoPairParameters = this.stereoPairParameters;
        res.isWaitingForPoint = this.isWaitingForPoint;
        res.pointIsWaitingFor = this.pointIsWaitingFor;
        res.isSubpixelPrecision = this.isSubpixelPrecision;
        return res;
    }
}
// TODO: remove this class, xN and yN are all double now!!!
class PanelMeasurementStatusD {
    double x1 = -1, y1 = -1, x2 = -1, y2 = -1, x3 = -1, y3 = -1, x4=-1, y4=-1, x5=-1, y5=-1;
    int w = 0, h = 0;
    double ifov;
    String descr;
    int centeringDX=0, centeringDY=0;
    final APoint first = new APoint() {
        @Override public double getAx() { return getAngle(x1, w); }
        @Override public double getAy() { return -getAngle(y1, h); }
        @Override public double getPx() { return x1-w/2; }
        @Override public double getPy() { return h/2-y1; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint second = new APoint() {
        @Override public double getAx() { return getAngle(x2, w); }
        @Override public double getAy() { return -getAngle(y2, h); }
        @Override public double getPx() { return x2-w/2; }
        @Override public double getPy() { return h/2-y2; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint third = new APoint() {
        @Override public double getAx() { return getAngle(x3, w); }
        @Override public double getAy() { return -getAngle(y3, h); }
        @Override public double getPx() { return x3-w/2; }
        @Override public double getPy() { return h/2-y3; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint fourth = new APoint() {
        @Override public double getAx() { return getAngle(x4, w); }
        @Override public double getAy() { return -getAngle(y4, h); }
        @Override public double getPx() { return x4-w/2; }
        @Override public double getPy() { return h/2-y4; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint fifth = new APoint() {
        @Override public double getAx() { return getAngle(x5, w); }
        @Override public double getAy() { return -getAngle(y5, h); }
        @Override public double getPx() { return x5-w/2; }
        @Override public double getPy() { return h/2-y5; }
        @Override public double getIfov() { return ifov; }
    };
    private double getAngle(double xy, double wh) {
        return ifov*(xy - wh/2);
    }
    @Override
    public String toString() {
        return ""+descr+
                "  " + x1 + " " + y1 +
                "  " + x2 + " " + y2 +
                "  " + x3 + " " + y3 +
                "  " + x4 + " " + y4 +
                "  " + x5 + " " + y5 +
                "  w=" + w + " h=" + h +
                " ifov=" + ifov;
    }
    public PanelMeasurementStatusD copyD() {
        var res = new PanelMeasurementStatusD();
        res.x1 = this.x1;
        res.x2 = this.x2;
        res.x3 = this.x3;
        res.x4 = this.x4;
        res.x5 = this.x5;
        res.y1 = this.y1;
        res.y2 = this.y2;
        res.y3 = this.y3;
        res.y4 = this.y4;
        res.y5 = this.y5;
        res.w = this.w;
        res.h = this.h;
        res.ifov = this.ifov;
        res.descr = this.descr;
        res.centeringDX = this.centeringDX;
        res.centeringDY = this.centeringDY;
        return res;
    }
    double xrotated(double x, double y, double rho) { return w/2 + (x-w/2)*Math.cos(rho) - (y-h/2)*Math.sin(rho); }
    double yrotated(double x, double y, double rho) { return h/2 + (x-w/2)*Math.sin(rho) + (y-h/2)*Math.cos(rho); }
    public PanelMeasurementStatusD addRollD(double rho) {
        var res = copyD();
        res.x1 = xrotated(x1, y1, rho);
        res.y1 = yrotated(x1, y1, rho);
        res.x2 = xrotated(x2, y2, rho);
        res.y2 = yrotated(x2, y2, rho);
        res.x3 = xrotated(x3, y3, rho);
        res.y3 = yrotated(x3, y3, rho);
        return res;
    }
    public PanelMeasurementStatusD addPitchD(double a) {
        var res = copyD();
        double pxs = a/ifov;
        res.y1 += pxs;
        res.y2 += pxs;
        res.y3 += pxs;
        return res;
    }
    public PanelMeasurementStatusD addYawD(double a) {
        var res = copyD();
        double pxs = a/ifov;
        res.x1 += pxs;
        res.x2 += pxs;
        res.x3 += pxs;
        return res;
    }

}

// TODO: camera parameters: L/R
class PanelMeasurementStatus {
    double x1=-1, y1=-1, x2=-1, y2=-1, x3=-1, y3=-1, x4=-1, y4=-1, x5=-1, y5=-1;
    int w=0, h=0;
    double ifov;
    String descr;
    int centeringDX=0, centeringDY=0;
    final APoint first = new APoint() {
        @Override public double getAx() { return getAngle(x1, w); }
        @Override public double getAy() { return -getAngle(y1, h); }
        @Override public double getPx() { return x1-w/2; }
        @Override public double getPy() { return h/2-y1; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint second = new APoint() {
        @Override public double getAx() { return getAngle(x2, w); }
        @Override public double getAy() { return -getAngle(y2, h); }
        @Override public double getPx() { return x2-w/2; }
        @Override public double getPy() { return h/2-y2; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint third = new APoint() {
        @Override public double getAx() { return getAngle(x3, w); }
        @Override public double getAy() { return -getAngle(y3, h); }
        @Override public double getPx() { return x3-w/2; }
        @Override public double getPy() { return h/2-y3; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint fourth = new APoint() {
        @Override public double getAx() { return getAngle(x4, w); }
        @Override public double getAy() { return -getAngle(y4, h); }
        @Override public double getPx() { return x4-w/2; }
        @Override public double getPy() { return h/2-y4; }
        @Override public double getIfov() { return ifov; }
    };
    final APoint fifth = new APoint() {
        @Override public double getAx() { return getAngle(x5, w); }
        @Override public double getAy() { return -getAngle(y5, h); }
        @Override public double getPx() { return x5-w/2; }
        @Override public double getPy() { return h/2-y5; }
        @Override public double getIfov() { return ifov; }
    };
    private double getAngle(double xy, int wh) {
        return ifov*(xy - wh/2);
    }
    public PanelMeasurementStatus copy() {
        var res = new PanelMeasurementStatus();
        res.x1 = this.x1;
        res.x2 = this.x2;
        res.x3 = this.x3;
        res.x4 = this.x4;
        res.x5 = this.x5;
        res.y1 = this.y1;
        res.y2 = this.y2;
        res.y3 = this.y3;
        res.y4 = this.y4;
        res.y5 = this.y5;
        res.w = this.w;
        res.h = this.h;
        res.ifov = this.ifov;
        res.descr = this.descr;
        res.centeringDX = this.centeringDX;
        res.centeringDY = this.centeringDY;
        return res;
    }
    public PanelMeasurementStatusD copyD() {
        var res = new PanelMeasurementStatusD();
        res.x1 = this.x1;
        res.x2 = this.x2;
        res.x3 = this.x3;
        res.x4 = this.x4;
        res.x5 = this.x5;
        res.y1 = this.y1;
        res.y2 = this.y2;
        res.y3 = this.y3;
        res.y4 = this.y4;
        res.y5 = this.y5;
        res.w = this.w;
        res.h = this.h;
        res.ifov = this.ifov;
        res.descr = this.descr;
        res.centeringDX = this.centeringDX;
        res.centeringDY = this.centeringDY;
        return res;
    }
    public BufferedImage drawMarks(BufferedImage img, MeasurementPointMark measurementPointMark) {
        BufferedImage res = img;
        // old version (xN, yN used to be int)
        int[] x = {(int)x1, (int)x2, (int)x3, (int)x4, (int)x5};
        int[] y = {(int)y1, (int)y2, (int)y3, (int)y4, (int)y5};
        int[] rgb = {0xFF0000, 0x00FF00, 0x0000FF, 0x00FFFF, 0xFF00FF};
        for (int i=0; i<5; i++) {
            if (x[i] >= 0 && y[i] >= 0 && x[i] < img.getWidth() && y[i] < img.getHeight()) {
                if (res == img) {
                    res = copyImage(img);
                }
                measurementPointMark.drawMark(res, x[i], y[i], rgb[i]);
            }
        }
        return res;
    }
    public BufferedImage drawMarks(BufferedImage img, MeasurementPointMark measurementPointMark, AffineTransform transform, double zoomLevel, int offX, int offY) {
        BufferedImage res = img;
        double[] x = {x1, x2, x3, x4, x5};
        double[] y = {y1, y2, y3, y4, y5};
        int[] rgb = {0xFF0000, 0x00FF00, 0x0000FF, 0x00FFFF, 0xFF00FF};
        for (int i=0; i<5; i++) {
            if (x[i] >= 0 && y[i] >= 0 && x[i] < img.getWidth() && y[i] < img.getHeight()) {
                if (res == img) {
                    res = copyImage(img);
                }
                measurementPointMark.drawMark(res, X3DViewer.mult(x[i]+centeringDX+offX, zoomLevel), X3DViewer.mult(y[i]+centeringDY+offY, zoomLevel), rgb[i]);
            }
        }
        return res;
    }
    static Stream<int[]> symmetricPoints(int[] p) {
        int[][] points = {{p[0],p[1]}, {-p[0],p[1]}, {p[0],-p[1]}, {-p[0],-p[1]}};
        return Arrays.stream(points)
               .flatMap(xy -> Arrays.stream(xy[0] == xy[1] ? new int[][]{xy} : new int[][] {xy, {xy[1], xy[0]}}));
    }
    public static BufferedImage copyImage(BufferedImage source){
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                b.setRGB(i,j,source.getRGB(i,j));
            }
        };
        return b;
    }
    public void setWHI(BufferedImage image, double new_ifov, String description) {
        w = image.getWidth();
        h = image.getHeight();
        ifov = new_ifov;
        descr = description;
    }
    @Override
    public String toString() {
        return ""+descr+
                "  " + x1 + " " + y1 +
                "  " + x2 + " " + y2 +
                "  " + x3 + " " + y3 +
                "  " + x4 + " " + y4 +
                "  " + x5 + " " + y5 +
                "  w=" + w + " h=" + h +
                " ifov=" + ifov;
    }
}
interface APoint {
    double getAx();
    double getAy();
    double getPx();
    double getPy();
    double getIfov();
    default String asString() {
        return "("+getPx()+", "+getPy()+"; ifov="+String.format("%.3e", getIfov())+")";
    }
}
// MVC Controller
class UiController implements UiEventListener {
    X3DViewer x3dViewer;
    DisplayParameters displayParameters;
    RawData rawData;
    MeasurementStatus measurementStatus;
    SaveOptions saveOptions;

    final LRNavigator lrNavigator;
    boolean dndOneToBoth = true;
    boolean unthumbnail = true;
    volatile long lastLoadTimestampL;
    volatile long lastLoadTimestampR;
    public UiController(X3DViewer xv, LRNavigator lrn, RawData rd, DisplayParameters dp, MeasurementStatus ms, SaveOptions so) {
        x3dViewer = xv;
        displayParameters = dp;
        rawData = rd;
        measurementStatus = ms;
        lrNavigator = lrn;
        saveOptions = so;
    }
    @Override
    public void zoomChanged(double newZoom) {
        displayParameters.zoom = newZoom;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void lZoomChanged(double newZoom) {
        displayParameters.zoomL = newZoom;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void rZoomChanged(double newZoom) {
        displayParameters.zoomR = newZoom;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void angleChanged(double newAngle) {
        displayParameters.angle = newAngle;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void lAngleChanged(double newLAngle) {
        displayParameters.angleL = newLAngle;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void rAngleChanged(double newRAngle) {
        displayParameters.angleR = newRAngle;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void lDebayerModeChanged(DebayerMode newLDebayerMode) {
        displayParameters.debayerL = newLDebayerMode;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void rDebayerModeChanged(DebayerMode newRDebayerMode) {
        displayParameters.debayerR = newRDebayerMode;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void lImageResamplingModeChanged(ImageResamplingMode newImageResamplingModeL) {
        displayParameters.imageResamplingModeL = newImageResamplingModeL;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
    }
    @Override
    public void rImageResamplingModeChanged(ImageResamplingMode newImageResamplingModeR) {
        displayParameters.imageResamplingModeR = newImageResamplingModeR;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
    }
    @Override
    public void xOffsetChanged(int newXOff) {
        displayParameters.offsetX = newXOff;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void yOffsetChanged(int newYOff) {
        displayParameters.offsetY = newYOff;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void lColorCorrectionChanged(ColorCorrection colorCorrection) {
        displayParameters.lColorCorrection = colorCorrection;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void rColorCorrectionChanged(ColorCorrection colorCorrection) {
        displayParameters.rColorCorrection = colorCorrection;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void swapImages() {
        System.out.println("swapImages "+Thread.currentThread());
        x3dViewer.updateViews(rawData = rawData.swapped(), displayParameters = displayParameters.swapped(), measurementStatus = measurementStatus.swapped());
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
        lrNavigator.swap();
    }
    @Override
    public void swapImageUrls() {
        System.out.println("swapImageUrls "+Thread.currentThread());
        x3dViewer.updateViews(rawData = rawData.swapped(), displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
        lrNavigator.swap();
    }

    @Override
    public void dndImport(String s, boolean isRight, OneOrBothPanes oneOrBoth) {
        List<String> urls = Arrays.asList(s.trim().split("[\\r\\n]+"));
        if (oneOrBoth == OneOrBothPanes.JUST_THIS) {
            urls = Arrays.asList(urls.get(0)); // ignore others
        }
        if (urls.size() > 2) {
            urls = urls.subList(0,1);
        }
        if (unthumbnail) {
            urls = HttpLocations.unPageAll(urls);
        }
        List<String> paths;
        switch (urls.size()) {
            case 0:
                return;
            case 1: {
                if (oneOrBoth==OneOrBothPanes.BOTH_PANES
                 || oneOrBoth==OneOrBothPanes.SEE_CHECKBOX && dndOneToBoth) {
                    paths = FileLocations.twoPaths(urls.get(0));
                } else {
                    if (isRight) {
                        paths = Arrays.asList(rawData.left.path, urls.get(0));
                    } else {
                        paths = Arrays.asList(urls.get(0), rawData.right.path);
                    }
                }
            }
            break;
            default:
            case 2: {
                paths = FileLocations.twoPaths(urls.get(0), urls.get(1));
            }
            break;
        }
        var uPaths = unThumbnailIfNecessary(paths);
        lrNavigator
            .onSetLeft(rawData.left.path, uPaths.get(0))
            .onSetRight(rawData.right.path, uPaths.get(1));
        showInProgressViewsAndThen(uPaths,
            () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
    }

    @Override
    public void dndSingleToBothChanged(boolean newValue) {
        dndOneToBoth = newValue;
    }

    @Override
    public void unthumbnailChanged(boolean newValue) {
        unthumbnail = newValue;
    }

    @Override
    public void copyUrl(boolean isRight) {
        String toCopy;
        try {
            toCopy = (isRight ? rawData.right : rawData.left).path;
        } catch (NullPointerException e) {
            System.out.println("nothing to copy: "+rawData);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(toCopy), null);
    }

    @Override
    public void copyUrls() {
        String toCopy = rawData.right.path + "\n" + rawData.left.path + "\n";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(toCopy), null);
    }

    @Override
    public void loadMatchOfOther(boolean isRight) {
        var paths = FileLocations.twoPaths((isRight ? rawData.left : rawData.right).path);
        var uPaths = unThumbnailIfNecessary(paths);
        lrNavigator
                .onSetLeft(rawData.left.path, uPaths.get(0))
                .onSetRight(rawData.right.path, uPaths.get(1));
        showInProgressViewsAndThen(uPaths,
                () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
    }

    @Override
    public void loadCopyOfOther(boolean isRight) {
        var other = isRight ? rawData.left : rawData.right;
        var otherNavigator = isRight ? lrNavigator.left : lrNavigator.right;
        changeRawData(new RawData(other, other));
        lrNavigator.left = otherNavigator;
        lrNavigator.right = FileNavigator.copy(otherNavigator);
    }

    @Override
    public void reload(boolean isRight) {
        var paths = Arrays.asList(rawData.left.pathToLoad, rawData.right.pathToLoad);
        var uPaths = unThumbnailIfNecessary(paths);
        showInProgressViewsAndThen(uPaths,
                () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
    }

    @Override
    public void gotoImage(GoToImageOptions goToImageOptions, boolean isRight, Optional<Integer> sol) {
        switch (goToImageOptions) {
            case CURIOSITY_FIRST_OF_SOL:
                sol.ifPresent(nSol -> {
                    System.out.println("gotoImage: " + goToImageOptions + " r:" + isRight + " " + nSol);
                    lrNavigator.toCuriositySol(this, true, true, nSol);
                });
                break;
            case CURIOSITY_LATEST:
                System.out.println("gotoImage: " + goToImageOptions + " r:" + isRight);
                lrNavigator.toCuriosityLatest(this, true, true);
                break;
            case PERSEVERANCE_FIRST_OF_SOL:
                sol.ifPresent(nSol -> {
                    System.out.println("gotoImage: " + goToImageOptions + " r:" + isRight);
                    lrNavigator.toPerseveranceSol(this, true, true, nSol);
                });
                break;
            case PERSEVERANCE_LATEST:
                System.out.println("gotoImage: " + goToImageOptions + " r:" + isRight);
                lrNavigator.toPerseveranceLatest(this, true, true);
                break;
        }
    }

    @Override
    public void newWindow() {
        ProcessForker.forkWrapped();
    }

    @Override
    public void setShowUrls(boolean visible) {
        x3dViewer.addUrlViews(visible, true);
    }

    @Override
    public void resetToDefaults() {
        System.out.println(rawData.left.path);
        System.out.println(rawData.right.path);
        if (MastcamPairFinder.areMrMlMatch(rawData.left.path, rawData.right.path)) {
            displayParameters.setDefaultsMrMl();
        } else {
            displayParameters.setDefaults();
        }
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
        // TODO: reset measurementStatus
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }

    @Override
    public void saveScreenshot() {
        x3dViewer.screenshotSaver.takeAndSaveScreenshot(x3dViewer.frame, x3dViewer.componentL, x3dViewer.componentR, rawData, displayParameters, saveOptions);
    }

    @Override
    public void navigate(boolean isRight, boolean isLeft, boolean forwardInTime, int byOneOrTwo) {
        lrNavigator.navigate(this, isRight, isLeft, forwardInTime, byOneOrTwo, rawData.left.pathToLoad, rawData.right.pathToLoad);
    }

    @Override
    public void openInBrowser(SiteOpenCommand command, boolean isRight, WhichRover whichRover) {
        System.out.println("isRight="+isRight+" whichRover="+whichRover);
        switch (whichRover) {
            case CURIOSITY:
                switch (command) {
                    case OPEN_CURRENT_SOL:
                        CompletableFuture.supplyAsync(() -> {
                                    CuriosityOpener.openCurrentSol(rawData, isRight);
                                    return null;
                                }
                        );
                        break;
                    case OPEN_TAKEN_LATER:
                        CuriosityOpener.openStartingFrom(rawData, isRight, true);
                        break;
                    case OPEN_TAKEN_EARLIER:
                        CuriosityOpener.openStartingFrom(rawData, isRight, false);
                        break;
                    case OPEN_LATEST:
                        CuriosityOpener.openLatest();
                        break;
                }
                break;
            case PERSEVERANCE:
                switch (command) {
                    case OPEN_CURRENT_SOL:
                        CompletableFuture.supplyAsync(() -> {
                                    PerseveranceOpener.openCurrentSol(rawData, isRight);
                                    return null;
                                }
                        );
                        break;
//                    case OPEN_TAKEN_LATER:
//                        CuriosityOpener.openStartingFrom(rawData, isRight, true);
//                        break;
//                    case OPEN_TAKEN_EARLIER:
//                        CuriosityOpener.openStartingFrom(rawData, isRight, false);
//                        break;
                    case OPEN_LATEST:
                        PerseveranceOpener.openLatest();
                        break;
                }
                break;
        }
    }

    @Override
    public void markPointWithMousePress(boolean isRight, MouseEvent e) {
        if (measurementStatus.isWaitingForPoint()) {
            System.out.println("markPointWithMousePress r="+isRight+" x="+e.getX()+" y="+e.getY()+" btn="+e.getButton());
            System.out.println("e="+e);
            var lr = isRight ? measurementStatus.right : measurementStatus.left;
            var zoom = displayParameters.zoom * (isRight ? displayParameters.zoomR : displayParameters.zoomL);
            var offX = Math.max(0, isRight ? -displayParameters.offsetX : displayParameters.offsetX);
            var offY = Math.max(0, isRight ? -displayParameters.offsetY : displayParameters.offsetY);
            int x = (int) (e.getX() / zoom - offX - lr.centeringDX);
            int y = (int) (e.getY() / zoom - offY - lr.centeringDY);
            switch (measurementStatus.pointWaitingFor()) {
                case 1:
                    lr.x1 = x;
                    lr.y1 = y;
                    break;
                case 2:
                    lr.x2 = x;
                    lr.y2 = y;
                    break;
                case 3:
                    lr.x3 = x;
                    lr.y3 = y;
                    break;
                case 4:
                    lr.x4 = x;
                    lr.y4 = y;
                    break;
                case 5:
                    lr.x5 = x;
                    lr.y5 = y;
                    break;
            }
            measurementStatus.clearWaitingForPoint();
            x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
            x3dViewer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
    @Override
    public void setWaitingForPoint(int forPointNumber) {
        measurementStatus.setWaitingForPoint(forPointNumber);
        x3dViewer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }
    @Override
    public void markedPointChanged(int coordId, double lrXy123) {
        switch (coordId) {
            case 0:  measurementStatus.left.x1 = lrXy123; break;
            case 1:  measurementStatus.left.y1 = lrXy123; break;
            case 2:  measurementStatus.left.x2 = lrXy123; break;
            case 3:  measurementStatus.left.y2 = lrXy123; break;
            case 4:  measurementStatus.left.x3 = lrXy123; break;
            case 5:  measurementStatus.left.y3 = lrXy123; break;
            case 6:  measurementStatus.left.x4 = lrXy123; break;
            case 7:  measurementStatus.left.y4 = lrXy123; break;
            case 8:  measurementStatus.left.x5 = lrXy123; break;
            case 9:  measurementStatus.left.y5 = lrXy123; break;
            case 10: measurementStatus.right.x1 = lrXy123; break;
            case 11: measurementStatus.right.y1 = lrXy123; break;
            case 12: measurementStatus.right.x2 = lrXy123; break;
            case 13: measurementStatus.right.y2 = lrXy123; break;
            case 14: measurementStatus.right.x3 = lrXy123; break;
            case 15: measurementStatus.right.y3 = lrXy123; break;
            case 16: measurementStatus.right.x4 = lrXy123; break;
            case 17: measurementStatus.right.y4 = lrXy123; break;
            case 18: measurementStatus.right.x5 = lrXy123; break;
            case 19: measurementStatus.right.y5 = lrXy123; break;
            default: throw new IllegalArgumentException("coordId="+coordId+" has no meaning, valid: [0..12)");
        }
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void clearAllMarks() {
        measurementStatus.left.x1 =
        measurementStatus.left.y1 =
        measurementStatus.left.x2 =
        measurementStatus.left.y2 =
        measurementStatus.left.x3 =
        measurementStatus.left.y3 =
        measurementStatus.left.x4 =
        measurementStatus.left.y4 =
        measurementStatus.left.x5 =
        measurementStatus.left.y5 =
        measurementStatus.right.x1 =
        measurementStatus.right.y1 =
        measurementStatus.right.x2 =
        measurementStatus.right.y2 =
        measurementStatus.right.x3 =
        measurementStatus.right.y3 =
        measurementStatus.right.x4 =
        measurementStatus.right.y4 =
        measurementStatus.right.x5 =
        measurementStatus.right.y5 = -1;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void setSubpixelPrecisionMarks(boolean precise) {
        measurementStatus.isSubpixelPrecision = precise;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void stereoCameraChanged(StereoPairParameters v) {
        System.out.println("stereoCameraChanged("+v+")");
        measurementStatus.stereoPairParameters = v;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
        // updateControls?
    }
    @Override
    public void markShapeChanged(MeasurementPointMark v) {
        measurementStatus.measurementPointMark = v;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void measurementShownChanged(boolean newIsShown) {
        displayParameters.measurementShown = newIsShown;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    @Override
    public void adjustOffsets() {
        displayParameters.offsetX = (int) (measurementStatus.right.x1 - measurementStatus.left.x1);
        displayParameters.offsetY = (int) (measurementStatus.right.y1 - measurementStatus.left.y1);
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
    }
    @Override
    public void escapePressed() {
        if (measurementStatus.isWaitingForPoint()) {
            measurementStatus.clearWaitingForPoint();
            x3dViewer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
    @Override
    public Optional<Integer> getSol(boolean isRight, WhichRover whichRover) {
        String currentPath = (isRight ? rawData.right : rawData.left).pathToLoad;
        boolean isPerseverance = currentPath.contains("/mars2020");
        if ((whichRover == WhichRover.PERSEVERANCE) != isPerseverance) {
            return Optional.empty();
        }
        return FileLocations.getSol(currentPath);
    }
    @Override
    public MeasurementStatus getMeasurementStatus() {
        return measurementStatus.copy();
    }
    @Override
    public DisplayParameters getDisplayParameters() {
        return displayParameters;
    }
    public List<String> unThumbnailIfNecessary(List<String> urlsOrFiles) {
        if (unthumbnail) {
            return urlsOrFiles.stream().map(FileLocations::unThumbnail).collect(Collectors.toList());
        } else {
            return urlsOrFiles;
        }
    }
    private ColorCorrection insertSrgb3IfNotThere(ColorCorrection cc) {
        if (!cc.getAlgos().contains(ColorCorrectionAlgo.STRETCH_CONTRAST_RGB_RGB3)) {
            cc = cc.copyWith(Arrays.asList(ColorCorrectionAlgo.STRETCH_CONTRAST_RGB_RGB3));
        }
        return cc;
    }
    @Override
    public ColorRange getViewportColorRange(boolean isRight) {
        Rectangle visibleArea = x3dViewer.getViewportRectangle(isRight);
        ColorCorrection lcc = insertSrgb3IfNotThere(displayParameters.lColorCorrection);
        ColorCorrection rcc = insertSrgb3IfNotThere(displayParameters.rColorCorrection);
        var dp = displayParameters
                .withColorCorrection(lcc, rcc)
                .withMeasurementShown(false);
        var bis = x3dViewer.processBothImages(rawData, dp, measurementStatus, ColorCorrection.Command.GET_RANGE);
        var bi = bis.get(isRight ? 1 : 0);
        var cr = ColorBalancer.getColorRangeFromImage(visibleArea, bi);
        return cr;
    }
    @Override
    public CustomStretchRgbParameters getCurrentCustomStretchRgbParameters(boolean isRight) {
        return displayParameters.getColorCorrection(isRight).customStretchRgbParameters;
    }
    @Override
    public void setCustomStretchRgbParameters(CustomStretchRgbParameters customStretchRgbParameters, boolean isRight) {
        ColorCorrection cc = displayParameters
                .getColorCorrection(isRight)
                .copyWith(customStretchRgbParameters);
        var newDp = displayParameters.withColorCorrection(isRight, cc);
        x3dViewer.updateViews(rawData, displayParameters=newDp, measurementStatus);
    }
    @Override
    public void setSaveOptions(boolean saveGif, boolean saveLeftRIght) {
        saveOptions.saveGif = saveGif;
        saveOptions.saveLeftRightImages = saveLeftRIght;
        x3dViewer.updateControls(displayParameters, measurementStatus, saveOptions);
    }
    public void createAndShowViews() {
        x3dViewer.createViews(rawData, displayParameters, measurementStatus, this);
    }
    public void changeRawData(RawData newRawData) {
        x3dViewer.updateViews(rawData=newRawData, displayParameters, measurementStatus);
    }
    public void showInProgressViewsAndThen(Supplier<List<String>> pathSrc, Runnable next) {
        RawData rawData0 = rawData;
        showInProgressViewsAndThen(
                Arrays.asList(ImageAndPath.IN_PROGRESS_PATH, ImageAndPath.IN_PROGRESS_PATH),
                () -> {
                    var paths = pathSrc.get();
                    changeRawData(rawData0);
                    showInProgressViewsAndThen(paths, next);
                }
        );
    }
    public void showInProgressViewsAndThen(List<String> paths, Runnable next) {
        try {
            ImageAndPath l = rawData.left.isPathEqual(paths.get(0))
                           ? rawData.left
                           : ImageAndPath.imageIoRead(ImageAndPath.IN_PROGRESS_PATH, paths.get(0));
            ImageAndPath r = rawData.right.isPathEqual(paths.get(1))
                           ? rawData.right
                           : ImageAndPath.imageIoRead(ImageAndPath.IN_PROGRESS_PATH, paths.get(1));
            var rdWhileInProgress = new RawData(l, r);
            javax.swing.SwingUtilities.invokeLater(
                    () -> {
                        changeRawData(rdWhileInProgress);
                        next.run();
                    }
            );
        } catch (IOException e) {
            System.err.println("an exception happened that could not happen even theoretically");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public void updateRawDataAsync(String path1, String path2) {
        boolean sameLeftPath = rawData.left.isPathEqual(path1);
        boolean sameRightPath = rawData.right.isPathEqual(path2);
        long timestamp = System.currentTimeMillis();
        if (!sameLeftPath) {
            lastLoadTimestampL = timestamp;
        }
        if (!sameRightPath) {
            lastLoadTimestampR = timestamp;
        }
//        System.out.println("path1: "+path1);
//        System.out.println("path2: "+path2);
//        System.out.println("path1: "+rawData.left.path);
//        System.out.println("path2: "+rawData.right.path);
//        System.out.println("path1: "+path1.equals(rawData.left.path) +"&&" + !ImageAndPath.isDummyImage(rawData.left.image));
//        System.out.println("path2: "+path2.equals(rawData.right.path) +"&&"+ !ImageAndPath.isDummyImage(rawData.right.image));
        CompletableFuture<ImageAndPath> futureImage2 =
                sameRightPath
              ? CompletableFuture.completedFuture(rawData.right)
              : CompletableFuture
                .supplyAsync(() -> ImageAndPath.imageIoReadNoExc(path2, path2))
                .exceptionally(t -> ImageAndPath.imageIoReadNoExc("", path2));
        CompletableFuture<ImageAndPath> futureImage1 =
                sameLeftPath
              ? CompletableFuture.completedFuture(rawData.left)
              : CompletableFuture
                .supplyAsync(() -> {
                    // Why: the NASA site sometimes returns a timeout/refused error,
                    // and I suspect that this is because this software loads two images
                    // in parallel, unlike any browser
                    try {
                        switch (Main.PARALLEL_IMAGE_LOADING) {
                            case PARALLEL:
                                break;
                            case DELAYED:
                                int randomDelay = (int)(Math.random() * 1000);
                                futureImage2.get(1200+randomDelay, TimeUnit.MILLISECONDS);
                                break;
                            case SEQUENTIAL:
                                futureImage2.get();
                                break;
                        }
                    } catch (InterruptedException|ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        System.out.println("waiting stopped because of a timeout");
                    }
                    return ImageAndPath.imageIoReadNoExc(path1, path1);
                })
                .exceptionally(t -> ImageAndPath.imageIoReadNoExc("", path1));

        Runnable uiUpdateRunnableSyncL =
            () -> javax.swing.SwingUtilities.invokeLater(
                () -> {
                    if (lastLoadTimestampL == timestamp) {
                        this.changeRawData(
                            new RawData(getNow(futureImage1, () -> ImageAndPath.imageIoReadNoExc(ImageAndPath.IN_PROGRESS_PATH, path1)),
                                        rawData.right
                            )
                        );
                    }
                }
            );
        Runnable uiUpdateRunnableSyncR =
            () -> javax.swing.SwingUtilities.invokeLater(
                () -> {
                    if (lastLoadTimestampR == timestamp) {
                        this.changeRawData(
                            new RawData(rawData.left,
                                        getNow(futureImage2, () -> ImageAndPath.imageIoReadNoExc(ImageAndPath.IN_PROGRESS_PATH, path2))
                            )
                        );
                    }
                }
            );
        futureImage1.thenRunAsync( uiUpdateRunnableSyncL );
        futureImage2.thenRunAsync( uiUpdateRunnableSyncR );
        // the two above ui updates are synchronized via the invokeLater() queue
        // the 2nd one, whichever it is, shows both images
    }
    public static <T> T getNow(CompletableFuture<T> future, Supplier<T> valueIfAbsent) {
        var res = future.getNow(null);
        if (res != null) {
            return res;
        } else {
            return valueIfAbsent.get();
        }
    }
}

// MVC View
class X3DViewer {
    JButton lblL;
    JButton lblR;
    JScrollPane componentL;
    JScrollPane componentR;
    JComponent urlPanel;
    JLabel urlL;
    JLabel urlR;
    JLabel colorCorrectionDescriptionL;
    JLabel colorCorrectionDescriptionR;
    ColorCorrectionPane colorCorrectionPane;
    MeasurementPanel measurementPanel;
    SettingsPanel settingsPanel;
    JCheckBoxMenuItem showMeasurementCbMenuItem;
    JFrame frame;
    DigitalZoomControl<Double, ZoomFactorWrapper> dcZoom;
    DigitalZoomControl<Double, ZoomFactorWrapper> dcZoomL;
    DigitalZoomControl<Double, ZoomFactorWrapper> dcZoomR;
    DigitalZoomControl<Double, RotationAngleWrapper> dcAngle;
    DigitalZoomControl<Double, RotationAngleWrapper> dcAngleL;
    DigitalZoomControl<Double, RotationAngleWrapper> dcAngleR;
    DigitalZoomControl<Integer, OffsetWrapper> dcOffX;
    DigitalZoomControl<Integer, OffsetWrapper> dcOffY;
    DebayerModeChooser debayerL;
    DebayerModeChooser debayerR;
    JButton helpButton;
    ScreenshotSaver screenshotSaver = new ScreenshotSaver(new JFileChooser());

    public void updateControls(DisplayParameters dp, MeasurementStatus ms, SaveOptions so) {
        dcZoom.setValueAndText(dp.zoom);
        dcZoomL.setValueAndText(dp.zoomL);
        dcZoomR.setValueAndText(dp.zoomR);
        dcOffX.setValueAndText(dp.offsetX);
        dcOffY.setValueAndText(dp.offsetY);
        dcAngle.setValueAndText(dp.angle);
        dcAngleL.setValueAndText(dp.angleL);
        dcAngleR.setValueAndText(dp.angleR);
        debayerL.setValue(dp.debayerL);
        debayerR.setValue(dp.debayerR);
        colorCorrectionPane.setColorCorrectionValue(false, dp.lColorCorrection);
        colorCorrectionPane.setColorCorrectionValue(true, dp.rColorCorrection);
        colorCorrectionPane.setImageResamplingModeValue(false, dp.imageResamplingModeL);
        colorCorrectionPane.setImageResamplingModeValue(true, dp.imageResamplingModeR);
        showMeasurementCbMenuItem.setState(dp.measurementShown);
        measurementPanel.setControls(ms);
        settingsPanel.setControls(so);
    }
    void setCursor(Cursor cursor) {
        lblL.setCursor(cursor);
        lblR.setCursor(cursor);
    }
    Rectangle getViewportRectangle(boolean isRight) {
        var scrollPane = isRight?componentR:componentL;
        Point pos = scrollPane.getViewport().getViewPosition();
        Dimension size = scrollPane.getViewport().getExtentSize();
        var res = new Rectangle(
            (int) pos.getX(),
            (int) pos.getY(),
            (int) size.getWidth(),
            (int) size.getHeight()
        );
        return res;
    }
    List<BufferedImage> processBothImages(RawData rd, DisplayParameters dp, MeasurementStatus ms, ColorCorrection.Command command) {
        final boolean PRECISE_MARKS = ms.isSubpixelPrecision;
        BufferedImage imgL = dp.debayerL.doAlgo(rd.left.image, () -> FileLocations.isBayered(rd.left.path), Debayer.debayering_methods);
        BufferedImage imgR = dp.debayerR.doAlgo(rd.right.image, () -> FileLocations.isBayered(rd.right.path), Debayer.debayering_methods);

        imgL = dp.lColorCorrection.doColorCorrection(imgL, command);
        imgR = dp.rColorCorrection.doColorCorrection(imgR, command);

        ms.left.setWHI(imgL, ms.stereoPairParameters.ifovL, "x3d:L straight:R");
        ms.right.setWHI(imgR, ms.stereoPairParameters.ifovR, "x3d:R, straight:L");
        if (!PRECISE_MARKS && dp.measurementShown) {
            imgL = ms.left.drawMarks(imgL, ms.measurementPointMark);
            imgR = ms.right.drawMarks(imgR, ms.measurementPointMark);
        }

        AffineTransform transformL = rotationTransform(imgL, dp.angle + dp.angleL);
        AffineTransform transformR = rotationTransform(imgL, dp.angle + dp.angleR);
        BufferedImage rotatedL = rotate(imgL, transformL);
        BufferedImage rotatedR = rotate(imgR, transformR);
        int dw = (rotatedR.getWidth() - rotatedL.getWidth()) / 2;
        int dh = (rotatedR.getHeight() - rotatedL.getHeight()) / 2;
        ms.left.centeringDX = Math.max(0,dw);
        ms.left.centeringDY = Math.max(0,dh);
        ms.right.centeringDX = Math.max(0,-dw);
        ms.right.centeringDY = Math.max(0,-dh);
        if (!PRECISE_MARKS || !dp.measurementShown) {
            return Arrays.asList(
                    zoom(rotatedL, dp.zoom * dp.zoomL, rotatedR, dp.zoom * dp.zoomR, dp.offsetX + dw, dp.offsetY + dh, dp.imageResamplingModeL),
                    zoom(rotatedR, dp.zoom * dp.zoomR, rotatedL, dp.zoom * dp.zoomL, -dp.offsetX - dw, -dp.offsetY - dh, dp.imageResamplingModeR)
            );
        } else {
            return Arrays.asList(
                    ms.left.drawMarks(
                            zoom(rotatedL, dp.zoom * dp.zoomL, rotatedR, dp.zoom * dp.zoomR, dp.offsetX + dw, dp.offsetY + dh, dp.imageResamplingModeL),
                            ms.measurementPointMark, transformL, dp.zoom * dp.zoomL, Math.max(0, dp.offsetX + dw), Math.max(0, dp.offsetY + dh)
                    ),
                    ms.right.drawMarks(
                            zoom(rotatedR, dp.zoom * dp.zoomR, rotatedL, dp.zoom * dp.zoomL, -dp.offsetX - dw, -dp.offsetY - dh, dp.imageResamplingModeR),
                            ms.measurementPointMark, transformR, dp.zoom * dp.zoomR, Math.max(0, -dp.offsetX - dw), Math.max(0, -dp.offsetY - dh)
                    )
            );
        }
    }
    public void updateViews(RawData rd, DisplayParameters dp, MeasurementStatus ms) {
        {
            {
                var bufferedImageList = processBothImages(rd, dp, ms, ColorCorrection.Command.SHOW);
                ImageIcon iconL = new ImageIcon(bufferedImageList.get(0));
                ImageIcon iconR = new ImageIcon(bufferedImageList.get(1));
                lblL.setIcon(iconL);
                lblR.setIcon(iconR);
            }

            lblL.setBorder(null);
            lblR.setBorder(null);
            lblL.setMargin(new Insets(0, 0, 0, 0));
            lblR.setMargin(new Insets(0, 0, 0, 0));
            {
                String leftFileName = FileLocations.getFileName(rd.left.path);
                String rightFileName = FileLocations.getFileName(rd.right.path);
                String leftTime = RoverTime.earthDateForFile(4, leftFileName);
                String rightTime = RoverTime.earthDateForFile(4, rightFileName);
                String times = "".equals(leftTime+rightTime)
                             ? ""
                             : leftTime.equals(rightTime)
                             ? " (" + leftTime + ")"
                             : " ("+leftTime+" : "+rightTime+")";
                String title = leftFileName + " : " + rightFileName + times;
                frame.setTitle(title);
            }
            {
                List<String> coloredPaths = StringDiffs.coloredUrls(Arrays.asList(rd.left.path, rd.right.path));
                urlL.setText(coloredPaths.get(0));
                urlR.setText(coloredPaths.get(1));
            }
            {
                colorCorrectionDescriptionL.setText(dp.lColorCorrection.getShortDescription());
                colorCorrectionDescriptionR.setText(dp.rColorCorrection.getShortDescription());
            }
        }
    }
    public void createViews(RawData rd, DisplayParameters dp, MeasurementStatus ms, UiEventListener uiEventListener)
    {
        lblL=new JButton();
        lblR=new JButton();
        frame=new JFrame();
        urlPanel = new JPanel(new GridBagLayout());
        urlL = new JLabel("url1");
        urlR = new JLabel("url2");
        colorCorrectionDescriptionL = new JLabel("....");
        colorCorrectionDescriptionR = new JLabel("....");
        colorCorrectionPane = new ColorCorrectionPane(uiEventListener);
        measurementPanel = new MeasurementPanel(uiEventListener);
        settingsPanel = new SettingsPanel(uiEventListener);
        {
            try {
                var mainIcon = ImageIO.read(ClassLoader.getSystemResource("icons/main64.png"));
                frame.setIconImage(mainIcon);
            } catch (IOException e) {
                // do nothing, it was just an icon
            }
        }
        {
            lblL.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    uiEventListener.markPointWithMousePress(false, e);
                }
            });
            lblR.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    uiEventListener.markPointWithMousePress(true, e);
                }
            });
        }
        {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.weightx = 0.5;
            urlPanel.add(urlL, gridBagConstraints);
        }
        {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.weightx = 0.5;
            urlPanel.add(colorCorrectionDescriptionL, gridBagConstraints);
        }
        {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.weightx = 0.5;
            urlPanel.add(urlR, gridBagConstraints);
        }
        {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.weightx = 0.5;
            urlPanel.add(colorCorrectionDescriptionR, gridBagConstraints);
        }
        {
            findAnyFont(Main.PREFERRED_FONTS).ifPresent(fontName -> {
                Font font = lblL.getFont();
                font = new Font(fontName, Font.BOLD, font.getSize());
                urlL.setFont(font);
                urlR.setFont(font);
            });
        }
        {
            findAnyFont(Main.PREFERRED_FONTS).ifPresent(fontName -> {
                Font fontCc = colorCorrectionDescriptionL.getFont();
                fontCc = new Font(fontName, Font.PLAIN, fontCc.getSize());
                colorCorrectionDescriptionL.setFont(fontCc);
                colorCorrectionDescriptionR.setFont(fontCc);
            });
        }
        {
            updateViews(rd,dp,ms);
        }

        var menuLR = new JPopupMenu();
        {
            {
                JMenuItem miCopy = new JMenuItem("Copy URL");
                menuLR.add(miCopy);
                miCopy.addActionListener(e ->
                        uiEventListener.copyUrl(
                                isFromComponentsMenu(e, lblR)
                        ));
            }
            {
                JMenuItem miCopyBoth = new JMenuItem("Copy Both URLs");
                menuLR.add(miCopyBoth);
                miCopyBoth.addActionListener(e ->
                        uiEventListener.copyUrls()
                );
            }
            {
                JMenuItem miPaste1 = new JMenuItem("Paste & Go (This Pane)");
                menuLR.add(miPaste1);
                miPaste1.addActionListener(e ->
                        doPaste(
                                uiEventListener,
                                isFromComponentsMenu(e, lblR),
                                OneOrBothPanes.JUST_THIS
                        ));
            }
            {
                JMenuItem miPaste2 = new JMenuItem("Paste & Go (Both Panes)");
                menuLR.add(miPaste2);
                miPaste2.addActionListener(e ->
                        doPaste(
                                uiEventListener,
                                isFromComponentsMenu(e, lblR),
                                OneOrBothPanes.BOTH_PANES
                        ));
            }
            {
                JMenuItem miAdjustOffsets = new JMenuItem("Adjust Offsets Using Measurement Red Marks");
                menuLR.add(miAdjustOffsets);
                miAdjustOffsets.addActionListener(e ->
                        uiEventListener.adjustOffsets()
                );
            }
            {
                String menuTitle = "Measurement...";
                JMenu mMeasure = new JMenu(menuTitle);
                menuLR.add(mMeasure);
                {
                    String title = "Show Marks";
                    showMeasurementCbMenuItem = new JCheckBoxMenuItem(title, dp.measurementShown);
                    mMeasure.add(showMeasurementCbMenuItem);
                    showMeasurementCbMenuItem.addActionListener(e ->
                            uiEventListener.measurementShownChanged(showMeasurementCbMenuItem.getState())
                    );
                }
                {
                    String title = "Mark First Point (Red)";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.setWaitingForPoint(1)
                    );
                }
                {
                    String title = "Mark Second Point (Green)";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.setWaitingForPoint(2)
                    );
                }
                {
                    String title = "Mark Far Calibration Point (Blue)";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.setWaitingForPoint(3)
                    );
                }
                {
                    String title = "Mark Left Calibration Point (Cyan)";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.setWaitingForPoint(4)
                    );
                }
                {
                    String title = "Mark Right Calibration Point (Magenta)";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.setWaitingForPoint(5)
                    );
                }
                {
                    String title = "Show Measurement Panel...";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            measurementPanel.showDialogIn(frame)
                    );
                }
                {
                    String title = "Measurement Help";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    HyperTextPane helpText = new HyperTextPane(
                        "<html>" +
                        "<h1>Measuring Distances</h1>" +
                        "<ol>" +
                        "<li>Mark the first point on the right and left images. Select the \"Mark First Point (Red)\" menu item,<br>" +
                            "the mouse cursor will become a cross, click the mouse on the first point. A red mark will appear,<br>" +
                            "and the cursor will return to the normal shape. Then, select again the \"Mark First Point (Red)\" menu item,<br>" +
                            "and mark the first point on the other panel. It's better to place the mark on some item (like a dark spot)<br>" +
                            "visible on both images the form the stereo pair.<br>" +
                            "The shape of the mark may be changed on the Measurement Panel (see the \"Show Measurement Panel...\" menu item)." +
                        "</li>" +
                            "<li>Mark the second point on the right and left images.<br>" +
                                "You will have to use the \"Mark Second Point (Green)\" menu item, the rest is like above." +
                            "</li>" +
                            "<li>Select the \"Show Measurement Panel...\" menu item.<br>" +
                                "You will see controls that let you change the mark coordinates, mark shape, and the type of camera.<br>" +
                                "For precise mark positioning, it may be recommended to use zoom=4, the \"Nearest Neighbor\" interpolation,<br>" +
                                "and a single-dot mark." +
                            "</li>" +
                            "<li>The text area will show the results of calculations. When you change any parameter, these results will " +
                                "be recalculated, but just in case there is a \"Calculate\" button." +
                            "</li>" +
                        "</ol>" +
                            "<b>Do not forget to select the right camera type!</b> NavCam and HazCam give different results for the same set of marks.<br>" +
                            "<br>" +
                            "It is recommended to set image scaling interpolation to \"NEAREST (neighbor)\", the one that<br>" +
                            "transforms pixels into squares. For distance measurement, it is important to see what the pixels are.<br>" +
                            "<br>" +
                            "There is angle correction, \"delta\". Without that, for example Rear HazCam B gives negative distances for objects at the horizon.<br>" +
                            "Just in case, the chooser includes <i>\"HazCam .167\"</i> and <i>\"HazCam .100\"</i> that have no correction, the digits are the stereo base in m.<br>" +
                            "<br>" +
                            "Keyboard Shortcuts<br>" +
                            "<b>Alt R</b> set the 1st (red) mark<br>" +
                            "<b>Alt G</b> set the 2nd (green) mark<br>" +
                            "<b>Alt H</b> set the 3rd (blue) mark<br>" +
                            "<b>Alt T</b> show distance measurement panel<br>" +
                            "<br>" +
                            "It's not really about measurement, but it is possible to use the red (1st) marks to set both offsets at once (horizontal and vertical).<br>" +
                            "Mark the same object in both panes (Alt R, click the mouse on some object in the left pane, Alt R, click mouse on the same object in the right pane),<br>" +
                            "select \"Adjust Offsets Using Measurement Red Marks\" in the menu, then hide the measurement marks in the menu.<br>" +
                            "Then, use the offsetX and offsetY controls to further adjust offsets with one-pixel precision: press Shift and click on the + or - button to adjust by 1 pixel,<br>" +
                            "do not press Shift to adjust offset by 3 pixels.<br>" +
                            "<br>" +
                        "</html>");
                    mi.addActionListener(e ->
                        JOptionPane.showMessageDialog(frame, helpText, "help", JOptionPane.PLAIN_MESSAGE)
                    );
                    {
                        Font f = helpText.getFont();
                        String FONT_NAME = "Verdana";
                        var fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
                        var exists = fonts.contains(FONT_NAME);
                        if (exists) {
                            f = new Font(FONT_NAME, Font.PLAIN, f.getSize());
                            helpText.setFont(f);
                        }
                    }
                }
                {
                    String title = "Clear All Marks";
                    JMenuItem mi = new JMenuItem(title);
                    mMeasure.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.clearAllMarks()
                    );
                }
            }
            {
                JMenuItem miLoadMatch = new JMenuItem("Load Match of Other");
                menuLR.add(miLoadMatch);
                miLoadMatch.addActionListener(e ->
                        uiEventListener.loadMatchOfOther(
                                isFromComponentsMenu(e, lblR)
                        ));
            }
            {
                JMenuItem miLoadMatch = new JMenuItem("Load Copy of Other");
                menuLR.add(miLoadMatch);
                miLoadMatch.addActionListener(e ->
                        uiEventListener.loadCopyOfOther(
                                isFromComponentsMenu(e, lblR)
                        ));
            }
            {
                JMenuItem miNewEmptyWindow = new JMenuItem("New Empty Window");
                menuLR.add(miNewEmptyWindow);
                miNewEmptyWindow.addActionListener(e ->
                                uiEventListener.newWindow()
                        );
            }
            {
                JMenuItem miColors = new JMenuItem("Color Correction...");
                menuLR.add(miColors);
                miColors.addActionListener(e ->
                        colorCorrectionPane.showDialogIn(frame)
                );
            }
            {
                JMenuItem miPrevImage = new JMenuItem("Go To Previous");
                menuLR.add(miPrevImage);
                miPrevImage.addActionListener(e ->
                        uiEventListener.navigate(
                                isFromComponentsMenu(e, lblR),
                                isFromComponentsMenu(e, lblL),
                                false,
                                1
                        )
                );
            }
            {
                JMenuItem miNextImage = new JMenuItem("Go To Next");
                menuLR.add(miNextImage);
                miNextImage.addActionListener(e ->
                        uiEventListener.navigate(
                                isFromComponentsMenu(e, lblR),
                                isFromComponentsMenu(e, lblL),
                                true,
                                1
                        )
                );
            }
            {
                JMenuItem miSwapUrls = new JMenuItem("Swap Image URLs, Keep Display Parameters");
                miSwapUrls.setToolTipText("Swap URLs but keep display parameters, this is different from just \"Swap Left And Right\"");
                menuLR.add(miSwapUrls);
                miSwapUrls.addActionListener(e ->
                        uiEventListener.swapImageUrls()
                );
            }
            {
                String title = "Go To First Image Taken on Curiosity Sol...";
                JMenuItem miGoTo = new JMenuItem(title);
                menuLR.add(miGoTo);
                miGoTo.addActionListener(e ->
                        {
                            boolean isRight = isFromComponentsMenu(e, lblR);
                            uiEventListener.gotoImage(
                                    GoToImageOptions.CURIOSITY_FIRST_OF_SOL,
                                    isRight,
                                    askForNumber(uiEventListener.getSol(isRight, WhichRover.CURIOSITY).orElse(100), title  )
                            );
                        }
                );
            }
            {
                String title = "Go To First Image Taken on Perseverance Sol...";
                JMenuItem miGoTo = new JMenuItem(title);
                menuLR.add(miGoTo);
                miGoTo.addActionListener(e ->
                        {
                            boolean isRight = isFromComponentsMenu(e, lblR);
                            uiEventListener.gotoImage(
                                    GoToImageOptions.PERSEVERANCE_FIRST_OF_SOL,
                                    isRight,
                                    askForNumber(uiEventListener.getSol(isRight, WhichRover.PERSEVERANCE).orElse(70), title  )
                            );
                        }
                );
            }
            {
                String title = "Go To The Latest Curiosity Image";
                JMenuItem miGoTo = new JMenuItem(title);
                menuLR.add(miGoTo);
                miGoTo.addActionListener(e ->
                        uiEventListener.gotoImage(
                                GoToImageOptions.CURIOSITY_LATEST,
                                isFromComponentsMenu(e, lblR),
                                Optional.empty() // unused
                        )
                );
            }
            {
                String title = "Go To The Latest Perseverance Image";
                JMenuItem miGoTo = new JMenuItem(title);
                menuLR.add(miGoTo);
                miGoTo.addActionListener(e ->
                        uiEventListener.gotoImage(
                                GoToImageOptions.PERSEVERANCE_LATEST,
                                isFromComponentsMenu(e, lblR),
                                Optional.empty() // unused
                        )
                );
            }
            {
                String menuTitle = "Curiosity: Open In Browser...";
                JMenu mOpen = new JMenu(menuTitle);
                menuLR.add(mOpen);
                {
                    String title = "All Images from Current Sol (on one page)";
                    JMenuItem mi = new JMenuItem(title);
                    mOpen.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.openInBrowser(SiteOpenCommand.OPEN_CURRENT_SOL, isFromComponentsSubmenu(e, lblR), WhichRover.CURIOSITY)
                    );
                }
                {
                    String title = "Images Taken Later (100/page)";
                    JMenuItem mi = new JMenuItem(title);
                    mOpen.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.openInBrowser(SiteOpenCommand.OPEN_TAKEN_LATER, isFromComponentsSubmenu(e, lblR), WhichRover.CURIOSITY)
                    );
                }
                {
                    String title = "Images Taken Earlier (100/page)";
                    JMenuItem mi = new JMenuItem(title);
                    mOpen.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.openInBrowser(SiteOpenCommand.OPEN_TAKEN_EARLIER, isFromComponentsSubmenu(e, lblR), WhichRover.CURIOSITY)
                    );
                }
                {
                    String title = "Latest Images";
                    JMenuItem mi = new JMenuItem(title);
                    mOpen.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.openInBrowser(SiteOpenCommand.OPEN_LATEST, false, WhichRover.CURIOSITY)
                    );
                }
            }
            {
                String menuTitle = "Perseverance: Open In Browser...";
                JMenu mOpen = new JMenu(menuTitle);
                menuLR.add(mOpen);
                {
                    String title = "Images from Current Sol";
                    JMenuItem mi = new JMenuItem(title);
                    mOpen.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.openInBrowser(SiteOpenCommand.OPEN_CURRENT_SOL, isFromComponentsSubmenu(e, lblR), WhichRover.PERSEVERANCE)
                    );
                }
                {
                    String title = "Latest Images";
                    JMenuItem mi = new JMenuItem(title);
                    mOpen.add(mi);
                    mi.addActionListener(e ->
                            uiEventListener.openInBrowser(SiteOpenCommand.OPEN_LATEST, false, WhichRover.PERSEVERANCE)
                    );
                }
            }
            {
                JMenuItem miReload = new JMenuItem("Reload");
                menuLR.add(miReload);
                miReload.addActionListener(e ->
                        uiEventListener.reload(
                                isFromComponentsMenu(e, lblR)
                        ));
            }
            lblR.setComponentPopupMenu(menuLR);
            lblL.setComponentPopupMenu(menuLR);
        }
        componentL = new JScrollPane(lblL);
        componentR = new JScrollPane(lblR);
        {
            componentL.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            componentR.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            componentL.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            componentR.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            // synchronize them: make them share the same model
            componentL.getHorizontalScrollBar().setModel(componentR.getHorizontalScrollBar().getModel());
            componentL.getVerticalScrollBar().setModel(componentR.getVerticalScrollBar().getModel());
            new DragMover(lblL);
            new DragMover(lblR);
        }

        GridBagLayout gbl = new GridBagLayout();
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbl.setConstraints(componentL, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbl.setConstraints(componentR, gbc);
        }

        JPanel statusPanel = new JPanel();
        JPanel statusPanel2 = new JPanel();
        {
            FlowLayout fl = new FlowLayout();
            statusPanel.setLayout(fl);

            {
                JButton saveButton = new JButton();
                DigitalZoomControl.loadIcon(saveButton,"icons/save12.png","⤓"); //
                saveButton.addActionListener(e -> uiEventListener.saveScreenshot());
                saveButton.setToolTipText("Save Screenshot");
                statusPanel.add(saveButton);
            }

            {
                JButton resetAllControlsButton = new JButton();
                DigitalZoomControl.loadIcon(resetAllControlsButton,"icons/clearAll25.png","xx"); // "<->" "<=>"
                resetAllControlsButton.addActionListener(e -> uiEventListener.resetToDefaults());
                resetAllControlsButton.setToolTipText("Reset All Controls (MR/ML pair: special case)");
                statusPanel.add(resetAllControlsButton);
            }

            {
                JButton settingsButton = new JButton();
                DigitalZoomControl.loadIcon(settingsButton,"icons/gear12.png","⚙"); //
                settingsButton.addActionListener(e -> settingsPanel.showDialogIn(frame));
                settingsButton.setToolTipText("Settings");
                statusPanel2.add(settingsButton);
            }

            statusPanel.add(dcZoom = new  DigitalZoomControl<Double, ZoomFactorWrapper>().init("zoom:",4, new ZoomFactorWrapper(), d -> uiEventListener.zoomChanged(d)));
            statusPanel.add(dcZoomL = new DigitalZoomControl<Double, ZoomFactorWrapper>().init("zoomL:",4, new ZoomFactorWrapper(), d -> uiEventListener.lZoomChanged(d)));
            statusPanel.add(dcZoomR = new DigitalZoomControl<Double, ZoomFactorWrapper>().init("zoomR:",4, new ZoomFactorWrapper(), d -> uiEventListener.rZoomChanged(d)));

            statusPanel.add(dcOffX = new DigitalZoomControl<Integer, OffsetWrapper>().init("offsetX:", 4, new OffsetWrapper(), i -> uiEventListener.xOffsetChanged(i)));
            statusPanel.add(dcOffY = new DigitalZoomControl<Integer, OffsetWrapper>().init("offsetY:", 4, new OffsetWrapper(), i -> uiEventListener.yOffsetChanged(i)));

            statusPanel2.add(dcAngle = new DigitalZoomControl<Double, RotationAngleWrapper>().init("rotate:",4, new RotationAngleWrapper(), d -> uiEventListener.angleChanged(d)));
            statusPanel2.add(dcAngleL = new DigitalZoomControl<Double, RotationAngleWrapper>().init("rotateL:",4, new RotationAngleWrapper(), d -> uiEventListener.lAngleChanged(d)));
            statusPanel2.add(dcAngleR = new DigitalZoomControl<Double, RotationAngleWrapper>().init("rotateR:",4, new RotationAngleWrapper(), d -> uiEventListener.rAngleChanged(d)));

            statusPanel2.add(new JLabel("Decode Color:"));
            statusPanel2.add(debayerL = new DebayerModeChooser(v -> uiEventListener.lDebayerModeChanged(v)));
            statusPanel2.add(debayerR = new DebayerModeChooser(v -> uiEventListener.rDebayerModeChanged(v)));

            {
                //statusPanel.add(new JLabel(" "));
                //statusPanel.add(new JLabel(" RL/LR:"));
                JButton swapButton = new JButton();
                DigitalZoomControl.loadIcon(swapButton,"icons/swap12.png","⇄"); // "<->" "<=>"
                swapButton.addActionListener(e -> uiEventListener.swapImages());
                swapButton.setToolTipText("Swap Left and Right");
                statusPanel.add(swapButton);
            }
            {
                helpButton = new JButton();
                DigitalZoomControl.loadIcon(helpButton,"icons/helpc12.png","?");
                HyperTextPane helpText = new HyperTextPane(
                        "<html>" +
                        "<h1>Curious: X3D Viewer</h1>" +
                        "Use Drag-and-Drop or the right mouse click menu to open a file or URL.<br>" +
                        "For example, you may Drag-and-Drop raw image thumbnails from "+
                        "<a href=\""+ Main.CURIOSITY_RAW_IMAGES_URL +"\">"+
                                "Curiosity" +
                        "</a>" +
                        " or " +
                        "<a href=\""+ Main.PERSEVERANCE_RAW_IMAGES_URL +"\">"+
                        "Perseverance" +
                        "</a>" +
                        " pages.<br>" +
                        "<br>" +
                        "When either of the images has the input focus: <br>" +
                        "<b>LEFT</b>, <b>RIGHT</b>, <b>UP</b>, <b>DOWN</b>: scroll both images<br>" +
                        "<b>Ctrl =</b>, <b>Alt I</b>: zoom in +10%<br>" +
                        "<b>Ctrl Shift +</b>, <b>Ctrl I</b>: zoom in +100%<br>" +
                        "<b>Clrl -</b>, <b>Alt O</b>: zoom out -10%<br>" +
                        "<b>Ctrl Shift _</b>, <b>Ctrl O</b>: zoom out -100%<br>" +
                        "<b>Shift LEFT</b>, <b>Shift RIGHT</b>: change horizontal offset by 3<br>" +
                        "<b>Ctrl LEFT</b>, <b>Ctrl RIGHT</b>: change horizontal offset by 30<br>" +
                        "<b>Shift UP</b>, <b>Shift DOWN</b>: change vertical offset by 3<br>" +
                        "<b>Ctrl UP</b>, <b>Ctrl DOWN</b>: change vertical offset by 30<br>" +
                        "Note: to set both vertical and horizontal offsets, mark the same object with <b>Alt R</b> in the left and right panes, and then<br>" +
                        "select \"Adjust Offsets...\" in the menu. Then, use the menu to hide the measurement marks.<br>" +
                        "<b>Alt B</b>: Toggle the \"drag-and-drop to both panes\" mode<br>" +
                        "<b>Ctrl U</b>: Swap the left and right images<br>" +
                        "<b>Alt R/Alt G/Alt T</b>: Set the red/green marks, show <b>distance measurement</b> panel; more options in the context menu<br>" +
//                        "<br>"+
                        "<b>Ctrl N</b>: New (empty) window<br>" +
                        "<b>Ctrl S</b>: Save the stereo pair (saves a screenshot of this application plus <br>" +
                        "a file that ends with <i>.source</i> and contains the URLs of images in the stereo pair)<br>" +
                        "Note: it may be confusing but if you prefer the keyboard to the mouse,<br>" +
                        "in Java dialogs the Enter key selects the default action rather than<br>" +
                        "the currently selected one; please use Space to select the current action.<br>" +
                        "<b>F1</b>: this help<br>" +
                        "<b>Buttons + and -</b> in the zoom/offset/rotate controls may be pressed with the <b>Shift</b> key.<br>"+
                        "<br>"+
                        "<b>Command line:</b> arguments may be either file paths or URLs<br>" +
                        "<b>Drag-and-Drop (DnD):</b> opens one or two images; if only one image was dropped, if the \"DnD BOTH\" <br>" +
                        "checkbox is checked, tries to also load the corresponding right or left image.<br>" +
                        "If the \"DnD BOTH\" box is not checked, the dropped image/url<br>" +
                        "just replaces the image on which it was dropped.<br>" +
                        "<br>"+
                        "<b>Navigation.</b> On the NASA site, there is a list of all images in chronological order.<br>" +
                        "Each pane has its own pointer to the current image. In most cases, you will want the right and left<br>" +
                        "panes to hold images that go one next to the other, but this is not necessary, any distance between the two<br>" +
                        "images is possible. Use the navigation buttons to replace images in both panes, use the context menu<br>" +
                        "to replace only one image.<br>"+
                        "<br>"+
                        "<b>Color correction.</b> <a href=\"https://homepages.inf.ed.ac.uk/rbf/HIPR2/stretch.htm\">Contrast stretching</a>" +
                        " and <a href=\"https://en.wikipedia.org/wiki/Gamma_correction\">gamma correction</a> are implemented (available via<br>" +
                        "the context menu or via the color button). The recommended value for gamma correction is γ=2.2.<br>"+
                        "<br>"+
                        "This project's " +
                        "<a href=\"https://github.com/martianch/curieux\">"+
                        "home page" +
                        "</a>" +
                        ", " +
                        "<a href=\"https://github.com/martianch/curieux/releases\">"+
                        "download page" +
                        "</a>" +
                        "." +
                        "<br>"+
                        "Learn X3D: " +
                        "<a href=\"https://corgegrault.blogspot.com/2020/02/what-is-x3d-and-how-to-view-it-just-on.html\">"+
                        "blog post" +
                        "</a>" +
                        ", " +
                        "<a href=\"https://www.youtube.com/watch?v=hxTMlDijDlU\">"+
                        "video" +
                        "</a>" +
                        ", " +
                        "<a href=\"https://www.whatsupinthesky.com/images/dgannett/3D_TUTORIALS/X3D.jpg\">"+
                        "image with text" +
                        "</a>" +
                        " (these are three completely different methods)." +
                        "<br><br>Build Version: " + BuildVersion.getBuildVersion() +
                        "");
                {
                    Font f = helpText.getFont();
                    String FONT_NAME = "Verdana";
                    var fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
                    var exists = fonts.contains(FONT_NAME);
                    if (exists) {
                        f = new Font(FONT_NAME, Font.PLAIN, f.getSize());
                        helpText.setFont(f);
                    }
                }
                helpButton.addActionListener(e -> {
                    JOptionPane.showMessageDialog(frame, helpText,
                            "help", JOptionPane.PLAIN_MESSAGE);
                });
                statusPanel.add(helpButton);
            }
            {
                JButton bButton = new JButton();
                DigitalZoomControl.loadIcon(bButton,"icons/twoearlier24.png","««"); // "<->" "<=>" "icons/swap12.png" « ‹ › » ⇉ ⇇ ↠ ↞ ↢ ↣
                bButton.addActionListener(e -> uiEventListener.navigate(true, true, false, 2));
                bButton.setToolTipText("go two images earlier in each pane");
                statusPanel.add(bButton);
            }
            {
                JButton bButton = new JButton();
                DigitalZoomControl.loadIcon(bButton,"icons/oneearlier24.png","‹‹"); // "<->" "<=>" "icons/swap12.png" « ‹ › » ⇉ ⇇ ↠ ↞ ↢ ↣
                bButton.addActionListener(e -> uiEventListener.navigate(true, true, false, 1));
                bButton.setToolTipText("go one image earlier in each pane");
                statusPanel.add(bButton);
            }
            {
                JButton fButton = new JButton();
                DigitalZoomControl.loadIcon(fButton,"icons/onelater24.png","››"); // "<->" "<=>" "icons/swap12.png" « ‹ › » ⇉ ⇇ ↠ ↞ ↢ ↣
                fButton.addActionListener(e -> uiEventListener.navigate(true, true, true, 1));
                fButton.setToolTipText("go one image later in each pane");
                statusPanel.add(fButton);
            }
            {
                JButton ffButton = new JButton();
                DigitalZoomControl.loadIcon(ffButton,"icons/twolater24.png","»»"); // "<->" "<=>" "icons/swap12.png" « ‹ › » ⇉ ⇇ ↠ ↞ ↢ ↣
                ffButton.addActionListener(e -> uiEventListener.navigate(true, true, true, 2));
                ffButton.setToolTipText("go two images later in each pane");
                statusPanel.add(ffButton);
            }
            {
                JButton colorButton = new JButton();
                DigitalZoomControl.loadIcon(colorButton,"icons/colors24.png","color");
                colorButton.setToolTipText("color correction...");
                colorButton.addActionListener(e -> {
                    colorCorrectionPane.showDialogIn(frame);
                });
                statusPanel2.add(colorButton);
            }
            {
                JCheckBox dndToBothCheckox = new JCheckBox("DnD to Both");
                dndToBothCheckox.setMnemonic(KeyEvent.VK_B);
                dndToBothCheckox.setSelected(true);
                dndToBothCheckox.addActionListener(
                    e -> uiEventListener.dndSingleToBothChanged(dndToBothCheckox.isSelected())
                );
                statusPanel2.add(dndToBothCheckox);
            }
        }

        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridheight = 1;
            gbc.gridwidth = 2;
            gbl.setConstraints(statusPanel, gbc);
        }

        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridheight = 1;
            gbc.gridwidth = 2;
            gbl.setConstraints(statusPanel2, gbc);
        }

        {
            frame.setLayout(gbl);
            frame.setSize(1366,800);
            frame.add(componentR);
            frame.add(componentL);
            frame.add(statusPanel);
            frame.add(statusPanel2);
        }
        {
            // keyboard shortcuts
            InputMap frameInputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap frameActionMap = frame.getRootPane().getActionMap();
            frameInputMap.put(KeyStroke.getKeyStroke("shift LEFT"), "xoffplus");
            frameInputMap.put(KeyStroke.getKeyStroke("shift RIGHT"), "xoffminus");
            frameInputMap.put(KeyStroke.getKeyStroke("shift UP"), "yoffplus");
            frameInputMap.put(KeyStroke.getKeyStroke("shift DOWN"), "yoffminus");
            frameActionMap.put("xoffplus", toAction(e->dcOffX.buttonPlus.doClick()));
            frameActionMap.put("xoffminus", toAction(e->dcOffX.buttonMinus.doClick()));
            frameActionMap.put("yoffplus", toAction(e->dcOffY.buttonPlus.doClick()));
            frameActionMap.put("yoffminus", toAction(e->dcOffY.buttonMinus.doClick()));
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl LEFT"), "xoffplus2");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl RIGHT"), "xoffminus2");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl UP"), "yoffplus2");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl DOWN"), "yoffminus2");
            frameActionMap.put("xoffplus2", toAction(e->dcOffX.buttonPlus2.doClick()));
            frameActionMap.put("xoffminus2", toAction(e->dcOffX.buttonMinus2.doClick()));
            frameActionMap.put("yoffplus2", toAction(e->dcOffY.buttonPlus2.doClick()));
            frameActionMap.put("yoffminus2", toAction(e->dcOffY.buttonMinus2.doClick()));
            frameInputMap.put(KeyStroke.getKeyStroke("alt I"), "zoomin");
            frameInputMap.put(KeyStroke.getKeyStroke("alt O"), "zoomout");
            frameInputMap.put(KeyStroke.getKeyStroke("altGraph I"), "zoomin");
            frameInputMap.put(KeyStroke.getKeyStroke("altGraph O"), "zoomout");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl I"), "zoomin2");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl O"), "zoomout2");
//            frameInputMap.put(KeyStroke.getKeyStroke("ctrl PLUS"), "zoomin2");
//            frameInputMap.put(KeyStroke.getKeyStroke("ctrl shift PLUS"), "zoomin2");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl EQUALS"), "zoomin");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl shift EQUALS"), "zoomin2");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl MINUS"), "zoomout");
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl shift MINUS"), "zoomout2");
            frameActionMap.put("zoomin", toAction(e->dcZoom.buttonPlus.doClick()));
            frameActionMap.put("zoomout", toAction(e->dcZoom.buttonMinus.doClick()));
            frameActionMap.put("zoomin2", toAction(e->dcZoom.buttonPlus2.doClick()));
            frameActionMap.put("zoomout2", toAction(e->dcZoom.buttonMinus2.doClick()));
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl N"), "newWindow");
            frameActionMap.put("newWindow", toAction(e->uiEventListener.newWindow()));
            frameInputMap.put(KeyStroke.getKeyStroke("F1"), "help");
            frameActionMap.put("help", toAction(e->helpButton.doClick()));
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl U"), "swapLeftRight");
            frameActionMap.put("swapLeftRight", toAction(e -> uiEventListener.swapImages()));
            frameInputMap.put(KeyStroke.getKeyStroke("ctrl S"), "saveScreenshot");
            frameActionMap.put("saveScreenshot", toAction(e->uiEventListener.saveScreenshot()));
            frameInputMap.put(KeyStroke.getKeyStroke("alt R"), "markred");
            frameInputMap.put(KeyStroke.getKeyStroke("alt G"), "markgreen");
            frameInputMap.put(KeyStroke.getKeyStroke("alt H"), "markblue");
            frameInputMap.put(KeyStroke.getKeyStroke("alt T"), "measurementpanel");
            frameActionMap.put("markred", toAction(e->uiEventListener.setWaitingForPoint(1)));
            frameActionMap.put("markgreen", toAction(e->uiEventListener.setWaitingForPoint(2)));
            frameActionMap.put("markblue", toAction(e->uiEventListener.setWaitingForPoint(3)));
            frameActionMap.put("measurementpanel", toAction(e->measurementPanel.showDialogIn(frame)));
            frameInputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
            frameActionMap.put("escape", toAction(e->uiEventListener.escapePressed()));
        }
        {
            TransferHandler transferHandler = new TransferHandler("text") {
                @Override
                public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                    return doCanImport(transferFlavors);
                }

                @Override
                public boolean importData(JComponent comp, Transferable t) {
                    return doImportData(comp == lblR, OneOrBothPanes.SEE_CHECKBOX, t, uiEventListener);
                }
            };
            lblL.setTransferHandler(transferHandler);
            lblR.setTransferHandler(transferHandler);
        }
        if (settingsPanel.showUrlsCheckbox.isSelected()) {
            addUrlViews(true, false);
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    static boolean isFromComponentsMenu(ActionEvent e, JComponent lblR) {
        return lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker();
    }

    static boolean isFromComponentsSubmenu(ActionEvent e, JComponent lblR) {
        return lblR == ((JPopupMenu) ((JMenuItem) ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker()).getParent()).getInvoker();
    }

    void addUrlViews(boolean visible, boolean repaint) {
        GridBagLayout gbl = (GridBagLayout) frame.getContentPane().getLayout();
        if (visible) {
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.gridheight = 1;
                gbc.gridwidth = 2;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbl.setConstraints(urlPanel, gbc);
            }
            frame.add(urlPanel);
        } else {
            frame.remove(urlPanel);
        }
        if (repaint) {
            frame.validate();
            frame.repaint();
        }
    }
    static Optional<String> findAnyFont(String... fontNames) {
        var fl = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        var fontName = Arrays.stream(fontNames).filter(fl::contains).findFirst();
        return fontName;
    }
    static Optional<Integer> askForNumber(int startWith, String title) {
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(
                Integer.valueOf(startWith), // value
                Integer.valueOf(0), // min
                null, // max
                Integer.valueOf(1) // step
        );
        JSpinner jSpinner = new JSpinner(spinnerNumberModel);

        JFormattedTextField jFormattedTextField = ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField();
        {
            NumberFormat numberFormat = new DecimalFormat("#");
            NumberFormatter numberFormatter = new NumberFormatter(numberFormat);
            numberFormatter.setValueClass(Integer.class);
            numberFormatter.setMinimum(0);
            numberFormatter.setMaximum(Integer.MAX_VALUE);
            numberFormatter.setAllowsInvalid(false);
            DefaultFormatterFactory myFormatterFactory = new DefaultFormatterFactory(numberFormatter);
            jFormattedTextField.setFormatterFactory(myFormatterFactory);
        }

        int result = JOptionPane.showConfirmDialog(null, jSpinner, title, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            return Optional.of((Integer)jSpinner.getValue());
        } else {
            return Optional.empty();
        }
    }
    public boolean doPaste(UiEventListener uiEventListener, boolean isRight, OneOrBothPanes oneOrBoth) {
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        var flavors = clipboard.getAvailableDataFlavors();
        var transferable = clipboard.getContents(null);
        if (doCanImport(flavors)) {
            return doImportData(isRight, oneOrBoth, transferable, uiEventListener);
        }
        return false;
    }
    public boolean doCanImport(DataFlavor[] transferFlavors) {
        for (DataFlavor df : transferFlavors) {
            if (df.isFlavorTextType() || df.isFlavorJavaFileListType()) {
                System.out.println("can accept: "+df);
                return true;
            }
        }
        System.out.println("{ canImport");
        for (DataFlavor df : transferFlavors) {
            System.out.println(df);
        }
        System.out.println("} canImport");
        return false;
    }
    public boolean doImportData(boolean isRight, OneOrBothPanes oneOrBoth, Transferable t, UiEventListener uiEventListener) {
        System.out.println("importData(\n"+isRight+",\n"+oneOrBoth+",\n"+t+"\n)");
        try {
            String toImport
                    = t.isDataFlavorSupported(DataFlavor.stringFlavor)
                    ? (String) t.getTransferData(DataFlavor.stringFlavor)
                    : String.join(
                    "\n",
                    ((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor))
                            .stream()
                            .map(x->x.toString())
                            .collect(Collectors.toList())
            );
            uiEventListener.dndImport(toImport, isRight, oneOrBoth);
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("DnD transfer failed");
            System.out.println("{ transfer offered in flavors");
            for (DataFlavor df : t.getTransferDataFlavors()) {
                System.out.println(df);
            }
            System.out.println("} transfer offered in flavors");
            return false;
        }
        return true;
    }
    static Action toAction(Consumer<ActionEvent> lambda) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lambda.accept(e);
            }
        };
    }

    static BufferedImage rotate(BufferedImage originalImage, AffineTransform transform) {
        if (ImageAndPath.isDummyImage(originalImage)) {
            return originalImage;
        }
        BufferedImageOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
        return operation.filter(originalImage, null);
    }
    static AffineTransform rotationTransform(BufferedImage originalImage, double alphaDegrees) {
        double alpha = Math.toRadians(alphaDegrees);
        double sinA = Math.sin(alpha);
        double cosA = Math.cos(alpha);
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int newWidth = (int) Math.round(Math.abs(cosA*width) + Math.abs(sinA*height));
        int newHeight = (int) Math.round(Math.abs(cosA*height) + Math.abs(sinA*width));

        AffineTransform transform = new AffineTransform();
        transform.translate(newWidth / 2.0, newHeight / 2.0);
        transform.rotate(alpha);
        transform.translate(-width / 2.0, -height / 2.0);

        return transform;
    }
    static BufferedImage zoom(BufferedImage originalImage, double zoomLevel, BufferedImage otherImage, double otherZoomLevel, int offX, int offY, ImageResamplingMode imageResamplingMode) {
        if (ImageAndPath.isDummyImage(originalImage) && ImageAndPath.isDummyImage(otherImage)) {
            return originalImage;
        }
        int newImageWidth = zoomedSize(originalImage.getWidth(), zoomLevel);
        int newImageHeight = zoomedSize(originalImage.getHeight(), zoomLevel);
        int otherImageWidth = zoomedSize(otherImage.getWidth(), otherZoomLevel);
        int otherImageHeight = zoomedSize(otherImage.getHeight(), otherZoomLevel);
        int thisCanvasWidth = Math.max(0, mult(offX, zoomLevel)) + newImageWidth;
        int thisCanvasHeight = Math.max(0, mult(offY, zoomLevel)) + newImageHeight;
        int otherCanvasWidth = Math.max(0, mult(-offX, otherZoomLevel)) + otherImageWidth;
        int otherCanvasHeight = Math.max(0, mult(-offY, otherZoomLevel)) + otherImageHeight;
        int canvasWidth = Math.max(thisCanvasWidth, otherCanvasWidth);
        int canvasHeight = Math.max(thisCanvasHeight, otherCanvasHeight);
        int xToDrawFrom = Math.max(0, mult(offX, zoomLevel));
        int yToDrawFrom = Math.max(0, mult(offY, zoomLevel));
//        int otherXToDrawFrom = mult(-offX, zoomLevel);
//        int otherYToDrawFrom = mult(-offY, zoomLevel);
        BufferedImage resizedImage = new BufferedImage(canvasWidth, canvasHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
//        if (zoomLevel > 1.5) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, imageResamplingMode.getRenderingHint());
//        }
        g.drawImage(
                originalImage,
                xToDrawFrom, // x
                yToDrawFrom, // y
                newImageWidth,  // width
                newImageHeight, // height
                null
        );
        g.dispose();
        return resizedImage;
    }

    static int zoomedSize(int orig, double zoom) {
        return (int) Math.round(orig * zoom);
    }
    static int mult(int v, double zoom) {
        return (int)(v*zoom);
    }
    static int mult(double v, double zoom) {
        return (int)(v*zoom);
    }

}

enum OneOrBothPanes {JUST_THIS, BOTH_PANES, SEE_CHECKBOX};

enum ImageResamplingMode {
    NEAREST(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR),
    BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR),
    BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    final Object renderingHint;
    ImageResamplingMode(Object hint) {
        renderingHint = hint;
    }
    public Object getRenderingHint() {
        return renderingHint;
    }
    static ImageResamplingMode getUiDefault(){
        return BICUBIC;
    }
};
class ImageResamplingModeChooser extends JComboBox<ImageResamplingMode> {
    static ImageResamplingMode[] modes = ImageResamplingMode.values();
    public ImageResamplingModeChooser(Consumer<ImageResamplingMode> valueListener) {
        super(modes);
        setValue(ImageResamplingMode.getUiDefault());
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((ImageResamplingMode) itemEvent.getItem());
            }
        });
    }
    public void setValue(ImageResamplingMode imageResamplingMode) {
        setSelectedItem(imageResamplingMode);
    }
}
enum DebayerMode {
    NEVER(false,-1),
    AUTO0(false,0), AUTO1(false,1), AUTO2(false,2), AUTO3(false,3), AUTO4(false,4),
    FORCE0(true,0), FORCE1(true,1), FORCE2(true,2), FORCE3(true,3), FORCE4(true,4);
    boolean force;
    int algo;
    DebayerMode(boolean force, int algo) {
        this.force = force;
        this.algo = algo;
    }
    <T> T doAlgo(T data, Supplier<Boolean> autoCheck, List<Function<T,T>> algorithms) {
        return (algo >= 0 && (force || autoCheck.get()))
                ? algorithms.get(algo).apply(data)
                : data
                ;
    }
    static DebayerMode getUiDefault(){
        return AUTO3;
    }
}
class DebayerModeChooser extends JComboBox<DebayerMode> {
    static DebayerMode[] modes = DebayerMode.values();
    public DebayerModeChooser(Consumer<DebayerMode> valueListener) {
        super(modes);
        setValue(DebayerMode.getUiDefault());
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((DebayerMode) itemEvent.getItem());
            }
        });
    }
    public void setValue(DebayerMode debayerMode) {
        setSelectedItem(debayerMode);
    }
}
class DigitalZoomControl<T, TT extends DigitalZoomControl.ValueWrapper<T>> extends JPanel {
    TT valueWrapper;
    JLabel label;
    JButton buttonMinus2;
    JButton buttonMinus;
    JButton buttonPlus;
    JButton buttonPlus2;
    JButton buttonDefault;
    JTextField textField;
    static final int GROUP_LENGTH = 2;
    DigitalZoomControl<T,TT> init(String labelText, int nColumns, TT valueWrapper0, Consumer<T> valueListener) {
        valueWrapper = valueWrapper0;

        FlowLayout fl = new FlowLayout();
        this.setLayout(fl);
        {
            label = new JLabel(labelText);
            this.add(label);
        }
        {
            buttonMinus2 = new JButton();
            loadIcon(buttonMinus2,"icons/minus12.png","––");
            buttonMinus2.addActionListener(e -> {
                valueWrapper.decrement(1 + getGroupIndex(e));
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            buttonMinus2.setToolTipText(valueWrapper.getButtonToolTip(-1, 1, 1+GROUP_LENGTH));
            this.add(buttonMinus2);
        }
        {
            buttonMinus = new JButton();
            loadIcon(buttonMinus,"icons/minusa12.png","–");
            buttonMinus.addActionListener(e -> {
                valueWrapper.decrement(0 + getGroupIndex(e));
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            buttonMinus.setToolTipText(valueWrapper.getButtonToolTip(-1, 0, 0+GROUP_LENGTH));
            this.add(buttonMinus);
        }
        {
            textField = new JTextField(valueWrapper.getAsString(),nColumns);
            textField.setMinimumSize(textField.getPreferredSize());
            textField.addActionListener(e -> {
                if (valueWrapper.setFromString(e.getActionCommand())) {
                    this.textField.setForeground(Color.BLACK);
                    valueListener.accept(valueWrapper.getSafeValue());
                } else {
                    this.textField.setForeground(Color.RED);
                }
            });
            this.add(textField);
        }
        {
            buttonPlus = new JButton();
            loadIcon(buttonPlus,"icons/plusa12.png","+");
            buttonPlus.addActionListener(e -> {
                valueWrapper.increment(0 + getGroupIndex(e));
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            buttonPlus.setToolTipText(valueWrapper.getButtonToolTip(+1, 0, 0+GROUP_LENGTH));
            this.add(buttonPlus);
        }
        {
            buttonPlus2 = new JButton();
            loadIcon(buttonPlus2,"icons/plus12.png","++");
            buttonPlus2.addActionListener(e -> {
                valueWrapper.increment(1 + getGroupIndex(e));
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            buttonPlus2.setToolTipText(valueWrapper.getButtonToolTip(+1, 1, 1+GROUP_LENGTH));
            this.add(buttonPlus2);
        }
        {
            buttonDefault = new JButton();
            loadIcon(buttonDefault,"icons/clear12.png","x");
            buttonDefault.addActionListener(e -> {
                valueWrapper.reset();
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            buttonDefault.setToolTipText("Reset to default");
            this.add(buttonDefault);
        }
        return this;
    }

    /**
     * Detect pressed modifier keys (only Shift at the moment).
     * When Shift is pressed, a different set of increment values is used.
     * @param e contains information about the modifiers (Shift/Ctrl/Alt/Meta)
     * @return 2 if Shift is pressed, 0 otherwise
     */
    int getGroupIndex(ActionEvent e) {
        if((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
            return GROUP_LENGTH;
        } else {
            return 0;
        }
    }
    DigitalZoomControl setTextFieldFromValue() {
        this.textField.setForeground(Color.BLACK);
        this.textField.setText(valueWrapper.getAsString());
        return this;
    }
    DigitalZoomControl setValueAndText(T v) {
        valueWrapper.setValue(v);
        setTextFieldFromValue();
        return this;
    }
    @Override
    public String toString() {
        return label.getText()+super.toString();
    }

    static void loadIcon(JButton button, String resourcePath, String altText) {
        button.setMargin(new Insets(0, 0, 0, 0));
        try {
            var icon = ImageIO.read(ClassLoader.getSystemResource(resourcePath)).getScaledInstance(12,12, SCALE_SMOOTH);
            button.setIcon(new ImageIcon(icon));
        } catch (Throwable e) {
            button.setText(altText);
        }
    }

    public abstract static class ValueWrapper<T> {
        T value;
        ValueWrapper() {
            reset();
        }
        abstract boolean setFromString(String s);
        abstract void increment(int incrementIndex);
        abstract void decrement(int decrementIndex);
        abstract void reset();
        abstract String getAsString();
        T getSafeValue() { return value; }
        ValueWrapper setValue(T v) { value = v; return this; }
        abstract String getButtonToolTip(int sign, int index1, int index2);
    }
}
class OffsetWrapper extends DigitalZoomControl.ValueWrapper<Integer> {
    int[] increments = {3,30,1,100};
    @Override
    boolean setFromString(String s) {
        try {
            value = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    void increment(int incrementIndex) {
        value += increments[incrementIndex];
    }

    @Override
    void decrement(int decrementIndex) {
        value -= increments[decrementIndex];
    }

    @Override
    void reset() {
        value = 0;
    }

    @Override
    String getAsString() {
        return value.toString();
    }

    @Override
    String getButtonToolTip(int sign, int index1, int index2) {
        return String.format("%+d, Shift: %+d", sign*increments[index1], sign*increments[index2]);
    }
}
class OffsetWrapper2 extends RotationAngleWrapper {
    {
        increments = new double[]{1,3,30,100};
    }
}
class RotationAngleWrapper extends DigitalZoomControl.ValueWrapper<Double> {
    double[] increments = {0.1, 1.0, 0.05, 5.0};
    @Override
    boolean setFromString(String s) {
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    @Override
    void increment(int incrementIndex) {
        value = value + increments[incrementIndex];
    }
    @Override
    void decrement(int decrementIndex) {
        value = value - increments[decrementIndex];
    }
    @Override
    void reset() {
        value = 0.;
    }
    @Override
    String getAsString() {
        return String.format ("%.3f", value);
    }
    @Override
    String getButtonToolTip(int sign, int index1, int index2) {
        DecimalFormat df = new DecimalFormat("+#.####;-#.####");
        return df.format(sign*increments[index1]) + ", Shift: " + df.format(sign*increments[index2]);
    }
}
class ZoomFactorWrapper extends DigitalZoomControl.ValueWrapper<Double> {
    double[] increments = {0.1, 1.0, 0.005, 2.0};
    final static double MIN_ZOOM_VALUE = 0.001;
    @Override
    boolean setFromString(String s) {
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    Double getSafeValue() {
        return Math.max(MIN_ZOOM_VALUE, super.getSafeValue());
    }

    @Override
    void increment(int incrementIndex) {
        value = x2zoom( zoom2x(value) + increments[incrementIndex] );
    }

    @Override
    void decrement(int decrementIndex) {
        value = x2zoom( zoom2x(value) - increments[decrementIndex] );
    }

    @Override
    void reset() {
        value = 1.0;
    }

    @Override
    String getAsString() {
        return String.format ("%.3f", value);
    }

    @Override
    String getButtonToolTip(int sign, int index1, int index2) {
        DecimalFormat df = new DecimalFormat("+#.####;-#.####");
        return df.format(sign*increments[index1]) + ", Shift: " + df.format(sign*increments[index2]);
    }

    /* zoom maths */
    final static double eps = 0.000001;
    final static int smallestPowOf2 = -61;
    final static double ln2 = Math.log(2.);
    static double log2(double x) { return Math.log(x)/ln2; }
    static double x2zoom(double x) {
        if (x >= 1) return x;
        x = Math.max(x, smallestPowOf2);
        if (Math.abs(x - Math.round(x))<eps) {
            return 1. / (1L<<(1-Math.round(x)));
        }
        double l = Math.floor(x);
        double u = Math.ceil(x);
        double fl = 1. / (1L<<(1-Math.round(l)));
        double fu = 1. / (1L<<(1-Math.round(u)));
        return fl + (fu-fl)*(x-l);
    }
    static double zoom2x(double y) {
        if (y >= 1) return y;
        if (y <= 1. / (1L<< 1-smallestPowOf2)) return y;
        double yi = 1. / y;
        long lyi = Math.round(yi);
        if (Math.abs(yi - lyi) < eps
         && 0 == (lyi & lyi-1)
        ) {
            return Math.round(log2(y)+1);
        }
        double xl = Math.floor(log2(y)+1.);
        double xu = Math.ceil(log2(y)+1.);
        double fl = 1. / (1L<<(1-Math.round(xl)));
        double fu = 1. / (1L<<(1-Math.round(xu)));
        return xl + (y-fl)/(fu-fl);
    }
}

class DragMover extends MouseInputAdapter {
    private JComponent m_view            = null;
    private Point      m_holdPointOnView = null;

    public DragMover(JComponent view) {
        m_view = view;
        m_view.addMouseListener(this);
        m_view.addMouseMotionListener(this);
    }
    @Override
    public void mousePressed(MouseEvent e) {
        m_view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        m_holdPointOnView = e.getPoint();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        m_view.setCursor(null);
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        Point dragEventPoint = e.getPoint();
        JViewport viewport = (JViewport) m_view.getParent();
        Point viewPos = viewport.getViewPosition();
        int maxViewPosX = m_view.getWidth() - viewport.getWidth();
        int maxViewPosY = m_view.getHeight() - viewport.getHeight();

        if (dragEventPoint == null) {
            System.out.println("bug: dragEventPoint == null");
            return;
        }
        if (m_holdPointOnView == null) {
            System.out.println("bug: m_holdPointOnView == null");
            return;
        }

        if(m_view.getWidth() > viewport.getWidth()) {
            viewPos.x -= dragEventPoint.x - m_holdPointOnView.x;

            if(viewPos.x < 0) {
                viewPos.x = 0;
                m_holdPointOnView.x = dragEventPoint.x;
            }

            if(viewPos.x > maxViewPosX) {
                viewPos.x = maxViewPosX;
                m_holdPointOnView.x = dragEventPoint.x;
            }
        }

        if(m_view.getHeight() > viewport.getHeight()) {
            viewPos.y -= dragEventPoint.y - m_holdPointOnView.y;

            if(viewPos.y < 0) {
                viewPos.y = 0;
                m_holdPointOnView.y = dragEventPoint.y;
            }

            if(viewPos.y > maxViewPosY) {
                viewPos.y = maxViewPosY;
                m_holdPointOnView.y = dragEventPoint.y;
            }
        }

        viewport.setViewPosition(viewPos);
    }
}

abstract class FileLocations {
    static String unThumbnail(String urlOrFile) {
        if (urlOrFile == null) {
            return urlOrFile;
        }
        // Curiosity
        final String thmSuffix = "-thm.jpg";
        final String brSuffix = "-br.jpg";
        final String imgSuffix = ".JPG";
        // Perseverance
        final String _320Suffix = "_320.jpg";
        final String _800Suffix = "_800.jpg";
        final String _1200Suffix = "_1200.jpg";
        final String pngSuffix = ".png";
        String unthumbnailed =
            replaceSuffix(thmSuffix, imgSuffix,
                replaceSuffix(brSuffix, imgSuffix,
                        replaceSuffix(_320Suffix, pngSuffix,
                                replaceSuffix(_800Suffix, pngSuffix,
                                        replaceSuffix(_1200Suffix, pngSuffix,
                                            urlOrFile
                                        )
                                )
                        )
                )
            );
        if (isUrl(urlOrFile) || isCuriousUrn(urlOrFile) || !isProblemWithFile(unthumbnailed)) {
            return unthumbnailed;
        } else {
            return urlOrFile;
        }
    }
    static boolean isProblemWithFile(String path) {
        try {
            if (new File(path).isFile()) {
                return false;
            }
        } catch (Throwable e) {
            // there are problems, return true
        }
        return true;
    }
    static String replaceSuffix(String oldSuffix, String newSuffix, String orig) {
        if (orig==null || !orig.endsWith(oldSuffix)) {
            return orig;
        }
        String base = orig.substring(0, orig.length()-oldSuffix.length());
        return base + newSuffix;
    }
    static String getFileName(String urnOrUrlOrPath) {
        String urlOrPath = uncuriousUri(urnOrUrlOrPath);
        String fullPath = urlOrPath;
        if (isUrl(urlOrPath)) {
            try {
                URL url = new URL(urlOrPath);
                fullPath = url.getPath();
            } catch (MalformedURLException e) {
                // do nothing, go on assuming it's a file
            }
        }
        Path fileName = Paths.get(fullPath).getFileName();
        return fileName == null ? "" : fileName.toString();
    }
    static String getFileNameNoExt(String urlOrPath) {
        String fileNameExt = getFileName(urlOrPath);
        int indexOfDot = fileNameExt.lastIndexOf('.');
        if (indexOfDot < 0) {
            return fileNameExt;
        } else {
            return fileNameExt.substring(0,indexOfDot);
        }
    }
    static String getFileExt(String urlOrPath) {
        String fileNameExt = getFileName(urlOrPath);
        int indexOfDot = fileNameExt.lastIndexOf('.');
        if (indexOfDot < 0) {
            return "";
        } else {
            return fileNameExt.substring(indexOfDot);
        }
    }
    // TODO: support Perseverance
    static Optional<Integer> getSol(String urlOrPath) {
        Pattern pattern = Pattern.compile("[/\\\\]([0-9]+)[/\\\\]");
        Matcher matcher = pattern.matcher(urlOrPath);
        if(matcher.find()) {
            try {
                return Optional.of(Integer.valueOf(matcher.group(1)));
            } catch (Throwable ignored) {
                // do nothing
            }
        }
        return Optional.empty();
    }
    static boolean isMrl(String fname) {
        return fname.matches("[0-9]{4}M[RL].*");
    }
    static boolean isMr(String fname) {
        return fname.matches("[0-9]{4}MR.*");
    }
    static boolean isMl(String fname) {
        return fname.matches("[0-9]{4}ML.*");
    }
    static List<String> twoPaths(String path0) {
        var fname = getFileName(path0);
        if (isMrl(fname)) {
            if (isMr(fname)) {
                return Arrays.asList(path0, "curious:l:"+path0);
            } else {
                return Arrays.asList("curious:r:"+path0, path0);
            }
        }
        if (isUrl(path0)) {
            try {
                URL url = new URL(path0);
                String urlFile = url.getFile();
                String urlBeforeFile = path0.substring(0, path0.length() - urlFile.length());
                return _twoPaths(urlFile).stream().map(s -> ImageAndPath.isSpecialPath(s)?s:urlBeforeFile+s).collect(Collectors.toList());
            } catch (MalformedURLException e) {
                // do nothing, go on assuming it's a file
            }
        }
        return _twoPaths(path0);
    }
    static String replaceFileName(String urlOrPath, String newFileName) {
        String fileName0 = Paths.get(urlOrPath).getFileName().toString();
        return urlOrPath.replace(fileName0, newFileName);
    }
    static List<String> _twoPaths(String path0) {
        String fullPath1 = "", fullPath2 = "";
        var fullPath = Paths.get(path0);
        var dir = fullPath.getParent();
        if (dir == null) {
            dir = Paths.get(".");
        }
        var file = fullPath.getFileName().toString();
        if (isMarkedRL(file)) {
            String otherFullPath = Paths.get(dir.toString(), toggleRL(file)).toString();
            if (file.charAt(1) == 'R') {
                fullPath1 = path0;
                fullPath2 = otherFullPath;
            } else {
                fullPath1 = otherFullPath;
                fullPath2 = path0;
            }
        } else if (isChemcamMarkedRL(file)) {
            if (isChemcamMarkedR(file)) {
                fullPath1 = path0;
                fullPath2 = chemcamRToL(path0);
            } else {
                fullPath1 = chemcamLToR(path0);
                fullPath2 = path0;
            }
        } else {
            fullPath1 = path0;
        }
        fullPath1 = getRidOfBackslashes(fullPath1);
        fullPath2 = getRidOfBackslashes(fullPath2);
        return Arrays.asList(fullPath1, fullPath2);
    }
    static String getRidOfBackslashes(String s) {
        return s.replaceAll("\\\\","/");
    }
    static List<String> twoPaths(String urlOrPath1, String urlOrPath2) {
        String file1 = getFileName(urlOrPath1);
        String file2 = getFileName(urlOrPath2);
        if( (isMarkedL(file1) && isMarkedR(file2))
         || (isMrlMarkedL(urlOrPath1, file1) && isMrlMarkedR(urlOrPath2, file2))
         || (isChemcamMarkedL(file1) && isChemcamMarkedR(file2))
          ) {
            return Arrays.asList(urlOrPath2, urlOrPath1);
        }
        return Arrays.asList(urlOrPath1, urlOrPath2);
    }
    private static boolean isMarkedR(String file) {
        return file.startsWith("NRB") || file.startsWith("RRB") || file.startsWith("FRB")
            || file.startsWith("NRA") || file.startsWith("RRA") || file.startsWith("FRA")
            // perseverance
            || file.startsWith("ZRF") || file.startsWith("ZR0")
            || file.startsWith("ZR1") || file.startsWith("ZR2") || file.startsWith("ZR3")
            || file.startsWith("ZR4") || file.startsWith("ZR5") || file.startsWith("ZR6")
            || file.startsWith("NRE") || file.startsWith("NRM") || file.startsWith("NRF")
            || file.startsWith("NRG") || file.startsWith("NRR") // NLB: above
            || file.startsWith("FRE") || file.startsWith("FRF") || file.startsWith("FRM")
            || file.startsWith("FRR") || file.startsWith("FRG") || file.startsWith("FRB")
            || file.startsWith("RRE") || file.startsWith("RRF") || file.startsWith("RRM")
            || file.startsWith("RRR") || file.startsWith("RRG") || file.startsWith("RRB")
            ;
    }
    private static boolean isMarkedL(String file) {
        return file.startsWith("NLB") || file.startsWith("RLB") || file.startsWith("FLB")
            || file.startsWith("NLA") || file.startsWith("RLA") || file.startsWith("FLA")
            // perseverance
            || file.startsWith("ZLF") || file.startsWith("ZL0")
            || file.startsWith("ZL1") || file.startsWith("ZL2") || file.startsWith("ZL3")
            || file.startsWith("ZL4") || file.startsWith("ZL5") || file.startsWith("ZL6")
            || file.startsWith("NLE") || file.startsWith("NLM") || file.startsWith("NLF")
            || file.startsWith("NLG") || file.startsWith("NLR") // NLB: above
            || file.startsWith("FLE") || file.startsWith("FLF") || file.startsWith("FLM")
            || file.startsWith("FLR") || file.startsWith("FLG") || file.startsWith("FLB")
            || file.startsWith("RLE") || file.startsWith("RLF") || file.startsWith("RLM")
            || file.startsWith("RLR") || file.startsWith("RLG") || file.startsWith("RLB")
            ;
    }
    private static boolean isMarkedRL(String file) {
        return isMarkedR(file) || isMarkedL(file);
    }
    static String toggleRL(String file) {
        StringBuilder sb = new StringBuilder(file);
        sb.setCharAt(1, (char) (sb.charAt(1)^('R'^'L')));
        return sb.toString();
    }
    static boolean isMrlMarkedR(String path, String fname) {
        return isCuriousUrn(path) ? isCuriousRUrn(path) : isMr(fname);
    }
    static boolean isMrlMarkedL(String path, String fname) {
        return isCuriousUrn(path) ? isCuriousLUrn(path) : isMl(fname);
    }
    static boolean isChemcamMarkedRL(String fname) {
        return isChemcamMarkedR(fname) || isChemcamMarkedL(fname);
    }
    static boolean isChemcamMarkedR(String fname) {
        return fname.startsWith("CR0_") && fname.matches("CR0_[0-9]+EDR_F[0-9]+CCAM[0-9]+M_.*");
    }
    static boolean isChemcamMarkedL(String fname) {
        return fname.startsWith("CR0_") && fname.matches("CR0_[0-9]+PRC_F[0-9]+CCAM[0-9]+L1.*");
    }
    static String chemcamRToL(String path) {
        String res = path
                .replace("/opgs/edr/ccam/CR0_", "/soas/rdr/ccam/CR0_")
                .replace("EDR_F", "PRC_F")
                .replaceFirst("M_(?:-[thmbr]+)?\\.[jJ][pP][gG]","L1.PNG");
        return res;
    }
    static String chemcamLToR(String path) {
        String res = path
                .replace("/soas/rdr/ccam/CR0_", "/opgs/edr/ccam/CR0_")
                .replace("PRC_F", "EDR_F")
                .replace("L1.PNG", "M_.JPG");
        return res;
    }
    static boolean isCuriousUrn(String path) {
        return path.startsWith("curious:");
    }
    static boolean isCuriousRUrn(String path) {
        return path.startsWith("curious:r:");
    }
    static boolean isCuriousLUrn(String path) {
        return path.startsWith("curious:l:");
    }
    static String uncuriousUri(String path) {
        if (isCuriousLUrn(path) || isCuriousRUrn(path)) {
            return path.substring("curious:l:".length());
        } else {
            return path;
        }
    }
    static boolean isUrl(String path) {
        String prefix = path.substring(0,Math.min(9, path.length())).toLowerCase();
        var res = prefix.startsWith("http:/") || prefix.startsWith("https:/") || prefix.startsWith("file:/");
        //System.out.println("prefix=["+prefix+"] => "+res+" "+Thread.currentThread());
        return res;
    }
    static boolean isBayered(String urlOrPath) {
        String fname = getFileName(urlOrPath);
        return fname.matches(".*\\d+M[RL]\\d+[CK]00_DXXX.*")
            || fname.matches("N[RL]E_\\d{4}_\\d+_\\d+ECM_N\\d+NCAM\\d+_\\d{2}_0LLJ.*")
            || fname.matches("SI1_\\d{4}_\\d+_\\d+ECM_N\\d+SRLC\\d+_\\d+LUJ.*");
    }
}

abstract class RoverTime {
    final static int MIN_CHARS_IN_TIMESTAMP = 6;
    public static long toUtcMillis(long roverTimestamp) {
        return Math.round(roverTimestamp*1.000009468 + 946724361)*1000L;
    }
    public static long parseTimestamp(int offset, String s) {
        if (offset >= s.length()) {
            return 0;
        }
        int charsParsed = 0;
        long res = 0;
        for (char c : s.substring(offset).toCharArray()) {
            if (c < '0' || c > '9') {
                break;
            }
            res = res*10 + (c - '0');
            charsParsed++;
        }
        if(charsParsed < MIN_CHARS_IN_TIMESTAMP) {
            return 0;
        }
        return res;
    }
    public static String earthDateForFile(int offset, String s) {
        long ts = parseTimestamp(offset, s);
        if (ts == 0) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String res  = dateFormat.format(new Date(toUtcMillis(ts)));
        return res;
    }
}
class HttpLocations {
    /**
     * Http HEAD Method to get URL content type
     *
     * @param urlString
     * @return content type
     * @throws IOException
     */
    public static String getContentType(String urlString) throws IOException{
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        NasaReader.setHttpHeaders(connection);
        if (isRedirect(connection.getResponseCode())) {
            String newUrl = connection.getHeaderField("Location"); // get redirect url from "location" header field
            System.out.println("Original request URL: "+urlString+" redirected to: "+ newUrl);
            return getContentType(newUrl);
        }
        String contentType = connection.getContentType();
        return contentType;
    }

    /**
     * Check status code for redirects
     *
     * @param statusCode
     * @return true if matched redirect group
     */
    protected static boolean isRedirect(int statusCode) {
        if (statusCode != HttpURLConnection.HTTP_OK) {
            if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
             || statusCode == HttpURLConnection.HTTP_MOVED_PERM
             || statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
                return true;
            }
        }
        return false;
    }
    public static String readStringFromURL(String requestURL) throws IOException
    {
        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),
                StandardCharsets.UTF_8.toString()))
        {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    static String unPage(String url) {
        if (!FileLocations.isUrl(url)) {
            return url;
        }
        String endOfUrl = url.substring(Math.max(0,url.length()-8)).toLowerCase();
        if (endOfUrl.endsWith(".jpg") || endOfUrl.endsWith(".jpeg") || endOfUrl.endsWith(".png")) {
            return url;
        }
        try {
            String type = getContentType(url);
            System.out.println("Content type: "+type +" -- "+url);
            if (type.toLowerCase().startsWith("text/html")) {
                var body = readStringFromURL(url);
                var imageUrl = HtmlParserProfanation.getCuriosityRawImageUrl(body);
                System.out.println("read & found: "+url +" => "+imageUrl);
                return imageUrl;
            }
        } catch (IOException e) {
            // do nothing
        }
        return url;
    }
    public static List<String> unPageAll(List<String> urls) {
        return urls.stream().map(HttpLocations::unPage).collect(Collectors.toList());
    }
}
class HtmlParserProfanation {
    static String getNode(String body, String type, Function<String, String> extract) {
        int i=0;
        String pref = "<"+type;
        while (-1 != (i=body.indexOf(pref, i))) {
            int j = body.indexOf(">", i);
            if (j==-1) {
                return null;
            }
            j++;
            String res = extract.apply(body.substring(i,j));
            if (res != null) {
                return res;
            }
            i=j;
        }
        return null;
    }
    static String getCuriosityRawImageUrl(String body) {
        return getNode(body, "meta", s -> {
            if("og:image".equals(getValue(s,"property="))) {
                return getValue(s, "content=");
            } else  {
                return null;
            }
        });
    }

    static String getValue(String s, String c) {
        if (!c.endsWith("=")) { c += "="; }
        int i=s.indexOf(c);
        if (i==-1) { return null; }
        i += c.length();
        char d = s.charAt(i);
        if (d != '\'' && d != '"') { d=' '; } else { i++; }
        int j = s.indexOf(d,i);
        if (j==-1) { j=s.length()-1; }
        return s.substring(i,j);
    }
}
class ProcessForker {
    static void forkWrapped() {
        System.out.println("forking this process...");
        try {
            fork();
            System.out.println("forking this process: done");
        } catch (Throwable t) {
            System.out.println("exception while forking this process!");
            t.printStackTrace();
        }
    }
    static void fork() {
        String jvm_location;
        if (System.getProperty("os.name").startsWith("Win")) {
            jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
        } else {
            jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }
        String fileBeingRun = null;
        URL fileBeingRunUrl = Main.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            fileBeingRun = Paths.get(fileBeingRunUrl.toURI()).toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fileBeingRun = fileBeingRunUrl.getPath();
        }
        System.out.println("jvm: "+jvm_location+" file: "+fileBeingRun);
        ProcessBuilder pb = fileBeingRun.endsWith(".java")
                          ? new ProcessBuilder(jvm_location, fileBeingRun, ImageAndPath.NO_PATH, ImageAndPath.NO_PATH)
                          : new ProcessBuilder(jvm_location, "-jar", fileBeingRun, ImageAndPath.NO_PATH, ImageAndPath.NO_PATH);
        pb.inheritIO();
        System.out.println("command: "+pb.command());
        try {
            var p = pb.start();
            System.out.println("new process: "+p);
        } catch (IOException e) {
            System.out.println("cannot start process");
            e.printStackTrace();
        }
    }
}

class StringDiffs {
    public static List<String> coloredUrls(List<String> urls) {
        String l = urls.get(0);
        String r = urls.get(1);
        Pair<BitSet> diff = diff(l,r);
        var dl = diff.first;
        var dr = diff.second;
        String cl = coloredString(l, dl);
        String cr = coloredString(r, dr);
        return Arrays.asList(cl,cr);
    }
    static String coloredString(String text, BitSet setOfRed) {
        String START_RED = "<font color='red'>";
        String END_RED = "</font>";
        String START_BLUE = "<font color='blue'>";
        String END_BLUE = "</font>";
        StringBuilder res = new StringBuilder("<html><nobr>");
        res.append(START_BLUE);
        boolean isRed = false;
        for (int i=0; i<text.length(); i++) {
            boolean nowRed = setOfRed.get(i);
            if (nowRed ^ isRed) {
                if (nowRed) {
                    res.append(START_RED);
                } else {
                    res.append(END_RED);
                }
            }
            res.append(text.charAt(i));
            isRed = nowRed;
        }
        if (isRed) {
            res.append(END_RED);
        }
        res.append(END_BLUE);
        res.append("</nobr></html>");
        return res.toString();
    }

    static void printExample(String a, String b) {
        var d = diff(a,b);
        String a1 = "";
        for (int i=0; i<a.length(); i++) {
            a1 += d.first.get(i) ? a.charAt(i) : ' ';
        }
        String b1 = "";
        for (int i=0; i<b.length(); i++) {
            b1 += d.second.get(i) ? b.charAt(i) : ' ';
        }
        System.out.println("--vv--");
        System.out.println(a1);
        System.out.println(a);
        System.out.println(b);
        System.out.println(b1);
        System.out.println("--^^--");
    }

    /**
     * Returns a minimal set of characters that have to be removed from (or added to) the respective
     * strings to make the strings equal.
     */
    public static Pair<BitSet> diff(String a, String b) {
        return diffHelper(a, b, new HashMap<>(), a.length(), b.length());
    }

    /**
     * Recursively compute a minimal set of characters while remembering already computed substrings.
     * Runs in O(n^2).
     */
    private static Pair<BitSet> diffHelper(String a, String b, Map<Long, Pair<BitSet>> lookup, int a0len, int b0len) {
        long key = ((long) a.length()) << 32 | b.length();
        if (!lookup.containsKey(key)) {
            Pair<BitSet> value;
            if (a.isEmpty() || b.isEmpty()) {
                BitSet aa = new BitSet();
                aa.set(a0len - a.length(), a0len);
                BitSet bb = new BitSet();
                bb.set(b0len - b.length(), b0len);
                value = new Pair<>(aa, bb);
            } else if (a.charAt(0) == b.charAt(0)) {
                value = diffHelper(a.substring(1), b.substring(1), lookup, a0len, b0len);
            } else {
                Pair<BitSet> aa = diffHelper(a.substring(1), b, lookup, a0len, b0len);
                Pair<BitSet> bb = diffHelper(a, b.substring(1), lookup, a0len, b0len);
                if (aa.first.cardinality() + aa.second.cardinality() < bb.first.cardinality() + bb.second.cardinality()) {
                    BitSet aas = new BitSet();
                    aas.or(aa.first);
                    aas.set(a0len - a.length());
                    value = new Pair<>(aas, aa.second);
                } else {
                    BitSet bbs = new BitSet();
                    bbs.or(bb.second);
                    bbs.set(b0len - b.length());
                    value = new Pair<>(bb.first, bbs);
                }
            }
            lookup.put(key, value);
        }
        return lookup.get(key);
    }

    public static class Pair<T> {
        public Pair(T first, T second) {
            this.first = first;
            this.second = second;
        }

        public final T first, second;

        public String toString() {
            return "(" + first + "," + second + ")";
        }
    }
}

class Debayer {
    static List<Function<BufferedImage, BufferedImage>> debayering_methods = Arrays.asList(
            Debayer::debayer_dotted,
            Debayer::debayer_squares,
            Debayer::debayer_avg,
            Debayer::debayer_closest_match_square,
            Debayer::debayer_closest_match_WNSE_clockwise
    );

    static BufferedImage debayer_dotted(BufferedImage orig) {
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<WIDTH; i++) {
            for (int j=0; j<HEIGHT; j++) {
                int type = (j&1)*2 + (i&1); // RGGB
                int r=0, g=0, b=0;
                switch (type) {
                    case 0: { // R
                        r = getC(orig, i, j);
                    } break;
                    case 1: case 2: { // G
                        g = getC(orig, i, j);
                    } break;
                    case 3: { // B
                        b = getC(orig,i,j);
                    } break;
                }
                res.setRGB(i,j,(r<<16)|(g<<8)|b);
            }
        }
        return res;
    }

    static BufferedImage debayer_squares(BufferedImage orig) {
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<WIDTH; i++) {
            for (int j=0; j<HEIGHT; j++) {
                int R = getC(orig, i&-2, j&-2);
                int Gr = getC(orig, i&-2, j|1);
                int Gb = getC(orig, i|1, j&-2);
                int B = getC(orig, i|1, j|1);
                // R Gr R Gr
                // Gb B Gb B
                // R Gr R Gr
                // Gb B Gb B
                int r = R;
                int g = Gb;
                int b = B;
                res.setRGB(i,j,(r<<16)|(g<<8)|b);
            }
        }
        return res;
    }

    static BufferedImage debayer_closest_match_square(BufferedImage orig) {
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<WIDTH; i++) {
            for (int j=0; j<HEIGHT; j++) {
                int type = (j&1)*2 + (i&1); // RGGB
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                int r,g,b;
                switch (type) {
                    case 0: { // R
                        r = getC(orig, i, j);
                        Direction dirG = findClosestMatchDist2(orig, i, j, Direction.E, Direction.S, Direction.W, Direction.N);
                        g = getC(orig, dirG.x1(i), dirG.y1(j));
                        Direction dirB = findClosestMatchDist2(orig, i, j, Direction.SE, Direction.SW, Direction.NW, Direction.NE);
                        b = getC(orig, dirB.x1(i), dirB.y1(j));
                    } break;
                    case 1: { // Gr
                        Direction dirR = findClosestMatchDist2(orig, i, j, Direction.W, Direction.E);
                        r = getC(orig, dirR.x1(i), dirR.y1(j));
                        g = getC(orig, i, j);
                        Direction dirB = findClosestMatchDist2(orig, i, j, Direction.S, Direction.N);
                        b = getC(orig, dirB.x1(i), dirB.y1(j));
                    } break;
                    case 2: { // Gb
                        Direction dirR = findClosestMatchDist2(orig, i, j, Direction.N, Direction.S);
                        r = getC(orig, dirR.x1(i), dirR.y1(j));
                        g = getC(orig, i, j);
                        Direction dirB = findClosestMatchDist2(orig, i, j, Direction.E, Direction.W);
                        b = getC(orig, dirB.x1(i), dirB.y1(j));
                    } break;
                    case 3: { // B
                        Direction dirR = findClosestMatchDist2(orig, i, j, Direction.NW, Direction.SW, Direction.SE, Direction.NE);
                        r = getC(orig, dirR.x1(i), dirR.y1(j));
                        Direction dirG = findClosestMatchDist2(orig, i, j, Direction.W, Direction.N, Direction.E, Direction.S);
                        g = getC(orig, dirG.x1(i), dirG.y1(j));
                        b = getC(orig,i,j);
                    } break;
                    default: // stupid Java, this is impossible! type is 0..3!
                        r=g=b=0;
                }
                res.setRGB(i,j,(r<<16)|(g<<8)|b);
            }
        }
        return res;
    }
    static BufferedImage debayer_closest_match_WNSE_clockwise(BufferedImage orig) {
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<WIDTH; i++) {
            for (int j=0; j<HEIGHT; j++) {
                int type = (j&1)*2 + (i&1); // RGGB
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                int r,g,b;
                switch (type) {
                    case 0: { // R
                        r = getC(orig, i, j);
                        Direction dirG = findClosestMatchDist2(orig, i, j, Direction.W, Direction.N, Direction.S, Direction.E);
                        g = getC(orig, dirG.x1(i), dirG.y1(j));
                        Direction dirB = findClosestMatchDist2(orig, i, j, Direction.NW, Direction.SW, Direction.NE, Direction.SE);
                        b = getC(orig, dirB.x1(i), dirB.y1(j));
                    } break;
                    case 1: { // Gr
                        Direction dirR = findClosestMatchDist2(orig, i, j, Direction.W, Direction.E);
                        r = getC(orig, dirR.x1(i), dirR.y1(j));
                        g = getC(orig, i, j);
                        Direction dirB = findClosestMatchDist2(orig, i, j, Direction.N, Direction.S);
                        b = getC(orig, dirB.x1(i), dirB.y1(j));
                    } break;
                    case 2: { // Gb
                        Direction dirR = findClosestMatchDist2(orig, i, j, Direction.N, Direction.S);
                        r = getC(orig, dirR.x1(i), dirR.y1(j));
                        g = getC(orig, i, j);
                        Direction dirB = findClosestMatchDist2(orig, i, j, Direction.W, Direction.E);
                        b = getC(orig, dirB.x1(i), dirB.y1(j));
                    } break;
                    case 3: { // B
                        Direction dirR = findClosestMatchDist2(orig, i, j, Direction.NW, Direction.SW, Direction.NE, Direction.SE);
                        r = getC(orig, dirR.x1(i), dirR.y1(j));
                        Direction dirG = findClosestMatchDist2(orig, i, j, Direction.W, Direction.N, Direction.S, Direction.E);
                        g = getC(orig, dirG.x1(i), dirG.y1(j));
                        b = getC(orig,i,j);
                    } break;
                    default: // stupid Java, this is impossible! type is 0..3!
                        r=g=b=0;
                }
                res.setRGB(i,j,(r<<16)|(g<<8)|b);
            }
        }
        return res;
    }
    static Direction findClosestMatchDist2(BufferedImage bi, int i, int j, Direction... directions) {
        int c0 = getC(bi, i, j);
        int diff = Integer.MAX_VALUE;
        Direction bestDirection = null;
        for (Direction d : directions) {
            int c = getC(bi, d.x2(i), d.y2(j));
            if (c==c0) {
                return d;
            }
            int newDiff = Math.abs(c - c0);
            if (newDiff < diff) {
                diff = newDiff;
                bestDirection = d;
            }
        }
        return bestDirection;
    }
    enum Direction {
        N(0,1),E(1,0),S(0,-1),W(-1,0),
        NW(-1,1), NE(1, 1),SE(1,-1),SW(-1,-1);
        int dx; int dy;
        Direction(int x, int y) { dx=x; dy=y; }
        int x1(int x0) { return x0+dx; }
        int y1(int y0) { return y0+dy; }
        int x2(int x0) { return x0+2*dx; }
        int y2(int y0) { return y0+2*dy; }
    }
    static int averageDist1(BufferedImage bi, int i, int j, Direction... directions) {
        int sum = 0;
        for (Direction dir : directions) {
            sum += getC(bi, dir.x1(i), dir.y1(j));
        }
        return sum / directions.length;
    }
    static BufferedImage debayer_avg(BufferedImage orig) {
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<WIDTH; i++) {
            for (int j=0; j<HEIGHT; j++) {
                int type = (j&1)*2 + (i&1); // RGGB
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                // R Gr R Gr R Gr
                // Gb B Gb B Gb B
                int r,g,b;
                switch (type) {
                    case 0: { // R
                        r = getC(orig, i, j);
                        g = averageDist1(orig, i, j, Direction.W, Direction.N, Direction.S, Direction.E);
                        b = averageDist1(orig, i, j, Direction.NW, Direction.SW, Direction.NE, Direction.SE);
                    } break;
                    case 1: { // Gr
                        r = averageDist1(orig, i, j, Direction.W, Direction.E);
                        g = getC(orig, i, j);
                        b = averageDist1(orig, i, j, Direction.N, Direction.S);
                    } break;
                    case 2: { // Gb
                        r = averageDist1(orig, i, j, Direction.N, Direction.S);
                        g = getC(orig, i, j);
                        b = averageDist1(orig, i, j, Direction.W, Direction.E);
                    } break;
                    case 3: { // B
                        r = averageDist1(orig, i, j, Direction.NW, Direction.SW, Direction.NE, Direction.SE);
                        g = averageDist1(orig, i, j, Direction.W, Direction.N, Direction.S, Direction.E);
                        b = getC(orig,i,j);
                    } break;
                    default: // stupid Java, this is impossible! type is 0..3!
                        r=g=b=0;
                }
                res.setRGB(i,j,(r<<16)|(g<<8)|b);
            }
        }
        return res;
    }
    static int getC(BufferedImage bi, int x, int y) {
        int res = 0;
        if (x >= 0 && y >= 0 && x < bi.getWidth() && y < bi.getHeight()) {
            res = r(bi.getRGB(x,y));
        }
        return res;
    }
    static int r(int argb) {
        return (argb>>16) & 0xff;
    }
    static int g(int argb) {
        return (argb>>8) & 0xff;
    }
    static int b(int argb) {
        return argb & 0xff;
    }
}

class HyperTextPane extends JTextPane {
    HyperlinkClickListener hyperlinkClickListener;

    public HyperTextPane(String htmlText) {
        setContentType("text/html");
        setEditable(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Element h = getHyperlinkElement(e);
                if (h != null) {
                    Object attribute = h.getAttributes().getAttribute(HTML.Tag.A);
                    if (attribute instanceof AttributeSet) {
                        AttributeSet set = (AttributeSet) attribute;
                        String href = (String) set.getAttribute(HTML.Attribute.HREF);
                        if (href != null) {
                            hyperlinkClickListener.onHyperlinkClicked(h, href, e.getButton());
                        }
                    }
                }
            }
        });
        setText(htmlText);
        setHyperlinkClickListener(
                (e,href,button) -> {
                    if (button == MouseEvent.BUTTON1) {
                        HyperTextPane.openHyperlink(href);
                    } else if (button == MouseEvent.BUTTON3) {
                        showHyperlinkCopyMenu(e,href);
                    }
        });
    }
    public void setHyperlinkClickListener(HyperlinkClickListener hyperlinkClickListener) {
        this.hyperlinkClickListener = hyperlinkClickListener;
    }
    private Element getHyperlinkElement(MouseEvent event) {
        JEditorPane editor = (JEditorPane) event.getSource();
        int pos = editor.getUI().viewToModel(editor, event.getPoint());
        if (pos >= 0 && editor.getDocument() instanceof HTMLDocument) {
            HTMLDocument hdoc = (HTMLDocument) editor.getDocument();
            Element elem = hdoc.getCharacterElement(pos);
            if (elem.getAttributes().getAttribute(HTML.Tag.A) != null) {
                return elem;
            }
        }
        return null;
    }
    public static boolean openHyperlink(String href) {
        try {
            Desktop.getDesktop().browse(new URI(href));
            return true;
        } catch (IOException | URISyntaxException e1) {
            e1.printStackTrace();
        }
        return false;
    }
    public void showHyperlinkCopyMenu(Element elem, String href) {
        JPopupMenu popup = new JPopupMenu();
        popup.add("Copy URL");
        ((JMenuItem)popup.getComponent(0)).addActionListener(e -> {
            StringSelection selection = new StringSelection(href);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);            }
        );
        try {
            Rectangle rec = modelToView(elem.getStartOffset());
            popup.show(this, rec.x, rec.y+rec.height);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    public interface HyperlinkClickListener {
        void onHyperlinkClicked(Element element, String href, int mouseButton);
    }
}

class ScreenshotSaver {
    JFileChooser fileChooser;

    public ScreenshotSaver(JFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    public interface SaveAction {
        void apply(File imgFile) throws Exception;
    }
    public void takeAndSaveScreenshot(JFrame frame, JComponent leftC, JComponent rightC, RawData rawData, DisplayParameters displayParameters, SaveOptions saveOptions) {
        try {
            BufferedImage bi = ScreenshotSaver.getScreenshot(frame);
            showSaveDialog(
                    frame,
                    FileLocations.getSol(rawData.left.path).map(x -> String.format("%04d-",x)).orElse(""),
                    "-x" + toSuffixNumber(displayParameters.zoom * displayParameters.zoomL),
                    (imgFile) -> {
                        String description = "Left, Right:\n" + rawData.left.path + "\n" + rawData.right.path + "\n";
                        ScreenshotSaver.writePng(imgFile, bi,
                            "Software", "Curious: X3D Viewer",
                            "Description", description);
                        ScreenshotSaver.writeText(toSrcFile(imgFile), description);
                        if (saveOptions.saveLeftRightImages) {
                            ScreenshotSaver.writePng(toLeftFile(imgFile), ScreenshotSaver.getScreenshot(leftC), "Description", description + "this is left\n");
                            ScreenshotSaver.writePng(toRightFile(imgFile), ScreenshotSaver.getScreenshot(rightC), "Description", description+"this is right\n");
                        }
                        if (saveOptions.saveGif) {
                            GifSequenceWriter.saveAsGif(
                                    toGifFile(imgFile),
                                    500,
                                    () -> ScreenshotSaver.getScreenshot(leftC),
                                    () -> ScreenshotSaver.getScreenshot(rightC)
                            );
                        }
                    }
            );
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
    static String toSuffixNumber(double f) {
        int i = (int) f;
        int d = ((int) (f*10)) % 10;
        return ("" + i) + ( d==0 ? "" : "_"+d );
    }
    static String toBase(String filename) {
        if (filename.length() > 4 && filename.endsWith(".png")) {
            return filename.substring(0, filename.length()-4);
        } else {
            return filename;
        }
    }
    static String toGif(String filename) {
        return toBase(filename)  + ".gif";
    }
    void showSaveDialog(JFrame frame, String prefix, String suffix, SaveAction howToSave) throws Exception {
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setSelectedFile(new File(prefix+"stereo"+suffix+".png"));
        while (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(frame)) {
            File imgFile = fileChooser.getSelectedFile();
            if (!endsWithIgnoreCase(imgFile.getAbsolutePath(),".png")) {
                JOptionPane.showMessageDialog(frame, "File name must end with \"png\"");
            } else if (checkAskOverwrite(frame, imgFile)
                    && checkAskOverwrite(frame, toSrcFile(imgFile))
                    && checkAskOverwrite(frame, toGifFile(imgFile))
                    && checkAskOverwrite(frame, toLeftFile(imgFile))
                    && checkAskOverwrite(frame, toRightFile(imgFile))
            ) {
                System.out.println("Saving to " + imgFile);
                howToSave.apply(imgFile);
                break;
            }
        }
    }
    private boolean checkAskOverwrite(JFrame frame, File file) {
        return !file.exists()
            || JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(frame, "File " + file + " already exists. Choose a different name?", "Overwrite?", JOptionPane.YES_NO_OPTION);
    }
    static File toGifFile(File imgFile) {
        return new File(toGif(imgFile.getAbsolutePath()));
    }
    static File toSrcFile(File imgFile) {
        return new File(imgFile.getAbsolutePath()+".source");
    }
    static File toLeftFile(File imgFile) {
        return new File(toBase(imgFile.getAbsolutePath())+".left.png");
    }
    static File toRightFile(File imgFile) {
        return new File(toBase(imgFile.getAbsolutePath())+".right.png");
    }
    static boolean endsWithIgnoreCase(String text, String suffix) {
        if (text.length() < suffix.length()) {
            return false;
        }
        String textSuffix = text.substring(text.length() - suffix.length());
        return textSuffix.equalsIgnoreCase(suffix);
    }
    public static void writePng(Object output, BufferedImage buffImg, String... keysAndValues) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

        //adding metadata
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        for (int i=0; i<keysAndValues.length/2; i++) {
            IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
            textEntry.setAttribute("keyword", keysAndValues[i*2]);
            textEntry.setAttribute("value", keysAndValues[i*2+1]);

            text.appendChild(textEntry);
        }

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(text);

        metadata.mergeTree("javax_imageio_png_1.0", root);

        //writing the data
        //output - an Object to be used as an output destination, such as a File, writable RandomAccessFile, or OutputStream.
        ImageOutputStream stream = ImageIO.createImageOutputStream(output);
        writer.setOutput(stream);
        writer.write(metadata, new IIOImage(buffImg, null, metadata), writeParam);
        stream.close();
    }
    public static void writeText(File file, String text) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(text);
        }
    }
    public static BufferedImage getScreenshot(Component frame) throws AWTException {
        Robot robot = new Robot();
        Point loc = frame.getLocationOnScreen();
        Rectangle appRect = new Rectangle(loc.x, loc.y, frame.getWidth(), frame.getHeight());
        BufferedImage bi = robot.createScreenCapture(appRect);
        return bi;
    }
}

class JsonDiy {
    public static final boolean IGNORE_NULL_IN_MAPS = true;
    static Object get(Object root, String... indexes) {
        Object obj = root;
        for (String index : indexes) {
            if (obj instanceof Map) {
                Map map = (Map)obj;
                obj = map.get(index);
            } else if (obj instanceof List) {
                try {
                    int i = Integer.parseInt(index);
                    List list = (List) obj;
                    obj = list.get(i);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            }
            if (null == obj) {
                return null;
            }
        }
        return obj;
    }
    static int getInt(Object root, String... indexes) {
        return Integer.parseInt(Objects.toString(get(root, indexes)));
    }
    public static Object jsonToDataStructure(String jsonString) {
        var input = new InputState(jsonString);
        Object res = getElement(input, '\0');
        input.skipWhitespace().errorIfNotAtEnd();
        return res;
    }
    static String getKey(InputState is, char... delimiters) {
        char c = is.skipWhitespace().get();
        switch (c) {
            case '"': {
                String res = getStringDelimitedBy(is, '"','\0');
                is.errorIfNotDelimiter(is.peekBack(),'"');
                is.errorIfNotDelimiter(is.skipWhitespace().get(), delimiters);
                return res;
            }
            default: {
                is.unget();
                String res = getStringDelimitedBy(is, delimiters).trim();
                return res;
            }
        }

    }
    static Object getElement(InputState is, char... delimiters) {
        char c = is.skipWhitespace().get();
        switch (c) {
            case '"': {
                String res = getStringDelimitedBy(is, '"','\0');
                is.errorIfNotDelimiter(is.peekBack(),'"');
                is.errorIfNotDelimiter(is.skipWhitespace().get(), delimiters);
                return res;
            }
            case '[':
                return getArray(is, delimiters);
            case '{':
                return getDictionary(is, delimiters);
            default:
                is.unget();
                return new Symbol(getStringDelimitedBy(is, delimiters).trim());
        }
    }
    static Map<String,Object> getDictionary(InputState is, char... delimiters) {
        var res = new HashMap<String, Object>();
        if ('}' != is.skipWhitespace().peek()) {
            do {
                String key = getKey(is, ':', '\0');
                is.errorIfNotDelimiter(is.peekBack(), ':');
                is.errorIfAtEnd();
                Object value = getElement(is, '}', ',', '\0');
                is.errorIfNotDelimiter(is.peekBack(), '}', ',');
                if (!IGNORE_NULL_IN_MAPS || !Symbol.isNull(value)) {
                    res.put(key, value);
                }
                if ('}' == is.peekBack()) {
                    break;
                }
                is.errorIfAtEnd();
            } while (true);
        } else {
            is.get();
        }
        is.errorIfNotDelimiter(is.skipWhitespace().get(), delimiters);
        return res;
    }
    static List<Object> getArray(InputState is, char... delimiters) {
        var res = new ArrayList<Object>();
        if (']' != is.skipWhitespace().peek()) {
            do {
                res.add(getElement(is, ',', ']', '\0'));
                if (']' == is.peekBack()) {
                    break;
                }
                is.errorIfAtEnd();
            } while (true);
        } else {
            is.get();
        }
        is.errorIfNotDelimiter(is.skipWhitespace().get(), delimiters);
        return res;
    }
    static String getStringDelimitedBy(InputState is, char... delimiters) {
        var res = new StringBuilder();
        char c;
        while (!arrayContains(delimiters, c = is.get())) {
            res.append(c);
        }
        return res.toString();
    }
    static private boolean arrayContains(char[] arr, char x) {
        int size = arr.length;
        for (int i=0; i<size; i++) {
            if (arr[i] == x) {
                return true;
            }
        }
        return false;
    }
    static class InputState {
        int pos;
        String source;
        InputState(String source) {
            this.pos = 0;
            this.source = source;
        }
        InputState(int pos, String source) {
            this.pos = pos;
            this.source = source;
        }
        char get() {
            if (pos >= source.length()) {
                pos++;
                return 0;
            } else {
                return source.charAt(pos++);
            }
        }
        char peek() {
            return pos >= source.length() ? 0 : source.charAt(pos);
        }
        char peekBack() {
            return pos-1 >= source.length() ? 0 : source.charAt(pos-1);
        }
        InputState unget() { --pos; return this; }
        InputState skipWhitespace() {
            while (Character.isWhitespace(peek())) pos++;
            return this;
        }
        InputState errorIfAtEnd() {
            if (pos >= source.length()) {
                throw new PrematureEndOfInputException(describePosition());
            }
            return this;
        }
        InputState errorIfNotDelimiter(char c, char... delimiters) {
            if (!arrayContains(delimiters, c)) {
                throw new DelimiterNotFoundException(describePosition()+"\n"+"c='"+c+"' not in ("+new String(delimiters)+")");
            }
            return this;
        }
        InputState errorIfNotAtEnd() {
            if (pos < source.length()) {
                throw new EndNotReachedException(describePosition());
            }
            return this;
        }
        String describePosition() {
            if (pos >= source.length()) {
                return pos+": *at_end*";
            }
            return pos+": -->"+source.substring(pos,Math.min(source.length(), pos+40));
        }
    }
    //* Any value not in quotes, including numbers
    static class Symbol {
        final String value;
        public Symbol(String value) {
            this.value = value;
        }
        public boolean isNull() {
            return "null".equals(value);
        }
        @Override
        public String toString() {
            return value;
        }
        public static boolean isNull(Object x) { return x==null || (x instanceof Symbol && "null".equals(((Symbol) x).value));}
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Symbol symbol = (Symbol) o;
            return value != null ? value.equals(symbol.value) : symbol.value == null;
        }
        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
    static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
    static class PrematureEndOfInputException extends ParseException {
        public PrematureEndOfInputException(String message) {
            super(message);
        }
    }
    static class DelimiterNotFoundException extends ParseException {
        public DelimiterNotFoundException(String message) {
            super(message);
        }
    }
    static class EndNotReachedException extends ParseException {
        public EndNotReachedException(String message) {
            super(message);
        }
    }
}

class LRNavigator {
    FileNavigator<Map<String, Object>> left;
    FileNavigator<Map<String, Object>> right;
    LRNavigator onSetLeft(String oldPath, String newPath) {
        if (!Objects.equals(oldPath, newPath)) {
            left = null;
        }
        return this;
    }
    LRNavigator onSetRight(String oldPath, String newPath) {
        if (!Objects.equals(oldPath, newPath)) {
            right = null;
        }
        return this;
    }
    public LRNavigator  swap() {
        var left0 = left;
        left = right;
        right = left0;
        return this;
    }
    LRNavigator toCuriositySol(UiController uiController, boolean isRight, boolean isLeft, int sol) {
        try {
            var rfn = new RemoteFileNavigator();
            rfn.loadFromDataStructure(NasaReader.dataStructureFromCuriositySolStarting(sol, rfn.nToLoad));
            String leftPath = rfn.toFirstLoaded().getCurrentPath();
            var leftRfn = rfn.copy();
            String rightPath = rfn.toNext().getCurrentPath();
            var paths = FileLocations.twoPaths(leftPath, rightPath);

            uiController.showInProgressViewsAndThen(paths,
                    () ->  {
                        if (Objects.equals(paths.get(0), leftPath)) {
                            left = leftRfn;
                            right = rfn;
                        } else {
                            right = leftRfn;
                            left = rfn;
                        }
                        uiController.updateRawDataAsync(paths.get(0), paths.get(1));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    LRNavigator toCuriosityLatest(UiController uiController, boolean isRight, boolean isLeft) {
        try {
            var rfn = new RemoteFileNavigator();
            rfn.loadFromDataStructure(NasaReader.dataStructureFromCuriositySolLatest(rfn.nToLoad));
            String leftPath = rfn.toLastLoaded().getCurrentPath();
            var leftRfn = rfn.copy();
            String rightPath = rfn.toPrev().getCurrentPath();
            var paths = FileLocations.twoPaths(leftPath, rightPath);

            uiController.showInProgressViewsAndThen(paths,
                    () ->  {
                        if (Objects.equals(paths.get(0), leftPath)) {
                            left = leftRfn;
                            right = rfn;
                        } else {
                            right = leftRfn;
                            left = rfn;
                        }
                        uiController.updateRawDataAsync(paths.get(0), paths.get(1));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    LRNavigator toPerseveranceSol(UiController uiController, boolean isRight, boolean isLeft, int sol) {
        try {
            var rfn = new RemoteFileNavigatorV2();
            rfn.loadBySol(sol);
//            rfn.loadFromDataStructure(NasaReader.dataStructureFromCuriositySolStarting(sol, rfn.nToLoad));
            String leftPath = rfn.toFirstLoaded().getCurrentPath();
            var leftRfn = rfn.copy();
            String rightPath = rfn.toNext().getCurrentPath();
            var paths = FileLocations.twoPaths(leftPath, rightPath);

            uiController.showInProgressViewsAndThen(paths,
                    () ->  {
                        if (Objects.equals(paths.get(0), leftPath)) {
                            left = leftRfn;
                            right = rfn;
                        } else {
                            right = leftRfn;
                            left = rfn;
                        }
                        uiController.updateRawDataAsync(paths.get(0), paths.get(1));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    LRNavigator toPerseveranceLatest(UiController uiController, boolean isRight, boolean isLeft) {
        try {
            var rfn = new RemoteFileNavigatorV2();
            rfn.loadBySol(rfn.getLatestSol());
//            rfn.loadFromDataStructure(NasaReader.dataStructureFromCuriositySolLatest(rfn.nToLoad));
            String leftPath = rfn.toLastLoaded().getCurrentPath();
            var leftRfn = rfn.copy();
            String rightPath = rfn.toPrev().getCurrentPath();
            var paths = FileLocations.twoPaths(leftPath, rightPath);

            uiController.showInProgressViewsAndThen(paths,
                    () ->  {
                        if (Objects.equals(paths.get(0), leftPath)) {
                            left = leftRfn;
                            right = rfn;
                        } else {
                            right = leftRfn;
                            left = rfn;
                        }
                        uiController.updateRawDataAsync(paths.get(0), paths.get(1));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    LRNavigator navigate(UiController uiController, boolean isRight, boolean isLeft, boolean forwardInTime, int byOneOrTwo, String leftPath, String rightPath) {
        String newLeftPath = leftPath;
        String newRightPath = rightPath;
        System.out.println("---");
        System.out.println("L "+newLeftPath+"\nR "+newRightPath+"\n");
        if (isLeft) {
            try {
                left = newIfNotSuitable(left, newLeftPath);
                for (int i = 0; i < byOneOrTwo; i++) {
                    newLeftPath = (forwardInTime ? left.toNext() : left.toPrev()).getCurrentPath();
                    System.out.println("L " + newLeftPath + "...");
                }
            } catch (NoSuchElementException nsee) {
                // TODO: think out something better
                // load the image from the other pane
                newLeftPath = uiController.rawData.right.pathToLoad;
                left = newIfNotSuitable(left, newLeftPath);
            }
        }
        if (isRight) {
            try {
                right = newIfNotSuitable(right, newRightPath);
                for (int i = 0; i < byOneOrTwo; i++) {
                    newRightPath = (forwardInTime ? right.toNext() : right.toPrev()).getCurrentPath();
                    System.out.println("R " + newRightPath + "...");
                }
            } catch (NoSuchElementException nsee) {
                // TODO: think out something better
                // load the image from the other pane
                newRightPath = uiController.rawData.left.pathToLoad;
                right = newIfNotSuitable(right, newRightPath);
            }
        }
        System.out.println("\nL "+newLeftPath+"\nR "+newRightPath);
        System.out.println("---");
        var uPaths = Arrays.asList(newLeftPath, newRightPath);
        uiController.showInProgressViewsAndThen(uPaths,
                () ->  uiController.updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
        return this;
    }
    FileNavigator<Map<String, Object>> newIfNotSuitable(FileNavigator<Map<String, Object>> fileNavigator, String path) {
        var needRemote = FileLocations.isUrl(path);
        var needRemoteV2 = needRemote && path.contains("/mars2020");
        var needRemoteV1 = needRemote & ! needRemoteV2;
        if (null == fileNavigator
           || needRemoteV1 != (fileNavigator instanceof RemoteFileNavigator)
           || needRemoteV2 != (fileNavigator instanceof RemoteFileNavigatorV2)
           ) {
            fileNavigator = FileNavigatorBase.makeNew(path);
        }
        return fileNavigator;
    }
}
interface FileNavigator<T> {
    FileNavigator<T> toNext();
    FileNavigator<T> toPrev();
    FileNavigator<T>  toFirstLoaded();
    FileNavigator<T>  toLastLoaded();
    T getCurrentValue();
    String getCurrentKey();
    FileNavigator<T> setCurrentKey(String key);
    String toKey(T obj);
    String getPath(T t);
    FileNavigator<T> copy();
    static<T> FileNavigator<T> copy(FileNavigator<T> orig) {
        return orig == null ? null : orig.copy();
    }
    default String getCurrentPath() { return getPath(getCurrentValue()); }
}
abstract class FileNavigatorBase implements FileNavigator<Map<String, Object>> {
    protected NavigableMap<String, Map<String, Object>> nmap = new TreeMap<>();
    protected String currentKey;
    int nToLoad=25;
    public static FileNavigatorBase makeNew(String path) {
        var needRemote = FileLocations.isUrl(path);
        // TODO: move this check to FileLocations or HttpLocations or sth like that
        var needRemoteV2 = needRemote && path.contains("/mars2020");
        FileNavigatorBase res = needRemoteV2 ? new RemoteFileNavigatorV2()
                              : needRemote ? new RemoteFileNavigator()
                              : new LocalFileNavigator();
        res._loadInitial(path);
        return res;
    }
    protected void setFrom(FileNavigatorBase other) {
        nmap.clear();
        nmap.putAll(other.nmap);
        currentKey = other.currentKey;
        nToLoad = other.nToLoad;
    }
    protected void moveWindow(boolean forwardInTime) {
        if (forwardInTime && Objects.equals(currentKey, nmap.lastKey())) {
            _loadHigher(); _cleanupLower();
        }
        if (!forwardInTime && Objects.equals(currentKey, nmap.firstKey())) {
            _loadLower(); _cleanupHigher();
        }
    }
    protected abstract void _loadInitial(String whereFrom);
    protected abstract void _loadHigher();
    protected abstract void _loadLower();
    protected abstract void _onLoadResult(NavigableMap<String, Map<String, Object>> newData);
    protected abstract void _cleanupHigher();
    protected abstract void _cleanupLower();
    protected int _numHigherToLoad() { return nToLoad; }
    protected int _numLowerToLoad() { return nToLoad; }

    @Override
    public FileNavigator<Map<String, Object>> toNext() {
        moveWindow(true);
        if (currentKey == null) {
            currentKey = nmap.firstKey();
        } else {
            currentKey = nmap.higherKey(currentKey);
        }
        return this;
    }
    @Override
    public FileNavigator<Map<String, Object>> toPrev() {
        moveWindow(false);
        if (currentKey == null) {
            currentKey = nmap.lastKey();
        } else {
            currentKey = nmap.lowerKey(currentKey);
        }
        return this;
    }
    @Override
    public FileNavigator<Map<String, Object>> toFirstLoaded() {
        currentKey = nmap.firstKey();
        return this;
    }
    @Override
    public FileNavigator<Map<String, Object>> toLastLoaded() {
        currentKey = nmap.lastKey();
        return this;
    }
    @Override
    public Map<String, Object> getCurrentValue() {
        if (currentKey == null) {
            return null;
        }
        return nmap.get(currentKey);
    }
    @Override
    public String getCurrentKey() { return currentKey; }
    @Override
    public FileNavigator<Map<String, Object>> setCurrentKey(String key) {
        currentKey = key;
        return this;
    }
    @Override
    public String toKey(Map<String, Object> obj) {
        if (obj == null) {
            return null;
        }
        return obj.get("date_taken")+"^"+obj.get("imageid");
    }
}
class LocalFileNavigator extends FileNavigatorBase {
    String currentDirectory = "";

    protected void setFrom(LocalFileNavigator other) {
        currentDirectory = other.currentDirectory;
        super.setFrom(other);
    }
    @Override
    public FileNavigator<Map<String, Object>> copy() {
        LocalFileNavigator res = new LocalFileNavigator();
        res.setFrom(this);
        return res;
    }

    @Override
    protected void _loadInitial(String whereFrom) {
        Path path0 = Paths.get(whereFrom);
        if (Files.isDirectory(path0)) {
            currentKey = null;
            currentDirectory = path0.toString();
        } else {
            currentKey = path0.getFileName().toString();
            path0 = path0.getParent();
            if (path0 == null) {
                path0 = Paths.get(".");
            }
            currentDirectory = path0.toString();
        }
        var res = new TreeMap<String, Map<String, Object>>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path0)) {
            for (Path path: stream) {
                Map<String, Object> m = new TreeMap<>();
                m.put("path",path);
                String fileName = path.getFileName().toString();
                m.put("key", fileName);
                res.put(fileName, m);
            }
            _onLoadResult(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void _onLoadResult(NavigableMap<String, Map<String, Object>> newData) {
        nmap = newData;
    }
    @Override
    public String getPath(Map<String, Object> map) {
        return map == null ? null : map.get("path").toString();
    }
    @Override
    protected void _loadHigher() {
        _loadInitial(Paths.get(currentDirectory, currentKey).toString());
    }
    @Override
    protected void _loadLower() {
        _loadInitial(Paths.get(currentDirectory, currentKey).toString());
    }
    @Override
    protected void _cleanupHigher() {}
    @Override
    protected void _cleanupLower() {}
}

class NasaReaderBase {
    static final String USER_AGENT;
    static {
        //Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:82.0) Gecko/20100101 Firefox/82.0
        String USER_AGENT_LINUX = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:81.0) Gecko/20100101 Firefox/81.0";
        String USER_AGENT_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:82.0) Gecko/20100101 Firefox/82.0";
        String USER_AGENT_MACOSX = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:82.0) Gecko/20100101 Firefox/82.0";
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName == null) {
            osName = "";
        }
        if (osName.startsWith("mac")) {
            USER_AGENT = USER_AGENT_MACOSX;
        } else if (osName.startsWith("lin")) {
            USER_AGENT = USER_AGENT_LINUX;
        } else {
            USER_AGENT = USER_AGENT_WINDOWS;
        }
    }

    static String nasaEncode(String params) {
        return params.replaceAll(",", "%2C").replaceAll(":", "%3A").replaceAll(" ","+");
    }

    static String readUrl(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        setHttpHeaders(conn);
        try {
            try (
                    InputStream connInputStream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connInputStream, StandardCharsets.UTF_8))
            ) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (SocketTimeoutException ste) {
            System.out.println("Disconnecting "+conn+" ...");
            ((HttpURLConnection)conn).disconnect();
            NasaReader.cleanupReading();
            throw ste;
        }
    }

    public static void setHttpHeaders(URLConnection uc) {
        uc.setRequestProperty("User-Agent",USER_AGENT);
        uc.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        uc.setRequestProperty("Accept-Language","en-US,en;q=0.5");
        uc.setRequestProperty("DNT","1");
//        uc.setRequestProperty("Pragma","no-cache");
//        uc.setRequestProperty("Cache-Control","no-cache");
        uc.setRequestProperty("Referer","https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc%2C+date_taken+desc&per_page=50&page=0&mission=msl");
//        uc.setRequestProperty("Cookie","raw_images_filter=order=sol+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc%2C+date_taken+desc&per_page=50&page=0&mission=msl; _ga=GA1.3.113901165.1610160696; _gid=GA1.3.1829179841.1610160696; _gat_GSA_ENOR0=1; _gat_GSA_ENOR1=1; _gat_GSA_ENOR2=1; _ga=GA1.2.113901165.1610160696; _gid=GA1.2.1829179841.1610160696");
//      uc.setRequestProperty("","");
        uc.setConnectTimeout(7000); // 7 sec
    }
}
class NasaReader extends NasaReaderBase {
    static String makeRequest(String parameters) {
        return "https://mars.nasa.gov/api/v1/raw_image_items/?" +
                nasaEncode( parameters );
    }
    static Object dataStructureFromRequest(String parameters) throws IOException {
        System.out.println("dataStructureFromRequest("+parameters+")");
        String url = makeRequest(parameters);
        System.out.println("dataStructureFromRequest url = "+url);
        String json = readUrl(new URL(url));
        Object res = JsonDiy.jsonToDataStructure(json);
        return res;
    }
    static Object dataStructureFromImageId(String imageId) throws IOException {
        return dataStructureFromRequest(
                "order=sol asc,date_taken asc,instrument_sort asc,sample_type_sort asc" +
                        "&per_page=10" +
                        "&page=0" +
                        "&condition_1=" + imageId + ":imageid:eq" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }

    static Object dataStructureMrlMatchesFromImageId(String imageId) throws IOException {
        StringBuilder sbPairId = new StringBuilder(imageId.substring(0, MastcamPairFinder.pairPrefixLength));
        sbPairId.setCharAt(5,(char)((int)'R'^(int)'L'^(int)sbPairId.charAt(5)));
        String pairId = sbPairId.toString();
        return dataStructureFromRequest(
                "order=sol asc,date_taken asc,instrument_sort asc,sample_type_sort asc" +
                        "&per_page=10" +
                        "&page=0" +
                        "&condition_1=" + pairId + ":imageid:gt" +
                        "&condition_2=" + pairId + "z:imageid:lt" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromCuriositySolStarting(int curiositySol, int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=sol asc,date_taken asc,instrument_sort asc,sample_type_sort asc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=msl:mission" +
                        "&condition_2=" + curiositySol + ":sol:gte" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromCuriositySolLatest(int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=sol desc,date_taken desc,instrument_sort desc,sample_type_sort desc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=msl:mission" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromDateStarting(String date, int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=sol asc,date_taken asc,instrument_sort asc,sample_type_sort asc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=msl:mission" +
                        "&condition_2=" + date + ":date_taken:gte" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromDateEnding(String date, int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=sol desc,date_taken desc,instrument_sort desc,sample_type_sort desc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=msl:mission" +
                        "&condition_2=" + date + ":date_taken:lte" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }

    public static void setHttpHeaders(URLConnection uc) {
        uc.setRequestProperty("User-Agent",USER_AGENT);
        uc.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        uc.setRequestProperty("Accept-Language","en-US,en;q=0.5");
        uc.setRequestProperty("DNT","1");
//        uc.setRequestProperty("Pragma","no-cache");
//        uc.setRequestProperty("Cache-Control","no-cache");
        uc.setRequestProperty("Referer","https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc%2C+date_taken+desc&per_page=50&page=0&mission=msl");
//        uc.setRequestProperty("Cookie","raw_images_filter=order=sol+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc%2C+date_taken+desc&per_page=50&page=0&mission=msl; _ga=GA1.3.113901165.1610160696; _gid=GA1.3.1829179841.1610160696; _gat_GSA_ENOR0=1; _gat_GSA_ENOR1=1; _gat_GSA_ENOR2=1; _ga=GA1.2.113901165.1610160696; _gid=GA1.2.1829179841.1610160696");
//      uc.setRequestProperty("","");
        uc.setConnectTimeout(7000); // 7 sec
    }

    /**
     * For an unknown reason, a "java.net.ConnectException: Connection timed out"
     * happens once in a while, and to make (reused?) HttpURLConnection-s work again,
     * we read from several different URLs. Sometimes this works, sometimes not.
     */
    public static void cleanupReading() {
        System.out.println("cleanup reading:");
        for (String url : Arrays.asList("https://mars.nasa.gov/favicon-16x16.png",
                                        "https://mars.nasa.gov/assets/facebook_icon@2x.png",
                                        "https://mars.nasa.gov/assets/twitter_icon@2x.png")
        ) {
            try {
                System.out.println("reading " + url);
                HttpURLConnection uc2 = (HttpURLConnection) new URL(url).openConnection();
                NasaReader.setHttpHeaders(uc2);
                uc2.setConnectTimeout(3000); // 3 sec
                try {
                    ImageIO.read(uc2.getInputStream());
                    System.out.println("url " + url + " read successfully");
                    return;
                } catch (IOException ee) {
                    System.out.println("url was not read: "+url);
                    ee.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("could not read "+url+" :");
                e.printStackTrace();
            }
        }
    }
}
class NasaReaderV2 extends NasaReaderBase {
    static String makeRequest(String parameters) {
        return "https://mars.nasa.gov/rss/api/?feed=raw_images" +
                "&category=mars2020" +
                "&feedtype=json" +
                "&num=100&" +
                parameters;
//                nasaEncode( parameters );
    }
    static Object dataStructureFromRequest(String parameters) throws IOException {
        System.out.println("dataStructureFromRequest("+parameters+")");
        String url = makeRequest(parameters);
        System.out.println("dataStructureFromRequest url = "+url);
        String json = readUrl(new URL(url));
        Object res = JsonDiy.jsonToDataStructure(json);
        return res;
    }
    static Object dataStructureForLatestSol() throws IOException {
        String url = "https://mars.nasa.gov/rss/api/?feed=raw_images&category=mars2020&feedtype=json&latest=true";
        String json = readUrl(new URL(url));
        Object res = JsonDiy.jsonToDataStructure(json);
        return res;
    }

}

class RemoteFileNavigatorV2 extends FileNavigatorBase {
    @Override
    public String getPath(Map<String, Object> stringObjectMap) {
        String res =
                Optional
                .ofNullable(stringObjectMap)
                .map(map -> map.get("image_files"))
                .filter(o -> o instanceof Map)
                .map(map -> ((Map) map).get("full_res"))
                .map(Object::toString)
                .orElse(null);
        return res;
    }
    @Override
    public FileNavigator<Map<String, Object>> copy() {
        RemoteFileNavigatorV2 res = new RemoteFileNavigatorV2();
        res.setFrom(this);
        return res;
    }

    void loadFromDataStructure(Object jsonObject) {
        try {
            List<Object> list = (List<Object>) JsonDiy.get(jsonObject, "images");
            list.stream().forEach( o -> {
                if (o instanceof Map) {
                    Map m = (Map) o;
                    String date = m.get("date_taken_mars").toString();
                    String id = m.get("imageid").toString();
                    // TODO: sol^mars_date^id
                    String key = date + "^" + id;
                    nmap.put(key, m);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    void loadBySol(Object sol) throws IOException {
        int page=0;
        do {
            String params = "page=" + page + "&=,,,,&order=sol%20desc&condition_2=" + sol + ":sol:gte&condition_3=" + sol + ":sol:lte&extended=sample_type::full,";
            Object jsonObject = NasaReaderV2.dataStructureFromRequest(params);
            loadFromDataStructure(jsonObject);
            int per_page = JsonDiy.getInt(jsonObject,"per_page");
            int res_page = JsonDiy.getInt(jsonObject,"page");
            int total_results = JsonDiy.getInt(jsonObject,"total_results");
            if (total_results <= per_page * (1+res_page)) {
                break;
            }
            page++;
        } while (true);
    }
    int getLatestSol() throws IOException {
        Object jsonObject = NasaReaderV2.dataStructureForLatestSol();
        return JsonDiy.getInt(jsonObject,"latest_sol");
    }
    @Override
    protected void _loadInitial(String whereFrom) {
        try {
            String fname = FileLocations.getFileNameNoExt(whereFrom);
            String imageId = fname.substring(0, 1+fname.lastIndexOf("J"));
            Object sol = solFromPerseveranceImageId(imageId);
            loadBySol(sol);
            currentKey = nmap.keySet().stream()
                    .filter(k -> k.endsWith(imageId))
                    .findFirst()
                    .orElseGet(() -> nmap.firstKey());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void _loadHigher() {
        try {
            String imageId = FileLocations.getFileNameNoExt(getCurrentPath());
            Object sol = solFromPerseveranceImageId(imageId) + 1;
            loadBySol(sol);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void _loadLower() {
        try {
            String imageId = FileLocations.getFileNameNoExt(getCurrentPath());
            Object sol = solFromPerseveranceImageId(imageId) - 1;
            loadBySol(sol);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void _onLoadResult(NavigableMap<String, Map<String, Object>> newData) {

    }

    static String getSolPrefix(String key) {
        return key.substring(0, 9);
    }

    // TODO: remove unused keys from nmap
    @Override
    protected void _cleanupHigher() {
        String lastSolPrefix = getSolPrefix(nmap.lastKey());
        String currentSolPrefix = getSolPrefix(currentKey);
        if (lastSolPrefix.compareTo(currentSolPrefix) > 0) {
            var higherAll = nmap.tailMap(currentSolPrefix, false);
            if(nmap.size() - higherAll.size() > 5) {
                higherAll.clear();
            }
        }
    }

    // TODO: remove unused keys from nmap
    @Override
    protected void _cleanupLower() {
        //NOTE: the current logic is that when we come to the last
        //image from Sol N, this code removes images from Sol N-1
        String firstSolPrefix = getSolPrefix(nmap.firstKey());
        String currentSolPrefix = getSolPrefix(currentKey);
        if (firstSolPrefix.compareTo(currentSolPrefix) < 0) {
            var lowerAll = nmap.headMap(currentSolPrefix, false);
            if(nmap.size() - lowerAll.size() > 5) {
                lowerAll.clear();
            }
        }
    }
    static Integer solFromPerseveranceImageId(String imageId) {
        return Integer.valueOf(imageId.substring(4,8));
    }
}
class RemoteFileNavigator extends FileNavigatorBase {
//    protected void setFrom(LocalFileNavigator other) {
////        xxx = other.xxx;
//        super.setFrom(other);
//    }
    @Override
    public FileNavigator<Map<String, Object>> copy() {
        RemoteFileNavigator res = new RemoteFileNavigator();
        res.setFrom(this);
        return res;
    }

    void loadFromDataStructure(Object jsonObject) {
        try {
            List<Object> list = (List<Object>) JsonDiy.get(jsonObject, "items");
            list.stream().forEach( o -> {
                if (o instanceof Map) {
                    Map m = (Map) o;
                    String date = m.get("date_taken").toString();
                    String id = m.get("imageid").toString();
                    nmap.put(date+"^"+id, m);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    @Override
    public String getPath(Map<String, Object> stringObjectMap) {
        if (stringObjectMap == null) {
            return null;
        }
        var res = stringObjectMap.get("https_url");
        if (res == null) {
            res = stringObjectMap.get("url");
        }
        return res == null ? null : res.toString();
    }
    @Override
    protected void _loadInitial(String whereFrom) {
        try {
            loadFromDataStructure(NasaReader.dataStructureFromImageId(FileLocations.getFileNameNoExt(whereFrom)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentKey = nmap.firstKey();
    }
    @Override
    protected void _loadHigher() {
        String date = (String) JsonDiy.get(getCurrentValue(),"date_taken");
        try {
            loadFromDataStructure(NasaReader.dataStructureFromDateStarting(date, nToLoad));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void _onLoadResult(NavigableMap<String, Map<String, Object>> newData) {

    }

    @Override
    protected void _loadLower() {
        String date = (String) JsonDiy.get(getCurrentValue(),"date_taken");
        try {
            loadFromDataStructure(NasaReader.dataStructureFromDateEnding(date, nToLoad));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void _cleanupHigher() {
        var higherAll = nmap.tailMap(currentKey, false);
        if (higherAll.size() > nToLoad) {
            String key = currentKey;
            for (int i=0; i<nToLoad; i++) {
                key = higherAll.higherKey(key);
            }
            var toDelete = higherAll.tailMap(key, false);
            toDelete.clear();
        }
    }

    @Override
    protected void _cleanupLower() {
        var lowerAll = nmap.headMap(currentKey, false);
        if (lowerAll.size() > nToLoad) {
            String key = currentKey;
            for (int i=0; i<nToLoad; i++) {
                key = lowerAll.lowerKey(key);
            }
            var toDelete = lowerAll.headMap(key, false);
            toDelete.clear();
        }
    }
}

class ColorCorrection {
    final List<ColorCorrectionAlgo> algos;
    final CustomStretchRgbParameters customStretchRgbParameters;

    public enum Command{SHOW, GET_RANGE}

    public ColorCorrection copyWith(List<ColorCorrectionAlgo> algos) {
        return new ColorCorrection(algos, this.customStretchRgbParameters);
    }
    public ColorCorrection copyWith(CustomStretchRgbParameters customStretchRgbParameters) {
        return new ColorCorrection(this.algos, customStretchRgbParameters);
    }
    public ColorCorrection(List<ColorCorrectionAlgo> algos, CustomStretchRgbParameters customStretchRgbParameters) {
        this.algos = Collections.unmodifiableList(new ArrayList<>(algos));
        this.customStretchRgbParameters = customStretchRgbParameters.copy();
    }
    public List<ColorCorrectionAlgo> getAlgos() {
        return algos;
    }
    public String getShortDescription() {
        String res = algos.stream()
                .filter(x -> x != ColorCorrectionAlgo.DO_NOTHING)
                .map(ColorCorrectionAlgo::shortName)
                .collect(Collectors.joining(","));
        return res;
    }
    BufferedImage doColorCorrection(BufferedImage image, Command command) {
        BufferedImage res = image;
        loop:
        for (ColorCorrectionAlgo algo : algos) {
            switch (algo) {
                default:
                case DO_NOTHING:
                    break;
                case STRETCH_CONTRAST_RGB_RGB:
                    res = ColorBalancer.stretchColorsRgb(res, true);
                    break;
                case STRETCH_CONTRAST_RGB_V:
                    res = ColorBalancer.stretchColorsRgb(res, false);
                    break;
                case STRETCH_CONTRAST_RGB_RGB3:
                    if (command == Command.GET_RANGE) {
                        break loop;
                    } else {
                        res = ColorBalancer.stretchColorsRgb(res, customStretchRgbParameters);
                    }
                    break;
                case STRETCH_CONTRAST_HSV_S:
                    res = HSVColorBalancer.balanceColors(res, false, true, false);
                    break;
                case STRETCH_CONTRAST_HSV_V:
                    res = HSVColorBalancer.balanceColors(res, false, false, true);
                    break;
                case STRETCH_CONTRAST_HSV_SV:
                    res = HSVColorBalancer.balanceColors(res, false, true, true);
                    break;
                case GAMMA_DECODE_2_4:
                    res = GammaColorBalancer.balanceColors(res, 2.4);
                    break;
                case GAMMA_DECODE_2_2:
                    res = GammaColorBalancer.balanceColors(res, 2.2);
                    break;
                case GAMMA_DECODE_2_0:
                    res = GammaColorBalancer.balanceColors(res, 2.0);
                    break;
                case GAMMA_DECODE_1_8:
                    res = GammaColorBalancer.balanceColors(res, 1.8);
                    break;
                case GAMMA_DECODE_1_6:
                    res = GammaColorBalancer.balanceColors(res, 1.6);
                    break;
                case GAMMA_DECODE_1_4:
                    res = GammaColorBalancer.balanceColors(res, 1.4);
                    break;
                case GAMMA_DECODE_1_2:
                    res = GammaColorBalancer.balanceColors(res, 1.2);
                    break;
                case GAMMA_ENCODE_2_4:
                    res = GammaColorBalancer.balanceColors(res, 1 / 2.4);
                    break;
                case GAMMA_ENCODE_2_2:
                    res = GammaColorBalancer.balanceColors(res, 1 / 2.2);
                    break;
                case GAMMA_ENCODE_2_0:
                    res = GammaColorBalancer.balanceColors(res, 1 / 2.0);
                    break;
                case GAMMA_ENCODE_1_8:
                    res = GammaColorBalancer.balanceColors(res, 1 / 1.8);
                    break;
                case GAMMA_ENCODE_1_6:
                    res = GammaColorBalancer.balanceColors(res, 1 / 1.6);
                    break;
                case GAMMA_ENCODE_1_4:
                    res = GammaColorBalancer.balanceColors(res, 1 / 1.4);
                    break;
                case GAMMA_ENCODE_1_2:
                    res = GammaColorBalancer.balanceColors(res, 1 / 1.2);
                    break;
                case UNGLARE1:
                    res = BilinearDeglareWhite.unglare(res);
            }
        }
        return res;
    }
}

enum ColorCorrectionAlgo {
    DO_NOTHING("as is", ""),
    STRETCH_CONTRAST_RGB_RGB("stretch R,G,B separately in RGB space", "sRGB"),
    STRETCH_CONTRAST_RGB_V("stretch R,G,B together in RGB space", "sRGB2"),
    STRETCH_CONTRAST_RGB_RGB3("stretch R,G,B in RGB space, custom parameters", "sRGB3"),
    STRETCH_CONTRAST_HSV_V("stretch V in HSV space", "sHSVv"),
    STRETCH_CONTRAST_HSV_S("stretch S in HSV space", "sHSVs"),
    STRETCH_CONTRAST_HSV_SV("stretch S & V in HSV space", "sHSV"),
    //  γ<1 is sometimes called an encoding gamma (γ-compression), γ>1 is called a decoding gamma (γ-expansion)
    GAMMA_DECODE_2_4("gamma decode, γ=2.4", "gd24"),
    GAMMA_DECODE_2_2("gamma decode, γ=2.2", "gd22"),
    GAMMA_DECODE_2_0("gamma decode, γ=2.0", "gd20"),
    GAMMA_DECODE_1_8("gamma decode, γ=1.8", "dg18"),
    GAMMA_DECODE_1_6("gamma decode, γ=1.6", "gd16"),
    GAMMA_DECODE_1_4("gamma decode, γ=1.4", "gd14"),
    GAMMA_DECODE_1_2("gamma decode, γ=1.2", "gd12"),
    GAMMA_ENCODE_2_4("gamma encode, γ=1/2.4", "ge24"),
    GAMMA_ENCODE_2_2("gamma encode, γ=1/2.2", "ge22"),
    GAMMA_ENCODE_2_0("gamma encode, γ=1/2.0", "ge20"),
    GAMMA_ENCODE_1_8("gamma encode, γ=1/1.8", "ge18"),
    GAMMA_ENCODE_1_6("gamma encode, γ=1/1.6", "ge16"),
    GAMMA_ENCODE_1_4("gamma encode, γ=1/1.4", "ge14"),
    GAMMA_ENCODE_1_2("gamma encode, γ=1/1.2", "ge12"),
    UNGLARE1("unglare, bilinear","ugb");

    final String name;
    final String shortName;

    ColorCorrectionAlgo(String userVisibleName, String shortName) {
        this.name = (!shortName.isEmpty() ? shortName + ": " : "")+userVisibleName;
        this.shortName = shortName;
    }

    @Override
    public String toString() { return name; }
    public String shortName() { return shortName; }
}

class ColorCorrectionModeChooser extends JComboBox<ColorCorrectionAlgo> {
    static ColorCorrectionAlgo[] modes = ColorCorrectionAlgo.values();
    public ColorCorrectionModeChooser(Consumer<ColorCorrectionAlgo> valueListener) {
        super(modes);
        setValue(ColorCorrectionAlgo.DO_NOTHING);
        setMaximumRowCount(modes.length);
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((ColorCorrectionAlgo) itemEvent.getItem());
            }
        });
    }
    public void setValue(ColorCorrectionAlgo algo) {
        setSelectedItem(algo);
    }
}

class ColorRangeAndFlagsChooser extends JPanel {
    final UiEventListener uiEventListener;
    final boolean isRight;
    CustomStretchRgbParameters old;
    CustomStretchRgbParameters usedNow;
    CustomStretchRgbParameters proposed;
    final JLabel lblPleaseSelectSrgb3;
    final DigitalZoomControl<Integer, OffsetWrapper> dcR0;
    final DigitalZoomControl<Integer, OffsetWrapper> dcG0;
    final DigitalZoomControl<Integer, OffsetWrapper> dcB0;
    final DigitalZoomControl<Integer, OffsetWrapper> dcR1;
    final DigitalZoomControl<Integer, OffsetWrapper> dcG1;
    final DigitalZoomControl<Integer, OffsetWrapper> dcB1;
    final JCheckBox cbPerChannel;
    final JCheckBox cbSaturation;
    final JButton buttonSet;

    final Color normalButtonColor;
    final Color highlightedButtonColor = new Color(250,127,127);
    private final String pleaseSelectSrgb3Norm;
    private final String pleaseSelectSrgb3Hili;

    ColorRangeAndFlagsChooser(UiEventListener uiEventListener, boolean isRight) {
        this.uiEventListener = uiEventListener;
        this.isRight = isRight;
        this.proposed = CustomStretchRgbParameters.newFullRange();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        {
            JLabel title = new JLabel("<html><center><h3>Custom RGB Range</h3></center></html>", SwingConstants.CENTER);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.add(title);
            title.setToolTipText("Parameters for the sRGB3 effect");
        }
        {
            pleaseSelectSrgb3Norm = "<html><center>To use these, please select<br/>the sRGB3 effect in the list above</center></html>";
            pleaseSelectSrgb3Hili = "<html><center><font color='red'>To use these, please select<br/>the sRGB3 effect in the list above</font></center></html>";
            lblPleaseSelectSrgb3 = new JLabel(pleaseSelectSrgb3Norm, SwingConstants.CENTER);
            lblPleaseSelectSrgb3.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.add(lblPleaseSelectSrgb3);
        }
        this.add(dcR0 = new DigitalZoomControl<Integer, OffsetWrapper>().init("R:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.minR=i; whenUpdated();}));
        this.add(dcG0 = new DigitalZoomControl<Integer, OffsetWrapper>().init("G:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.minG=i; whenUpdated();}));
        this.add(dcB0 = new DigitalZoomControl<Integer, OffsetWrapper>().init("B:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.minB=i; whenUpdated();}));
        this.add(dcR1 = new DigitalZoomControl<Integer, OffsetWrapper>().init("R:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.maxR=i; whenUpdated();}));
        this.add(dcG1 = new DigitalZoomControl<Integer, OffsetWrapper>().init("G:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.maxG=i; whenUpdated();}));
        this.add(dcB1 = new DigitalZoomControl<Integer, OffsetWrapper>().init("B:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.maxB=i; whenUpdated();}));
        this.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent ancestorEvent) {
                whenShown();
            }
            @Override
            public void ancestorRemoved(AncestorEvent ancestorEvent) {
            }
            @Override
            public void ancestorMoved(AncestorEvent ancestorEvent) {
            }
        });
        {
            cbPerChannel = new JCheckBox("Per Channel");
            this.add(cbPerChannel);
            cbPerChannel.addActionListener(
                    e -> { proposed.isPerChannel = cbPerChannel.isSelected(); whenUpdated(); }
            );
            cbPerChannel.setToolTipText("Stretch Red/Green/Blue channels independently");
        }
        {
            cbSaturation = new JCheckBox("Saturation");
            this.add(cbSaturation);
            cbSaturation.addActionListener(
                    e -> { proposed.isSaturated = cbSaturation.isSelected(); whenUpdated(); }
            );
            cbSaturation.setToolTipText("Too bright pixels must remain white or \"wrap around\" to dark?");
        }
        {
            JButton button = new JButton("Get from Viewport");
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> actionCalculateViewportColorRange());
            button.setToolTipText("First zoom/resize/scroll the window to exclude pixels that are too dark/too bright");
            this.add(button);
        }
        {
            JButton button = new JButton("Reset to Original");
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> actionResetToOriginal());
            button.setToolTipText("The \"original\" values are saved when this dialog opens");
            this.add(button);
        }
        {
            buttonSet = new JButton("Set Custom Color Range");
            buttonSet.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonSet.addActionListener(e -> actionSetCustomStretchRgbParameters());
            this.add(buttonSet);
            normalButtonColor = buttonSet.getBackground();
            buttonSet.setToolTipText("Applying incomplete changes is meaningless, so first choose parameters, then click this");
        }
    }
    private void whenShown() {
        usedNow = uiEventListener.getCurrentCustomStretchRgbParameters(isRight).copy();
        old = usedNow.copy();
        proposed = old.copy();
        customStretchRgbParametersToControls(proposed);
        whenUpdated();
    }
    private void whenUpdated() {
        var isChanged = !usedNow.equals(proposed);
        buttonSet.setBackground(isChanged ? highlightedButtonColor : normalButtonColor);
        buttonSet.setOpaque(true);
//        System.out.println("whenUpdated: isChanged="+isChanged+";");
//        System.out.println("     old="+old);
//        System.out.println(" usedNow="+usedNow);
//        System.out.println("proposed="+proposed);
//        System.out.println(" from_ui="+uiEventListener.getCurrentCustomStretchRgbParameters(isRight));
        var isSrgb3Used = uiEventListener
                .getDisplayParameters()
                .getColorCorrection(isRight).getAlgos()
                .contains(ColorCorrectionAlgo.STRETCH_CONTRAST_RGB_RGB3);
        var isTrivial = proposed.colorRange.isFullRange()
                     || proposed.colorRange.isEmpty();
        lblPleaseSelectSrgb3.setText(
                  isSrgb3Used || isTrivial
                ? pleaseSelectSrgb3Norm
                : pleaseSelectSrgb3Hili
        );
    }
    public void notifyOfUpdates() {
        usedNow = uiEventListener.getCurrentCustomStretchRgbParameters(isRight).copy();
        whenUpdated();
    }
    private void actionCalculateViewportColorRange() {
        ColorRange cr = uiEventListener.getViewportColorRange(isRight);
        colorRangeToControls(cr);
        proposed.colorRange = cr.copy();
        whenUpdated();
    }
    private void customStretchRgbParametersToControls(CustomStretchRgbParameters param) {
        ColorRange cr = param.colorRange;
        colorRangeToControls(cr);
        cbPerChannel.setSelected(param.isPerChannel);
        cbSaturation.setSelected(param.isSaturated);
    }
    private void colorRangeToControls(ColorRange cr) {
        dcR0.setValueAndText(cr.minR);
        dcG0.setValueAndText(cr.minG);
        dcB0.setValueAndText(cr.minB);
        dcR1.setValueAndText(cr.maxR);
        dcG1.setValueAndText(cr.maxG);
        dcB1.setValueAndText(cr.maxB);
    }
    private void actionResetToOriginal() {
        proposed = old.copy();
        customStretchRgbParametersToControls(proposed);
        whenUpdated();
    }
    private void actionSetCustomStretchRgbParameters() {
        uiEventListener.setCustomStretchRgbParameters(proposed.copy(), isRight);
        usedNow = proposed.copy();
        whenUpdated();
    }
    CustomStretchRgbParameters getCustomStretchRgbParameters() {
        return proposed.copy();
    }
}
class ColorCorrectionPane extends JPanel {
    final UiEventListener uiEventListener;
    final List<ColorCorrectionModeChooser> lChoosers = new ArrayList<>();
    final List<ColorCorrectionModeChooser> rChoosers = new ArrayList<>();
    final ImageResamplingModeChooser lImageResamplingModeChooser;
    final ImageResamplingModeChooser rImageResamplingModeChooser;
    final ColorRangeAndFlagsChooser lColorRangeChooser;
    final ColorRangeAndFlagsChooser rColorRangeChooser;

    public ColorCorrectionPane(UiEventListener uiEventListener) {
        this.uiEventListener = uiEventListener;
        lColorRangeChooser = new ColorRangeAndFlagsChooser(uiEventListener, false);
        rColorRangeChooser = new ColorRangeAndFlagsChooser(uiEventListener, true);

        GridBagLayout gbl = new GridBagLayout();

        {
            var text = new JLabel("Apply effects to each image, in this sequence:");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(text, gbc);
            this.add(text);
        }
        for (int col = 0; col<2; col++) {
            final boolean isLeft = (col & 1) == 0;
            for (int row = 0; row < 5; row++) {
                ColorCorrectionModeChooser chooser = new ColorCorrectionModeChooser(x -> {
                    if (isLeft) {
                        javax.swing.SwingUtilities.invokeLater( () -> {
                            uiEventListener.lColorCorrectionChanged(getColorCorrection(lChoosers, lColorRangeChooser));
                            lColorRangeChooser.notifyOfUpdates();
                        });
                    } else {
                        javax.swing.SwingUtilities.invokeLater( () -> {
                            uiEventListener.rColorCorrectionChanged(getColorCorrection(rChoosers, rColorRangeChooser));
                            rColorRangeChooser.notifyOfUpdates();
                        });
                    }
                });
                (isLeft ? lChoosers : rChoosers).add(chooser);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1.0;
                gbc.weighty = 1.0;
                gbc.gridx = col*3;
                gbc.gridy = row+1;
                gbl.setConstraints(chooser, gbc);
                this.add(chooser);
            }
        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 7;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
            var text = new JLabel("Image Scaling Interpolation:");
            row.add(text);
            row.add(lImageResamplingModeChooser = new ImageResamplingModeChooser(v -> uiEventListener.lImageResamplingModeChanged(v)));
            row.add(rImageResamplingModeChooser = new ImageResamplingModeChooser(v -> uiEventListener.rImageResamplingModeChanged(v)));
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 0;
            gbc.gridy = 8;
            gbl.setConstraints(lColorRangeChooser, gbc);
            this.add(lColorRangeChooser);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 3;
            gbc.gridy = 8;
            gbl.setConstraints(rColorRangeChooser, gbc);
            this.add(rColorRangeChooser);
        }
        this.setLayout(gbl);
    }

    void showDialogIn(JFrame mainFrame) {
        JOptionPane.showMessageDialog(mainFrame, this,"Color Correction", JOptionPane.PLAIN_MESSAGE);
    }

    ColorCorrection getColorCorrection(List<ColorCorrectionModeChooser> choosers, ColorRangeAndFlagsChooser colorRangeChooser) {
        var algos = choosers.stream().map(c -> (ColorCorrectionAlgo)c.getSelectedItem()).collect(Collectors.toList());
        return new ColorCorrection(algos, colorRangeChooser.getCustomStretchRgbParameters());
    }
    ColorCorrectionPane setColorCorrectionValue(boolean isRight, ColorCorrection colorCorrection) {
        var algos = colorCorrection.getAlgos();
        int nAlgos = algos.size();
        var choosers = isRight ? rChoosers : lChoosers;
        for (int i=0, N=choosers.size(); i<N; i++) {
            choosers.get(i).setSelectedItem(
                i < nAlgos ? algos.get(i) : ColorCorrectionAlgo.DO_NOTHING
            );
        }
        return this;
    }
    ColorCorrectionPane setImageResamplingModeValue(boolean isRight, ImageResamplingMode imageResamplingMode) {
        if (isRight) {
            rImageResamplingModeChooser.setValue(imageResamplingMode);
        } else {
            lImageResamplingModeChooser.setValue(imageResamplingMode);
        }
        return this;
    }
}

class ColorBalancer {
    static ColorRange getColorRangeFromImage(Rectangle rectangle, BufferedImage src) {
        int iStart = (int) rectangle.getX();
        int iFinal = (int) rectangle.getMaxX();
        int jStart = (int) rectangle.getY();
        int jFinal = (int) rectangle.getMaxY();
        return getColorRangeFromImage(iStart, iFinal, jStart, jFinal, src);
    }
    static ColorRange getColorRangeFromImage(int iStart, int iFinal, int jStart, int jFinal, BufferedImage src) {
        ColorRange cr = ColorRange.newEmptyRange();
        for (int j = jStart; j < jFinal; j++) {
            for (int i = iStart; i < iFinal; i++) {
                int color = src.getRGB(i, j);
                cr.minR = Math.min(cr.minR, 0xff & (color >> 16));
                cr.minG = Math.min(cr.minG, 0xff & (color >> 8));
                cr.minB = Math.min(cr.minB, 0xff & (color));
                cr.maxR = Math.max(cr.maxR, 0xff & (color >> 16));
                cr.maxG = Math.max(cr.maxG, 0xff & (color >> 8));
                cr.maxB = Math.max(cr.maxB, 0xff & (color));
            }
        }
        return cr;
    }
    public static BufferedImage stretchColorsRgb(BufferedImage src, boolean perChannel) {
        try {
            final int M = 16;
            int width = src.getWidth();
            int height = src.getHeight();
            ColorRange cr = getColorRangeFromImage(M, width - M, M, height - M, src);
            BufferedImage res = stretchColorsUsingRgbRange(cr, src, perChannel, false);
            return res;
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }
    public static BufferedImage stretchColorsRgb(BufferedImage src, CustomStretchRgbParameters customStretchRgbParameters) {
        if (customStretchRgbParameters.colorRange.isEmpty()
         || customStretchRgbParameters.colorRange.isFullRange()
        ) {
            System.out.println("stretchColorsRgb: full or empty range");
            return src;
        }
        boolean perChannel = customStretchRgbParameters.isPerChannel;
        ColorRange cr = customStretchRgbParameters.colorRange;
        boolean saturate = customStretchRgbParameters.isSaturated;
        try {
            return stretchColorsUsingRgbRange(cr, src, perChannel, saturate);
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }

    static BufferedImage stretchColorsUsingRgbRange(ColorRange cr, BufferedImage src, boolean perChannel, boolean saturate) {
        int width = src.getWidth();
        int height = src.getHeight();
        int minR = cr.minR, minG = cr.minG, minB = cr.minB, maxR = cr.maxR, maxG = cr.maxG, maxB = cr.maxB;
        int minV = Math.min(minR, Math.min(minG, minB));
        int maxV = Math.max(maxR, Math.max(maxG, maxB));
        int dv = maxV - minV;

        int dr = maxR - minR;
        int dg = maxG - minG;
        int db = maxB - minB;
        //int dw = Math.max(dr, Math.max(dg, db));
        System.out.println("balanceColors("+//src+
                ", perChannel="+perChannel+")");
        System.out.println("min: " + minR + " " + minG + " " + minB);
        System.out.println("max: " + maxR + " " + maxG + " " + maxB);
        System.out.println("d: " + dr + " " + dg + " " + db);

        if (!perChannel) {
            maxR = maxG = maxB = maxV;
            minR = minG = minB = minV;
            dr = dg = db = dv;
        }

        var res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        if (saturate) {
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int color = src.getRGB(i, j);
                    int r = 0xff & (color >> 16);
                    int g = 0xff & (color >> 8);
                    int b = 0xff & (color);
                    int r1 = Math.max(0, Math.min(255, (r - minR) * 255 / dr));
                    int g1 = Math.max(0, Math.min(255, (g - minG) * 255 / dg));
                    int b1 = Math.max(0, Math.min(255, (b - minB) * 255 / db));
                    int color1 = (r1 << 16) | (g1 << 8) | (b1);
                    res.setRGB(i, j, color1);
                }
            }
        } else {
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int color = src.getRGB(i, j);
                    int r = 0xff & (color >> 16);
                    int g = 0xff & (color >> 8);
                    int b = 0xff & (color);
                    int r1 = 0xff & (r - minR) * 255 / dr;
                    int g1 = 0xff & (g - minG) * 255 / dg;
                    int b1 = 0xff & (b - minB) * 255 / db;
                    int color1 = (r1 << 16) | (g1 << 8) | (b1);
                    res.setRGB(i, j, color1);
                }
            }
        }
        return res;
    }
}

class HSVColorBalancer {
    public static BufferedImage balanceColors(BufferedImage src, boolean stretchH, boolean stretchS, boolean stretchV) {
//        if (ImageAndPath.isDummyImage(src)) {
//            return src;
//        }
        try {
            final int M = 16;
            float minH = Float.MAX_VALUE, minS = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxH = Float.MIN_VALUE, maxS = Float.MIN_VALUE, maxV = Float.MIN_VALUE;
            int width = src.getWidth();
            int height = src.getHeight();
            float avgH = 0, avgS = 0, avgV = 0;
            float[] hsv = { 0.f, 0.f, 0.f };
            for (int j = M; j < height - M; j++) {
                for (int i = M; i < width - M; i++) {
                    int color = src.getRGB(i, j);
                    hsv = Color.RGBtoHSB(0xff & (color >> 16), 0xff & (color >> 8), 0xff & (color), hsv);
                    minH = Math.min(minH, hsv[0]);
                    minS = Math.min(minS, hsv[1]);
                    minV = Math.min(minV, hsv[2]);
                    maxH = Math.max(maxH, hsv[0]);
                    maxS = Math.max(maxS, hsv[1]);
                    maxV = Math.max(maxV, hsv[2]);
                    avgH += hsv[0];
                    avgS += hsv[1];
                    avgV += hsv[2];
                }
            }
            {
                long N = (height - 2 * M) * (width - 2 * M);
                avgH /= N;
                avgS /= N;
                avgV /= N;
            }
            float dh = maxH - minH;
            float ds = maxS - minS;
            float dv = maxV - minV;
            System.out.println("min:  " + minH + " " + minS + " " + minV);
            System.out.println("max:  " + maxH + " " + maxS + " " + maxV);
            System.out.println("avg:  " + avgH + " " + avgS + " " + avgV);
            System.out.println("  d:  " + dh + " " + ds + " " + dv);
            System.out.println("avgd: " + (avgH - minH) + " " + (avgS - minS) + " " + (avgV - minV));
            var res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int color = src.getRGB(i, j);
                    hsv = Color.RGBtoHSB(0xff & (color >> 16), 0xff & (color >> 8), 0xff & (color), hsv);
//                    int r = 0xff & (color >> 16);
//                    int g = 0xff & (color >> 8);
//                    int b = 0xff & (color);
////                    int r1 = (int) (r * avgB*avgG/avgR/255);//r * 255 / maxR;//(r - minR) * 255 / dr;
////                    int g1 = (int) (g * avgB*avgR/avgG/255); //g * 255 / maxG;//(g - minG) * 255 / dg;
////                    int b1 = (int) (b * avgG*avgR/avgB/255); //b * 255 / maxB;// b * (b - minB) * 255 / db;
////                    int r1 = (r - minR) * 255 / dr;
////                    int g1 = (g - minG) * 255 / dg;
////                    int b1 = (b - minB) * 255 / db;
//                    float h1 = stretchH ? (hsv[0] - minH) * maxH / dh : hsv[0];
//                    float s1 = stretchS ? (hsv[1] - minS) * maxS / ds : hsv[1];
//                    float v1 = stretchV ? (hsv[2] - minV) * maxV / dv : hsv[2];
                    float h1 = stretchH ? (hsv[0] - minH) / dh : hsv[0];
                    float s1 = stretchS ? (hsv[1] - minS) / ds : hsv[1];
                    float v1 = stretchV ? (hsv[2] - minV) / dv : hsv[2];
                    int color1 = Color.HSBtoRGB(h1, s1, v1);
                    res.setRGB(i, j, color1);
                }
            }
            return res;
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }
}

class GammaColorBalancer {
    public static BufferedImage balanceColors(BufferedImage src, double gamma) {
//        if (ImageAndPath.isDummyImage(src)) {
//            return src;
//        }
        try {
            int width = src.getWidth();
            int height = src.getHeight();
            var res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int color = src.getRGB(i, j);

                    int r = 0xff & (color >> 16);
                    int g = 0xff & (color >> 8);
                    int b = 0xff & (color);
                    int r1 = gamma(gamma, r);
                    int g1 = gamma(gamma, g);
                    int b1 = gamma(gamma, b);
                    int color1 = (r1 << 16) | (g1 << 8) | (b1);
                    res.setRGB(i, j, color1);
                }
            }
            return res;
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }
    static int gamma(double gamma, int colorValue) {
        double res = Math.pow((colorValue / 255.), gamma) * 255.;
        return (int) Math.round(res);
    }
}

class BilinearDeglareWhite {
    final static int NX = 32, NY=32;
    public static BufferedImage unglare(BufferedImage src) {
        try {
            int width = src.getWidth();
            int height = src.getHeight();
            int dx = (width+NX-1)/NX;
            int dy = (height+NY-1)/NY;

            var aminv = new ByteArray2d(NX,NY);
            var amaxv = new ByteArray2d(NX,NY);
            aminv.setAll(255);

            {
                var smaxv = new StretchedArray2d(amaxv, dx, dy);
                var sminv = new StretchedArray2d(aminv, dx, dy);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        int color = src.getRGB(i, j);
                        int r = 0xff & (color >> 16);
                        int g = 0xff & (color >> 8);
                        int b = 0xff & (color);
                        int v = Math.min(Math.min(r,g),b);
                        int vv = Math.max(Math.max(r,g),b);
                        if (v < sminv.get(i, j)) { sminv.set(i,j,r); }
                        if (vv > smaxv.get(i, j)) { smaxv.set(i,j,vv); }
                    }
                }
                sminv.copyBoundaries();
                smaxv.copyBoundaries();
            }

            var bminv = new ByteArray2d(NX,NY);
            var bmaxv = new ByteArray2d(NX,NY);
            for (int j=0; j<NY; j++) {
                for (int i=0; i<NX; i++) {
                    bminv.set(i,j, aminv.foldAround(i,j, Math::min, 255));
                    bmaxv.set(i,j, amaxv.foldAround(i,j, Math::max, 0));
                }
            }
            bminv.copyBoundaries();
            bmaxv.copyBoundaries();

            var iminv = new InterpolatingArray2d(bminv, dx, dy);
            var imaxv = new InterpolatingArray2d(bmaxv, dx, dy);

            var res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int j=0; j<height; j++) {
                for (int i=0; i<width; i++) {
                    int color = src.getRGB(i, j);
                    int r = 0xff & (color >> 16);
                    int g = 0xff & (color >> 8);
                    int b = 0xff & (color);
                    int minV = iminv.get(i,j);
                    int maxV = imaxv.get(i,j);
                    int r1 = (r - minV) * 255 / Math.max(40, maxV-minV);
                    int g1 = (g - minV) * 255 / Math.max(40, maxV-minV);
                    int b1 = (b - minV) * 255 / Math.max(40, maxV-minV);
                    int color1 = (r1 << 16) | (g1 << 8) | (b1);
                    res.setRGB(i, j, color1);
                }
            }
            return res;
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }
    //====
    interface ROArray2d {
        int get(int x, int y);
        int getXSize();
        int getYSize();
    }
    interface Array2d extends ROArray2d {
        void set(int x, int y, int value);
        void copyBoundaries();
        int setAll(int value);
    }
    //====
    static class ByteArray2d implements Array2d {
        final byte[][]values;
        final int xsize, ysize;
        ByteArray2d(int xsize, int ysize) {
            this.values = new byte[xsize+2][ysize+2];
            this.xsize = xsize;
            this.ysize = ysize;
        }
        public int get(int x, int y) {
            return 0xff & values[x+1][y+1];
        }
        public void set(int x, int y, int value) {
            values[x+1][y+1] = (byte)value;
            if((value & 0xffff_ff00) != 0) {
                System.err.println("storing "+value+" to byte at ("+x+","+y+")");
            }
        }
        public void copyBoundaries() {
            set(-1,-1,get(0,0));
            set(-1,ysize,get(0,ysize-1));
            set(xsize,-1,get(xsize-1,0));
            set(xsize,ysize,get(xsize-1,ysize-1));
            for(int i=0; i<xsize; i++) {
                set(-1,i,get(0,i));
                set(i,-1,get(i,0));
                set(xsize,i,get(xsize-1,i));
                set(i,ysize,get(i,ysize-1));
            }
        }
        public int setAll(int value) {
            for (int i=0; i<values.length; i++) {
                Arrays.fill(values[i], (byte)value);
            }
            if((value & 0xffff_ff00) != 0) {
                System.err.println("storing "+value+" to byte at all indexes)");
            }
            return value;
        }
        @Override
        public int getXSize() {
            return xsize;
        }
        @Override
        public int getYSize() {
            return ysize;
        }
        public int foldAround(int i, int j, IntBinaryOperator f, int startVal) {
            int val = startVal;
            for (int dj=-1; dj<=1; ++dj) {
                for (int di=-1; di<=1; ++di) {
                    val = f.applyAsInt(val, get(i+di, j+dj));
                }
            }
            return val;
        }
    }
    //====
    static class StretchedArray2d implements Array2d {
        final Array2d backingArray;
        final int dx, dy;
        public StretchedArray2d(Array2d backingArray, int dx, int dy) {
            this.backingArray = backingArray;
            this.dx = dx;
            this.dy = dy;
        }
        @Override
        public int get(int x, int y) {
            return backingArray.get(Math.floorDiv(x,dx), Math.floorDiv(y,dy));
        }
        @Override
        public void set(int x, int y, int value) {
            backingArray.set(Math.floorDiv(x,dx), Math.floorDiv(y,dy), value);
        }
        @Override
        public void copyBoundaries() {
            backingArray.copyBoundaries();
        }
        @Override
        public int setAll(int value) {
            return backingArray.setAll(value);
        }
        @Override
        public int getXSize() {
            return backingArray.getXSize()*dx;
        }
        @Override
        public int getYSize() {
            return backingArray.getYSize()*dy;
        }
    }
    //====
    static class InterpolatingArray2d implements ROArray2d {
        final ROArray2d backingArray;
        final int dx, dy;
        public InterpolatingArray2d(ROArray2d backingArray, int dx, int dy) {
            this.backingArray = backingArray;
            this.dx = dx;
            this.dy = dy;
        }
        public int get(int x, int y) {
            int dx2 = dx/2, dy2 = dy/2;
            int ia = Math.floorDiv(x-dx2, dx);
            int iz = ia + 1;
            int ja = Math.floorDiv(y-dy2, dy);
            int jz = ja + 1;
            int xa = dx*ia+dx2;
            int ya = dy*ja+dy2;
            double faa = backingArray.get(ia,ja), fza = backingArray.get(iz,ja),
                    faz = backingArray.get(ia,jz), fzz = backingArray.get(iz,jz);
            int xx = x - xa;
            int yy = y - ya;
            double f0 = bilinear4(xx, yy, faa, fza, faz, fzz);
            return (int)f0;
        }
        double bilinear4(int x, int y, double faa, double fza, double faz, double fzz) {
            double fa = ( faa*(dx-x) + fza*x ) / dx;
            double fz = ( faz*(dx-x) + fzz*x ) / dx;
            double f = ( fa*(dy-y) + fz*y ) / dy;
            return f;
        }
        @Override
        public int getXSize() {
            return backingArray.getXSize()*dx;
        }
        @Override
        public int getYSize() {
            return backingArray.getYSize()*dy;
        }
    }
}

enum SiteOpenCommand {
    OPEN_CURRENT_SOL, OPEN_TAKEN_LATER, OPEN_TAKEN_EARLIER, OPEN_LATEST;
}
class CuriosityOpener {
    public static void openCurrentSol(RawData rawData, boolean isRight) {
        try {
            Integer sol = FileLocations.getSol(isRight ? rawData.right.path : rawData.left.path).orElse(1);

            Object re = NasaReader.dataStructureFromCuriositySolStarting(sol, 3);
            if (!(re instanceof Map)) {
                System.err.println("Response is not a map: "+re);
                return;
            }
            String perPage = ((Map) re).get("total").toString();
            String url = "https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+asc%2C+date_taken+asc%2Cinstrument_sort+asc%2Csample_type_sort+asc" +
                         "&per_page="+perPage+"&page=0&mission=msl&begin_sol="+sol+"&end_sol="+sol;
            HyperTextPane.openHyperlink(url);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    public static void openStartingFrom(RawData rawData, boolean isRight, boolean timeAsc) {
        try {
            Integer sol = FileLocations.getSol(isRight ? rawData.right.path : rawData.left.path).orElse(1);

            String xsc = timeAsc ? "asc" : "desc";
            String url = "https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+"+xsc+"%2C+date_taken+"+xsc+"%2C" +
                    "instrument_sort+asc%2Csample_type_sort+asc" +
                    "&per_page=100&page=0&mission=msl&begin_sol="+(timeAsc ? sol : 0)+"&end_sol="+(timeAsc ? "" : sol);
            HyperTextPane.openHyperlink(url);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    public static void openLatest() {
        HyperTextPane.openHyperlink(Main.CURIOSITY_RAW_IMAGES_URL);
    }
}

class PerseveranceOpener {
    public static void openCurrentSol(RawData rawData, boolean isRight) {
        try {
            String whereFrom = isRight ? rawData.right.path : rawData.left.path;
            String fname = FileLocations.getFileNameNoExt(whereFrom);
            String imageId = fname.substring(0, 1+fname.lastIndexOf("J"));
            Integer sol = RemoteFileNavigatorV2.solFromPerseveranceImageId(imageId);
            String url = "https://mars.nasa.gov/mars2020/multimedia/raw-images/?begin_sol="+sol+"&end_sol="+sol+"#raw-images";
            HyperTextPane.openHyperlink(url);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
//    public static void openStartingFrom(RawData rawData, boolean isRight, boolean timeAsc) {
//        try {
//            Integer sol = FileLocations.getSol(isRight ? rawData.right.path : rawData.left.path).orElse(1);
//
//            String xsc = timeAsc ? "asc" : "desc";
//            String url = "https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+"+xsc+"%2C+date_taken+"+xsc+"%2C" +
//                    "instrument_sort+asc%2Csample_type_sort+asc" +
//                    "&per_page=100&page=0&mission=msl&begin_sol="+(timeAsc ? sol : 0)+"&end_sol="+(timeAsc ? "" : sol);
//            HyperTextPane.openHyperlink(url);
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//    }
    public static void openLatest() {
        HyperTextPane.openHyperlink(Main.PERSEVERANCE_RAW_IMAGES_URL);
    }
}

class MastcamPairFinder {
    //ssssMXjjjjjjpppCCnnnnnYNN_DXXX
    //2893ML0151010011101294C00_DXXX.jpg
    //2893MR0151010011300209C00_DXXX.jpg
    //2893ML0151010021101295C00_DXXX.jpg
    //2893MR0151010021300210C00_DXXX.jpg
    final static int pairPrefixLength = "ssssMXjjjjjjppp".length();
    private static int suffixLength = "C00_DXXX".length();

    public static boolean areMrMlMatch(String url1, String url2) {
        var fname1 = FileLocations.getFileName(url1);
        var fname2 = FileLocations.getFileName(url2);
        return fname1.length() > pairPrefixLength
            && fname2.length() > pairPrefixLength
            && fname1.substring(0, 4).equals(fname2.substring(0, 4))
            && fname1.substring(4,6).equalsIgnoreCase("MR")
            && fname2.substring(4,6).equalsIgnoreCase("ML")
            && fname1.substring(6, pairPrefixLength).equals(fname2.substring(6, pairPrefixLength));
    }
    /**
     * Find the corresponding left/right mastcam image (a query to the NASA site is made).
     * @param imageId image id, e.g. "1407ML0068890100601896C00_DXXX"
     * @return id of the corresponding other image, e.g. "1407MR0068890100702104C00_DXXX"
     * @throws IOException
     */
    public static Optional<String> findMrlMatch(String imageId) throws IOException {
        Object jsonObjectPairs = NasaReader.dataStructureMrlMatchesFromImageId(imageId);
        List<Map> items = (List) (
            ((List<Object>) JsonDiy.get(jsonObjectPairs, "items"))
            .stream()
            .filter(o -> o instanceof Map)
            .collect(Collectors.toList())
        );
        Optional<Map> match = Optional.empty();
        if (items.size() > 1) {
            String suffix = imageId.substring(imageId.length()-suffixLength);
            match = items.stream().filter(m -> m.get("imageid").toString().endsWith(suffix)).findFirst();
        }
        if (!match.isPresent()) {
            match = items.stream().findAny();
        }
        return match.map(m -> m.get("imageid").toString());
    }

}

class BuildVersion {
    public static String getBuildVersion(){
        String ver = BuildVersion.class.getPackage().getImplementationVersion();
        return ver == null ? "n/a" : ver;
    }
}

class MeasurementPanel extends JPanel {
    final UiEventListener uiEventListener;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLX1;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLY1;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLX2;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLY2;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLX3;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLY3;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLX4;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLY4;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLX5;
    final DigitalZoomControl<Double, OffsetWrapper2> dcLY5;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRX1;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRY1;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRX2;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRY2;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRX3;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRY3;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRX4;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRY4;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRX5;
    final DigitalZoomControl<Double, OffsetWrapper2> dcRY5;
    final JTextArea textArea;
    final JScrollPane textAreaScroll;
    final StereoCamChooser stereoCamChooser;
    final MeasurementPointMarkChooser measurementPointMarkChooser;

    public MeasurementPanel(UiEventListener uiEventListener) {
        this.uiEventListener = uiEventListener;

        GridBagLayout gbl = new GridBagLayout();

        dcLX1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">X1L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(0,i); doCalculate7();});
        dcLY1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">Y1L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(1,i); doCalculate7();});
        dcLX2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">X2L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(2,i); doCalculate7();});
        dcLY2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">Y2L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(3,i); doCalculate7();});
        dcLX3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">X3L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(4,i); doCalculate7();});
        dcLY3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">Y3L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(5,i); doCalculate7();});
        dcLX4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">X4L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(6,i); doCalculate7();});
        dcLY4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">Y4L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(7,i); doCalculate7();});
        dcLX5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">X5L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(8,i); doCalculate7();});
        dcLY5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">Y5L:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(9,i); doCalculate7();});
        dcRX1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">X1R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(10,i); doCalculate7();});
        dcRY1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">Y1R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(11,i); doCalculate7();});
        dcRX2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">X2R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(12,i); doCalculate7();});
        dcRY2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">Y2R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(13,i); doCalculate7();});
        dcRX3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">X3R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(14,i); doCalculate7();});
        dcRY3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">Y3R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(15,i); doCalculate7();});
        dcRX4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">X4R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(16,i); doCalculate7();});
        dcRY4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">Y4R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(17,i); doCalculate7();});
        dcRX5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">X5R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(18,i); doCalculate7();});
        dcRY5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">Y5R:</font></html>", 9, new OffsetWrapper2(), i -> { uiEventListener.markedPointChanged(19,i); doCalculate7();});
        textArea = new JTextArea(18,120);
        textAreaScroll = new JScrollPane(textArea);
        stereoCamChooser = new StereoCamChooser(v -> { uiEventListener.stereoCameraChanged(v); doCalculate7(); });
        measurementPointMarkChooser = new MeasurementPointMarkChooser(v -> uiEventListener.markShapeChanged(v));

        var dcs = Arrays.asList(dcLX1, dcLY1, dcLX2, dcLY2, dcLX3, dcLY3, dcLX4, dcLY4, dcLX5, dcLY5, dcRX1, dcRY1, dcRX2, dcRY2, dcRX3, dcRY3, dcRX4, dcRY4, dcRX5, dcRY5);
        var dcRows = dcs.size()/4;

        {
            var row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
//            var text1 = new JLabel("Please focus your eyes on the object of interest and make both marks of the same color");
//            var text2 = new JLabel("EXACTLY match ON THAT OBJECT in x3d, otherwise you will get a measurement error!");
            // TODO: refactor, use an array + a loop
            var text1 = new JLabel("The best method is to mark THE SAME SPOT ON THE SAME OBJECT on both photos with red marks, and mark another spot on another object with the green marks, again on both photos.");
            var text3 = new JLabel("Note that moving a mark by 1 pixel MAY significantly change the result of measurement. The blue/cyan/magenta marks are used only for calibration.");
            var text5 = new JLabel(" ");
            row.add(text1);
            row.add(text3);
            row.add(text5);
        }
        {
            var text = new JLabel("X and Y coordinates of points 1 and 2 on the left and right images:");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(text, gbc);
            this.add(text);
        }

        for (int lr = 0; lr<2; lr++) {
            for (int i = 0; i < dcRows*2; i++) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1.0;
                gbc.weighty = 1.0;
                gbc.gridx = lr*3 + i%2;
                gbc.gridy = i/2+2;
                var dc = dcs.get(lr*dcRows*2 + i);
                gbl.setConstraints(dc, gbc);
                this.add(dc);
            }
        }
        // white space between controls for the left and right images
        for (int i = 0; i < dcRows; i++) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 2;
            gbc.gridy = i+2;
            var t = new JLabel("        ");
            gbl.setConstraints(t, gbc);
            this.add(t);
        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 11;          // MAGIC NUMBER // TODO: calculate
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
            var text = new JLabel("Camera:");
            row.add(text);
            row.add(stereoCamChooser);
            var text2 = new JLabel("Mark Shape:");
            row.add(text2);
            row.add(measurementPointMarkChooser);
            var text3 = new JLabel("Image Scaling Interpolation:");
            row.add(text3);
            {
                JButton setNearestNeighbor = new JButton("Set to NEAREST");
                setNearestNeighbor.addActionListener(v -> {
                    uiEventListener.lImageResamplingModeChanged(ImageResamplingMode.NEAREST);
                    uiEventListener.rImageResamplingModeChanged(ImageResamplingMode.NEAREST);
                });
                row.add(setNearestNeighbor);
            }
            {
                JButton setBicubic = new JButton("Set to BICUBIC");
                setBicubic.addActionListener(v -> {
                    uiEventListener.lImageResamplingModeChanged(ImageResamplingMode.BICUBIC);
                    uiEventListener.rImageResamplingModeChanged(ImageResamplingMode.BICUBIC);
                });
                row.add(setBicubic);
            }
        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 12;         // MAGIC NUMBER // TODO: calculate
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
            var button = new JButton("Calculate");
            button.addActionListener(e -> doCalculate7());
            row.add(button);
            var text = new JLabel("<html>To calibrate, set both cyan marks on some spot on the left, both magenta marks<br>on some spot on the right, set both blue marks on some infinitely distant spot, and press:</html>");
            row.add(text);
            var button2 = new JButton("Calibrate");
            button2.addActionListener(e -> doCalibrate7());
            row.add(button2);
        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 13;         // MAGIC NUMBER // TODO: calculate
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
            row.add(textAreaScroll);
        }
        {
            {
                var row = new JPanel();
                {
                    var subpixelPrecisionMarksCheckbox = new JCheckBox("Position Marks with Subpixel Precision (1/N px for zoom factor N) — slow but more precise");
                    subpixelPrecisionMarksCheckbox.addActionListener(
                            e -> uiEventListener.setSubpixelPrecisionMarks(
                                    subpixelPrecisionMarksCheckbox.isSelected()
                            )
                    );
                    row.add(subpixelPrecisionMarksCheckbox);
                }
                {
                    JButton clearAll = new JButton("Clear All Marks");
                    clearAll.addActionListener(v -> uiEventListener.clearAllMarks());
                    row.add(clearAll);
                }
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 14;         // MAGIC NUMBER // TODO: calculate
                gbc.gridheight = 1;
                gbc.gridwidth = 6;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
        }
        this.setLayout(gbl);
    }

    void showDialogIn(JFrame mainFrame) {
        setControls(uiEventListener.getMeasurementStatus());
        doCalculate7();
        JOptionPane.showMessageDialog(mainFrame, this,"Measurement", JOptionPane.PLAIN_MESSAGE);
    }

    void setResult(String s) {
        textArea.setText(s);
    }
    StereoPairParameters getStereoPairParameters() {
        return (StereoPairParameters) stereoCamChooser.getSelectedItem();
    }
    void setStereoPairParameters(StereoPairParameters stereoPairParameters) {
        stereoCamChooser.setValue(stereoPairParameters);
    }
    private void doCalibrate7() {
        var ms = uiEventListener.getMeasurementStatus();
        String oldParameters = ms.stereoPairParameters.toStringDetailed();
        {
            var stereoPairParameters = ms.stereoPairParameters;
            if (stereoPairParameters != StereoPairParameters.Custom) {
                setStereoPairParameters(ms.stereoPairParameters = StereoPairParameters.Custom.assignFrom(stereoPairParameters));
                // no need to call uiEventListener.stereoCameraChanged(...) explicitly, it already is called
            }
        }

        var correction = MeasurementPanel.calibrate7(ms.right, ms.left, ms.stereoPairParameters);
        getStereoPairParameters().measurementCorrection = correction;

        try (var baos = new ByteArrayOutputStream();
             var pw = new PrintWriter(baos)
        ) {
            pw.println(oldParameters);
            pw.println(getStereoPairParameters().toStringDetailed());
            pw.println("assuming the marked spot is at horizon, that is, at "+ms.stereoPairParameters.horizon+" m");
            pw.println("(which means that if you try to measure distance to something at the horizon, you'll get back this assumed digit)");
            pw.flush();
            String result = baos.toString(StandardCharsets.UTF_8.name());
            System.out.println(result);
            setResult(result);
        } catch (Exception ioe) {
            ioe.printStackTrace();
            setResult("error");
        }
    }
    static double getDRoll(APoint l1, APoint l2, APoint r1, APoint r2) {
        double rollL = Math.atan((l2.getPy() - l1.getPy()) / (l2.getPx() - l1.getPx()));
        double rollR = Math.atan((r2.getPy() - r1.getPy()) / (r2.getPx() - r1.getPx()));
        double dRoll = rollR - rollL;
        return dRoll;
    }
//    static double getDRoll(PanelMeasurementStatus left, PanelMeasurementStatus right) {
//        double rollL = Math.atan((left.y2 - left.y1) / (double) (left.x2 - left.x1));
//        double rollR = Math.atan((right.y2 - right.y1) / (double) (right.x2 - right.x1));
//        double dRoll = rollR - rollL;
//        return dRoll;
//    }
    static double getDPitch(APoint l1, APoint r1) {
        return r1.getAy() - l1.getAy();
    }
    static double getDYaw(APoint l, APoint r, StereoPairParameters spp) {
        var h = spp.horizon;
        var b = spp.base;
        var gamma = b/h;
        var dYaw = l.getAx() - r.getAx() - gamma;
        return dYaw;
    }

    // TODO: use this
    static MeasurementCorrection calibrate7(PanelMeasurementStatus left0, PanelMeasurementStatus right0, StereoPairParameters spp) {
        System.out.println("Calibration:");
        System.out.println("left0: "+left0);
        System.out.println("right0: "+right0);
        System.out.println("3: "+left0.x3+" "+left0.y3+"  "+right0.x3+" "+right0.y3);
        System.out.println("4: "+left0.x4+" "+left0.y4+"  "+right0.x4+" "+right0.y4);
        System.out.println("5: "+left0.x5+" "+left0.y5+"  "+right0.x5+" "+right0.y5);
        System.out.println("wh: "+left0.w+" "+left0.h+"  "+right0.w+" "+right0.h);
        var dRollL = getDRoll(left0.fourth, left0.fifth, right0.fourth, right0.fifth);
        var dRollR = -dRollL;
        System.out.println("dRollL: "+dRollL);
        var left1L = left0;
        var right1L = right0.copyD().addRollD(dRollL);
        var left1R = left0.copyD().addRollD(dRollR);
        var right1R = right0;
        System.out.println("left1L: "+left1L);
        System.out.println("right1L: "+right1L);
        System.out.println("left1R: "+left1R);
        System.out.println("right1R: "+right1R);
        System.out.println("4L: "+left1L.x4+" "+left1L.y4+"  "+right1L.x4+" "+right1L.y4);
        System.out.println("5L: "+left1L.x5+" "+left1L.y5+"  "+right1L.x5+" "+right1L.y5);
        System.out.println("3L: "+left1L.x3+" "+left1L.y3+"  "+right1L.x3+" "+right1L.y3);
        System.out.println("4R: "+left1R.x4+" "+left1R.y4+"  "+right1R.x4+" "+right1R.y4);
        System.out.println("5R: "+left1R.x5+" "+left1R.y5+"  "+right1R.x5+" "+right1R.y5);
        System.out.println("3R: "+left1R.x3+" "+left1R.y3+"  "+right1R.x3+" "+right1R.y3);
        System.out.println("new dRoll L: "+getDRoll(left1L.fourth, left1L.fifth, right1L.fourth, right1L.fifth));
        System.out.println("new dRoll R: "+getDRoll(left1R.fourth, left1R.fifth, right1R.fourth, right1R.fifth));

        var dPitchL =  (getDPitch(left1L.fourth, right1L.fourth) + getDPitch(left1L.fifth, right1L.fifth))/2;
        var dPitchR = -(getDPitch(left1R.fourth, right1R.fourth) + getDPitch(left1R.fifth, right1R.fifth))/2;
        System.out.println("dPitchL: "+dPitchL + " dPitchR:"+dPitchR);

        var left2L = left1L;
        var right2L = right1L.addPitchD(dPitchL);
        var left2R = left1R.addPitchD(dPitchR);
        var right2R = right1R;
        System.out.println("left2L: "+left2L);
        System.out.println("right2L: "+right2L);
        System.out.println("left2R: "+left2R);
        System.out.println("right2R: "+right2R);
        System.out.println("new dPitch L "+getDPitch(left2L.fourth, right2L.fourth));
        System.out.println("new dPitch R "+getDPitch(left2R.fourth, right2R.fourth));

        System.out.println("dist pt");
        System.out.println("LL "+left2L.third.asString());
        System.out.println("RL "+right2L.third.asString());
        System.out.println("LR "+left2R.third.asString());
        System.out.println("RR "+right2R.third.asString());
        System.out.println();

        var dYawL = getDYaw(left2L.third, right2L.third, spp);
        var dYawR = -getDYaw(left2R.third, right2R.third, spp);
        System.out.println("dYawL: "+dYawL + " dYawR:"+dYawR);
        var left3L = left2L;
        var right3L = right2L.addYawD(dYawL);
        var left3R = left2R.addYawD(dYawR);
        var right3R = right2R;

        System.out.println("After all three rotations:");
        System.out.println("new dRoll L: "+getDRoll(left3L.fourth, left3L.fifth, right3L.fourth, right3L.fifth));
        System.out.println("new dRoll R: "+getDRoll(left3R.fourth, left3R.fifth, right3R.fourth, right3R.fifth));
        System.out.println("new dPitch L "+getDPitch(left3L.fourth, right3L.fourth));
        System.out.println("new dPitch R "+getDPitch(left3R.fourth, right3R.fourth));
        System.out.println("new dYaw L "+getDYaw(left3L.third, right3L.third, spp));
        System.out.println("new dYaw R "+getDYaw(left3R.third, right3R.third, spp));
        var val3l = calculateDist7(spp.base, left3L.third, right3L.third);
        var val3r = calculateDist7(spp.base, left3R.third, right3R.third);
        System.out.println("Distance to the 3rd point (the one at the horizon):");
        System.out.println("point3L: l1="+val3l[0]+" l2="+val3l[1]+" distL="+dist3d(val3l[2],val3l[3],val3l[4])+" distR="+dist3d(val3l[5],val3l[6],val3l[7]));
        System.out.println("point3R: l1="+val3r[0]+" l2="+val3r[1]+" distL="+dist3d(val3r[2],val3r[3],val3r[4])+" distR="+dist3d(val3r[5],val3r[6],val3r[7]));
        System.out.println("Calibration complete, digits in the 2 above lines must be around "+spp.horizon+" m");
        MeasurementCorrection res = new MeasurementCorrection(dRollL, dPitchL, dYawL, dRollR, dPitchR, dYawR);
        System.out.println(res);
        return res;
    }
    void doCalculate7() { // TODO: refrite for v7
        var ms = uiEventListener.getMeasurementStatus();
        System.out.println();
        System.out.println("Distance calculation");
        var left = ms.left.copy();
        var right = ms.right.copy();
        StereoPairParameters spp = ms.stereoPairParameters;
        System.out.println("Point1: "+ms.left.x1+" "+ms.left.y1+"   "+ms.right.x1+" "+ms.right.y1);
        System.out.println("Point2: "+ms.left.x2+" "+ms.left.y2+"   "+ms.right.x2+" "+ms.right.y2);
        System.out.println("Point3: "+ms.left.x3+" "+ms.left.y3+"   "+ms.right.x3+" "+ms.right.y3);
        System.out.println("Center: "+ms.left.w/2+" "+ms.left.h/2+"   "+ms.right.w/2+" "+ms.right.h/2);
        MeasurementCorrection correction = spp.measurementCorrection;

        // swap left and right because of X3D: left on screen is right
        var leftc = correction.correctToUseWithRight(right.copyD());
        var righto = left;
        var lefto = right;
        var rightc = correction.correctToUseWithLeft(left.copyD());
        var val1l = calculateDist7(spp.base, lefto.first, rightc.first);
        var val1r = calculateDist7(spp.base, leftc.first, righto.first);
        var val2l = calculateDist7(spp.base, lefto.second, rightc.second);
        var val2r = calculateDist7(spp.base, leftc.second, righto.second);
        System.out.println("point1L: l1="+val1l[0]+" l2="+val1l[1]+" distL="+dist3d(val1l[2],val1l[3],val1l[4])+" distR="+dist3d(val1l[5],val1l[6],val1l[7]));
        System.out.println("point2L: l1="+val2l[0]+" l2="+val2l[1]+" distL="+dist3d(val2l[2],val2l[3],val2l[4])+" distR="+dist3d(val2l[5],val2l[6],val2l[7]));
        System.out.println("point1R: l1="+val1r[0]+" l2="+val1r[1]+" distL="+dist3d(val1r[2],val1r[3],val1r[4])+" distR="+dist3d(val1r[5],val1r[6],val1r[7]));
        System.out.println("point2R: l1="+val2r[0]+" l2="+val2r[1]+" distL="+dist3d(val2r[2],val2r[3],val2r[4])+" distR="+dist3d(val2r[5],val2r[6],val2r[7]));
        double dist3dLl = dist3d(val2l[2] - val1l[2], val2l[3] - val1l[3], val2l[4] - val1l[4]);
        double dist3dRl = dist3d(val2l[5] - val1l[5], val2l[6] - val1l[6], val2l[7] - val1l[7]);
        double dist3dLr = dist3d(val2r[2] - val1r[2], val2r[3] - val1r[3], val2r[4] - val1r[4]);
        double dist3dRr = dist3d(val2r[5] - val1r[5], val2r[6] - val1r[6], val2r[7] - val1r[7]);
        double dist2dLl = dist3d(val2l[2] - val1l[2], 0, val2l[4] - val1l[4]);
        double dist2dRl = dist3d(val2l[5] - val1l[5], 0, val2l[7] - val1l[7]);
        double dist2dLr = dist3d(val2r[2] - val1r[2], 0, val2r[4] - val1r[4]);
        double dist2dRr = dist3d(val2r[5] - val1r[5], 0, val2r[7] - val1r[7]);
        System.out.println("dist12l: distL="+ dist3dLl +" distR="+ dist3dRl);
        System.out.println("dist12r: distL="+ dist3dLr +" distR="+ dist3dRr);
        try (var baos = new ByteArrayOutputStream();
             var pw = new PrintWriter(baos)
        ) {
            pw.println("Point1: "+ms.left.x1+" "+ms.left.y1+"   "+ms.right.x1+" "+ms.right.y1);
            pw.println("Point2: "+ms.left.x2+" "+ms.left.y2+"   "+ms.right.x2+" "+ms.right.y2);
            pw.println("Point3: "+ms.left.x3+" "+ms.left.y3+"   "+ms.right.x3+" "+ms.right.y3);
            pw.println("Point4: "+ms.left.x4+" "+ms.left.y4+"   "+ms.right.x4+" "+ms.right.y4);
            pw.println("Point5: "+ms.left.x5+" "+ms.left.y5+"   "+ms.right.x5+" "+ms.right.y5);
            pw.println(spp.toStringDetailed());
            pw.println("γ₁: "+val1l[8]+"° "+val1r[8]+"° "+val2l[8]+"° "+val2r[8]+"°");
            if(val1l[8]<0 || val1r[8]<0 || val2l[8]<0 || val2r[8]<0) {
                pw.println("\nERROR: γ₁<0 !!!!! (the angle opposite the line between the cameras)\n");
            }
            pw.println("Distance from the left and right cameras, m:");
            pw.println("Point 1: "+ val1l[1] + ", " + val1l[0] + " // " + val1r[1] + ", " + val1r[0]);
            pw.println("Point 2: "+ val2l[1] + ", " + val2l[0] + " // " + val2r[1] + ", " + val2r[0]);
            pw.println( "Distance between Points 1 and 2, "+
                    "calculated in the coordinate systems of left and right cameras, m:");
            pw.println(""+ dist3dRl + ", " + dist3dLl + ", " + dist3dRr + ", " + dist3dLr);
            pw.println( "The same distance between Points 1 and 2, "+
                    "calculated assuming Δy=0, m:");
            pw.println(""+ dist2dRl + ", " + dist2dLl + ", " + dist2dRr + ", " + dist2dLr);
            pw.println("Δx: "+ (val2l[2] - val1l[2])
                      + ", " + (val2l[5] - val1l[5])
                    + " // " + (val2r[2] - val1r[2])
                      + ", " + (val2r[5] - val1r[5])
            );
            pw.println("Δy: "+ (val2l[3] - val1l[3])
                      + ", " + (val2l[6] - val1l[6])
                    + " // " + (val2r[3] - val1r[3])
                      + ", " + (val2r[6] - val1r[6])
            );
            pw.println("Δz: "+ (val2l[4] - val1l[4])
                      + ", " + (val2l[7] - val1l[7])
                    + " // " + (val2r[4] - val1r[4])
                      + ", " + (val2r[7] - val1r[7])
            );
            pw.println("Axes: x left to right, y forward, z bottom to top");
            pw.flush();
            String result = baos.toString(StandardCharsets.UTF_8.name());
            System.out.println(result);
            setResult(result);
        } catch (Exception ioe) {
            ioe.printStackTrace();
            setResult("error");
        }
    }
    /**
     * Calculate the angle in the inclined plane whose projection to the horizontal plane is alpha.
     * @param alpha angle in the horizontal plane between the stereo base and the projection of the point onto the horizontal plane
     * @param phi angle between the horizontal and inclined planes
     * @return angle in the inclined plane, between the stereo base and the point
     */
    static double caclulateInclined(double alpha, double phi) {
        var alpha0 = Math.atan2(Math.sin(alpha),Math.cos(alpha)*Math.cos(phi));
        return alpha0;
    }
    static double dist3d(double dx, double dy, double dz) {
        var res = Math.sqrt(sqr(dx)+sqr(dy)+sqr(dz));
        return res;
    }
    static double[] calculateDist7(double base, APoint l, APoint r) {
        var phiL = l.getAy();
        var phiR = r.getAy();

        var alpha1 = Math.PI/2 - l.getAx();
        var beta1 = Math.PI/2 + r.getAx();
        var gamma1 = l.getAx() - r.getAx(); // pi - alpha1 - beta1
        System.out.println("alpha1="+Math.toDegrees(alpha1)+"° beta1="+Math.toDegrees(beta1)+"° gamma1="+Math.toDegrees(gamma1)+"°");
        System.out.println("alpha1="+alpha1+" beta1="+beta1+" gamma1="+gamma1+"");
        var alphaL = caclulateInclined(alpha1, phiL);
        var alphaR = caclulateInclined(alpha1, phiR);
        var betaL = caclulateInclined(beta1, phiL);
        var betaR = caclulateInclined(beta1, phiR);
        var gammaL = Math.PI - alphaL - betaL;
        var gammaR = Math.PI - alphaR - betaR;
        System.out.println("alphaL="+Math.toDegrees(alphaL)+"° betaL="+Math.toDegrees(betaL)+"° gammaL="+Math.toDegrees(gammaL)+"°");
        System.out.println("alphaR="+Math.toDegrees(alphaR)+"° betaR="+Math.toDegrees(betaR)+"° gammaR="+Math.toDegrees(gammaR)+"°");
        var l1L = base * (Math.cos(betaL) + Math.cos(alphaL)*Math.cos(gammaL)) / sqr(Math.sin(gammaL));
        var l1R = base * (Math.cos(betaR) + Math.cos(alphaR)*Math.cos(gammaR)) / sqr(Math.sin(gammaR));
        var l2L = base * (Math.cos(alphaL) + Math.cos(betaL)*Math.cos(gammaL)) / sqr(Math.sin(gammaL));
        var l2R = base * (Math.cos(alphaR) + Math.cos(betaR)*Math.cos(gammaR)) / sqr(Math.sin(gammaR));

        var azl = l1L * Math.sin(phiL);
        var azr = l2R * Math.sin(phiR);
        var ayl = l1L * Math.cos(phiL) * Math.sin(alpha1);
        var ayr = l2R * Math.cos(phiR) * Math.sin(beta1);
        var axl = l1L * Math.cos(phiL) * Math.cos(alpha1);
        var axr = l2R * Math.cos(phiR) * Math.cos(beta1);
        System.out.println(" l1="+l1L+"\n l2="+l2R+"\n "+"l: x="+axl+" y="+ayl+" z="+azl+"\n r: x="+axr+" y="+ayr+" z="+azr);
        return new double[]{l1L,l2R,axl,ayl,azl,axr,ayr,azr,Math.toDegrees(gamma1)};
    }
    static double sqr(double x) { return x*x; }
    MeasurementPanel setControls(MeasurementStatus ms) {
        dcLX1.setValueAndText(ms.left.x1);
        dcLY1.setValueAndText(ms.left.y1);
        dcLX2.setValueAndText(ms.left.x2);
        dcLY2.setValueAndText(ms.left.y2);
        dcLX3.setValueAndText(ms.left.x3);
        dcLY3.setValueAndText(ms.left.y3);
        dcLX4.setValueAndText(ms.left.x4);
        dcLY4.setValueAndText(ms.left.y4);
        dcLX5.setValueAndText(ms.left.x5);
        dcLY5.setValueAndText(ms.left.y5);
        dcRX1.setValueAndText(ms.right.x1);
        dcRY1.setValueAndText(ms.right.y1);
        dcRX2.setValueAndText(ms.right.x2);
        dcRY2.setValueAndText(ms.right.y2);
        dcRX3.setValueAndText(ms.right.x3);
        dcRY3.setValueAndText(ms.right.y3);
        dcRX4.setValueAndText(ms.right.x4);
        dcRY4.setValueAndText(ms.right.y4);
        dcRX5.setValueAndText(ms.right.x5);
        dcRY5.setValueAndText(ms.right.y5);
        return this;
    }
}
class MeasurementCorrection {
    double dRollL;
    double dPitchL;
    double dYawL;
    double dRollR;
    double dPitchR;
    double dYawR;
    public MeasurementCorrection() {
    }
    public MeasurementCorrection(double dRollL, double dPitchL, double dYawL, double dRollR, double dPitchR, double dYawR) {
        this.dRollL = dRollL;
        this.dPitchL = dPitchL;
        this.dYawL = dYawL;
        this.dRollR = dRollR;
        this.dPitchR = dPitchR;
        this.dYawR = dYawR;
    }
    public PanelMeasurementStatusD correctToUseWithLeft(PanelMeasurementStatusD right) {
        return right.addRollD(dRollL).addPitchD(dPitchL).addYawD(dYawL);
    }
    public PanelMeasurementStatusD correctToUseWithRight(PanelMeasurementStatusD left) {
        return left.addRollD(dRollR).addPitchD(dPitchR).addYawD(dYawR);
    }
    @Override
    public String toString() {
        return  "{" +
                "dRollL=" + dRollL +
                ", dPitchL=" + dPitchL +
                ", dYawL=" + dYawL +
                ", dRollR=" + dRollR +
                ", dPitchR=" + dPitchR +
                ", dYawR=" + dYawR +
                '}';
    }
}
class StereoPairParameters {
    String name;
    double base;
    double ifovL;
    double ifovR;
    double horizon;
    MeasurementCorrection measurementCorrection;

    public StereoPairParameters(String name, double base, double ifovL, double ifovR, double horizon) {
        this.name = name;
        this.base = base;
        this.ifovL = ifovL;
        this.ifovR = ifovR;
        this.horizon = horizon;
        this.measurementCorrection = new MeasurementCorrection();
    }

    public StereoPairParameters(String name, double base, double ifovL, double ifovR, double horizon, MeasurementCorrection measurementCorrection) {
        this.name = name;
        this.base = base;
        this.ifovL = ifovL;
        this.ifovR = ifovR;
        this.horizon = horizon;
        this.measurementCorrection = measurementCorrection;
    }

//    static StereoPairParameters NavCam =     new StereoPairParameters("Curiosity NAVCAM",         0.424, 0.82E-3, 0.82E-3, 3.7E3, new int[]{0,0,0,-14, 0,15,0,0});
    static StereoPairParameters NavCam =     new StereoPairParameters("Curiosity NAVCAM",            0.424, 0.82E-3, 0.82E-3, 3.6E3);//, new int[]{0,0,0,-14, 0,15,0,0});
    static StereoPairParameters FHazCamB =   new StereoPairParameters("Curiosity Front HAZCAM B",    0.100, 2.10E-3, 2.10E-3, 2.1E3); // base=.167 is wrong!!!
    static StereoPairParameters RHazCamB =   new StereoPairParameters("Curiosity Rear HAZCAM B",     0.100, 2.10E-3, 2.10E-3, 2.3E3);//, new int[]{0,0,10,-6, -10,6,0,0});
    static StereoPairParameters MastCam =    new StereoPairParameters("Curiosity MASTCAM",           0.242, 7.5E-5,  2.25E-4, 3.7E3);
    static StereoPairParameters HazCam_167 = new StereoPairParameters("Curiosity HAZCAM .167(Front)",0.167, 2.10E-3, 2.10E-3, 2.1E3); // I do not know if base=.167 really is there
    static StereoPairParameters HazCam_100 = new StereoPairParameters("Curiosity HAZCAM .100(Rear)", 0.100, 2.10E-3, 2.10E-3, 2.1E3);

    // TODO: horizon=???
    static StereoPairParameters PFHazCamV1 = new StereoPairParameters("Perseverance F HAZCAM .248(5K1px)",         0.248, 0.45E-3, 0.45E-3, 2.1E3);
    static StereoPairParameters PFHazCamV2 = new StereoPairParameters("Perseverance F HAZCAM .248(1K2px)(wrong?)", 0.248, 1.84E-3, 1.84E-3, 2.1E3);
    static StereoPairParameters PRHazCamV1 = new StereoPairParameters("Perseverance R HAZCAM .934(5K1px)",         0.934, 0.45E-3, 0.45E-3, 2.1E3);
    static StereoPairParameters PRHazCamV2 = new StereoPairParameters("Perseverance R HAZCAM .934(1K2px)(wrong?)", 0.934, 1.84E-3, 1.84E-3, 2.1E3);
    static StereoPairParameters PNavCamV1 =  new StereoPairParameters("Perseverance NAVCAM .424(5K1 px)",          0.424, 0.45E-3, 0.45E-3, 2.1E3);
    static StereoPairParameters PNavCamV2 =  new StereoPairParameters("Perseverance NAVCAM .424(1K2 px)(wrong?)",  0.424, 1.84E-3, 1.84E-3, 2.1E3);

    static CustomStereoPairParameters Custom=new CustomStereoPairParameters(
                                                               "Custom[=========================================]",0.100, 2.10E-3, 2.10E-3, 2.1E3);

    public static StereoPairParameters getUiDefault() { return NavCam; }

    @Override
    public String toString() {
        return name;
    }
    public String toStringDetailed() {
        String res = "\""+name+"\" base="+base+"(m) ifovL="+ifovL+"(rad/px) ifovR="+ifovR+"(rad/px)" +
                " correction="+measurementCorrection
                ;
        return res;
    }
}
class CustomStereoPairParameters extends StereoPairParameters {
    public CustomStereoPairParameters(String name, double base, double ifovL, double ifovR, double horizon) {
        super(name, base, ifovL, ifovR, horizon);
    }
    CustomStereoPairParameters assignFrom(StereoPairParameters other) {
        this.name = "Custom["+other.name+"]";
        this.base = other.base;
        this.ifovL = other.ifovL;
        this.ifovR = other.ifovR;
        this.horizon = other.horizon;
        return this;
    }
}

class StereoCamChooser extends JComboBox<StereoPairParameters> {
    static StereoPairParameters[] modes = {
            StereoPairParameters.NavCam,
            StereoPairParameters.FHazCamB,
            StereoPairParameters.RHazCamB,
            StereoPairParameters.MastCam,
            StereoPairParameters.HazCam_167,
            StereoPairParameters.HazCam_100,
            StereoPairParameters.PFHazCamV1,
            StereoPairParameters.PFHazCamV2,
            StereoPairParameters.PRHazCamV1,
            StereoPairParameters.PRHazCamV2,
            StereoPairParameters.PNavCamV1,
            StereoPairParameters.PNavCamV2,
            StereoPairParameters.Custom,
    };
    public StereoCamChooser(Consumer<StereoPairParameters> valueListener) {
        super(modes);
        setValue(StereoPairParameters.getUiDefault());
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((StereoPairParameters) itemEvent.getItem());
            }
        });
    }
    public void setValue(StereoPairParameters stereoPairParameters) {
        setSelectedItem(stereoPairParameters);
    }
}

enum MeasurementPointMark {
    POINT(new int[][]{{0,0}}),
    CROSS(new int[][]{{1,0}, {2,0}, {3,0}, {4,0}, {5,0}, {6,0}}),
    XCROSS(new int[][]{{1,1}, {2,2}, {3,3}, {4,4}, {5,5}, {6,6}}),
    DAISY(new int[][]{{1,1}, {2,2}, {3,3}, {4,4}, {5,5}, {6,6}, {3,2}, {4,3}, {5,4}, {6,5}, {6,4}, {5,3}});

    int[][] listOfXY;

    MeasurementPointMark(int[][] listOfXY) {
        this.listOfXY = listOfXY;
    }
    public static MeasurementPointMark getUiDefault() {
        return CROSS;
    }
    void drawMark(BufferedImage bi, int x, int y, int rgb) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        Arrays.stream(listOfXY)
                .flatMap(PanelMeasurementStatus::symmetricPoints)
                .filter(xy -> x+xy[0] >= 0 && x+xy[0] < w && y+xy[1] >= 0 && y+xy[1] < h)
                .forEach(
                        xy -> bi.setRGB(x + xy[0], y + xy[1], rgb)
                );
    }
}
class MeasurementPointMarkChooser extends JComboBox<MeasurementPointMark> {
    static MeasurementPointMark[] modes = MeasurementPointMark.values();
    public MeasurementPointMarkChooser(Consumer<MeasurementPointMark> valueListener) {
        super(modes);
        setValue(MeasurementPointMark.getUiDefault());
        setMaximumRowCount(modes.length);
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((MeasurementPointMark) itemEvent.getItem());
            }
        });
    }
    public void setValue(MeasurementPointMark markType) {
        setSelectedItem(markType);
    }
}

/*
 * This class was borrowed from https://memorynotfound.com/generate-gif-image-java-delay-infinite-loop-example/
 */
class GifSequenceWriter {

    protected ImageWriter writer;
    protected ImageWriteParam params;
    protected IIOMetadata metadata;

    public GifSequenceWriter(ImageOutputStream out, int imageType, int delay, boolean loop) throws IOException {
        writer = ImageIO.getImageWritersBySuffix("gif").next();
        params = writer.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);

        configureRootMetadata(delay, loop);

        writer.setOutput(out);
        writer.prepareWriteSequence(null);
    }

    private void configureRootMetadata(int delay, boolean loop) throws IIOInvalidTreeException {
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delay / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by: https://memorynotfound.com");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loopContinuously = loop ? 0 : 1;
        child.setUserObject(new byte[]{ 0x1, (byte) (loopContinuously & 0xFF), (byte) ((loopContinuously >> 8) & 0xFF)});
        appExtensionsNode.appendChild(child);
        metadata.setFromTree(metaFormatName, root);
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName){
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++){
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)){
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return(node);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        writer.writeToSequence(new IIOImage(img, null, metadata), params);
    }

    public void close() throws IOException {
        writer.endWriteSequence();
    }
    // end of code borrowed from https://memorynotfound.com/generate-gif-image-java-delay-infinite-loop-example/

    interface BufferedImageSupplier {
        BufferedImage get() throws Exception;
    }
    public static void saveAsGif(File outFile, int delay, BufferedImageSupplier firstImageSupplier, BufferedImageSupplier... imageSuppliers) throws Exception {
        ImageOutputStream output = new FileImageOutputStream(outFile);

        GifSequenceWriter writer;
        {
            var first = firstImageSupplier.get();
            writer = new GifSequenceWriter(output, first.getType(), delay, true);
            writer.writeToSequence(first);
        }

        for (var supplier : imageSuppliers) {
            BufferedImage next = supplier.get();
            writer.writeToSequence(next);
        }

        writer.close();
        output.close();
    }
}

class SettingsPanel extends JPanel {
    final UiEventListener uiEventListener;
    JCheckBox showUrlsCheckbox;
    JCheckBox saveGifCheckbox;
    JCheckBox saveRightLeftCheckbox;

    public SettingsPanel(UiEventListener uiEventListener) {
        this.uiEventListener = uiEventListener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        {
            JCheckBox unThumbnailCheckox = new JCheckBox("Un-Thumbnail");
            unThumbnailCheckox.setSelected(true);
            unThumbnailCheckox.addActionListener(
                    e -> uiEventListener.unthumbnailChanged(unThumbnailCheckox.isSelected())
            );
            unThumbnailCheckox.setToolTipText("You can drag thumbnails, the full-scale version will be shown");
            this.add(unThumbnailCheckox);
        }
        {
            showUrlsCheckbox = new JCheckBox("Show URLs");
            showUrlsCheckbox.setSelected(true);
            showUrlsCheckbox.addActionListener(
                    e -> uiEventListener.setShowUrls(showUrlsCheckbox.isSelected())
            );
            showUrlsCheckbox.setToolTipText("Show image URLs in the bottom of the window");
            this.add(showUrlsCheckbox);
        }
        {
            saveGifCheckbox = new JCheckBox("Save Animated GIF");
            saveGifCheckbox.setSelected(true);
            saveGifCheckbox.addActionListener(
                    e -> uiEventListener.setSaveOptions(saveGifCheckbox.isSelected(),  saveRightLeftCheckbox.isSelected())
            );
            saveGifCheckbox.setToolTipText("When you click \"Save\", additionally save an animated GIF with the right and left halves of the stereo pair");
            this.add(saveGifCheckbox);
        }
        {
            saveRightLeftCheckbox = new JCheckBox("Save Left and Right Images Separately");
            saveRightLeftCheckbox.setSelected(true);
            saveRightLeftCheckbox.addActionListener(
                    e -> uiEventListener.setSaveOptions(saveGifCheckbox.isSelected(), saveRightLeftCheckbox.isSelected())
            );
            saveRightLeftCheckbox.setToolTipText("When you click \"Save\", additionally save left and right halves of the stereo pair separately");
            this.add(saveRightLeftCheckbox);
        }

    }
    void setControls (SaveOptions so) {
        saveGifCheckbox.setSelected(so.saveGif);
        saveRightLeftCheckbox.setSelected(so.saveLeftRightImages);
    }
    void showDialogIn(JFrame mainFrame) {
        JOptionPane.showMessageDialog(mainFrame, this,"Settings", JOptionPane.PLAIN_MESSAGE);
    }

}
