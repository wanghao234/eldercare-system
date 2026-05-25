package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.dto.file.FileUploadResponse;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.ImportAdmissionContractResponse;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.service.file.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StreamUtils;

@Service
public class AdmissionContractImportService {

    private static final String DOCUMENT_XML_ENTRY = "word/document.xml";
    private static final Set<String> OPEN_ADMISSION_STATUSES = Set.of("pending", "active");
    private static final Pattern CONTRACT_NO_PATTERN =
            Pattern.compile("合同编号\\s*[:：]\\s*([^\\s\\n\\r]+)");
    private static final Pattern ID_NUMBER_PATTERN =
            Pattern.compile("\\b\\d{17}[\\dXx]\\b|\\b\\d{15}\\b");
    private static final Pattern DEPOSIT_PATTERN =
            Pattern.compile("押金\\s*[￥¥]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*元");

    private final FileStorageService fileStorageService;
    private final ElderProfileRepository elderProfileRepository;
    private final AdmissionRecordRepository admissionRecordRepository;

    public AdmissionContractImportService(FileStorageService fileStorageService,
                                          ElderProfileRepository elderProfileRepository,
                                          AdmissionRecordRepository admissionRecordRepository) {
        this.fileStorageService = fileStorageService;
        this.elderProfileRepository = elderProfileRepository;
        this.admissionRecordRepository = admissionRecordRepository;
    }

    public ImportAdmissionContractResponse importContract(MultipartFile file) {
        validateDocx(file);

        ContractImportData importData = parse(file);
        ElderProfileEntity profile = matchElderProfile(importData.idNumbers());
        AdmissionRecord admission = admissionRecordRepository
                .findFirstByElderIdAndStatusInOrderByUpdatedAtDescAdmissionIdDesc(profile.getElderId(), OPEN_ADMISSION_STATUSES)
                .orElseThrow(() -> badRequest("未找到该老人可更新的入住记录"));
        return applyImport(admission, profile, importData, file);
    }

    public ImportAdmissionContractResponse importContract(MultipartFile file, AdmissionRecord admission) {
        validateDocx(file);
        if (admission == null) {
            throw badRequest("入住记录不存在");
        }
        ContractImportData importData = parse(file);
        ElderProfileEntity profile = elderProfileRepository.findById(admission.getElderId()).orElseGet(() -> {
            ElderProfileEntity created = new ElderProfileEntity();
            created.setElderId(admission.getElderId());
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            return created;
        });
        applyImportedIdNumber(profile, importData.idNumbers());
        return applyImport(admission, profile, importData, file);
    }

