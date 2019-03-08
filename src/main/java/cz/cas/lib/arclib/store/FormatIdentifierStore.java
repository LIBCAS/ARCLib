package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatIdentifier;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatIdentifierType;
import cz.cas.lib.arclib.domain.preservationPlanning.QFormatIdentifier;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class FormatIdentifierStore
        extends DatedStore<FormatIdentifier, QFormatIdentifier> {
    public FormatIdentifierStore() {
        super(FormatIdentifier.class, QFormatIdentifier.class);
    }

    public FormatIdentifier findByIdentifierTypeAndIdentifier(FormatIdentifierType identifierType, String identifier) {
        QFormatIdentifier formatIdentifier = qObject();

        JPAQuery<FormatIdentifier> query = query()
                .select(formatIdentifier)
                .where(formatIdentifier.identifierType.eq(identifierType))
                .where(formatIdentifier.identifier.eq(identifier));

        FormatIdentifier formatIdentifierFound = query.fetchFirst();

        detachAll();
        return formatIdentifierFound;
    }
}
