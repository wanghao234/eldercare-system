package com.wanghao.eldercare.eldercaresystem.controller.shift;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.dto.shift.BatchStaffShiftScheduleRequest;
import com.wanghao.eldercare.eldercaresystem.dto.shift.CopyWeekStaffShiftRequest;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftBatchResultDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftPageResponse;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftScheduleDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftStaffOptionDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftStatsDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftWeekViewDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.UpsertStaffShiftScheduleRequest;
import com.wanghao.eldercare.eldercaresystem.service.shift.StaffShiftScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff-shifts")
public class StaffShiftScheduleController {

    private final StaffShiftScheduleService staffShiftScheduleService;
    private final CurrentUserUtils currentUserUtils;

    public StaffShiftScheduleController(StaffShiftScheduleService staffShiftScheduleService,
                                        CurrentUserUtils currentUserUtils) {
        this.staffShiftScheduleService = staffShiftScheduleService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping
    public ApiResponse<List<StaffShiftScheduleDTO>> list(@RequestParam(required = false) Long staffId,
                                                         @RequestParam(required = false) String shiftType,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(staffShiftScheduleService.list(currentUser, staffId, shiftType, status, date, startDate, endDate));
    }

    @GetMapping("/my")
    public ApiResponse<List<StaffShiftScheduleDTO>> myShifts(@RequestParam(required = false) String view,
                                                             @RequestParam(required = false) String shiftType,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(staffShiftScheduleService.myShifts(currentUser, view, shiftType, status, date, startDate, endDate));
    }

    @GetMapping("/page")
    public ApiResponse<StaffShiftPageResponse> page(@RequestParam(required = false) Long staffId,
                                                    @RequestParam(required = false) String shiftType,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(staffShiftScheduleService.page(currentUser, staffId, shiftType, status, startDate, endDate, page, size));
    }

    @GetMapping("/week")
    public ApiResponse<List<StaffShiftWeekViewDTO>> week(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                         @RequestParam(required = false) Long staffId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(staffShiftScheduleService.week(currentUser, startDate, endDate, staffId));
    }

    @GetMapping("/stats")
    public ApiResponse<StaffShiftStatsDTO> stats(@RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(staffShiftScheduleService.stats(currentUser, date));
    }

    @GetMapping("/staff-options")
    public ApiResponse<List<StaffShiftStaffOptionDTO>> staffOptions() {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(staffShiftScheduleService.staffOptions(currentUser));
    }

    @PostMapping
    public ApiResponse<StaffShiftScheduleDTO> create(@Valid @RequestBody UpsertStaffShiftScheduleRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("排班创建成功", staffShiftScheduleService.create(currentUser, request));
    }

    @PutMapping("/{shiftId}")
    public ApiResponse<StaffShiftScheduleDTO> update(@PathVariable Long shiftId,
                                                     @Valid @RequestBody UpsertStaffShiftScheduleRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("排班修改成功", staffShiftScheduleService.update(currentUser, shiftId, request));
    }

    @DeleteMapping("/{shiftId}")
    public ApiResponse<Void> delete(@PathVariable Long shiftId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        staffShiftScheduleService.delete(currentUser, shiftId);
        return ApiResponse.success("排班已取消", null);
    }

    @PutMapping("/{shiftId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long shiftId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        staffShiftScheduleService.cancel(currentUser, shiftId);
        return ApiResponse.success("排班已取消", null);
    }

    @PostMapping("/batch")
    public ApiResponse<StaffShiftBatchResultDTO> batchCreate(@Valid @RequestBody BatchStaffShiftScheduleRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("批量排班创建成功", staffShiftScheduleService.batchCreate(currentUser, request));
    }

    @PostMapping("/copy-week")
    public ApiResponse<StaffShiftBatchResultDTO> copyWeek(@Valid @RequestBody CopyWeekStaffShiftRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.success("复制上周排班成功", staffShiftScheduleService.copyWeek(currentUser, request));
    }
}
