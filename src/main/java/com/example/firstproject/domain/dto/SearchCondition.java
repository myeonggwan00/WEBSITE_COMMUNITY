package com.example.firstproject.domain.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @Setter
public class SearchCondition {
    private String option;
    private String keyword;

    public SearchCondition() {
        this.option = "";
        this.keyword = "";
    }

    @Override
    public String toString() {
        return "SearchCondition{" +
                "option='" + option + '\'' +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
