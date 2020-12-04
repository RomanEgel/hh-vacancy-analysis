package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;
import scala.Serializable;

@Data
public class SkillWordCount implements Serializable {
    private final String keyword;
    private final Double count;
}
