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
import javax.swing.text.SimpleAttributeSet;
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
import java.io.UncheckedIOException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
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
        var bo = new BehavioralOptions();
        var uic = new UiController(xv, lrn, rd0, dp, ms, bo);
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
    void loadBigImage(boolean isRight);
    void reload(boolean isRight);
    void saveProcessedImage(boolean isRight);
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
    void adjustOffsets(int pointId);
    void escapePressed();
    Optional<Integer> getSol(boolean isRight, WhichRover whichRover);
    MeasurementStatus getMeasurementStatus();
    DisplayParameters getDisplayParameters();
    ColorRange getViewportColorRange(boolean isRight, boolean ignoreBroken);
    CustomStretchRgbParameters getCurrentCustomStretchRgbParameters(boolean isRight);
    FisheyeCorrection getFisheyeCorrection(boolean isRight);
    ColorCorrectionAlgo getPreFilter(boolean isRight);
    Dimension getRawImageDimensions(boolean isRight);
    void setCustomStretchRgbParameters(CustomStretchRgbParameters customStretchRgbParameters, boolean isRight); //???
    void setSaveOptions(boolean saveGif, boolean saveLeftRIght);
    void setUseCustomCrosshairCursor(boolean useCustomCrosshairCursor);
    void setFisheyeCorrection(boolean isRight, FisheyeCorrection fc);
    void setPreFilter(boolean isRight, boolean isOn);
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
    public void setMinAll(int minX) {
        minR = minG = minB = minX;
    }
    public void setMaxAll(int maxX) {
        maxR = maxG = maxB = maxX;
    }
    boolean isEmpty() {
        return minR >= maxR || minG >= maxG || minB >= maxB;
    }
    boolean isFullRange() {
        return minR == 0 && maxR == 255
            && minG == 0 && maxG == 255
            && minB == 0 && maxB == 255;
    }
    void setEmpty() {
        minR = minG = minB = Integer.MAX_VALUE;
        maxR = maxG = maxB = 0;
    }
    void update(int rgb) {
        {
            int r = 0xff & (rgb >> 16);
            minR = Math.min(minR, r);
            maxR = Math.max(maxR, r);
        }
        {
            int g = 0xff & (rgb >> 8);
            minG = Math.min(minG, g);
            maxG = Math.max(maxG, g);
        }
        {
            int b = 0xff & (rgb);
            minB = Math.min(minB, b);
            maxB = Math.max(maxB, b);
        }
    }
    boolean contains(int rgb) {
        int c;
        return minR <= (c = 0xff & (rgb >> 16)) && c <= maxR
            && minG <= (c = 0xff & (rgb >> 8))  && c <= maxG
            && minB <= (c = 0xff & (rgb))       && c <= maxB;
    }
    boolean almostContains(int rgb, int d) {
        int c;
        return minR-d <= (c = 0xff & (rgb >> 16)) && c <= maxR+d
               && minG-d <= (c = 0xff & (rgb >> 8))  && c <= maxG+d
               && minB-d <= (c = 0xff & (rgb))       && c <= maxB+d;
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
    boolean isBlackSaturated;

    public CustomStretchRgbParameters(ColorRange colorRange, boolean isPerChannel, boolean isSaturated, boolean isBlackSaturated) {
        this.colorRange = colorRange.copy();
        this.isPerChannel = isPerChannel;
        this.isSaturated = isSaturated;
        this.isBlackSaturated = isBlackSaturated;
    }
    public CustomStretchRgbParameters copy() {
        return new CustomStretchRgbParameters(colorRange, isPerChannel, isSaturated, isBlackSaturated);
    }
    public static CustomStretchRgbParameters newEmpty() {
        return new CustomStretchRgbParameters(ColorRange.newEmptyRange(), true, true, false);
    }
    public static CustomStretchRgbParameters newFullRange() {
        return new CustomStretchRgbParameters(ColorRange.newFullRange(), true, true, false);
    }
    @Override
    public String toString() {
        return "CustomStretchRgbParameters{" +
                "colorRange=" + colorRange +
                ", isPerChannel=" + isPerChannel +
                ", isSaturated=" + isSaturated +
                ", isBlackSaturated=" + isBlackSaturated +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomStretchRgbParameters that = (CustomStretchRgbParameters) o;
        return  isPerChannel == that.isPerChannel &&
                isSaturated == that.isSaturated &&
                isBlackSaturated == that.isBlackSaturated &&
                Objects.equals(colorRange, that.colorRange);
    }
    @Override
    public int hashCode() {
        return Objects.hash(colorRange, isPerChannel, isSaturated, isBlackSaturated);
    }
}
class BehavioralOptions {
    boolean saveGif = true;
    boolean saveLeftRightImages = true;
    boolean useCustomCrosshairCursor = true;
}
class DisplayParameters {
    double zoom, zoomL, zoomR;
    int offsetX, offsetY;
    double angle, angleR, angleL;
    DebayerMode debayerL, debayerR;
    ColorCorrectionAlgo preFilterL, preFilterR; // either nothing or repair-broken-pixels, for Curiosity rear cam
    ImageResamplingMode imageResamplingModeL, imageResamplingModeR;
    ColorCorrection lColorCorrection, rColorCorrection;
    FisheyeCorrection lFisheyeCorrection, rFisheyeCorrection;

    public DisplayParameters() {
        setDefaults();
    }
    void setDefaults() {
        zoom = zoomL = zoomR = 1.;
        offsetX = offsetY = 0;
        angle = angleL = angleR = 0.;
        debayerL = debayerR = DebayerMode.getUiDefault();
        preFilterL = preFilterR = ColorCorrectionAlgo.DO_NOTHING;
        imageResamplingModeL = imageResamplingModeR = ImageResamplingMode.getUiDefault();
        lColorCorrection = rColorCorrection = new ColorCorrection(Collections.EMPTY_LIST, CustomStretchRgbParameters.newFullRange());
        lFisheyeCorrection = rFisheyeCorrection = FisheyeCorrection.defaultValue();
    }
    void setDefaultsMrMl() {
        setDefaults();
        zoomR = 3.;
        offsetX = -820;
        offsetY = 20;
    }
    private DisplayParameters(double zoom, double zoomL, double zoomR, int offsetX, int offsetY, double angle, double angleL, double angleR, DebayerMode debayerL, DebayerMode debayerR, ColorCorrectionAlgo preFilterL, ColorCorrectionAlgo preFilterR, ImageResamplingMode imageResamplingModeL, ImageResamplingMode imageResamplingModeR, ColorCorrection lColorCorrection, ColorCorrection rColorCorrection, FisheyeCorrection lFisheyeCorrection, FisheyeCorrection rFisheyeCorrection) {
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
        this.preFilterL = preFilterL;
        this.preFilterR = preFilterR;
        this.imageResamplingModeL = imageResamplingModeL;
        this.imageResamplingModeR = imageResamplingModeR;
        this.lColorCorrection = lColorCorrection;
        this.rColorCorrection = rColorCorrection;
        this.lFisheyeCorrection = lFisheyeCorrection;
        this.rFisheyeCorrection = rFisheyeCorrection;
    }
    public DisplayParameters swapped() {
        return new DisplayParameters(zoom, zoomR, zoomL, -offsetX, -offsetY, angle, angleR, angleL, debayerR, debayerL, preFilterR, preFilterL, imageResamplingModeR, imageResamplingModeL, rColorCorrection, lColorCorrection, rFisheyeCorrection, lFisheyeCorrection);
    }
    public DisplayParameters withColorCorrection(ColorCorrection lColorCorrection, ColorCorrection rColorCorrection) {
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, preFilterL, preFilterR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, lFisheyeCorrection, rFisheyeCorrection);
    }
    public DisplayParameters withColorCorrection(boolean isRight, ColorCorrection cc) {
        ColorCorrection lColorCorrection = isRight ? this.lColorCorrection : cc;
        ColorCorrection rColorCorrection = isRight ? cc : this.rColorCorrection;
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, preFilterL, preFilterR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, lFisheyeCorrection, rFisheyeCorrection);
    }
    public DisplayParameters withFisheyeCorrection(boolean isRight, FisheyeCorrection fc) {
        FisheyeCorrection lFisheyeCorrection = isRight ? this.lFisheyeCorrection : fc;
        FisheyeCorrection rFisheyeCorrection = isRight ? fc : this.rFisheyeCorrection;
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, preFilterL, preFilterR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, lFisheyeCorrection, rFisheyeCorrection);
    }
    public DisplayParameters withPreFilter(boolean isRight, ColorCorrectionAlgo algo) {
        ColorCorrectionAlgo preFilterL = isRight ? this.preFilterL : algo;
        ColorCorrectionAlgo preFilterR = isRight ? algo : this.preFilterR;
        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY, angle, angleL, angleR, debayerL, debayerR, preFilterL, preFilterR, imageResamplingModeL, imageResamplingModeR, lColorCorrection, rColorCorrection, lFisheyeCorrection, rFisheyeCorrection);
    }
    public DisplayParameters withPreFilter(boolean isRight, boolean isOn) {
        return withPreFilter(isRight, isOn
                                      ? ColorCorrectionAlgo.INTERPOLATE_BROKEN_PIXELS
                                      : ColorCorrectionAlgo.DO_NOTHING);
    }
    public ImageEffect[] getImageEffects(boolean isRight) {
        return isRight
            ? new ImageEffect[] {debayerR, preFilterR, rFisheyeCorrection.algo}
            : new ImageEffect[] {debayerL, preFilterL, lFisheyeCorrection.algo};
    }
