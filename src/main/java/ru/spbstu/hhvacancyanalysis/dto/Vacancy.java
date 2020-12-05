package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class Vacancy {
    private Long id;
    private String name;
    private Salary salary;
    private IdNameUrl area;
    private IdName experience;
    private IdName schedule;
    private IdName employment;
    private String description;
    private List<KeySkill> key_skills;
    private IdNameUrl employer;
    private String created_at;

    @Data
    public static class KeySkill {
        private String name;
    }

    @Data
    public static class IdName {
        private String id;
        private String name;
    }

    @Getter
    @Setter
    public static class IdNameUrl extends IdName {
        private String url;
    }

    @Data
    public static class Salary {
        private Long from;
        private Long to;
        private String currency;


    }
}
