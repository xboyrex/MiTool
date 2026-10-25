"""
Silent Smart DNS-over-HTTPS (DoH) Resolver (Cloudflare 1.1.1.1 & Google 8.8.8.8)
for MiTool by xboyrex. Automatically resolves `*.mi.com`, `*.miui.com`, and
`*.xiaomi.com` domains if the ISP or custom DNS fails.
"""

import json
import socket
import urllib.request
from typing import Dict, Tuple

DOH_ENDPOINTS: Tuple[Tuple[str, str], ...] = (
    ("Cloudflare 1.1.1.1", "https://1.1.1.1/dns-query?name={host}&type=A"),
    ("Google 8.8.8.8", "https://8.8.8.8/resolve?name={host}&type=A"),
)

_DNS_CACHE: Dict[str, str] = {}


def resolve_host_smart(host: str, timeout: float = 2.0) -> Tuple[str, str]:
    if host in _DNS_CACHE:
        return _DNS_CACHE[host], "Cached"

    try:
        ip = socket.gethostbyname(host)
        if ip and not ip.startswith("127.") and ip != "0.0.0.0":
            _DNS_CACHE[host] = ip
            return ip, "System DNS"
    except Exception:
        pass

    for provider_name, url_tmpl in DOH_ENDPOINTS:
        try:
            url = url_tmpl.format(host=host)
            req = urllib.request.Request(url, headers={"Accept": "application/dns-json"})
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                for ans in data.get("Answer", []):
                    if ans.get("type") == 1 and ans.get("data"):
                        ip = ans["data"]
                        _DNS_CACHE[host] = ip
                        return ip, provider_name
        except Exception:
            continue

    return "", "Unresolved"


def install_smart_dns_hook() -> None:
    """Transparently fallback to Cloudflare 1.1.1.1 / Google 8.8.8.8 DoH if OS DNS fails."""
    orig_getaddrinfo = socket.getaddrinfo

    def _smart_getaddrinfo(host, port, family=0, type=0, proto=0, flags=0):
        try:
            return orig_getaddrinfo(host, port, family, type, proto, flags)
        except socket.gaierror:
            if isinstance(host, str) and (
                host.endswith(".mi.com")
                or host.endswith(".miui.com")
                or host.endswith(".xiaomi.com")
            ):
                ip, _ = resolve_host_smart(host)
                if ip:
                    return orig_getaddrinfo(ip, port, family, type, proto, flags)
            raise

    if getattr(socket.getaddrinfo, "__name__", "") != "_smart_getaddrinfo":
        socket.getaddrinfo = _smart_getaddrinfo