//    public boolean isPreFilterOn(boolean isRight) {
//        return (isRight ? preFilterR : preFilterL).notNothing();
//    }
    ColorCorrection getColorCorrection(boolean isRight) {
        return isRight ? rColorCorrection : lColorCorrection;
    }
    FisheyeCorrection getFisheyeCorrection(boolean isRight) {
        return isRight ? rFisheyeCorrection : lFisheyeCorrection;
    }
    double getFullZoom(boolean isRight) {
        return zoom * (isRight ? zoomR : zoomL);
    }
}
class ImageAndPath {
    public static final String IN_PROGRESS_PATH = "..."; // pseudo-path, means "download in progress"
    public static final String NO_PATH = "-"; // pseudo-path, means "no path"
    public static final String ERROR_PATH = ""; // pseudo-path, means "error while loading path"
    public static final int DUMMY_SIZE = 12;
    static final SuccessFailureCounter SUCCESS_FAILURE_COUNTER = new SuccessFailureCounter();
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
        } else if(FileLocations.isCuriousLRUrn(path)) {
            String path1 = FileLocations.uncuriousUri(path);
            String imageid1 = FileLocations.getFileNameNoExt(path1);
            String ext1 = FileLocations.getFileExt(path1);
            Optional<String> match = MastcamPairFinder.findMrlMatch(imageid1).map(fname -> FileLocations.replaceFileName(path1, fname + ext1));
            String newPath = match.orElse("");
            return imageIoRead(newPath, newPath);
        } else if(FileLocations.isCuriousMUrn(path)) {
            String path1 = FileLocations.uncuriousUri(path);
            res = ImageMerger.assembleBigImageFromImagesLike(
                    path1,
                    url -> {
                        var img = imageIoReadNoExc(url, pathToLoad).image;
                        if (isDummyImage(img)) {
                            System.out.println("could not read " + url);
                            return null;
                        }
                        System.out.println("successfully read " + url);
                        return img;
                    }
            );
        } else if(FileLocations.isUrl(path)) {
            System.out.println("downloading "+path+ " ...");
            try {
                long startedAt = System.currentTimeMillis();
                URLConnection uc = new URL(path).openConnection();
                NasaReader.setHttpHeaders(uc);
                long openedAt = System.currentTimeMillis();
                try(
                    InputStream inputStream = uc.getInputStream()
                ) {
                    res = ImageIO.read(inputStream);
                    res.getWidth(); // throw an exception if null
                    long readAt = System.currentTimeMillis();
                    System.out.println("Read in " + (openedAt - startedAt) + ", " + (readAt - openedAt) + " ms");
                    System.out.println("downloaded " + path);
                    SUCCESS_FAILURE_COUNTER.countSuccess();
                } catch (IOException e) {
                    long errorAt = System.currentTimeMillis();
                    SUCCESS_FAILURE_COUNTER.countFailure();
                    System.out.println("Exception in " + (openedAt - startedAt) + ", " + (errorAt - openedAt) + " ms");
                    System.out.println("Was reading in thread: " + Thread.currentThread());
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
                            System.out.println("Error while printing the error stream: " + ex);
//                            ex.printStackTrace();
                            System.out.println("DISCONNECTING URLConnection...");
                            httpUc.disconnect();
                        }
                    }
                    System.out.println("original exception:");
                    e.printStackTrace();
                    System.out.println("could not download "+path);
                    System.out.println("Per-thread successes and failures:\n" + SUCCESS_FAILURE_COUNTER);
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
            throw new UncheckedIOException(e);
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
    boolean measurementShown;
    public MeasurementStatus(PanelMeasurementStatus left, PanelMeasurementStatus right) {
        this.left = left;
        this.right = right;
        isWaitingForPoint = false;
        pointIsWaitingFor = 0;
        isSubpixelPrecision = false;
        measurementShown = true;
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
        res.measurementShown = this.measurementShown;
        return res;
    }
    public MeasurementStatus copy() {
        var res = new MeasurementStatus(left.copy(), right.copy());
        res.measurementPointMark = this.measurementPointMark;
        res.stereoPairParameters = this.stereoPairParameters;
        res.isWaitingForPoint = this.isWaitingForPoint;
        res.pointIsWaitingFor = this.pointIsWaitingFor;
        res.isSubpixelPrecision = this.isSubpixelPrecision;
        res.measurementShown = this.measurementShown;
        return res;
    }
    public PanelMeasurementStatus getPanelMeasurementStatus(boolean isRight) {
        return isRight ? right : left;
    }

    @Override
    public String toString() {
        return "MeasurementStatus{" +
                "left=" + left +
                ", right=" + right +
                ", measurementPointMark=" + measurementPointMark +
                ", stereoPairParameters=" + stereoPairParameters +
                ", isWaitingForPoint=" + isWaitingForPoint +
                ", pointIsWaitingFor=" + pointIsWaitingFor +
                ", isSubpixelPrecision=" + isSubpixelPrecision +
                ", measurementShown=" + measurementShown +
                '}';
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
        int xToDrawFrom = Math.max(0, X3DViewer.mult(offX, zoomLevel));
        int yToDrawFrom = Math.max(0, X3DViewer.mult(offY, zoomLevel));

        for (int i=0; i<5; i++) {
            if (x[i] >= 0 && y[i] >= 0 && x[i] < img.getWidth() && y[i] < img.getHeight()) {
                if (res == img) {
                    res = copyImage(img);
                }
                measurementPointMark.drawMark(res, xToDrawFrom+X3DViewer.mult(x[i], zoomLevel), yToDrawFrom+X3DViewer.mult(y[i], zoomLevel), rgb[i]);
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
                " ifov=" + ifov +
                " c(" + centeringDX + "," + centeringDY + ")";
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
    BehavioralOptions behavioralOptions;

    final LRNavigator lrNavigator;
    boolean dndOneToBoth = true;
    boolean unthumbnail = true;
    volatile long lastLoadTimestampL;
    volatile long lastLoadTimestampR;
    public UiController(X3DViewer xv, LRNavigator lrn, RawData rd, DisplayParameters dp, MeasurementStatus ms, BehavioralOptions bo) {
        x3dViewer = xv;
        displayParameters = dp;
        rawData = rd;
        measurementStatus = ms;
        lrNavigator = lrn;
        behavioralOptions = bo;
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
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
    }
    @Override
    public void rImageResamplingModeChanged(ImageResamplingMode newImageResamplingModeR) {
        displayParameters.imageResamplingModeR = newImageResamplingModeR;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
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
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
        lrNavigator.swap();
    }
    @Override
    public void swapImageUrls() {
        System.out.println("swapImageUrls "+Thread.currentThread());
        x3dViewer.updateViews(rawData = rawData.swapped(), displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
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
    public void loadBigImage(boolean isRight) {
        boolean toEnable = !FileLocations.isCuriousMUrn((isRight ? rawData.right : rawData.left).pathToLoad);
        var paths = Arrays.asList(
                FileLocations.setUriCuriousM(rawData.left.pathToLoad, toEnable),
                FileLocations.setUriCuriousM(rawData.right.pathToLoad, toEnable)
        );
        var uPaths = unThumbnailIfNecessary(paths);
        lrNavigator
                .onSetLeft(rawData.left.path, uPaths.get(0))
                .onSetRight(rawData.right.path, uPaths.get(1));
        showInProgressViewsAndThen(uPaths,
                () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
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
    public void saveProcessedImage(boolean isRight) {
        try {
            String urlOrPath = isRight ? rawData.right.path : rawData.left.path;
            RenderedImage bi = (RenderedImage)
                    ((ImageIcon) (isRight ? x3dViewer.lblR : x3dViewer.lblL).getIcon()).getImage();
            x3dViewer.processedImageSaver.showSaveDialog(
                    x3dViewer.frame,
                    urlOrPath,
                    imgFile -> {
                        String effects = isRight
                            ? displayParameters.rColorCorrection.getShortDescription(rawData.right.path, displayParameters.getImageEffects(isRight))
                            : displayParameters.lColorCorrection.getShortDescription(rawData.left.path, displayParameters.getImageEffects(isRight));
                        if (effects.trim().isEmpty()) {
                            effects = "none";
                        }
                        String effects2 = isRight
                            ? displayParameters.rColorCorrection.getFullDescription(rawData.right.path, displayParameters.getImageEffects(isRight))
                            : displayParameters.lColorCorrection.getFullDescription(rawData.left.path, displayParameters.getImageEffects(isRight));
                        if (effects2.trim().isEmpty()) {
                            effects2 = "none";
                        }

                        String fisheyeDescr = "";
                        if(displayParameters.getFisheyeCorrection(isRight).algo.notNothing()) {
                            fisheyeDescr = displayParameters.getFisheyeCorrection(isRight).parametersToString() + "\n";
                        }
                        String description =
                                ( FileLocations.isNonLocalUrl(urlOrPath)
                                ? "Original URL: " + urlOrPath + "\n"
                                : ""
                                )
                                + "Exported from the Curious X3D Viewer.\n"
                                + "Effects: "+effects + "\n"
                                + "Effects_verbose: "+effects2 + "\n"
                                + fisheyeDescr;
                        ScreenshotSaver.writePng(imgFile, bi,
                                "Software", "Curious: X3D Viewer",
                                "Description", description);
                        System.out.println("Saved " + imgFile + ":\n" + description);
                    }
            );
        } catch (Exception e) {
            System.out.println("Could not save file");
            e.printStackTrace();
        }
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
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
        // TODO: reset measurementStatus
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }

    @Override
    public void saveScreenshot() {
        x3dViewer.screenshotSaver.takeAndSaveScreenshot(x3dViewer.frame, x3dViewer.componentL, x3dViewer.componentR, rawData, displayParameters, behavioralOptions);
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
            var panelMStatus = isRight ? measurementStatus.right : measurementStatus.left;
            Point xy = mouseEventToPoint(isRight, e);
            int x = xy.x;
            int y = xy.y;
            switch (measurementStatus.pointWaitingFor()) {
                case 1:
                    panelMStatus.x1 = x;
                    panelMStatus.y1 = y;
                    break;
                case 2:
                    panelMStatus.x2 = x;
                    panelMStatus.y2 = y;
                    break;
                case 3:
                    panelMStatus.x3 = x;
                    panelMStatus.y3 = y;
                    break;
                case 4:
                    panelMStatus.x4 = x;
                    panelMStatus.y4 = y;
                    break;
                case 5:
                    panelMStatus.x5 = x;
                    panelMStatus.y5 = y;
                    break;
            }
            measurementStatus.clearWaitingForPoint();
            if (measurementStatus.measurementShown) {
                x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
            }
            x3dViewer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    Point mouseEventToPoint(boolean isRight, MouseEvent e) {
        var panelMStatus = isRight ? measurementStatus.right : measurementStatus.left;
        var otherMStatus = !isRight ? measurementStatus.right : measurementStatus.left;
        System.out.println("{markPointWithMousePress r="+isRight+" x="+e.getX()+" y="+e.getY()+" btn="+e.getButton());
        System.out.println("e="+e);
        var zoom = displayParameters.zoom * (isRight ? displayParameters.zoomR : displayParameters.zoomL);
        var offX = isRight ? -displayParameters.offsetX : displayParameters.offsetX;
        var offY = isRight ? -displayParameters.offsetY : displayParameters.offsetY;
        int marginX, marginY;
        {
            JButton source = (JButton) e.getSource();
            ImageIcon icon = (ImageIcon) source.getIcon();
            marginX = Math.max(0, (source.getWidth() - icon.getIconWidth()) / 2);
            marginY = Math.max(0, (source.getHeight() - icon.getIconHeight()) / 2);
        }
        var res = new Point(
            (int) ((e.getX() - marginX) / zoom
                    - Math.max(0, offX - otherMStatus.centeringDX + panelMStatus.centeringDX)
            ),
            (int) ((e.getY() - marginY) / zoom
                    - Math.max(0, offY - otherMStatus.centeringDY + panelMStatus.centeringDY)
            )
        );
        System.out.println(""+measurementStatus);
        System.out.println("zoom="+zoom+" marginX="+marginX+" marginY="+marginY+" offX="+offX+" offY="+offY);
        System.out.println("res="+res);
        System.out.println("}");
        return res;
    }

    @Override
    public void setWaitingForPoint(int forPointNumber) {
        measurementStatus.setWaitingForPoint(forPointNumber);
        x3dViewer.setCursor(CustomCursorMaker.getCrosshairCursor(behavioralOptions));
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
        measurementStatus.measurementShown = newIsShown;
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
    }
    static int calculateOffsetBigImgSmallImg(double l, double r, int lCenteringD, int rCenteringD, double zl, double zr) {
        if (lCenteringD != 0) {
            System.out.println("Error: calculateOffsetBigImgSmallImg with non-zero lCenteringD="+lCenteringD);
        }
//        System.out.println("{calculateOffsetBigImgSmallImg("+l+","+r+", "+lCenteringD+","+rCenteringD+", "+zl+","+zr+")");
        double diff = r*zr - l*zl;
        double offset;
        if (diff >= 0) {
            offset = diff/zl + rCenteringD;
        } else {
            offset = diff/zr + rCenteringD;
        }
//        System.out.println("}diff="+diff+" offset="+offset);
        return (int)offset;
    }
    static int calculateOffset(double l, double r, int lCenteringD, int rCenteringD, double zoomL, double zoomR) {
//        System.out.println("--------------calculateOffset("+l+","+r+", "+lCenteringD+","+rCenteringD+", "+zoomL+","+zoomR+")");
        if (lCenteringD == 0) {
            return calculateOffsetBigImgSmallImg(l,r,lCenteringD,rCenteringD,zoomL,zoomR);
        } else {
            // (small img, big img): swap and negate offset
            return -calculateOffsetBigImgSmallImg(r,l,rCenteringD,lCenteringD,zoomR,zoomL);
        }
    }
    @Override
    public void adjustOffsets(int pointId) {
        doAdjustOffsets(pointId, displayParameters, measurementStatus);
        x3dViewer.updateViews(rawData, displayParameters, measurementStatus);
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
    }
    static void doAdjustOffsets(int pointId, DisplayParameters displayParameters, MeasurementStatus measurementStatus) {
        switch (pointId) {
            case 1: {
                displayParameters.offsetX =
                        calculateOffset(
                                measurementStatus.left.x1,
                                measurementStatus.right.x1,
                                measurementStatus.left.centeringDX,
                                measurementStatus.right.centeringDX,
                                displayParameters.zoomL,
                                displayParameters.zoomR
                        );
                displayParameters.offsetY =
                        calculateOffset(
                                measurementStatus.left.y1,
                                measurementStatus.right.y1,
                                measurementStatus.left.centeringDY,
                                measurementStatus.right.centeringDY,
                                displayParameters.zoomL,
                                displayParameters.zoomR
                        );
            } break;
            case 2: {
                displayParameters.offsetX =
                        calculateOffset(
                                measurementStatus.left.x2,
                                measurementStatus.right.x2,
                                measurementStatus.left.centeringDX,
                                measurementStatus.right.centeringDX,
                                displayParameters.zoomL,
                                displayParameters.zoomR
                        );
                displayParameters.offsetY =
                        calculateOffset(
                                measurementStatus.left.y2,
                                measurementStatus.right.y2,
                                measurementStatus.left.centeringDY,
                                measurementStatus.right.centeringDY,
                                displayParameters.zoomL,
                                displayParameters.zoomR
                        );
            } break;
        }
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
    public ColorRange getViewportColorRange(boolean isRight, boolean ignoreBroken) {
        Rectangle visibleArea = x3dViewer.getViewportRectangle(isRight);
        ColorCorrection lcc = insertSrgb3IfNotThere(displayParameters.lColorCorrection);
        ColorCorrection rcc = insertSrgb3IfNotThere(displayParameters.rColorCorrection);
        var dp = displayParameters
                .withColorCorrection(lcc, rcc);
        var bis = x3dViewer.processBothImages(rawData, dp, measurementStatus, ColorCorrection.Command.GET_RANGE);
        var bi = bis.get(isRight ? 1 : 0);
        int d = (int) Math.round(displayParameters.getFullZoom(isRight));
        var cr = ColorBalancer.getColorRangeFromImage(visibleArea, bi, ignoreBroken, d);
        return cr;
    }
    @Override
    public CustomStretchRgbParameters getCurrentCustomStretchRgbParameters(boolean isRight) {
        return displayParameters.getColorCorrection(isRight).customStretchRgbParameters;
    }
    @Override
    public FisheyeCorrection getFisheyeCorrection(boolean isRight) {
        return isRight
             ? displayParameters.rFisheyeCorrection
             : displayParameters.lFisheyeCorrection;
    }
    @Override
    public ColorCorrectionAlgo getPreFilter(boolean isRight) {
        return isRight
             ? displayParameters.preFilterR
             : displayParameters.preFilterL;
    }
    @Override
    public Dimension getRawImageDimensions(boolean isRight) {
        BufferedImage bi = isRight ? rawData.right.image : rawData.left.image;
        return new Dimension(bi.getWidth(), bi.getHeight());
    }
    @Override
    public void setFisheyeCorrection(boolean isRight, FisheyeCorrection fc) {
        System.out.println("setFisheyeCorrection isRight=" + isRight + " fc=" + fc);
        var newDp = displayParameters.withFisheyeCorrection(isRight, fc);
        x3dViewer.updateViews(rawData, displayParameters=newDp, measurementStatus);
    }
    @Override
    public void setPreFilter(boolean isRight, boolean isOn) {
        var newDp = displayParameters.withPreFilter(isRight, isOn);
        x3dViewer.updateViews(rawData, displayParameters=newDp, measurementStatus);
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
        behavioralOptions.saveGif = saveGif;
        behavioralOptions.saveLeftRightImages = saveLeftRIght;
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
    }
    @Override
    public void setUseCustomCrosshairCursor(boolean useCustomCrosshairCursor) {
        behavioralOptions.useCustomCrosshairCursor = useCustomCrosshairCursor;
        x3dViewer.updateControls(displayParameters, measurementStatus, behavioralOptions);
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
    FisheyeCorrectionPane fisheyeCorrectionPane;
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
    ProcessedImageSaver processedImageSaver = new ProcessedImageSaver(new JFileChooser());

    public void updateControls(DisplayParameters dp, MeasurementStatus ms, BehavioralOptions bo) {
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
        showMeasurementCbMenuItem.setState(ms.measurementShown);
        measurementPanel.setControls(ms);
        settingsPanel.setControls(bo);
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
//        BufferedImage imgL = dp.debayerL.doAlgo(rd.left.image, () -> FileLocations.isBayered(rd.left.path), Debayer.debayering_methods);
//        BufferedImage imgR = dp.debayerR.doAlgo(rd.right.image, () -> FileLocations.isBayered(rd.right.path), Debayer.debayering_methods);
        BufferedImage imgL = dp.debayerL.doAlgo2(rd.left.image, rd.left.path);
        BufferedImage imgR = dp.debayerR.doAlgo2(rd.right.image, rd.right.path);

        System.out.println("processBothImages L:" + dp.preFilterL);
        System.out.println("processBothImages R:" + dp.preFilterR);
        System.out.println("processBothImages L:" + dp.lFisheyeCorrection);
        System.out.println("processBothImages R:" + dp.rFisheyeCorrection);
        if (dp.preFilterL.notNothing()) {
            imgL = ColorBalancer.interpolateBrokenPixels(imgL);
        }
        if (dp.preFilterR.notNothing()) {
            imgR = ColorBalancer.interpolateBrokenPixels(imgR);
        }
        // barrel distortion correction
        imgL = dp.lFisheyeCorrection.doFisheyeCorrection(imgL);
        imgR = dp.rFisheyeCorrection.doFisheyeCorrection(imgR);

        imgL = dp.lColorCorrection.doColorCorrection(imgL, command);
        imgR = dp.rColorCorrection.doColorCorrection(imgR, command);

        ms.left.setWHI(imgL, ms.stereoPairParameters.ifovL, "pane:L eye:R");
        ms.right.setWHI(imgR, ms.stereoPairParameters.ifovR, "pane:R eye:L");
        if (!PRECISE_MARKS && ms.measurementShown) {
            imgL = ms.left.drawMarks(imgL, ms.measurementPointMark);
            imgR = ms.right.drawMarks(imgR, ms.measurementPointMark);
        }

        AffineTransform transformL = rotationTransform(imgL, dp.angle + dp.angleL);
        AffineTransform transformR = rotationTransform(imgL, dp.angle + dp.angleR);
        BufferedImage rotatedL = rotate(imgL, transformL);
        BufferedImage rotatedR = rotate(imgR, transformR);
//        System.out.println("----");
        double dw1 = (rotatedR.getWidth()*dp.zoomR - rotatedL.getWidth()*dp.zoomL) / 2;
        double dh1 = (rotatedR.getHeight()*dp.zoomR - rotatedL.getHeight()*dp.zoomL) / 2;
        int dwR = (int) (dw1/dp.zoomR);
        int dwL = (int) (dw1/dp.zoomL);
        int dhR = (int) (dh1/dp.zoomR);
        int dhL = (int) (dh1/dp.zoomL);
        ms.left.centeringDX = max0(dwL);
        ms.left.centeringDY = max0(dhL);
        ms.right.centeringDX = max0(-dwR);
        ms.right.centeringDY = max0(-dhR);
        double zL = dp.zoom * dp.zoomL;
        double zR = dp.zoom * dp.zoomR;
        int offXL = dp.offsetX + ms.left.centeringDX - ms.right.centeringDX;
        int offYL = dp.offsetY + ms.left.centeringDY - ms.right.centeringDY;
        if (!PRECISE_MARKS || !ms.measurementShown) {
            return Arrays.asList(
                    zoom(rotatedL, zL, rotatedR, zR, offXL, offYL, dp.imageResamplingModeL),
                    zoom(rotatedR, zR, rotatedL, zL, -offXL, -offYL, dp.imageResamplingModeR)
            );
        } else {
            return Arrays.asList(
                ms.left.drawMarks(
                    zoom(rotatedL, zL, rotatedR, zR, offXL, offYL, dp.imageResamplingModeL),
                    ms.measurementPointMark, transformL, zL, offXL, offYL
                ),
                ms.right.drawMarks(
                    zoom(rotatedR, zR, rotatedL, zL, -offXL, -offYL, dp.imageResamplingModeR),
                    ms.measurementPointMark, transformR, zR, -offXL, -offYL
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
                String leftTime = RoverTime.earthDateForFile(leftFileName);
                String rightTime = RoverTime.earthDateForFile(rightFileName);
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
                colorCorrectionDescriptionL.setText(dp.lColorCorrection.getShortDescription(rd.left.path, dp.getImageEffects(false)));
                colorCorrectionDescriptionR.setText(dp.rColorCorrection.getShortDescription(rd.right.path, dp.getImageEffects(true)));
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
        fisheyeCorrectionPane = new FisheyeCorrectionPane(uiEventListener);
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
            // create urlPanel
            final int LEFT = 1;
            final int MID = 2;
            final int RIGHT = 3;
            final double LIGHTWEIGHT = .1;
            final double HEAVYWEIGHT = 100.;
            {
                GridBagConstraints gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = MID;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = GridBagConstraints.EAST;
                gridBagConstraints.weightx = LIGHTWEIGHT;
                urlPanel.add(urlL, gridBagConstraints);
            }
            {
                GridBagConstraints gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = RIGHT;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = GridBagConstraints.EAST;
                gridBagConstraints.weightx = HEAVYWEIGHT;
                urlPanel.add(colorCorrectionDescriptionL, gridBagConstraints);
            }
            {
                // An empty label to have something on the left
                GridBagConstraints gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = LEFT;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = GridBagConstraints.WEST;
                gridBagConstraints.weightx = HEAVYWEIGHT;
                urlPanel.add(new JLabel(""), gridBagConstraints);
            }
            {
                GridBagConstraints gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = MID;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = GridBagConstraints.EAST;
                gridBagConstraints.weightx = LIGHTWEIGHT;
                urlPanel.add(urlR, gridBagConstraints);
            }
            {
                GridBagConstraints gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = RIGHT;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = GridBagConstraints.EAST;
                gridBagConstraints.weightx = HEAVYWEIGHT;
                urlPanel.add(colorCorrectionDescriptionR, gridBagConstraints);
            }
            // done with urlPanel
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
                JMenuItem miAdjustOffsets = new JMenuItem("Adjust Offsets Using Red Measurement Marks");
                menuLR.add(miAdjustOffsets);
                miAdjustOffsets.addActionListener(e ->
                        uiEventListener.adjustOffsets(1)
                );
            }
            {
                JMenuItem miAdjustOffsets = new JMenuItem("Adjust Offsets Using Green Measurement Marks");
                menuLR.add(miAdjustOffsets);
                miAdjustOffsets.addActionListener(e ->
                        uiEventListener.adjustOffsets(2)
                );
            }
            {
                String menuTitle = "Measurement...";
                JMenu mMeasure = new JMenu(menuTitle);
                menuLR.add(mMeasure);
                {
                    String title = "Show Marks";
                    showMeasurementCbMenuItem = new JCheckBoxMenuItem(title, ms.measurementShown);
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
                            "<b>Alt 1</b> set the 1st (red) mark<br>" +
                            "<b>Alt 2</b> set the 2nd (green) mark<br>" +
                            "<b>Alt 3</b> set the 3rd (blue) mark<br>" +
                            "<b>Alt 4</b> set the 4th (cyan) mark<br>" +
                            "<b>Alt 5</b> set the 5th (magenta) mark<br>" +
                            "<br>" +
                            "It's not really about measurement, but it is possible to use the red and green (1st and 2nd) marks to set both offsets at once (horizontal and vertical).<br>" +
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
                JMenuItem miFisheye = new JMenuItem("Fisheye Correction...");
                menuLR.add(miFisheye);
                miFisheye.addActionListener(e ->
                        fisheyeCorrectionPane.showDialogIn(frame)
                );
            }
            {
                JMenuItem miLoadMatch = new JMenuItem("Assemble Big Image (Perseverance NAV/HAZ, Slow!)");
                menuLR.add(miLoadMatch);
                miLoadMatch.addActionListener(e ->
                        uiEventListener.loadBigImage(
                                isFromComponentsMenu(e, lblR)
                        ));
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
                JMenuItem miSave = new JMenuItem("Save Processed Image As...");
                menuLR.add(miSave);
                miSave.addActionListener(e ->
                        uiEventListener.saveProcessedImage(
                                isFromComponentsMenu(e, lblR)
                        ));
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
                settingsButton.setToolTipText("Settings...");
                statusPanel2.add(settingsButton);
            }
            {
                JButton colorButton = new JButton();
                DigitalZoomControl.loadIcon(colorButton,"icons/colors24.png","color");
                colorButton.setToolTipText("Color correction...");
                colorButton.addActionListener(e -> {
                    colorCorrectionPane.showDialogIn(frame);
                });
                statusPanel2.add(colorButton);
            }

            statusPanel.add(dcZoom = new  DigitalZoomControl<Double, ZoomFactorWrapper>().init("zoom:",4, new ZoomFactorWrapper(), d -> uiEventListener.zoomChanged(d)));
            statusPanel.add(dcZoomL = new DigitalZoomControl<Double, ZoomFactorWrapper>().init("zoomL:",4, new ZoomFactorWrapper(), d -> uiEventListener.lZoomChanged(d)));
            statusPanel.add(dcZoomR = new DigitalZoomControl<Double, ZoomFactorWrapper>().init("zoomR:",4, new ZoomFactorWrapper(), d -> uiEventListener.rZoomChanged(d)));

            statusPanel.add(dcOffX = new DigitalZoomControl<Integer, OffsetWrapper>().init("offsetX:", 4, new OffsetWrapper("[←|→]:  ", "[→|←]:  "), i -> uiEventListener.xOffsetChanged(i)));
            statusPanel.add(dcOffY = new DigitalZoomControl<Integer, OffsetWrapper>().init("offsetY:", 4, new OffsetWrapper("[↑|↓]:  ", "[↓|↑]:  "), i -> uiEventListener.yOffsetChanged(i)));

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
                        "<b>Alt 1/Alt 2/Alt 3/Alt 4/Alt 5</b>: Set the marks 1-5, red/green/blue/cyan/magenta correspondingly<br>" +
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
                        "If the \"DnD BOTH\" box is not checked, the dropped image/url just replaces the image on which it was dropped.<br>" +
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
                            "; Java " + BuildVersion.getJavaVersion() +
                            " (" + BuildVersion.getVmVersion() + ")" +
//                            ", OS: " + BuildVersion.getOsVersion() +
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
                helpButton.addActionListener(e ->
                    JOptionPane.showMessageDialog(frame, helpText, "help", JOptionPane.PLAIN_MESSAGE)
                );
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
            frameInputMap.put(KeyStroke.getKeyStroke("alt 1"), "markred");
            frameInputMap.put(KeyStroke.getKeyStroke("alt 2"), "markgreen");
            frameInputMap.put(KeyStroke.getKeyStroke("alt 3"), "markblue");
            frameInputMap.put(KeyStroke.getKeyStroke("alt 4"), "markcyan");
            frameInputMap.put(KeyStroke.getKeyStroke("alt 5"), "markmagenta");
            frameActionMap.put("markred", toAction(e->uiEventListener.setWaitingForPoint(1)));
            frameActionMap.put("markgreen", toAction(e->uiEventListener.setWaitingForPoint(2)));
            frameActionMap.put("markblue", toAction(e->uiEventListener.setWaitingForPoint(3)));
            frameActionMap.put("markcyan", toAction(e->uiEventListener.setWaitingForPoint(4)));
            frameActionMap.put("markmagenta", toAction(e->uiEventListener.setWaitingForPoint(5)));
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
        System.out.println(
                "zoom("+originalImage.getWidth()+"x"+originalImage.getHeight()+
                ", zoomLevel="+zoomLevel+", otherImage:"+ otherImage.getWidth()+"x"+otherImage.getHeight()+
                ", otherZoomLevel="+otherZoomLevel+", offX="+offX+", offY="+offY+ ", imageResamplingMode="+ imageResamplingMode+
                ")\n" +
                " newImageWidth=" + newImageWidth +
                " newImageHeight=" + newImageHeight +
                " otherImageWidth=" + otherImageWidth +
                " otherImageHeight=" + otherImageHeight + "\n" +
                " thisCanvasWidth=" + thisCanvasWidth +
                " thisCanvasHeight=" + thisCanvasHeight +
                " otherCanvasWidth=" + otherCanvasWidth +
                " otherCanvasHeight=" + otherCanvasHeight +
                " canvasWidth=" + canvasWidth +
                " canvasHeight=" + canvasHeight + "\n" +
                " xToDrawFrom=" + xToDrawFrom +
                " yToDrawFrom=" + yToDrawFrom +
                " newImageWidth=" + newImageWidth +
                " newImageHeight=" + newImageHeight
        );
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
    static int max0(int x) {
        return Math.max(0, x);
    }
    static double max0(double x) {
        return Math.max(0., x);
    }

}

enum OneOrBothPanes {JUST_THIS, BOTH_PANES, SEE_CHECKBOX};

enum ImageResamplingMode {
    NEAREST(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, "\"Nearest Neighbor\" value interpolation"),
    BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR, "Bilinear value interpolation"),
    BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC, "Biquadratic value interpolation");

    final Object renderingHint;
    final String description;
    ImageResamplingMode(Object hint, String descr) {
        renderingHint = hint;
        description = descr;
    }
    public Object getRenderingHint() {
        return renderingHint;
    }
    public String getDescription() { return description; }
    static ImageResamplingMode getUiDefault(){
        return BICUBIC;
    }
};
class ImageResamplingModeChooser extends ComboBoxWithTooltips<ImageResamplingMode> {
    static ImageResamplingMode[] modes = ImageResamplingMode.values();
    public ImageResamplingModeChooser(Consumer<ImageResamplingMode> valueListener) {
        super(modes, ImageResamplingMode::getDescription);
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
enum DebayerMode implements ImageEffect {
    NEVER(false,-1) {
        @Override public boolean notNothing() { return false; }
        @Override public String effectShortName() { return ""; }
    },
    AUTO0(false,0), AUTO1(false,1), AUTO2(false,2), AUTO3(false,3), AUTO4(false,4), AUTO5(false,5),
    FORCE0(true,0), FORCE1(true,1), FORCE2(true,2), FORCE3(true,3), FORCE4(true,4), FORCE5(true,5);
    boolean force;
    int algo;
    DebayerMode(boolean force, int algo) {
        this.force = force;
        this.algo = algo;
    }
//    <T> T doAlgo(T data, Supplier<Boolean> autoCheck, List<Function<T,T>> algorithms) {
//        return (algo >= 0 && (force || autoCheck.get()))
//                ? algorithms.get(algo).apply(data)
//                : data
//                ;
//    }
//    BufferedImage doAlgo1(BufferedImage bi, Supplier<Boolean> autoCheck) {
//        return doAlgo(bi, autoCheck, debayering_methods);
//    }
    BufferedImage doAlgo2(BufferedImage orig, String path) {
        return  notNothingFor(path)
                ? debayering_methods.get(algo).apply(orig)
                : orig
                ;
    }
    private static String[] names = {
            "as is", //-1
            "demonstrate color encoding in the Bayer mosaic", //0
            "decode color (one color value for a 2x2 square)", //1
            "decode color (average color intensity)", //2
            "closest match clockwise Bayer pattern demosaicing", //3
            "closest match clockwise Bayer pattern demosaicing (v2)", //4
            "cubic/bicubic Bayer pattern demosaicing", //5
    };
    static List<Function<BufferedImage, BufferedImage>> debayering_methods = Arrays.asList(
            Debayer::debayer_dotted,
            Debayer::debayer_squares,
            Debayer::debayer_avg,
            Debayer::debayer_closest_match_square,
            Debayer::debayer_closest_match_WNSE_clockwise,
            DebayerBicubic::debayer_bicubic
    );
    static DebayerMode getUiDefault(){
        return AUTO5;
    }

    @Override
    public String effectName() {
        return names[algo+1];
    }
    @Override
    public String effectShortName() {
        return "dm" + algo;
    }
    @Override
    public boolean notNothingFor(String path) {
        return (algo >= 0 && (force || FileLocations.isBayered(path)));
    }
}
class DebayerModeChooser extends ComboBoxWithTooltips<DebayerMode> {
    static DebayerMode[] modes = DebayerMode.values();
    public DebayerModeChooser(Consumer<DebayerMode> valueListener) {
        super(modes, ImageEffect::effectName);
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
    T getSafeValue() {
        return valueWrapper.getSafeValue();
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
    int[] increments = {3,30,1,100}; // [+] [++] [shift+] [shift++]
    final String tooltipPrefixForMinus, tooltipPrefixForPlus;

    public OffsetWrapper() {
        tooltipPrefixForMinus = tooltipPrefixForPlus = "";
    }
    public OffsetWrapper(String forMinus, String forPlus) {
        tooltipPrefixForMinus = forMinus;
        tooltipPrefixForPlus = forPlus;
    }

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
        String pref = sign < 0 ? tooltipPrefixForMinus : tooltipPrefixForPlus;
        return String.format("%s%+d, Shift: %+d", pref, sign*increments[index1], sign*increments[index2]);
    }
}
class OffsetWrapper2 extends RotationAngleWrapper {
    static final double[] INCREMENTS = {1, 3, 30, 100};
    {
        increments = INCREMENTS;
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
class SizeChangeWrapper extends ZoomFactorWrapper {
    @Override
    void reset() {
        value = 2.0;
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
        if (isUrl(urlOrFile) || isCuriousLRUrn(urlOrFile) || !isProblemWithFile(unthumbnailed)) {
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
    // support Perseverance -- it works
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
        return isCuriousLRUrn(path) ? isCuriousRUrn(path) : isMr(fname);
    }
    static boolean isMrlMarkedL(String path, String fname) {
        return isCuriousLRUrn(path) ? isCuriousLUrn(path) : isMl(fname);
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
    static boolean isCuriousLRUrn(String path) {
        return isCuriousLUrn(path) || isCuriousRUrn(path);
    }
    // big (4x4 or 4x3) image assembled from multiple images, Perseverance only
    static boolean isCuriousMUrn(String path) {
        return path.startsWith("curious:m:");
    }
    // left/right match for URI
    static boolean isCuriousRUrn(String path) {
        return path.startsWith("curious:r:");
    }
    static boolean isCuriousLUrn(String path) {
        return path.startsWith("curious:l:");
    }
    static String uncuriousUri(String path) {
        if (isCuriousLRUrn(path)||isCuriousMUrn(path)) {
            return path.substring("curious:l:".length());
        } else {
            return path;
        }
    }
    static String setUriCuriousM(String path, boolean enable) {
        if (enable) {
            return isCuriousMUrn(path)
                 ? path
                 : "curious:m:" + path;
        } else {
            return isCuriousMUrn(path)
                 ? uncuriousUri(path)
                 : path;
        }
    }
    static boolean isUrl(String path) {
        String prefix = path.substring(0,Math.min(9, path.length())).toLowerCase();
        var res = prefix.startsWith("http:/") || prefix.startsWith("https:/") || prefix.startsWith("file:/");
        //System.out.println("prefix=["+prefix+"] => "+res+" "+Thread.currentThread());
        return res;
    }
    static boolean isNonLocalUrl(String path) {
        String prefix = path.substring(0,Math.min(9, path.length())).toLowerCase();
        var res = prefix.startsWith("http:/") || prefix.startsWith("https:/");
        return res;
    }
    static boolean isBayered(String urlOrPath) {
        String fname = getFileName(urlOrPath);
        final String MARS_PERSEVERANCE_ = "mars_perseverance_";
        if (fname.toLowerCase().startsWith(MARS_PERSEVERANCE_)) {
            fname = fname.substring(MARS_PERSEVERANCE_.length());
        }
        return fname.matches(".*\\d+M[RL]\\d+[CK]00_DXXX.*")
            || fname.matches("N[RL]E_\\d{4}_\\d+_\\d+ECM_N\\d+NCAM\\d+_\\d{2}_0LLJ.*")
            || fname.matches("Z[RL]\\d_\\d{4}_\\d+_\\d+ECM_N\\d+ZCAM\\d+_\\d{4}LMJ.*")
            || fname.matches("L[RL]E_\\d{4}_\\d+_\\d+ECM_N\\d+SCAM\\d+_\\d{4}I\\dJ.*")
            || fname.matches("F[RL]E_\\d{4}_\\d+_\\d+ECM_N\\d+FHAZ\\d+_\\d{2}_0L[LMU]J.*")
            || fname.matches("E[ADSU]E_\\d{4}_\\d+_\\d+ECM_N\\d+EDLC\\d+_\\d{4}L[MU]J.*")
            || fname.matches("CCE_\\d{4}_\\d+_\\d+ECM_N\\d+CACH\\d+_\\d{2}_0LLJ.*")
            || fname.matches("SI\\d_\\d{4}_\\d+_\\d+ECM_N\\d+SRLC\\d+_\\d+L[MU]J.*");
    }
    static WhichRover getWhichRover(String urlOrPath) {
        String fname = getFileName(urlOrPath);
        if (
            fname.matches("[A-Z]{2}[A-Z0-9]_\\d{4}_\\d{10}_\\d{3}[A-Z]{3}_N\\d{7}[A-Z_]{4}\\d{5}_\\d{2}[0-9_]\\d[0-9A-Z_]{2}[JA]\\d{2}.*")
            //                    FRE            _ 0477 _ 0709296768                    FHAZ     02008_     01_      0 LL          J    01.png
            //                                                  _ 777  ECM     _N 0261004
        ) {
            return WhichRover.PERSEVERANCE;
        }
        return WhichRover.CURIOSITY;
    }
}

abstract class RoverTime {
    private static final int MIN_CHARS_IN_TIMESTAMP = 6;
    private static final int TIMESTAMP_OFFSET_PERSEVERANCE = 9;
    private static final int TIMESTAMP_OFFSET_CURIOSITY = 4;

    public static long toUtcMillisC(long roverTimestamp) {
        return Math.round(roverTimestamp*1.000009468 + 946724361)*1000L;
    }
    public static long toUtcMillisP(long roverTimestamp) {
        return (Math.round((roverTimestamp-666952977L)*1.000007886) + 1613681069L)*1000L;
    }
    public static long toUtcMillis(long roverTimestamp, WhichRover rover) {
        return rover == WhichRover.PERSEVERANCE
             ? toUtcMillisP(roverTimestamp)
             : toUtcMillisC(roverTimestamp);
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
    public static String earthDateForFile(String s) {
        var rover = FileLocations.getWhichRover(s);
        int offset = rover == WhichRover.PERSEVERANCE ? TIMESTAMP_OFFSET_PERSEVERANCE : TIMESTAMP_OFFSET_CURIOSITY;
        long ts = parseTimestamp(offset, s);
        if (ts == 0) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String res  = dateFormat.format(new Date(toUtcMillis(ts, rover)));
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
}

class Pair<T> {
    public Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public final T first, second;

    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?> pair = (Pair<?>) o;

        if (!Objects.equals(first, pair.first)) return false;
        return Objects.equals(second, pair.second);
    }
    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}

class Debayer {
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
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent ancestorEvent) {
                enableHrefTooltips();
            }
            @Override
            public void ancestorRemoved(AncestorEvent ancestorEvent) {
                disableHrefTooltips();
            }
            @Override
            public void ancestorMoved(AncestorEvent ancestorEvent) {
            }
        });
    }
    void enableHrefTooltips() {
        ToolTipManager.sharedInstance().registerComponent(HyperTextPane.this);
    }
    void disableHrefTooltips() {
        ToolTipManager.sharedInstance().unregisterComponent(HyperTextPane.this);
    }
    @Override
    public String getToolTipText(MouseEvent evt) {
        // return href destination to be shown as a tooltip
        String text = null;
        int pos = viewToModel(evt.getPoint());
        if (pos >= 0) {
            HTMLDocument hdoc = (HTMLDocument) getDocument();
            javax.swing.text.Element e = hdoc.getCharacterElement(pos);
            AttributeSet a = e.getAttributes();

            SimpleAttributeSet value = (SimpleAttributeSet) a.getAttribute(HTML.Tag.A);
            if (value != null) {
                String href = (String) value.getAttribute(HTML.Attribute.HREF);
                if (href != null) {
                    text = href;
                }
            }
        }
        return text;
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

abstract class SaverBase {
    static boolean endsWithIgnoreCase(String text, String suffix) {
        if (text.length() < suffix.length()) {
            return false;
        }
        String textSuffix = text.substring(text.length() - suffix.length());
        return textSuffix.equalsIgnoreCase(suffix);
    }

    static boolean checkAskOverwrite(JFrame frame, File file) {
        return !file.exists()
            || JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(frame, "File " + file + " already exists. Choose a different name?", "Overwrite?", JOptionPane.YES_NO_OPTION);
    }

    public interface SaveAction {
        void apply(File imgFile) throws Exception;
    }
}
class ProcessedImageSaver extends SaverBase {
    JFileChooser fileChooser;

    public ProcessedImageSaver(JFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    void showSaveDialog(JFrame frame, String urlOrPath, SaveAction howToSave) throws Exception {
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setSelectedFile(new File(FileLocations.getFileNameNoExt(urlOrPath)+".cc.png"));
        while (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(frame)) {
            File imgFile = fileChooser.getSelectedFile();
            if (!endsWithIgnoreCase(imgFile.getAbsolutePath(),".png")) {
                JOptionPane.showMessageDialog(frame, "File name must end with \"png\"");
            } else if (checkAskOverwrite(frame, imgFile)
            ) {
                System.out.println("Saving to " + imgFile);
                howToSave.apply(imgFile);
                break;
            }
        }
    }
}
class ScreenshotSaver extends SaverBase {
    JFileChooser fileChooser;

    public ScreenshotSaver(JFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    public void takeAndSaveScreenshot(JFrame frame, JComponent leftC, JComponent rightC, RawData rawData, DisplayParameters displayParameters, BehavioralOptions behavioralOptions) {
        try {
            BufferedImage bi = ScreenshotSaver.getScreenshot(frame);
            showSaveDialog(
                    frame,
                    FileLocations.getSol(rawData.left.path).map(x -> String.format("%04d-",x)).orElse(""),
                    "-x" + toSuffixNumber(displayParameters.zoom * displayParameters.zoomL),
                    (imgFile) -> {
                        String fisheyeDescr = "";
                        {
                            if (displayParameters.getFisheyeCorrection(false).algo.notNothing()) {
                                fisheyeDescr += "De-fisheye, left: " + displayParameters.getFisheyeCorrection(false).parametersToString() + "\n";
                            }
                            if (displayParameters.getFisheyeCorrection(true).algo.notNothing()) {
                                fisheyeDescr += "De-fisheye, right: " + displayParameters.getFisheyeCorrection(true).parametersToString() + "\n";
                            }
                        }

                        String description = "Left, Right:\n"
                                             + rawData.left.path + "\n"
                                             + rawData.right.path + "\n"
                                             + fisheyeDescr;
                        ScreenshotSaver.writePng(imgFile, bi,
                            "Software", "Curious: X3D Viewer",
                            "Description", description);
                        ScreenshotSaver.writeText(toSrcFile(imgFile), description);
                        if (behavioralOptions.saveLeftRightImages) {
                            ScreenshotSaver.writePng(toLeftFile(imgFile), ScreenshotSaver.getScreenshot(leftC), "Description", description + "this is left\n");
                            ScreenshotSaver.writePng(toRightFile(imgFile), ScreenshotSaver.getScreenshot(rightC), "Description", description+"this is right\n");
                        }
                        if (behavioralOptions.saveGif) {
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

    static File lr(File file) {
        return mkdirs(toLr(file));
    }
    static File toLr(File file) {
        return new File(new File(file.getParentFile(), "lr"), file.getName());
    }
    static File mkdirs(File imgFile) {
        imgFile.getParentFile().mkdirs();
        return imgFile;
    }
    static File toGifFile(File imgFile) {
        return lr(new File(toGif(imgFile.getAbsolutePath())));
    }
    static File toSrcFile(File imgFile) {
        return new File(imgFile.getAbsolutePath()+".source");
    }
    static File toLeftFile(File imgFile) {
        return lr(new File(toBase(imgFile.getAbsolutePath())+".left.png"));
    }
    static File toRightFile(File imgFile) {
        return lr(new File(toBase(imgFile.getAbsolutePath())+".right.png"));
    }

    public static void writePng(Object output, RenderedImage buffImg, String... keysAndValues) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

        //adding metadata
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

        // Note: tEXt does NOT use Unicode, it uses Latin-1!!!
        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        for (int i=0; i<keysAndValues.length/2; i++) {
            IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
            textEntry.setAttribute("keyword", toAsciiOnly(keysAndValues[i*2]));
            textEntry.setAttribute("value", toAsciiOnly(keysAndValues[i*2+1]));

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
    static String toAsciiOnly(String text) {
        return text
                .replaceAll("γ","gamma")
                .replaceAll("[^\\u0000-\\u007F]","?");
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
        newPath = FileLocations.setUriCuriousM(newPath, false);
        if (!Objects.equals(oldPath, newPath)) {
            left = null;
        }
        return this;
    }
    LRNavigator onSetRight(String oldPath, String newPath) {
        newPath = FileLocations.setUriCuriousM(newPath, false);
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
//        System.out.println("cleanup reading:");
//        for (String url : Arrays.asList("https://mars.nasa.gov/favicon-16x16.png",
//                                        "https://mars.nasa.gov/assets/facebook_icon@2x.png",
//                                        "https://mars.nasa.gov/assets/twitter_icon@2x.png")
//        ) {
//            try {
//                System.out.println("reading " + url);
//                HttpURLConnection uc2 = (HttpURLConnection) new URL(url).openConnection();
//                NasaReader.setHttpHeaders(uc2);
//                uc2.setConnectTimeout(3000); // 3 sec
//                try(
//                    InputStream inputStream = uc2.getInputStream()
//                ) {
//                    ImageIO.read(inputStream);
//                    System.out.println("url " + url + " read successfully");
//                    return;
//                } catch (IOException ee) {
//                    System.out.println("url was not read: "+url);
//                    ee.printStackTrace();
//                }
//            } catch (IOException e) {
//                System.out.println("could not read "+url+" :");
//                e.printStackTrace();
//            }
//        }
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

class FisheyeCorrection {
    final static double DEFAULT_SIZE_CHANGE = 2.;
    final FisheyeCorrectionAlgo algo;
    final HumanVisibleMathFunction func;
    final DistortionCenterLocation distortionCenterLocation;
    final double sizeChange;

    private FisheyeCorrection(
            FisheyeCorrectionAlgo algo,
            HumanVisibleMathFunction func,
            DistortionCenterLocation distortionCenterLocation,
            double sizeChange) {
        this.algo = algo;
        this.func = func;
        this.distortionCenterLocation = distortionCenterLocation;
        this.sizeChange = sizeChange;
    }
    static FisheyeCorrection of(FisheyeCorrectionAlgo algo, HumanVisibleMathFunction func, DistortionCenterLocation distortionCenterLocation, double sizeChange) {
        return new FisheyeCorrection(algo, func, distortionCenterLocation, sizeChange);
    }
    static FisheyeCorrection defaultValue() {
        return of(FisheyeCorrectionAlgo.NONE,
                  HumanVisibleMathFunction.NO_FUNCTION,
                  DistortionCenterLocation.of(
                          DistortionCenterStationing.CENTER,
                          DistortionCenterStationing.CENTER
                  ),
                  DEFAULT_SIZE_CHANGE);
    }
    FisheyeCorrection withAlgo(FisheyeCorrectionAlgo algo) {
        return new FisheyeCorrection(algo, func, distortionCenterLocation, sizeChange);
    }
    FisheyeCorrection withFunc(HumanVisibleMathFunction func) {
        return new FisheyeCorrection(algo, func, distortionCenterLocation, sizeChange);
    }
    FisheyeCorrection withCenterH(DistortionCenterStationing distortionCenterStationingH) {
        return new FisheyeCorrection(
                algo,
                func,
                DistortionCenterLocation.of(
                        distortionCenterStationingH,
                        distortionCenterLocation.getV()),
                sizeChange);
    }
    FisheyeCorrection withCenterV(DistortionCenterStationing distortionCenterStationingV) {
        return new FisheyeCorrection(
                algo,
                func,
                DistortionCenterLocation.of(
                        distortionCenterLocation.getH(),
                        distortionCenterStationingV),
                sizeChange);
    }
    FisheyeCorrection withSizeChange(double sizeChange) {
        return new FisheyeCorrection(algo, func, distortionCenterLocation, sizeChange);
    }
    BufferedImage doFisheyeCorrection(BufferedImage orig) {
        return algo.doFisheyeCorrection(orig, this);
    }
    String parametersToString() {
        return "" + algo
             + " " + distortionCenterLocation.getH()
             + " " + distortionCenterLocation.getV()
             + " " + func.parameterString()
             + " : " + sizeChange + " # "; // + descr
    }
    static FisheyeCorrection fromParameterString(String s) {
        try {
            var p = s.trim().split("\\s+", 4);
            var algo1 = FisheyeCorrectionAlgo.valueOf(p[0]);
            var center1x = DistortionCenterStationing.valueOf(p[1]);
            var center1y = DistortionCenterStationing.valueOf(p[2]);
            var center1 = DistortionCenterLocation.of(center1x, center1y);
            var pp = p[3].split("\\s*:\\s*", 2);
            var f1 = HumanVisibleMathFunction.fromParameterString(pp[0]).get();
            var ppp = pp[1].split("\\s*#\\s*", 2);
            var sizeChange1 = Double.parseDouble(ppp[0]);
            //String descr1 = ppp[1];
            return FisheyeCorrection.of(algo1, f1, center1, sizeChange1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // TODO
    }

    @Override
    public String toString() {
        return "FisheyeCorrection{" +
                "algo=" + algo +
                ", distortionCenterLocation=" + distortionCenterLocation +
                ", func=" + func +
                ", sizeChange=" + sizeChange +
                "}@"+Integer.toHexString(hashCode());
    }
}
enum FisheyeCorrectionAlgo implements ImageEffect {
    NONE {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return orig;
        }
        @Override
        HumanVisibleMathFunction calculateFunction(int width, int height, DistortionCenterLocation dcl, PanelMeasurementStatus pms) {
            return QuadraticPolynomial.of(0, 0, 1);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return QuadraticPolynomial.of(0, 0, 1);
        }
        @Override public String effectShortName() { return ""; }
        @Override public boolean notNothing() { return false; }
    },
    UNFISH1 {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return doFisheyeCorrectionNearestNeighbor(orig, fc);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return QuadraticPolynomial.from3Points(x1, y1, x2, y2, x3, y3);
        }
        @Override public String effectName() { return "Unfish, quadratic"; }
    },
    UNFISH2 {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return doFisheyeCorrectionNearestNeighbor(orig, fc);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return BiquadraticPolynomial.from3Points(x1, y1, x2, y2, x3, y3);
        }
        @Override public String effectName() { return "Unfish, biquadratic"; }
    },
    UNFISH3 {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return doFisheyeCorrectionNearestNeighbor(orig, fc);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return MultiplicativeInversePlusC.from3Points(x1, y1, x2, y2, x3, y3, QuadraticPolynomial::from3Points);
        }
        @Override public String effectName() { return "Unfish, inverse quadratic"; }
    },
    UNFISH4 {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return doFisheyeCorrectionNearestNeighbor(orig, fc);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return MultiplicativeInversePlusC.from3Points(x1, y1, x2, y2, x3, y3, BiquadraticPolynomial::from3Points);
        }
        @Override public String effectName() { return "Unfish, inverse biquadratic"; }
    },
    UNFISH5 {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return doFisheyeCorrectionNearestNeighbor(orig, fc);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return LinearPolynomial.from2Points(x1, y1, x3, y3);
        }
        @Override public String effectName() { return "Unfish, linear"; }
    },
    UNFISH6 {
        @Override
        BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc) {
            return doFisheyeCorrectionNearestNeighbor(orig, fc);
        }
        @Override
        HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
            return MultiplicativeInversePlusC.from3Points(x1, y1, x2, y2, x3, y3, UNFISH5::calculateFunctionFrom3Points);
        }
        @Override public String effectName() { return "Unfish, inverse linear"; }
    },
    ;

    private static BufferedImage doFisheyeCorrectionNearestNeighbor(BufferedImage orig, FisheyeCorrection fc) {
        var k = fc.sizeChange;

        int width = orig.getWidth();
        int height = orig.getHeight();
        int WIDTH = fc.distortionCenterLocation.getWidthAfter(width, height, k);
        int HEIGHT = fc.distortionCenterLocation.getHeightAfter(width, height, k);

        DoubleUnaryOperator xf = fc.func.asFunctionMulX();

        int xc = fc.distortionCenterLocation.getPoleXBefore(width, height);
        int yc = fc.distortionCenterLocation.getPoleYBefore(width, height);
        int XC, YC;
        if (between(xc, 0, width) && between(yc, 0, height)) {
            XC = (int) Math.round(k*xc);
            YC = (int) Math.round(k*yc);
        } else {
            int nx = fc.distortionCenterLocation.getNearestToPoleXBefore(width, height);
            int ny = fc.distortionCenterLocation.getNearestToPoleYBefore(width, height);
            double nr = Math.hypot(nx-xc, ny-yc);
            double rFar = Math.max(Math.max(
                    Math.hypot(width - xc, height - yc),
                    Math.hypot(width - xc, yc)
            ), Math.max(
                    Math.hypot(xc, height - yc),
                    Math.hypot(xc, yc)
            ));
            double NR = HumanVisibleMathFunctionBase.findRootWhenSafe(
                    R -> xf.applyAsDouble(R) - nr,
                    0,
                    k * rFar
            );
            XC = (int) Math.round(k*nx + (xc-nx)*NR/nr);
            YC = (int) Math.round(k*ny + (yc-ny)*NR/nr);
        }

        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < HEIGHT; j++) {
            for (int i = 0; i < WIDTH; i++) {
                double R = Math.hypot(i - XC, j - YC);
                double theta = Math.atan2(j - YC, i - XC);
                double r = xf.applyAsDouble(R);
                double xx = r * Math.cos(theta);
                double yy = r * Math.sin(theta);
                if (Double.isFinite(xx) && Double.isFinite(yy)) { // false if NaN
                    int x = (int) Math.round(xc + xx);
                    int y = (int) Math.round(yc + yy);
                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        res.setRGB(i, j, orig.getRGB(x, y));
                    }
                }
            }
        }
        return res;
    }
    private static boolean between(int value, int lower, int upper) {
        return lower <= value && value < upper;
    }
    abstract BufferedImage doFisheyeCorrection(BufferedImage orig, FisheyeCorrection fc);

    <T>T doWith3Points(int width, int height,
                       DistortionCenterLocation dcl, PanelMeasurementStatus pms,
                       Double6Function<T> todo
    ) {
        double x0 = dcl.getPoleXBefore(width, height);
        double y0 = dcl.getPoleYBefore(width, height);
        double x1 = pms.x3;
        double y1 = pms.y3;
        double x2 = pms.x4;
        double y2 = pms.y4;
        double x3 = pms.x5;
        double y3 = pms.y5;
        double r1 = Math.hypot(x1-x0,y1-y0);
        double r2 = Math.hypot(x2-x0,y2-y0);
        double r3 = Math.hypot(x3-x0,y3-y0);
        double R3 = r3;
        double R2 = r2*(y3-y0)/(y2-y0);
        double R1 = r1*(y3-y0)/(y1-y0);
        return todo.apply(R1, r1, R2, r2, R3, r3);
    }

    HumanVisibleMathFunction calculateFunction(int width, int height, DistortionCenterLocation dcl, PanelMeasurementStatus pms) {
        return doWith3Points(
                width, height, dcl, pms,
                // the function is R->r
                (R1, r1, R2, r2, R3, r3) -> {
                    HumanVisibleMathFunction g = calculateFunctionFrom3Points(R1, r1/R1, R2, r2/R2, R3, r3/R3);

                    System.out.println("R1="+R1+" R2="+R2+" R3="+R3);
                    System.out.println("r1="+r1+" r2="+r2+" r3="+r3);
                    System.out.println("g="+g);

                    return g;
                }
        );
    }

    double[] get3Points(int width, int height, DistortionCenterLocation dcl, PanelMeasurementStatus pms) {
        return doWith3Points(
                width, height, dcl, pms,
                (R1, r1, R2, r2, R3, r3) -> new double[] {R1, R2, R3}
        );
    }
    abstract HumanVisibleMathFunction calculateFunctionFrom3Points(double x1, double y1, double x2, double y2, double x3, double y3);
    @Override public String effectName() { return toString(); }
    @Override
    public String effectShortName() {
        return toString().replaceAll("UNFISH", "uf");
    }
    @Override public boolean notNothing() { return true; }
    @Override public boolean notNothingFor(String path) { return notNothing(); }
}
class FisheyeCorrectionAlgoChooser extends ComboBoxWithTooltips<FisheyeCorrectionAlgo> {
    static FisheyeCorrectionAlgo[] modes = FisheyeCorrectionAlgo.values();
    public FisheyeCorrectionAlgoChooser(Consumer<FisheyeCorrectionAlgo> valueListener) {
        super(modes, ImageEffect::effectName);
        setValue(FisheyeCorrectionAlgo.NONE);
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((FisheyeCorrectionAlgo) itemEvent.getItem());
            }
        });
    }
    public void setValue(FisheyeCorrectionAlgo algo) {
        setSelectedItem(algo);
    }
}
class DistortionCenterStationingChooser extends ComboBoxWithTooltips<DistortionCenterStationing> {
    public DistortionCenterStationingChooser(int modes, Consumer<DistortionCenterStationing> valueListener) {
        super(DistortionCenterStationing.values(modes), DistortionCenterStationing::getDescription);
        setValue(DistortionCenterStationing.CENTER);
        addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                valueListener.accept((DistortionCenterStationing) itemEvent.getItem());
            }
        });
    }
    public void setValue(DistortionCenterStationing location) {
        setSelectedItem(location);
    }
}

interface DistortionCenterLocation {
    /** x of the distortion center, before correction, probably outside of the image */
    int getPoleXBefore(int width, int height);
    /** y of the distortion center, before correction, probably outside of the image */
    int getPoleYBefore(int width, int height);
    /** x of the point in the image nearest to the distortion center, before correction */
    int getNearestToPoleXBefore(int width, int height);
    /** y of the point in the image nearest to the distortion center, before correction */
    int getNearestToPoleYBefore(int width, int height);
    int getWidthAfter(int width, int height, double k);
    int getHeightAfter(int width, int height, double k);
    double getMinRBefore(int width, int height);
    double getMaxRBefore(int width, int height);
    double getMinRAfter(int width, int height, double k);
    double getMaxRAfter(int width, int height, double k);
//    double getMaxRThetaBefore(int width, int height);
    DistortionCenterStationing getH();
    DistortionCenterStationing getV();
    static DistortionCenterLocation of(DistortionCenterStationing cx, DistortionCenterStationing cy) {
        return new DistortionCenterLocationImpl(cx, cy);
    }

}
interface DistortionCenterStationingAux {
    // These could be defined in DistortionCenterStationing but this would require testing on different VMs,
    // since forward-referencing a constant inside an enum definitely is a corner case,
    // and such testing is expensive.
    public static final int HORIZ = 1;
    public static final int VERT = 2;
}
enum DistortionCenterStationing implements DistortionCenterStationingAux {
    CENTER(0, 0, 0.5, HORIZ+VERT, "Distortion center in the image center"),
    INSIDE_1to2(0, 0, 1./3, VERT, "Distortion center inside image, 1/3 above, 2/3 below"),
    INSIDE_2to1(0, 0, 1./3, VERT, "Distortion center inside image, 2/3 above, 1/3 below"),
    LEFT_EDGE(8, 8, 0.0, HORIZ, "Distortion center on the left edge of the image"),
    RIGHT_EDGE(8, 8, 1.0, HORIZ, "Distortion center on the right edge of the image"),
    LEFT_EDGE_OVER(8, 0, -1.0, HORIZ, "Distortion center on the left edge of the next image"),
    RIGHT_EDGE_OVER(0, 8, 2.0, HORIZ, "Distortion center on the right edge of the next image"),
    TOP_EDGE(8, 8, 0.0, VERT, "Distortion center at the top edge of the image"),
    BOTTOM_EDGE(8, 8, 1.0, VERT, "Distortion center at the bottom edge of the image"),
    TOP_EDGE_OVER(8, 0, -1.0, VERT, "Distortion center at the top edge of the image above this one"),
    BOTTOM_EDGE_OVER(0, 8, 2.0, VERT, "Distortion center at the bottom edge of the image below this one"),
    ;

    final int margin1, margin2;
    final double m;
    final int flags;
    final String description;

    public static DistortionCenterStationing[] values(int flags) {
        if (flags==0) {
            return values(); // 0 should never
        }
        return Arrays.stream(values())
               .filter(x -> (x.flags & flags) != 0)
               .toArray(DistortionCenterStationing[]::new);
    }
    DistortionCenterStationing(int margin1, int margin2, double m, int flags, String descr) {
        this.margin1 = margin1;
        this.margin2 = margin2;
        this.m = m;
        this.flags = flags;
        this.description = descr;
    }
//    private DistortionCenterStationing(int m)
    /** coordinate (x or y) of the distortion center, before correction, probably outside of the image */
    int getPoleCoordBefore(int widthOrHeight) {
        return margin1 + (int)((widthOrHeight - margin1 - margin2)*m);
    }
    /** coordinate (x or y) of the point in the image nearest to the distortion center, before correction */
    int getNearestToPoleCoordBefore(int widthOrHeight) {
        return Math.max(0, Math.min(widthOrHeight, getPoleCoordBefore(widthOrHeight)));
    }
    int getSizeAfter(int widthOrHeight, double k) {
        return (int) Math.round(widthOrHeight*k);
    }
    int getMinRBefore(int widthOrHeight) {
        int c = getPoleCoordBefore(widthOrHeight);
        return 0 <= c && c <= widthOrHeight
             ? 0
             : Math.min(Math.abs(c), Math.abs(c-widthOrHeight));
    }
    int getMaxRBefore(int widthOrHeight) {
        int c = getPoleCoordBefore(widthOrHeight);
        return Math.max(Math.abs(c), Math.abs(c-widthOrHeight));
    }
    public String getDescription() { return description; }
}
class DistortionCenterLocationImpl implements DistortionCenterLocation {
    DistortionCenterStationing cx, cy;

    public DistortionCenterLocationImpl(DistortionCenterStationing cx, DistortionCenterStationing cy) {
        this.cx = cx;
        this.cy = cy;
    }

    @Override
    public int getPoleXBefore(int width, int height) {
        return cx.getPoleCoordBefore(width);
    }
    @Override
    public int getPoleYBefore(int width, int height) {
        return cy.getPoleCoordBefore(height);
    }
    @Override
    public int getNearestToPoleXBefore(int width, int height) {
        return cx.getNearestToPoleCoordBefore(width);
    }
    @Override
    public int getNearestToPoleYBefore(int width, int height) {
        return cy.getNearestToPoleCoordBefore(height);
    }
    @Override
    public int getWidthAfter(int width, int height, double k) {
        return cx.getSizeAfter(width, k);
    }
    @Override
    public int getHeightAfter(int width, int height, double k) {
        return cy.getSizeAfter(height, k);
    }
    @Override
    public double getMinRBefore(int width, int height) {
        return Math.hypot(cx.getMinRBefore(width), cy.getMinRBefore(height));
    }
    @Override
    public double getMaxRBefore(int width, int height) {
        return Math.hypot(cx.getMaxRBefore(width), cy.getMaxRBefore(height));
    }
    @Override
    public double getMinRAfter(int width, int height, double k) {
        return k * getMinRBefore(width, height);
    }
    @Override
    public double getMaxRAfter(int width, int height, double k) {
        return k * getMaxRBefore(width, height);
    }

    @Override public DistortionCenterStationing getH() { return cx; }
    @Override public DistortionCenterStationing getV() { return cy; }

//    @Override
//    public double getMaxRThetaBefore(int width, int height) {
//        return 0;
//    }

    @Override
    public String toString() {
        return "(" + cx + ", " + cy + ')';
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
    // TODO find a better class
    public String getShortDescription(String path, ImageEffect... prefixes) {
        String res =
                Stream.concat(
                        Stream.of(prefixes),
                        algos.stream()
                )
                .filter(x -> x.notNothingFor(path))
                .map(ImageEffect::effectShortName)
                .collect(Collectors.joining(","));
        return res;
    }
    public String getFullDescription(String path, ImageEffect... prefixes) {
        String res =
                Stream.concat(
                        Stream.of(prefixes),
                        algos.stream()
                )
                        .filter(x -> x.notNothingFor(path))
                        .map(ImageEffect::effectName)
                        .collect(Collectors.joining("; "));
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
                    res = ColorBalancer.stretchColorsRgb(res, true, false);
                    break;
//                case STRETCH_CONTRAST_RGB_RGB_I:
//                    res = ColorBalancer.stretchColorsRgb(res, true, true);
//                    break;
                case STRETCH_CONTRAST_RGB_V:
                    res = ColorBalancer.stretchColorsRgb(res, false, false);
                    break;
//                case STRETCH_CONTRAST_RGB_V_I:
//                    res = ColorBalancer.stretchColorsRgb(res, false, true);
//                    break;
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
                case INTERPOLATE_BROKEN_PIXELS:
                    res = ColorBalancer.interpolateBrokenPixels(res);
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

interface ImageEffect {
    String effectName();
    String effectShortName();
    default boolean notNothing() { return true; }
    default boolean notNothingFor(String path) { return notNothing(); }
}
enum ColorCorrectionAlgo implements ImageEffect {
    DO_NOTHING("as is", "") { @Override public boolean notNothing() { return false; } },
    STRETCH_CONTRAST_RGB_RGB("stretch R,G,B separately in RGB space", "sRGB"),
//    STRETCH_CONTRAST_RGB_RGB_I("stretch R,G,B separately in RGB space, ignore broken pixels", "sRGBi"),
    STRETCH_CONTRAST_RGB_V("stretch R,G,B together in RGB space", "sRGB2"),
//    STRETCH_CONTRAST_RGB_V_I("stretch R,G,B together in RGB space, ignore broken pixels", "sRGB2i"),
    STRETCH_CONTRAST_RGB_RGB3("stretch R,G,B in RGB space, custom parameters", "sRGB3"),
    STRETCH_CONTRAST_HSV_V("stretch V in HSV space", "sHSVv"),
    STRETCH_CONTRAST_HSV_S("stretch S in HSV space", "sHSVs"),
    STRETCH_CONTRAST_HSV_SV("stretch S & V in HSV space", "sHSV"),
    INTERPOLATE_BROKEN_PIXELS("interpolate broken pixels", "ib"),
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
    @Override
    public String effectName() { return name; }
    @Override
    public String effectShortName() { return shortName; }
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
    final JCheckBox cbSaturateToBlack;
    final JButton buttonSet;

    final Color normalButtonColor;
    final Color highlightedButtonColor = new Color(250,127,127);
    private final String pleaseSelectSrgb3Norm;
    private final String pleaseSelectSrgb3Hili;

    private static final String ALL3 = "RGB";
    private static final String ALL3_TOOLTOP = "Set all 3 channels to this value";
    private JButton makeAll3Button(Consumer<ColorRange> updater) {
        JButton all3 = new JButton();
        DigitalZoomControl.loadIcon(all3,"icons/rgb24.png",ALL3);
        all3.setToolTipText(ALL3_TOOLTOP);
        all3.addActionListener(e -> {
            updater.accept(proposed.colorRange);
            customStretchRgbParametersToControls(proposed);
            whenUpdated();
        });
        return all3;
    }

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
        dcR0.add(makeAll3Button(cr -> cr.setMinAll(cr.minR)));
        this.add(dcG0 = new DigitalZoomControl<Integer, OffsetWrapper>().init("G:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.minG=i; whenUpdated();}));
        dcG0.add(makeAll3Button(cr -> cr.setMinAll(cr.minG)));
        this.add(dcB0 = new DigitalZoomControl<Integer, OffsetWrapper>().init("B:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.minB=i; whenUpdated();}));
        dcB0.add(makeAll3Button(cr -> cr.setMinAll(cr.minB)));
        this.add(dcR1 = new DigitalZoomControl<Integer, OffsetWrapper>().init("R:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.maxR=i; whenUpdated();}));
        dcR1.add(makeAll3Button(cr -> cr.setMaxAll(cr.maxR)));
        this.add(dcG1 = new DigitalZoomControl<Integer, OffsetWrapper>().init("G:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.maxG=i; whenUpdated();}));
        dcG1.add(makeAll3Button(cr -> cr.setMaxAll(cr.maxG)));
        this.add(dcB1 = new DigitalZoomControl<Integer, OffsetWrapper>().init("B:", 4, new OffsetWrapper(), i -> {
            proposed.colorRange.maxB=i; whenUpdated();}));
        dcB1.add(makeAll3Button(cr -> cr.setMaxAll(cr.maxB)));
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
            cbSaturateToBlack = new JCheckBox("Saturate to Black");
            this.add(cbSaturateToBlack);
            cbSaturateToBlack.addActionListener(
                    e -> { proposed.isBlackSaturated = cbSaturateToBlack.isSelected(); whenUpdated(); }
            );
            cbSaturateToBlack.setToolTipText("Overexposed (too bright) pixels must become black");
        }
        {
            JButton button = new JButton("Get from Viewport");
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> actionCalculateViewportColorRange(false));
            button.setToolTipText("First zoom/resize/scroll the window to exclude pixels that are too dark/too bright");
            this.add(button);
        }
        {
            JButton button = new JButton("Get from Viewport, Ignore Broken Pixels");
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> actionCalculateViewportColorRange(true));
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
        copyAndUseProposedFrom(old);
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
    private void actionCalculateViewportColorRange(boolean ignoreBroken) {
        ColorRange cr = uiEventListener.getViewportColorRange(isRight, ignoreBroken);
        colorRangeToControls(cr);
        proposed.colorRange = cr.copy();
        whenUpdated();
    }
    private void customStretchRgbParametersToControls(CustomStretchRgbParameters param) {
        ColorRange cr = param.colorRange;
        colorRangeToControls(cr);
        cbPerChannel.setSelected(param.isPerChannel);
        cbSaturation.setSelected(param.isSaturated);
        cbSaturateToBlack.setSelected(param.isBlackSaturated);
    }
    private void colorRangeToControls(ColorRange cr) {
        dcR0.setValueAndText(cr.minR);
        dcG0.setValueAndText(cr.minG);
        dcB0.setValueAndText(cr.minB);
        dcR1.setValueAndText(cr.maxR);
        dcG1.setValueAndText(cr.maxG);
        dcB1.setValueAndText(cr.maxB);
    }
    void copyAndUseProposedFrom(CustomStretchRgbParameters toCopyAndUse) {
        proposed = toCopyAndUse.copy();
        customStretchRgbParametersToControls(proposed);
        whenUpdated();
    }
    private void actionResetToOriginal() {
        copyAndUseProposedFrom(old);
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
    public static final int N_EFFECTS = 5;

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
        {
            JButton button = new JButton("Copy ->");
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setToolTipText("Copy all parameters to the right");
            button.addActionListener(e ->
                rColorRangeChooser.copyAndUseProposedFrom(lColorRangeChooser.proposed)
            );
            lColorRangeChooser.add(button);
        }
        {
            JButton button = new JButton("<- Copy");
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setToolTipText("Copy all parameters to the left");
            button.addActionListener(e ->
                lColorRangeChooser.copyAndUseProposedFrom(rColorRangeChooser.proposed)
            );
            rColorRangeChooser.add(button);
        }

        GridBagLayout gbl = new GridBagLayout();

        int rowNumber = 0;
        {
            var text = new JLabel("Apply effects to each image, in this sequence:");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(text, gbc);
            this.add(text);
        }
        for (int col = 0; col<2; col++) {
            final boolean isLeft = (col & 1) == 0;
            for (int row = 0; row < N_EFFECTS; row++) {
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
                gbc.gridy = rowNumber+row;
                gbl.setConstraints(chooser, gbc);
                this.add(chooser);
            }
        }
        rowNumber += N_EFFECTS;
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
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
            gbc.gridy = rowNumber;
            gbl.setConstraints(lColorRangeChooser, gbc);
            this.add(lColorRangeChooser);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 3;
            gbc.gridy = rowNumber;
            gbl.setConstraints(rColorRangeChooser, gbc);
            this.add(rColorRangeChooser);
        }
        rowNumber++;
        this.setLayout(gbl);
    }

    void showDialogIn(JFrame mainFrame) {
        JustDialog.showMessageDialog(mainFrame, this,"Color Correction", JOptionPane.PLAIN_MESSAGE);
//        JOptionPane.showMessageDialog(mainFrame, this,"Color Correction", JOptionPane.PLAIN_MESSAGE);
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
    static ColorRange getColorRangeFromImage(Rectangle rectangle, BufferedImage src, boolean ignoreBroken, int d) {
        int iStart = (int) rectangle.getX();
        int iFinal = (int) rectangle.getMaxX();
        int jStart = (int) rectangle.getY();
        int jFinal = (int) rectangle.getMaxY();
        return getColorRangeFromImage(iStart, iFinal, jStart, jFinal, src, ignoreBroken, d);
    }
    static ColorRange getColorRangeFromImage(int iStart, int iFinal, int jStart, int jFinal, BufferedImage src, boolean ignoreBroken) {
        return getColorRangeFromImage(iStart, iFinal, jStart, jFinal, src, ignoreBroken, 1);
    }
    static ColorRange getColorRangeFromImage(int iStart, int iFinal, int jStart, int jFinal, BufferedImage src, boolean ignoreBroken, int d) {
        // if the viewport rectangle is wider/higher than the image,
        // we take the whole width/height of the image.
        // We assume that either the viewport is somewhere within the image,
        // or the image is somewhere within the viewport.
        int M = 16*d;
        iStart = Math.max(iStart, M);
        jStart = Math.max(jStart, M);
        iFinal = Math.min(iFinal, src.getWidth()-M);
        jFinal = Math.min(jFinal, src.getHeight()-M);
        if (ignoreBroken) { // TODO ??
            iStart+=d;
            iFinal-=d;
            jStart+=d;
            jFinal-=d;
        }
        ColorRange cr = ColorRange.newEmptyRange();
        ColorRange around = ColorRange.newEmptyRange();
        int dd = d + d/2;
        for (int j = jStart; j < jFinal; j++) {
            for (int i = iStart; i < iFinal; i++) {
                int color = src.getRGB(i, j);
                if (ignoreBroken) {
                    if (pixelLooksNotBroken(color, setToMinMaxRgbDiag(around, src, i, j, dd))
                     && pixelLooksNotBroken(color, setToMinMaxRgbHorVer(around, src, i, j, dd))
                    ) {
                        if ((color&0xff_ff_ff)==0xff_ff_ff) {
                            System.out.println("not broken 255: ("+i+","+j+")");
                        }
                        cr.update(color);
                    }
                } else {
                    cr.update(color);
                }
            }
        }
        return cr;
    }
    static boolean pixelLooksNotBroken(int rgb, ColorRange rgbs) {
        return rgbs.almostContains(rgb, 10); // 20?
    }
    static ColorRange setToMinMaxRgbDiag(ColorRange rgbs, BufferedImage src, int i, int j, int d) {
        rgbs.setEmpty();
        rgbs.update(src.getRGB(i-d, j-d));
        rgbs.update(src.getRGB(i+d, j-d));
        rgbs.update(src.getRGB(i-d, j+d));
        rgbs.update(src.getRGB(i+d, j+d));
        return rgbs;
    }
    static ColorRange setToMinMaxRgbHorVer(ColorRange rgbs, BufferedImage src, int i, int j, int d) {
        rgbs.setEmpty();
        rgbs.update(src.getRGB(i-d, j));
        rgbs.update(src.getRGB(i+d, j));
        rgbs.update(src.getRGB(i, j-d));
        rgbs.update(src.getRGB(i, j+d));
        return rgbs;
    }
//    static void setToMinMaxRgbAround(ColorRange rgbs, BufferedImage src, int i, int j, int d) {
//        rgbs.setEmpty();
//        rgbs.update(src.getRGB(i-d, j-d));
//        rgbs.update(src.getRGB(i, j-d));
//        rgbs.update(src.getRGB(i+d, j-d));
//        rgbs.update(src.getRGB(i-d, j));
//        rgbs.update(src.getRGB(i+d, j));
//        rgbs.update(src.getRGB(i-d, j+d));
//        rgbs.update(src.getRGB(i, j+d));
//        rgbs.update(src.getRGB(i+d, j+d));
//    }
    public static BufferedImage interpolateBrokenPixels(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        var res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ColorRange around = ColorRange.newEmptyRange();
        for (int j = 0; j < height; j += height-1) {
            for (int i = 0; i < width; i += width-1) {
                res.setRGB(i, j, src.getRGB(i, j));
            }
        }

        for (int j = 1; j < height-1; j++) {
            for (int i = 1; i < width-1; i++) {
                int color = src.getRGB(i, j);
                if (!pixelLooksNotBroken(color, setToMinMaxRgbDiag(around, src, i, j, 1))) {
                    res.setRGB(i, j, mendRgbDiag(src, j, i));
                } else if(!pixelLooksNotBroken(color, setToMinMaxRgbHorVer(around, src, i, j, 1))) {
                    res.setRGB(i, j, mendRgbHorVer(src, j, i));
                }else {
                    res.setRGB(i, j, color);
                }
            }
        }
        return res;
    }
    private static int mendRgbDiag(BufferedImage src, int j, int i) {
        int c1 = src.getRGB(i-1, j-1);
        int c2 = src.getRGB(i+1, j-1);
        int c3 = src.getRGB(i-1, j+1);
        int c4 = src.getRGB(i+1, j+1);
        int r = (((c1&0xff0000) + (c2&0xff0000) + (c3&0xff0000) + (c4&0xff0000)) >> 2) & 0xff0000;
        int g = (((c1&0xff00) + (c2&0xff00) + (c3&0xff00) + (c4&0xff00)) >> 2) & 0xff00;
        int b = (((c1&0xff) + (c2&0xff) + (c3&0xff) + (c4&0xff)) >> 2) & 0xff;
        return r|g|b;
    }
    private static int mendRgbHorVer(BufferedImage src, int j, int i) {
        int c1 = src.getRGB(i-1, j);
        int c2 = src.getRGB(i+1, j);
        int c3 = src.getRGB(i, j-1);
        int c4 = src.getRGB(i, j+1);
        int r = (((c1&0xff0000) + (c2&0xff0000) + (c3&0xff0000) + (c4&0xff0000)) >> 2) & 0xff0000;
        int g = (((c1&0xff00) + (c2&0xff00) + (c3&0xff00) + (c4&0xff00)) >> 2) & 0xff00;
        int b = (((c1&0xff) + (c2&0xff) + (c3&0xff) + (c4&0xff)) >> 2) & 0xff;
        return r|g|b;
    }

    public static BufferedImage stretchColorsRgb(BufferedImage src, boolean perChannel, boolean ignoreBroken) {
        if (ImageAndPath.isDummyImage(src)) {
            return src;
        }
        try {
            final int M = 16;
            int width = src.getWidth();
            int height = src.getHeight();
            ColorRange cr = getColorRangeFromImage(M, width - M, M, height - M, src, ignoreBroken);
            BufferedImage res = stretchColorsUsingRgbRange(cr, src, perChannel, false, false);
            return res;
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }
    public static BufferedImage stretchColorsRgb(BufferedImage src, CustomStretchRgbParameters customStretchRgbParameters) {
        if (ImageAndPath.isDummyImage(src)) {
            return src;
        }
        if (customStretchRgbParameters.colorRange.isEmpty()
         || customStretchRgbParameters.colorRange.isFullRange()
        ) {
            System.out.println("stretchColorsRgb: full or empty range");
            return src;
        }
        boolean perChannel = customStretchRgbParameters.isPerChannel;
        ColorRange cr = customStretchRgbParameters.colorRange;
        boolean saturate = customStretchRgbParameters.isSaturated;
        boolean saturateToBlack = customStretchRgbParameters.isBlackSaturated;
        try {
            return stretchColorsUsingRgbRange(cr, src, perChannel, saturate, saturateToBlack);
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return src;
        }
    }

    static BufferedImage stretchColorsUsingRgbRange(ColorRange cr, BufferedImage src,
                                                    boolean perChannel, boolean saturate, boolean saturateToBlack) {
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
        if (saturate && !saturateToBlack) {
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
        } else if (saturateToBlack) {
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int color = src.getRGB(i, j);
                    int r = 0xff & (color >> 16);
                    int g = 0xff & (color >> 8);
                    int b = 0xff & (color);
                    int r1 = 0xff & Math.max(0, Math.min(256, (r - minR) * 255 / dr));
                    int g1 = 0xff & Math.max(0, Math.min(256, (g - minG) * 255 / dg));
                    int b1 = 0xff & Math.max(0, Math.min(256, (b - minB) * 255 / db));
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
        if (ImageAndPath.isDummyImage(src)) {
            return src;
        }
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
        if (ImageAndPath.isDummyImage(src)) {
            return src;
        }
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
    public static String getBuildVersion() {
        String ver = BuildVersion.class.getPackage().getImplementationVersion();
        return orNA(ver);
    }
    public static String getVmVersion() {
        var vmname = System.getProperty("java.vm.name");
//        var vmvendor = System.getProperty("java.vm.vendor");
//        var vmver = System.getProperty("java.vm.version");
        return orNA(vmname);
    }
    public static String getJavaVersion() {
        var javaver = System.getProperty("java.version");
        return orNA(javaver);
    }
//    public static String getOsVersion() {
//        var os = System.getProperty("os.name");
//        return orNa(os);
//    }
    static String orNA(String value) {
        return value == null ? "n/a" : value;
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
        JustDialog.showMessageDialog(mainFrame, this,"Measurement", JOptionPane.PLAIN_MESSAGE);
//        JOptionPane.showMessageDialog(mainFrame, this,"Measurement", JOptionPane.PLAIN_MESSAGE);
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
} // MeasurementPanel
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

class FisheyeCorrectionPane extends JPanel {
    static final Color HILI_BGCOLOR = new Color(0xffff80);
    static final int GRAPH_WIDTH = 500;
    static final int GRAPH_HEIGHT = 150+1;
    static final String IMAGE_INFO_FORMAT = "<html>in: %dx%d r=%d<br>out: %dx%d R=%d</html>";
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
    final JCheckBox cbMendPixelsPrefilterL;
    final JCheckBox cbMendPixelsPrefilterR;
    final FisheyeCorrectionAlgoChooser chooserAlgoL;
    final FisheyeCorrectionAlgoChooser chooserAlgoR;
    final DistortionCenterStationingChooser chooserCenterHL;
    final DistortionCenterStationingChooser chooserCenterVL;
    final DistortionCenterStationingChooser chooserCenterHR;
    final DistortionCenterStationingChooser chooserCenterVR;
    final JLabel lblGraphL;
    final JLabel lblGraphR;
    final JLabel lblFunctionInfoL;
    final JLabel lblFunctionInfoR;
    final JLabel lblImageInfoL;
    final JLabel lblImageInfoR;
    // for highlighting {
    final JLabel lblCorrectionMethodL;
    final JLabel lblCorrectionMethodR;
    final JLabel lblFunctionL;
    final JLabel lblFunctionR;
    final JButton btnCalculateL;
    final JButton btnCalculateR;
    final JButton btnApplyL;
    final JButton btnApplyR;
    // } for highlighting
    final JTextField etxFunctionL;
    final JTextField etxFunctionR;
    final HalfPane leftHalf;
    final HalfPane rightHalf;
    FisheyeCorrection fisheyeCorrectionL;
    FisheyeCorrection fisheyeCorrectionR;
    static class HalfPane {
        final FisheyeCorrectionPane parentPane;
        final boolean isRight;
        final JCheckBox cbMendPixelsPrefilter;
        final FisheyeCorrectionAlgoChooser chooserAlgo;
        final DistortionCenterStationingChooser chooserCenterH;
        final DistortionCenterStationingChooser chooserCenterV;
        final JLabel lblGraph;
        final JLabel lblFunctionInfo;
        final JLabel lblImageInfo;
        final JTextField etxFunction;
        // for highlighting {
        final JLabel lblCorrectionMethod;
        final JLabel lblFunction;
        final JButton btnCalculate;
        final JButton btnApply;
        final List<DigitalZoomControl<Double, OffsetWrapper2>> digitalZoomControls;
        // } for highlighting
//        final JLabel lblWidthXHeightR; // TODO
        public HalfPane(FisheyeCorrectionPane parentPane,
                        boolean isRight,
                        JCheckBox cbMendPixelsPrefilter,
                        FisheyeCorrectionAlgoChooser chooserAlgo,
                        DistortionCenterStationingChooser chooserCenterH,
                        DistortionCenterStationingChooser chooserCenterV,
                        JLabel lblGraph,
                        JLabel lblFunctionInfo,
                        JLabel lblImageInfo,
                        JTextField etxFunction,
                        JLabel lblCorrectionMethod,
                        JLabel lblFunction,
                        JButton btnCalculate,
                        JButton btnApply,
                        List<DigitalZoomControl<Double, OffsetWrapper2>> digitalZoomControls) {
            this.parentPane = parentPane;
            this.isRight = isRight;
            this.cbMendPixelsPrefilter = cbMendPixelsPrefilter;
            this.chooserAlgo = chooserAlgo;
            this.chooserCenterH = chooserCenterH;
            this.chooserCenterV = chooserCenterV;
            this.lblGraph = lblGraph;
            this.lblFunctionInfo = lblFunctionInfo;
            this.lblImageInfo = lblImageInfo;
            this.etxFunction = etxFunction;
            this.lblCorrectionMethod = lblCorrectionMethod;
            this.lblFunction = lblFunction;
            this.btnCalculate = btnCalculate;
            this.btnApply = btnApply;
            this.digitalZoomControls = digitalZoomControls;
        }
        void setFromData() {
            FisheyeCorrection fisheyeCorrection = getFC();
            chooserAlgo.setValue(fisheyeCorrection.algo);
            chooserCenterH.setValue(fisheyeCorrection.distortionCenterLocation.getH());
            chooserCenterV.setValue(fisheyeCorrection.distortionCenterLocation.getV());
            etxFunction.setText(fisheyeCorrection.parametersToString());
            Dimension dim = parentPane.uiEventListener.getRawImageDimensions(isRight);
            var threePoints = fisheyeCorrection.algo.get3Points(dim.width, dim.height, fisheyeCorrection.distortionCenterLocation, getPMS());
            var threeColors = new int[] {0x0000FF, 0x00FFFF, 0xFF00FF};
            updateDefisheyeFunctionUi(dim.width, dim.height, fisheyeCorrection, this, threePoints, threeColors);
            cbMendPixelsPrefilter.setSelected(parentPane.uiEventListener.getPreFilter(isRight).notNothing());
            updateHilighting();
        }
        static void updateDefisheyeFunctionUi(int width,
                                              int height,
                                              FisheyeCorrection fc,
                                              HalfPane halfPane,
                                              double[] threePoints,
                                              int[] threeColors) {
            var k = fc.sizeChange;
            var dcl = fc.distortionCenterLocation;
            var g = fc.func;
            var maxPoint = DoubleStream.of(threePoints).max().orElse(0);
            var rMin = 0.;
            var rMax = Math.max(maxPoint, dcl.getMaxRAfter(width, height, k));
            halfPane.lblGraph.setIcon(new ImageIcon(
                    GraphPlotter.plotGraph(
                            GRAPH_WIDTH, GRAPH_HEIGHT,
                            rMin, rMax,
                            g.maxInRange(rMin, rMax),
                            Color.RED,
                            g.asFunction(),
                            threePoints,
                            threeColors
                    )
            ));
            String info =
                    "<html>"
                    + "g(r) = " + g.asString("r") + "<br>"
                    + "r ∈ [" + (int) Math.round(rMin) + ", " + (int) Math.round(rMax) + "]<br>"
                    + "g(r) ∈ [" + fmtD(g.minInRange(rMin, rMax)) + ", " + fmtD(g.maxInRange(rMin, rMax)) + "]<br>"
                    + "&nbsp "
                    + "</html>";
            halfPane.lblFunctionInfo.setText(info);
            halfPane.lblImageInfo.setText(
                String.format(IMAGE_INFO_FORMAT,
                    width,
                        height,
                        (int)Math.round(dcl.getMaxRBefore(width, height)),
                    dcl.getWidthAfter(width, height, k),
                        dcl.getHeightAfter(width, height, k),
                        (int)Math.round(dcl.getMaxRAfter(width, height, k))
                )
            );
        }
        static String fmtD(double x) {
            return String.format("%.4g",x);
        }

        /** The purpose of this highlighting is to inform the user what to do in this dialog */
        void updateHilighting() {
            int phase1; // 0: nothing 1:got correction method 2:got marks 3:calculated 4:after apply
            int phase2; // 0: nothing 3: after setFromText 4:after apply
            var fc = getFC();

            if (fc.algo == FisheyeCorrectionAlgo.NONE) {
                phase1 = 0;
            } else if (digitalZoomControls.stream().anyMatch(c -> c.getSafeValue()<0)) {
                phase1 = 1;
            } else if (!Double.isFinite(fc.func.apply(1))) {
                phase1 = 2;
            } else if (!fc.parametersToString().equals(
                       parentPane.uiEventListener.getFisheyeCorrection(isRight).parametersToString()
            )) {
                phase1 = 3;
            } else {
                phase1 = 4;
            }
            if (!Double.isFinite(fc.func.apply(1))) {
                phase2 = 2;
            } else if (!fc.parametersToString().equals(
                       parentPane.uiEventListener.getFisheyeCorrection(isRight).parametersToString()
            )) {
                phase2 = 3;
            } else {
                phase2 = 4;
            }
//            System.out.println("updateHilighting phase1="+phase1+" phase2="+phase2);
//            System.out.println("local  fc: "+fc.parametersToString());
//            System.out.println("global fc: "+parentPane.uiEventListener.getFisheyeCorrection(isRight).parametersToString());
            highlightLabel(lblCorrectionMethod, phase1 == 0);
            for (var dc: digitalZoomControls) {
                highlightLabel(dc.label, phase1 == 1 && phase2 <= 2 && dc.getSafeValue() < 0);
            }
            highlightButton(btnCalculate, phase1 == 2 && phase2 <= 2);
            highlightLabel(lblFunction, phase2 <= 2);
            highlightButton(btnApply, phase2 == 3);
        }
        void highlightLabel(JLabel label, boolean highlight) {
            System.out.println(" "+label.getText()+" -- "+highlight);
            if (highlight) {
                label.setBackground(HILI_BGCOLOR);
                label.setOpaque(true);
            } else {
                label.setOpaque(false);
            }
            label.repaint();
        }
        void highlightButton(JButton button, boolean highlight) {
            System.out.println(" "+button.getText()+" -- "+highlight);
            if (highlight) {
                button.setBackground(HILI_BGCOLOR);
            } else {
                button.setBackground(null);
            }
            button.repaint();
        }

        private FisheyeCorrection getFC() {
            return parentPane.getFisheyeCorrection(isRight);
        }

        private PanelMeasurementStatus getPMS() {
            return parentPane.getPanelMeasurementStatus(isRight);
        }
    }

    public FisheyeCorrectionPane(UiEventListener uiEventListener) {
        this.uiEventListener = uiEventListener;

        GridBagLayout gbl = new GridBagLayout();

        dcLX1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">X1L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(0, i);
            doCalculate(false);
        });
        dcLY1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">Y1L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(1, i);
            doCalculate(false);
        });
        dcLX2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">X2L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(2, i);
            doCalculate(false);
        });
        dcLY2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">Y2L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(3, i);
            doCalculate(false);
        });
        dcLX3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">X3L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(4, i);
            doCalculate(false);
        });
        dcLY3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">Y3L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(5, i);
            doCalculate(false);
        });
        dcLX4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">X4L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(6, i);
            doCalculate(false);
        });
        dcLY4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">Y4L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(7, i);
            doCalculate(false);
        });
        dcLX5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">X5L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(8, i);
            doCalculate(false);
        });
        dcLY5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">Y5L:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(9, i);
            doCalculate(false);
        });
        dcRX1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">X1R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(10, i);
            doCalculate(true);
        });
        dcRY1 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"red\">Y1R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(11, i);
            doCalculate(true);
        });
        dcRX2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">X2R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(12, i);
            doCalculate(true);
        });
        dcRY2 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"green\">Y2R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(13, i);
            doCalculate(true);
        });
        dcRX3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">X3R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(14, i);
            doCalculate(true);
        });
        dcRY3 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"blue\">Y3R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(15, i);
            doCalculate(true);
        });
        dcRX4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">X4R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(16, i);
            doCalculate(true);
        });
        dcRY4 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#00ffff\">Y4R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(17, i);
            doCalculate(true);
        });
        dcRX5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">X5R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(18, i);
            doCalculate(true);
        });
        dcRY5 = new DigitalZoomControl<Double, OffsetWrapper2>().init("<html><font color=\"#ff00ff\">Y5R:</font></html>", 9, new OffsetWrapper2(), i -> {
            uiEventListener.markedPointChanged(19, i);
            doCalculate(true);
        });
        var dcs = Arrays.asList(
                dcLX1, dcLY1, dcLX2, dcLY2, dcLX3, dcLY3, dcLX4, dcLY4, dcLX5, dcLY5,
                dcRX1, dcRY1, dcRX2, dcRY2, dcRX3, dcRY3, dcRX4, dcRY4, dcRX5, dcRY5
        );
        var dcRows = dcs.size()/4;

        var rowNumber = 0;
        {
            var row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
//            var text1 = new JLabel("Please focus your eyes on the object of interest and make both marks of the same color");
//            var text2 = new JLabel("EXACTLY match ON THAT OBJECT in x3d, otherwise you will get a measurement error!");
            // TODO: refactor, use an array + a loop
            var text1 = new JLabel("<html>" +
                    "The <i>\"fisheye effect\"</i> is officially called <i>barrel distortion.</i> We can correct it " +
                    "knowing the distortion center and any 3 points that" +
                    "<br>would be on one horizontal line if there were no distortion." +
                    "<br>" +
                    "<br>Find a line that should be horizontal in the image, and place marks" +
                    " <font color='blue'>3</font>, <font color='#00ffff'>4</font>, <font color='#ff00ff'>5</font>" +
                    " on this line, preferably one mark right above or below the distortion center," +
                    "<br>and two others to the right or left of it, on the same side. " +
                    "To set the marks, use key combinations Alt 1 through Alt 5, but not in this dialog." +
                    "<br>For example, to set the 3rd (<font color='#0000ff'>blue</font>) mark, exit this dialog," +
                    "press Alt 3 (the mouse cursor will become a crosshair), and click the point that you want to mark." +
                    "</html>");
            var text3 = new JLabel("Note that moving a mark by 1 pixel MAY significantly change the result.");
//            var text5 = new JLabel(" ");
            row.add(text1);
            row.add(text3);
//            row.add(text5);
        }
        {
            boolean isRight = false;
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = rowNumber;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            row.add(lblImageInfoL = new JLabel("<html>in: 1234567x1234567 r=1234567<br>out: 1234567x1234567 R=1234567</html>"));
            row.add(new DigitalZoomControl<Double, SizeChangeWrapper>().init("Size change:",4, new SizeChangeWrapper(), d -> doUpdateSizeChange(isRight, d)));
        }
        {
            boolean isRight = true;
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 3;
                gbc.gridy = rowNumber++;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            row.add(lblImageInfoR = new JLabel("<html>in: 1234567x1234567 r=1234567<br>out: 1234567x1234567 R=1234567</html>"));
            row.add(new  DigitalZoomControl<Double, SizeChangeWrapper>().init("Size change:",4, new SizeChangeWrapper(), d -> doUpdateSizeChange(isRight, d)));
        }
        {
            boolean isRight = false;
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = rowNumber;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            row.add(lblCorrectionMethodL = new JLabel("Correction method:"));
            row.add(chooserAlgoL=new FisheyeCorrectionAlgoChooser(algo -> {
                setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withAlgo(algo));
                System.out.println(getFisheyeCorrection(isRight));
            }));
            row.add(cbMendPixelsPrefilterL = new JCheckBox("Pre-filter: interpolate broken pixels"));
            cbMendPixelsPrefilterL.addActionListener( e -> {
                doSetPrefilter(isRight, cbMendPixelsPrefilterL.isSelected());
            });
        }
        {
            boolean isRight = true;
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 3;
                gbc.gridy = rowNumber++;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            row.add(lblCorrectionMethodR = new JLabel("Correction method:"));
            row.add(chooserAlgoR=new FisheyeCorrectionAlgoChooser(algo -> {
                setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withAlgo(algo));
                System.out.println(getFisheyeCorrection(isRight));
            }));
            row.add(cbMendPixelsPrefilterR = new JCheckBox("Pre-filter: interpolate broken pixels"));
            cbMendPixelsPrefilterR.addActionListener( e -> {
                doSetPrefilter(isRight, cbMendPixelsPrefilterR.isSelected());
            });
        }
        {
            boolean isRight = false;
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = rowNumber;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            row.add(new JLabel("Distortion center:"));
            row.add(chooserCenterHL=new DistortionCenterStationingChooser(
                    DistortionCenterStationing.HORIZ,
                    c -> {
                        setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withCenterH(c));
                        System.out.println(getFisheyeCorrection(isRight));
                    }));
            row.add(chooserCenterVL=new DistortionCenterStationingChooser(
                    DistortionCenterStationing.VERT,
                    c -> {
                        setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withCenterV(c));
                        System.out.println(getFisheyeCorrection(isRight));
                    }));
        }
        {
            boolean isRight = true;
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 3;
                gbc.gridy = rowNumber++;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            row.add(new JLabel("Distortion center:"));
            row.add(chooserCenterHR=new DistortionCenterStationingChooser(
                    DistortionCenterStationing.HORIZ,
                    c -> {
                        setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withCenterH(c));
                        System.out.println(getFisheyeCorrection(isRight));
                    }));
            row.add(chooserCenterVR=new DistortionCenterStationingChooser(
                    DistortionCenterStationing.VERT,
                    c -> {
                        setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withCenterV(c));
                        System.out.println(getFisheyeCorrection(isRight));
                    }));
        }
        {
            var text = new JLabel("Object marks: X and Y coordinates of marks 1 and 2 on the left and right images (NOT used for function calculation)");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(text, gbc);
            this.add(text);
        }

        {
            var text = new JLabel(
                    "<html>Calibration marks: X and Y coordinates of marks 3, 4 and 5 on the left and right images" +
                            " (<font color='blue'>Alt 3</font>/<font color='#00afaf'>Alt 4</font>/<font color='#ff00ff'>Alt 5</font>" +
                            " after closing this dialog)</html>"
            );
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber+3;
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
                gbc.gridy = (i&-2)+rowNumber;
                var dc = dcs.get(lr*dcRows*2 + i);
                gbl.setConstraints(dc, gbc);
                this.add(dc);
            }
        }
        // white space between controls for the left and right images
        for (int i = 0; i < dcRows*2; i++) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 2;
            gbc.gridy = (i&-2)+rowNumber;
            var t = new JLabel("        ");
            gbl.setConstraints(t, gbc);
            this.add(t);
        }
        rowNumber += dcRows*2;
        {
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = rowNumber++;
                gbc.gridheight = 1;
                gbc.gridwidth = 6;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            {
                var button = btnCalculateL = new JButton("Calculate Left");
                button.addActionListener(e -> doCalculate(false));
                row.add(button);
            }
            {
                var text = new JLabel("<html>Calculate correction function by 3 points (marks 3 to 5, "
                        + "<font color='blue'>blue</font>/<font color='#00ffff'>cyan</font>/<font color='#ff00ff'>magenta</font>)."
                        + "<br> Note that marks 1 and 2 are never used here!</html>");
                row.add(text);
            }
            {
                var button = btnCalculateR = new JButton("Calculate Right");
                button.addActionListener(e -> doCalculate(true));
                row.add(button);
            }
        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
            var text = new JLabel("<html>Normalize: make <i>g(r)=1</i> at one of points:</html>");
            row.add(text);
        }
        for (int threeCols=0; threeCols<2; threeCols++)
        {
            var row1 = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = threeCols * 3;
                gbc.gridy = rowNumber;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row1, gbc);
                this.add(row1);
            }
            var row2 = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = threeCols * 3;
                gbc.gridy = rowNumber+1;
                gbc.gridheight = 1;
                gbc.gridwidth = 3;
                gbl.setConstraints(row2, gbc);
                this.add(row2);
            }
            boolean isRight = threeCols != 0;
            {
                var button = new JButton("<html><font color='black'>Distortion Center</font></html>");
                button.addActionListener(e -> {
                    var fc = getFisheyeCorrection(isRight);
                    Dimension imageDim = uiEventListener.getRawImageDimensions(isRight);
                    doNormalizeFunction(
                        isRight,
                        fc.distortionCenterLocation.getPoleXBefore(imageDim.width, imageDim.height),
                        fc.distortionCenterLocation.getPoleYBefore(imageDim.width, imageDim.height)
                    );
                });
                button.setToolTipText("Distortion center may be outside the image (e.g. for Perseverance Hazcam)");
                row1.add(button);
            }
            {
                var button = new JButton("<html><font color='black'>Point In Image Nearest To Distortion Center</font></html>");
                button.addActionListener(e -> {
                    var fc = getFisheyeCorrection(isRight);
                    Dimension imageDim = uiEventListener.getRawImageDimensions(isRight);
                    doNormalizeFunction(
                            isRight,
                            fc.distortionCenterLocation.getNearestToPoleXBefore(imageDim.width, imageDim.height),
                            fc.distortionCenterLocation.getNearestToPoleYBefore(imageDim.width, imageDim.height)
                    );
                });
                button.setToolTipText("Point inside the image nearest to the distortion center");
                row1.add(button);
            }
            {
                var button = new JButton("<html><font color='red'>Mark 1</font></html>");
                button.addActionListener(e -> {
                    var pms = getPanelMeasurementStatus(isRight);
                    doNormalizeFunction(isRight, pms.x1, pms.y1);
                });
                row2.add(button);
            }
            {
                var button = new JButton("<html><font color='green'>Mark 2</font></html>");
                button.addActionListener(e -> {
                    var pms = getPanelMeasurementStatus(isRight);
                    doNormalizeFunction(isRight, pms.x2, pms.y2);
                });
                row2.add(button);
            }
            {
                var button = new JButton("<html><font color='blue'>Mark 3</font></html>");
                button.addActionListener(e -> {
                    var pms = getPanelMeasurementStatus(isRight);
                    doNormalizeFunction(isRight, pms.x3, pms.y3);
                });
                row2.add(button);
            }
            {
                var button = new JButton("<html><font color='#00afaf'>Mark 4</font></html>");
                button.addActionListener(e -> {
                    var pms = getPanelMeasurementStatus(isRight);
                    doNormalizeFunction(isRight, pms.x4, pms.y4);
                });
                row2.add(button);
            }
            {
                var button = new JButton("<html><font color='#ff00ff'>Mark 5</font></html>");
                button.addActionListener(e -> {
                    var pms = getPanelMeasurementStatus(isRight);
                    doNormalizeFunction(isRight, pms.x5, pms.y5);
                });
                row2.add(button);
            }
        }
        rowNumber += 2;
        {
            var text = new JLabel("Correction parameters as a string (you can copy or paste):");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(text, gbc);
            this.add(text);
        }
        {
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = rowNumber++;
                gbc.gridheight = 1;
                gbc.gridwidth = 6;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            {
                row.add(lblFunctionL = new JLabel("L:"));
                row.add(etxFunctionL = new JTextField(80));
                var button = new JButton("Set From Text");
                button.addActionListener(e -> {
                    if (doSetFcFromText(false, etxFunctionL.getText())) {
                        etxFunctionL.setForeground(Color.BLACK);
                    } else {
                        etxFunctionL.setForeground(Color.RED);
                    }
                });
                row.add(button);
            }
        }
        {
            var row = new JPanel();
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = rowNumber++;
                gbc.gridheight = 1;
                gbc.gridwidth = 6;
                gbl.setConstraints(row, gbc);
                this.add(row);
            }
            {
                row.add(lblFunctionR = new JLabel("R:"));
                row.add(etxFunctionR = new JTextField(80));
                var button = new JButton("Set From Text");
                button.addActionListener(e -> {
                    if (doSetFcFromText(true, etxFunctionR.getText())) {
                        etxFunctionR.setForeground(Color.BLACK);
                    } else {
                        etxFunctionR.setForeground(Color.RED);
                    }
                });
                row.add(button);
            }
        }
        {
            var text = new JLabel("<html>" +
                "Function <i>g(r):</i> " +
                "a pixel with polar coordinates <i>(r,φ)</i> comes from the pixel <i>(r·g(r),φ)</i> of the \"fisheye\" image," +
                " e.g. <i>g(r)=0.5</i> means \"magnify 2x\"" +
                "</html>"
            );
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(text, gbc);
            this.add(text);
        }
        {
            {
                ImageIcon emptyIcon = new ImageIcon(
                    new BufferedImage(GRAPH_WIDTH, GRAPH_HEIGHT, BufferedImage.TYPE_INT_ARGB)
                );
                lblGraphL = new JLabel(emptyIcon);
                lblGraphR = new JLabel(emptyIcon);
            }

            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);
