package org.safehaus.subutai.core.container.ui.executor;

/**
 * Created by timur on 9/8/14.
 */
public interface AgentCommand {
    public void execute() throws AgentExecutionException;
}