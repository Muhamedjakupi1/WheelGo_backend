param(
    [Parameter(Mandatory = $true)]
    [string]$Name,
    [string]$TenantSchema,
    [switch]$Apply
)

$ErrorActionPreference = "Stop"

& .\mvnw.cmd -q -DskipTests compile dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"

$dependencyClasspath = (Get-Content target\classpath.txt -Raw).Trim()
$classpath = "target\classes;$dependencyClasspath"

$javaArgs = @($Name)
if ($TenantSchema) {
    $javaArgs += "--tenant-schema=$TenantSchema"
}

java -cp $classpath com.wheelGo.tools.MigrationScaffoldGenerator @javaArgs

& .\mvnw.cmd -q -DskipTests resources:resources dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"

$dependencyClasspath = (Get-Content target\classpath.txt -Raw).Trim()
$classpath = "target\classes;$dependencyClasspath"

if ($Apply) {
    java -cp $classpath com.wheelGo.tools.MigrationApplier
}
