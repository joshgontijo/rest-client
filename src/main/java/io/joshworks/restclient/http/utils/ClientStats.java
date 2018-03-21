package io.joshworks.restclient.http.utils;

import org.apache.http.pool.PoolStats;

/**
 * Created by Josh Gontijo on 3/21/18.
 */
public class ClientStats {

    public final PoolStats sync;
    public final PoolStats async;

    public ClientStats(PoolStats sync, PoolStats async) {
        this.sync = sync;
        this.async = async;
    }

    @Override
    public String toString() {
        return "ClientStats {" +
                "SYNC=" + sync +
                ", ASYNC=" + async +
                '}';
    }
}
