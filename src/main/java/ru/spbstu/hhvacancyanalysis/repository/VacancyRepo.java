package ru.spbstu.hhvacancyanalysis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.spbstu.hhvacancyanalysis.dto.Vacancy;

@Repository
public interface VacancyRepo extends MongoRepository<Vacancy, Long> {

}
