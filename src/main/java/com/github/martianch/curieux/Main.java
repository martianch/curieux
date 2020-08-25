//#!/usr/bin/java --source 11
// Note: the above shebang can work only if the file extension IS NOT .java

package com.github.martianch.curieux;

/*
Curious: an X3D Viewer
Designed to view the Curiosity Rover images from Mars in X3D, but can be used to view any stereo pairs (both LR and X3D).
Opens images from Internet or local drive, supports drag-and-drop, for example, you can drag-n-drop the red DOWNLOAD
button from the raw images index on the NASA site.
*/


import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.awt.Image.SCALE_SMOOTH;

/** The app runner class */
public class Main {

    public static final String NASA_RAW_IMAGES_URL =
        "https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+desc%2C+date_taken+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc&per_page=100&page=0&mission=msl";

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
        var lrn = new LRNavigator();
        var uic = new UiController(xv, lrn, rd0, dp);
        javax.swing.SwingUtilities.invokeLater(
                () -> uic.createAndShowViews()
        );
        paths = uic.unThumbnailIfNecessary(paths);
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
    void xOffsetChanged(int newXOff);
    void yOffsetChanged(int newYOff);
    void swapImages();
    void dndImport(String s, boolean isRight, OneOrBothPanes oneOrBoth);
    void dndSingleToBothChanged(boolean newValue);
    void unthumbnailChanged(boolean newValue);
    void copyUrl(boolean isRight);
    void copyUrls();
    void loadMatchOfOther(boolean isRight);
    void reload(boolean isRight);
    void gotoImage(GoToImageOptions goToImageOptions, boolean isRight, Optional<Integer> sol);
    void newWindow();
    void setShowUrls(boolean visible);
    void resetToDefaults();
    void saveScreenshot();
    void navigate(boolean isRight, boolean isLeft, boolean forwardInTime, int byOneOrTwo);
}

enum GoToImageOptions {
    CURIOSITY_FIRST_OF_SOL
}

class DisplayParameters {
    double zoom, zoomL, zoomR;
    int offsetX, offsetY;
    double angle, angleR, angleL;
    DebayerMode debayerL, debayerR;

