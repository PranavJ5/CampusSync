param(
    [switch]$VerifyConsole,
    [switch]$CommitteeReferencesOnly
)

$ErrorActionPreference = 'Stop'
if ($VerifyConsole -and $CommitteeReferencesOnly) {
    throw 'Choose either -VerifyConsole or -CommitteeReferencesOnly, not both.'
}
Set-Location (Join-Path $PSScriptRoot '..')

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($null -eq $maven) {
    throw 'Maven (mvn.cmd) must be available on PATH.'
}
$java = Get-Command java.exe -ErrorAction SilentlyContinue
if ($null -eq $java) {
    throw 'Java (java.exe) must be available on PATH.'
}

$classpathFile = Join-Path ([System.IO.Path]::GetTempPath()) (
    'campussync-demo-classpath-' + [guid]::NewGuid().ToString('N') + '.txt')
$environmentNames = @(
    'CAMPSYNC_DEMO_SEED_DB_USERNAME',
    'CAMPSYNC_DEMO_SEED_DB_PASSWORD',
    'CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME'
)
$originalEnvironment = @{}
foreach ($name in $environmentNames) {
    $originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$securePassword = $null
$passwordPointer = [IntPtr]::Zero
try {
    $mavenArguments = @(
        '-q'
        '-DskipTests'
        "-Dmdep.outputFile=$classpathFile"
        '-Dmdep.includeScope=runtime'
        'compile'
        'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath'
    )
    & $maven.Source @mavenArguments
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $classpathFile)) {
        throw 'Could not compile the seeder and build its runtime classpath.'
    }

    $dependencyClasspath = [System.IO.File]::ReadAllText($classpathFile).Trim()
    if ([string]::IsNullOrWhiteSpace($dependencyClasspath)) {
        throw 'Maven produced an empty seeder runtime classpath.'
    }
    $runtimeClasspath = "target\classes;$dependencyClasspath"
    $consoleCheckArguments = @(
        '-cp'
        $runtimeClasspath
        'com.campussync.demo.DemoDataSeeder'
        '--verify-console'
    )
    & $java.Source @consoleCheckArguments
    if ($LASTEXITCODE -ne 0) {
        throw 'Java has no interactive console. Run this launcher directly in an interactive terminal.'
    }
    if ($VerifyConsole) {
        return
    }

    $username = Read-Host 'Demo seeder MySQL username (must have only approved campussync_demo grants)'
    $expectedHostname = Read-Host 'Independently verified MySQL server hostname'
    if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($expectedHostname)) {
        throw 'Seeder username and independently verified MySQL hostname are required.'
    }

    $securePassword = Read-Host 'Demo seeder MySQL password' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)

    $confirmationPrompt = if ($CommitteeReferencesOnly) {
        'Type campussync_demo to authorize ensuring the demo committee reference data'
    } else {
        'Type campussync_demo to authorize the synthetic data seeder'
    }
    $confirmation = Read-Host $confirmationPrompt
    if ($confirmation -cne 'campussync_demo') {
        throw 'Confirmation did not match. No demo data was seeded.'
    }

    $env:CAMPSYNC_DEMO_SEED_DB_USERNAME = $username
    $env:CAMPSYNC_DEMO_SEED_DB_PASSWORD = $plainPassword
    $env:CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME = $expectedHostname

    $seederModeArguments = if ($CommitteeReferencesOnly) {
        @('--ensure-demo-committee-references', '--confirm=campussync_demo')
    } else {
        @('--confirm=campussync_demo')
    }
    $seederArguments = @('-cp', $runtimeClasspath, 'com.campussync.demo.DemoDataSeeder') + $seederModeArguments
    & $java.Source @seederArguments
    if ($LASTEXITCODE -ne 0) {
        throw 'Demo seeder failed. Inspect the database state before any retry.'
    }
}
finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    if ($securePassword -ne $null) {
        $securePassword.Dispose()
    }
    Remove-Item -LiteralPath $classpathFile -Force -ErrorAction SilentlyContinue
    Remove-Variable plainPassword -ErrorAction SilentlyContinue
    foreach ($name in $environmentNames) {
        $original = $originalEnvironment[$name]
        if ($null -eq $original) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($name, $original, 'Process')
        }
    }
}
