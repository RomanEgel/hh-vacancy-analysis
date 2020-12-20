package ru.spbstu.hhvacancyanalysis.dto;

import lombok.Data;

import java.util.List;

@Data
public class Vacancies {
    private List<Vacancy> items;
    private int page;
    private int pages;
    private int found;
}
