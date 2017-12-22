package cz.inqool.uas.svg;

import lombok.extern.slf4j.Slf4j;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.OutputStream;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Service class for converting SVG file into PNG file
 */
@Service
@Slf4j
public class SvgConverter {

    /**
     * Method for transforming SVG picture to PNG picture
     *
     * @param svgStream input stream of source SVG file
     * @param pngStream output stream of target PNG file
     * @param width     width of the target PNG file
     * @param height    height of the target PNG file
     */
    public void convertSvgToPng(InputStream svgStream, OutputStream pngStream, Float width, Float height) {
        notNull(svgStream, IllegalArgumentException::new);
        notNull(pngStream, IllegalArgumentException::new);
        notNull(width, IllegalArgumentException::new);
        notNull(height, IllegalArgumentException::new);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height muset be bigger than zero");
        }

        try {
            TranscoderInput input = new TranscoderInput(svgStream);
            TranscoderOutput output = new TranscoderOutput(pngStream);

            PNGTranscoder converter = new PNGTranscoder();
            converter.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width);
            converter.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height);

            converter.transcode(input, output);

        } catch (TranscoderException ex) {
            throw new SvgConverterException("Exception during transforming SVG to PNG", ex);
        }
    }

    /**
     * Method for transforming SVG picture into EMF picture
     * Method is based on Wolfgang Fang's code - https://github.com/WolfgangFahl/svg2emf
     *
     * @param svgStream SVG file stream (source file)
     * @param emfStream EMF file stream (target file)
     */
    public void convertSvgToEmf(InputStream svgStream, OutputStream emfStream) {
        notNull(svgStream, IllegalArgumentException::new);
        notNull(emfStream, IllegalArgumentException::new);

        UserAgentAdapter userAgentAdapter = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgentAdapter);
        try {
            BridgeContext bridgeContext = new BridgeContext(userAgentAdapter, loader);
            try {
                Document svgDocument = loader.loadDocument("", svgStream);
                GraphicsNode rootNode = buildGVTTree(bridgeContext, svgDocument);

                writeEmf(rootNode, emfStream);
            } catch (Exception ex) {
                throw new SvgConverterException("Exception during writing to EMF", ex);
            } finally {
                bridgeContext.dispose();
            }
        } catch (Exception ex) {
            throw new SvgConverterException("Exception during creating bridge context bridge context", ex);
        } finally {
            loader.dispose();
        }
    }

    /**
     * Private auxiliary method for building GVTT tree, needed for proper SVG to EMF conversion
     * Method is based on Wolfgang Fang's code - https://github.com/WolfgangFahl/svg2emf
     *
     * @param bridgeContext Batik BridgeContext
     * @param svgDoc        Source SVG document
     * @return Graphic node built by GVTBuilder
     */
    private GraphicsNode buildGVTTree(BridgeContext bridgeContext, Document svgDoc) {
        GVTBuilder gvtBuilder = new GVTBuilder();
        return gvtBuilder.build(bridgeContext, svgDoc);
    }

    /**
     * Private auxiliary method for final to EMF export
     * Method is based on Wolfgang Fang's code - https://github.com/WolfgangFahl/svg2emf
     *
     * @param rootNode  Root node generated in buildGVTTree method
     * @param emfStream EMF output stream for exporting into file
     */
    private void writeEmf(GraphicsNode rootNode, OutputStream emfStream) {
        // x,y can be non-(0,0)
        Rectangle2D bounds = rootNode.getBounds();
        int w = (int) (bounds.getX() + bounds.getWidth());
        int h = (int) (bounds.getY() + bounds.getHeight());

        Dimension size = new Dimension(w, h);
        EmfWriterGraphics eg2d = new EmfWriterGraphics(emfStream, size);
        eg2d.setDeviceIndependent(true);
        eg2d.startExport();
        rootNode.paint(eg2d);
        eg2d.endExport();
    }
}
