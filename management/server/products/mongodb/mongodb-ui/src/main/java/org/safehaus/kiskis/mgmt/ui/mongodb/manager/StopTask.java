/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.ui.mongodb.manager;

import java.util.UUID;
import org.safehaus.kiskis.mgmt.api.mongodb.Timeouts;
import org.safehaus.kiskis.mgmt.api.tracker.ProductOperationState;
import org.safehaus.kiskis.mgmt.api.tracker.ProductOperationView;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.NodeState;
import org.safehaus.kiskis.mgmt.ui.mongodb.MongoUI;

/**
 *
 * @author dilshat
 */
public class StopTask implements Runnable {

    private final String clusterName, lxcHostname;
    private final CompleteEvent completeEvent;

    public StopTask(String clusterName, String lxcHostname, CompleteEvent completeEvent) {
        this.clusterName = clusterName;
        this.lxcHostname = lxcHostname;
        this.completeEvent = completeEvent;
    }

    public void run() {

        UUID trackID = MongoUI.getMongoManager().stopNode(clusterName, lxcHostname);

        long start = System.currentTimeMillis();
        NodeState state = NodeState.UNKNOWN;

        while (!Thread.interrupted()) {
            ProductOperationView po = MongoUI.getMongoManager().getProductOperationView(trackID);
            if (po != null) {
                if (po.getState() != ProductOperationState.RUNNING) {
                    if (po.getState() == ProductOperationState.SUCCEEDED) {
                        state = NodeState.STOPPED;
                    }
                    break;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                break;
            }
            if (System.currentTimeMillis() - start > (Timeouts.STOP_NODE_TIMEOUT_SEC + 3) * 1000) {
                break;
            }
        }

        completeEvent.onComplete(state);
    }

}
