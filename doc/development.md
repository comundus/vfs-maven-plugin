## Tip for debugging

Often, the plugin has to be tested within a Maven call running in a separate JVM. This can be tested using `mvnDebug`
or adding this parameters to the JVM where the maven build is called:
 
    -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
    
Then, we can use remote debugging attaching to the port 8000.