//            row.add(lblFunctionInfoL);
            row.add(lblGraphL);
            {
                var col = new JPanel();
                col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
                {
                    var button = new JButton("L->R");
                    col.add(button);
                    button.addActionListener(actionEvent ->
                        setFisheyeCorrectionAndUpdateUi(true, getFisheyeCorrection(false))
                    );
                }
                {
                    var button = new JButton("L<-R");
                    col.add(button);
                    button.addActionListener(actionEvent ->
                        setFisheyeCorrectionAndUpdateUi(false, getFisheyeCorrection(true))
                    );
                }
                row.add(col);
            }
            row.add(lblGraphR);
//            row.add(lblFunctionInfoR);
        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);

            lblFunctionInfoL = new JLabel("<html>-------------------------<br>-<br>-<br>-</html>");
            lblFunctionInfoR = new JLabel("<html>-------------------------<br>-<br>-<br>-</html>");

            row.add(lblFunctionInfoL);
            row.add(new JLabel(""));
            row.add(lblFunctionInfoR);

        }
        {
            var row = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = rowNumber++;
            gbc.gridheight = 1;
            gbc.gridwidth = 6;
            gbl.setConstraints(row, gbc);
            this.add(row);

            JButton applyL = btnApplyL = new JButton("Apply (Left)");
            applyL.addActionListener(a -> { doApply(false); });
            row.add(applyL);
            row.add(new JLabel(""));
            JButton applyR = btnApplyR = new JButton("Apply (Right)");
            applyR.addActionListener(a -> { doApply(true); });
            row.add(applyR);
        }
        this.setLayout(gbl);
        leftHalf = new HalfPane(this, false,
                cbMendPixelsPrefilterL, chooserAlgoL, chooserCenterHL, chooserCenterVL,
                lblGraphL, lblFunctionInfoL, lblImageInfoL, etxFunctionL,
                lblCorrectionMethodL, lblFunctionL, btnCalculateL, btnApplyL,
                Arrays.asList(dcLX3, dcLY3, dcLX4, dcLY4, dcLX5, dcLY5)
        );
        rightHalf = new HalfPane(this, true,
                cbMendPixelsPrefilterR, chooserAlgoR, chooserCenterHR, chooserCenterVR,
                lblGraphR, lblFunctionInfoR, lblImageInfoR, etxFunctionR,
                lblCorrectionMethodR, lblFunctionR, btnCalculateR, btnApplyR,
                Arrays.asList(dcRX3, dcRY3, dcRX4, dcRY4, dcRX5, dcRY5)
        );
        // instead of addAncestorListener(new AncestorListener() { ... });
        // the initialization is done in showDialogIn()
    } //constructor

    // getters & setters
    PanelMeasurementStatus getPanelMeasurementStatus(boolean isRight) {
        return uiEventListener.getMeasurementStatus().getPanelMeasurementStatus(isRight);
    }
    FisheyeCorrection getFisheyeCorrection(boolean isRight) {
        return isRight ? fisheyeCorrectionR : fisheyeCorrectionL;
    }
    void setFisheyeCorrection(boolean isRight, FisheyeCorrection fc) {
        if (isRight) {
            fisheyeCorrectionR = fc;
        } else {
            fisheyeCorrectionL = fc;
        }
    }
    HalfPane getHalfPane(boolean isRight) {
        return isRight ? rightHalf : leftHalf;
    }
    void setFisheyeCorrectionAndUpdateUi(boolean isRight, FisheyeCorrection fcNew) {
        setFisheyeCorrection(isRight, fcNew);
        getHalfPane(isRight).setFromData();
    }

    // doXxx: reaction on buttons
    void doApply(boolean isRight) {
        uiEventListener.setFisheyeCorrection(isRight, getFisheyeCorrection(isRight));
        getHalfPane(isRight).setFromData();
    }
    void doSetPrefilter(boolean isRight, boolean isOn) {
        uiEventListener.setPreFilter(isRight, isOn);
    }
    void doNormalizeFunction(boolean isRight, double x, double y) {
        var fc = getFisheyeCorrection(isRight);
        Dimension imageDim = uiEventListener.getRawImageDimensions(isRight);
        var x0 = fc.distortionCenterLocation.getPoleXBefore(imageDim.width, imageDim.height);
        var y0 = fc.distortionCenterLocation.getPoleYBefore(imageDim.width, imageDim.height);

        System.out.println("normalizeFunction("+isRight+", "+x+", "+y+")  center:"+x0+", "+y0);
        var g = getFisheyeCorrection(isRight).func;
        double r = Math.hypot(x-x0, y-y0);
        var gr = g.apply(r);
        var fcNew = getFisheyeCorrection(isRight).withFunc(g.mul(1/gr));
        System.out.println("R=r="+r+", g(R)="+fcNew.func.apply(r));
        setFisheyeCorrectionAndUpdateUi(isRight, fcNew);
    }
    boolean doSetFcFromText(boolean isRight, String s) {
        var newFisheyeCorrection = FisheyeCorrection.fromParameterString(s);
        if (newFisheyeCorrection != null) {
            setFisheyeCorrectionAndUpdateUi(isRight, newFisheyeCorrection);
            return true;
        } else {
            return false;
        }
    }
    void doCalculate(boolean isRight) {
        System.out.println("doCalculate isRight="+isRight);
        new Exception("doCalculate invoked").printStackTrace();

        var pms = uiEventListener.getMeasurementStatus().getPanelMeasurementStatus(isRight);

        Dimension imageDim = uiEventListener.getRawImageDimensions(isRight);
        FisheyeCorrection fc = getFisheyeCorrection(isRight);
        DistortionCenterLocation dcl = fc.distortionCenterLocation;

        var g = fc.algo.calculateFunction(imageDim.width, imageDim.height, dcl, pms);

        setFisheyeCorrectionAndUpdateUi(isRight, fc.withFunc(g));
        System.out.println("doCalculate fc2="+ getFisheyeCorrection(isRight));
    }
    void doUpdateSizeChange(boolean isRight, Double d) {
        System.out.println("doUpdateSizeChange isRight="+isRight+" d="+d);
        setFisheyeCorrectionAndUpdateUi(isRight, getFisheyeCorrection(isRight).withSizeChange(d));
    }

    void showDialogIn(JFrame mainFrame) {
        setControls(uiEventListener.getMeasurementStatus());
        setFisheyeCorrectionAndUpdateUi(
                false,
                uiEventListener.getDisplayParameters().lFisheyeCorrection);
        setFisheyeCorrectionAndUpdateUi(
                true,
                uiEventListener.getDisplayParameters().rFisheyeCorrection);
        JustDialog.showMessageDialog(mainFrame, this,"Fisheye Correction", JOptionPane.PLAIN_MESSAGE);
//        JOptionPane.showMessageDialog(mainFrame, this,"Fisheye Correction", JOptionPane.PLAIN_MESSAGE);
    }
    FisheyeCorrectionPane setControls(MeasurementStatus ms) {
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
} // FisheyeCorrectionPane

