package ru.spbstu.hhvacancyanalysis.service;

import ru.spbstu.hhvacancyanalysis.dto.ScheduleWordCount;
import ru.spbstu.hhvacancyanalysis.dto.SkillWordCount;

import java.util.List;

public interface VacancyService {
    void uploadVacancies(String keyWord);

    void calculateSkillStat();

    List<SkillWordCount> generateSkillStatReport();

    void calculateScheduleStat();

    List<ScheduleWordCount> generateScheduleReport();
}
