package org.apache.sling.devops;

import java.util.Set;

/** service that starts and stops minions */
public interface MinionsController {
    /** Start a number of minions with specified config */
    void startMinions(final String config, final String crankFilePath, final int num);

    /** Stop all minions that currently run the specified config */
    void stopMinions(String config, Set<String> endPoints);
}
