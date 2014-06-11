Sling Devops Experiments, Volume 4: Git-Driven Scenario
=======================================================

This is the fourth of a series of experiments about how [Apache Sling](http://sling.apache.org) can be made more devops-friendly.
 
Continuing from the Crankstart-based instance starting mechanism demonstrated in the previous [vol3](../../tree/vol3) experiment, we configure the Orchestrator to monitor a crank file in a Git repository and start new Sling instances for each new version of this file (representing a new Sling configuration). This essentially puts the cluster controller in charge of the running instances.

The tentative test scenario is as follows:

1. Start the demo with the Git repository URL of the crank file.
2. The Orchestrator watches that file in Git for new versions.
3. Once a new version `V` is found (or the initial version, when starting), the Orchestrator starts\* N Sling instances with that version.
4. The crankstart file has a few variables: Sling HTTP port number, MongoDB URL, ZooKeeper URL, etc.
5. The Sling instances announce themselves to the Orchestrator when ready.
6. Orchestrator has a target version `V` that it wants to expose via the HTTP front-end. Once N Sling instances have announced themselves with that target version, the Orchestrator activates them atomically on the front-end.
7. When old Sling instances are not needed anymore, they are killed\*.

(\*) *For the first demo, starting/stopping instances can just be a console message saying "please do this manually" - we can look at automating this later.*

Implementation
--------------

The main difference with the previous prototype is the introduction of a Git repository monitoring mechanism. This is achieved via the use of the [JGit](http://www.eclipse.org/jgit/) library.

The Orchestrator monitors a crank file in the repository, and, when a new version of it is available, downloads it into the `<SLING-HOME>/devops` directory. The filename (and the corresponding Sling config) are based on the commit timestamp. The next step is to start Minions from this crank file: in this prototype, this step must still be done manually; the Orchestrator simply prints what needs to be done.

Running
-------

Before running this prototype, it is necessary to prepare your local Maven repository as follows:

1. `mvn clean install` this project.
2. Build the Sling snapshot bundles.
  1. Checkout [Sling trunk](http://svn.apache.org/repos/asf/sling/trunk/) at revision 1601574.
  2. Apply [patch](https://issues.apache.org/jira/secure/attachment/12648482/SLING-3648.patch) from [SLING-3648](https://issues.apache.org/jira/browse/SLING-3648) to it.
  3. `mvn clean install` the following paths:
    1. `contrib/crankstart`
    2. `bundles/jcr/contentloader`
    3. `bundles/jcr/jackrabbit-server`
    4. `bundles/jcr/oak-server`
    5. `bundles/extensions/fsresource`
    6. `bundles/extensions/groovy`
    7. `contrib/launchpad/karaf`
3. Navigate to the `sample-bundle` directory and build two versions of the sample bundle:
  1. `mvn -P1 clean install`
  2. `mvn -P2 clean install`

### Vagrant

The easiest way to run the prototype is using [Vagrant](http://www.vagrantup.com/).

#### Environment

The current configuration creates five virtual machines running Ubuntu 12.04:
* 1 Orchestrator machine, `orchestrator` at 10.10.10.10, which also has the ZooKeeper server, the MongoDB server, the web server (httpd front-end), and the Git repository
* 2 Minion machines running the first config, `minion-C1-1` at 10.10.10.11 and `minion-C1-2` at 10.10.10.12
* 2 Minion machines running the second config, `minion-C2-1` at 10.10.10.21 and `minion-C2-2` at 10.10.10.22

Machines are set up by copying up the Crankstart Launcher JAR and crankstarting the Sling instances. `orchestrator` in addition is set up by configuring ZooKeeper, MongoDB, httpd, and Git.

The first Minion crank file is committed to the Git repository during setup of the `orchestrator`, the second during setup of each C2 `minion`.

#### Launching

The following is necessary to run the prototype with Vagrant:

1. Navigate to the `vagrant` directory.
2. Change `CRANKSTART_LAUNCHER_PATH` variable on line 123 of `Vagrantfile` to point to the Crankstart Launcher JAR built above.
3. If your local Maven repository is not under `~/.m2/repository`, change the variable `MAVEN_REPO_PATH` on line 124 accordingly.

Then, bring up the Orchestrator and the two Minion C1 machines:
```
vagrant up --provision orchestrator minion-C1-1 minion-C1-2
```

If everything goes well, after Vagrant is done, Sling with the first config should be at <http://10.10.10.10/>, and the output of the test script at <http://10.10.10.10/mynode.test>.

Afterwards, bring up the two Minion C2 machines:
```
vagrant up --provision minion-C2-1 minion-C2-2
```

Eventually <http://10.10.10.10/mynode.test> should switch to the second config.

Note: to re-provision machines that are already up, `vagrant provision` should be used instead of `vagrant up --provision`. Please `vagrant halt` the Minions before re-provisioning the Orchestrator.

Finally, machines can be brought down using
```
vagrant halt
```

or forever destroyed using
```
vagrant destroy
```

#### Troubleshooting

Vagrant machines can be `ssh`ed to using `vagrant ssh <machine>`. Crankstart keeps its log in the `~/crankstart-launcher.out` file (also printed by Vagrant during provisioning), and Sling keeps its log in the `~/<sling-home>/logs/error.log` file where `<sling-home>` is `sling-{orch|Cx-1|Cx-2}-crankstart`.

* If the `orchestrator` did not initialize properly, re-provision all machines.
* If one of the `minion`s did not initialize properly, re-provision it.
* If `orchestrator` is running and `10.10.10.10` is not pingable, try resetting the `vboxnet0` network interface on the host (`ifconfig vboxnet0 down` followed by `ifconfig vboxnet0 up`).

Testing
-------

To verify that the switch between the Sling configs is atomic (from the client point of view), the HttpResourceMonitor tool from the `tools` directory can be used. This tool sends an HTTP request over and over in a single thread and logs changes in responses.

Usage:
```
HttpResourceMonitor [host [resource]]
```

To monitor the output of our test script while running the prototype with Vagrant:
```
java -cp target/org.apache.sling.devops.tools-0.0.1-SNAPSHOT.jar org.apache.sling.devops.tools.HttpResourceMonitor 10.10.10.10 /mynode.test
```
