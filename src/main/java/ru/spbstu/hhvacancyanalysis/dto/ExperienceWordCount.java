package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;
import scala.Serializable;

@Data
public class ExperienceWordCount implements Serializable {
    private final String keyword;
    private final Long count;
}
