package cz.inqool.uas.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.exception.MissingAttribute;
import cz.inqool.uas.export.pptx.Definition;
import cz.inqool.uas.export.pptx.ImageType;
import cz.inqool.uas.export.pptx.Picture;
import cz.inqool.uas.export.pptx.Slide;
import cz.inqool.uas.service.Templater;
import cz.inqool.uas.svg.SvgConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cz.inqool.uas.util.Utils.notNull;
import static java.util.Collections.emptyMap;

/**
 * PPTX exporter.
 *
 * Uses Velocity for string replace and SvgConverter for SVG to PNG conversion.
 */
@Slf4j
@Service
public class PptxExporter {
    private ObjectMapper objectMapper;

    private Templater templater;

    private SvgConverter svgConverter;

    /**
     * Exports provided data using pptx template.
     *
     * Specific of this exporter is that structure of arguments is equal to {@link Definition}.
     *
     * @param template PPTX template
     * @param arguments provided arguments
     * @return Exported PPTX
     */
    public byte[] export(InputStream template, Map<String, Object> arguments) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            XMLSlideShow ppt = new XMLSlideShow(template);

            Definition definition = objectMapper.convertValue(arguments, Definition.class);

            List<Slide> slideDefinitions = definition.getSlides();
            slideDefinitions.forEach(slideDefinition -> {

                int masterIndex = slideDefinition.getMaster() != null ? slideDefinition.getMaster() : 0;
                XSLFSlideMaster master = ppt.getSlideMasters().get(masterIndex);
                XSLFSlideLayout layout = master.getLayout(slideDefinition.getLayout());

                XSLFSlide slide = ppt.createSlide(layout);

                Map<String, Object> data = slideDefinition.getData() != null ? slideDefinition.getData() : emptyMap();
                Map<String, Picture> pictures = slideDefinition.getPictures() != null ? slideDefinition.getPictures() : emptyMap();

                Stream<XSLFTextShape> placeholders = Stream.of(slide.getPlaceholders());
                placeholders.forEach(placeholder -> {
                    Placeholder type = placeholder.getPlaceholder();
                    String placeholderText = placeholder.getText();

                    if (type == Placeholder.CONTENT) {
                        String pictureId = placeholderText.trim();
                        if (pictureId.startsWith("$picture:")) {
                            pictureId = pictureId.substring("$picture:".length()); // remove "$picture:"

                            Picture picture = pictures.get(pictureId);
                            if (picture == null) {
                                log.warn("Missing picture with id {}", pictureId);
                            } else {
                                replacePicturePlaceholder(placeholder, picture, slide);
                            }
                        }
                    } else {
                        String text = templater.transform(placeholderText, data);
                        placeholder.setText(text);
                    }
                });

            });


            ppt.write(out);

            return out.toByteArray();
        } catch (Exception ex) {
            throw new GeneralException(ex);
        }
    }

    private void replacePicturePlaceholder(XSLFTextShape placeholder, Picture picture, XSLFSlide slide) {
        notNull(picture.getContent(), () -> new MissingAttribute(picture, "content"));
        notNull(picture.getType(), () -> new MissingAttribute(picture, "type"));

        Rectangle2D anchor = placeholder.getAnchor();
        slide.removeShape(placeholder);

        byte[] pngData = getPNGData(picture, anchor.getWidth(), anchor.getHeight());

        XMLSlideShow ppt = slide.getSlideShow();
        XSLFPictureData pd = ppt.addPicture(pngData, PictureData.PictureType.EMF);

        XSLFPictureShape pic = slide.createPicture(pd);

        Rectangle2D fitRectangle = calculateFitRectangle(pd, anchor);

        pic.setAnchor(fitRectangle);
    }

    private Rectangle2D calculateFitRectangle(XSLFPictureData picture, Rectangle2D anchor) {
        Dimension imageDimension = picture.getImageDimension();

        double aspectImg = imageDimension.getWidth() / imageDimension.getHeight();
        double aspectPh = anchor.getWidth() / anchor.getHeight();

        double centerX = anchor.getCenterX();
        double centerY = anchor.getCenterY();
        double width, height;

        if (aspectPh > aspectImg) {
            height = anchor.getHeight();
            width = aspectImg * anchor.getHeight();
        } else {
            width = anchor.getWidth();
            height = aspectImg * anchor.getWidth();
        }

        double x = centerX - width/2;
        double y = centerY - height/2;

        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    private byte[] getPNGData(Picture picture, double width, double height) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decoded = decoder.decode(picture.getContent());

        if (picture.getType() == ImageType.SVG) {
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            //svgConverter.convertSvgToPng(new ByteArrayInputStream(decoded), pngStream, (float)width, (float)height);
            svgConverter.convertSvgToEmf(new ByteArrayInputStream(decoded), pngStream);

            return pngStream.toByteArray();
        } else {
            return decoded;
        }
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }

    @Inject
    public void setSvgConverter(SvgConverter svgConverter) {
        this.svgConverter = svgConverter;
    }
}
