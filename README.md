Sling Devops Experiments, Volume 2: Atomic Switch of System Content
===================================================================

This is the second of a series of experiments about how [Apache Sling](http://sling.apache.org)
can be made more devops-friendly. 
 
Building on the atomic configuration switching mechanism of the previous 
[sling-devops-vol1](https://github.com/bdelacretaz/sling-devops-vol1) experiment, we add more 
realistic Sling configurations to the Sling instances, to demonstrate atomic switching of a setup 
that includes both scripts and OSGi services.  

For this set of experiments, the term "Sling configuration" includes everything that 
defines the behavior of a Sling instance: scripts, OSGi services, OSGi configurations, etc.

The general scenario is similar to the previous experiment, and we use the same 
components to implement the switching: cluster controller, HTTP front-end
and a load balancer configured by the cluster controller, Zookeeper for coordination and two
sets of Sling instances with Sling configurations C1 and C2. 

The difference with the previous experiment is that our Sling instances get an actual Sling configuration,
consisting of a rendering script that combines values provided by an OSGi service with repository content.

The demo scenario then upgrades both the OSGi service and rendering script and the test HTTP client verifies
that the resulting output switches atomically from C1 to C2.

![Sling instances configuration](./sling-devops-proto-2.png)

The diagram shows how the Sling instances are configured. They all share the same Oak repository,
but the C1 and C2 sets of instances are configured with different Sling search paths. For a simple example
like this that's sufficient to implement the different behavior of C1 and C2. Real applications might have
hardcoded the default Sling search paths (/libs and /apps) in places, and might
have spread system content outside of those paths. Both these issues will need to be addressed to use this
technique for real applications, before this atomic switching mechanism can be used.

