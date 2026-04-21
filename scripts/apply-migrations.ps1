param()

$ErrorActionPreference = "Stop"

& .\mvnw.cmd -q -DskipTests compile resources:resources dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"

$dependencyClasspath = (Get-Content target\classpath.txt -Raw).Trim()
$classpath = "target\classes;$dependencyClasspath"

java -cp $classpath com.wheelGo.tools.MigrationApplier
