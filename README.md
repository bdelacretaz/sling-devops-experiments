Sling Devops Experiments, Volume 2: Atomic Switch of System Content
===================================================================

This is the second of a series of experiments about how [Apache Sling](http://sling.apache.org)
can be made more devops-friendly. 
 
Building on the atomic configuration switching mechanism of the previous 
[vol1](../../tree/vol1) experiment, we add more 
realistic Sling configurations to the Sling instances, to demonstrate atomic switching of a setup 
that includes both scripts and OSGi services, along with a shared content repository.

Note that for this set of experiments the term "Sling configuration" includes everything that 
defines the behavior of a Sling instance: scripts, OSGi services, OSGi configurations, etc.

The general scenario is similar to the previous experiment, and we use the same 
components to implement the switching: cluster controller, HTTP front-end
and a load balancer configured by the cluster controller, Zookeeper for coordination and two
sets of Sling instances with Sling configurations C1 and C2. 

The difference with the previous experiment is that our Sling instances get an actual Sling configuration,
consisting of a rendering script that combines values provided by an OSGi service with repository content.

The demo scenario then upgrades both the OSGi service and rendering script and the test HTTP client verifies
that the resulting output switches atomically from C1 to C2.

![Sling instances configuration](./sling-devops-proto-2.jpg)

The diagram shows how the Sling instances are configured. They all share the same Oak repository,
but the C1 and C2 sets of instances are configured with different Sling search paths. For a simple example
like this that's sufficient to implement the different behavior of C1 and C2. Real applications might have
hardcoded the default Sling search paths (/libs and /apps) in places, and might
have spread system content outside of those paths. Both these issues will need to be addressed to use this
technique for real applications, before this atomic switching mechanism can be used.

Implementation
--------------

This prototype calls for three major differences with the previous prototype:
* **Shared Oak Repository**: implemented using Oak 0.18 with the MongoDB backend (`DocumentNodeStoreService` component).
  * This required changes to the existing Oak Server bundle as well as the Oak Core bundle (see 0.18 patch at [SLING-3479](https://issues.apache.org/jira/browse/SLING-3479) and the accompanying comment regarding [OAK-1581](https://issues.apache.org/jira/browse/OAK-1581)).
  * `DocumentNodeStoreService` can be configured using the [setup-minion-mongo.sh](scripts/setup-minion-mongo.sh) script.
* **Configuration of Search Paths**: must be done in two components, `JcrInstaller` and `JcrResourceResolverFactoryImpl`.
  * This is tricky to do manually because multiple instances of each may spawn, and instead can be done using the [setup-minion-configs.sh](scripts/setup-minion-configs.sh) script.
* **Actual Sling Configuration**: modelled by two versions of a test bundle with a Sling service and two versions of a test script using the service. Once the search paths are configured, bundle JARs can simply be put under `/sling-cfg/<config>/apps/install` to be installed, and scripts under `/sling-cfg/<config>/apps`.
  * This mechanism can also be leveraged to convert a standard standalone Launchpad instance to a Minion instance.
  * The [setup-minion-repo.sh](scripts/setup-minion-repo.sh) script can be used to convert an instance to a Minion instance and to upload the appropriate version of the test bundle and the test script to it. The output of the test script can then be seen at `http://<sling>/mynode.test`.

Additionally, while in the previous prototype Minion instances were announcing themselves to ZooKeeper immediately upon startup, here they perform several self-tests to check whether they are ready before doing that. This mechanism is achieved using the [Sling Health Checks](http://sling.apache.org/documentation/bundles/sling-health-check-tool.html) based on JUnit tests. The following tests have been implemented:
* `ComponentTest` verifies that both `JcrInstaller` and `JcrResourceResolverFactoryImpl` components are present and active
* `SearchPathTest` verifies that both components are configured with the correct search paths
* `BundleTest` verifies that all bundles are active and all bundle fragments are resolved (as a way to see if `JcrInstaller` installed new bundles from each new search path; it is therefore assumed that the `minion` bundle itself is installed from there)
