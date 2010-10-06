pom-for-building-all-trunks.xml
  This file can be used to build all component trunks in one go.
  
  It has different contents on different git branches. Check out the one
  you are interested in before building, for example:
  
  git checkout apoc-components-work
  
  or
  
  git checkout some-more-components-work
  
  
  This file is useful for testing that all modules are consistent.
  However, when testing that, you should make sure your repository is
  clean according to these steps:
  
  First, make sure you have no proxies activated in ~/.m2/settings.xml.

  mvn clean -f pom-for-building-all-trunks.xml
  rm -rf ~/.m2/repository
  mvn install -f parent/trunk/pom.xml
  mvn install -f pom-for-building-all-trunks.xml
