package com.dropit.backend.maker;

import java.util.Locale;
import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MakerService {

    private static final String DEFAULT_TONE = "#d9e6ff";

    private final MakerRepository makerRepository;

    public MakerService(MakerRepository makerRepository) {
        this.makerRepository = makerRepository;
    }

    @Transactional(readOnly = true)
    public MakerResponse get(UUID makerId) {
        return makerRepository.findById(makerId)
            .map(MakerResponse::from)
            .orElseThrow(() -> ApiException.notFound("프로필을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public MakerEntity getEntity(UUID makerId) {
        return makerRepository.findById(makerId)
            .orElseThrow(() -> ApiException.notFound("프로필을 먼저 등록해주세요."));
    }

    @Transactional
    public MakerResponse upsert(UUID userId, MakerRequest request) {
        String name = request.name().trim();
        String role = request.role().trim();
        String bio = normalizeBio(request.bio());
        String initials = initialsFromName(name);

        MakerEntity maker = makerRepository.findById(userId).orElse(null);
        String tone = request.tone() == null || request.tone().isBlank()
            ? maker == null ? DEFAULT_TONE : maker.getTone()
            : request.tone().trim();

        if (maker == null) {
            maker = new MakerEntity(userId, name, initials, normalizeNullable(request.avatarUrl()), role, bio, tone);
        } else {
            maker.update(name, initials, normalizeNullable(request.avatarUrl()), role, bio, tone);
        }
        return MakerResponse.from(makerRepository.save(maker));
    }

    private String normalizeBio(String bio) {
        return bio.trim().replace("\r\n", "\n").replaceAll("\\n{3,}", "\n\n");
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String initialsFromName(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }
        return initials.length() == 0 ? "나" : initials.toString().toUpperCase(Locale.ROOT);
    }
}
