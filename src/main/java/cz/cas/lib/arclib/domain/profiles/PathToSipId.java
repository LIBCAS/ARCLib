package cz.cas.lib.arclib.domain.profiles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

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
     * Cesta k XML súboru obsahujúcemu autorské ID
     */
    @Column(name = "authorial_id_file_path")
    private String pathToXml;

    /**
     * XPath k uzlu s autorským ID v rámci XML suboru
     */
    @Column(name = "authorial_id_xpath")
    private String XPathToId;
}
