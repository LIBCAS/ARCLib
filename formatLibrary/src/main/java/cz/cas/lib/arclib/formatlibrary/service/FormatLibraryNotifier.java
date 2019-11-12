package cz.cas.lib.arclib.formatlibrary.service;

import java.time.Instant;

public interface FormatLibraryNotifier {
    void sendUnsupportedPronomValueNotification(String username, String msg, Instant timestamp);

    void sendFormatLibraryUpdateNotification(String username, String msg, Instant timestamp);

}
