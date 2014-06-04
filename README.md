Sling Devops Experiments, Volume 4: Git-Driven Scenario
=======================================================

This is the fourth of a series of experiments about how [Apache Sling](http://sling.apache.org) can be made more devops-friendly.
 
Continuing from the Crankstart-based instance starting mechanism demonstrated in the previous [vol3](../../tree/vol3) experiment, we configure the Orchestrator to monitor a crank file in a Git repository and start new Sling instances for each new version of this file (representing a new Sling configuration). This essentially puts the cluster controller in charge of the running instances.

The tentative test scenario is as follows:

# Start the demo with the Git repository URL of the crank file.
# The Orchestrator watches that file in Git for new versions.
# Once a new version `V` is found (or the initial version, when starting), the Orchestrator starts\* N Sling instances with that version.
# The crankstart file has a few variables: Sling HTTP port number, MongoDB URL, ZooKeeper URL, etc.
# The Sling instances announce themselves to the Orchestrator when ready.
# Orchestrator has a target version `V` that it wants to expose via the HTTP front-end. Once N Sling instances have announced themselves with that target version, the Orchestrator activates them atomically on the front-end.
# When old Sling instances are not needed anymore, they are killed\*.

(\*) *For the first demo, starting/stopping instances can just be a console message saying "please do this manually" - we can look at automating this later.*
