<#
.SYNOPSIS
    Compute the SHA-256 SubjectPublicKeyInfo (SPKI) pin for a remote host.

.DESCRIPTION
    Connects to <HostName>:<Port> over TLS, captures the certificate chain
    the server presented, then prints the base64 SHA-256 of each
    certificate's SPKI. Paste the leaf value into
    network_security_config.xml and the intermediate value as the backup.

    Always pin two values. The simplest backup is the intermediate CA SPKI,
    which rotates far less often than the leaf.

.PARAMETER HostName
    Hostname to connect to. Example: api.openweathermap.org

.PARAMETER Port
    TLS port. Defaults to 443.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\compute-spki-pin.ps1 api.openweathermap.org
    powershell -ExecutionPolicy Bypass -File scripts\compute-spki-pin.ps1 overpass-api.de

.NOTES
    This script does NOT validate the certificate chain. It captures
    whatever the server presents. Run from a trusted network.

    The SPKI is reconstructed manually so this works on Windows PowerShell
    5.1 (.NET Framework 4.x), not just PowerShell 7.
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$HostName,
    [Parameter(Position = 1)]
    [int]$Port = 443
)

$ErrorActionPreference = 'Stop'

function ConvertTo-DerLength([int]$len) {
    if ($len -lt 128) {
        return [byte[]]@([byte]$len)
    }
    $tmp = $len
    $bytes = New-Object System.Collections.Generic.List[byte]
    while ($tmp -gt 0) {
        $bytes.Insert(0, [byte]($tmp -band 0xFF))
        $tmp = $tmp -shr 8
    }
    $out = New-Object System.Collections.Generic.List[byte]
    $out.Add([byte](0x80 -bor $bytes.Count))
    $out.AddRange($bytes)
    return [byte[]]$out.ToArray()
}

function ConvertTo-DerSequence([byte[]]$contents) {
    $out = New-Object System.Collections.Generic.List[byte]
    $out.Add([byte]0x30)
    $lenBytes = [byte[]](ConvertTo-DerLength $contents.Length)
    $out.AddRange($lenBytes)
    $out.AddRange([byte[]]$contents)
    return [byte[]]$out.ToArray()
}

function ConvertTo-DerBitString([byte[]]$contents) {
    $out = New-Object System.Collections.Generic.List[byte]
    $out.Add([byte]0x03)
    $lenBytes = [byte[]](ConvertTo-DerLength ($contents.Length + 1))
    $out.AddRange($lenBytes)
    $out.Add([byte]0x00)
    $out.AddRange([byte[]]$contents)
    return [byte[]]$out.ToArray()
}

function ConvertTo-DerOid([string]$oid) {
    $parts = $oid -split '\.'
    $body = New-Object System.Collections.Generic.List[byte]
    $body.Add([byte]([int]$parts[0] * 40 + [int]$parts[1]))
    for ($i = 2; $i -lt $parts.Length; $i++) {
        $val = [int]$parts[$i]
        if ($val -eq 0) {
            $body.Add([byte]0)
            continue
        }
        $stack = New-Object System.Collections.Generic.List[byte]
        while ($val -gt 0) {
            $stack.Insert(0, [byte]($val -band 0x7F))
            $val = $val -shr 7
        }
        for ($j = 0; $j -lt $stack.Count - 1; $j++) {
            $stack[$j] = [byte]($stack[$j] -bor 0x80)
        }
        $body.AddRange($stack)
    }
    $out = New-Object System.Collections.Generic.List[byte]
    $out.Add([byte]0x06)
    $lenBytes = [byte[]](ConvertTo-DerLength $body.Count)
    $out.AddRange($lenBytes)
    $out.AddRange([byte[]]$body.ToArray())
    return [byte[]]$out.ToArray()
}

function Get-SpkiBase64Sha256 {
    param([System.Security.Cryptography.X509Certificates.X509Certificate2]$Cert)

    $exportMethod = $Cert.PublicKey.GetType().GetMethod('ExportSubjectPublicKeyInfo')
    if ($null -ne $exportMethod) {
        $spkiBytes = $exportMethod.Invoke($Cert.PublicKey, $null)
    } else {
        $oid = $Cert.PublicKey.Oid.Value
        $params = $Cert.PublicKey.EncodedParameters.RawData
        if ($null -eq $params) { $params = @() }
        $keyValue = $Cert.PublicKey.EncodedKeyValue.RawData

        $oidDer = ConvertTo-DerOid $oid
        $algIdSeq = ConvertTo-DerSequence ($oidDer + $params)
        $bitString = ConvertTo-DerBitString $keyValue
        $spkiBytes = ConvertTo-DerSequence ($algIdSeq + $bitString)
    }

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash($spkiBytes)
    } finally {
        $sha.Dispose()
    }
    return [Convert]::ToBase64String($hash)
}

$capturedChain = New-Object System.Collections.Generic.List[System.Security.Cryptography.X509Certificates.X509Certificate2]

$callback = {
    param($senderObj, $cert, $chain, $errors)
    if ($null -ne $chain -and $chain.ChainElements.Count -gt 0) {
        foreach ($element in $chain.ChainElements) {
            $script:capturedChain.Add($element.Certificate)
        }
    } elseif ($null -ne $cert) {
        $script:capturedChain.Add([System.Security.Cryptography.X509Certificates.X509Certificate2]::new($cert))
    }
    return $true
}

$tcp = New-Object System.Net.Sockets.TcpClient
$tcp.Connect($HostName, $Port)
$ssl = New-Object System.Net.Security.SslStream($tcp.GetStream(), $false, $callback)

try {
    $ssl.AuthenticateAsClient($HostName)
} finally {
    $ssl.Dispose()
    $tcp.Dispose()
}

if ($capturedChain.Count -eq 0) {
    Write-Error "No certificate captured for $HostName"
    exit 1
}

Write-Host ""
Write-Host "SPKI pins for $HostName" -ForegroundColor Cyan
Write-Host "Paste the leaf value first and the intermediate value as the backup."
Write-Host ""

for ($i = 0; $i -lt $capturedChain.Count; $i++) {
    $label = switch ($i) {
        0 { "Leaf      " }
        1 { "Intermed. " }
        default { "Chain[$i]" }
    }
    $pin = Get-SpkiBase64Sha256 -Cert $capturedChain[$i]
    Write-Host ("{0}  {1}" -f $label, $capturedChain[$i].Subject)
    Write-Host ("              {0}" -f $pin) -ForegroundColor Yellow
}

Write-Host ""
Write-Host "XML block ready to paste:"
Write-Host ""
$leafPin = Get-SpkiBase64Sha256 -Cert $capturedChain[0]
$backupPin = if ($capturedChain.Count -gt 1) { Get-SpkiBase64Sha256 -Cert $capturedChain[1] } else { "REPLACE_WITH_BACKUP" }
@"
<domain-config>
    <domain includeSubdomains="true">$HostName</domain>
    <pin-set expiration="2027-01-01">
        <pin digest="SHA-256">$leafPin</pin>
        <pin digest="SHA-256">$backupPin</pin>
    </pin-set>
</domain-config>
"@
