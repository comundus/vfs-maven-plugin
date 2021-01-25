**Attention:**
The plugin is not running with Maven Version > 3.5.4.

---

To use `vfs-maven-plugin` in the build, following repository has to be defined in your Maven `settings.xml`

	<repository>
		<id>maven-vfs-plugin-github</id>
		<url>https://comundus.github.io/maven2-repository</url>
	</repository>
    
This other one is possibly not needed anymore
    
    <repository>
        <id>maven2-repository.dev.java.net</id>
        <name>Java.net Repository for Maven</name>
        <url>http://download.java.net/maven/2/</url>
        <layout>default</layout>
    </repository>