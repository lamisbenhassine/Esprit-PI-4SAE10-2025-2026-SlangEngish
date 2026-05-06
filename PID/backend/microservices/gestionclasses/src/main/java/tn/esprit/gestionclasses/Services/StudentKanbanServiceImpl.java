package tn.esprit.gestionclasses.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gestionclasses.DTO.*;
import tn.esprit.gestionclasses.Entities.KanbanColumn;
import tn.esprit.gestionclasses.Entities.StudentKanbanTask;
import tn.esprit.gestionclasses.Entities.TaskImportance;
import tn.esprit.gestionclasses.Repositories.StudentKanbanTaskRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("studentKanbanServiceImpl")
@RequiredArgsConstructor
public class StudentKanbanServiceImpl implements IStudentKanbanService {

    private static final Comparator<StudentKanbanTask> DISPLAY_ORDER = Comparator
            .comparing(StudentKanbanTask::getImportance, Comparator.comparing(TaskImportance::getSortOrder))
            .thenComparingInt(StudentKanbanTask::getPositionInColumn)
            .thenComparing(StudentKanbanTask::getId);

    private final StudentKanbanTaskRepository taskRepository;

    @Override
    public KanbanBoardResponseDto getBoard(Long userId) {
        return KanbanBoardResponseDto.builder()
                .toDo(mapSortedColumn(userId, KanbanColumn.TODO))
                .doing(mapSortedColumn(userId, KanbanColumn.DOING))
                .done(mapSortedColumn(userId, KanbanColumn.DONE))
                .build();
    }

    @Override
    @Transactional
    public StudentKanbanTaskResponseDto createTask(Long userId, StudentKanbanTaskRequestDto request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }
        KanbanColumn col = request.getColumnStatus() != null ? request.getColumnStatus() : KanbanColumn.TODO;
        TaskImportance imp = request.getImportance() != null ? request.getImportance() : TaskImportance.MEDIUM;

        List<StudentKanbanTask> inColumn = taskRepository.findByUserIdAndColumnStatus(userId, col);
        int nextPos = inColumn.stream().mapToInt(StudentKanbanTask::getPositionInColumn).max().orElse(-1) + 1;

        StudentKanbanTask task = new StudentKanbanTask();
        task.setUserId(userId);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setColumnStatus(col);
        task.setImportance(imp);
        task.setPositionInColumn(nextPos);

        StudentKanbanTask saved = taskRepository.save(task);
        return toDto(saved);
    }

    @Override
    @Transactional
    public StudentKanbanTaskResponseDto updateTask(Long userId, Long taskId, StudentKanbanTaskRequestDto request) {
        StudentKanbanTask task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable"));
        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new IllegalArgumentException("Le titre ne peut pas être vide");
            }
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getImportance() != null) {
            task.setImportance(request.getImportance());
        }
        KanbanColumn column = task.getColumnStatus();
        taskRepository.save(task);
        reindexColumn(userId, column);
        return toDto(taskRepository.findByIdAndUserId(taskId, userId).orElseThrow());
    }

    @Override
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        StudentKanbanTask task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable"));
        KanbanColumn col = task.getColumnStatus();
        taskRepository.delete(task);
        reindexColumn(userId, col);
    }

    @Override
    @Transactional
    public StudentKanbanTaskResponseDto moveTask(Long userId, Long taskId, MoveKanbanTaskRequestDto request) {
        if (request.getTargetColumn() == null) {
            throw new IllegalArgumentException("targetColumn est obligatoire");
        }
        StudentKanbanTask task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable"));

        KanbanColumn oldCol = task.getColumnStatus();
        KanbanColumn newCol = request.getTargetColumn();

        List<StudentKanbanTask> oldList = sortedTasksInColumn(userId, oldCol);
        oldList.removeIf(t -> t.getId().equals(taskId));
        for (int i = 0; i < oldList.size(); i++) {
            oldList.get(i).setPositionInColumn(i);
            oldList.get(i).setColumnStatus(oldCol);
        }
        taskRepository.saveAll(oldList);

        task.setColumnStatus(newCol);
        List<StudentKanbanTask> newList = sortedTasksInColumn(userId, newCol);
        newList.removeIf(t -> t.getId().equals(taskId));

        int newIndex = request.getTargetIndex();
        if (newIndex < 0) {
            newIndex = 0;
        }
        if (newIndex > newList.size()) {
            newIndex = newList.size();
        }
        newList.add(newIndex, task);

        for (int i = 0; i < newList.size(); i++) {
            newList.get(i).setPositionInColumn(i);
            newList.get(i).setColumnStatus(newCol);
        }
        taskRepository.saveAll(newList);

        return toDto(task);
    }

    private void reindexColumn(Long userId, KanbanColumn column) {
        List<StudentKanbanTask> list = sortedTasksInColumn(userId, column);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setPositionInColumn(i);
            list.get(i).setColumnStatus(column);
        }
        taskRepository.saveAll(list);
    }

    private List<StudentKanbanTask> sortedTasksInColumn(Long userId, KanbanColumn column) {
        List<StudentKanbanTask> all = new ArrayList<>(taskRepository.findByUserIdAndColumnStatus(userId, column));
        all.sort(DISPLAY_ORDER);
        return all;
    }

    private List<StudentKanbanTaskResponseDto> mapSortedColumn(Long userId, KanbanColumn column) {
        return sortedTasksInColumn(userId, column).stream().map(this::toDto).toList();
    }

    private StudentKanbanTaskResponseDto toDto(StudentKanbanTask t) {
        return StudentKanbanTaskResponseDto.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .title(t.getTitle())
                .description(t.getDescription())
                .columnStatus(t.getColumnStatus())
                .importance(t.getImportance())
                .positionInColumn(t.getPositionInColumn())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
