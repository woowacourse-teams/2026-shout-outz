package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import java.util.List;

public interface ProjectNameFinder {

    /**
     * 검색어가 이름으로 가리키는 공개 프로젝트. 이름이 같은 프로젝트가 여럿이면 모두 준다.
     */
    List<MatchedProject> findNamed(String keyword);
}
