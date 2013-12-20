package org.safehaus.kiskis.mgmt.server.agent;

//import org.osgi.framework.BundleContext;
import org.safehaus.kiskis.mgmt.shared.protocol.*;
import org.safehaus.kiskis.mgmt.shared.protocol.api.*;
import org.safehaus.kiskis.mgmt.shared.protocol.api.ui.AgentListener;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.RequestType;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.TaskStatus;
import org.safehaus.kiskis.mgmt.shared.protocol.settings.Common;

/**
 * Created with IntelliJ IDEA. User: daralbaev Date: 11/7/13 Time: 11:11 PM
 */
public class AgentManager implements AgentManagerInterface, ResponseListener {

    private static final Logger LOG = Logger.getLogger(AgentManager.class.getName());
    private PersistenceInterface persistenceAgent;
    private CommandManagerInterface commandManager;
    private CommandTransportInterface commandTransportInterface;
    private final Queue<AgentListener> listeners = new ConcurrentLinkedQueue<AgentListener>();
    private ExecutorService exec;
    private int heartbeatTimeoutSec;
    private int heartbeatFromMin;
    private int heartbeatToMin;
    private int agentFreshnessMin;
    private AgentNotifier agentNotifier;

    public AgentManager() {
    }

    @Override
    public void onResponse(Response response) {
        switch (response.getType()) {
            case REGISTRATION_REQUEST: {
                updateAgent(response, true);
                break;
            }
            case HEARTBEAT_RESPONSE: {
                updateAgent(response, false);
            }
            default: {
                break;
            }
        }
    }

    @Override
    public Agent getAgent(UUID uuid) {
        return persistenceAgent.getAgent(uuid);
    }

    private void updateAgent(Response response, boolean register) {
        try {
            if (response.getUuid() == null) {
                throw new Exception("Error " + (register ? "registering" : "updating") + " agent: UUID is null " + response);
            }
            Agent agent = new Agent();
            agent.setUuid(response.getUuid());
            agent.setHostname(response.getHostname());
            agent.setMacAddress(response.getMacAddress());
            if (response.isIsLxc() == null) {
                agent.setIsLXC(false);
            } else {
                agent.setIsLXC(response.isIsLxc());
            }
            agent.setListIP(response.getIps());
            if (agent.getHostname() == null || agent.getHostname().trim().isEmpty()) {
                agent.setHostname(agent.getUuid().toString());
            }
            if (agent.isIsLXC()) {
                if (agent.getHostname() != null && agent.getHostname().matches(".+" + Common.PARENT_CHILD_LXC_SEPARATOR + ".+")) {
                    agent.setParentHostName(agent.getHostname().substring(0, agent.getHostname().indexOf(Common.PARENT_CHILD_LXC_SEPARATOR)));
                } else {
                    agent.setParentHostName(Common.UNKNOWN_LXC_PARENT_NAME);
                }
            }

            if (persistenceAgent.saveAgent(agent)) {
                if (register) {
                    Task task = new Task();
                    task.setDescription("Agent registration");
                    task.setTaskStatus(TaskStatus.NEW);
                    task.setReqSeqNumber(0);
                    commandManager.saveTask(task);
                    response.setTaskUuid(task.getUuid());
                    response.setRequestSequenceNumber(task.getReqSeqNumber());
                    persistenceAgent.saveResponse(response);
                    //
                    Request request = new Request();
                    request.setTaskUuid(task.getUuid());
                    request.setRequestSequenceNumber(task.getReqSeqNumber());
                    request.setType(RequestType.REGISTRATION_REQUEST_DONE);
                    request.setUuid(agent.getUuid());
                    request.setSource(response.getSource());
                    request.setStdErr(OutputRedirection.NO);
                    request.setStdOut(OutputRedirection.NO);
                    Command command = new Command(request);
                    commandManager.executeCommand(command);
                    //
                    task.setTaskStatus(TaskStatus.SUCCESS);
                    persistenceAgent.saveTask(task);
                    //
                }
                agentNotifier.refresh = true;
                LOG.log(Level.INFO, String.format("Agent %s is %s", agent.getHostname(), register ? "registered" : "updated"));
            } else {
                LOG.log(Level.WARNING, String.format("\nError %s agent %s", register ? "registering" : "updating", agent.getHostname()));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in updatAgent", ex);
        }
    }

    @Override
    public void addListener(AgentListener listener) {
        try {
            listeners.add(listener);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in addListener", ex);
        }
    }

    @Override
    public void removeListener(AgentListener listener) {
        try {
            listeners.remove(listener);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in removeListener", ex);
        }
    }

    public void init() {
        try {
            if (commandTransportInterface != null) {
                exec = Executors.newFixedThreadPool(2);
                exec.execute(new AgentHeartBeat(this, commandTransportInterface, heartbeatTimeoutSec));
                agentNotifier = new AgentNotifier(this, listeners);
                exec.execute(agentNotifier);
                commandTransportInterface.addListener(this);
            } else {
                throw new Exception("Missing communication service");
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in init", ex);
        }
    }

    public void destroy() {
        try {
            exec.shutdownNow();
            if (commandTransportInterface != null) {
                commandTransportInterface.removeListener(this);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in destroy", ex);
        }
    }

    public void setPersistenceAgentService(PersistenceInterface persistenceAgent) {
        this.persistenceAgent = persistenceAgent;
    }

    public void setCommandManagerService(CommandManagerInterface commandManager) {
        this.commandManager = commandManager;

    }

    public void setCommandTransportInterface(CommandTransportInterface commandTransportInterface) {
        this.commandTransportInterface = commandTransportInterface;
    }

    public void setHeartbeatTimeoutSec(int heartbeatTimeoutSec) {
        this.heartbeatTimeoutSec = heartbeatTimeoutSec;
    }

    public void setHeartbeatFromMin(int heartbeatFromMin) {
        this.heartbeatFromMin = heartbeatFromMin;
    }

    public void setHeartbeatToMin(int heartbeatToMin) {
        this.heartbeatToMin = heartbeatToMin;
    }

    public void setAgentFreshnessMin(int agentFreshnessMin) {
        this.agentFreshnessMin = agentFreshnessMin;
    }

    @Override
    public List<Agent> getRegisteredAgents() {
        return persistenceAgent.getRegisteredAgents(agentFreshnessMin);
    }

    @Override
    public List<Agent> getAgentsToHeartbeat() {
        return persistenceAgent.getAgentsByHeartbeat(heartbeatFromMin, heartbeatToMin);
    }

    public List<Agent> getRegisteredLxcAgents() {
        return persistenceAgent.getRegisteredLxcAgents(agentFreshnessMin);
    }

    public List<Agent> getRegisteredPhysicalAgents() {
        return persistenceAgent.getRegisteredPhysicalAgents(agentFreshnessMin);
    }

    /**
     * assume the following: lets say that physical agent's hostname is "py01"
     * then its child lxc agents will be like "py01_lxc_hadoop-node-1"
     *
     * @param physicalAgent - physical agent
     * @return child lxc agents of a physical agent
     */
    public List<Agent> getChildLxcAgents(Agent physicalAgent) {
        return persistenceAgent.getRegisteredChildLxcAgents(physicalAgent, agentFreshnessMin);
    }

    public List<Agent> getUnknownChildLxcAgents() {
        return persistenceAgent.getUnknownChildLxcAgents(agentFreshnessMin);
    }
}
