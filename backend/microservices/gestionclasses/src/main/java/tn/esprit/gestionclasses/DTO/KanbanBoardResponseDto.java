package tn.esprit.gestionclasses.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanBoardResponseDto {

    private List<StudentKanbanTaskResponseDto> toDo;
    private List<StudentKanbanTaskResponseDto> doing;
    private List<StudentKanbanTaskResponseDto> done;
}
