package com.team2.postservice.post.entity;

public enum PostCategory {
    ALL("전체"),
    ELECTRIC_LIGHT("전기·조명"),
    PLUMBING("배관·설비"),
    FURNITURE_INSTALL("가구·설치"),
    HOME_APPLIANCE("가전제품"),
    DOOR_WINDOW("문·창문"),
    LIVING_ETC("생활·기타");

    private final String description;

    PostCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}