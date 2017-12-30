package cz.inqool.uas.svg;

import cz.inqool.uas.exception.MissingObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.io.*;

import static cz.inqool.uas.util.Utils.resource;

@Slf4j
public class SvgConverterTest {

    private InputStream inputStream;

    private OutputStream outputStreamPng;

    private OutputStream outputStreamEmf;

    @InjectMocks
    SvgConverter svgConverter = new SvgConverter();

    @Before
    public void setUp() throws IOException {
        inputStream = resource("cz/inqool/uas/svg/homer-simpson.svg");
        outputStreamPng = new FileOutputStream("homer-simpson.png");
        outputStreamEmf = new FileOutputStream("homer-simpson.emf");
    }

    @Test
    public void svgToPngConverterTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, 600f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterNullInputTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(null, outputStreamPng, 800f, 600f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterNullOutputTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, null, 800f, 600f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterNullWidthTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, null, 600f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterNullHeightTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterSmallerThanOneWidthTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, -1f, 600f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterSmallerThanOneHeightTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, -1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterZeroWidthTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 0f, 600f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterZeroHeightTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToPngConverterNullAllTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToPng(null, null, null, null);
    }

    @Test(expected = MissingObject.class)
    public void svgToPngConverterWrongFileTest() throws IOException, TranscoderException {
        inputStream = resource("cz/inqool/uas/svg/not-existing.svg");
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, 600f);
    }

    @Test(expected = SvgConverterException.class)
    public void svgToPngConverterWrongInputFileTest() throws IOException, TranscoderException {
        inputStream = resource("cz/inqool/uas/svg/bad-homer-simpson.svg");
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, 600f);
    }

    @Test(expected = FileNotFoundException.class)
    public void svgToPngConverterWrongOutputFileTest() throws IOException, TranscoderException {
        outputStreamPng = new FileOutputStream("cz/inqool/uas/svg/homer-simpson.png");
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, 600f);
    }

    @Test(expected = SvgConverterException.class)
    public void svgToPngConverterWrongInputFile2Test() throws IOException, TranscoderException {
        inputStream = resource("cz/inqool/uas/svg/bad-homer-simpson-2.svg");
        svgConverter.convertSvgToPng(inputStream, outputStreamPng, 800f, 600f);
    }

    @Test
    public void svgToEmfConverterTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToEmf(inputStream, outputStreamEmf);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToEmfConverterNullSvgStreamTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToEmf(null, outputStreamEmf);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svgToEmfConverterNullEmfStreamTest() throws IOException, TranscoderException {
        svgConverter.convertSvgToEmf(inputStream, null);
    }

    @Test(expected = MissingObject.class)
    public void svgToEmfConverterWrongFileTest() throws IOException, TranscoderException {
        inputStream = resource("cz/inqool/uas/svg/not-existing.svg");
        svgConverter.convertSvgToEmf(inputStream, outputStreamPng);
    }

    @Test(expected = SvgConverterException.class)
    public void svgToEmfConverterWrongInputFileTest() throws IOException, TranscoderException {
        inputStream = resource("cz/inqool/uas/svg/bad-homer-simpson.svg");
        svgConverter.convertSvgToEmf(inputStream, outputStreamPng);
    }

    @Test(expected = FileNotFoundException.class)
    public void svgToEmfConverterWrongOutputFileTest() throws IOException, TranscoderException {
        outputStreamPng = new FileOutputStream("cz/inqool/uas/svg/homer-simpson.png");
        svgConverter.convertSvgToEmf(inputStream, outputStreamPng);
    }

    @Test(expected = SvgConverterException.class)
    public void svgToEmfConverterWrongInputFile2Test() throws IOException, TranscoderException {
        inputStream = resource("cz/inqool/uas/svg/bad-homer-simpson-2.svg");
        svgConverter.convertSvgToEmf(inputStream, outputStreamPng);
    }
}