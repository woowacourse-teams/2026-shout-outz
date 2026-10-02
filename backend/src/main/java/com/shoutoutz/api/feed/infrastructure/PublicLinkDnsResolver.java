package com.shoutoutz.api.feed.infrastructure;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.hc.client5.http.DnsResolver;

/** 접속 직전 DNS 결과를 검사해 내부망 주소로 향하는 요청을 차단한다. */
public class PublicLinkDnsResolver implements DnsResolver {

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            throw new UnknownHostException("DNS 결과가 없습니다.");
        }
        for (InetAddress address : addresses) {
            if (!isPublic(address)) {
                throw new UnknownHostException("공개 주소가 아닌 URL은 가져올 수 없습니다.");
            }
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) {
        return host;
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int a = bytes[0] & 0xff;
            int b = bytes[1] & 0xff;
            int c = bytes[2] & 0xff;
            return a != 0 && a != 10 && a != 127 && a < 224
                    && !(a == 100 && b >= 64 && b <= 127)
                    && !(a == 169 && b == 254)
                    && !(a == 172 && b >= 16 && b <= 31)
                    && !(a == 192 && (b == 0 || b == 168 || (b == 88 && c == 99)))
                    && !(a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100)))
                    && !(a == 203 && b == 0 && c == 113);
        }
        if (address instanceof Inet6Address) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            int third = bytes[2] & 0xff;
            int fourth = bytes[3] & 0xff;
            // 공개 유니캐스트(2000::/3)만 허용하고 터널·문서화 전용 대역은 제외한다.
            return first >= 0x20 && first <= 0x3f
                    && !(first == 0x20 && second == 0x01 && third == 0x0d && fourth == 0xb8)
                    && !(first == 0x20 && second == 0x01 && third == 0x00)
                    && !(first == 0x20 && second == 0x02);
        }
        return false;
    }
}
