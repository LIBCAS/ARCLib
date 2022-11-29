package cz.cas.lib.arclib.formatlibrary.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.FormatIdentifier;
import cz.cas.lib.arclib.formatlibrary.domain.QFormatIdentifier;
import org.springframework.stereotype.Repository;

@Repository
public class FormatIdentifierStore
        extends DatedStore<FormatIdentifier, QFormatIdentifier> {
    public FormatIdentifierStore() {
        super(FormatIdentifier.class, QFormatIdentifier.class);
    }

    public FormatIdentifier findByIdentifierTypeAndIdentifier(String identifierType, String identifier) {
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
