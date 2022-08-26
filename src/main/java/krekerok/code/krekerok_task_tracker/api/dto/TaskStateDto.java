package krekerok.code.krekerok_task_tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import krekerok.code.krekerok_task_tracker.store.entities.TaskEntity;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStateDto {

    @NonNull
    private Long id;

    @NonNull
    private String name;

    @JsonProperty("left_task_state_id")
    private Long leftTaskStateId;

    @JsonProperty("right_task_state_id")
    private Long rightTaskStateId;

    @NonNull
    @JsonProperty("created_at")
    private Instant createdAt;

    @NonNull
    private List<TaskDto> tasks;
}
