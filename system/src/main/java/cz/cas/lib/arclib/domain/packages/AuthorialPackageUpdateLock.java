package cz.cas.lib.arclib.domain.packages;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.time.Instant;

/**
 * Zámok aktívny počas editácie autorského balíčka
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_authorial_package_update_lock")
public class AuthorialPackageUpdateLock extends DomainObject {
    /**
     * Autorský balík
     */
    @OneToOne
    private AuthorialPackage authorialPackage;

    /**
     * Zamknutý
     */
    private boolean locked;

    /**
     * Užívateľ, ktorý uzamkol balík
     */
    @ManyToOne
    private User lockedByUser;

    /**
     * Čas posledného obnovenia
     */
    private Instant latestLockedInstant;

    /**
     * Časový job kontrolujúci posledné obnovenie
     */
    @OneToOne(cascade = CascadeType.ALL)
    private Job timeoutCheckJob;
}