    public DisplayParameters() {
        setDefaults();
    }
    void setDefaults() {
        zoom = zoomL = zoomR = 1.;
        offsetX = offsetY = 0;
        angle = angleL = angleR = 0.;
        debayerL = debayerR = DebayerMode.AUTO3;
    }
    private DisplayParameters(double zoom, double zoomL, double zoomR, int offsetX, int offsetY, double angle, double angleL, double angleR, DebayerMode debayerL, DebayerMode debayerR) {
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
    }
//    public DisplayParameters copy() {
//        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY);
//    }
    public DisplayParameters swapped() {
        return new DisplayParameters(zoom, zoomR, zoomL, -offsetX, -offsetY, angle, angleR, angleL, debayerR, debayerL);
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
        } else if(FileLocations.isUrl(path)) {
            System.out.println("downloading "+path+ " ...");
            try {
                res = ImageIO.read(new URL(path));
                res.getWidth(); // throw an exception if null
                System.out.println("downloaded "+path);
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
class UiController implements UiEventListener {
    X3DViewer x3dViewer;
    DisplayParameters displayParameters;
    RawData rawData;
    LRNavigator lrNavigator;
    boolean dndOneToBoth = true;
    boolean unthumbnail = true;
    volatile long lastLoadTimestampL;
    volatile long lastLoadTimestampR;
    public UiController(X3DViewer xv, LRNavigator lrn, RawData rd, DisplayParameters dp) {
        x3dViewer = xv;
        displayParameters = dp;
        rawData = rd;
        lrNavigator = lrn;
    }
    @Override
    public void zoomChanged(double newZoom) {
        displayParameters.zoom = newZoom;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void lZoomChanged(double newZoom) {
        displayParameters.zoomL = newZoom;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void rZoomChanged(double newZoom) {
        displayParameters.zoomR = newZoom;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void angleChanged(double newAngle) {
        displayParameters.angle = newAngle;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void lAngleChanged(double newLAngle) {
        displayParameters.angleL = newLAngle;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void rAngleChanged(double newRAngle) {
        displayParameters.angleR = newRAngle;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void lDebayerModeChanged(DebayerMode newLDebayerMode) {
        displayParameters.debayerL = newLDebayerMode;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void rDebayerModeChanged(DebayerMode newRDebayerMode) {
        displayParameters.debayerR = newRDebayerMode;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void xOffsetChanged(int newXOff) {
        displayParameters.offsetX = newXOff;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void yOffsetChanged(int newYOff) {
        displayParameters.offsetY = newYOff;
        x3dViewer.updateViews(rawData, displayParameters);
    }
    @Override
    public void swapImages() {
        System.out.println("swapImages "+Thread.currentThread());
        x3dViewer.updateViews(rawData = rawData.swapped(), displayParameters = displayParameters.swapped());
        x3dViewer.updateControls(displayParameters);
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
    public void reload(boolean isRight) {
        var paths = Arrays.asList(rawData.left.pathToLoad, rawData.right.pathToLoad);
        var uPaths = unThumbnailIfNecessary(paths);
        showInProgressViewsAndThen(uPaths,
                () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
    }

    @Override
    public void gotoImage(GoToImageOptions goToImageOptions, boolean isRight, Optional<Integer> sol) {
        sol.ifPresent(nSol -> {
            // TODO: implement
            System.out.println("gotoImage: "+goToImageOptions+" r:"+isRight+" "+nSol);
        });
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
        displayParameters.setDefaults();
        x3dViewer.updateControls(displayParameters);
        x3dViewer.updateViews(rawData, displayParameters);
    }

    @Override
    public void saveScreenshot() {
        x3dViewer.screenshotSaver.takeAndSaveScreenshot(x3dViewer.frame,rawData);
    }

    @Override
    public void navigate(boolean isRight, boolean isLeft, boolean forwardInTime, int byOneOrTwo) {
        lrNavigator.navigate(this, isRight, isLeft, forwardInTime, byOneOrTwo, rawData.left.pathToLoad, rawData.right.pathToLoad);
    }

    public List<String> unThumbnailIfNecessary(List<String> urlsOrFiles) {
        if (unthumbnail) {
            return urlsOrFiles.stream().map(FileLocations::unThumbnail).collect(Collectors.toList());
        } else {
            return urlsOrFiles;
        }
    }
    public void createAndShowViews() {
        x3dViewer.createViews(rawData, displayParameters, this);
    }
    public void changeRawData(RawData newRawData) {
        x3dViewer.updateViews(rawData=newRawData, displayParameters);
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
                .supplyAsync(() -> ImageAndPath.imageIoReadNoExc(path1, path1))
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

class X3DViewer {
    JButton lblL;
    JButton lblR;
    JLabel urlL;
    JLabel urlR;
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

    public void updateControls(DisplayParameters dp) {
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
    }
    public void updateViews(RawData rd, DisplayParameters dp) {
        {
            ImageIcon iconL;
            ImageIcon iconR;
            {
                BufferedImage imgL = dp.debayerL.doAlgo(rd.left.image, () -> FileLocations.isBayered(rd.left.path), Debayer.debayering_methods);
                BufferedImage imgR = dp.debayerR.doAlgo(rd.right.image, () -> FileLocations.isBayered(rd.right.path), Debayer.debayering_methods);

                BufferedImage rotatedL = rotate(imgL, dp.angle + dp.angleL);
                BufferedImage rotatedR = rotate(imgR, dp.angle + dp.angleR);
                iconL = new ImageIcon(zoom(rotatedL, dp.zoom * dp.zoomL, rotatedR, dp.zoom * dp.zoomR, dp.offsetX, dp.offsetY));
                iconR = new ImageIcon(zoom(rotatedR, dp.zoom * dp.zoomR, rotatedL, dp.zoom * dp.zoomL, -dp.offsetX, -dp.offsetY));
            }
            lblL.setIcon(iconL);
            lblR.setIcon(iconR);
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
        }
    }
    public void createViews(RawData rd, DisplayParameters dp, UiEventListener uiEventListener)
    {
        lblL=new JButton();
        lblR=new JButton();
        frame=new JFrame();
        urlL=new JLabel("url1");
        urlR=new JLabel("url2");

        Font font = lblL.getFont();
        {
            String FONT_NAME = "Verdana";
            var fl = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            var exists = fl.contains(FONT_NAME);
            if (exists) {
                font = new Font(FONT_NAME, Font.ITALIC + Font.BOLD, font.getSize());
                urlL.setFont(font);
                urlR.setFont(font);
            }
        }
        {
            updateViews(rd,dp);
        }

        var menuLR = new JPopupMenu();
        {
            {
                JMenuItem miCopy = new JMenuItem("Copy URL");
                menuLR.add(miCopy);
                miCopy.addActionListener(e ->
                        uiEventListener.copyUrl(
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker()
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
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
                                OneOrBothPanes.JUST_THIS
                        ));
            }
            {
                JMenuItem miPaste2 = new JMenuItem("Paste & Go (Both Panes)");
                menuLR.add(miPaste2);
                miPaste2.addActionListener(e ->
                        doPaste(
                                uiEventListener,
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
                                OneOrBothPanes.BOTH_PANES
                        ));
            }
            {
                JMenuItem miLoadMatch = new JMenuItem("Load Match of the Other");
                menuLR.add(miLoadMatch);
                miLoadMatch.addActionListener(e ->
                        uiEventListener.loadMatchOfOther(
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker()
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
                JMenuItem miPrevImage = new JMenuItem("Go To Previous");
                menuLR.add(miPrevImage);
                miPrevImage.addActionListener(e ->
                        uiEventListener.navigate(
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
                                lblL == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
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
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
                                lblL == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
                                true,
                                1
                        )
                );
            }
            {
                String title = "Go To First of Curiosity Sol...";
                JMenuItem miGoTo = new JMenuItem(title);
                menuLR.add(miGoTo);
                miGoTo.addActionListener(e ->
                        uiEventListener.gotoImage(
                                GoToImageOptions.CURIOSITY_FIRST_OF_SOL,
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker(),
                                askForNumber(100, title)
                        ));
            }
            {
                JMenuItem miReload = new JMenuItem("Reload");
                menuLR.add(miReload);
                miReload.addActionListener(e ->
                        uiEventListener.reload(
                                lblR == ((JPopupMenu) ((JMenuItem) e.getSource()).getParent()).getInvoker()
                        ));
            }
            lblR.setComponentPopupMenu(menuLR);
            lblL.setComponentPopupMenu(menuLR);
        }
        JScrollPane compL=new JScrollPane(lblL);
        JScrollPane compR=new JScrollPane(lblR);
        {
            compL.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            compR.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            compL.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            compR.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            // synchronize them: make them share the same model
            compL.getHorizontalScrollBar().setModel(compR.getHorizontalScrollBar().getModel());
            compL.getVerticalScrollBar().setModel(compR.getVerticalScrollBar().getModel());
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
            gbl.setConstraints(compL, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbl.setConstraints(compR, gbc);
        }

        JCheckBox showUrlsCheckox;
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
                resetAllControlsButton.setToolTipText("Reset All Controls");
                statusPanel.add(resetAllControlsButton);
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
                        "For example, you may Drag-and-Drop raw image thumbnails "+
                        "<a href=\""+ Main.NASA_RAW_IMAGES_URL+"\">"+
                                "from the NASA site" +
                        "</a>" +
                        ".<br>" +
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
                        "<b>Alt B</b>: Toggle the \"drag-and-drop to both panes\" mode<br>" +
                        "<b>Ctrl U</b>: Swap the left and right images<br>" +
//                        "<br>"+
                        "<b>Ctrl N</b>: New (empty) window<br>" +
                        "<b>Ctrl S</b>: Save the stereo pair (saves a screenshot of this application plus <br>" +
                        "a file that ends with <i>.source</i> and contains the URLs of images in the stereo pair)<br>" +
                        "Note: it may be confusing but if you prefer the keyboard to the mouse,<br>" +
                        "in Java dialogs the Enter key selects the default action rather than<br>" +
                        "the currently selected one; please use Space to select the current action.<br>" +
                        "<b>F1</b>: this help<br>" +
                        "<br>"+
                        "Command line: arguments may be either file paths or URLs<br>" +
                        "Drag-and-Drop (DnD): opens one or two images;<br>" +
                        "if only one image was dropped, if the \"DnD BOTH\" <br>" +
                        "checkbox is checked, tries to also load<br>" +
                        "the corresponding right or left image.<br>" +
                        "If the \"DnD BOTH\" box is not checked, the dropped image/url<br>" +
                        "just replaces the image on which it was dropped.<br>" +
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
                DigitalZoomControl.loadIcon(bButton,null,"⇇"); // "<->" "<=>" "icons/swap12.png" ⇉ ⇇ ↠ ↞ ↢ ↣
                bButton.addActionListener(e -> uiEventListener.navigate(true, true, false, 2));
                bButton.setToolTipText("load two images taken earlier");
                statusPanel.add(bButton);
            }
            {
                JButton bButton = new JButton();
                DigitalZoomControl.loadIcon(bButton,null,"↢"); // "<->" "<=>" "icons/swap12.png" ⇉ ⇇ ↠ ↞ ↢ ↣
                bButton.addActionListener(e -> uiEventListener.navigate(true, true, false, 1));
                bButton.setToolTipText("load one image taken earlier");
                statusPanel.add(bButton);
            }
            {
                JButton fButton = new JButton();
                DigitalZoomControl.loadIcon(fButton,null,"↣"); // "<->" "<=>" "icons/swap12.png" ⇉ ⇇ ↠ ↞ ↢ ↣
                fButton.addActionListener(e -> uiEventListener.navigate(true, true, true, 1));
                fButton.setToolTipText("load one image taken later");
                statusPanel.add(fButton);
            }
            {
                JButton ffButton = new JButton();
                DigitalZoomControl.loadIcon(ffButton,null,"⇉"); // "<->" "<=>" "icons/swap12.png" ⇉ ⇇ ↠ ↞ ↢ ↣
                ffButton.addActionListener(e -> uiEventListener.navigate(true, true, true, 2));
                ffButton.setToolTipText("load two images taken later");
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
            {
                JCheckBox unThumbnailCheckox = new JCheckBox("Un-Thumbnail");
                unThumbnailCheckox.setSelected(true);
                unThumbnailCheckox.addActionListener(
                    e -> uiEventListener.unthumbnailChanged(unThumbnailCheckox.isSelected())
                );
                statusPanel2.add(unThumbnailCheckox);
            }
            {
                showUrlsCheckox = new JCheckBox("Show URLs");
                showUrlsCheckox.setSelected(true);
                showUrlsCheckox.addActionListener(
                        e -> uiEventListener.setShowUrls(showUrlsCheckox.isSelected())
                );
                statusPanel2.add(showUrlsCheckox);
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
            frame.setSize(1360,800);
            frame.add(compR);
            frame.add(compL);
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
        if (showUrlsCheckox.isSelected()) {
            addUrlViews(true, false);
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
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
                gbl.setConstraints(urlL, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 4;
                gbc.gridheight = 1;
                gbc.gridwidth = 2;
                gbl.setConstraints(urlR, gbc);
            }
            frame.add(urlL);
            frame.add(urlR);
        } else {
            frame.remove(urlL);
            frame.remove(urlR);
        }
        if (repaint) {
            frame.validate();
            frame.repaint();
        }
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

    static BufferedImage rotate(BufferedImage originalImage, double alphaDegrees) {
        if (ImageAndPath.isDummyImage(originalImage)) {
            return originalImage;
        }
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

        BufferedImageOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);

        return operation.filter(originalImage, null);
    }
    static BufferedImage zoom(BufferedImage originalImage, double zoomLevel, BufferedImage otherImage, double otherZoomLevel, int offX, int offY) {
        if (ImageAndPath.isDummyImage(originalImage) && ImageAndPath.isDummyImage(otherImage)) {
            return originalImage;
        }
        int newImageWidth = zoomedSize(originalImage.getWidth(), zoomLevel);
        int newImageHeight = zoomedSize(originalImage.getHeight(), zoomLevel);
        int otherImageWidth = zoomedSize(otherImage.getWidth(), otherZoomLevel);
        int otherImageHeight = zoomedSize(otherImage.getHeight(), otherZoomLevel);
        int thisCanvasWidth = Math.abs(mult(offX, zoomLevel)) + newImageWidth;
        int thisCanvasHeight = Math.abs(mult(offY, zoomLevel)) + newImageHeight;
        int otherCanvasWidth = Math.abs(mult(offX, otherZoomLevel)) + otherImageWidth;
        int otherCanvasHeight = Math.abs(mult(offY, otherZoomLevel)) + otherImageHeight;
        int canvasWidth = Math.max(thisCanvasWidth, otherCanvasWidth);
        int canvasHeight = Math.max(thisCanvasHeight, otherCanvasHeight);
        int centeringOffX = Math.max(0, otherCanvasWidth - thisCanvasWidth)/2;
        int centeringOffY = Math.max(0, otherCanvasHeight - thisCanvasHeight)/2;
        BufferedImage resizedImage = new BufferedImage(canvasWidth, canvasHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(
                originalImage,
                Math.max(0, centeringOffX + mult(offX, zoomLevel)),
                Math.max(0, centeringOffY + mult(offY, zoomLevel)),
                newImageWidth,
                newImageHeight,
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

}

enum OneOrBothPanes {JUST_THIS, BOTH_PANES, SEE_CHECKBOX};

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
}
class DebayerModeChooser extends JComboBox<DebayerMode> {
    static DebayerMode[] modes = DebayerMode.values();
    public DebayerModeChooser(Consumer<DebayerMode> valueListener) {
        super(modes);
        setValue(DebayerMode.AUTO3);
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
    DigitalZoomControl init(String labelText, int nColumns, TT valueWrapper0, Consumer<T> valueListener) {
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
                valueWrapper.decrement(1);
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            this.add(buttonMinus2);
        }
        {
            buttonMinus = new JButton();
            loadIcon(buttonMinus,"icons/minusa12.png","–");
            buttonMinus.addActionListener(e -> {
                valueWrapper.decrement(0);
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
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
                valueWrapper.increment(0);
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
            this.add(buttonPlus);
        }
        {
            buttonPlus2 = new JButton();
            loadIcon(buttonPlus2,"icons/plus12.png","++");
            buttonPlus2.addActionListener(e -> {
                valueWrapper.increment(1);
                setTextFieldFromValue();
                valueListener.accept(valueWrapper.getSafeValue());
            });
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
            this.add(buttonDefault);
        }
        return this;
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
//    DigitalZoomControl kbShortcut(int key, int modifiers) {
//        return this;
//    }

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
    }
}
class OffsetWrapper extends DigitalZoomControl.ValueWrapper<Integer> {
    int[] increments = {3,30,100};
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
}
class RotationAngleWrapper extends DigitalZoomControl.ValueWrapper<Double> {
    double[] increments = {0.1, 1.0, 2.0};
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
}
class ZoomFactorWrapper extends DigitalZoomControl.ValueWrapper<Double> {
    double[] increments = {0.1, 1.0, 2.0};
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
        final String thmSuffix = "-thm.jpg";
        final String brSuffix = "-br.jpg";
        final String imgSuffix = ".JPG";
        String unthumbnailed =
            replaceSuffix(thmSuffix, imgSuffix,
                replaceSuffix(brSuffix, imgSuffix,
                        urlOrFile
                )
            );
        if (isUrl(urlOrFile) || !isProblemWithFile(unthumbnailed)) {
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
        if (!orig.endsWith(oldSuffix)) {
            return orig;
        }
        String base = orig.substring(0, orig.length()-oldSuffix.length());
        return base + newSuffix;
    }
    static String getFileName(String urlOrPath) {
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
    static List<String> twoPaths(String path0) {
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
    static List<String> _twoPaths(String path0) {
        String fullPath1 = "", fullPath2 = "";
        var fullPath = Paths.get(path0);
        var dir = fullPath.getParent();
        if (dir == null) {
            dir = Paths.get(".");
        }
        var file = fullPath.getFileName().toString();
        if (isMarkedRL(file)) {
            StringBuilder sb = new StringBuilder(file);
            if (sb.charAt(1) == 'R') {
                sb.setCharAt(1, 'L');
                fullPath1 = path0;
                fullPath2 = Paths.get(dir.toString(),sb.toString()).toString();
            } else {
                sb.setCharAt(1, 'R');
                fullPath1 = Paths.get(dir.toString(),sb.toString()).toString();
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
        if(isMarkedRL(file1) && isMarkedRL(file2)) {
            if (isMarkedL(file1)) {
                return Arrays.asList(urlOrPath2, urlOrPath1);
            }
        }
        return Arrays.asList(urlOrPath1, urlOrPath2);
    }
    private static boolean isMarkedR(String file) {
        return file.startsWith("NRB") || file.startsWith("RRB") || file.startsWith("FRB")
            || file.startsWith("NRA") || file.startsWith("RRA") || file.startsWith("FRA");
    }
    private static boolean isMarkedL(String file) {
        return file.startsWith("NLB") || file.startsWith("RLB") || file.startsWith("FLB")
            || file.startsWith("NLA") || file.startsWith("RLA") || file.startsWith("FLA");
    }
    private static boolean isMarkedRL(String file) {
        return isMarkedR(file) || isMarkedL(file);
    }
    static boolean isUrl(String path) {
        String prefix = path.substring(0,Math.min(9, path.length())).toLowerCase();
        var res = prefix.startsWith("http:/") || prefix.startsWith("https:/") || prefix.startsWith("file:/");
        //System.out.println("prefix=["+prefix+"] => "+res+" "+Thread.currentThread());
        return res;
    }
    static boolean isBayered(String urlOrPath) {
        String fname = getFileName(urlOrPath);
        return fname.matches(".*\\d+M[RL]\\d+C00_DXXX.*");
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
        public void apply(File imgFile, File srcFile) throws Exception;
    }
    public void takeAndSaveScreenshot(JFrame frame, RawData rawData) {
        try {
            BufferedImage bi = ScreenshotSaver.getScreenshot(frame);
            showSaveDialog(
                    frame,
                    (imgFile, srcFile) -> {
                        String description = "Left, Right:\n" + rawData.left.path + "\n" + rawData.right.path + "\n";
                        ScreenshotSaver.writePng(imgFile, bi,
                            "Software", "Curious: X3D Viewer",
                            "Description", description);
                        ScreenshotSaver.writeText(srcFile, description);
                    }
            );
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }
    void showSaveDialog(JFrame frame, SaveAction howToSave) throws Exception {
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setSelectedFile(new File("stereo.png"));
//        File imgFile;
        while (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(frame)) {
            File imgFile = fileChooser.getSelectedFile();
            File srcFile = new File(imgFile.getAbsolutePath()+".source");
            if (!endsWithIgnoreCase(imgFile.getAbsolutePath(),".png")) {
                JOptionPane.showMessageDialog(frame, "File name must end with \"png\"");
            } else if (
                        (  !imgFile.exists()
                        || JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(frame, "File " + imgFile + " already exists. Choose a different name?", "Overwrite?", JOptionPane.YES_NO_OPTION)
                    ) && ( !srcFile.exists()
                        || JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(frame, "File " + srcFile + " already exists. Choose a different name?", "Overwrite?", JOptionPane.YES_NO_OPTION)
                    )
            ) {
                System.out.println("Saving to " + imgFile);
                howToSave.apply(imgFile, srcFile);
                break;
            }
        }
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
    public static BufferedImage getScreenshot(JFrame frame) throws AWTException {
        Robot robot = new Robot();
        Rectangle appRect = new Rectangle(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
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
                    int i = Integer.valueOf(index);
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
    LRNavigator navigate(UiController uiController, boolean isRight, boolean isLeft, boolean forwardInTime, int byOneOrTwo, String leftPath, String rightPath) {
        String newLeftPath = leftPath;
        String newRightPath = rightPath;
        System.out.println("---");
        System.out.println("L "+newLeftPath+"\nR "+newRightPath+"\n");
        if (isLeft) {
            left = newIfNotSuitable(left, leftPath);
            for (int i=0; i<byOneOrTwo; i++) {
                newLeftPath = (forwardInTime ? left.toNext() : left.toPrev()).getCurrentPath();
                System.out.println("L "+newLeftPath+"...");
            }
        }
        if (isRight) {
            right = newIfNotSuitable(right, rightPath);
            for (int i=0; i<byOneOrTwo; i++) {
                newRightPath = (forwardInTime ? right.toNext() : right.toPrev()).getCurrentPath();
                System.out.println("R "+newRightPath+"...");
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
        if (null == fileNavigator || needRemote != (fileNavigator instanceof RemoteFileNavigator)) {
            fileNavigator = FileNavigatorBase.makeNew(path);
        }
        return fileNavigator;
    }
}
interface FileNavigator<T> {
    FileNavigator<T> toNext();
    FileNavigator<T> toPrev();
    T getCurrentValue();
    String getCurrentKey();
    FileNavigator<T> setCurrentKey(String key);
    String toKey(T obj);
    String getPath(T t);
    default String getCurrentPath() { return getPath(getCurrentValue()); }
}
abstract class FileNavigatorBase implements FileNavigator<Map<String, Object>> {
    protected NavigableMap<String, Map<String, Object>> nmap = new TreeMap<>();
    protected String currentKey;
    int nToLoad=12;
    public static FileNavigatorBase makeNew(String path) {
        var needRemote = FileLocations.isUrl(path);
        FileNavigatorBase res = needRemote ? new RemoteFileNavigator() : new LocalFileNavigator();
        res._loadInitial(path);
        return res;
    }
    protected void moveWindow() {
        if (Objects.equals(currentKey, nmap.lastKey())) {
            _loadHigher(); _cleanupLower();
        }
        if (Objects.equals(currentKey, nmap.firstKey())) {
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
        moveWindow();
        if (currentKey == null) {
            currentKey = nmap.firstKey();
        } else {
            currentKey = nmap.higherKey(currentKey);
        }
        return this;
    }
    @Override
    public FileNavigator<Map<String, Object>> toPrev() {
        moveWindow();
        if (currentKey == null) {
            currentKey = nmap.lastKey();
        } else {
            currentKey = nmap.lowerKey(currentKey);
        }
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

class NasaReader {

    static String nasaEncode(String params) {
        return params.replaceAll(",", "%2C").replaceAll(":", "%3A").replaceAll(" ","+");
    }

    static String readUrl(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    static String makeRequest(String parameters) {
        return "https://mars.nasa.gov/api/v1/raw_image_items/?" +
                nasaEncode( parameters );
    }
    static Object dataStructureFromRequest(String parameters) throws IOException {
        String url = makeRequest(parameters);
        String json = NasaReader.readUrl(new URL(url));
        Object res = JsonDiy.jsonToDataStructure(json);
        return res;
    }
    static Object dataStructureFromImageId(String imageId) throws IOException {
        return dataStructureFromRequest(
                "order=date_taken asc" +
                        "&per_page=10" +
                        "&page=0" +
                        "&condition_1=" + imageId + ":imageid:eq" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromCuriositySolStarting(int curiositySol, int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=date_taken asc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=msl:mission" +
                        "&condition_2=" + curiositySol + ":sol:gte" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromDateStarting(String date, int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=date_taken asc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=" + date + ":date_taken:gte" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
    static Object dataStructureFromDateEnding(String date, int perPage) throws IOException {
        return dataStructureFromRequest(
                "order=date_taken desc" +
                        "&per_page=" + perPage +
                        "&page=0" +
                        "&condition_1=" + date + ":date_taken:lte" +
                        "&search=" +
                        "&extended=thumbnail::sample_type::noteq"
        );
    }
}
class RemoteFileNavigator extends FileNavigatorBase {
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


