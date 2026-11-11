package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceExperience;
import java.util.List;

public record ResourceExperienceResponse(
        String problem,
        String cause,
        String solution,
        String result,
        List<String> techs
) {

    public ResourceExperienceResponse {
        techs = List.copyOf(techs);
    }

    public static ResourceExperienceResponse from(ResourceExperience experience) {
        return new ResourceExperienceResponse(
                experience.problem(),
                experience.cause(),
                experience.solution(),
                experience.result(),
                experience.techs()
        );
    }
}