    public byte[] loadContractFileBytes(AdmissionRecord admission) {
        if (admission == null) {
            throw badRequest("入住记录不存在");
        }
        String contractFileUrl = admission.getContractFileUrl();
        if (!StringUtils.hasText(contractFileUrl) || !contractFileUrl.startsWith("/uploads/")) {
            throw badRequest("当前入住记录未上传合同文件");
        }
        String fileName = contractFileUrl.substring(contractFileUrl.lastIndexOf('/') + 1);
        if (!StringUtils.hasText(fileName)) {
            throw badRequest("合同文件地址不合法");
        }
        Path path = fileStorageService.getStorageAbsolutePath().resolve(fileName).normalize();
        try {
            if (!path.startsWith(fileStorageService.getStorageAbsolutePath()) || !Files.exists(path)) {
                throw badRequest("合同文件不存在");
            }
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取合同文件失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ImportAdmissionContractResponse applyImport(AdmissionRecord admission,
                                                        ElderProfileEntity profile,
                                                        ContractImportData importData,
                                                        MultipartFile file) {
        FileUploadResponse stored = fileStorageService.store(file);
        LocalDateTime now = LocalDateTime.now();
        profile.setUpdatedAt(now);
        elderProfileRepository.save(profile);
        admission.setContractNo(importData.contractNo());
        admission.setDepositAmount(importData.depositAmount());
        admission.setContractFileUrl(stored.getUrl());
        admission.setUpdatedAt(now);
        admissionRecordRepository.save(admission);

        ImportAdmissionContractResponse response = new ImportAdmissionContractResponse();
        response.setAdmissionId(admission.getAdmissionId());
        response.setElderId(admission.getElderId());
        response.setElderIdNumber(profile.getIdNumber());
        response.setContractNo(importData.contractNo());
        response.setDepositAmount(importData.depositAmount());
        response.setContractFileUrl(stored.getUrl());
        response.setContractFileName(stored.getFileName());
        return response;
    }

    private void validateDocx(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("合同文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw badRequest("仅支持导入 docx 合同文件");
        }
    }

    private ContractImportData parse(MultipartFile file) {
        String text = extractDocumentText(file);
        String contractNo = matchRequired(CONTRACT_NO_PATTERN, text, "未识别到合同编号");
        BigDecimal depositAmount = parseRequiredDecimal(DEPOSIT_PATTERN, text, "未识别到押金");
        List<String> idNumbers = collectIdNumbers(text);
        if (idNumbers.isEmpty()) {
            throw badRequest("未识别到身份证号");
        }
        return new ContractImportData(contractNo, depositAmount, idNumbers);
    }

    private String extractDocumentText(MultipartFile file) {
        try (InputStream input = file.getInputStream();
             ZipInputStream zin = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (DOCUMENT_XML_ENTRY.equals(entry.getName())) {
                    String xml = StreamUtils.copyToString(zin, StandardCharsets.UTF_8);
                    return xml.replaceAll("<[^>]+>", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .replaceAll("\\s+", " ")
                            .trim();
                }
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "合同文件解析失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        throw badRequest("合同文件内容不完整");
    }

    private ElderProfileEntity matchElderProfile(List<String> idNumbers) {
        List<ElderProfileEntity> matchedProfiles = new ArrayList<>();
        Set<Long> seenElderIds = new LinkedHashSet<>();
        for (String idNumber : idNumbers) {
            Optional<ElderProfileEntity> profile = elderProfileRepository.findFirstByIdNumber(idNumber);
            if (profile.isPresent() && seenElderIds.add(profile.get().getElderId())) {
                matchedProfiles.add(profile.get());
            }
        }
        if (matchedProfiles.isEmpty()) {
            throw badRequest("未找到身份证号对应的老人档案");
        }
        if (matchedProfiles.size() > 1) {
            throw badRequest("合同中识别到多个老人身份证号，无法确定目标入住记录");
        }
        return matchedProfiles.get(0);
    }

    private void applyImportedIdNumber(ElderProfileEntity profile, List<String> idNumbers) {
        if (idNumbers == null || idNumbers.isEmpty()) {
            throw badRequest("未识别到身份证号");
        }
        String importedIdNumber = idNumbers.get(0);
        String existingIdNumber = profile.getIdNumber();
        if (existingIdNumber != null && !existingIdNumber.isBlank()
                && !existingIdNumber.trim().equalsIgnoreCase(importedIdNumber)) {
            throw badRequest("合同身份证号与当前老人档案不一致");
        }
        profile.setIdNumber(importedIdNumber);
    }

    private List<String> collectIdNumbers(String text) {
        LinkedHashSet<String> idNumbers = new LinkedHashSet<>();
        Matcher matcher = ID_NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            idNumbers.add(matcher.group().toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(idNumbers);
    }

    private String matchRequired(Pattern pattern, String text, String errorMessage) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw badRequest(errorMessage);
        }
        return matcher.group(1).trim();
    }

    private BigDecimal parseRequiredDecimal(Pattern pattern, String text, String errorMessage) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw badRequest(errorMessage);
        }
        return new BigDecimal(matcher.group(1));
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private record ContractImportData(String contractNo, BigDecimal depositAmount, List<String> idNumbers) {
    }
}
