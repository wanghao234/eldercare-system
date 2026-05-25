package com.wanghao.eldercare.eldercaresystem.mapper.careplan;

import com.wanghao.eldercare.eldercaresystem.entity.careplan.CarePlanTask;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarePlanTaskRepository extends JpaRepository<CarePlanTask, Long> {

    boolean existsByCarePlanId(Long carePlanId);

    boolean existsByCarePlanIdAndStatusIn(Long carePlanId, List<String> statuses);

    List<CarePlanTask> findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(Long carePlanId);

    List<CarePlanTask> findAllByCarePlanIdAndStatusOrderByScheduledAtAscCreatedAtAscTaskIdAsc(Long carePlanId, String status);

    List<CarePlanTask> findAllByAssignedNurseIdAndStatusNotInOrderByScheduledAtAscCreatedAtAscTaskIdAsc(Long assignedNurseId, List<String> statuses);

    List<CarePlanTask> findAllByCarePlanIdAndStatusInOrderByScheduledAtAscCreatedAtAscTaskIdAsc(Long carePlanId, Collection<String> statuses);

    List<CarePlanTask> findAllByAssignedNurseIdInAndScheduledDateInAndStatusNotIn(Collection<Long> assignedNurseIds,
                                                                                   Collection<LocalDate> scheduledDates,
                                                                                   Collection<String> excludedStatuses);

    List<CarePlanTask> findAllByStatusIgnoreCaseOrderByScheduledAtAscCreatedAtAscTaskIdAsc(String status);
}