interface HumanVisibleMathFunction {
    double apply(double x);
    default double applyMulX(double x) { return x*apply(x); }
    default DoubleUnaryOperator asFunction() { return this::apply; }
    default DoubleUnaryOperator asFunctionMulX() { return this::applyMulX; }
    String asString();
    default String asString(String x) { return asString().replaceAll("x",x); }
    String parameterString();
    double maxInRange(double x1, double x2);
    double minInRange(double x1, double x2);
    HumanVisibleMathFunction mul(double k);
    HumanVisibleMathFunction sub(double m);
    default HumanVisibleMathFunction add(double m) { return sub(-m); }
    HumanVisibleMathFunction derivative();
    default double[] findRoots() { return findRootsIn(-MAX_MEANINGFUL_X, MAX_MEANINGFUL_X); }
    double[] findRootsIn(double x1, double x2);
    default double[] findEqualIn(double y, double x1, double x2) { return sub(y).findRootsIn(x1, x2); }
    String DOUBLE_FMT = "%.4g";
    double MAX_MEANINGFUL_X = 1e10;

    HumanVisibleMathFunction NO_FUNCTION = QuadraticPolynomial.of(Double.NaN, Double.NaN, Double.NaN);

    static Optional<HumanVisibleMathFunction> fromParameterString(String params) {
        // It is bad, that the interface knows its implementations, but...
        // There is a side effect that all these classes are loaded and are
        // available via the UI, even if not referenced from the code.
        // We could use a static initializer:
        //   static { declareParser(Xyz::fromParamString); }
        // but it does not guarantee loading of all these classes.
        // Static referencing a subclass from a superclass opens a possibility for a class loader deadlock.
        List<Function<String, Optional<HumanVisibleMathFunction>>> parsers =
        Arrays.asList(
                MultiplicativeInversePlusC::fromParamString,
                OfXSquared::fromParamString,
                BiquadraticPolynomial::fromParamString,
                QuarticPolynomial::fromParamString,
                CubicPolynomial::fromParamString,
                QuadraticPolynomial::fromParamString,
                LinearPolynomial::fromParamString,
                ConstantPolynomial::fromParamString,
                ReTangentPlusC::fromParamString
        );
        for (var p : parsers) {
            var res = p.apply(params);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }
}
interface Double6Function<R> {
    R apply(double x1, double y1, double x2, double y2, double x3, double y3);
}
abstract class HumanVisibleMathFunctionBase implements HumanVisibleMathFunction {
    static final double ROOT_PREC = 0.001;

