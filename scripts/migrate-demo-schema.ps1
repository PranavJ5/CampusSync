$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

if ([string]::IsNullOrWhiteSpace($env:CAMPSYNC_DEMO_MIGRATION_DB_USERNAME) -or
    [string]::IsNullOrWhiteSpace($env:CAMPSYNC_DEMO_MIGRATION_DB_PASSWORD)) {
    throw 'Set the local CAMPSYNC_DEMO_MIGRATION_DB_USERNAME and CAMPSYNC_DEMO_MIGRATION_DB_PASSWORD variables first.'
}

if ([string]::IsNullOrWhiteSpace($env:CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME)) {
    throw 'Set CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME to the independently verified local demo MySQL hostname.'
}

$confirmation = Read-Host 'Type campussync_demo to confirm this one-shot schema migration'
if ($confirmation -cne 'campussync_demo') {
    throw 'Confirmation did not match. No migration was started.'
}

$mavenArguments = @(
    '-q'
    '-DskipTests'
    '-Dexec.mainClass=com.campussync.config.DemoSchemaMigration'
    '-Dexec.classpathScope=runtime'
    '-Dexec.args=--confirm=campussync_demo'
    'compile'
    'org.codehaus.mojo:exec-maven-plugin:3.5.0:java'
)

& mvn.cmd @mavenArguments

if ($LASTEXITCODE -ne 0) {
    throw 'Demo migration command failed. Inspect the error and schema state before retrying.'
}
