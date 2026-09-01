package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import com.shoutoutz.api.user.domain.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfileEntity extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    @Column(length = 10)
    private String track;

    private Short cohort;

    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "github_profile_url")
    private String githubProfileUrl;

    @Column(name = "blog_url")
    private String blogUrl;
}
