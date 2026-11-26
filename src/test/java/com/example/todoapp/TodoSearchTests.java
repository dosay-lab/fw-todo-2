package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.todoapp.api.TodoApiController;

class TodoSearchTests {
    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void optionalDatesReachMapperAndAddInclusiveSqlConditions(boolean hasFrom, boolean hasTo)
            throws Exception {
        LocalDate from = hasFrom ? LocalDate.of(2026, 4, 1) : null;
        LocalDate to = hasTo ? LocalDate.of(2026, 4, 30) : null;
        TodoMapper mapper = mock(TodoMapper.class);
        when(mapper.search("task", "category", "desc", from, to)).thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new TodoApiController(new TodoService(mapper))).build();
        MockHttpServletRequestBuilder request = get("/api/todos")
                .param("keyword", "task").param("category", "category").param("order", "desc");
        if (hasFrom) {
            request.param("from", from.toString());
        }
        if (hasTo) {
            request.param("to", to.toString());
        }
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(mapper).search("task", "category", "desc", from, to);

        BoundSql boundSql = searchSql("task", "category", "desc", from, to);
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        String expected = "SELECT id, title, detail, category, priority, due_date, completed, created_at, updated_at"
                + " FROM todos WHERE title LIKE CONCAT('%', ?, '%') AND category = ?"
                + (hasFrom ? " AND due_date >= ?" : "")
                + (hasTo ? " AND due_date <= ?" : "") + " ORDER BY due_date DESC";
        assertThat(sql).isEqualTo(expected);
        List<String> parameters = boundSql.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList();
        assertThat(parameters).contains("keyword", "category");
        assertThat(parameters.contains("from")).isEqualTo(hasFrom);
        assertThat(parameters.contains("to")).isEqualTo(hasTo);
    }

    @Test
    void omittedDatesKeepApiDefaultsAndExistingScreenSearch() throws Exception {
        TodoMapper mapper = mock(TodoMapper.class);
        when(mapper.search("", "", "asc", null, null)).thenReturn(List.of());
        TodoService service = new TodoService(mapper);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TodoApiController(service)).build();
        mvc.perform(get("/api/todos")).andExpect(status().isOk());
        verify(mapper).search("", "", "asc", null, null);
        service.search("screen", "category", "desc");
        verify(mapper).search("screen", "category", "desc", null, null);
        assertThat(searchSql("", "", "asc", null, null).getSql().replaceAll("\\s+", " "))
                .doesNotContain("WHERE").endsWith("ORDER BY due_date ASC");
    }

    @Test
    void invalidDateReturnsBadRequest() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new TodoApiController(new TodoService(mock(TodoMapper.class)))).build();
        mvc.perform(get("/api/todos").param("from", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/todos").param("to", "invalid")).andExpect(status().isBadRequest());
    }

    private BoundSql searchSql(String keyword, String category, String order,
                               LocalDate from, LocalDate to) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/TodoMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("keyword", keyword);
        parameters.put("category", category);
        parameters.put("order", order);
        parameters.put("from", from);
        parameters.put("to", to);
        return configuration.getMappedStatement("com.example.todoapp.TodoMapper.search")
                .getBoundSql(parameters);
    }
}
