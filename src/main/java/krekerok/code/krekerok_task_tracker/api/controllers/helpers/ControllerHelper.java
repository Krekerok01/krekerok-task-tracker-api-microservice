package krekerok.code.krekerok_task_tracker.api.controllers.helpers;

import krekerok.code.krekerok_task_tracker.api.exceptions.NotFoundException;
import krekerok.code.krekerok_task_tracker.store.entities.ProjectEntity;
import krekerok.code.krekerok_task_tracker.store.entities.TaskEntity;
import krekerok.code.krekerok_task_tracker.store.entities.TaskStateEntity;
import krekerok.code.krekerok_task_tracker.store.repositories.ProjectRepository;
import krekerok.code.krekerok_task_tracker.store.repositories.TaskRepository;
import krekerok.code.krekerok_task_tracker.store.repositories.TaskStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@RequiredArgsConstructor
@Component
@Transactional
public class ControllerHelper {

    private final ProjectRepository projectRepository;
    private final TaskStateRepository taskStateRepository;
    private final TaskRepository taskRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Project with \"%s\" doesn't exist.", projectId)
                        )
                );
    }

   public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository.findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Task state with \"%s\" doesn't exist.", taskStateId)
                        )
                );
    }

    public TaskEntity getTaskOrThrowException(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Task with \"%s\" doesn't exist.", taskId)
                        )
                );
    }
}
