package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;
import scala.Serializable;

@Data
public class ScheduleWordCount implements Serializable {
    private final String keyword;
    private final Long count;
}
