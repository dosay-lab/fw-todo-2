package com.example.todoapp.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;

@RestController
public class TodoApiController {
    private final TodoService todoService;

    public TodoApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/api/todos")
    public List<TodoDto> todos(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order) {
        return todoService.search(keyword, category, order).stream()
                .map(TodoDto::from)
                .toList();
    }

    @GetMapping("/api/todos/{id}")
    public TodoDto findById(@PathVariable("id") Long id) {
        return TodoDto.from(findTodoOrThrow(id));
    }

    @PostMapping("/api/todos")
    public ResponseEntity<TodoDto> create(@RequestBody Todo todo) {
        todoService.create(todo);
        Todo created = todoService.findById(todo.getId());
        return ResponseEntity.created(URI.create("/api/todos/" + todo.getId()))
                .body(TodoDto.from(created));
    }

    @PutMapping("/api/todos/{id}")
    public TodoDto update(@PathVariable("id") Long id, @RequestBody Todo todo) {
        findTodoOrThrow(id);
        todo.setId(id);
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @DeleteMapping("/api/todos/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        findTodoOrThrow(id);
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(exception.getBody());
    }

    private Todo findTodoOrThrow(Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND, "Todo with id " + id + " was not found.");
            problem.setType(URI.create("about:blank"));
            problem.setTitle("Not Found");
            problem.setInstance(URI.create("/api/todos/" + id));
            throw new ErrorResponseException(HttpStatus.NOT_FOUND, problem, null);
        }
        return todo;
    }
}
