package com.wanghao.eldercare.eldercaresystem.service.shift;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ConflictBusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.dto.shift.BatchStaffShiftScheduleRequest;
import com.wanghao.eldercare.eldercaresystem.dto.shift.CopyWeekStaffShiftRequest;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftBatchResultDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftConflictDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftConflictResponse;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftPageResponse;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftScheduleDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftStaffOptionDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftStatsDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftWeekDayDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.StaffShiftWeekViewDTO;
import com.wanghao.eldercare.eldercaresystem.dto.shift.UpsertStaffShiftScheduleRequest;
import com.wanghao.eldercare.eldercaresystem.entity.shift.StaffShiftSchedule;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.StaffShiftScheduleRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StaffShiftScheduleService {

    private static final Set<String> SCHEDULABLE_ROLES = Set.of("nurse", "caregiver");
    private static final Set<String> ALLOWED_SHIFT_TYPES = Set.of("morning", "afternoon", "night", "full_day");
    private static final Set<String> ALLOWED_REPEAT_TYPES = Set.of("daily", "workday", "weekly");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StaffShiftScheduleRepository staffShiftScheduleRepository;
    private final UserRepository userRepository;

    public StaffShiftScheduleService(StaffShiftScheduleRepository staffShiftScheduleRepository,
                                     UserRepository userRepository) {
        this.staffShiftScheduleRepository = staffShiftScheduleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffShiftScheduleDTO> list(CurrentUser currentUser,
                                            Long staffId,
                                            String shiftType,
                                            String status,
                                            LocalDate date,
                                            LocalDate startDate,
                                            LocalDate endDate) {
        requireManager(currentUser);
        List<StaffShiftSchedule> rows = staffShiftScheduleRepository.searchAdvanced(
                staffId,
                normalizeShiftTypeFilter(shiftType),
                normalizeStatusFilter(status),
                date,
                startDate,
                endDate
        );
        return mapDtos(rows);
    }

    @Transactional(readOnly = true)
    public List<StaffShiftScheduleDTO> myShifts(CurrentUser currentUser,
                                                String view,
                                                String shiftType,
                                                String status,
                                                LocalDate date,
                                                LocalDate startDate,
                                                LocalDate endDate) {
        requireShiftViewer(currentUser);
        QueryWindow window = resolveMyShiftWindow(view, date, startDate, endDate);
        List<StaffShiftSchedule> rows = staffShiftScheduleRepository.searchAdvanced(
                currentUser.getUserId(),
                normalizeShiftTypeFilter(shiftType),
                normalizeStatusFilter(status),
                window.date(),
                window.startDate(),
                window.endDate()
        );
        return mapDtos(rows);
    }

    @Transactional(readOnly = true)
    public StaffShiftPageResponse page(CurrentUser currentUser,
                                       Long staffId,
                                       String shiftType,
                                       String status,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       int page,
                                       int size) {
        requireManager(currentUser);
        Page<StaffShiftSchedule> result = staffShiftScheduleRepository.searchPage(
                staffId,
                normalizeShiftTypeFilter(shiftType),
                normalizeStatusFilter(status),
                startDate,
                endDate,
                PageRequest.of(page, size, Sort.by(
                        Sort.Order.asc("shiftDate"),
                        Sort.Order.asc("startTime"),
                        Sort.Order.asc("staffId"),
                        Sort.Order.asc("shiftId")
                ))
        );
        StaffShiftPageResponse response = new StaffShiftPageResponse();
        response.setContent(mapDtos(result.getContent()));
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public List<StaffShiftWeekViewDTO> week(CurrentUser currentUser, LocalDate startDate, LocalDate endDate, Long staffId) {
        requireManager(currentUser);
        validateDateRange(startDate, endDate, "周视图日期范围非法");
        List<StaffShiftStaffOptionDTO> staffOptions = resolveStaffOptions(staffId);
        List<LocalDate> dates = enumerateDates(startDate, endDate);
        List<StaffShiftSchedule> rows = staffShiftScheduleRepository.searchAdvanced(
                staffId,
                null,
                null,
                null,
                startDate,
                endDate
        );
        Map<Long, Map<LocalDate, List<StaffShiftScheduleDTO>>> scheduleMap = mapDtos(rows).stream()
                .collect(Collectors.groupingBy(
                        StaffShiftScheduleDTO::getStaffId,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                StaffShiftScheduleDTO::getShiftDate,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));
        List<StaffShiftWeekViewDTO> result = new ArrayList<>();
        for (StaffShiftStaffOptionDTO option : staffOptions) {
            StaffShiftWeekViewDTO staffWeek = new StaffShiftWeekViewDTO();
            staffWeek.setStaffId(option.getStaffId());
            staffWeek.setStaffName(option.getStaffName());
            staffWeek.setRoleName(option.getRoleName());
            List<StaffShiftWeekDayDTO> days = new ArrayList<>();
            Map<LocalDate, List<StaffShiftScheduleDTO>> byDate = scheduleMap.getOrDefault(option.getStaffId(), Map.of());
            for (LocalDate date : dates) {
                StaffShiftWeekDayDTO day = new StaffShiftWeekDayDTO();
                day.setDate(date);
                day.setWeekDay(toWeekDay(date));
                day.setShifts(byDate.getOrDefault(date, List.of()));
                days.add(day);
            }
            staffWeek.setDays(days);
            result.add(staffWeek);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public StaffShiftStatsDTO stats(CurrentUser currentUser, LocalDate date) {
        requireManager(currentUser);
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        List<StaffShiftSchedule> rows = staffShiftScheduleRepository
                .findAllByShiftDateBetweenOrderByShiftDateAscStaffIdAscStartTimeAscShiftIdAsc(targetDate, targetDate);
        StaffShiftStatsDTO stats = new StaffShiftStatsDTO();
        stats.setOnDutyCount(rows.stream()
                .filter(this::isActive)
                .map(StaffShiftSchedule::getStaffId)
                .distinct()
                .count());
        stats.setMorningCount(countDistinctStaffByShiftType(rows, "morning"));
        stats.setAfternoonCount(countDistinctStaffByShiftType(rows, "afternoon"));
        stats.setNightCount(countDistinctStaffByShiftType(rows, "night"));
        stats.setFullDayCount(countDistinctStaffByShiftType(rows, "full_day"));
        stats.setCancelledCount(rows.stream().filter(item -> "cancelled".equals(item.getStatus())).count());
        stats.setConflictCount(countConflictShifts(rows));
        return stats;
    }

    @Transactional
    public StaffShiftScheduleDTO create(CurrentUser currentUser, UpsertStaffShiftScheduleRequest request) {
        requireManager(currentUser);
        User staff = findSchedulableStaff(request.getStaffId());
        ShiftDraft draft = buildDraft(request.getStaffId(), request.getShiftDate(), request.getShiftType(),
                request.getStartTime(), request.getEndTime(), request.getStatus(), request.getRemark());
        assertNoConflict(staff, draft, null);
        return toDto(staffShiftScheduleRepository.save(newEntity(draft)), staff);
    }

    @Transactional
    public StaffShiftScheduleDTO update(CurrentUser currentUser, Long shiftId, UpsertStaffShiftScheduleRequest request) {
        requireManager(currentUser);
        StaffShiftSchedule entity = staffShiftScheduleRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("排班不存在"));
        User staff = findSchedulableStaff(request.getStaffId());
        ShiftDraft draft = buildDraft(request.getStaffId(), request.getShiftDate(), request.getShiftType(),
                request.getStartTime(), request.getEndTime(), request.getStatus(), request.getRemark());
        assertNoConflict(staff, draft, shiftId);
        applyDraft(entity, draft);
        entity.setUpdatedAt(LocalDateTime.now());
        return toDto(staffShiftScheduleRepository.save(entity), staff);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long shiftId) {
        requireManager(currentUser);
        cancelInternal(shiftId);
    }

    @Transactional
    public void cancel(CurrentUser currentUser, Long shiftId) {
        requireManager(currentUser);
        cancelInternal(shiftId);
    }

    @Transactional
    public StaffShiftBatchResultDTO batchCreate(CurrentUser currentUser, BatchStaffShiftScheduleRequest request) {
        requireManager(currentUser);
        validateDateRange(request.getStartDate(), request.getEndDate(), "批量排班日期范围非法");
        User staff = findSchedulableStaff(request.getStaffId());
        ShiftTemplate template = buildTemplate(request.getStaffId(), request.getShiftType(),
                request.getStartTime(), request.getEndTime(), request.getRemark());
        List<LocalDate> dates = generateBatchDates(request.getStartDate(), request.getEndDate(),
                normalizeRepeatType(request.getRepeatType()), request.getWeekDays());
        List<ShiftDraft> drafts = dates.stream()
                .map(date -> template.toDraft(date))
                .toList();
        assertNoConflicts(staff, drafts);
        List<StaffShiftSchedule> saved = staffShiftScheduleRepository.saveAll(drafts.stream().map(this::newEntity).toList());
        StaffShiftBatchResultDTO result = new StaffShiftBatchResultDTO();
        result.setCreatedCount(saved.size());
        result.setItems(mapDtos(saved));
        return result;
    }

    @Transactional
    public StaffShiftBatchResultDTO copyWeek(CurrentUser currentUser, CopyWeekStaffShiftRequest request) {
        requireManager(currentUser);
        LocalDate sourceStart = request.getSourceWeekStart();
        LocalDate targetStart = request.getTargetWeekStart();
        if (sourceStart == null || targetStart == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "周起始日期不能为空", HttpStatus.BAD_REQUEST);
        }
        List<StaffShiftSchedule> source = staffShiftScheduleRepository
                .findAllByShiftDateBetweenAndStatusOrderByShiftDateAscStaffIdAscStartTimeAscShiftIdAsc(
                        sourceStart, sourceStart.plusDays(6), "active");
        Map<Long, User> userMap = loadUserMap(source.stream().map(StaffShiftSchedule::getStaffId).distinct().toList());
        List<ShiftDraft> drafts = new ArrayList<>();
        for (StaffShiftSchedule item : source) {
            drafts.add(buildDraft(
                    item.getStaffId(),
                    targetStart.plusDays(item.getShiftDate().toEpochDay() - sourceStart.toEpochDay()),
                    item.getShiftType(),
                    item.getStartTime(),
                    item.getEndTime(),
                    "active",
                    item.getRemark()
            ));
        }
        assertNoConflicts(userMap, drafts);
        List<StaffShiftSchedule> saved = staffShiftScheduleRepository.saveAll(drafts.stream().map(this::newEntity).toList());
        StaffShiftBatchResultDTO result = new StaffShiftBatchResultDTO();
        result.setCreatedCount(saved.size());
        result.setItems(mapDtos(saved));
        return result;
    }

    @Transactional(readOnly = true)
    public List<StaffShiftStaffOptionDTO> staffOptions(CurrentUser currentUser) {
        requireManager(currentUser);
        return resolveStaffOptions(null);
    }

    private void cancelInternal(Long shiftId) {
        StaffShiftSchedule entity = staffShiftScheduleRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("排班不存在"));
        entity.setStatus("cancelled");
        entity.setUpdatedAt(LocalDateTime.now());
        staffShiftScheduleRepository.save(entity);
    }

    private List<StaffShiftScheduleDTO> mapDtos(List<StaffShiftSchedule> rows) {
        Map<Long, User> userMap = loadUserMap(rows.stream().map(StaffShiftSchedule::getStaffId).distinct().toList());
        return rows.stream()
                .map(item -> toDto(item, userMap.get(item.getStaffId())))
                .toList();
    }

    private Map<Long, User> loadUserMap(Collection<Long> staffIds) {
        Map<Long, User> userMap = new HashMap<>();
        if (staffIds != null && !staffIds.isEmpty()) {
            userRepository.findAllById(staffIds).forEach(user -> userMap.put(user.getUserId(), user));
        }
        return userMap;
    }

    private StaffShiftScheduleDTO toDto(StaffShiftSchedule entity, User user) {
        return StaffShiftScheduleDTO.from(
                entity,
                user == null ? null : firstNonBlank(user.getRealName(), user.getUsername()),
                user == null ? null : toRoleName(user.getRole())
        );
    }

    private User findSchedulableStaff(Long staffId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new NotFoundException("护理人员不存在"));
        String role = normalizeRole(user.getRole());
        if (!SCHEDULABLE_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "排班人员角色必须是 nurse/caregiver", HttpStatus.BAD_REQUEST);
        }
        return user;
    }

    private List<StaffShiftStaffOptionDTO> resolveStaffOptions(Long staffId) {
        if (staffId != null) {
            User user = findSchedulableStaff(staffId);
            return List.of(toStaffOption(user));
        }
        return userRepository.findActiveByRoles(SCHEDULABLE_ROLES, "active").stream()
                .map(this::toStaffOption)
                .toList();
    }

    private StaffShiftStaffOptionDTO toStaffOption(User user) {
        StaffShiftStaffOptionDTO dto = new StaffShiftStaffOptionDTO();
        dto.setStaffId(user.getUserId());
        dto.setStaffName(firstNonBlank(user.getRealName(), user.getUsername()));
        dto.setRoleName(toRoleName(user.getRole()));
        return dto;
    }

    private void requireManager(CurrentUser currentUser) {
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("当前角色无权限维护护理人员排班");
        }
    }

    private void requireShiftViewer(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return;
        }
        if (!SCHEDULABLE_ROLES.contains(normalizeRole(currentUser.getRole()))) {
            throw new AccessDeniedException("当前角色无权限查看我的班表");
        }
    }

    private QueryWindow resolveMyShiftWindow(String view,
                                             LocalDate date,
                                             LocalDate startDate,
                                             LocalDate endDate) {
        String normalizedView = trimToNull(view);
        if (normalizedView == null) {
            normalizedView = "all";
        }
        normalizedView = normalizedView.toLowerCase(Locale.ROOT);
        return switch (normalizedView) {
            case "today" -> new QueryWindow(date == null ? LocalDate.now() : date, null, null);
            case "week" -> {
                LocalDate anchor = date == null ? LocalDate.now() : date;
                LocalDate weekStart = anchor.minusDays(anchor.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
                LocalDate weekEnd = weekStart.plusDays(6);
                yield new QueryWindow(null, weekStart, weekEnd);
            }
            case "range" -> {
                validateDateRange(startDate, endDate, "时间范围查询必须提供合法的 startDate 和 endDate");
                yield new QueryWindow(null, startDate, endDate);
            }
            case "all" -> new QueryWindow(null, null, null);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "view 仅支持 today/week/range/all", HttpStatus.BAD_REQUEST);
        };
    }

    private ShiftDraft buildDraft(Long staffId,
                                  LocalDate shiftDate,
                                  String shiftType,
                                  LocalTime startTime,
                                  LocalTime endTime,
                                  String status,
                                  String remark) {
        if (shiftDate == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "shiftDate 不能为空", HttpStatus.BAD_REQUEST);
        }
        String normalizedShiftType = normalizeShiftType(shiftType);
        LocalTime resolvedStartTime = startTime == null ? defaultStartTime(normalizedShiftType) : startTime;
        LocalTime resolvedEndTime = endTime == null ? defaultEndTime(normalizedShiftType) : endTime;
        validateTimeRange(resolvedStartTime, resolvedEndTime);
        return new ShiftDraft(
                staffId,
                shiftDate,
                normalizedShiftType,
                resolvedStartTime,
                resolvedEndTime,
                normalizeShiftStatus(status, "active"),
                trimToNull(remark)
        );
    }

    private ShiftTemplate buildTemplate(Long staffId,
                                        String shiftType,
                                        LocalTime startTime,
                                        LocalTime endTime,
                                        String remark) {
        String normalizedShiftType = normalizeShiftType(shiftType);
        LocalTime resolvedStartTime = startTime == null ? defaultStartTime(normalizedShiftType) : startTime;
        LocalTime resolvedEndTime = endTime == null ? defaultEndTime(normalizedShiftType) : endTime;
        validateTimeRange(resolvedStartTime, resolvedEndTime);
        return new ShiftTemplate(staffId, normalizedShiftType, resolvedStartTime, resolvedEndTime, trimToNull(remark));
    }

    private StaffShiftSchedule newEntity(ShiftDraft draft) {
        StaffShiftSchedule entity = new StaffShiftSchedule();
        applyDraft(entity, draft);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private void applyDraft(StaffShiftSchedule entity, ShiftDraft draft) {
        entity.setStaffId(draft.staffId());
        entity.setShiftDate(draft.shiftDate());
        entity.setShiftType(draft.shiftType());
        entity.setStartTime(draft.startTime());
        entity.setEndTime(draft.endTime());
        entity.setStatus(draft.status());
        entity.setRemark(draft.remark());
    }

    private void assertNoConflict(User staff, ShiftDraft draft, Long excludeShiftId) {
        if (!"active".equals(draft.status())) {
            return;
        }
        List<StaffShiftConflictDTO> conflicts = toConflicts(
                staff,
                draft,
                staffShiftScheduleRepository.findActiveConflicts(
                        draft.staffId(), draft.shiftDate(), draft.startTime(), draft.endTime(), excludeShiftId
                )
        );
        if (!conflicts.isEmpty()) {
            throwConflict(conflicts);
        }
    }

    private void assertNoConflicts(User staff, List<ShiftDraft> drafts) {
        Map<Long, User> userMap = new HashMap<>();
        userMap.put(staff.getUserId(), staff);
        assertNoConflicts(userMap, drafts);
    }

    private void assertNoConflicts(Map<Long, User> userMap, List<ShiftDraft> drafts) {
        List<StaffShiftConflictDTO> conflicts = new ArrayList<>();
        for (ShiftDraft draft : drafts) {
            if (!"active".equals(draft.status())) {
                continue;
            }
            User staff = userMap.computeIfAbsent(draft.staffId(), this::findSchedulableStaff);
            conflicts.addAll(toConflicts(
                    staff,
                    draft,
                    staffShiftScheduleRepository.findActiveConflicts(
                            draft.staffId(), draft.shiftDate(), draft.startTime(), draft.endTime(), null
                    )
            ));
        }
        conflicts.addAll(findDraftConflicts(drafts, userMap));
        if (!conflicts.isEmpty()) {
            throwConflict(conflicts);
        }
    }

    private List<StaffShiftConflictDTO> toConflicts(User staff, ShiftDraft draft, List<StaffShiftSchedule> existing) {
        String staffName = firstNonBlank(staff.getRealName(), staff.getUsername());
        String newTimeRange = toTimeRange(draft.startTime(), draft.endTime());
        return existing.stream().map(item -> {
            StaffShiftConflictDTO dto = new StaffShiftConflictDTO();
            dto.setStaffId(draft.staffId());
            dto.setStaffName(staffName);
            dto.setShiftDate(draft.shiftDate());
            dto.setExistingShiftId(item.getShiftId());
            dto.setExistingTimeRange(toTimeRange(item.getStartTime(), item.getEndTime()));
            dto.setNewTimeRange(newTimeRange);
            dto.setMessage(staffName + "在 " + draft.shiftDate() + " 已存在 " + dto.getExistingTimeRange() + " 的排班");
            return dto;
        }).toList();
    }

    private List<StaffShiftConflictDTO> findDraftConflicts(List<ShiftDraft> drafts, Map<Long, User> userMap) {
        List<StaffShiftConflictDTO> conflicts = new ArrayList<>();
        Map<String, List<ShiftDraft>> grouped = drafts.stream()
                .filter(item -> "active".equals(item.status()))
                .collect(Collectors.groupingBy(item -> item.staffId() + "_" + item.shiftDate()));
        for (List<ShiftDraft> items : grouped.values()) {
            List<ShiftDraft> sorted = items.stream()
                    .sorted(Comparator.comparing(ShiftDraft::startTime).thenComparing(ShiftDraft::endTime))
                    .toList();
            for (int i = 0; i < sorted.size(); i++) {
                ShiftDraft current = sorted.get(i);
                for (int j = i + 1; j < sorted.size(); j++) {
                    ShiftDraft next = sorted.get(j);
                    if (current.endTime().compareTo(next.startTime()) <= 0) {
                        break;
                    }
                    User staff = userMap.computeIfAbsent(current.staffId(), this::findSchedulableStaff);
                    StaffShiftConflictDTO dto = new StaffShiftConflictDTO();
                    dto.setStaffId(current.staffId());
                    dto.setStaffName(firstNonBlank(staff.getRealName(), staff.getUsername()));
                    dto.setShiftDate(current.shiftDate());
                    dto.setExistingShiftId(null);
                    dto.setExistingTimeRange(toTimeRange(current.startTime(), current.endTime()));
                    dto.setNewTimeRange(toTimeRange(next.startTime(), next.endTime()));
                    dto.setMessage(dto.getStaffName() + "在 " + current.shiftDate() + " 的批量排班时间段存在重叠");
                    conflicts.add(dto);
                }
            }
        }
        return conflicts;
    }

    private void throwConflict(List<StaffShiftConflictDTO> conflicts) {
        throw new ConflictBusinessException(
                ErrorCode.BAD_REQUEST,
                "该护理人员在当前日期已有重叠排班，请调整时间。",
                new StaffShiftConflictResponse(conflicts)
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, String message) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
        }
    }

    private List<LocalDate> enumerateDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    private List<LocalDate> generateBatchDates(LocalDate startDate,
                                               LocalDate endDate,
                                               String repeatType,
                                               List<Integer> weekDays) {
        List<LocalDate> dates = new ArrayList<>();
        Set<Integer> allowedWeekDays = weekDays == null ? Set.of() : new HashSet<>(weekDays);
        if (!allowedWeekDays.stream().allMatch(day -> day >= 1 && day <= 7)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "weekDays 仅支持 1-7", HttpStatus.BAD_REQUEST);
        }
        if ("weekly".equals(repeatType) && allowedWeekDays.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "weekly 模式必须传 weekDays", HttpStatus.BAD_REQUEST);
        }
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            if ("daily".equals(repeatType)
                    || ("workday".equals(repeatType) && dayOfWeek <= DayOfWeek.FRIDAY.getValue())
                    || ("weekly".equals(repeatType) && allowedWeekDays.contains(dayOfWeek))) {
                dates.add(date);
            }
        }
        if (dates.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未生成任何排班日期，请检查参数", HttpStatus.BAD_REQUEST);
        }
        return dates;
    }

    private long countDistinctStaffByShiftType(List<StaffShiftSchedule> rows, String shiftType) {
        return rows.stream()
                .filter(this::isActive)
                .filter(item -> shiftType.equals(item.getShiftType()))
                .map(StaffShiftSchedule::getStaffId)
                .distinct()
                .count();
    }

    private long countConflictShifts(List<StaffShiftSchedule> rows) {
        Set<Long> conflictIds = new HashSet<>();
        Map<Long, List<StaffShiftSchedule>> byStaff = rows.stream()
                .filter(this::isActive)
                .collect(Collectors.groupingBy(StaffShiftSchedule::getStaffId));
        for (List<StaffShiftSchedule> shifts : byStaff.values()) {
            List<StaffShiftSchedule> sorted = shifts.stream()
                    .sorted(Comparator.comparing(StaffShiftSchedule::getStartTime).thenComparing(StaffShiftSchedule::getEndTime))
                    .toList();
            for (int i = 0; i < sorted.size(); i++) {
                StaffShiftSchedule current = sorted.get(i);
                for (int j = i + 1; j < sorted.size(); j++) {
                    StaffShiftSchedule next = sorted.get(j);
                    if (current.getEndTime().compareTo(next.getStartTime()) <= 0) {
                        break;
                    }
                    conflictIds.add(current.getShiftId());
                    conflictIds.add(next.getShiftId());
                }
            }
        }
        return conflictIds.size();
    }

    private boolean isActive(StaffShiftSchedule item) {
        return "active".equals(item.getStatus()) && item.getShiftType() != null;
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "班次结束时间必须晚于开始时间", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeShiftType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "full_day";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_SHIFT_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "shiftType 非法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeShiftTypeFilter(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalizeShiftType(normalized);
    }

    private String normalizeRepeatType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "daily";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_REPEAT_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "repeatType 非法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeShiftStatus(String value, String fallback) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return fallback;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!("active".equals(normalized) || "cancelled".equals(normalized))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 非法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeStatusFilter(String value) {
        return trimToNull(value) == null ? null : normalizeShiftStatus(value, null);
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }

    private String toRoleName(String role) {
        return switch (normalizeRole(role)) {
            case "nurse" -> "护士";
            case "caregiver" -> "护理员";
            case "nurse_leader" -> "护士长";
            case "admin" -> "管理员";
            default -> role;
        };
    }

    private LocalTime defaultStartTime(String shiftType) {
        return switch (shiftType) {
            case "morning" -> LocalTime.of(8, 0);
            case "afternoon" -> LocalTime.of(12, 0);
            case "night" -> LocalTime.of(18, 0);
            case "full_day" -> LocalTime.of(8, 0);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "shiftType 非法", HttpStatus.BAD_REQUEST);
        };
    }

    private LocalTime defaultEndTime(String shiftType) {
        return switch (shiftType) {
            case "morning" -> LocalTime.of(12, 0);
            case "afternoon" -> LocalTime.of(18, 0);
            case "night" -> LocalTime.of(22, 0);
            case "full_day" -> LocalTime.of(18, 0);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "shiftType 非法", HttpStatus.BAD_REQUEST);
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : trimToNull(fallback);
    }

    private String toWeekDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private String toTimeRange(LocalTime startTime, LocalTime endTime) {
        return startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }

    private record ShiftDraft(Long staffId,
                              LocalDate shiftDate,
                              String shiftType,
                              LocalTime startTime,
                              LocalTime endTime,
                              String status,
                              String remark) {
    }

    private record ShiftTemplate(Long staffId,
                                 String shiftType,
                                 LocalTime startTime,
                                 LocalTime endTime,
                                 String remark) {
        private ShiftDraft toDraft(LocalDate shiftDate) {
            return new ShiftDraft(staffId, shiftDate, shiftType, startTime, endTime, "active", remark);
        }
    }

    private record QueryWindow(LocalDate date, LocalDate startDate, LocalDate endDate) {
    }
}
