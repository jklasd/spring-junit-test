mvn clean source:jar javadoc:jar package gpg:sign deploy -s E:\maven-3.8.5\conf\spring-junit-test.xml

pause

 -- mvn deploy:deploy-file -DgroupId=com.github.jklasd -DartifactId=spring-junit-dubbo-test -Dversion=2.0.1-RELEASE -Dfile=spring-junit-dubbo-test-2.0.1-RELEASE.jar.asc -DrepositoryId=comgithubjklasd-1030 -Durl=https://s01.oss.sonatype.org


mvn clean source:jar javadoc:jar package gpg:sign deploy -s E:\maven-3.8.5\conf\spring-junit-test.xml