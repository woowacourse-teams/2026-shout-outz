package com.shoutoutz.api.feed.infrastructure;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 브라우저에서는 열리지만 {@link URI} 로는 만들 수 없는 주소를 미리보기 수집에 쓸 수 있게 고친다.
 * 경로, 쿼리, # 뒤에 그대로 들어간 한글, 공백, 대괄호, 이모지 같은 글자를 UTF-8 퍼센트 인코딩한다.
 * 이미 인코딩된 %XX 와 원래 허용되는 글자는 그대로 두므로 올바른 주소는 바뀌지 않는다.
 * scheme 과 호스트 부분은 손대지 않는다.
 */
final class LinkPreviewUri {

    private static final Pattern SCHEME_AND_AUTHORITY = Pattern.compile("^(?:[A-Za-z][A-Za-z0-9+.-]*:)?//[^/?#]*");
    private static final String UNRESERVED_MARKS = "-_.!~*'()";
    private static final String PATH_CHARACTERS = ",;:$&+=/@";
    private static final String QUERY_CHARACTERS = PATH_CHARACTERS + "?[]";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private LinkPreviewUri() {
    }

    /** 미리보기를 받을 처음 주소. */
    static URI parse(String value) {
        return URI.create(encode(value.strip()));
    }

    /** 페이지 안의 이미지, 파비콘 주소처럼 HTML 에서 읽은 주소를 페이지 주소 기준으로 푼다. */
    static URI resolve(URI base, String reference) {
        return base.resolve(encode(reference.strip()));
    }

    /**
     * 리다이렉트 응답의 Location 을 푼다.
     * GitHub Pages 처럼 한글을 인코딩하지 않고 UTF-8 바이트 그대로 보내는 서버가 있다.
     * HttpClient 는 헤더 바이트를 글자 하나씩 ISO-8859-1 로 읽으므로, 그 바이트를 다시 UTF-8 로 읽어 되살린다.
     */
    static URI resolveLocation(URI base, String location) {
        return resolve(base, utf8FromLatin1(location));
    }

    static String encode(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        Matcher authority = SCHEME_AND_AUTHORITY.matcher(value);
        int index = 0;
        if (authority.find()) {
            encoded.append(authority.group());
            index = authority.end();
        }
        Part part = Part.PATH;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (character == '%' && isHex(value, index + 1) && isHex(value, index + 2)) {
                encoded.append(value, index, index + 3);
                index += 3;
                continue;
            }
            if (character == '?' && part == Part.PATH) {
                part = Part.QUERY;
                encoded.append(character);
            } else if (character == '#' && part != Part.FRAGMENT) {
                part = Part.FRAGMENT;
                encoded.append(character);
            } else if (part.allows(character)) {
                encoded.append(character);
            } else {
                int codePoint = value.codePointAt(index);
                appendPercentEncoded(encoded, new String(Character.toChars(codePoint)));
                index += Character.charCount(codePoint);
                continue;
            }
            index++;
        }
        return encoded.toString();
    }

    private static String utf8FromLatin1(String value) {
        boolean hasHighByte = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character > 0xFF) {
                return value;
            }
            hasHighByte |= character >= 0x80;
        }
        if (!hasHighByte) {
            return value;
        }
        try {
            return UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value.getBytes(ISO_8859_1)))
                    .toString();
        } catch (CharacterCodingException notUtf8) {
            return value;
        }
    }

    private static void appendPercentEncoded(StringBuilder encoded, String characters) {
        for (byte octet : characters.getBytes(UTF_8)) {
            encoded.append('%').append(HEX[(octet >> 4) & 0x0F]).append(HEX[octet & 0x0F]);
        }
    }

    private static boolean isHex(String value, int index) {
        return index < value.length() && Character.digit(value.charAt(index), 16) >= 0;
    }

    private enum Part {
        PATH(PATH_CHARACTERS),
        QUERY(QUERY_CHARACTERS),
        FRAGMENT(QUERY_CHARACTERS);

        private final String allowedSymbols;

        Part(String allowedSymbols) {
            this.allowedSymbols = allowedSymbols;
        }

        boolean allows(char character) {
            return (character < 0x80 && Character.isLetterOrDigit(character))
                    || UNRESERVED_MARKS.indexOf(character) >= 0
                    || allowedSymbols.indexOf(character) >= 0;
        }
    }
}
