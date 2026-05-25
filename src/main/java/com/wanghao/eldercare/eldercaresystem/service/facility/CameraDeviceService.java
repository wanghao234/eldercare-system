package com.wanghao.eldercare.eldercaresystem.service.facility;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.dto.facility.CameraDeviceUpsertRequest;
import com.wanghao.eldercare.eldercaresystem.dto.facility.FacilityPageResponse;
import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.CameraDeviceRepository;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CameraDeviceService {

    private final CameraDeviceRepository cameraDeviceRepository;

    public CameraDeviceService(CameraDeviceRepository cameraDeviceRepository) {
        this.cameraDeviceRepository = cameraDeviceRepository;
    }

    @Transactional(readOnly = true)
    public FacilityPageResponse<CameraDevice> list(String keyword, String status, int page, int size) {
        Specification<CameraDevice> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String likeValue = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("cameraName")), likeValue),
                    cb.like(cb.lower(root.get("cameraCode")), likeValue),
                    cb.like(cb.lower(root.get("locationText")), likeValue)
            ));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim()));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "cameraId"));
        Page<CameraDevice> result = cameraDeviceRepository.findAll(spec, pageable);

        FacilityPageResponse<CameraDevice> response = new FacilityPageResponse<>();
        response.setItems(result.getContent());
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public CameraDevice getById(Long cameraId) {
        return getOrThrow(cameraId);
    }

    @Transactional
    public CameraDevice create(CameraDeviceUpsertRequest request) {
        CameraDevice entity = new CameraDevice();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        apply(entity, request, true);
        return save(entity);
    }

    @Transactional
    public CameraDevice update(Long cameraId, CameraDeviceUpsertRequest request) {
        CameraDevice entity = getOrThrow(cameraId);
        entity.setUpdatedAt(LocalDateTime.now());
        apply(entity, request, false);
        return save(entity);
    }

    @Transactional
    public void delete(Long cameraId) {
        CameraDevice entity = getOrThrow(cameraId);
        cameraDeviceRepository.delete(entity);
    }

    private CameraDevice getOrThrow(Long cameraId) {
        return cameraDeviceRepository.findById(cameraId)
                .orElseThrow(() -> new NotFoundException("摄像头不存在"));
    }

    private void apply(CameraDevice entity, CameraDeviceUpsertRequest request, boolean creating) {
        entity.setCameraName(request.getCameraName().trim());
        entity.setCameraCode(trimToNull(request.getCameraCode()));
        entity.setCameraType(defaultIfBlank(request.getCameraType(), "webcam"));
        entity.setStreamUrl(trimToNull(request.getStreamUrl()));
        entity.setElderId(request.getElderId());
        entity.setRoomId(request.getRoomId());
        entity.setBedId(request.getBedId());
        entity.setLocationText(trimToNull(request.getLocationText()));
        entity.setMapX(request.getMapX());
        entity.setMapY(request.getMapY());
        entity.setStatus(defaultIfBlank(request.getStatus(), creating ? "online" : entity.getStatus() == null ? "online" : entity.getStatus()));
        entity.setRemark(trimToNull(request.getRemark()));
    }

    private CameraDevice save(CameraDevice entity) {
        try {
            return cameraDeviceRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cameraCode 已存在或数据约束不合法", HttpStatus.BAD_REQUEST);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
