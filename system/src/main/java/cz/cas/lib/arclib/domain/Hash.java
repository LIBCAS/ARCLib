package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

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
