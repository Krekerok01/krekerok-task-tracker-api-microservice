package krekerok.code.krekerok_task_tracker.api.factories;


import krekerok.code.krekerok_task_tracker.api.dto.ProjectDto;
import krekerok.code.krekerok_task_tracker.store.entities.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto(ProjectEntity entity) {
        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
