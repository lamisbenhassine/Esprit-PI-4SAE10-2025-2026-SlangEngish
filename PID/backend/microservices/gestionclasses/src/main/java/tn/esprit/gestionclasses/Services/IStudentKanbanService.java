package tn.esprit.gestionclasses.Services;

import tn.esprit.gestionclasses.DTO.*;

public interface IStudentKanbanService {

    KanbanBoardResponseDto getBoard(Long userId);

    StudentKanbanTaskResponseDto createTask(Long userId, StudentKanbanTaskRequestDto request);

    StudentKanbanTaskResponseDto updateTask(Long userId, Long taskId, StudentKanbanTaskRequestDto request);

    void deleteTask(Long userId, Long taskId);

    StudentKanbanTaskResponseDto moveTask(Long userId, Long taskId, MoveKanbanTaskRequestDto request);
}
