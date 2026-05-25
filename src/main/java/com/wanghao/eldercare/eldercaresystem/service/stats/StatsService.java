package com.wanghao.eldercare.eldercaresystem.service.stats;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.stats.*;
import com.wanghao.eldercare.eldercaresystem.dto.stats.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

    private final JdbcTemplate jdbcTemplate;

    public StatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AlarmStatsResponse alarmStats(LocalDateTime from, LocalDateTime to) {
        String where = buildWhere("created_at", from, to);
        Object[] args = buildArgs(from, to);

        AlarmStatsResponse response = new AlarmStatsResponse();

        Long total = jdbcTemplate.queryForObject("select count(*) from alarms " + where, args, Long.class);
        response.setTotal(valueOrZero(total));

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        bySeverity.put("info", 0L);
        bySeverity.put("warning", 0L);
        bySeverity.put("critical", 0L);
        jdbcTemplate.queryForList(
                "select lower(severity) as severity, count(*) as cnt from alarms " + where + " group by lower(severity)",
                args
        ).forEach(row -> bySeverity.put(String.valueOf(row.get("severity")), ((Number) row.get("cnt")).longValue()));
        response.setBySeverity(bySeverity);

        Double avgAccept = jdbcTemplate.queryForObject(
                "select avg(timestampdiff(second, created_at, accepted_at)) from alarms "
                        + where + " and accepted_at is not null",
                args,
                Double.class
        );
        response.setAvgAcceptSeconds(round2(valueOrZero(avgAccept)));

        Double avgArrive = jdbcTemplate.queryForObject(
                "select avg(timestampdiff(second, accepted_at, arrived_at)) from alarms "
                        + where + " and accepted_at is not null and arrived_at is not null",
                args,
                Double.class
        );
        response.setAvgArriveSeconds(round2(valueOrZero(avgArrive)));

        Long overtimeAccept = jdbcTemplate.queryForObject(
                "select count(*) from alarms " + where
                        + " and accepted_at is not null and timestampdiff(second, created_at, accepted_at) > 120",
                args,
                Long.class
        );
        response.setOvertimeAcceptCount(valueOrZero(overtimeAccept));

        Long overtimeArrive = jdbcTemplate.queryForObject(
                "select count(*) from alarms " + where
                        + " and accepted_at is not null and arrived_at is not null "
                        + "and timestampdiff(second, accepted_at, arrived_at) > 600",
                args,
                Long.class
        );
        response.setOvertimeArriveCount(valueOrZero(overtimeArrive));

        return response;
    }

    public TaskStatsResponse taskStats(LocalDateTime from, LocalDateTime to) {
        String where = buildWhere("created_at", from, to);
        Object[] args = buildArgs(from, to);

        TaskStatsResponse response = new TaskStatsResponse();
        Long total = jdbcTemplate.queryForObject("select count(*) from tasks " + where, args, Long.class);
        long totalValue = valueOrZero(total);
        response.setTotal(totalValue);

        Long completed = jdbcTemplate.queryForObject(
                "select count(*) from tasks " + where + " and lower(status) = 'completed'",
                args,
                Long.class
        );
        response.setCompletedRate(rate(valueOrZero(completed), totalValue));

        Long overdue = jdbcTemplate.queryForObject(
                "select count(*) from tasks " + where
                        + " and (lower(status) = 'overdue' "
                        + "or (due_at is not null and due_at < now() and lower(status) <> 'completed'))",
                args,
                Long.class
        );
        response.setOverdueRate(rate(valueOrZero(overdue), totalValue));

        Map<String, Long> byType = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                "select coalesce(task_type, 'unknown') as task_type, count(*) as cnt "
                        + "from tasks " + where + " group by task_type",
                args
        ).forEach(row -> byType.put(String.valueOf(row.get("task_type")), ((Number) row.get("cnt")).longValue()));
        response.setByType(byType);
        return response;
    }

    public MedicationStatsResponse medicationStats(LocalDateTime from, LocalDateTime to) {
        String where = buildWhere("administered_time", from, to);
        Object[] args = buildArgs(from, to);

        MedicationStatsResponse response = new MedicationStatsResponse();
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from medication_admin_records " + where,
                args,
                Long.class
        );
        long totalValue = valueOrZero(total);
        response.setTotalRecords(totalValue);

        Long missed = jdbcTemplate.queryForObject(
                "select count(*) from medication_admin_records " + where + " and lower(status) = 'missed'",
                args,
                Long.class
        );
        response.setMissedRate(rate(valueOrZero(missed), totalValue));

        Long refused = jdbcTemplate.queryForObject(
                "select count(*) from medication_admin_records " + where + " and lower(status) = 'refused'",
                args,
                Long.class
        );
        response.setRefusedRate(rate(valueOrZero(refused), totalValue));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                "select lower(status) as status, count(*) as cnt from medication_admin_records "
                        + where + " group by lower(status)",
                args
        ).forEach(row -> byStatus.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue()));
        response.setByStatus(byStatus);
        return response;
    }

    public OccupancyStatsResponse occupancyStats() {
        OccupancyStatsResponse response = new OccupancyStatsResponse();
        Long totalBeds = jdbcTemplate.queryForObject("select count(*) from beds", Long.class);
        Long occupiedBeds = jdbcTemplate.queryForObject(
                "select count(*) from beds b where lower(b.status) = 'occupied' "
                        + "or exists (select 1 from admission_records a where a.bed_id = b.bed_id and lower(a.status) = 'active')",
                Long.class
        );
        long total = valueOrZero(totalBeds);
        long occupied = valueOrZero(occupiedBeds);
        response.setTotalBeds(total);
        response.setOccupiedBeds(occupied);
        response.setOccupancyRate(rate(occupied, total));
        return response;
    }

    public PersonnelStatsResponse personnelStats() {
        PersonnelStatsResponse response = new PersonnelStatsResponse();

        Map<String, Long> staffByRole = new LinkedHashMap<>();
        staffByRole.put("admin", countUsersByRole("admin"));
        staffByRole.put("nurse_leader", countUsersByRole("nurse_leader"));
        staffByRole.put("nurse", countUsersByRole("nurse"));
        staffByRole.put("caregiver", countUsersByRole("caregiver"));
        staffByRole.put("doctor", countUsersByRole("doctor"));

        long totalStaff = 0L;
        for (Long count : staffByRole.values()) {
            totalStaff += count;
        }

        response.setStaffByRole(staffByRole);
        response.setTotalStaff(totalStaff);
        response.setTotalElders(countUsersByRole("elder"));
        response.setTotalFamilies(countUsersByRole("family"));
        return response;
    }

    private String buildWhere(String timeColumn, LocalDateTime from, LocalDateTime to) {
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (from != null) {
            where.append(" and ").append(timeColumn).append(" >= ? ");
        }
        if (to != null) {
            where.append(" and ").append(timeColumn).append(" <= ? ");
        }
        return where.toString();
    }

    private Object[] buildArgs(LocalDateTime from, LocalDateTime to) {
        List<Object> args = new ArrayList<>();
        if (from != null) {
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            args.add(Timestamp.valueOf(to));
        }
        return args.toArray();
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private double valueOrZero(Double value) {
        return value == null ? 0D : value;
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return round4((double) numerator / (double) denominator);
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private long countUsersByRole(String role) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from users where deleted_at is null and lower(role) = ?",
                Long.class,
                role
        );
        return valueOrZero(count);
    }
}
