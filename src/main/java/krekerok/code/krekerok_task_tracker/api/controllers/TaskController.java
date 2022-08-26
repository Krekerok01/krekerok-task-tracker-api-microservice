package krekerok.code.krekerok_task_tracker.api.controllers;

import krekerok.code.krekerok_task_tracker.api.controllers.helpers.ControllerHelper;
import krekerok.code.krekerok_task_tracker.api.dto.TaskDto;
import krekerok.code.krekerok_task_tracker.api.dto.TaskStateDto;
import krekerok.code.krekerok_task_tracker.api.exceptions.BadRequestException;
import krekerok.code.krekerok_task_tracker.api.exceptions.NotFoundException;
import krekerok.code.krekerok_task_tracker.api.factories.TaskDtoFactory;
import krekerok.code.krekerok_task_tracker.api.factories.TaskStateDtoFactory;
import krekerok.code.krekerok_task_tracker.store.entities.ProjectEntity;
import krekerok.code.krekerok_task_tracker.store.entities.TaskEntity;
import krekerok.code.krekerok_task_tracker.store.entities.TaskStateEntity;
import krekerok.code.krekerok_task_tracker.store.repositories.ProjectRepository;
import krekerok.code.krekerok_task_tracker.store.repositories.TaskRepository;
import krekerok.code.krekerok_task_tracker.store.repositories.TaskStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@RestController
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskStateRepository taskStateRepository;
    private final TaskDtoFactory taskDtoFactory;


    public static final String GET_TASKS = "/api/task-states/{task_state_id}/tasks";
    public static final String CREATE_TASK = "/api/task-states/{task_state_id}/tasks";
    public static final String UPDATE_TASK = "/api/tasks/{task_id}";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(value = "task_state_id") Long taskStateId) {

        TaskStateEntity taskState = getTaskStateOrThrowExeption(taskStateId);

        return taskState
                .getTasks()
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());

    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(
            @PathVariable(value = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_name") String taskName,
            @RequestParam(name = "task_description") String taskDescription){

        if (taskName.trim().isEmpty() || taskDescription.trim().isEmpty()){
            throw new BadRequestException("Information about task can't be empty.");
        }

        TaskStateEntity taskState = getTaskStateOrThrowExeption(taskStateId);

        for (TaskEntity task: taskState.getTasks()){
            if (task.getName().equalsIgnoreCase(taskName)){
                throw new BadRequestException(String.format("Task \"%s\" already exists.", taskName));
            }
        }

        TaskEntity task = taskRepository.saveAndFlush(
                TaskEntity.builder()
                        .name(taskName)
                        .description(taskDescription)
                        .taskState(taskState)
                        .build()
        );


        final TaskEntity savedTask = taskRepository.saveAndFlush(task);

        return taskDtoFactory.makeTaskDto(savedTask);
    }

    private TaskStateEntity getTaskStateOrThrowExeption(Long taskStateId) {
        return taskStateRepository.findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Task state with \"%s\" doesn't exist.", taskStateId)
                        )
                );
    }

}
