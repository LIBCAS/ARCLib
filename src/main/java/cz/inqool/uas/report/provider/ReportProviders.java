package cz.inqool.uas.report.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

@Service
public class ReportProviders {
    private List<ReportProvider> providers;

    /**
     * Gets all {@link ReportProvider}s.
     *
     * @return {@link List} of {@link ReportProvider}
     */
    public List<ReportProvider> getProviders() {
        return unmodifiableList(providers);
    }

    /**
     * Gets {@link ReportProvider} corresponding to specified provider class.
     *
     * @param providerClass Specified class
     * @return {@link ReportProvider}
     */
    public ReportProvider getProvider(String providerClass) {
        return providers.stream()
                        .filter(provider -> Objects.equals(provider.getClass().getName(), providerClass))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Tests if there is {@link ReportProvider} with specified class.
     *
     * @param providerClass Specified class to test
     * @return Existence of {@link ReportProvider}
     */
    public boolean isValid(String providerClass) {
        return providers.stream()
                 .map(provider -> provider.getClass().getName())
                 .anyMatch(id -> Objects.equals(id, providerClass));
    }

    @Autowired(required = false)
    public void setProviders(List<ReportProvider> providers) {
        this.providers = providers != null ? providers : emptyList();
    }
}
