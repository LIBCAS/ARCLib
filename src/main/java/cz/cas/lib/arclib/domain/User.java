package cz.cas.lib.arclib.domain;

import cz.cas.lib.core.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Užívateľ
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_user")
@AllArgsConstructor
@NoArgsConstructor
public class User extends DatedObject {
    /**
     * Používateľské meno
     */
    private String username;

    /**
     * Krstné meno
     */
    private String firstName;

    /**
     * Priezvisko
     */
    private String lastName;

    /**
     * Email
     */
    private String email;

    /**
     * Unikátny identifikátor v rámci LDAPu
     */
    private String ldapDn;

    /**
     * Dodávateľ
     */
    @ManyToOne
    private Producer producer;

    public User(String id) {
        setId(id);
    }

    public User(String id, Producer p) {
        setId(id);
        setProducer(p);
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return username;
        }
    }
}
