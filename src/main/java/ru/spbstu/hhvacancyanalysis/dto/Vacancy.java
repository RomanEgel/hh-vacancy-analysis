package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;
import scala.Serializable;

import java.util.List;

@Data
public class Vacancy implements Serializable {
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
    public static class KeySkill implements Serializable{
        private String name;
    }

    @Data
    public static class IdName implements Serializable{
        private String id;
        private String name;
    }

    @Data
    public static class IdNameUrl extends IdName implements Serializable {
        private String url;
    }

    @Data
    public static class Salary implements Serializable{
        private Long from;
        private Long to;
        private String currency;


    }
}
