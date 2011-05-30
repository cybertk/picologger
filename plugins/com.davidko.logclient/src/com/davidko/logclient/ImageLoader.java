package com.davidko.logclient;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ImageLoader {

    private static final String PATH = "/images/";

    private final HashMap<String, Image> mLoadedImages = new HashMap<String, Image>();
    private static final HashMap<Class<?>, ImageLoader> mInstances =
            new HashMap<Class<?>, ImageLoader>();
    private final Class<?> mClass;

    private ImageLoader(Class<?> theClass) {
        if (theClass == null) {
            theClass = ImageLoader.class;
        }
        mClass = theClass;
    }

    public static ImageLoader getDdmUiLibLoader() {
        return getLoader(null);
    }

    public static ImageLoader getLoader(Class<?> theClass) {
        ImageLoader instance = mInstances.get(theClass);
        if (instance == null) {
            instance = new ImageLoader(theClass);
            mInstances.put(theClass, instance);
        }

        return instance;
    }

    public static void dispose() {
        for (ImageLoader loader : mInstances.values()) {
            loader.doDispose();
        }
    }

    private synchronized void doDispose() {
        for (Image image : mLoadedImages.values()) {
            image.dispose();
        }

        mLoadedImages.clear();
    }

    public ImageDescriptor loadDescriptor(String filename) {
        URL url = mClass.getResource(PATH + filename);
        return ImageDescriptor.createFromURL(url);
    }

    public synchronized Image loadImage(String filename, Display display) {
        Image img = mLoadedImages.get(filename);
        if (img == null) {
            String tmp = PATH + filename;
            InputStream imageStream = mClass.getResourceAsStream(tmp);

            if (imageStream != null) {
                img = new Image(display, imageStream);
                mLoadedImages.put(filename, img);
                return img;
            }
        }
        return img;
    }

    public Image loadImage(Display display, String fileName, int width, int height,
            Color phColor) {
        Image img = loadImage(fileName, display);
        if (img == null) {
            Log.w("localhost", "", "logclient", "Couldn't load " + fileName);
            if (width != -1 && height != -1) {
                return createPlaceHolderArt(display, width, height,
                        phColor != null ? phColor : display
                                .getSystemColor(SWT.COLOR_BLUE));
            }
            return null;
        }
        return img;
    }

    public static Image createPlaceHolderArt(Display display, int width,
            int height, Color color) {
        Image img = new Image(display, width, height);
        GC gc = new GC(img);
        gc.setForeground(color);
        gc.drawLine(0, 0, width, height);
        gc.drawLine(0, height - 1, width, -1);
        gc.dispose();
        return img;
    }
}