    static boolean isBetween(double xLim1, double x, double xLim2) {
        return Double.isFinite(x)
            && ((xLim1 <= x && x <= xLim2) || (xLim2 <= x && x <= xLim1));
    }
    static double[] pointsAndLimits(double[] points, double x1, double x2) {
        var res = DoubleStream.concat(
            DoubleStream.of(points),
            DoubleStream.of(x1, x2)
        )
                .filter(d -> x1 <= d && d <= x2)
                .sorted().toArray();
        return removeDuplicatesInSorted(res);
    }
    static double[] removeDuplicatesInSorted(double[] p) {
        return removeDuplicatesInSorted(p, p.length);
    }

    /**
     * Assuming that p is sorted, remove duplicates and non-finite numbers.
     * @param p sorted array, probably with infinities and NaN-s
     * @param pSize how many numbers are stored in p, pSize <= p.length
     * @return
     */
    static double[] removeDuplicatesInSorted(double[] p, int pSize) {
        double prev = Double.NaN;
        int o = 0;
        int i = 0;
        for (; i<pSize; i++) {
            double v = p[i];
            if (Double.isFinite(v) && v != prev) {
                o++;
                prev = v;
            } else {
                i++;
                break;
            }
        }
        for (; i<pSize; i++) {
            double v = p[i];
            if (Double.isFinite(v) && v != prev) {
                p[o++] = v;
                prev = v;
            }
        }
        if (o < p.length) {
            return Arrays.copyOf(p, o);
        } else {
            return p;
        }
    }

