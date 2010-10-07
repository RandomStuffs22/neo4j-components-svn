pom-for-building-all-trunks.xml
  This file can be used to build all component trunks in one go.
  
  It is useful for testing that all modules are consistent.
  However, when testing that, you should make sure your repository is
  clean according to these steps:
  
  First, make sure you have no proxies activated in ~/.m2/settings.xml.

  mvn clean -f pom-for-building-all-trunks.xml
  rm -rf ~/.m2/repository
  mvn install -f ../community/maven-skin/tags/1.0.3/pom.xml
  mvn install -f parent-central/tags/1/pom.xml
  mvn install -f pom-for-building-all-trunks.xml
