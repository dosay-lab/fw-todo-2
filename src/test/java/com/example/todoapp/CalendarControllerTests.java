package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class CalendarControllerTests {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        mockMvc = MockMvcBuilders.standaloneSetup(new CalendarController())
                .setViewResolvers(views).build();
    }

    @Test
    void rendersDatesAndEmptyCellsWithoutDateAttributes() throws Exception {
        String html = render(2026, 4);
        assertThat(html).contains("2026年4月", "data-from=\"2026-04-01\"",
                "data-to=\"2026-04-30\"", "data-date=\"2026-04-04\"");
        assertThat(Pattern.compile("data-date=\"").matcher(html).results().count()).isEqualTo(30);
        assertThat(Pattern.compile("<td>\\s*</td>").matcher(html).results().count()).isEqualTo(5);
        assertThat(html).containsPattern("(?s)<tbody>\\s*<tr>\\s*(<td>\\s*</td>\\s*){3}<td data-date=\"2026-04-01\">");
        assertThat(html).containsSubsequence(">日</th>", ">月</th>", ">火</th>", ">水</th>",
                ">木</th>", ">金</th>", ">土</th>");
    }

    @Test
    void handlesLeapYearsAndFourToSixWeekMonths() throws Exception {
        assertThat(render(2024, 2)).contains("data-date=\"2024-02-29\"", "data-to=\"2024-02-29\"");
        assertThat(Pattern.compile("<td[ >]").matcher(render(2026, 2)).results().count()).isEqualTo(28);
        assertThat(Pattern.compile("<td[ >]").matcher(render(2026, 8)).results().count()).isEqualTo(42);
    }

    @Test
    void navigationCrossesYearBoundaries() throws Exception {
        assertThat(render(2026, 1)).contains("/calendar?year=2025&amp;month=12");
        assertThat(render(2026, 12)).contains("/calendar?year=2027&amp;month=1");
    }

    @Test
    void defaultsEachMissingParameterToCurrentYearOrMonth() {
        YearMonth before = YearMonth.now();
        ExtendedModelMap defaultModel = new ExtendedModelMap();
        ExtendedModelMap yearOnlyModel = new ExtendedModelMap();
        ExtendedModelMap monthOnlyModel = new ExtendedModelMap();
        CalendarController controller = new CalendarController();
        assertThat(controller.calendar(null, null, defaultModel)).isEqualTo("calendar");
        controller.calendar(2024, null, yearOnlyModel);
        controller.calendar(null, 4, monthOnlyModel);
        YearMonth after = YearMonth.now();
        assertThat(defaultModel.get("currentMonth")).isIn(before, after);
        assertThat(yearOnlyModel.get("currentMonth"))
                .isIn(YearMonth.of(2024, before.getMonthValue()), YearMonth.of(2024, after.getMonthValue()));
        assertThat(monthOnlyModel.get("currentMonth"))
                .isIn(YearMonth.of(before.getYear(), 4), YearMonth.of(after.getYear(), 4));
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyWeekHasSevenCellsAndDatesAppearExactlyOnce() {
        CalendarController controller = new CalendarController();
        for (int month = 1; month <= 12; month++) {
            ExtendedModelMap model = new ExtendedModelMap();
            controller.calendar(2026, month, model);
            List<List<LocalDate>> weeks = (List<List<LocalDate>>) model.get("weeks");
            assertThat(weeks).allSatisfy(week -> assertThat(week).hasSize(7));
            List<LocalDate> dates = weeks.stream().flatMap(List::stream).filter(date -> date != null).toList();
            YearMonth expected = YearMonth.of(2026, month);
            assertThat(dates).containsExactlyElementsOf(expected.atDay(1).datesUntil(expected.atEndOfMonth().plusDays(1)).toList());
        }
    }

    @Test
    void invalidParametersReturnBadRequest() throws Exception {
        mockMvc.perform(get("/calendar").param("month", "13")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/calendar").param("month", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/calendar").param("year", "abc")).andExpect(status().isBadRequest());
    }

    private String render(int year, int month) throws Exception {
        return mockMvc.perform(get("/calendar").param("year", Integer.toString(year))
                        .param("month", Integer.toString(month)))
                .andExpect(status().isOk()).andExpect(view().name("calendar"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
