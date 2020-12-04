package ru.spbstu.hhvacancyanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.spbstu.hhvacancyanalysis.dto.ExperienceWordCount;
import ru.spbstu.hhvacancyanalysis.dto.ScheduleWordCount;
import ru.spbstu.hhvacancyanalysis.dto.SkillWordCount;
import ru.spbstu.hhvacancyanalysis.service.VacancyService;

import java.util.List;

@SpringBootApplication
@EnableMongoRepositories
public class HhVacancyAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(HhVacancyAnalysisApplication.class, args);
    }


    @RestController("api")
    static class Api {
        private final VacancyService vacancyService;

        Api(VacancyService vacancyService) {
            this.vacancyService = vacancyService;
        }

        @GetMapping("upload-vacancies")
        public void uploadVacancies(@RequestParam String keyWord) {
            vacancyService.uploadVacancies(keyWord);
        }

        @PostMapping("skill-stat")
        public void calculateSkillStatistic() {
            vacancyService.calculateSkillStat();
        }

        @PostMapping("generate-skill-stat-report")
        public List<SkillWordCount> generateSkillStatReport() {
            return vacancyService.generateSkillStatReport();
        }

        @PostMapping("schedule-stat")
        public void calculateScheduleStat() {
            vacancyService.calculateScheduleStat();
        }

        @PostMapping("generate-schedule-report")
        public List<ScheduleWordCount> generateScheduleReport() {
            return vacancyService.generateScheduleReport();
        }

        @PostMapping("experience-stat")
        public void calculateExperienceStat() {
            vacancyService.calculateExperienceStat();
        }

        @PostMapping("generate-experience-report")
        public List<ExperienceWordCount> generateExperienceReport() {
            return vacancyService.generateExperienceReport();
        }
    }

    @Controller
    static class Visualisator {
        @GetMapping("/")
        public String getMainPage() {
            return "main_page";
        }

        @GetMapping("/skill_stat")
        public String getSkillStatReport() {
            return "skill_stat_report";
        }

        @GetMapping("/schedule_stat")
        public String getScheduleStatReport() {
            return "schedule_stat_report";
        }

        @GetMapping("/experience_stat")
        public String getExperienceStatReport() {
            return "experience_stat_report";
        }
    }
}