    /** Knowing that only one root exists in [x1, x2], find the root */
    static double findRootWhenSafe(DoubleUnaryOperator function, double x1, double x2) {
        var s1 = function.applyAsDouble(x1) > 0;
        var PREC = ROOT_PREC/2;
        while(Math.abs(x1/2-x2/2) > PREC) {
            var x = x1/2 + x2/2;
            var s = function.applyAsDouble(x) > 0;
            if (s==s1) {
                x1 = x;
            } else {
                x2 = x;
            }
        }
        return x1;
    }
    boolean haveRoots(double x1, double x2) {
        return Math.signum(apply(x1)) != Math.signum(apply(x2));
    }

    /** The default implementation of findRootsIn(): find critical points and,
     * if the fuction changes sign, find roots between them. */
    @Override
    public double[] findRootsIn(double x1, double x2) {
        double[] points = pointsAroundIntervalsOfMonotonicity(x1, x2);
        var roots = new double[points.length-1];
        int o = 0;
        for (int i=1; i<points.length; i++) {
            if (haveRoots(points[i-1], points[i])) {
                roots[o++] = findRootWhenSafe(this.asFunction(), points[i - 1], points[i]);
            }
        }
        Arrays.sort(roots,0,o);
        return removeDuplicatesInSorted(roots, o); // TODO: delta
    }
    protected double[] pointsAroundIntervalsOfMonotonicity(double x1, double x2) {
        return pointsAndLimits(derivative().findRootsIn(x1, x2), x1, x2);
    }
    /** Implementation of findRootsIn() that works if findRoots() can find all roots using
     * an analytic formula */
    public double[] findRootsIn_via_findRoots(double x1, double x2) {
        var r = findRoots();
        return DoubleStream.of(r).filter(d -> isBetween(x1, d, x2)).toArray();
    }

