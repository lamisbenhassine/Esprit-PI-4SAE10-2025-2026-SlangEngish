package tn.esprit.gestionclasses.Controllers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gestionclasses.DTO.*;
import tn.esprit.gestionclasses.Services.IStudentKanbanService;

@RestController
@RequestMapping("/kanban")
public class KanbanController {

    private final IStudentKanbanService kanbanService;

    public KanbanController(@Qualifier("studentKanbanServiceImpl") IStudentKanbanService kanbanService) {
        this.kanbanService = kanbanService;
    }

    @GetMapping("/board")
    public ResponseEntity<KanbanBoardResponseDto> getBoard(@RequestParam Long userId) {
        return ResponseEntity.ok(kanbanService.getBoard(userId));
    }

    @PostMapping("/tasks")
    public ResponseEntity<StudentKanbanTaskResponseDto> createTask(
            @RequestParam Long userId,
            @RequestBody StudentKanbanTaskRequestDto body) {
        try {
            StudentKanbanTaskResponseDto created = kanbanService.createTask(userId, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<StudentKanbanTaskResponseDto> updateTask(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody StudentKanbanTaskRequestDto body) {
        try {
            return ResponseEntity.ok(kanbanService.updateTask(userId, id, body));
        } catch (IllegalArgumentException e) {
            if ("Tâche introuvable".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/tasks/{id}/move")
    public ResponseEntity<StudentKanbanTaskResponseDto> moveTask(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody MoveKanbanTaskRequestDto body) {
        try {
            return ResponseEntity.ok(kanbanService.moveTask(userId, id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, @RequestParam Long userId) {
        try {
            kanbanService.deleteTask(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
