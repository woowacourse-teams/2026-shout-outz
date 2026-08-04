package com.dropit.backend.app;

import java.util.List;
import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import com.dropit.backend.crew.CrewService;
import com.dropit.backend.maker.MakerEntity;
import com.dropit.backend.maker.MakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServicePolicyTest {

    @Mock
    AppRepository appRepository;

    @Mock
    MakerService makerService;

    @Mock
    CrewService crewService;

    private AppService appService;

    @BeforeEach
    void setUp() {
        appService = new AppService(appRepository, makerService, crewService);
    }

    @Test
    void onlyCrewMembersCanCreateApps() {
        UUID userId = UUID.randomUUID();
        when(crewService.isCrewMember(userId)).thenReturn(false);

        assertThatThrownBy(() -> appService.create(userId, request(List.of(AppCategory.DEVELOPMENT))))
            .isInstanceOf(ApiException.class)
            .hasMessage("크루 인증을 완료한 사용자만 서비스를 등록할 수 있습니다.");
    }

    @Test
    void duplicateCategoriesAreRejected() {
        UUID userId = UUID.randomUUID();
        when(crewService.isCrewMember(userId)).thenReturn(true);
        when(makerService.getEntity(userId)).thenReturn(maker(userId));

        assertThatThrownBy(() -> appService.create(
                userId,
                request(List.of(AppCategory.DEVELOPMENT, AppCategory.DEVELOPMENT))
            ))
            .isInstanceOf(ApiException.class)
            .hasMessage("카테고리는 중복해서 선택할 수 없습니다.");
    }

    @Test
    void createsAnAppWithTheCurrentMakerSnapshot() {
        UUID userId = UUID.randomUUID();
        when(crewService.isCrewMember(userId)).thenReturn(true);
        when(makerService.getEntity(userId)).thenReturn(maker(userId));
        when(appRepository.save(any(AppEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppResponse response = appService.create(userId, request(List.of(AppCategory.DEVELOPMENT)));

        assertThat(response.ownerId()).isEqualTo(userId);
        assertThat(response.maker().id()).isEqualTo(userId);
        assertThat(response.categories()).containsExactly(AppCategory.DEVELOPMENT);
    }

    private AppRequest request(List<AppCategory> categories) {
        return new AppRequest(
            "Dropit",
            "서비스 아카이빙",
            "설명",
            "https://example.com",
            null,
            categories,
            ThumbnailVariant.NEW,
            null,
            List.of("Spring", "JPA")
        );
    }

    private MakerEntity maker(UUID userId) {
        return new MakerEntity(userId, "샤를", "샤", null, "BE", "소개", "#d9e6ff");
    }
}
