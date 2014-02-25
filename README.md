# Sling Devops Experiments, volume 1

This is the first of a series of experiments about how Sling can be made more devops-friendly.

The goal of this experiment is to implement a simple cluster controller that detects available
Sling instances, and controls an HTTP load balancer front end to implement an atomic switch
between two Sling configurations C1 and C2.

The difference between C1 and C2 can be just a different rendering of /index.html for example,
the goal is only to verify that HTTP clients accessing the front-end see only C1 responses up
to a certain point in time, and only C2 responses after that, with continuous service during
the switch.

The attached diagram explains the scenario. To keep this experiment as simple as possible
we can start the Sling instances manually with the appropriate configs.

![System structure and scenario](./sling-devops-vol1.jpg)

Each Sling instance announces itself to Zookeeper with its IP adress and port, indicating
which config it is running. A Sling StartupListener service can be used to trigger this
announce when Sling is ready.

The Sling instances can run on different hosts, as long as they can access Zookeeper and
as long as the front-end can access them.

The cluster controller first waits for N instances to be available with the C1 config, and
configures the load balancer to use those once they are available.

Later, we start N additional instances with the C2 config, which also announce themselves
to Zookeeper. 

When the cluster controller detects that N C2 instances are active, it reconfigures the
load balancer to switch to them atomically, so that front-end clients see only C2 responses
from this point on.

The C1 instances can then be shutdown, as soon as they done processing any outstanding
requests.

To verify the scenario, the HTTP clients should verify that they see an atomic switch
from C1 to C2 with no interruption.
