package com.wanghao.eldercare.eldercaresystem.security;

import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    private final CareTeamAssignmentRepository careTeamAssignmentRepository;

    public PermissionService(CareTeamAssignmentRepository careTeamAssignmentRepository) {
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
    }

    // 返回 null 表示可见全量老人；返回非 null 列表表示可见 elder_id 列表。
    public List<Long> getVisibleElderIds(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return null;
        }

        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            return careTeamAssignmentRepository.findActiveElderIdsByNurseId(currentUser.getUserId());
        }

        if (currentUser.hasRole("family")) {
            return careTeamAssignmentRepository.findActiveElderIdsByFamilyId(currentUser.getUserId());
        }

        if (currentUser.hasRole("elder")) {
            return List.of(currentUser.getUserId());
        }

        return List.of();
    }

    public void assertCanAccessElder(CurrentUser currentUser, Long elderId) {
        List<Long> visibleElderIds = getVisibleElderIds(currentUser);
        if (visibleElderIds == null || visibleElderIds.contains(elderId)) {
            return;
        }
        throw new AccessDeniedException("无权限访问该老人数据");
    }
}
