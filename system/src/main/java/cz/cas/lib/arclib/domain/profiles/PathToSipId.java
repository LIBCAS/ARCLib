package cz.cas.lib.arclib.domain.profiles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Cesta k autorskému ID balíku
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PathToSipId {
    /**
     * Cesta k XML súboru obsahujúcemu autorské ID vo forme glob vzoru
     */
    @Column(name = "authorial_id_file_path_regex")
    private String pathToXmlRegex;

    /**
     * XPath k uzlu s autorským ID v rámci XML suboru
     */
    @Column(name = "authorial_id_xpath")
    private String XPathToId;
}
