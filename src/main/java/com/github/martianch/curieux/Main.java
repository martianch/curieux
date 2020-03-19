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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
                paths = FileLocations.twoPaths(args[0]);
            }
            break;
            default:
            case 2: {
                paths = FileLocations.twoPaths(args[0], args[1]);
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
    void lAngleChanged(double newLAngle);
    void rAngleChanged(double newRAngle);
    void xOffsetChanged(int newXOff);
    void yOffsetChanged(int newYOff);
    void swapImages();
    void dndImport(String s, boolean isRight, OneOrBothPanes oneOrBoth);
    void dndSingleToBothChanged(boolean newValue);
    void unthumbnailChanged(boolean newValue);
    void copyUrl(boolean isRight);
    void copyUrls();
    void loadMatchOfOther(boolean isRight);
}

class DisplayParameters {
    double zoom, zoomL, zoomR;
    int offsetX, offsetY;
    double angleR, angleL;

    public DisplayParameters() {
        zoom = zoomL = zoomR = 1.;
        offsetX = offsetY = 0;
        angleL = angleR = 0.;
    }
    private DisplayParameters(double zoom, double zoomL, double zoomR, int offsetX, int offsetY) {
        this.zoom = zoom;
        this.zoomL = zoomL;
        this.zoomR = zoomR;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
//    public DisplayParameters copy() {
//        return new DisplayParameters(zoom, zoomL, zoomR, offsetX, offsetY);
//    }
    public DisplayParameters swapped() {
        return new DisplayParameters(zoom, zoomR, zoomL, -offsetX, -offsetY);
    }
}
class ImageAndPath {
    public static final String IN_PROGRESS_PATH = "..."; // pseudo-path, means "download in progress"
    public static final String NO_PATH = ""; // pseudo-path, means "no path"
    public static final int DUMMY_SIZE = 12;
    final BufferedImage image;
    final String path;

