package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;

@Data
public class SkillWordCount {
    private final String keyword;
    private final Double count;
}
