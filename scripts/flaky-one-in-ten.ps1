# Example for deflaker: fails with exit code 1 about 1 in 10 runs; otherwise exit 0.
# Paste into the app, e.g.: powershell -NoProfile -File "scripts\flaky-one-in-ten.ps1"
# (Run the app from the repo root so the path resolves.)

if ((Get-Random -Maximum 10) -eq 0) {
    exit 1
}
exit 0