    public ImageAndPath(BufferedImage image, String path) {
        this.image = image;
        this.path = path;
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
                        : new Color(0, 0, 0);
            res = dummyImage(color);
        } else if(FileLocations.isUrl(path)) {
            System.out.println("downloading "+path+ IN_PROGRESS_PATH);
            try {
                res = ImageIO.read(new URL(path));
                res.getWidth(); // throw an exception if null
                System.out.println("downloaded "+path);
            } catch (Throwable t) {
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
        return NO_PATH.equals(path) || IN_PROGRESS_PATH.equals(path);
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
        showInProgressViewsAndThen(
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
        showInProgressViewsAndThen(
                () ->  updateRawDataAsync(uPaths.get(0), uPaths.get(1))
        );
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
    public void showInProgressViewsAndThen(Runnable next) {
        try {
            var rd0 = new RawData(ImageAndPath.IN_PROGRESS_PATH, ImageAndPath.IN_PROGRESS_PATH);
            javax.swing.SwingUtilities.invokeLater(
                    () -> {
                        changeRawData(rd0);
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
        CompletableFuture<ImageAndPath> futureImage2 = CompletableFuture
                .supplyAsync(() -> ImageAndPath.imageIoReadNoExc(path2))
                .exceptionally(t -> ImageAndPath.imageIoReadNoExc(""));
        CompletableFuture<ImageAndPath> futureImage1 = CompletableFuture
                .supplyAsync(() -> ImageAndPath.imageIoReadNoExc(path1))
                .exceptionally(t -> ImageAndPath.imageIoReadNoExc(""));
        ImageAndPath inProgress = ImageAndPath.imageIoReadNoExc(ImageAndPath.IN_PROGRESS_PATH);

        Runnable uiUpdateRunnableSync =
            () -> javax.swing.SwingUtilities.invokeLater(
                () -> this.changeRawData(
                        new RawData(futureImage1.getNow(inProgress),
                                    futureImage2.getNow(inProgress)
                        )
                )
            );
        futureImage1.thenRunAsync( uiUpdateRunnableSync );
        futureImage2.thenRunAsync( uiUpdateRunnableSync );
        // the two above ui updates are synchronized via the invokeLater() queue
        // the 2nd one, whichever it is, shows both images
    }
}

class X3DViewer {
    JButton lblL=new JButton();
    JButton lblR=new JButton();
    JFrame frame;
    DigitalZoomControl<Double, ZoomFactorWrapper> dcZoom;
    DigitalZoomControl<Double, ZoomFactorWrapper> dcZoomL;
    DigitalZoomControl<Double, ZoomFactorWrapper> dcZoomR;
    DigitalZoomControl<Double, RotationAngleWrapper> dcAngleL;
    DigitalZoomControl<Double, RotationAngleWrapper> dcAngleR;
    DigitalZoomControl<Integer, OffsetWrapper> dcOffX;
    DigitalZoomControl<Integer, OffsetWrapper> dcOffY;

    public void updateControls(DisplayParameters dp) {
        dcZoom.setValueAndText(dp.zoom);
        dcZoomL.setValueAndText(dp.zoomL);
        dcZoomR.setValueAndText(dp.zoomR);
        dcOffX.setValueAndText(dp.offsetX);
        dcOffY.setValueAndText(dp.offsetY);
        dcAngleL.setValueAndText(dp.angleL);
        dcAngleR.setValueAndText(dp.angleR);
    }
    public void updateViews(RawData rd, DisplayParameters dp) {
        {
            ImageIcon iconL;
            ImageIcon iconR;
            {
                BufferedImage imgL = rd.left.image;
                BufferedImage imgR = rd.right.image;
                BufferedImage rotatedL = rotate(imgL, dp.angleL);
                BufferedImage rotatedR = rotate(imgR, dp.angleR);
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
        }
    }
    public void createViews(RawData rd, DisplayParameters dp, UiEventListener uiEventListener)
    {
        lblL=new JButton();
        lblR=new JButton();
        frame=new JFrame();

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

            statusPanel2.add(dcAngleL = new DigitalZoomControl<Double, RotationAngleWrapper>().init("rotateL:",4, new RotationAngleWrapper(), d -> uiEventListener.lAngleChanged(d)));
            statusPanel2.add(dcAngleR = new DigitalZoomControl<Double, RotationAngleWrapper>().init("rotateR:",4, new RotationAngleWrapper(), d -> uiEventListener.rAngleChanged(d)));

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
                JButton helpButton = new JButton();
                DigitalZoomControl.loadIcon(helpButton,"icons/helpc12.png","?");
                String helpText =
                        "When either of the images has the input focus:\n\n" +
                        "LEFT, RIGHT, UP, DOWN: scroll both images\n" +
                        "Alt+I, Ctrl+'=': zoom in +10%\n" +
                        "Ctrl+I, Ctrl+Shift+'+': zoom in +100%\n" +
                        "Alt+O, Clrl+'-': zoom out -10%\n" +
                        "Ctrl+O, Ctrl+Shift+'_': zoom out -100%\n" +
                        "Shift+LEFT, Shift+RIGHT: change horizontal offset by 3\n" +
                        "Ctrl+LEFT, Ctrl+RIGHT: change horizontal offset by 30\n" +
                        "Shift+UP, Shift+DOWN: change vertical offset by 3\n" +
                        "Ctrl+UP, Ctrl+DOWN: change vertical offset by 30\n" +
                        "Alt+B Toggle the \"drag-and-drop to both panes\" mode\n" +
                        "\n"+
                        "Command line: arguments may be either file paths or URLs\n" +
                        "Drag-and-Drop (DnD): opens one or two images;\n" +
                        "if only one image was dropped, if the \"DnD BOTH\" \n" +
                        "checkbox is checked, tries to also load\n" +
                        "the corresponding right or left image.\n" +
                        "If the \"DnD BOTH\" box is not checked, the dropped image/url\n" +
                        "just replaces the image on which it was dropped.\n" +
                        "";
                helpButton.addActionListener(e -> {
                    JOptionPane.showMessageDialog(frame, helpText,
                            "help", JOptionPane.PLAIN_MESSAGE);
                });
                statusPanel2.add(helpButton);
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
            for (var lbl : Arrays.asList(lblL, lblR)) {
                lbl.getInputMap().put(KeyStroke.getKeyStroke("shift LEFT"), "xoffplus");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("shift RIGHT"), "xoffminus");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("shift UP"), "yoffplus");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("shift DOWN"), "yoffminus");
                lbl.getActionMap().put("xoffplus", toAction(e->dcOffX.buttonPlus.doClick()));
                lbl.getActionMap().put("xoffminus", toAction(e->dcOffX.buttonMinus.doClick()));
                lbl.getActionMap().put("yoffplus", toAction(e->dcOffY.buttonPlus.doClick()));
                lbl.getActionMap().put("yoffminus", toAction(e->dcOffY.buttonMinus.doClick()));
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl LEFT"), "xoffplus2");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl RIGHT"), "xoffminus2");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl UP"), "yoffplus2");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl DOWN"), "yoffminus2");
                lbl.getActionMap().put("xoffplus2", toAction(e->dcOffX.buttonPlus2.doClick()));
                lbl.getActionMap().put("xoffminus2", toAction(e->dcOffX.buttonMinus2.doClick()));
                lbl.getActionMap().put("yoffplus2", toAction(e->dcOffY.buttonPlus2.doClick()));
                lbl.getActionMap().put("yoffminus2", toAction(e->dcOffY.buttonMinus2.doClick()));
                lbl.getInputMap().put(KeyStroke.getKeyStroke("alt I"), "zoomin");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("alt O"), "zoomout");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl I"), "zoomin2");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl O"), "zoomout2");
//                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl PLUS"), "zoomin2");
//                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl shift PLUS"), "zoomin2");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl EQUALS"), "zoomin");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl shift EQUALS"), "zoomin2");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl MINUS"), "zoomout");
                lbl.getInputMap().put(KeyStroke.getKeyStroke("ctrl shift MINUS"), "zoomout2");
                lbl.getActionMap().put("zoomin", toAction(e->dcZoom.buttonPlus.doClick()));
                lbl.getActionMap().put("zoomout", toAction(e->dcZoom.buttonMinus.doClick()));
                lbl.getActionMap().put("zoomin2", toAction(e->dcZoom.buttonPlus2.doClick()));
                lbl.getActionMap().put("zoomout2", toAction(e->dcZoom.buttonMinus2.doClick()));
            }
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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
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
        if (ImageAndPath.isDummyImage(originalImage)) {
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
        final String imgSuffix = ".JPG";
        if (!urlOrFile.endsWith(thmSuffix)) {
            return urlOrFile;
        }
        String base = urlOrFile.substring(0, urlOrFile.length()-thmSuffix.length());
        String unthumbnailed = base + imgSuffix;
        if (isUrl(urlOrFile)) {
            return unthumbnailed;
        } else {
            try {
                if (new File(unthumbnailed).isFile()) {
                    return unthumbnailed;
                }
            } catch (Throwable e) {
                // problems with the file, return the original
            }
            return urlOrFile;
        }
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
        return file.startsWith("NRB") || file.startsWith("RRB") || file.startsWith("FRB");
    }
    private static boolean isMarkedL(String file) {
        return file.startsWith("NLB") || file.startsWith("RLB") || file.startsWith("FLB");
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

