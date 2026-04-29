Win+R → sysdm.cpl → 高级 → 环境变量

用户 / 系统变量 → 新建：
HTTP_PROXY = http://127.0.0.1:7890
HTTPS_PROXY = http://127.0.0.1:7890

打开 PowerShell 执行：
notepad $PROFILE
如果提示不存在，就执行：$PROFILE ，然后手动把这个文件创建出来，再执行 notepad $PROFILE

整个下面这段完整复制，直接粘贴到记事本里：
```
function proxy {
    $env:http_proxy = "http://127.0.0.1:7890"
    $env:https_proxy = "http://127.0.0.1:7890"
    [System.Net.WebRequest]::DefaultWebProxy = New-Object System.Net.WebProxy("http://127.0.0.1:7890")
    Write-Host "Proxy enabled: http://127.0.0.1:10809" -ForegroundColor Green
}

function unproxy {
    $env:http_proxy = $null
    $env:https_proxy = $null
    [System.Net.WebRequest]::DefaultWebProxy = $null
    Write-Host "Proxy disabled" -ForegroundColor Yellow
}

function check-proxy {
    if ($env:http_proxy -or $env:https_proxy) {
        Write-Host "Current proxy settings:" -ForegroundColor Cyan
        Write-Host "HTTP Proxy: $env:http_proxy"
        Write-Host "HTTPS Proxy: $env:https_proxy"
    } else {
        Write-Host "No proxy is currently set." -ForegroundColor Cyan
    }
}
```
如果你遇到 “禁止运行脚本” 报错  执行一次这条命令（管理员 PowerShell）：
```ssh
Set-ExecutionPolicy RemoteSigned
```
重新打开 PowerShell 执行 . $PROFILE

输入 proxy 来启用代理
输入 unproxy 来禁用代理
输入 check-proxy 来查看当前的代理设置


Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' | Select ProxyServer, ProxyEnable

(Invoke-WebRequest -UseBasicParsing ifconfig.me/ip).Content

Remove-Item Env:http_proxy -ErrorAction SilentlyContinue
Remove-Item Env:https_proxy -ErrorAction SilentlyContinue

(Invoke-WebRequest -UseBasicParsing ifconfig.me/ip).Content

关闭 PowerShell，重新普通打开一个，就不会再报错了。





### 给 idea 当前项目加代理：
添加 VM options参数：-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=6984 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=6984 -Dhttp.nonProxyHosts=localhost|127.0.0.1|192.168.*



GEMINI_API_KEY=AIzaSyCQYcWfv4bZALsOJx6LRkEoxz8CoB6mnl8
GEMINI_MODEL=gemini-3-flash-preview
GOOGLE_GEMINI_BASE_URL=https://generativelanguage.googleapis.com
