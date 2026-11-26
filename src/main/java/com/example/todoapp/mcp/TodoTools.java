package com.example.todoapp.mcp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;
import com.example.todoapp.api.HolidayClient;
import com.example.todoapp.api.TodoDto;

@Component
public class TodoTools {
    private final TodoService todoService;
    private final HolidayClient holidayClient;

    public TodoTools(TodoService todoService, HolidayClient holidayClient) {
        this.todoService = todoService;
        this.holidayClient = holidayClient;
    }

    @McpTool(name = "list_todos", description = "やることの一覧を返す（期間・ジャンルで絞れる）")
    public List<TodoDto> listTodos(
            @McpToolParam(required = false) String keyword,
            @McpToolParam(required = false) String category,
            @McpToolParam(required = false) String from,
            @McpToolParam(required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);

        return todoService.search(keyword, category, "asc", fromDate, toDate).stream()
                .map(TodoDto::from)
                .toList();
    }

    @McpTool(name = "get_todo", description = "やることを1件返す")
    public TodoDto getTodo(@McpToolParam(required = true) Long id) {
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "add_todo", description = "やることを1件足す")
    public TodoDto addTodo(
            @McpToolParam(required = true) String title,
            @McpToolParam(required = true) String category,
            @McpToolParam(required = true) Integer priority,
            @McpToolParam(required = false) String detail,
            @McpToolParam(required = false) String dueDate) {
        if (title == null || title.isBlank() || title.length() > 255) {
            throw new IllegalArgumentException("やることは1～255文字で入力してください");
        }
        if (category == null || !Set.of("デザイン", "マーケティング", "プログラミング", "資格", "就職活動")
                .contains(category)) {
            throw new IllegalArgumentException("ジャンルはデザイン・マーケティング・プログラミング・資格・就職活動から選んでください");
        }
        if (priority == null) {
            throw new IllegalArgumentException("優先度を指定してください");
        }
        if (detail != null && detail.length() > 255) {
            throw new IllegalArgumentException("メモは255文字以内で入力してください");
        }
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDetail(detail);
        todo.setDueDate(dueDate == null ? null : LocalDate.parse(dueDate));
        todo.setCompleted(false);
        todoService.create(todo);
        return TodoDto.from(todoService.findById(todo.getId()));
    }

    @McpTool(name = "update_todo", description = "やることを1件直す（期限を変えるのもこれ）")
    public TodoDto updateTodo(
            @McpToolParam(required = true) Long id,
            @McpToolParam(required = false) String title,
            @McpToolParam(required = false) String category,
            @McpToolParam(required = false) Integer priority,
            @McpToolParam(required = false) String detail,
            @McpToolParam(required = false) String dueDate,
            @McpToolParam(required = false) Boolean completed) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            throw new IllegalArgumentException("指定されたやることが見つかりません: " + id);
        }
        if (title != null && (title.isBlank() || title.length() > 255)) {
            throw new IllegalArgumentException("やることは1～255文字で入力してください");
        }
        if (category != null && !Set.of("デザイン", "マーケティング", "プログラミング", "資格", "就職活動")
                .contains(category)) {
            throw new IllegalArgumentException("ジャンルはデザイン・マーケティング・プログラミング・資格・就職活動から選んでください");
        }
        if (detail != null && detail.length() > 255) {
            throw new IllegalArgumentException("メモは255文字以内で入力してください");
        }
        LocalDate parsedDueDate = dueDate == null ? null : LocalDate.parse(dueDate);
        if (title != null) todo.setTitle(title);
        if (category != null) todo.setCategory(category);
        if (priority != null) todo.setPriority(priority);
        if (detail != null) todo.setDetail(detail);
        if (dueDate != null) todo.setDueDate(parsedDueDate);
        if (completed != null) todo.setCompleted(completed);
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "complete_todo", description = "やることを完了にする")
    public TodoDto completeTodo(@McpToolParam(required = true) Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            throw new IllegalArgumentException("指定されたやることが見つかりません: " + id);
        }
        todo.setCompleted(true);
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "delete_todo", description = "やることを1件消す")
    public void deleteTodo(@McpToolParam(required = true) Long id) {
        todoService.delete(id);
    }

    @McpTool(name = "find_free_days", description = "期間の中で、期限のやることが無く、土日でも祝日でもない「空いている日」を返す。やることの期限を動かす先を決めるのに使う")
    public List<LocalDate> findFreeDays(
            @McpToolParam(required = true) String from,
            @McpToolParam(required = true) String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("開始日は終了日以前にしてください");
        }
        var holidayResult = holidayClient.fetchHolidays();
        if (holidayResult.unavailable()) {
            throw new IllegalStateException("祝日を取得できませんでした。時間をおいて再度実行してください");
        }
        Set<LocalDate> busyDays = todoService.search(null, null, "asc", fromDate, toDate).stream()
                .map(Todo::getDueDate)
                .filter(date -> date != null)
                .collect(Collectors.toSet());
        return Stream.iterate(fromDate, date -> !date.isAfter(toDate), date -> date.plusDays(1))
                .filter(date -> !busyDays.contains(date))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY
                        && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(date -> !holidayResult.holidays().containsKey(date.toString()))
                .toList();
    }
}
