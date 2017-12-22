package cz.inqool.uas.file;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileRefStoreTest {
    private FileRefStoreImpl store;

    @Before
    public void setUp() {
        store = new FileRefStoreImpl();
    }

    @Test
    public void toIndexObjectTest() {
        FileRef fileRef = new FileRef();
        fileRef.setName("name");

        IndexedFileRef indexedFileRef = store.toIndexObject(fileRef);

        assertThat(indexedFileRef.getName(), is(fileRef.getName()));
    }

    private class FileRefStoreImpl extends FileRefStore {
    }
}
