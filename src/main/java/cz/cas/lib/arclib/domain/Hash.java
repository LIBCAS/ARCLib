package cz.cas.lib.arclib.domain;

import cz.cas.lib.core.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Fixita
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_hash")
public class Hash extends DatedObject {
    /**
     * Hodnota fixity
     */
    private String hashValue;

    /**
     * Typ fixity
     */
    @Enumerated(EnumType.STRING)
    private HashType hashType;
}
