package krekerok.code.krekerok_task_tracker.api.controllers;

import krekerok.code.krekerok_task_tracker.api.controllers.helpers.ControllerHelper;
import krekerok.code.krekerok_task_tracker.api.dto.AskDto;
import krekerok.code.krekerok_task_tracker.api.dto.TaskDto;
import krekerok.code.krekerok_task_tracker.api.exceptions.BadRequestException;
import krekerok.code.krekerok_task_tracker.api.factories.TaskDtoFactory;
import krekerok.code.krekerok_task_tracker.store.entities.TaskEntity;
import krekerok.code.krekerok_task_tracker.store.entities.TaskStateEntity;
import krekerok.code.krekerok_task_tracker.store.repositories.TaskRepository;
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
    private final TaskDtoFactory taskDtoFactory;

    private final ControllerHelper controllerHelper;

    public static final String GET_TASKS = "/api/task-states/{task_state_id}/tasks";
    public static final String CREATE_TASK = "/api/task-states/{task_state_id}/tasks";
    public static final String UPDATE_TASK = "/api/tasks/{task_id}";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(value = "task_state_id") Long taskStateId) {

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        return taskState.getTasks()
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());

    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(@PathVariable(value = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_name") String taskName,
            @RequestParam(name = "task_description") String taskDescription){

        if (taskName.trim().isEmpty() || taskDescription.trim().isEmpty()) throw new BadRequestException("Information about task can't be empty.");

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);
        checkingForATaskNameMatch(taskState.getTasks(), taskName);

        TaskEntity task = buildTaskByTaskNameAndTaskDescriptionAndTaskStateAndAfterSaveToTheDB(taskName, taskDescription, taskState);
        return taskDtoFactory.makeTaskDto(taskRepository.saveAndFlush(task));
    }



    @PatchMapping(UPDATE_TASK)
    public TaskDto updateTask( @PathVariable(value = "task_id") Long taskId,
            @RequestParam(value = "task_name", required = false) Optional<String> optionalTaskName,
            @RequestParam(value = "task_description", required = false) Optional<String> optionalTaskDescription
    ) {

        optionalTaskName = optionalTaskName.filter(taskName -> !taskName.trim().isEmpty());
        optionalTaskDescription = optionalTaskDescription.filter(taskDescription -> !taskDescription.trim().isEmpty());

        checkingTheTaskNameAndTheTaskDescriptionForExistence(optionalTaskName, optionalTaskDescription);

        TaskEntity task = checkingTheExistenceOfTheTaskByIdAndSetNewParams(taskId, optionalTaskName, optionalTaskDescription);

        return taskDtoFactory.makeTaskDto(taskRepository.saveAndFlush(task));
    }


    @DeleteMapping(DELETE_TASK)
    public AskDto deleteTask(@PathVariable("task_id") Long taskId){
        controllerHelper.getTaskOrThrowException(taskId);
        taskRepository.deleteById(taskId);
        return AskDto.makeDefault(true);
    }

    public TaskEntity buildTaskByTaskNameAndTaskDescriptionAndTaskStateAndAfterSaveToTheDB(String taskName, String taskDescription, TaskStateEntity taskState){
        return taskRepository.saveAndFlush(TaskEntity.builder().name(taskName)
                .description(taskDescription)
                .taskState(taskState)
                .build());
    }

    private void checkingForATaskNameMatch(List<TaskEntity> tasks, String taskName){
        for (TaskEntity task: tasks){
            if (task.getName().equalsIgnoreCase(taskName)){
                throw new BadRequestException(String.format("Task \"%s\" already exists.", taskName));
            }
        }
    }

    private TaskEntity checkingTheExistenceOfTheTaskByIdAndSetNewParams(Long taskId, Optional<String> optionalTaskName, Optional<String> optionalTaskDescription) {
        TaskEntity task = controllerHelper.getTaskOrThrowException(taskId);

        optionalTaskName.ifPresent(taskName -> {task.setName(taskName);});
        optionalTaskDescription.ifPresent(taskDescription -> {task.setDescription(taskDescription);});

        return task;
    }


    private void checkingTheTaskNameAndTheTaskDescriptionForExistence(Optional<String> optionalTaskName, Optional<String> optionalTaskDescription){
        if (!optionalTaskName.isPresent() && !optionalTaskDescription.isPresent()){
            throw new BadRequestException("Task name and task description can't be empty at the same time.");
        }
    }
}
