package cz.inqool.uas.svg;

import org.freehep.graphicsio.emf.EMFGraphics2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public class EmfWriterGraphics extends EMFGraphics2D {

    /**
     * EMF writer, based on  GraphicsConfiguration of local device
     * Method is based on Wolfgang Fang's code - https://github.com/WolfgangFahl/svg2emf
     *
     * @param outputStream Output stream to write in
     * @param size         Size of the dimension
     */
    public EmfWriterGraphics(OutputStream outputStream, Dimension size) {
        super(outputStream, size);

        new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * EmfWriterGraphics constructor
     * Method is based on Wolfgang Fang's code - https://github.com/WolfgangFahl/svg2emf
     *
     * @param graphics           EMF 2D graphics
     * @param doRestoreOnDispose Boolean value if method should do restore on dispose
     */
    protected EmfWriterGraphics(EMFGraphics2D graphics, boolean doRestoreOnDispose) {
        super(graphics, doRestoreOnDispose);
    }

    @Override
    public Graphics create() {
        try {
            writeGraphicsSave();
        } catch (IOException ex) {
            throw new GraphicsContextException("Exception during creating new graphics context", ex);
        }
        EMFGraphics2D result = new EmfWriterGraphics(this, true);
        return result;
    }
}
