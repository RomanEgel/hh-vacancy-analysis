package ru.spbstu.hhvacancyanalysis.service;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import ru.spbstu.hhvacancyanalysis.dto.*;
import scala.Tuple2;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.spbstu.hhvacancyanalysis.repository.VacancyRepo;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class VacancyServiceImpl implements VacancyService {
    private static final String listVacanciesQuery = "https://api.hh.ru/vacancies?text=%s&date_from=%s&date_to=%s&page=%s&per_page=100";
    private static final String vacancyQuery = "https://api.hh.ru/vacancies/%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final VacancyRepo vacancyRepo;
    private final MongoOperations mongoOperations;

    private final SparkSession spark = SparkSession
            .builder()
            .appName("hhvacancyanalysis")
            .config("spark.master", "local")
            .getOrCreate();

    private final SparkContext sc = spark.sparkContext();
    private final JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sc);

    public VacancyServiceImpl(VacancyRepo vacancyRepo, MongoOperations mongoOperations) {
        this.vacancyRepo = vacancyRepo;
        this.mongoOperations = mongoOperations;
    }


    @Override
    public void uploadVacancies(String keyWord) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(1);
        vacancyRepo.deleteAll();

        while (!start.isEqual(end)) {
            uploadVacanciesForADay(keyWord, start);
            start = start.plusDays(1);
        }
    }

    @Override
    public void calculateSkillStat() {
        mongoOperations.dropCollection(SkillWordCount.class);
        List<Vacancy> vacancies = vacancyRepo.findAll();

        if (vacancies.isEmpty()) {
            return;
        }

        JavaRDD<Vacancy> vacancyJavaRDD = jsc.parallelize(vacancies);

        Long skillsCount = vacancyJavaRDD
                .filter(v -> v.getKey_skills() != null)
                .count();
        List<SkillWordCount> wordCounts = vacancyJavaRDD
                .filter(v -> v.getKey_skills() != null)
                .flatMap(v -> v.getKey_skills().iterator())
                .map(Vacancy.KeySkill::getName)
                //.flatMap(s -> Arrays.stream(s.split("[ ]+")).iterator())
                .mapToPair(s -> new Tuple2<>(s, 1L))
                .reduceByKey(Long::sum)
                .map(e -> new SkillWordCount(e._1(), ((double) e._2() / (double) skillsCount) * 100))
                .collect();

        mongoOperations.insertAll(wordCounts);
    }

    @Override
    public List<SkillWordCount> generateSkillStatReport() {
        calculateSkillStat();
        List<SkillWordCount> words = mongoOperations.findAll(SkillWordCount.class);
        List<SkillWordCount> top = words.stream()
                .sorted(Comparator.comparingDouble(SkillWordCount::getCount).reversed())
                .limit(50)
                .collect(Collectors.toList());

        return top;
    }

    @Override
    public void calculateScheduleStat() {
        mongoOperations.dropCollection(ScheduleWordCount.class);
        List<Vacancy> vacancies = vacancyRepo.findAll();

        if (vacancies.isEmpty()) {
            return;
        }

        JavaRDD<Vacancy> vacancyJavaRDD = jsc.parallelize(vacancies);
        List<ScheduleWordCount> wordCounts = vacancyJavaRDD
                .filter(v -> v.getSchedule() != null)
                .map(v -> v.getSchedule().getName())
                .mapToPair(s -> new Tuple2<>(s, 1))
                .reduceByKey(Integer::sum)
                .map(e -> new ScheduleWordCount(e._1(), e._2().longValue()))
                .collect();

        mongoOperations.insertAll(wordCounts);
    }

    @Override
    public List<ScheduleWordCount> generateScheduleReport() {
        calculateScheduleStat();
        List<ScheduleWordCount> words = mongoOperations.findAll(ScheduleWordCount.class);
        List<ScheduleWordCount> top = words.stream()
                .sorted(Comparator.comparingLong(ScheduleWordCount::getCount).reversed())
                .limit(50)
                .collect(Collectors.toList());

        return top;
    }

    @Override
    public void calculateExperienceStat() {
        mongoOperations.dropCollection(ExperienceWordCount.class);
        List<Vacancy> vacancies = vacancyRepo.findAll();

        if (vacancies.isEmpty()) {
            return;
        }

        JavaRDD<Vacancy> vacancyJavaRDD = jsc.parallelize(vacancies);
        List<ExperienceWordCount> wordCounts = vacancyJavaRDD
                .filter(v -> v.getExperience() != null)
                .map(v -> v.getExperience().getName())
                .mapToPair(s -> new Tuple2<>(s, 1))
                .reduceByKey(Integer::sum)
                .map(e -> new ExperienceWordCount(e._1(), e._2().longValue()))
                .collect();

        mongoOperations.insertAll(wordCounts);
    }

    @Override
    public List<ExperienceWordCount> generateExperienceReport() {
        calculateExperienceStat();
        List<ExperienceWordCount> words = mongoOperations.findAll(ExperienceWordCount.class);
        List<ExperienceWordCount> top = words.stream()
                .sorted(Comparator.comparingLong(ExperienceWordCount::getCount).reversed())
                .limit(50)
                .collect(Collectors.toList());
        return top;
    }

    private void uploadVacanciesForADay(String keyWord, LocalDate start) {
        int page = 0;
        Vacancies body = getVacancies(keyWord, start, page);
        if (body == null || body.getItems().isEmpty()) return;

        int pages = body.getPages();
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

        uploadDetailedVacanciesAsync(body);
        return body;
    }

    private void uploadDetailedVacanciesAsync(Vacancies body) {
        threadPool.submit(() -> {
            for (Vacancy vacancy : body.getItems()) {
                Vacancy result = restTemplate.getForEntity(String.format(vacancyQuery, vacancy.getId()), Vacancy.class)
                        .getBody();
                if (result != null) {
                    vacancyRepo.save(result);
                }
            }
        });
    }
}
