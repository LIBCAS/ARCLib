package cz.cas.lib.arclib.service.archivalStorage;

import lombok.Getter;

/**
 * State of object stored at archival storage
 */
@Getter
public enum ObjectState {
    //phase before processing, during which, for example, the checksum of the object is verified after the transfer
    PRE_PROCESSING,
    //object is being processed (creating/deleting/rollback), used for both SIP and XML, stored also at storage layer
    PROCESSING,
    //object creation has finished and now is archived, used for both SIP and XML, stored also at storage layer
    ARCHIVED,
    //object processing has failed and so its file content was physically deleted from the storage, used for both SIP and XML, record itself remains in database and also in storage
    ROLLED_BACK,
    //object has been physically deleted from the storage, used only for SIP (XMLs cant be deleted), stored also at storage layer
    DELETED,
    //object has been logically removed: it exists in storage but should not be accessible to all users, used only for SIP (XMLs cant be removed), stored also at storage layer
    REMOVED,
    //object archiving has failed and following rollback has also failed, this state is held only in DB
    ARCHIVAL_FAILURE,
    //object deletion has failed, this state is held only in DB
    DELETION_FAILURE,
    //object rollback has failed, this state is held only in DB
    ROLLBACK_FAILURE;
}
