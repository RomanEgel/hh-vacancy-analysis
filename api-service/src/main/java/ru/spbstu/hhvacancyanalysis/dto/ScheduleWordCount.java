package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;

@Data
public class ScheduleWordCount {
    private final String keyword;
    private final Long count;
}
