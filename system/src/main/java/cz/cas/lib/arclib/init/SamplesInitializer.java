package cz.cas.lib.arclib.init;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.index.solr.ReindexService;
import cz.cas.lib.arclib.security.authorization.role.UserRoleStore;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.store.Transactional;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@Order(2)
@ConditionalOnProperty(prefix = "arclib.init.samples", name = "enabled", havingValue = "true")
public class SamplesInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final String PRODUCER_PROFILE_ID = "b0384aeb-5169-459a-b5f4-483e6ad7b949";
    private static final String SUPERADMIN_ROLE_ID = "b7a43ad5-883f-4741-948b-08678fa38604";
    private static final String SAMPLE_USER_ID = "962d8cb6-556a-4396-9c4a-2af00c3a17b9";

    @Value("${arclib.init.samples.debug:false}")
    private boolean samplesInDebugMode;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserStore userStore;
    @Value("classpath:sql/sampleProfiles.sql")
    private Resource sampleProfilesSql;
    @Value("classpath:sql/formatLibraryInit.sql")
    private Resource formatLibraryInitSql;
    @Autowired
    private ProducerProfileStore producerProfileStore;
    @Autowired
    private ReindexService reindexService;
    @Autowired
    private UserRoleStore userRoleStore;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("samples initializer started");
        if (userStore.countAll() == 0) {
            ((SessionImpl) entityManager.getDelegate()).doWork(c -> {
                ScriptUtils.executeSqlScript(c, sampleProfilesSql);
                ScriptUtils.executeSqlScript(c, formatLibraryInitSql);
            });
            ProducerProfile producerProfile = producerProfileStore.find(PRODUCER_PROFILE_ID);
            if (!samplesInDebugMode) {
                producerProfile.setDebuggingModeActive(false);
                producerProfileStore.save(producerProfile);
            }
            User user = new User();
            user.setId(SAMPLE_USER_ID);
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("admin"));
            user.setProducer(producerProfile.getProducer());
            user.setRoles(Set.of(userRoleStore.find(SUPERADMIN_ROLE_ID)));
            userStore.save(user);
            reindexService.dropReindexManagedSync();
            log.info("samples initializer finished");
        } else {
            log.info("samples initializer skipped since there are already some users present in DB");
        }
    }
}