    /**
     *
     * @param s string to parse
     * @param prefix Prefix common for each class of functions,
     *              like "P2" for quadratic polynomials
     * @param f a lambda that receives an Iterator<String>,
     *         parses fp numbers in strings retrieved from the iterator,
     *         and creates a HumanVisibleMathFunction or throws a RuntimeException
     * @return
     */
    protected static Optional<HumanVisibleMathFunction> doFromParamString(String s, String prefix, Function<Iterator<String>, HumanVisibleMathFunction> f) {
        try {
            var arr = s.split("\\s+");
            var list = Collections.unmodifiableList(Arrays.asList(arr)); // List.of(arr) requires Java 9 :(
            var iter = list.iterator();
            parseExpect(iter.next(), prefix);
            HumanVisibleMathFunction res = f.apply(iter);
            parseSuffix(iter);
            return Optional.of(res);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    protected static Optional<HumanVisibleMathFunction> ifParamStringPrefix(String prefix, String paramString, Function<String, Optional<HumanVisibleMathFunction>> parseTheRest) {
        var ss = paramString.split("\\s+", 2);
        if (ss.length == 2 && prefix.equals(ss[0])) {
            return parseTheRest.apply(ss[1]);
        } else {
            return Optional.empty();
        }
    }
    public  static void parseExpect(String s, String e) {
        if (!s.equals(e)) {
            throw new IllegalArgumentException();
        }
    }
    public static void parseSuffix(Iterator<String> i) {
        if (i.hasNext()) {
            throw new IllegalArgumentException();
        }
    }
} // HumanVisibleMathFunctionBase

/**
 * "Re-tangent": retan(k,q,x) = (q/k)*tan(k*arctan(x/q))
 * In fact, here we use a*retan(k,q,x)+c = (a*q/k)*tan(k*arctan(x/q))+c,
 * and now we are interested only in k<1.
 */
class ReTangentPlusC extends HumanVisibleMathFunctionBase {
    final double k, q, a, c, aqk, dx0;

    private ReTangentPlusC(double k, double q, double a, double c) {
        this.k = k;
        this.q = q;
        this.a = a;
        this.c = c;
        this.aqk = a * q / k;
        // this should depend on k and q, but I do not want to split hairs
        this.dx0 = Math.nextUp(1.) - 1.;
    }

    /**
     * Optimization for the case of c==0.
     * The functions defined here will be used in a big long loop.
     */
    static class ReTangentPlusC0 extends ReTangentPlusC {
        private ReTangentPlusC0(double k, double q, double a) {
            super(k, q, a, 0.);
        }
        @Override
        public double apply(double x) {
            return Math.abs(x) > dx0 ? aqk * Math.tan(k * Math.atan(x / q)) / x : a;
        }
        @Override
        public double applyMulX(double x) {
            return aqk * Math.tan(k * Math.atan(x / q));
        }
    }
    public static ReTangentPlusC of(double k, double q, double a, double c) {
        if (c == 0.) {
            return new ReTangentPlusC0(k, q, a);
        } else {
            return new ReTangentPlusC(k, q, a, c);
        }
    }
    public static ReTangentPlusC of(double k, double q) {
        return of(k, q, 1., 0.);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return doFromParamString(
                s,
                "RETAN",
                i -> {
                    var a = Double.parseDouble(i.next());
                    var b = Double.parseDouble(i.next());
                    var c = Double.parseDouble(i.next());
                    var d = Double.parseDouble(i.next());
                    HumanVisibleMathFunction p = ReTangentPlusC.of(a, b, c, d);
                    return p;
                });
    }
    @Override
    public double apply(double x) {
        return (Math.abs(x) > dx0 ? aqk * Math.tan(k * Math.atan(x / q)) / x : a) + c;
    }
    @Override
    public double applyMulX(double x) {
        return aqk * Math.tan(k * Math.atan(x / q)) + c * x;
    }
    @Override
    public String asString() {
        return String.format("(" + DOUBLE_FMT + "*" + DOUBLE_FMT + "/" + DOUBLE_FMT + ")*tan("+ DOUBLE_FMT + "*atan(x/"+ DOUBLE_FMT+")) + " + DOUBLE_FMT,
                                   a,                 q,                 k,                     k,                       q,                   c);
    }
    @Override
    public String parameterString() {
        return "RETAN " + k + " " + q + " " + a + " " + c;
    }
    @Override
    public double maxInRange(double x1, double x2) {
        if (k < 1.) {
            if (x1 <= 0. && 0 <= x2) {
                return Math.max(Math.max(apply(0), apply(x1)), apply(x2));
            } else {
                return Math.max(apply(x1), apply(x2));
            }
        } else if (k == 1.) {
            return apply(0);
        } else {
            throw new UnsupportedOperationException("not going to use k>1");
        }
    }
    @Override
    public double minInRange(double x1, double x2) {
        if (Math.abs(k) < 1.) {
            if (x1 <= 0. && 0 <= x2) {
                return Math.min(Math.min(apply(0), apply(x1)), apply(x2));
            } else {
                return Math.min(apply(x1), apply(x2));
            }
        } else if (Math.abs(k) == 1.) {
            return apply(0.);
        } else {
            throw new UnsupportedOperationException("not going to use k>1");
        }
    }
    @Override
    protected double[] pointsAroundIntervalsOfMonotonicity(double x1, double x2) {
        if (Math.abs(k) < 1.) {
            if (x1 <= 0. && 0 <= x2) {
                return pointsAndLimits(new double[] {0}, x1, x2);
            } else {
                return pointsAndLimits(new double[] {}, x1, x2);
            }
        } else if (Math.abs(k) == 1.) {
            return pointsAndLimits(new double[] {}, x1, x2);
        } else {
            throw new UnsupportedOperationException("not going to use k>1");
        }
    }
    @Override
    public ReTangentPlusC mul(double kk) {
        return of(k, q, a*kk, c*kk);
    }
    @Override
    public ReTangentPlusC sub(double m) {
        return of(k, q, a, c-m);
    }
    @Override
    public ReTangentPlusC add(double m) {
        return (ReTangentPlusC) super.add(m);
    }
    @Override
    public HumanVisibleMathFunction derivative() {
        throw new UnsupportedOperationException();
    }
    @Override
    public String toString() {
        return "ReTangentPlusC{" +
               "k=" + k +
               ", q=" + q +
               ", a=" + a +
               ", c=" + c +
               ", a*q/k=" + aqk +
               ", " + asString() +
               '}';
    }
} // ReTangentPlusC
class MultiplicativeInversePlusC<T extends HumanVisibleMathFunction> extends HumanVisibleMathFunctionBase {
    final T f;
    final double c;

    MultiplicativeInversePlusC(T f, double c) {
        this.f = f;
        this.c = c;
    }
    public static<HVMF extends HumanVisibleMathFunction> MultiplicativeInversePlusC<HVMF> of(HVMF f, double c) {
        return new MultiplicativeInversePlusC<HVMF>(f, c);
    }
    public static<HVMF extends HumanVisibleMathFunction> MultiplicativeInversePlusC<HVMF> from3Points(double x1, double y1, double x2, double y2, double x3, double y3, Double6Function<HVMF> from3points) {
        HVMF q = from3points.apply(x1,1/y1, x2,1/y2, x3,1/y3);
        return of(q, 0);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return ifParamStringPrefix("C+1/", s, rest -> {
            try {
                var ss = rest.split("\\s+", 2);
                var c = Double.parseDouble(ss[0]);
                var fo = HumanVisibleMathFunction.fromParameterString(ss[1]);
                return fo.map(f -> of(f, c));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public double apply(double x) {
        return 1./f.apply(x) + c;
    }
    @Override
    public double applyMulX(double x) {
        return x*(1./f.apply(x) + c);
    }
    @Override
    public String asString() {
        return String.format(DOUBLE_FMT, c) + " + 1/( " + f.asString() + " )";
    }
    @Override
    public String parameterString() {
        return "C+1/ " + c + " " + f.parameterString();
    }
    @Override
    public double maxInRange(double x1, double x2) {
        double[] roots = f.findRootsIn(x1, x2);
        for (var r: roots) {
            if (x1 <= r && r <= x2) {
                return Double.NaN;
            }
        }
        return 1./f.minInRange(x1,x2) + c;
    }
    @Override
    public double minInRange(double x1, double x2) {
        double[] roots = f.findRootsIn(x1, x2);
        for (var r: roots) {
            if (x1 <= r && r <= x2) {
                return Double.NaN;
            }
        }
        return 1./f.maxInRange(x1, x2) + c;
    }
    @Override
    public HumanVisibleMathFunction mul(double k) {
        return of(f.mul(1/k), k*c);
    }
    @Override
    public HumanVisibleMathFunction sub(double m) {
        return of(f, c-m);
    }
    @Override
    public HumanVisibleMathFunction derivative() {
        throw new UnsupportedOperationException("derivative not implemented");
    }
    @Override
    public double[] findRoots() {
        return f.add(1./c).findRoots();
    }
    @Override
    public double[] findRootsIn(double x1, double x2) {
        return f.add(1./c).findRootsIn(x1, x2);
    }
    @Override
    public String toString() {
        return "MultiplicativeInversePlusC{" +
               "f=" + f +
               ", c=" + c +
               ", " + asString() +
               '}';
    }
} // MultiplicativeInversePlusC
class OfXSquared<T extends HumanVisibleMathFunction> extends HumanVisibleMathFunctionBase {
    final T f;

    OfXSquared(T f) {
        this.f = f;
    }
    public static<TT extends HumanVisibleMathFunction> OfXSquared<TT> of(TT f) {
        return new OfXSquared<TT>(f);
    }
    public OfXSquared<T> v_of(T f) {
        return of(f);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return ifParamStringPrefix("OFSQR", s, restOfS ->
                HumanVisibleMathFunction.fromParameterString(restOfS).map(OfXSquared::of)
        );
    }
    @Override
    public double apply(double x) {
        return f.apply(x*x);
    }
    @Override
    public double applyMulX(double x) {
        return x*f.apply(x*x);
    }
    @Override
    public String asString() {
        return f.asString("(x^2)");
    }
    @Override
    public String parameterString() {
        return "OFSQR "+f.parameterString();
    }
    @Override
    public double maxInRange(double x1, double x2) {
        return f.maxInRange(x1*x1, x2*x2);
    }
    @Override
    public double minInRange(double x1, double x2) {
        return f.minInRange(x1*x1, x2*x2);
    }
    @Override
    public OfXSquared<T> mul(double k) {
        return v_of((T)f.mul(k)); // mul() returns type_of(this)
    }
    @Override
    public OfXSquared<T> sub(double m) {
        return v_of((T)f.sub(m)); // sub() returns type_of(this)
    }
    @Override
    public HumanVisibleMathFunction derivative() {
        throw new UnsupportedOperationException("derivative not implemented");
    }
    @Override
    public double[] findRoots() {
        double[] roots = Arrays.stream(f.findRoots())
                .map(Math::sqrt)
                .filter(Double::isFinite)
                .flatMap(d -> d==0. ? DoubleStream.of(d) : DoubleStream.of(d, -d) )
                .toArray();
        Arrays.sort(roots);
        return roots;
    }
    @Override
    public double[] findRootsIn(double x1, double x2) {
        double xx1 = x1*x1, xx2 = x2*x2;
        double l, u;
        if (x1 < 0 && x2 < 0 || x1 > 0 && x2 > 0) {
            l = Math.min(xx1, xx2);
            u = Math.max(xx1, xx2);
        } else {
            l = 0.;
            u = Math.max(xx1, xx2);
        }
        double[] roots = Arrays.stream(f.findRootsIn(l, u))
                .map(Math::sqrt)
                .filter(Double::isFinite)
                .flatMap(d -> d==0. ? DoubleStream.of(d) : DoubleStream.of(d, -d) )
                .filter(x -> x >= x1 && x <= x2)
                .toArray();
        Arrays.sort(roots);
        return roots;
    }
    @Override
    public String toString() {
        return "OfXSquared{"+f.toString()+"}";
    }
} // OfXSquared
class BiquadraticPolynomial extends OfXSquared<QuadraticPolynomial> {
    private BiquadraticPolynomial(QuadraticPolynomial f) {
        super(f);
    }
    public static BiquadraticPolynomial of(QuadraticPolynomial f) {
        return new BiquadraticPolynomial(f);
    }
    public static<TT extends HumanVisibleMathFunction> OfXSquared<TT> of(TT f) {
        throw new UnsupportedOperationException("generic of() disabled");
    }
    public BiquadraticPolynomial v_of(QuadraticPolynomial f) {
        return of(f);
    }
    public static BiquadraticPolynomial from3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
        QuadraticPolynomial q = QuadraticPolynomial.from3Points(x1*x1,y1, x2*x2,y2,x3*x3,y3);
        return of(q);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        var ss = s.split("\\s+", 2);
        if (ss.length == 2 && "BIQUADR".equals(ss[0])) {
            return HumanVisibleMathFunction.fromParameterString(ss[1]).map(p -> of((QuadraticPolynomial)p));
        } else {
            return Optional.empty();
        }
    }
    @Override
    public String parameterString() {
        return "BIQUADR "+f.parameterString();
    }
    @Override
    public BiquadraticPolynomial mul(double k) {
        return (BiquadraticPolynomial) super.mul(k);
    }
    @Override
    public BiquadraticPolynomial sub(double m) {
        return (BiquadraticPolynomial) super.sub(m);
    }
    @Override
    public CubicPolynomial derivative() {
        return toQuartic().derivative();
    }
    QuarticPolynomial toQuartic() {
        return QuarticPolynomial.of(f.a, 0, f.b, 0, f.c);
    }
    @Override
    public String toString() {
        return "BiquadraticPolynomial{"+f.toString()
               + ", " + asString()
               + "}";
    }
} // BiquadraticPolynomial
class QuarticPolynomial extends HumanVisibleMathFunctionBase implements HumanVisibleMathFunction {
    final double a,b,c,d,e;

    private QuarticPolynomial(double a, double b, double c, double d, double e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }
    public static QuarticPolynomial of(double a, double b, double c, double d, double e) {
        return new QuarticPolynomial(a, b, c, d, e);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return doFromParamString(
                s,
                "P4",
                i -> {
                    var a = Double.parseDouble(i.next());
                    var b = Double.parseDouble(i.next());
                    var c = Double.parseDouble(i.next());
                    var d = Double.parseDouble(i.next());
                    var e = Double.parseDouble(i.next());
                    HumanVisibleMathFunction p = QuarticPolynomial.of(a, b, c, d, e);
                    return p;
                });
    }
    @Override
    public double apply(double x) {
        return (((a*x + b)*x + c)*x + d)*x +e;
    }
    @Override
    public double applyMulX(double x) {
        return ((((a*x + b)*x + c)*x + d)*x +e)*x;
    }
    @Override
    public String asString() {
        return String.format(DOUBLE_FMT + "*x^4 + "+ DOUBLE_FMT + "*x^3 + "+ DOUBLE_FMT + "*x^2 + "+ DOUBLE_FMT + "*x + "+ DOUBLE_FMT,
                             a,                      b,                      c,                      d,                    e);
    }
    @Override
    public String parameterString() {
        return "P4 " + a + " " + b + " " + c + " " + d + " " + e;
    }
    @Override
    public QuarticPolynomial mul(double k) {
        return of(k*a, k*b, k*c, k*d, k*e);
    }
    @Override
    public QuarticPolynomial sub(double m) {
        return of(a, b, c, d, e-m);
    }
    public QuarticPolynomial add(double m) {
        return (QuarticPolynomial) super.add(m);
    }
    @Override
    public CubicPolynomial derivative() {
        return CubicPolynomial.of(4*a, 3*b, 2*c, d);
    }
    @Override
    public double maxInRange(double x1, double x2) {
        double[] xs = pointsAndLimits(derivative().findRootsIn(x1, x2), x1, x2);
        OptionalDouble res = DoubleStream.of(xs).map(this::apply).max();
        return res.getAsDouble();
    }
    @Override
    public double minInRange(double x1, double x2) {
        double[] xs = pointsAndLimits(derivative().findRootsIn(x1, x2), x1, x2);
        OptionalDouble res = DoubleStream.of(xs).map(this::apply).min();
        return res.getAsDouble();
    }
    @Override
    public String toString() {
        return "QuarticPolynomial{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", e=" + e +
                ", " + asString() +
                '}';
    }
}
class CubicPolynomial extends HumanVisibleMathFunctionBase implements HumanVisibleMathFunction {
    final double a,b,c,d;

    private CubicPolynomial(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    public static CubicPolynomial of(double a, double b, double c, double d) {
        return new CubicPolynomial(a, b, c, d);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return doFromParamString(
                s,
                "P3",
                i -> {
                    var a = Double.parseDouble(i.next());
                    var b = Double.parseDouble(i.next());
                    var c = Double.parseDouble(i.next());
                    var d = Double.parseDouble(i.next());
                    HumanVisibleMathFunction p = CubicPolynomial.of(a, b, c, d);
                    return p;
                });
    }
    @Override
    public double apply(double x) {
        return ((a*x + b)*x + c)*x + d;
    }
    @Override
    public double applyMulX(double x) {
        return (((a*x + b)*x + c)*x + d)*x;
    }
    @Override
    public String asString() {
        return String.format(DOUBLE_FMT + "*x^3 + "+ DOUBLE_FMT + "*x^2 + "+ DOUBLE_FMT + "*x + "+ DOUBLE_FMT,
                             a,                      b,                      c,                    d);
    }
    @Override
    public String parameterString() {
        return "P3 " + a + " " + b + " " + c + " " + d;
    }
    @Override
    public CubicPolynomial mul(double k) {
        return of(k*a, k*b, k*c, k*d);
    }
    @Override
    public CubicPolynomial sub(double m) {
        return of(a, b, c, d-m);
    }
    @Override
    public CubicPolynomial add(double m) {
        return (CubicPolynomial) super.add(m);
    }
    @Override
    public QuadraticPolynomial derivative() {
        return QuadraticPolynomial.of(3*a, 2*b, c);
    }
    @Override
    public double maxInRange(double x1, double x2) {
        double[] xs = pointsAndLimits(derivative().findRootsIn(x1, x2), x1, x2);
        OptionalDouble res = DoubleStream.of(xs).map(this::apply).max();
        return res.getAsDouble();
    }
    @Override
    public double minInRange(double x1, double x2) {
        double[] xs = pointsAndLimits(derivative().findRootsIn(x1, x2), x1, x2);
        OptionalDouble res = DoubleStream.of(xs).map(this::apply).min();
        return res.getAsDouble();

    }
    @Override
    public String toString() {
        return "CubicPolynomial{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", " + asString() +
                '}';
    }
}
class QuadraticPolynomial extends HumanVisibleMathFunctionBase implements HumanVisibleMathFunction {
    final double a,b,c;

    private QuadraticPolynomial(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    public static QuadraticPolynomial of(double a, double b, double c) {
        return new QuadraticPolynomial(a, b, c);
    }
    public static QuadraticPolynomial from3Points(double x1, double y1, double x2, double y2, double x3, double y3) {
        double YX1 = y1 / ((x1 - x2) * (x1 - x3));
        double YX2 = y2 / ((x2 - x1) * (x2 - x3));
        double YX3 = y3 / ((x3 - x1) * (x3 - x2));
        return of(
            YX1 + YX2 + YX3, // *x^2
            - YX1 * (x2 + x3) - YX2 * (x1 + x3) - YX3 * (x1 + x2), // *x^1
            YX1*x2*x3 + YX2*x1*x3 + YX3*x1*x2 // *1
        );
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return doFromParamString(
                s,
                "P2",
                i -> {
                    var a = Double.parseDouble(i.next());
                    var b = Double.parseDouble(i.next());
                    var c = Double.parseDouble(i.next());
                    HumanVisibleMathFunction p = QuadraticPolynomial.of(a, b, c);
                    return p;
                });
    }
    @Override
    public double apply(double x) {
        return (a*x + b)*x + c;
    }
    @Override
    public double applyMulX(double x) {
        return ((a*x + b)*x + c)*x;
    }
    @Override
    public String asString() {
        return String.format(DOUBLE_FMT + "*x^2 + "+ DOUBLE_FMT + "*x + "+ DOUBLE_FMT,
                             a,                      b,                    c);
    }
    @Override
    public String parameterString() {
        return "P2 " + a + " " + b + " " + c;
    }
    @Override
    public QuadraticPolynomial mul(double k) {
        return of(k*a, k*b, k*c);
    }
    @Override
    public QuadraticPolynomial sub(double m) {
        return of(a, b, c-m);
    }
    @Override
    public QuadraticPolynomial add(double m) {
        return (QuadraticPolynomial) super.add(m);
    }
    @Override
    public LinearPolynomial derivative() {
        return LinearPolynomial.of(2*a, b);
    }
    @Override
    public double maxInRange(double x1, double x2) {
        double y = Math.max(apply(x1), apply(x2));
        double xe = xOfExtremum();
        if (isBetween(x1, xe, x2)) {
            y = Math.max(y, apply(xe));
        }
        return y;
    }
    @Override
    public double minInRange(double x1, double x2) {
        double y = Math.min(apply(x1), apply(x2));
        double xe = xOfExtremum();
        if (isBetween(x1, xe, x2)) {
            y = Math.min(y, apply(xe));
        }
        return y;
    }
    double xOfExtremum() {
        return -b / (2 * a);
    }
    @Override
    public double[] findRoots() {
        double sqrtD = Math.sqrt(b*b - 4*a*c);
        double x1 = (-b - sqrtD) / (2*a);
        double x2 = (-b + sqrtD) / (2*a);
        if (!Double.isFinite(x1)) {
            x1 = x2;
        } else if (!Double.isFinite(x2)) {
            x2 = x1;
        }
        if (Double.doubleToLongBits(x1) == Double.doubleToLongBits(x2)) // need NaN==NaN
        {
            if (Double.isFinite(x1)) {
                return new double[] { x1 };
            } else {
                return new double[] {};
            }
        } else {
            return new double[] { x1, x2 };
        }
    }
    @Override
    public double[] findRootsIn(double x1, double x2) {
        return findRootsIn_via_findRoots(x1, x2);
    }
    @Override
    public String toString() {
        return "QuadraticPolynomial{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", " + asString() +
                '}';
    }
}
class LinearPolynomial extends HumanVisibleMathFunctionBase implements HumanVisibleMathFunction {
    final double a,b;

    public LinearPolynomial(double a, double b) {
        this.a = a;
        this.b = b;
    }
    public static LinearPolynomial of(double a, double b) {
        return new LinearPolynomial(a, b);
    }
    public static LinearPolynomial from2Points(double x1, double y1, double x2, double y2) {
        double a = (y2 - y1) / (x2 - x1);
        double b = y1 - a*x1;
        return of(a, b);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return doFromParamString(
                s,
                "P1",
                i -> {
                    var a = Double.parseDouble(i.next());
                    var b = Double.parseDouble(i.next());
                    HumanVisibleMathFunction p = LinearPolynomial.of(a, b);
                    return p;
                });
    }
    @Override
    public double apply(double x) {
        return a*x + b;
    }
    @Override
    public double applyMulX(double x) {
        return (a*x + b)*x;
    }
    @Override
    public String asString() {
        return String.format(DOUBLE_FMT + "*x + "+ DOUBLE_FMT,
                             a,                      b);
    }
    @Override
    public String parameterString() {
        return "P1 " + a + " " + b;
    }
    @Override
    public double maxInRange(double x1, double x2) {
        return Math.max(apply(x1), apply(x2));
    }
    @Override
    public double minInRange(double x1, double x2) {
        return Math.min(apply(x1), apply(x2));
    }
    @Override
    public LinearPolynomial mul(double k) {
        return of(k*a, k*b);
    }
    @Override
    public LinearPolynomial sub(double m) {
        return of(a, b-m);
    }
    @Override
    public ConstantPolynomial derivative() {
        return ConstantPolynomial.of(a);
    }
    @Override
    public double[] findRoots() {
        return new double[]{ -b/a };
    }
    @Override
    public double[] findRootsIn(double x1, double x2) {
        return findRootsIn_via_findRoots(x1, x2);
    }

    @Override
    public String toString() {
        return "LinearPolynomial{" +
                "a=" + a +
                ", b=" + b +
                ", " + asString() +
                '}';
    }
}
class ConstantPolynomial extends HumanVisibleMathFunctionBase implements HumanVisibleMathFunction {
    final double a;

    public ConstantPolynomial(double a) {
        this.a = a;
    }
    public static ConstantPolynomial of(double a) {
        return new ConstantPolynomial(a);
    }
    public static ConstantPolynomial from1Points(double x1, double y1) {
        return of(y1);
    }
    public static Optional<HumanVisibleMathFunction> fromParamString(String s) {
        return doFromParamString(
                s,
                "P0",
                i -> {
                    var a = Double.parseDouble(i.next());
                    HumanVisibleMathFunction p = ConstantPolynomial.of(a);
                    return p;
                });
    }
    @Override
    public double apply(double x) {
        return a;
    }
    @Override
    public String asString() {
        return String.format(DOUBLE_FMT, a);
    }
    @Override
    public String parameterString() {
        return "P0 " + a;
    }
    @Override
    public double maxInRange(double x1, double x2) {
        return a;
    }
    @Override
    public double minInRange(double x1, double x2) {
        return a;
    }
    @Override
    public ConstantPolynomial mul(double k) {
        return of(k*a);
    }
    @Override
    public HumanVisibleMathFunction sub(double m) {
        return of(a-m);
    }
    @Override
    public HumanVisibleMathFunction derivative() {
        return ConstantPolynomial.of(0.);
    }
    @Override
    public double[] findRoots() {
        return new double[]{};
    }
    @Override
    public double[] findRootsIn(double x1, double x2) {
        return new double[]{};
    }

    @Override
    public String toString() {
        return "ConstantPolynomial{" +
                "a=" + a +
                ", " + asString() +
                '}';
    }
}
class GraphPlotter {
    final static int BMARGIN = 1;
    final static int DIGIT_XMARGIN = 1;
    final static int DIGIT_YMARGIN = 2;

    static BufferedImage plotGraph(
            final int width, final int height,
            final double xMin, final double xMax,
            final double fMax,
            Color color,
            DoubleUnaryOperator f,
            double[] points,
            int[] colors) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        var yMax = graphMaxY(fMax);
        int zeroYLevel = height - BMARGIN;
        var yScale = zeroYLevel / yMax;
        var xScale = (width-1) / xMax;
        {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
        }
        {
            int y1 = 0;//zeroYLevel - 3;
            int y2 = zeroYLevel;
            for (int k=0; k<points.length; k++) {
                double x = points[k];
                g.setColor(new Color(colors[k]));
                int i = (int) Math.round(x * xScale);
                g.drawLine(i, y1, i, y2);
            }
        }
        {
            int y1 = 0;//zeroYLevel - 3;
            int y2 = zeroYLevel;
            var d = deltaXBetweenDotsOnAxis(xMax);
            for (double x=d; x<xMax; x+=d) {
                int i = (int) Math.round(x * xScale);
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(i, y1, i, y2);
                g.setColor(Color.BLACK);
                g.drawString(String.format("%d",(int)Math.round(x)), i + DIGIT_XMARGIN, y2 - DIGIT_YMARGIN);
            }
        }
        {
            g.setColor(Color.BLACK);
            var d = deltaYBetweenLines(fMax);
            String yFormat = d % 1. == 0. ? "%.0f" : "%.2f";
            for (double y=0; y<yMax; y += d) {
                int j = (int) Math.round(y * yScale);
                if(y != 0) {
                    g.setColor(Color.LIGHT_GRAY);
                }
                g.drawLine(0, zeroYLevel - j, width, zeroYLevel - j);
                g.setColor(Color.BLACK);
                if (y!=0) {
                    g.drawString(String.format(yFormat, y), 0 + DIGIT_XMARGIN, zeroYLevel - j - DIGIT_YMARGIN);
                } else {
                    g.drawString("0", 0 + DIGIT_XMARGIN, zeroYLevel - j - DIGIT_YMARGIN);
                }
            }
        }
        {
            if (xMin == 0) {
                // Y axis
                g.drawLine(0,zeroYLevel,0,zeroYLevel-(int)Math.round(yMax * yScale));
            }
        }
        {
            g.setColor(color);
            double w1 = width - 1;
            int jOld = (int) Math.round(f.applyAsDouble(xMin) * yScale);
            for (int i = 1; i < width; i++) {
                double x = xMin + xMax * i / w1;
                int j = (int) Math.round(f.applyAsDouble(x) * yScale);
                g.drawLine(i - 1, zeroYLevel - jOld, i, zeroYLevel - j);
                jOld = j;
            }
        }

        g.dispose();
        return bi;
    }
    static double deltaYBetweenLines(double y) {
        double dy = Math.pow(10, Math.floor(Math.log10(y)-0.004));
        if (y/dy > 9) {
            dy *= 2;
        }
        if (y/dy < 4) {
            dy /= 2;
        }
        if (y/dy < 4) {
            dy /= 2;
        }
        return dy;
    }
    static double deltaXBetweenDotsOnAxis(double x) {
        double dx = Math.pow(10, Math.floor(Math.log10(x)-0.004));
        if (x/dx < 4) {
            dx /= 2;
        }
        return dx;
    }
    static double graphMaxY(double y) {
        var dy = deltaYBetweenLines(y);
        var n = Math.ceil(y/dy);
        return n*dy;
    }
}

enum MeasurementPointMark {
    POINT(new int[][]{{0,0}}, "a single pixel"),
    CROSS(new int[][]{{1,0}, {2,0}, {3,0}, {4,0}, {5,0}, {6,0}}, "cross-hair"),
    XCROSS(new int[][]{{1,1}, {2,2}, {3,3}, {4,4}, {5,5}, {6,6}}, "x-shaped"),
    DAISY(new int[][]{{1,1}, {2,2}, {3,3}, {4,4}, {5,5}, {6,6}, {3,2}, {4,3}, {5,4}, {6,5}, {6,4}, {5,3}}, "thick mark, visible with zoom=0.125");

    final int[][] listOfXY;
    final String description;

    MeasurementPointMark(int[][] listOfXY, String descr) {
        this.listOfXY = listOfXY;
        this.description = descr;
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
    public String getDescription() { return description; }
}
class MeasurementPointMarkChooser extends ComboBoxWithTooltips<MeasurementPointMark> {
    static MeasurementPointMark[] modes = MeasurementPointMark.values();
    public MeasurementPointMarkChooser(Consumer<MeasurementPointMark> valueListener) {
        super(modes, MeasurementPointMark::getDescription);
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
    JCheckBox customCrosshairCursorCheckbox;

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
                    e -> uiEventListener.setSaveOptions(saveGifCheckbox.isSelected(), saveRightLeftCheckbox.isSelected())
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
        {
            customCrosshairCursorCheckbox = new JCheckBox("Use custom cursor when setting measurement marks");
            customCrosshairCursorCheckbox.setSelected(true);
            customCrosshairCursorCheckbox.addActionListener(
                    e -> uiEventListener.setUseCustomCrosshairCursor(customCrosshairCursorCheckbox.isSelected())
            );
            customCrosshairCursorCheckbox.setToolTipText("You can use either the standard thick cross-hair cursor or a custom one");
            this.add(customCrosshairCursorCheckbox);
        }

    }
    void setControls (BehavioralOptions bo) {
        saveGifCheckbox.setSelected(bo.saveGif);
        saveRightLeftCheckbox.setSelected(bo.saveLeftRightImages);
        customCrosshairCursorCheckbox.setSelected(bo.useCustomCrosshairCursor);
    }
    void showDialogIn(JFrame mainFrame) {
        JOptionPane.showMessageDialog(mainFrame, this,"Settings", JOptionPane.PLAIN_MESSAGE);
    }

}

class CustomCursorMaker {
    static final int DESIRED_WIDTH = 31;
    static final int DESIRED_HEIGHT = 31;
    static final int MIN_WIDTH = 21;
    static final int MIN_HEIGHT = 21;

    static BufferedImage makeCrossHairImage(final int width, final int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        final int x0 = width / 2;
        final int y0 = height / 2;
        int c = 6;
        int d = c; // + 1
        int b = c+4; // + 1
        g.setColor(Color.WHITE);
        g.drawLine(x0 + d, y0 + 1, width, y0 + 1);
        g.drawLine(x0 - b, y0 + 1, 0, y0 + 1);
        g.drawLine(x0 + b, y0 - 1, width, y0 - 1);
        g.drawLine(x0 - d, y0 - 1, 0, y0 - 1);
        g.drawLine(x0 + 1, y0 + b, x0 + 1, height);
        g.drawLine(x0 + 1, y0 - d, x0 + 1, 0);
        g.drawLine(x0 - 1, y0 + d, x0 - 1, height);
        g.drawLine(x0 - 1, y0 - b, x0 - 1, 0);
        g.drawOval(0, 0, width - 1, height - 1);
        g.setColor(Color.BLACK);
        g.drawLine(x0 + c, y0, width, y0);
        g.drawLine(x0 - c, y0, 0, y0);
        g.drawLine(x0, y0 + c, x0, height);
        g.drawLine(x0, y0 - c, x0, 0);
        g.drawOval(1, 1, width - 3, height - 3);
        g.dispose();
        return bi;
    }
    static Cursor getCrosshairCursor(BehavioralOptions behavioralOptions) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dim = toolkit.getBestCursorSize(DESIRED_WIDTH, DESIRED_HEIGHT);
        if (behavioralOptions.useCustomCrosshairCursor && dim.getWidth() >= MIN_WIDTH && dim.getHeight() >= MIN_HEIGHT) {
            int width = (int) dim.getWidth();
            if (width % 2 == 0) {
                width--;
            }
            int height = (int) dim.getHeight();
            if (height % 2 == 0) {
                height--;
            }
            try {
                Cursor cursor = toolkit.createCustomCursor(makeCrossHairImage(width, height), new Point(width/2+1, height/2+1), "custom-cross-hair");
                return cursor;
            } catch (Throwable t) {
                System.err.println("cannot create custom cursor");
                t.printStackTrace();
            }
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }
}

class DebayerBicubic {
    abstract static class BasePointSet<T> {
        int x,y;
        BufferedImage bi;
        int w, h;

        int x(int dx) {
            int xx = x+dx;
            while (xx<0) { xx += 2; }
            while (xx >= w) { xx -= 2; }
            return xx;
        }
        int y(int dy) {
            int yy = y+dy;
            while (yy<0) { yy += 2; }
            while (yy >= h) { yy -= 2; }
            return yy;
        }

        public T with(BufferedImage bi) {
            this.bi = bi;
            w = bi.getWidth();
            h = bi.getHeight();
            return (T) this;
        }
        public T at(int x, int y) {
            this.x = x;
            this.y = y;
            return (T) this;
        }

        public abstract int q0();
        public abstract int p1();
        public abstract int q1();
        public abstract int p2();
        public abstract int q2();

        public int interpolate() {
            return (
                    ((p1()+p2())<<2) + (q1()<<1) - q0() - q2()
                   ) >> 3;
        }
    }
    static class HorizPointSet extends BasePointSet<HorizPointSet> {
        @Override
        public int q0() {
            return getC(bi, x(-2), y(0));
        }
        @Override
        public int p1() {
            return getC(bi, x(-1), y(0));
        }
        @Override
        public int q1() {
            return getC(bi, x(0), y(0));
        }
        @Override
        public int p2() {
            return getC(bi, x(1), y(0));
        }
        @Override
        public int q2() {
            return getC(bi, x(2), y(0));
        }
    }
    static class VertPointSet extends BasePointSet<VertPointSet> {
        @Override
        public int q0() {
            return getC(bi, x(0), y(-2));
        }
        @Override
        public int p1() {
            return getC(bi, x(0), y(-1));
        }
        @Override
        public int q1() {
            return getC(bi, x(0), y(0));
        }
        @Override
        public int p2() {
            return getC(bi, x(0), y(1));
        }
        @Override
        public int q2() {
            return getC(bi, x(0), y(2));
        }
    }
    static class TwoDPointSet extends BasePointSet<TwoDPointSet> {
        HorizPointSet above = new HorizPointSet();
        HorizPointSet below = new HorizPointSet();

        @Override
        public TwoDPointSet with(BufferedImage bi) {
            above.with(bi);
            below.with(bi);
            return super.with(bi);
        }
        @Override
        public TwoDPointSet at(int x, int y) {
            above.at(x,y-1);
            below.at(x,y+1);
            return super.at(x, y);
        }
        @Override
        public int q0() {
            return getC(bi, x(0), y(-2));
        }
        @Override
        public int p1() {
            return above.interpolate();
        }
        @Override
        public int q1() {
            return getC(bi, x(0), y(0));
        }
        @Override
        public int p2() {
            return below.interpolate();
        }
        @Override
        public int q2() {
            return getC(bi, x(0), y(2));
        }
    }
    static BufferedImage debayer_bicubic(BufferedImage orig) {
        if (ImageAndPath.isDummyImage(orig)) {
            return orig;
        }
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        System.out.println("debayer_bicubic " + WIDTH + "x" + HEIGHT);
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var hps = new HorizPointSet().with(orig);
        var vps = new VertPointSet().with(orig);
        var tps = new TwoDPointSet().with(orig);
        for (int j=0; j<HEIGHT; j++) {
            for (int i=0; i<WIDTH; i++) {
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
                        g = hps.at(i,j).interpolate();
                        b = tps.at(i,j).interpolate();
                    } break;
                    case 1: { // Gr
                        r = hps.at(i,j).interpolate();
                        g = getC(orig, i, j);
                        b = vps.at(i,j).interpolate();
                    } break;
                    case 2: { // Gb
                        r = vps.at(i,j).interpolate();
                        g = getC(orig, i, j);
                        b = hps.at(i,j).interpolate();
                    } break;
                    case 3: { // B
                        r = tps.at(i,j).interpolate();
                        g = hps.at(i,j).interpolate();
                        b = getC(orig, i, j);
                    } break;
                    default: // stupid Java, this is impossible! type is 0..3!
                        r=g=b=0;
                }
                res.setRGB(i,j,rgb(r,g,b));
            }
        }
        return res;
    }
    static int getC(BufferedImage bi, int x, int y) {
        int res = 0;
        if (x >= 0 && y >= 0 && x < bi.getWidth() && y < bi.getHeight()) {
            res = b(bi.getRGB(x,y));
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
    static int c(int c) {
        return Math.max(0, Math.min(255, c));
    }
    static int rgb(int r, int g, int b) {
        return (c(r) << 16) | (c(g) << 8) | c(b);
    }
}

class SuccessFailureCounter {
    ConcurrentHashMap<String, Pair<Integer>> counts = new ConcurrentHashMap<>();
    static final Pair<Integer> ONE_SUCCESS = new Pair<>(1, 0);
    static final Pair<Integer> ONE_FAILURE = new Pair<>(0, 1);

    void countSuccess() {
        String thisThread = Thread.currentThread().toString();
        counts.merge(thisThread, ONE_SUCCESS, (v,unused) -> new Pair<>(v.first+1, v.second));
    }
    void countFailure() {
        String thisThread = Thread.currentThread().toString();
        counts.merge(thisThread, ONE_FAILURE, (v,unused) -> new Pair<>(v.first, v.second+1));
    }
    @Override
    public String toString() {
        var treeMap = new TreeMap<>(counts);
        String res = "{\n";
        for (var entry : treeMap.entrySet()) {
            res += " " + entry.getKey() + " -> " +entry.getValue().toString() + "\n";
        }
        res += "}\n";
        return res;
    }
}

class JustDialog {
    static void showMessageDialog(JFrame parent, JPanel message, String title, int messageTypeIgnored) {
        JDialog dialog = new JDialog(parent, title, true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(message);

        panel.add(Box.createRigidArea(new Dimension(5, 10)));

        JButton closeButton = new JButton("OK");
        closeButton.setMargin(new Insets(2, 8, 2, 8));
        closeButton.addActionListener(e -> {
            dialog.dispose();
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        buttonPanel.add(closeButton, new GridBagConstraints());
        panel.add(buttonPanel);

        JPanel contentPane = (JPanel) dialog.getContentPane();
        InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = contentPane.getActionMap();
        KeyStroke escKs = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        inputMap.put(escKs, "__esc");
        actionMap.put("__esc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                dialog.dispose();
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        closeButton.requestFocus();
        dialog.setVisible(true);
    }
}

class ImageMerger
{
    static BufferedImage assembleBigImageFromImagesLike(String fname0, Function<String, BufferedImage> imageReader) {
        List<String> smallImageNameList = ImageMerger.getSmallImageNames(fname0);
        System.out.println(smallImageNameList);
        var idl = filterOutUniqueSizes(getSmallImages(smallImageNameList, imageReader));
        final int rowSize = idl.size() == 4 ? 2
                          : idl.size() == 9 ? 3 : 4;

        var seq = findSequence(idl, rowSize);

        System.out.println("found sequence: "+seq);
        BufferedImage bigImage = ImageMerger.mergeImages(idl, seq, rowSize);
        System.out.println("images merged: "+seq);
        try {
            idl.forEach(d -> System.out.println(d + " " + smallImageNameList.get(d.id - 1)));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return bigImage;
    }
    static List<ImageDescriptor> filterOutUniqueSizes(List<ImageDescriptor> list) {
        Map<Integer, Integer> widths = new HashMap<>();
        Map<Integer, Integer> heights = new HashMap<>();
        list.forEach(descr -> {
            widths.merge(descr.width, 1, Integer::sum);
            heights.merge(descr.height, 1, Integer::sum);
        });
        var res = list.stream()
                .filter(descr -> widths.get(descr.width) > 1)
                .filter(descr -> heights.get(descr.height) > 1)
                .collect(Collectors.toList());
        return res;
    }
    static IdSequence findSequence(List<ImageDescriptor> idl, int rowSize) {
        List<ImageDescriptor> imageDescriptorsById = idl.stream().collect(Collector.of(
                () -> new ArrayList<ImageDescriptor>(idl.size()+1),
                (a,x) -> listPut(a, x.id, x),
                (a,b) -> {b.stream().filter(Objects::nonNull).forEach(x -> listPut(a, x.id, x)); return a;},
                Collections::unmodifiableList
        ));
        PriorityQueue<IdSequence> queue = new PriorityQueue<>(Comparator.comparing(IdSequence::getDist));
        int wSmall = idl.stream().map(d -> d.width).min(Integer::compare).get();
        int wLarge = idl.stream().map(d -> d.width).max(Integer::compare).get();
        List<Integer> corners = idl.stream().filter(d -> d.width == wSmall /*&& d.height == HSmall*/).map(d -> d.id).collect(Collectors.toList());
        System.out.println("corners="+corners);
        corners.forEach(id -> {
            IdSequence list = IdSequence.ofJust(id, minDistToNext(imageDescriptorsById.get(id), 0, idl.size(), rowSize));
            queue.add(list);
        });
        System.out.println("idl="+idl);
//        System.out.println("queue="+queue);
        int _n_iter_ = 0;
        while (queue.peek().size() < idl.size()) {
//            System.out.println("queue="+queue);
            var oldSequence = queue.poll();
//            System.out.println("took "+oldSequence);
            double oldDist = oldSequence.dist
                             - minDistToNext(imageDescriptorsById.get(oldSequence.getLast()), oldSequence.size() - 1, idl.size(), rowSize);
            int newPos = oldSequence.size();
            int left = newPos - 1;
            int above = newPos - rowSize;
            int w = widthFor(newPos, wSmall, wLarge, rowSize);
//            int h = heightFor(newPos, idl.size());
            idl.stream()
                    .filter(d -> d.width == w /*&& d.height == h*/)
                    .filter(d -> !oldSequence.contains(d.id))
                    .forEach(d -> {
                        double addedDist = 0.;
                        if (newPos / rowSize == left / rowSize) { // same row
                            addedDist += imageDescriptorsById.get(oldSequence.get(left)).distToOtherLeft(d);
                        }
                        if (above >= 0) {
                            addedDist += imageDescriptorsById.get(oldSequence.get(above)).distToOtherTop(d);
                        }
                        if (Double.isFinite(addedDist)) {
                            addedDist += minDistToNext(d, oldSequence.size(), idl.size(), rowSize);
                            IdSequence newSequence = oldSequence.with(d.id, oldDist + addedDist);
                            queue.add(newSequence);
                        }
                    });
            _n_iter_++;
        }
        System.out.println("iterations: "+_n_iter_);
        System.out.println("queue size="+queue.size());
        return queue.peek();
    }
    static double minDistToNext(ImageDescriptor descriptor, int pos, int M, int rowSize) {
        int col = pos%rowSize;
        int row = pos/rowSize;
        double res = 0.;
        if (col != rowSize-1) {
            res += descriptor.minDistToOtherLeft;
        }
        if (row != M/rowSize-1) {
            res += descriptor.minDistToOtherTop;
        }
        return res;
    }
    static int widthFor(int i, int wSmall, int wLarge, int rowSize) {
        return i%rowSize==0 || i%rowSize==rowSize-1 ? wSmall : wLarge;
    }

    static BufferedImage mergeImages(List<ImageDescriptor> idl, IdSequence seq, int rowSize) {
        Map<Integer, ImageDescriptor> imageDescriptorsById = idl.stream().collect(Collectors.toMap(descriptor -> descriptor.id, Function.identity()));
        var xOff = new ArrayList<Integer>();
        {
            xOff.add(0);
            for (int i = 0, M = rowSize; i < M; i++) {
                xOff.add(lastOf(xOff) + imageDescriptorsById.get(seq.get(i)).width - (i == 0 || i == rowSize-1 ? 8 : 16));
            }
        }
        var yOff = new ArrayList<Integer>();
        {
            yOff.add(0);
            for (int i = 0, M = seq.size(); i < M; i += rowSize) {
                yOff.add(lastOf(yOff) + imageDescriptorsById.get(seq.get(i)).height - (i == 0 || i/rowSize == M / rowSize-1 ? 8 : 16));
            }
        }
        BufferedImage bigImage = new BufferedImage(lastOf(xOff), lastOf(yOff), BufferedImage.TYPE_INT_RGB);
        for (int i=0, M=seq.size(); i<M; i++) {
            var d = imageDescriptorsById.get(seq.get(i));
            int row = i/rowSize;
            int col = i%rowSize;
            copyToFrom(
                    bigImage, xOff.get(col), yOff.get(row),
                    d.img,
                    col == 0 ? 0 : 8,
                    row == 0 ? 0 : 8,
                    d.width - (col == 0 || col == rowSize-1 ? 8 : 16),
                    d.height - (row == 0 || row == M/rowSize-1 ? 8 : 16)
            );
        }
        return bigImage;
    }

    static BufferedImage copyToFrom(BufferedImage dst, int offx, int offy, BufferedImage src, int fromX, int fromY, int width, int height) {
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                int rgb = src.getRGB(x+fromX,y+fromY);
                dst.setRGB(offx+x, offy+y, rgb);
            }
        }
        return dst;
    }
    static List<ImageMerger.ImageDescriptor> getSmallImages(List<String> fileList, Function<String, BufferedImage> imageReader) {
        List<ImageMerger.ImageDescriptor> idl = new ArrayList<>();
        for (var path : fileList) {
            try {
                int nn = getFragmentNumber(path);
                var img = imageReader.apply(path);
                if (img != null) {
                    var imgd = new ImageMerger.ImageDescriptor().init(nn, img);
                    idl.add(imgd);
                }
//            } catch (NumberFormatException e) {
//                System.out.println("ignoring "+path);
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.out.println("ignoring "+path);
            }
        }
        idl.forEach(descriptor -> descriptor.init2(idl));
        return idl;
    }

    static List<String> getSmallImageNames(String fname) {
        var res = new ArrayList<String>();
        int last, beforeLast;
        if (!((last = fname.lastIndexOf('_')) > 0 && (beforeLast = fname.lastIndexOf('_', last-1)) > 0)) {
            throw new NumberFormatException("number not found in string: \""+fname+"\"");
        }
        int fnum = Integer.parseInt(fname.substring(beforeLast + 1, last));
        if (!(0 < fnum && fnum <= 16)) {
            throw new IllegalArgumentException("bad fragment number: outside range [1..16]");
        }
        for (int i=1; i<= 16; i++) {
            res.add(String.format("%s%02d%s",fname.substring(0,beforeLast+1), i, fname.substring(last)));
        }
        return res;
    }
    static int getFragmentNumber(String fname) {
        int last, beforeLast;
        if ((last = fname.lastIndexOf('_')) > 0 && (beforeLast = fname.lastIndexOf('_', last-1)) > 0)
            return Integer.parseInt(fname.substring(beforeLast+1, last));
        throw new NumberFormatException("number not found in string: \""+fname+"\"");
    }
    static String getBigFileName(String fname) {
        int last, beforeLast;
        return (last = fname.lastIndexOf('_')) > 0 && (beforeLast = fname.lastIndexOf('_', last-1)) > 0
                ? fname.substring(0, beforeLast+1) + "yy" + fname.substring(last)
                : fname + ".yy";
    }
    private static<T> T lastOf(List<T> list) {
        return list.get(list.size() - 1);
    }
    static<T> void listPut(List<T> list, int i, T x) {
        if (list.size() > i) {
            list.set(i, x);
        } else {
            while (list.size() < i) {
                list.add(null);
            }
            list.add(x);
        }
    }
    static class Side {
        int[] points;
        int[] delta;

        public Side(int[] points, int[] delta) {
            this.points = points;
            this.delta = delta;
        }
    }
    static class SideComparator {
        static final int D = 8;
        static Side getLine(IntSupplier getSize, IntUnaryOperator getRgb) {
            int size = getSize.getAsInt();
            int[] res = new int[3*(size - 2*D)];
            for (int i=D, ii=0, upb = size-D; i<upb; i++) {
                int rgb = getRgb.applyAsInt(i);
                res[ii++] = (rgb & 0xff0000) >> 16;
                res[ii++] = (rgb & 0xff00) >> 8;
                res[ii++] = (rgb & 0xff);
            }
            int[] min = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
            int[] max = new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (int i=0, m=res.length; i<m; i++) {
                min[i%3] = Math.min(min[i%3], res[i]);
                max[i%3] = Math.max(max[i%3], res[i]);
            }
            for (int i=0, m=res.length; i<m; i++) {
                res[i] = (int) Math.round((res[i] - min[i%3])*255./(max[i%3] - min[i%3]));
            }
            int[] delta = new int[3];
            for (int i=0; i<3; i++) {
                delta[i] = (int) Math.ceil(255./(max[i] - min[i]));
            }
            return new Side(res, delta);
        }
        static Side getTopSide(BufferedImage img) {
            int Y = D;
            return getLine(img::getWidth, i -> img.getRGB(i,Y));
        }
        static Side getBottomSide(BufferedImage img) {
            int Y = img.getHeight() - D;
            return getLine(img::getWidth, i -> img.getRGB(i,Y));
        }
        static Side getLeftSide(BufferedImage img) {
            int X = D;
            return getLine(img::getHeight, i -> img.getRGB(X, i));
        }
        static Side getRightSide(BufferedImage img) {
            int X = img.getWidth() - D;
            return getLine(img::getHeight, i -> img.getRGB(X, i));
        }
        static double distBetweenSides(Side a, Side b) {
            if (a.points.length != b.points.length) {
                return Double.POSITIVE_INFINITY;
            }
            int length = a.points.length;
            long s = 0;
            for (int i=0; i<length; i++) {
                s += sqr(Math.max(0, Math.abs(a.points[i] - b.points[i]) - 2*Math.max(a.delta[i%3], b.delta[i%3])));
            }
            return Math.sqrt(s / (double) length);
        }
        static int sqr(int x) {
            return x*x;
        }
    }
    static class IdSequence {
        final byte[] ids;
        final double dist;
        private IdSequence(byte[] ids, double dist) {
            this.ids = ids;
            this.dist = dist;
        }
        public static IdSequence ofJust(int id, double dist) {
            return new IdSequence(new byte[] {(byte)id}, dist);
        }
        public IdSequence with(int id, double dist) {
            byte[] bytes = Arrays.copyOf(ids, ids.length+1);
            bytes[ids.length] = (byte)id;
            return new IdSequence(bytes, dist);
        }
        public double getDist() {
            return dist;
        }
        public int size() {
            return ids.length;
        }
        public int get(int i) {
            return ids[i];
        }
        public int getLast() {
            return ids[ids.length-1];
        }
        public boolean contains(int id) {
            for (int i=0, M=ids.length; i<M; i++) {
                if (ids[i] == id) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IdSequence that = (IdSequence) o;
            return Arrays.equals(ids, that.ids);
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(ids);
        }
        @Override
        public String toString() {
            return Arrays.toString(ids) + ":" + String.format("%.3f",dist);
        }
    }
    static class ImageDescriptor {
        int id;
        int width, height;
        Side sideT, sideB, sideL, sideR;
        double minDistToOtherTop, minDistToOtherLeft;
        BufferedImage img;
        Map<Integer, Double> fromBtoOtherT = new HashMap<>();
        Map<Integer, Double> fromLtoOtherR = new HashMap<>();

        ImageDescriptor init(int id, BufferedImage img) {
            this.id = id;
            this.img = img;
            width = img.getWidth();
            height = img.getHeight();
            sideT = SideComparator.getTopSide(img);
            sideB = SideComparator.getBottomSide(img);
            sideL = SideComparator.getLeftSide(img);
            sideR = SideComparator.getRightSide(img);
            return this;
        }
        ImageDescriptor init2(Collection<ImageDescriptor> others) {
            minDistToOtherTop = others.stream().filter(other -> this != other).map(this::distToOtherTop).min(Double::compareTo).get();
            minDistToOtherLeft = others.stream().filter(other -> this != other).map(this::distToOtherLeft).min(Double::compareTo).get();
            return this;
        }
        double distToOtherTop(ImageDescriptor other) {
            return fromBtoOtherT.computeIfAbsent(other.id, unused -> SideComparator.distBetweenSides(this.sideB, other.sideT));
        }
        double distToOtherLeft(ImageDescriptor other) {
            return fromLtoOtherR.computeIfAbsent(other.id, unused -> SideComparator.distBetweenSides(this.sideR, other.sideL));
        }

        @Override
        public String toString() {
            return "ImageDescriptor{" +
                   "id=" + id +
                   " " + width +
                   "x" + height +
                   '}';
        }
    }
}
class ComboBoxWithTooltips<T> extends JComboBox<T> {
    final ComboboxToolTipRenderer toolTipRenderer;

    public ComboBoxWithTooltips(T[] items, Function<T,String> getTooltip) {
        super(items);
        this.toolTipRenderer = new ComboboxToolTipRenderer();
        this.setRenderer(toolTipRenderer);
        toolTipRenderer.setTooltips(
                Arrays.stream(items)
                        .map(getTooltip)
                        .collect(Collectors.toList())
        );
    }

    class ComboboxToolTipRenderer extends DefaultListCellRenderer {
        //https://stackoverflow.com/a/4480209/755804
        List<String> tooltips;

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

            if (-1 < index && null != value && null != tooltips) {
                list.setToolTipText(tooltips.get(index));
            }
            return comp;
        }

        public void setTooltips(List<String> tooltips) {
            this.tooltips = tooltips;
        }
    }
}
