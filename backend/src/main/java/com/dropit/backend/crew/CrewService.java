package com.dropit.backend.crew;

import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewService {

    private final CrewMemberRepository crewMemberRepository;
    private final CrewAccessCodeRepository crewAccessCodeRepository;

    public CrewService(
        CrewMemberRepository crewMemberRepository,
        CrewAccessCodeRepository crewAccessCodeRepository
    ) {
        this.crewMemberRepository = crewMemberRepository;
        this.crewAccessCodeRepository = crewAccessCodeRepository;
    }

    @Transactional(readOnly = true)
    public boolean isCrewMember(UUID userId) {
        return crewMemberRepository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public CrewStatusResponse status(UUID userId) {
        return crewMemberRepository.findById(userId)
            .map(member -> new CrewStatusResponse(true, member.getVerifiedAt()))
            .orElseGet(() -> new CrewStatusResponse(false, null));
    }

    @Transactional
    public CrewVerifyResponse verify(UUID userId, String code) {
        if (code == null || code.trim().isEmpty() || code.trim().length() > 80) {
            throw ApiException.badRequest("크루 인증 코드를 확인해주세요.");
        }
        boolean verified = crewAccessCodeRepository.existsValidCode(code.trim());
        if (verified) {
            crewMemberRepository.insertIfAbsent(userId);
        }
        return new CrewVerifyResponse(verified);
    }
}
