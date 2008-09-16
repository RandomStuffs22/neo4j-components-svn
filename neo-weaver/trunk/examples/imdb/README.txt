This example replaces the existing domain layer in the IMDB application with
a lightweight version that doesn't contain any code plumbing.

To test NeoWeaver on the IMDB application you'll need to do the following:

  1. Add the dependency to the neo-weaver component in pom.xml:
  
		<dependency>
            <groupId>org.neo4j</groupId>
            <artifactId>neo-weaver</artifactId>
            <version>0.1-SNAPSHOT</version>
        </dependency>
  
    
  2. Strip the code out of the domain layer. Replace the existing sources by
     unzipping the source files located in domain-src.jar


  3. Add the following to src/main/webapp/WEB-INF/imdb-app-servlet.xml:

	<bean id="domainObjectFactory" class="org.neo4j.weaver.impl.CGLibDomainObjectFactory">
		<property name="neoService" ref="neoService" />
	</bean>

    The Spring object will be wired to the
    org.neo4j.apps.imdb.domain.ImdbServiceImpl class automatically.

  4. Compile and run.
  
  
Enjoy an easier way of creating Neo supported applications!

This version of the lib has been tested successfully with version 0.5-SNAPSHOT
of the IMDB application.