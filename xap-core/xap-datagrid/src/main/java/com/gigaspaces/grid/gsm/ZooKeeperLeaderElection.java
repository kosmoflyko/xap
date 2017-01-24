package com.gigaspaces.grid.gsm;

/**
 * Created by moran on 1/24/17.
 */
public interface ZooKeeperLeaderElection {

    /**
     * The selection for this instance doesn't start until the leader selector is started
     */
    void start();

    /**
     * @return {@code true} if this participant is elected as leader
     */
    boolean hasLeadership();

    /**
     * close any resources held by this handler
     */
    void close();
}
