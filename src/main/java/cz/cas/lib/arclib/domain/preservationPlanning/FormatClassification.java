package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.index.Labeled;
import lombok.Getter;

/**
 * Klasifikácia formátu
 */
@Getter
public enum FormatClassification implements Labeled {
    IMAGE_RASTER("Image (Raster)"),
    IMAGE_VECTOR("Image (Vector)"),
    AUDIO("Audio"),
    VIDEO("Video"),
    AGGREGATE("Aggregate"),
    DATABASE_SPREADSHEET("Database Spreadsheet"),
    WORD_PROCESSOR("Word processor"),
    TEXT_UNSTRUCTURED("Text (Unstructured)"),
    TEXT_STRUCTURED("Text (Structured)"),
    TEXT_MARKUP("Text (Mark-up)"),
    TEXT_WORDPROCESSED("Text (Wordprocessed)"),
    SPREADSHEET("Spreadsheet"),
    PRESENTATION("Presentation"),
    MODEL("Model"),
    DATASET("Dataset"),
    FONT("Font"),
    DATABASE("Database"),
    GIS("GIS"),
    PAGE_DESCRIPTION("Page Description"),
    EMAIL("Email");

    private String label;

    FormatClassification(String label) {
        this.label = label;
    }
}
