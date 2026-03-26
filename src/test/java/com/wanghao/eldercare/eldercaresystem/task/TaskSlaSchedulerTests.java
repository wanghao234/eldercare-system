package com.wanghao.eldercare.eldercaresystem.task;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.task.*;
import com.wanghao.eldercare.eldercaresystem.dto.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.task.*;
import com.wanghao.eldercare.eldercaresystem.mapper.task.*;
import com.wanghao.eldercare.eldercaresystem.service.task.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TaskSlaSchedulerTests {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskSlaScheduler taskSlaScheduler;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void overdue_refresh_marks_pending_task_to_overdue() {
        Task task = new Task();
        task.setTaskType("care");
        task.setTitle("逾期刷新测试");
        task.setStatus("pending");
        task.setDueAt(LocalDateTime.now().minusMinutes(10));
        task.setCreatedAt(LocalDateTime.now().minusHours(1));
        task.setUpdatedAt(LocalDateTime.now().minusHours(1));
        task = taskRepository.save(task);

        int updated = taskSlaScheduler.refreshOverdueTasksNow();
        assertThat(updated).isGreaterThanOrEqualTo(1);

        Task after = taskRepository.findById(task.getTaskId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo("overdue");
    }
}
