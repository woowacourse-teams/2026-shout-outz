package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

public record ResourceExperience(String problem, String cause, String solution, String result, List<String> techs) {

    public ResourceExperience {
        techs = List.copyOf(techs);
    }
}
