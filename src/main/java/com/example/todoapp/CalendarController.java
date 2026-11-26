package com.example.todoapp;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(name = "year", required = false) Integer year,
                           @RequestParam(name = "month", required = false) Integer month,
                           Model model) {
        YearMonth now = YearMonth.now();
        YearMonth currentMonth;
        YearMonth previousMonth;
        YearMonth nextMonth;
        try {
            currentMonth = YearMonth.of(year == null ? now.getYear() : year,
                    month == null ? now.getMonthValue() : month);
            previousMonth = currentMonth.minusMonths(1);
            nextMonth = currentMonth.plusMonths(1);
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year or month", ex);
        }

        LocalDate firstDate = currentMonth.atDay(1);
        LocalDate lastDate = currentMonth.atEndOfMonth();
        int leadingEmptyCells = firstDate.getDayOfWeek().getValue() % 7;
        int cellCount = ((leadingEmptyCells + currentMonth.lengthOfMonth() + 6) / 7) * 7;
        List<List<LocalDate>> weeks = new ArrayList<>();
        for (int cell = 0; cell < cellCount; cell += 7) {
            List<LocalDate> week = new ArrayList<>();
            for (int weekday = 0; weekday < 7; weekday++) {
                int day = cell + weekday - leadingEmptyCells + 1;
                week.add(day >= 1 && day <= currentMonth.lengthOfMonth()
                        ? currentMonth.atDay(day) : null);
            }
            weeks.add(week);
        }

        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("previousMonth", previousMonth);
        model.addAttribute("nextMonth", nextMonth);
        model.addAttribute("firstDate", firstDate);
        model.addAttribute("lastDate", lastDate);
        model.addAttribute("weeks", weeks);
        return "calendar";
    }
}
