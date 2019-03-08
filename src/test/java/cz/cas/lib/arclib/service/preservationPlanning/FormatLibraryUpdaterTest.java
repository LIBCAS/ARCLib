package cz.cas.lib.arclib.service.preservationPlanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.preservationPlanning.*;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.core.domain.DatedObject;
import cz.cas.lib.core.mail.AsyncMailSender;
import cz.cas.lib.core.util.Utils;
import helper.SrDbTest;
import org.dom4j.DocumentException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.asList;
import static cz.cas.lib.core.util.Utils.asSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class FormatLibraryUpdaterTest extends SrDbTest {

    private FormatLibraryUpdater formatLibraryUpdater;
    private FormatService formatService;
    private FormatStore formatStore;
    private FormatIdentifierStore formatIdentifierStore;
    private FormatDeveloperStore formatDeveloperStore;
    private RelatedFormatStore relatedFormatStore;
    private User user;
    private FormatDefinitionService formatDefinitionService;
    private UserDelegate userDelegate;
    private FormatDefinitionStore formatDefinitionStore;
    private FormatDeveloperService formatDeveloperService;
    private RelatedFormatService relatedFormatService;
    private FormatIdentifierService formatIdentifierService;
    private ObjectMapper objectMapper;

    private static Integer FORMAT_ID = 668;
    private static Integer FORMAT_ID_2 = 735;
    private static Integer FORMAT_ID_3 = 768;

    @InjectMocks
    protected ArclibMailCenter mailCenter = new ArclibMailCenter();

    @InjectMocks
    protected AsyncMailSender asyncMailSender = new AsyncMailSender();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mailCenter.setSender(asyncMailSender);
        mailCenter.setSenderEmail("noreply@test.cz");
        mailCenter.setSenderName("test");
        mailCenter.setEnabled(false);

        asyncMailSender.setSender(new JavaMailSenderImpl());

        user = new User();
        userDelegate = new UserDelegate(user);

        formatIdentifierStore = new FormatIdentifierStore();
        formatIdentifierStore.setEntityManager(getEm());
        formatIdentifierStore.setQueryFactory(new JPAQueryFactory(getEm()));

        formatStore = new FormatStore();
        formatStore.setEntityManager(getEm());
        formatStore.setQueryFactory(new JPAQueryFactory(getEm()));
        formatStore.setTemplate(getTemplate());

        formatIdentifierService = new FormatIdentifierService();
        formatIdentifierService.setDelegate(formatIdentifierStore);

        relatedFormatStore = new RelatedFormatStore();
        relatedFormatStore.setEntityManager(getEm());
        relatedFormatStore.setQueryFactory(new JPAQueryFactory(getEm()));

        formatDeveloperStore = new FormatDeveloperStore();
        formatDeveloperStore.setEntityManager(getEm());
        formatDeveloperStore.setQueryFactory(new JPAQueryFactory(getEm()));

        formatService = new FormatService();
        formatService.setFormatStore(formatStore);

        formatDeveloperService = new FormatDeveloperService();
        formatDeveloperService.setDelegate(formatDeveloperStore);

        relatedFormatService = new RelatedFormatService();
        relatedFormatService.setDelegate(relatedFormatStore);

        formatLibraryUpdater = new FormatLibraryUpdater();
        formatLibraryUpdater.setFormatListUrl("http://www.nationalarchives.gov.uk/PRONOM/Format/proFormatDetailListAction.aspx");
        formatLibraryUpdater.setFormatDetailListUrl("http://www.nationalarchives.gov.uk/PRONOM/Format/proFormatListAction.aspx");
        formatLibraryUpdater.setFormatService(formatService);
        formatLibraryUpdater.setFormatIdentifierService(formatIdentifierService);
        formatLibraryUpdater.setRelatedFormatService(relatedFormatService);
        formatLibraryUpdater.setArclibMailCenter(mailCenter);
        formatLibraryUpdater.setFormatDeveloperService(formatDeveloperService);
        formatLibraryUpdater.setUserDetails(userDelegate);

        formatDefinitionStore = new FormatDefinitionStore();
        formatDefinitionStore.setEntityManager(getEm());
        formatDefinitionStore.setQueryFactory(new JPAQueryFactory(getEm()));
        formatDefinitionStore.setTemplate(getTemplate());
        formatDefinitionStore.setUserDetails(userDelegate);

        formatDefinitionService = new FormatDefinitionService();
        formatDefinitionService.setDelegate(formatDefinitionStore);
        formatLibraryUpdater.setFormatDefinitionService(formatDefinitionService);

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        formatLibraryUpdater.setObjectMapper(objectMapper);
    }

    @Test
    public void downloadFormatDetailTest() {
        ResponseEntity<String> stringResponseEntity = formatLibraryUpdater.downloadFormatDetail(FORMAT_ID);
        assertThat(stringResponseEntity.getStatusCode().is2xxSuccessful(), is(true));
    }

    @Test
    public void downloadListOfFormatsTest() {
        ResponseEntity<String> stringResponseEntity = formatLibraryUpdater.downloadListOfFormats();
        assertThat(stringResponseEntity.getStatusCode().is2xxSuccessful(), is(true));
    }

    @Test
    public void getListOfFormatIdsFromExternalTest() throws DocumentException {
        List<Integer> listOfFormatIdsFromExternal = formatLibraryUpdater.getListOfFormatIdsFromExternal();
        assertThat(listOfFormatIdsFromExternal.isEmpty(), is(false));
    }

    @Test
    public void getFormatFromExternalTest() throws ParseException, DocumentException {
        FormatDefinition formatDefinitionFromExternal = formatLibraryUpdater.getFormatDefinitionFromExternal(FORMAT_ID);
        assertThat(formatDefinitionFromExternal, is(notNullValue()));
        assertThat(formatDefinitionFromExternal.getFormat().getFormatId(), is(FORMAT_ID));

        Set<FormatIdentifier> identifiers = formatDefinitionFromExternal.getIdentifiers();
        assertThat(identifiers, hasSize(3));

        List<FormatIdentifier> puidIdentifiers = identifiers.stream()
                .filter(identifier -> identifier.getIdentifierType() == FormatIdentifierType.PUID)
                .collect(Collectors.toList());
        assertThat(puidIdentifiers, hasSize(1));
        FormatIdentifier puidIdentifier = puidIdentifiers.get(0);
        assertThat(puidIdentifier.getIdentifier(), is("fmt/43"));

        List<FormatIdentifier> mimeIdentifiers = identifiers.stream()
                .filter(identifier -> identifier.getIdentifierType() == FormatIdentifierType.MIME)
                .collect(Collectors.toList());
        assertThat(mimeIdentifiers, hasSize(1));
        FormatIdentifier mimeIdentifier = mimeIdentifiers.get(0);
        assertThat(mimeIdentifier.getIdentifier(), is("image/jpeg"));

        List<FormatIdentifier> appleIdentifiers = identifiers.stream()
                .filter(identifier -> identifier.getIdentifierType() == FormatIdentifierType.APPLE_UNIFORM_TYPE_IDENTIFIER)
                .collect(Collectors.toList());
        assertThat(appleIdentifiers, hasSize(1));
        FormatIdentifier appleIdentifier = appleIdentifiers.get(0);
        assertThat(appleIdentifier.getIdentifier(), is("public.jpeg"));

        Set<RelatedFormat> relatedFormats = formatDefinitionFromExternal.getRelatedFormats();
        assertThat(relatedFormats, hasSize(3));

        assertThat(formatDefinitionFromExternal.getAliases().contains("JFIF (1.01)"), is(true));
        assertThat(formatDefinitionFromExternal.getFormatClassifications(), hasItem(FormatClassification.IMAGE_RASTER));
        Set<FormatDeveloper> developers = formatDefinitionFromExternal.getDevelopers();
        assertThat(developers, hasSize(2));

        assertThat(formatDefinitionFromExternal.getFormatDescription()
                .contains("The JPEG File Interchange Format (JFIF) is a file format"), is(true));
        Set<String> formatFamilies = formatDefinitionFromExternal.getFormatFamilies();
        assertThat(formatFamilies, hasSize(0));

        assertThat(formatDefinitionFromExternal.getReleaseDate(), is(nullValue()));
        assertThat(formatDefinitionFromExternal.getWithdrawnDate(), is(nullValue()));
        assertThat(formatDefinitionFromExternal.getFormatVersion(), is("1.01"));
        assertThat(formatDefinitionFromExternal.getFormat().getFormatName(), is("JPEG File Interchange Format"));
        assertThat(formatDefinitionFromExternal.getFormatNote(), is(""));

        FormatDefinition formatDefinition2FromExternal = formatLibraryUpdater.getFormatDefinitionFromExternal(FORMAT_ID_2);
        assertThat(formatDefinition2FromExternal.getWithdrawnDate(), is(Instant.parse("2001-06-30T22:00:00Z")));
        assertThat(formatDefinition2FromExternal.getReleaseDate(), is(Instant.parse("1996-12-31T23:00:00Z")));

        FormatDefinition formatDefinition3FromExternal = formatLibraryUpdater.getFormatDefinitionFromExternal(FORMAT_ID_3);
        assertThat(formatDefinition3FromExternal.getFormatClassifications(), hasItem(FormatClassification.TEXT_MARKUP));
    }

    @Test
    public void updateFormatFromExternalUpstreamAndLocalDefinitionsEmptyTest() throws ParseException, DocumentException {
        List<FormatDefinition> beforeUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(beforeUpdate, hasSize(0));

        String message = formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);
        assertThat(message.contains("new upstream definition created"), is(true));

        List<FormatDefinition> afterUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterUpdate, hasSize(1));

        FormatDefinition formatDefinition = afterUpdate.get(0);
        assertThat(formatDefinition.isPreferred(), is(true));
        assertThat(formatDefinition.getInternalVersionNumber(), is(1));
    }

    @Test
    public void updateFormatFromExternalLocalDefinitionsEmptyTest() throws ParseException, DocumentException {
        formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);

        List<FormatDefinition> afterFirstUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterFirstUpdate, hasSize(1));

        /*
        Changing some attributes of the current definition so that the update process was triggered
         */
        FormatDefinition formatDefinition1 = afterFirstUpdate.get(0);
        formatDefinition1.setDevelopers(asSet());
        formatDefinition1.setRelatedFormats(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinitionService.save(formatDefinition1);

        String message = formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);
        assertThat(message.contains("has been updated with the recent upstream definition"), is(true));

        List<FormatDefinition> afterSecondUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterSecondUpdate, hasSize(2));

        FormatDefinition formatDefinitionOld = afterSecondUpdate.stream()
                .sorted(Comparator.comparing(DatedObject::getUpdated))
                .findFirst()
                .get();
        assertThat(formatDefinitionOld.isPreferred(), is(false));
        assertThat(formatDefinitionOld.getInternalVersionNumber(), is(1));

        FormatDefinition formatDefinitionNew = afterSecondUpdate.stream()
                .sorted(Comparator.comparing(DatedObject::getUpdated).reversed())
                .findFirst()
                .get();
        assertThat(formatDefinitionNew.isPreferred(), is(true));
        assertThat(formatDefinitionNew.getInternalVersionNumber(), is(2));
    }

    @Test
    public void updateFormatFromExternalUpstreamDefinitionsEmptyPreferredLocalDefinitionTest()
            throws ParseException, DocumentException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);
        formatStore.save(format);

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(true);
        formatDefinitionService.save(formatDefinition);

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition);

        List<FormatDefinition> localDefinitionCreated = formatDefinitionService.findByFormatId(FORMAT_ID, true);
        assertThat(localDefinitionCreated, hasSize(1));

        String message = formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);
        assertThat(message.contains("new upstream definition created, there is a preferred local definition"), is(true));

        List<FormatDefinition> afterUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterUpdate, hasSize(1));

        FormatDefinition formatNew = afterUpdate.get(0);
        assertThat(formatNew.isPreferred(), is(false));
        assertThat(formatNew.getInternalVersionNumber(), is(1));
    }

    @Test
    public void updateFormatFromExternalUpstreamDefinitionsEmptyNonPreferredLocalDefinitionTest()
            throws ParseException, DocumentException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);
        formatStore.save(format);

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(false);
        formatDefinitionService.save(formatDefinition);

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition);

        List<FormatDefinition> localDefinitionCreated = formatDefinitionService.findByFormatId(FORMAT_ID, true);
        assertThat(localDefinitionCreated, hasSize(1));

        formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);

        List<FormatDefinition> afterFirstUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterFirstUpdate, hasSize(1));

        /*
        Changing some attributes of the current definition so that the update process was triggered
         */
        FormatDefinition formatDefinition1 = afterFirstUpdate.get(0);
        formatDefinition1.setDevelopers(asSet());
        formatDefinition1.setRelatedFormats(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinitionService.save(formatDefinition1);

        String message = formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);
        assertThat(message.contains("has been updated with the recent upstream definition"), is(true));

        List<FormatDefinition> afterSecondUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterSecondUpdate, hasSize(2));

        FormatDefinition formatNew = afterSecondUpdate.get(0);
        assertThat(formatNew.getInternalVersionNumber(), is(2));
    }

    @Test
    public void updateFormatFromExternalUpstreamAndLocalDefinitionsNonEmptyTest()
            throws ParseException, DocumentException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);
        formatStore.save(format);

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(false);
        formatDefinitionService.save(formatDefinition);

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition);

        List<FormatDefinition> localDefinitionCreated = formatDefinitionService.findByFormatId(FORMAT_ID, true);
        assertThat(localDefinitionCreated, hasSize(1));

        String message = formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);
        assertThat(message.contains("new upstream definition created"), is(true));

        List<FormatDefinition> afterUpdate = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterUpdate, hasSize(1));

        FormatDefinition formatDefinition1 = afterUpdate.get(0);
        assertThat(formatDefinition1.getInternalVersionNumber(), is(1));

        /*
        Changing some attributes of the current definition so that the update process was triggered
         */
        formatDefinition1.setDevelopers(asSet());
        formatDefinition1.setRelatedFormats(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinitionService.save(formatDefinition1);

        String message2 = formatLibraryUpdater.updateFormatFromExternal(FORMAT_ID);
        assertThat(message2.contains("has been updated with the recent upstream definition"), is(true));

        List<FormatDefinition> afterUpdate2 = formatDefinitionService.findByFormatId(FORMAT_ID, false);
        assertThat(afterUpdate2, hasSize(2));

        FormatDefinition formatNew2 = afterUpdate2.get(1);
        assertThat(formatNew2.getInternalVersionNumber(), is(1));
    }

    @Test
    public void updateFormatWithLocalDefinitionTest() {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        formatService.save(format);
        flushCache();

        FormatDefinition formatDefinition1 = new FormatDefinition();
        formatDefinition1.setFormat(format);
        formatDefinition1.setPreferred(false);
        formatDefinition1.setFormatNote("note 1");

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition1);

        FormatDefinition preferredDefinitionByFormatId1 = formatDefinitionService.findPreferredDefinitionByFormatId(FORMAT_ID);
        assertThat(preferredDefinitionByFormatId1.getId(), is(formatDefinition1.getId()));

        FormatDefinition formatDefinition2 = new FormatDefinition();
        formatDefinition2.setFormat(format);
        formatDefinition2.setPreferred(true);
        formatDefinition2.setFormatNote("note 2");

        FormatIdentifier i1 = new FormatIdentifier();
        i1.setIdentifierType(FormatIdentifierType.FOUR_CC);
        i1.setIdentifier("fst");
        FormatIdentifier i2 = new FormatIdentifier();
        i2.setIdentifierType(FormatIdentifierType.PUID);
        i2.setIdentifier("snd");
        Set<FormatIdentifier> formatIdentifiers = asSet(i1, i2);
        formatIdentifierStore.save(formatIdentifiers);
        formatDefinition2.setIdentifiers(formatIdentifiers);

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition2);

        FormatDefinition preferredDefinitionByFormatId2 = formatDefinitionService.findPreferredDefinitionByFormatId(FORMAT_ID);
        assertThat(preferredDefinitionByFormatId2.getId(), is(formatDefinition2.getId()));
        assertThat(preferredDefinitionByFormatId2.getIdentifiers(), hasSize(2));
        //assertThat(preferredDefinitionByFormatId2.getFormat().getPuid(), is("snd"));
    }

    /*
    The test is ignored because the execution takes too long
     */
    @Test
    @Ignore
    public void updateFormatsFromExternalTest() throws ParseException, DocumentException {
        formatLibraryUpdater.updateFormatsFromExternal(null);
    }

    @Test
    public void exportFormatDefinitionToJsonTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        formatService.save(format);
        flushCache();

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(true);
        formatDefinition.setFormatNote("note 1");
        formatDefinition.setAliases(asSet());
        formatDefinition.setIdentifiers(asSet());
        formatDefinition.setFormatFamilies(asSet());
        formatDefinition.setFormatClassifications(asSet());

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition);

        String json = formatLibraryUpdater.exportFormatDefinitionToJson(formatDefinition.getId());

        FormatDefinition formatDefinitionExported = objectMapper.readValue(json, FormatDefinition.class);

        assertThat(formatDefinitionExported.equals(formatDefinition), is(true));
    }

    @Test
    public void exportFormatDefinitionsToJsonTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        formatService.save(format);
        flushCache();

        FormatDefinition formatDefinition1 = new FormatDefinition();
        formatDefinition1.setFormat(format);
        formatDefinition1.setPreferred(false);
        formatDefinition1.setFormatNote("note 1");
        formatDefinition1.setAliases(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinition1.setFormatFamilies(asSet());
        formatDefinition1.setFormatClassifications(asSet());

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition1);

        FormatDefinition formatDefinition2 = new FormatDefinition();
        formatDefinition2.setFormat(format);
        formatDefinition2.setPreferred(true);
        formatDefinition2.setFormatNote("note 2");
        formatDefinition2.setAliases(asSet());
        formatDefinition2.setIdentifiers(asSet());
        formatDefinition2.setFormatFamilies(asSet());
        formatDefinition2.setFormatClassifications(asSet());

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition2);

        String json = formatLibraryUpdater.exportFormatDefinitionsToJson();

        CollectionType typeReference =
                TypeFactory.defaultInstance().constructCollectionType(Collection.class, FormatDefinition.class);

        Collection<FormatDefinition> formatDefinitionsExported = objectMapper.readValue(json, typeReference);

        assertThat(formatDefinitionsExported.size(), is(2));

        List<FormatDefinition> byId1 = formatDefinitionsExported.stream()
                .filter(f -> f.getId().equals(formatDefinition1.getId()))
                .collect(Collectors.toList());

        List<FormatDefinition> byId2 = formatDefinitionsExported.stream()
                .filter(f -> f.getId().equals(formatDefinition2.getId()))
                .collect(Collectors.toList());

        assertThat(byId1.size(), is(1));
        assertThat(byId1.get(0).equals(formatDefinition1), is(true));
        assertThat(byId2.size(), is(1));
        assertThat(byId2.get(0).equals(formatDefinition2), is(true));
    }

    @Test
    public void exportFormatDefinitionToByteArrayTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        formatService.save(format);
        flushCache();

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(true);
        formatDefinition.setFormatNote("note 1");
        formatDefinition.setAliases(asSet());
        formatDefinition.setIdentifiers(asSet());
        formatDefinition.setFormatFamilies(asSet());
        formatDefinition.setFormatClassifications(asSet());

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition);

        byte[] compressedBytes = formatLibraryUpdater.exportFormatDefinitionToByteArray(formatDefinition.getId());

        byte[] bytes = Utils.decompress(compressedBytes);
        FormatDefinition formatDefinitionExported = objectMapper.readValue(bytes, FormatDefinition.class);

        assertThat(formatDefinitionExported.equals(formatDefinition), is(true));
    }

    @Test
    public void exportFormatDefinitionsToByteArrayTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        formatService.save(format);
        flushCache();

        FormatDefinition formatDefinition1 = new FormatDefinition();
        formatDefinition1.setFormat(format);
        formatDefinition1.setPreferred(true);
        formatDefinition1.setFormatNote("note 1");
        formatDefinition1.setAliases(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinition1.setFormatFamilies(asSet());
        formatDefinition1.setFormatClassifications(asSet());

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition1);

        FormatDefinition formatDefinition2 = new FormatDefinition();
        formatDefinition2.setFormat(format);
        formatDefinition2.setPreferred(true);
        formatDefinition2.setFormatNote("note 2");
        formatDefinition2.setAliases(asSet());
        formatDefinition2.setIdentifiers(asSet());
        formatDefinition2.setFormatFamilies(asSet());
        formatDefinition2.setFormatClassifications(asSet());

        formatLibraryUpdater.updateFormatWithLocalDefinition(formatDefinition2);

        byte[] bytes = formatLibraryUpdater.exportFormatDefinitionsToByteArray();

        byte[] compressed = Utils.decompress(bytes);

        CollectionType typeReference =
                TypeFactory.defaultInstance().constructCollectionType(Collection.class, FormatDefinition.class);

        Collection<FormatDefinition> formatDefinitionsExported = objectMapper.readValue(compressed, typeReference);

        assertThat(formatDefinitionsExported.size(), is(2));

        List<FormatDefinition> byId1 = formatDefinitionsExported.stream()
                .filter(f -> f.getId().equals(formatDefinition1.getId()))
                .collect(Collectors.toList());

        List<FormatDefinition> byId2 = formatDefinitionsExported.stream()
                .filter(f -> f.getId().equals(formatDefinition2.getId()))
                .collect(Collectors.toList());

        assertThat(byId1.size(), is(1));
        assertThat(byId1.get(0).equals(formatDefinition1), is(true));
        assertThat(byId2.size(), is(1));
        assertThat(byId2.get(0).equals(formatDefinition2), is(true));
    }

    @Test
    public void importFormatDefinitionFromJsonTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(true);
        formatDefinition.setFormatNote("note 1");
        formatDefinition.setAliases(asSet());
        formatDefinition.setIdentifiers(asSet());
        formatDefinition.setFormatFamilies(asSet());
        formatDefinition.setFormatClassifications(asSet());

        formatLibraryUpdater.importFormatDefinitionFromJson(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formatDefinition));

        Collection<FormatDefinition> all = formatDefinitionService.findAll();
        assertThat(all.size(), is(1));

        FormatDefinition formatDefinitionImported = all.iterator().next();
        assertThat(formatDefinitionImported.equals(formatDefinition), is(true));
    }

    @Test
    public void importFormatDefinitionFromByteArrayTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setFormat(format);
        formatDefinition.setPreferred(true);
        formatDefinition.setFormatNote("note 1");
        formatDefinition.setAliases(asSet());
        formatDefinition.setIdentifiers(asSet());
        formatDefinition.setFormatFamilies(asSet());
        formatDefinition.setFormatClassifications(asSet());

        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(formatDefinition);

        byte[] compressed = Utils.compress(bytes);

        formatLibraryUpdater.importFormatDefinitionFromByteArray(compressed);

        Collection<FormatDefinition> all = formatDefinitionService.findAll();
        assertThat(all.size(), is(1));

        FormatDefinition formatDefinitionImported = all.iterator().next();
        assertThat(formatDefinitionImported.equals(formatDefinition), is(true));
    }

    @Test
    public void importFormatDefinitionsFromJsonTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);
        format.setThreatLevel(ThreatLevel.MODERATE);

        Risk risk = new Risk();
        risk.setDescription("a dangerous risk");

        Risk risk2 = new Risk();
        risk2.setDescription("another dangerous risk");

        format.setRelatedRisks(asSet(risk, risk2));

        FormatDefinition formatDefinition1 = new FormatDefinition();
        formatDefinition1.setFormat(format);
        formatDefinition1.setPreferred(true);
        formatDefinition1.setFormatNote("note 1");
        formatDefinition1.setAliases(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinition1.setFormatFamilies(asSet());
        formatDefinition1.setFormatClassifications(asSet());

        FormatDefinition formatDefinition2 = new FormatDefinition();
        formatDefinition2.setFormat(format);
        formatDefinition2.setPreferred(true);
        formatDefinition2.setFormatNote("note 2");
        formatDefinition2.setAliases(asSet());
        formatDefinition2.setIdentifiers(asSet());
        formatDefinition2.setFormatFamilies(asSet());
        formatDefinition2.setFormatClassifications(asSet());

        formatLibraryUpdater.importFormatDefinitionsFromJson(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(asList(formatDefinition1, formatDefinition2)));

        flushCache();

        Collection<FormatDefinition> formatDefinitionsImported = formatDefinitionService.findAll();

        assertThat(formatDefinitionsImported.size(), is(2));

        List<FormatDefinition> byId1 = formatDefinitionsImported.stream()
                .filter(f -> f.getId().equals(formatDefinition1.getId()))
                .collect(Collectors.toList());

        List<FormatDefinition> byId2 = formatDefinitionsImported.stream()
                .filter(f -> f.getId().equals(formatDefinition2.getId()))
                .collect(Collectors.toList());

        assertThat(byId1.size(), is(1));
        assertThat(byId1.get(0).equals(formatDefinition1), is(true));
        assertThat(byId2.size(), is(1));
        assertThat(byId2.get(0).equals(formatDefinition2), is(true));
    }

    @Test
    public void importFormatDefinitionsFromByteArrayTest() throws IOException {
        Format format = new Format();
        format.setFormatId(FORMAT_ID);

        FormatDefinition formatDefinition1 = new FormatDefinition();
        formatDefinition1.setFormat(format);
        formatDefinition1.setPreferred(true);
        formatDefinition1.setFormatNote("note 1");
        formatDefinition1.setAliases(asSet());
        formatDefinition1.setIdentifiers(asSet());
        formatDefinition1.setFormatFamilies(asSet());
        formatDefinition1.setFormatClassifications(asSet());

        FormatDefinition formatDefinition2 = new FormatDefinition();
        formatDefinition2.setFormat(format);
        formatDefinition2.setPreferred(true);
        formatDefinition2.setFormatNote("note 2");
        formatDefinition2.setAliases(asSet());
        formatDefinition2.setIdentifiers(asSet());
        formatDefinition2.setFormatFamilies(asSet());
        formatDefinition2.setFormatClassifications(asSet());

        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(asList(formatDefinition1, formatDefinition2));

        byte[] compressed = Utils.compress(bytes);
        formatLibraryUpdater.importFormatDefinitionsFromByteArray(compressed);

        Collection<FormatDefinition> formatDefinitionsImported = formatDefinitionService.findAll();

        assertThat(formatDefinitionsImported.size(), is(2));

        List<FormatDefinition> byId1 = formatDefinitionsImported.stream()
                .filter(f -> f.getId().equals(formatDefinition1.getId()))
                .collect(Collectors.toList());

        List<FormatDefinition> byId2 = formatDefinitionsImported.stream()
                .filter(f -> f.getId().equals(formatDefinition2.getId()))
                .collect(Collectors.toList());

        assertThat(byId1.size(), is(1));
        assertThat(byId1.get(0).equals(formatDefinition1), is(true));
        assertThat(byId2.size(), is(1));
        assertThat(byId2.get(0).equals(formatDefinition2), is(true));
    }
}
