package ru.spbstu.hhvacancyanalysis.service;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.spbstu.hhvacancyanalysis.dto.ScheduleWordCount;
import ru.spbstu.hhvacancyanalysis.dto.SkillWordCount;
import ru.spbstu.hhvacancyanalysis.dto.Vacancies;
import ru.spbstu.hhvacancyanalysis.dto.Vacancy;
import ru.spbstu.hhvacancyanalysis.repository.VacancyRepo;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VacancyServiceImpl implements VacancyService {
    private static final String listVacanciesQuery = "https://api.hh.ru/vacancies?text=%s&date_from=%s&date_to=%s&page=%s&per_page=100";
    private static final String vacancyQuery = "https://api.hh.ru/vacancies/%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(4);

    private final VacancyRepo vacancyRepo;
    private final MongoOperations mongoOperations;

    public VacancyServiceImpl(VacancyRepo vacancyRepo, MongoOperations mongoOperations) {
        this.vacancyRepo = vacancyRepo;
        this.mongoOperations = mongoOperations;
    }


    @Override
    public void uploadVacancies(String keyWord) {
        vacancyRepo.deleteAll();

        threadPool.execute(() -> {
            System.out.println(">>> START UPLOAD OF VACANCIES FROM HH");
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusMonths(1);
            int count = 0;
            while (!start.isEqual(end)) {
                LocalDate effectivelyFinalStart = start;
                threadPool.schedule(() -> uploadVacanciesForADay(keyWord, effectivelyFinalStart), count, TimeUnit.MINUTES);
                start = start.plusDays(1);
                count++;
            }
        });
    }

    @Override
    public void calculateSkillStat() {
        mongoOperations.dropCollection(SkillWordCount.class);
        List<Vacancy> vacancies = vacancyRepo.findAll();

        if (vacancies.isEmpty()) {
            return;
        }

        List<SkillWordCount> wordCounts = vacancies.parallelStream()
            .filter(v -> v.getKey_skills() != null)
            .flatMap(v -> v.getKey_skills().stream())
            .map(Vacancy.KeySkill::getName)
            .flatMap(s -> Arrays.stream(s.split("[ ]+")))
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new SkillWordCount(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        mongoOperations.insertAll(wordCounts);
    }

    @Override
    public List<SkillWordCount> generateSkillStatReport() {
        List<SkillWordCount> words = mongoOperations.findAll(SkillWordCount.class);
        calculateSkillStat();

        return words.stream()
            .sorted(Comparator.comparingLong(SkillWordCount::getCount).reversed())
            .limit(50)
            .collect(Collectors.toList());
    }

    @Override
    public void calculateScheduleStat() {
        mongoOperations.dropCollection(ScheduleWordCount.class);
        List<Vacancy> vacancies = vacancyRepo.findAll();

        if (vacancies.isEmpty()) {
            return;
        }

        List<ScheduleWordCount> wordCounts = vacancies.parallelStream()
            .filter(v -> v.getSchedule() != null)
            .map(v -> v.getSchedule().getName())
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ScheduleWordCount(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        mongoOperations.insertAll(wordCounts);
    }

    @Override
    public List<ScheduleWordCount> generateScheduleReport() {
        List<ScheduleWordCount> words = mongoOperations.findAll(ScheduleWordCount.class);
        calculateScheduleStat();

        return words.stream()
            .sorted(Comparator.comparingLong(ScheduleWordCount::getCount).reversed())
            .limit(50)
            .collect(Collectors.toList());
    }


    private void uploadVacanciesForADay(String keyWord, LocalDate start) {
        System.out.println(">> STARTED VACANCY UPLOAD FOR A DAY " + start.toString());
        int page = 0;
        Vacancies body = getVacancies(keyWord, start, page);

        if (body == null || body.getItems().isEmpty()) return;

        int pages = body.getPages();
        System.out.println("TOTAL PAGES FOR A DAY " + start.toString() + " is " + pages);
        while (page < pages) {
            body = getVacancies(keyWord, start, ++page);
            if (body == null || body.getItems().isEmpty()) return;
        }

    }

    private Vacancies getVacancies(String keyWord, LocalDate start, int page) {
        Vacancies body = restTemplate.getForEntity(
            String.format(listVacanciesQuery, keyWord, start, start.plusDays(1), page), Vacancies.class)
            .getBody();
        if (body == null || body.getItems().isEmpty())
            return null;

        System.out.println("> STARTED VACANCY UPLOAD FOR A PAGE " + page + " AND START DATE " + start.toString());
        uploadDetailedVacancies(body, page);
        return body;
    }

    private void uploadDetailedVacancies(Vacancies body, int page) {
        for (Vacancy vacancy : body.getItems()) {
            System.out.println("UPLOAD VACANCY " + vacancy.getId() + "on page " + page);
            Vacancy result = restTemplate.getForEntity(String.format(vacancyQuery, vacancy.getId()), Vacancy.class)
                .getBody();
            if (result != null) {
                vacancyRepo.save(result);
            }
        }
    }
}
