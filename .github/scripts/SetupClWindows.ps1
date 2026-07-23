if ($(Split-Path -Path (Get-Location) -Leaf) -eq "scripts" ) {
    Set-Location ..
}

Write-Output "Writing ci gradle.properties"
$GradleUserHomeDir = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE ".gradle" }
if (!(Test-Path -Path $GradleUserHomeDir)) {
    New-Item -ItemType Directory -Force -Path $GradleUserHomeDir -ErrorAction SilentlyContinue
}
Copy-Item ".github/runner-files/ci-gradle.properties" (Join-Path $GradleUserHomeDir "gradle.properties") -Force