package cz.inqool.uas.error;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.security.UserDetails;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ErrorServiceTest extends DbTest {
    protected ErrorService errorService;

    protected ErrorStore errorStore;

    @Mock
    protected HttpServletRequest httpServletRequest;

    @Mock
    protected UserDetails userDetails;

    @Mock
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(httpServletRequest.getRemoteAddr()).thenReturn("remote addr");
        when(userDetails.getId()).thenReturn("user details id");
        when(userDetails.getUsername()).thenReturn("user details id");


        errorStore =  new ErrorStore();
        errorStore.setEntityManager(getEm());
        errorStore.setTemplate(elasticsearchTemplate);
        errorStore.setQueryFactory(new JPAQueryFactory(getEm()));

        errorService = new ErrorService();
        errorService.setRequest(httpServletRequest);
        errorService.setUser(userDetails);
        errorService.setStore(errorStore);
    }

    @Test
    public void logErrorTest() {
        Collection<Error> all = errorStore.findAll();
        assertThat(all, hasSize(0));

        errorService.logError("message", "stacktrace", "url", "userAgent");
        flushCache();

        all = errorStore.findAll();
        assertThat(all, hasSize(1));

        Error error1 = all.iterator().next();
        assertThat(error1.getMessage(), is("message"));
        assertThat(error1.getStackTrace(), is("stacktrace"));

        assertThat(error1.getUserAgent(), is("userAgent"));
        assertThat(error1.getClientSide(), is(true));
        assertThat(error1.getIp(), is(httpServletRequest.getRemoteAddr()));
        assertThat(error1.getUrl(), is("url"));
        assertThat(error1.getUserId(), is (userDetails.getId()));

        /*
        Request is null
         */
        errorService.setRequest(null);

        errorService.logError("message2", "stacktrace2", "url2", "userAgent2");
        flushCache();

        all = errorStore.findAll();
        assertThat(all, hasSize(2));

        Error error2 = all.stream()
                .filter(e -> e.getId() != error1.getId())
                .findFirst()
                .get();

        assertThat(error2.getUserAgent(), is(nullValue()));
        assertThat(error2.getIp(), is(nullValue()));
        assertThat(error2.getUrl(), is(nullValue()));
        assertThat(error1.getUserId(), is (userDetails.getId()));
    }
}
