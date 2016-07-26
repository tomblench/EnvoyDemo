package cloudant.com.envoydemo;

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreException;
import com.cloudant.sync.datastore.DatastoreManager;

/**
 * Created by tomblench on 26/07/2016.
 */
abstract public class DatastoreHelper<R> {

    Datastore ds;

    public DatastoreHelper(DatastoreManager dsm, String user) throws DatastoreException {
        ds = dsm.openDatastore(user+"-db");
    }

    public R run() throws Exception {
        try (AutoCloseableDatastore acds = new AutoCloseableDatastore(ds)) {
            return performOnDatastore(acds.ds);
        } catch (Exception e) {
            ;
        }
        return null;
    }

    abstract R performOnDatastore(Datastore d) throws Exception;

    private class AutoCloseableDatastore implements AutoCloseable {

        final public Datastore ds;

        public AutoCloseableDatastore(Datastore ds) {
            this.ds = ds;
        }

        @Override
        public void close() {
            ds.close();
        }

    }

}
