Sling Devops Experiments, Volume 3: Crankstarting Sling Instances
=================================================================

This is the third of a series of experiments about how [Apache Sling](http://sling.apache.org)
can be made more devops-friendly. 
 
Building on the atomic switching of system content mechanism demonstrated in the previous [vol2](../../tree/vol2) experiment, we configure our Sling instances to start from simple [Crankstart](http://svn.apache.org/repos/asf/!svn/bc/1595780/sling/trunk/contrib/crankstart/) files rather than as Stand-alone Launchpad instances. This makes it easier to configure instances and install additional bundles, thus eliminating the need to do this via external shell scripts.

As in the previous experiment, our Sling instances get an actual Sling configuration, consisting of a rendering script that combines values provided by an OSGi service with repository content. The demo scenario then upgrades both the OSGi service and rendering script and the test HTTP client verifies that the resulting output switches atomically from C1 to C2.

![Sling instances configuration](./sling-devops-proto-2.jpg)

The diagram shows how the Sling instances are configured. They all share the same Oak repository,
but the C1 and C2 sets of instances are configured with different Sling search paths. For a simple example
like this that's sufficient to implement the different behavior of C1 and C2. Real applications might have
hardcoded the default Sling search paths (`/libs` and `/apps`) in places, and might
have spread system content outside of those paths. Both these issues will need to be addressed to use this
technique for real applications, before this atomic switching mechanism can be used.

Implementation
--------------

This prototype calls for two major differences with the previous prototype:
* **Crankstart Files**: five `.crank.txt` files have been added, each describing one of the Sling instances of the prototype. Each includes a list of all bundles of a standard Stand-alone Launchpad instance, along with bundles from Oak and this project. The crank files for Minion instances are practically identical except the default values for the config and HTTP port.
* **Fewer Scripts**: Crankstart greatly simplifies Sling configuration, so most shell scripts have become obsolete except those that load config-specific content into the repository.

Running
-------

Before running this prototype, it is necessary to prepare your local Maven repository as follows:

1. `mvn clean install` this project.
2. Oak Core 0.18
  1. Checkout [Oak Core 0.18](http://svn.apache.org/repos/asf/jackrabbit/oak/tags/jackrabbit-oak-0.18/oak-core/).
  2. Manually apply fix from [OAK-1581](https://issues.apache.org/jira/browse/OAK-1581) to it.
  3. `mvn clean install` it.
3. Sling snapshot bundles
  1. Checkout [Sling trunk](http://svn.apache.org/repos/asf/sling/trunk/) at revision 1595780.
  2. Apply [patch](https://issues.apache.org/jira/secure/attachment/12636928/SLING-3479-Oak018.patch) from [SLING-3479](https://issues.apache.org/jira/browse/SLING-3479) to it.
  3. `mvn clean install` the following paths:
    1. `contrib/crankstart`
    2. `bundles/jcr/oak-server`
    3. `bundles/extensions/groovy`
    4. `contrib/launchpad/karaf`

### Vagrant

The easiest way to run the prototype is using [Vagrant](http://www.vagrantup.com/).

#### Environment

The current configuration creates five virtual machines running Ubuntu 12.04:
* 1 Orchestrator machine, `orchestrator` at 10.10.10.10, which is also the ZooKeeper server, the MongoDB server, and the web server (httpd front-end)
* 2 Minion machines with config C1, `minion-C1-1` at 10.10.10.11 and `minion-C1-2` at 10.10.10.12
* 2 Minion machines with config C2, `minion-C2-1` at 10.10.10.21 and `minion-C2-2` at 10.10.10.22

Machines are set up by copying up the Crankstart Launcher JAR and crankstarting the Sling instances. `orchestrator` in addition is set up by configuring ZooKeeper, MongoDB, and httpd.

#### Launching

The following is necessary to run the prototype with Vagrant:

1. Navigate to `vagrant` directory.
2. Change `CRANKSTART_LAUNCHER_PATH` variable on line 123 of `Vagrantfile` to point to the Crankstart Launcher JAR built above.
3. If your local Maven repository is not under `~/.m2/repository`, change the variable `MAVEN_REPO_PATH` on line 124 accordingly.

Then, to bring up the Orchestrator and the two Minion C1 machines:
```
vagrant up --provision orchestrator minion-C1-1 minion-C1-2
```

If everything goes well, after Vagrant is done Sling with config C1 should be at <http://10.10.10.10/>, and the output of the test script at <http://10.10.10.10/mynode.test>.

Afterwards, to bring up the two Minion C2 machines:
```
vagrant up --provision minion-C2-1 minion-C2-2
```

Eventually Sling at <http://10.10.10.10/> should switch to config C2.

Note: to re-provision machines that are already up, `vagrant provision` should be used instead of `vagrant up --provision`.

Finally, machines can be brought down using
```
vagrant halt
```

or forever destroyed using
```
vagrant destroy
```

#### Troubleshooting

Vagrant machines can be `ssh`ed to using `vagrant ssh <machine>`. Crankstart keeps its log in the `~/crankstart-launcher.out` file (also printed by Vagrant during provisioning), and Sling keeps its log in the `~/<sling-home>/logs/error.log` file where `<sling-home>` is `sling-{orch|C1-1|C1-2|C2-1|C2-2}-crankstart`.

If not everything goes well, <http://10.10.10.10/> will be inaccessible, which could be due to network problems:

* If `orchestrator` is running and `10.10.10.10` is not pingable, try resetting the `vboxnet0` network interface on the host (`ifconfig vboxnet0 down` followed by `ifconfig vboxnet0 up`).

Testing
-------

To verify that the switch between the Sling configs is atomic (from the client point of view), the HttpResourceMonitor tool from the `org.apache.sling.devops.tools` module can be used. This tool sends an HTTP request over and over in a single thread and logs changes in responses.

Usage:
```
HttpResourceMonitor [host [resource]]
```

To monitor the output of our test script while running the prototype with Vagrant:
```
HttpResourceMonitor 10.10.10.10 /mynode.test
```
