package krekerok.code.krekerok_task_tracker.api.controllers.helpers;

import krekerok.code.krekerok_task_tracker.api.exceptions.NotFoundException;
import krekerok.code.krekerok_task_tracker.store.entities.ProjectEntity;
import krekerok.code.krekerok_task_tracker.store.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@RequiredArgsConstructor
@Component
@Transactional
public class ControllerHelper {

    private final ProjectRepository projectRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Project with \"%s\" doesn't exist.", projectId)
                        )
                );
    }
}
