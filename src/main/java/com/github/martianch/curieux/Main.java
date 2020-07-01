//#!/usr/bin/java --source 11
// Note: the above shebang can work only if the file extension IS NOT .java

package com.github.martianch.curieux;

/*
Curious: an X3D Viewer
Designed to view the Curiosity Rover images from Mars in X3D, but can be used to view any stereo pairs (both LR and X3D).
Opens images from Internet or local drive, supports drag-and-drop, for example, you can drag-n-drop the red DOWNLOAD
button from the raw images index on the NASA site.
*/


import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.awt.Image.SCALE_SMOOTH;

/** The app runner class */
public class Main {

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

        var rd0 = new RawData(ImageAndPath.IN_PROGRESS_PATH, ImageAndPath.IN_PROGRESS_PATH);
        var dp = new DisplayParameters();
        var xv = new X3DViewer();
        var uic = new UiController(xv, rd0, dp);
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
    void newWindow();
    void setShowUrls(boolean visible);
}

class DisplayParameters {
    double zoom, zoomL, zoomR;
    int offsetX, offsetY;
    double angle, angleR, angleL;
    DebayerMode debayerL, debayerR;

    public DisplayParameters() {
        zoom = zoomL = zoomR = 1.;
        offsetX = offsetY = 0;
        angle = angleL = angleR = 0.;
        debayerL = debayerR = DebayerMode.AUTO;
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

    public ImageAndPath(BufferedImage image, String path) {
        this.image = image;
        this.path = path;
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
    static ImageAndPath imageIoRead(String path) throws IOException {
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
        return new ImageAndPath(res, path);
    }

    public static boolean isSpecialPath(String path) {
        return NO_PATH.equals(path) || IN_PROGRESS_PATH.equals(path) || ERROR_PATH.equals(path);
    }

    static ImageAndPath imageIoReadNoExc(String path) {
        try {
            return imageIoRead(path);
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

    RawData(String pathL, String pathR) throws IOException {
        this(ImageAndPath.imageIoRead(pathL), ImageAndPath.imageIoRead(pathR));
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
    boolean dndOneToBoth = true;
    boolean unthumbnail = true;
    volatile long lastLoadTimestampL;
    volatile long lastLoadTimestampR;
    public UiController(X3DViewer xv, RawData rd, DisplayParameters dp) {
        x3dViewer = xv;
        displayParameters = dp;
        rawData = rd;
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
        showInProgressViewsAndThen(uPaths,
                () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
    }

    @Override
    public void newWindow() {
        ProcessForker.forkWrapped();
    }

    @Override
    public void setShowUrls(boolean visible) {
        x3dViewer.addUrlViews(visible, true);
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
    public void showInProgressViewsAndThen(List<String> paths, Runnable next) {
        try {
            ImageAndPath l = rawData.left.isPathEqual(paths.get(0))
                           ? rawData.left
                           : ImageAndPath.imageIoRead(ImageAndPath.IN_PROGRESS_PATH);
            ImageAndPath r = rawData.right.isPathEqual(paths.get(1))
                           ? rawData.right
                           : ImageAndPath.imageIoRead(ImageAndPath.IN_PROGRESS_PATH);
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
                .supplyAsync(() -> ImageAndPath.imageIoReadNoExc(path2))
                .exceptionally(t -> ImageAndPath.imageIoReadNoExc(""));
        CompletableFuture<ImageAndPath> futureImage1 =
                sameLeftPath
              ? CompletableFuture.completedFuture(rawData.left)
              : CompletableFuture
                .supplyAsync(() -> ImageAndPath.imageIoReadNoExc(path1))
                .exceptionally(t -> ImageAndPath.imageIoReadNoExc(""));
        ImageAndPath inProgress = ImageAndPath.imageIoReadNoExc(ImageAndPath.IN_PROGRESS_PATH);

        Runnable uiUpdateRunnableSyncL =
            () -> javax.swing.SwingUtilities.invokeLater(
                () -> {
                    if (lastLoadTimestampL == timestamp) {
                        this.changeRawData(
                            new RawData(futureImage1.getNow(inProgress),
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
                                        futureImage2.getNow(inProgress)
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
                BufferedImage imgL = dp.debayerL == DebayerMode.FORCE
                                  || dp.debayerL == DebayerMode.AUTO && FileLocations.isBayered(rd.left.path)
                                   ? Debayer.debayer(rd.left.image)
                                   : rd.left.image;
                BufferedImage imgR = dp.debayerR == DebayerMode.FORCE
                                  || dp.debayerR == DebayerMode.AUTO && FileLocations.isBayered(rd.right.path)
                                   ? Debayer.debayer(rd.right.image)
                                   : rd.right.image;
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

        JPanel statusPanel = new JPanel();
        JPanel statusPanel2 = new JPanel();
        {
            FlowLayout fl = new FlowLayout();
            statusPanel.setLayout(fl);

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
                statusPanel.add(swapButton);
            }
            {
                //statusPanel.add(new JLabel(" help:"));
                helpButton = new JButton();
                DigitalZoomControl.loadIcon(helpButton,"icons/helpc12.png","?");
                JLabel helpText = new JLabel(
                        "<html>" +
                        "<h1>Curious: X3D Viewer</h1>" +
                        "Use Drag-and-Drop or the right mouse click menu to open a file or URL.<br>" +
                        "For example, you may Drag-and-Drop raw image thumbnails from the NASA site.<br>" +
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
//                        "<br>"+
                        "<b>Ctrl N</b>: new (empty) window<br>" +
                        "<b>F1</b>: this help<br>" +
                        "<br>"+
                        "Command line: arguments may be either file paths or URLs<br>" +
                        "Drag-and-Drop (DnD): opens one or two images;<br>" +
                        "if only one image was dropped, if the \"DnD BOTH\" <br>" +
                        "checkbox is checked, tries to also load<br>" +
                        "the corresponding right or left image.<br>" +
                        "If the \"DnD BOTH\" box is not checked, the dropped image/url<br>" +
                        "just replaces the image on which it was dropped.<br>" +
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
                JCheckBox showUrlsCheckox = new JCheckBox("Show URLs");
                showUrlsCheckox.setSelected(false);
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
//        addUrlViews(true, false);
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

enum DebayerMode {NEVER, AUTO, FORCE}
class DebayerModeChooser extends JComboBox<DebayerMode> {
    static DebayerMode[] modes = DebayerMode.values();
    public DebayerModeChooser(Consumer<DebayerMode> valueListener) {
        super(modes);
        setValue(DebayerMode.AUTO);
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
                fullPath = url.getFile();
            } catch (MalformedURLException e) {
                // do nothing, go on assuming it's a file
            }
        }
        Path fileName = Paths.get(fullPath).getFileName();
        return fileName == null ? "" : fileName.toString();
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
    static BufferedImage debayer(BufferedImage orig) {
        int HEIGHT = orig.getHeight();
        int WIDTH = orig.getWidth();
        BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<WIDTH; i++) {
            for (int j=0; j<HEIGHT; j++) {
                int type = (i&1)*2 + (j&1); // RGGB
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