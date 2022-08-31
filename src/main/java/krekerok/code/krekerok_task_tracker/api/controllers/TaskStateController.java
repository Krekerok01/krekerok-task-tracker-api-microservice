package krekerok.code.krekerok_task_tracker.api.controllers;

import krekerok.code.krekerok_task_tracker.api.controllers.helpers.ControllerHelper;
import krekerok.code.krekerok_task_tracker.api.dto.AskDto;
import krekerok.code.krekerok_task_tracker.api.dto.TaskStateDto;
import krekerok.code.krekerok_task_tracker.api.exceptions.BadRequestException;
import krekerok.code.krekerok_task_tracker.api.exceptions.NotFoundException;
import krekerok.code.krekerok_task_tracker.api.factories.TaskStateDtoFactory;
import krekerok.code.krekerok_task_tracker.store.entities.ProjectEntity;
import krekerok.code.krekerok_task_tracker.store.entities.TaskStateEntity;
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
public class TaskStateController {

    private final TaskStateRepository taskStateRepository;
    private final TaskStateDtoFactory taskStateDtoFactory;
    private final ControllerHelper controllerHelper;

    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    public static final String UPDATE_TASK_STATE = "/api/task-states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/task-states/{task_state_id}/position/change";
    public static final String DELETE_TASK_STATE = "/api/task-states/{task_state_id}";


    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(value = "project_id") Long projectId) {

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        return project.getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());

    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(@PathVariable(value = "project_id") Long projectId, @RequestParam(name = "task_state_name") String taskStateName){

        checkingTheExistenceOfTaskStateName(taskStateName);
        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();
        optionalAnotherTaskState = getOptionalAnotherTaskStateAndComparisonByEqualityOfNames(taskStateName, project, optionalAnotherTaskState);


        TaskStateEntity taskState = taskStateRepository.saveAndFlush(TaskStateEntity.builder().name(taskStateName).project(project).build());

        optionalAnotherTaskState.ifPresent(anotherTaskState -> {
                   taskState.setLeftTaskState(anotherTaskState);
                   anotherTaskState.setRightTaskState(taskState);
                   taskStateRepository.saveAndFlush(anotherTaskState);});

        return taskStateDtoFactory.makeTaskStateDto(taskStateRepository.saveAndFlush(taskState));
    }


    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(
            @PathVariable(value = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName){

        checkingTheExistenceOfTaskStateName(taskStateName);
        TaskStateEntity taskState = getTaskStateOrThrowException(taskStateId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                        taskState.getProject().getId(),
                        taskStateName)
                        .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                        .ifPresent(anotherTaskState -> {
                            throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
                        });

        taskState.setName(taskStateName);

        return taskStateDtoFactory.makeTaskStateDto(taskStateRepository.saveAndFlush(taskState));
    }


    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
            @PathVariable(value = "task_state_id") Long taskStateId,
            @RequestParam(name = "left_task_state_id") Optional<Long> optionalLeftTaskStateId){

        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskStateId);
        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState.getLeftTaskState().map(TaskStateEntity::getId);


        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) return taskStateDtoFactory.makeTaskStateDto(changeTaskState);


        Optional<TaskStateEntity> optionalNewLeftTaskState = getOptionalNewLeftTaskStateOrThrowException(taskStateId, optionalLeftTaskStateId, project);
        Optional<TaskStateEntity> optionalNewRightTaskState = getOptionalNewRightTaskState(optionalNewLeftTaskState, project);


        replaceOldTaskStatePosition(changeTaskState);

        setLeftTaskStateToTheChangeTaskState(changeTaskState, optionalNewLeftTaskState);
        setRightTaskStateToTheChangeTaskState(changeTaskState, optionalNewRightTaskState);

        changeTaskState = taskStateRepository.saveAndFlush(changeTaskState);
        optionalNewLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);
        optionalNewRightTaskState.ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
    }

    private void setRightTaskStateToTheChangeTaskState(TaskStateEntity changeTaskState, Optional<TaskStateEntity> optionalNewRightTaskState) {
        if (optionalNewRightTaskState.isPresent()){

            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(changeTaskState);
            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }
    }

    private void setLeftTaskStateToTheChangeTaskState(TaskStateEntity changeTaskState, Optional<TaskStateEntity> optionalNewLeftTaskState) {
        if (optionalNewLeftTaskState.isPresent()){

            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(changeTaskState);
            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }
    }


    @DeleteMapping(DELETE_TASK_STATE)
    public AskDto deleteTaskState(@PathVariable(value = "task_state_id") Long taskStateId){

        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskStateId);

        replaceOldTaskStatePosition(changeTaskState);

        taskStateRepository.saveAndFlush(changeTaskState);
        taskStateRepository.delete(changeTaskState);

        return AskDto.builder().answer(true).build();
    }


    private Optional<TaskStateEntity> getOptionalAnotherTaskStateAndComparisonByEqualityOfNames(String taskStateName, ProjectEntity project, Optional<TaskStateEntity> optionalAnotherTaskState) {
        for (TaskStateEntity taskState: project.getTaskStates()){
            if (taskState.getName().equalsIgnoreCase(taskStateName)){
                throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
            }

            if (!taskState.getRightTaskState().isPresent()){
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }
        return optionalAnotherTaskState;
    }

    private Optional<TaskStateEntity> getOptionalNewLeftTaskStateOrThrowException(Long taskStateId, Optional<Long> optionalLeftTaskStateId, ProjectEntity project) {
        return optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) throw new BadRequestException("Left task state id equals changed task state id.");

                    TaskStateEntity leftTaskStateEntity = getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) throw new BadRequestException("Tsk state position can be changed within the same project.");

                    return leftTaskStateEntity;
                });
    }

    private Optional<TaskStateEntity> getOptionalNewRightTaskState( Optional<TaskStateEntity> optionalNewLeftTaskState, ProjectEntity project){
        Optional<TaskStateEntity> optionalNewRightTaskState;

        if (!optionalNewLeftTaskState.isPresent()) {
            optionalNewRightTaskState = project.getTaskStates().stream()
                    .filter(anotherTaskState -> !anotherTaskState.getLeftTaskState().isPresent())
                    .findAny();
        } else {
            optionalNewRightTaskState = optionalNewLeftTaskState.get().getRightTaskState();
        }

        return optionalNewRightTaskState;
    }

    private void checkingTheExistenceOfTaskStateName(String taskStateName) {
        if (taskStateName.trim().isEmpty()){
            throw new BadRequestException("Task state name can't be empty.");
        }
    }

    private void replaceOldTaskStatePosition(TaskStateEntity changeTaskState) {
        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();


        optionalOldLeftTaskState.ifPresent(it -> {
                    it.setRightTaskState(optionalOldRightTaskState.orElse(null));
                    taskStateRepository.saveAndFlush(it);});

        optionalOldRightTaskState.ifPresent(it -> {
                    it.setLeftTaskState(optionalOldLeftTaskState.orElse(null));
                    taskStateRepository.saveAndFlush(it);});
    }

    private TaskStateEntity getTaskStateOrThrowException(Long taskStateId){
        return taskStateRepository.findById(taskStateId)
                                  .orElseThrow(() -> new NotFoundException(String.format("Task state with \"%s\" id doesn't exist.", taskStateId)));

    }
}
