/*
 * Copyright 2016 Sudipto Chandra.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tuntuni.connection;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import org.tuntuni.models.Logs;
import org.tuntuni.util.SocketUtils;

/**
 * Search all subnet masks for active users and update user-list periodically.
 */
public class Subnet {

    // logger
    private static final Logger logger = Logger.getGlobal();

    public static final int SCAN_START_DELAY_MILLIS = 1_000;
    public static final int SCAN_INTERVAL_MILLIS = 15_000;
    public static final int REACHABLE_THREAD_COUNT = 256;
    public static final int REACHABLE_TIMEOUT_MILLIS = 500;

    private final StringProperty mState;
    private final ExecutorService mExecutor;
    private final ScheduledExecutorService mSchedular;
    private final SimpleMapProperty<String, Client> mUserList;

    /**
     * Creates a new instance of Subnet.
     */
    public Subnet() {
        mState = new SimpleStringProperty(UUID.randomUUID().toString());
        mUserList = new SimpleMapProperty<>(FXCollections.observableHashMap());
        mExecutor = Executors.newFixedThreadPool(REACHABLE_THREAD_COUNT);
        mSchedular = Executors.newSingleThreadScheduledExecutor();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties and public methods
    ////////////////////////////////////////////////////////////////////////////    
    /**
     * Gets the current state of the subnet. Every time a client arrives or
     * leaves, the state changes.
     *
     * @return
     */
    public StringProperty stateProperty() {
        return mState;
    }

    /**
     * Gets the read-only user list property.
     * <p>
     * You can bind or attach listener to this property, but since it is
     * readonly, you can not change the values.</p>
     *
     * @return
     */
    public SimpleMapProperty<String, Client> userListProperty() {
        return mUserList;
    }

    /**
     * Search for the user client on the user list and returns the client if
     * found, otherwise a null value is returned
     *
     * @param address Address of the user to search for
     * @return null if not found.
     */
    public Client getClient(String address) {
        synchronized (mUserList) {
            return mUserList.get(address);
        }
    }

    /**
     * Adds the address as client if not already exists
     *
     * @param address Address to check
     */
    public void addAsClient(String address) {
        mExecutor.submit(() -> checkAddress(address, true));
    }

    ////////////////////////////////////////////////////////////////////////////
    // MOST IMPORTANT: Start or stop subnet scans
    ////////////////////////////////////////////////////////////////////////////    
    /**
     * Start the periodic task that scan through all possible subnets.
     * <p>
     * After calling the {@linkplain start()} method, the first scan starts
     * after around {@value #SCAN_START_DELAY_MILLIS} milliseconds and repeat
     * once every {@value #SCAN_INTERVAL_MILLIS} milliseconds. </p>
     */
    public void start() {
        // start periodic check to get active user list        
        mSchedular.scheduleAtFixedRate(performScan, SCAN_START_DELAY_MILLIS,
                SCAN_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancel the scheduled task.
     */
    public void stop() {
        mExecutor.shutdownNow();
        mSchedular.shutdownNow();
    }

    // to scan over whole subnet of all networks for active users
    // if new network interfaces are added, it also includes them on the fly
    private final Runnable performScan = () -> {
        logger.log(Level.INFO, Logs.SUBNET_SCAN_START);
        try {
            // get all network interfaces
            Enumeration<NetworkInterface> ne
                    = NetworkInterface.getNetworkInterfaces();
            // loop through all of them
            while (ne.hasMoreElements()) {
                checkNetworkInterface(ne.nextElement());
            }
            logger.log(Level.INFO, Logs.SUBNET_SCAN_SUCCESS);
        } catch (SocketException ex) {
            logger.log(Level.SEVERE, Logs.SUBNET_SCAN_FAILED, ex);
        }

    };

    // check all address avaiable in a network interface
    private void checkNetworkInterface(NetworkInterface netFace) {
        try {
            // loopbacks are not necessary to check
            // only check the interface that is up or active.
            if (netFace.isLoopback() || !netFace.isUp()) {
                return;
            }

            // list of all tasks
            ArrayList<Callable<Integer>> taskList = new ArrayList<>();

            // loop through addresses assigned to this interface. (usually 1)
            netFace.getInterfaceAddresses().stream().forEach((ia) -> {
                // get network address
                InetAddress address = ia.getAddress();

                // address must be an IPv4 address.
                // and it should be a site local address.
                // --- Site Local Address ---
                // These have the scope of an entire site, or organization. 
                // They allow addressing within an organization without need for
                // using a public prefix. 
                // Routers will forward datagrams using site-local addresses 
                // within the site, but not outside it to the public Internet.
                if (!address.isSiteLocalAddress()
                        || !(address instanceof Inet4Address)) {
                    return;
                }

                logger.log(Level.INFO, Logs.SUBNET_CHECKING_SUBNETS, address.getHostAddress());

                // get network's first and last hosts
                int prefix = ia.getNetworkPrefixLength();
                int current = SocketUtils.addressAsInteger(address);
                int first = SocketUtils.getFirstHost(address, prefix);
                int last = SocketUtils.getLastHost(address, prefix);
                /*
                 System.out.println("Prefix Length: " + prefix);
                 System.out.println("Current Address : " + SocketUtils.addressAsString(current));
                 System.out.println("  First Address : " + SocketUtils.addressAsString(first));
                 System.out.println("   Last Address : " + SocketUtils.addressAsString(last));
                 */
                // find all active hosts in the same local network
                for (int ip = first; ip <= last; ++ip) {
                    // skip current address
                    if (ip == current) {
                        continue;
                    }
                    String host = SocketUtils.addressAsString(ip);
                    taskList.add(checkAddress(host, false));
                }
            });

            // wait until all tasks are done    
            mExecutor.invokeAll(taskList);
            changeState();

        } catch (SocketException ex) {
            logger.log(Level.SEVERE, Logs.SUBNET_INTERFACE_CHECK_ERROR, ex);
        } catch (InterruptedException iex) {
            //logger.log(Level.SEVERE, null, ex);
        }
    }

    // check if the given address is active.    
    private Callable<Integer> checkAddress(String address, boolean changeStateNow) {
        // returns 1 only when something changes
        return () -> {
            // the address might be on the list. check it first
            Client real = getClient(address);
            if (real != null && real.checkServer()) {
                return 0;     // retains connection
            }
            // try new connection with the server 
            int i = Server.PORTS.length - 1;
            for (; i >= 0; --i) {
                Client sub = new Client(
                        new InetSocketAddress(address, Server.PORTS[i]));
                if (sub.checkServer()) {
                    addUser(sub);   // new connection
                    break;
                }
            }
            if (real == null || i < 0) {
                return 0;  // no change
            }
            // has change
            if (changeStateNow) {
                changeState(); // update change
            }
            return 1;
        };
    }

    private synchronized void addUser(Client client) {
        Platform.runLater(() -> {
            mUserList.put(client.getHostString(), client);
        });
    }

    private synchronized void changeState() {
        Platform.runLater(() -> {
            mState.set(UUID.randomUUID().toString());
        });
    }

}
