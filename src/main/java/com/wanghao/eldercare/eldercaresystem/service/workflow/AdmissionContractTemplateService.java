package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.admission.Bed;
import com.wanghao.eldercare.eldercaresystem.entity.admission.Room;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.BedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.RoomRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class AdmissionContractTemplateService {

    private static final String TEMPLATE_PATH = "contract-templates/颐养云端养老院护理服务合同.docx";
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String DOCUMENT_XML_ENTRY = "word/document.xml";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日");
    private static final String INSTITUTION_CREDIT_CODE = "91310101MA1ELDER01";
    private static final String INSTITUTION_PHONE = "400-820-5678";
    private static final String INSTITUTION_ADDRESS = "上海市黄浦区颐养云端养老院 1 号楼";

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final CareTeamAssignmentRepository careTeamAssignmentRepository;
    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;

    public AdmissionContractTemplateService(UserRepository userRepository,
                                            ElderProfileRepository elderProfileRepository,
                                            CareTeamAssignmentRepository careTeamAssignmentRepository,
                                            BedRepository bedRepository,
                                            RoomRepository roomRepository) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.careTeamAssignmentRepository = careTeamAssignmentRepository;
        this.bedRepository = bedRepository;
        this.roomRepository = roomRepository;
    }

    public byte[] generate(AdmissionRecord admission, JsonNode formData, String operatorName, String operatorPhone) {
        ContractContext context = buildContext(admission, formData, operatorName, operatorPhone);
        try {
            return renderTemplate(context);
        } catch (Exception ex) {
            throw new IllegalStateException("合同模板生成失败", ex);
        }
    }

    public String buildDownloadFileName(AdmissionRecord admission) {
        User elderUser = userRepository.findByUserIdAndDeletedAtIsNull(admission.getElderId()).orElse(null);
        String elderName = elderUser == null ? null : elderUser.getRealName();
        String safeName = preferredText(elderName, "老人");
        return safeName + "颐养云端养老院护理服务合同.docx";
    }

    private ContractContext buildContext(AdmissionRecord admission, JsonNode formData, String operatorName, String operatorPhone) {
        User elderUser = userRepository.findByUserIdAndDeletedAtIsNull(admission.getElderId()).orElse(null);
        ElderProfileEntity profile = elderProfileRepository.findById(admission.getElderId()).orElse(null);
        Bed bed = bedRepository.findById(admission.getBedId()).orElse(null);
        Room room = bed == null ? null : roomRepository.findById(bed.getRoomId()).orElse(null);
        User familyUser = loadFamilyUser(admission.getElderId());

        LocalDate signDate = readDate(formData, "signDate").orElse(LocalDate.now());
        LocalDate startDate = readDate(formData, "serviceStartDate").orElse(admission.getStartDate());
        LocalDate endDate = readDate(formData, "serviceEndDate").orElse(null);

        String contractNo = preferredText(
                readText(formData, "contractNo"),
                admission.getContractNo(),
                "YYYD-HL-" + signDate.getYear() + "-" + String.format(Locale.ROOT, "%04d", admission.getAdmissionId())
        );
        String signLocation = preferredText(readText(formData, "signLocation"), "颐养云端养老院");
        String packageName = preferredText(readText(formData, "packageName"), admission.getPackageName(), "");
        BigDecimal depositAmount = readDecimal(formData, "depositAmount").orElse(admission.getDepositAmount());
        BigDecimal monthlyFee = readDecimal(formData, "monthlyFee").orElse(null);
        BigDecimal bedFee = readDecimal(formData, "bedFee").orElse(null);
        BigDecimal careFee = readDecimal(formData, "careFee").orElse(null);
        BigDecimal mealFee = readDecimal(formData, "mealFee").orElse(null);
        BigDecimal specialServiceFee = readDecimal(formData, "specialServiceFee").orElse(null);
        BigDecimal otherFee = readDecimal(formData, "otherFee").orElse(null);

        String partyBName = preferredText(readText(formData, "partyBName"), familyUser == null ? null : familyUser.getRealName(), "");
        String partyBPhone = preferredText(readText(formData, "partyBPhone"), familyUser == null ? null : familyUser.getPhone(), "");
        String partyBIdNumber = preferredText(readText(formData, "partyBIdNumber"), "");
        String partyBRelation = preferredText(readText(formData, "partyBRelation"), partyBName.isBlank() ? "" : "家属");
        String emergencyName = preferredText(
                readText(formData, "emergencyContactName"),
                profile == null ? null : profile.getEmergencyContactName(),
                partyBName
        );
        String emergencyPhone = preferredText(
                readText(formData, "emergencyContactPhone"),
                profile == null ? null : profile.getEmergencyContactPhone(),
                partyBPhone
        );
        String elderName = preferredText(elderUser == null ? null : elderUser.getRealName(), "");
        String elderPhone = preferredText(elderUser == null ? null : elderUser.getPhone(), "");
        String elderGender = normalizeGender(preferredText(readText(formData, "gender"), profile == null ? null : profile.getGender(), ""));
        String elderAge = profile != null && profile.getBirthday() != null
                ? String.valueOf(Math.max(0, Period.between(profile.getBirthday(), signDate).getYears()))
                : "";
        String elderIdNumber = preferredText(readText(formData, "elderIdNumber"), profile == null ? null : profile.getIdNumber(), "");
        String elderAddress = preferredText(readText(formData, "elderAddress"), profile == null ? null : profile.getAddress(), "");
        String chronicConditions = preferredText(readText(formData, "chronicConditions"), profile == null ? null : profile.getChronicConditions(), "");
        String allergies = preferredText(readText(formData, "allergies"), profile == null ? null : profile.getAllergies(), "");
        String medicationInfo = preferredText(readText(formData, "medicationInfo"), "");
        String dietTaboo = preferredText(readText(formData, "dietTaboo"), profile == null ? null : profile.getDietTaboo(), "");
        String specialCareNeeds = preferredText(readText(formData, "specialCareNeeds"), profile == null ? null : profile.getNotes(), "");
        String careLevel = preferredText(readText(formData, "careLevel"), profile == null ? null : profile.getCareLevel(), "");
        String bodyStatus = preferredText(readText(formData, "bodyStatus"), inferBodyStatus(careLevel), "");
        String roomType = preferredText(readText(formData, "roomType"), "");
        String roomNumber = room == null ? "" : preferredText(room.getRoomNo(), String.valueOf(room.getRoomId()));
        String bedCode = bed == null ? "" : preferredText(bed.getBedNo(), String.valueOf(bed.getBedId()));
        String handlerName = preferredText(readText(formData, "handlerName"), operatorName, "");
        String handlerPhone = preferredText(readText(formData, "handlerPhone"), operatorPhone, "");
        String managerName = preferredText(readText(formData, "managerName"), "");
        String institutionPhone = INSTITUTION_PHONE;
        String institutionAddress = INSTITUTION_ADDRESS;
        String institutionCreditCode = INSTITUTION_CREDIT_CODE;
        String remark = preferredText(readText(formData, "remark"), packageName, "");
        String trialDays = preferredText(readText(formData, "trialDays"), "");

        return new ContractContext(
                contractNo,
                signLocation,
                signDate,
                startDate,
                endDate,
                trialDays,
                roomType,
                roomNumber,
                bedCode,
                careLevel,
                monthlyFee,
                bedFee,
                careFee,
                mealFee,
                depositAmount,
                specialServiceFee,
                otherFee,
                packageName,
                partyBName,
                partyBPhone,
                partyBIdNumber,
                partyBRelation,
                emergencyName,
                emergencyPhone,
                elderName,
                elderPhone,
                elderGender,
                elderAge,
                elderIdNumber,
                elderAddress,
                bodyStatus,
                chronicConditions,
                allergies,
                medicationInfo,
                dietTaboo,
                specialCareNeeds,
                handlerName,
                handlerPhone,
                managerName,
                institutionPhone,
                institutionAddress,
                institutionCreditCode,
                remark
        );
    }

    private User loadFamilyUser(Long elderId) {
        List<CareTeamAssignment> assignments =
                careTeamAssignmentRepository.findAllByElderIdAndIsActiveAndFamilyIdIsNotNullOrderByAssignmentIdAsc(elderId, 1);
        if (assignments.isEmpty()) {
            return null;
        }
        Long familyId = assignments.get(0).getFamilyId();
        if (familyId == null) {
            return null;
        }
        return userRepository.findByUserIdAndDeletedAtIsNull(familyId).orElse(null);
    }

    private byte[] renderTemplate(ContractContext context) throws Exception {
        byte[] templateBytes = loadTemplateBytes();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(templateBytes));
             ByteArrayOutputStream bout = new ByteArrayOutputStream();
             ZipOutputStream zout = new ZipOutputStream(bout)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                zout.putNextEntry(new ZipEntry(entry.getName()));
                if (DOCUMENT_XML_ENTRY.equals(entry.getName())) {
                    String xml = StreamUtils.copyToString(zin, StandardCharsets.UTF_8);
                    String rendered = renderDocumentXml(xml, context);
                    zout.write(rendered.getBytes(StandardCharsets.UTF_8));
                } else {
                    StreamUtils.copy(zin, zout);
                }
                zout.closeEntry();
                zin.closeEntry();
            }
            zout.finish();
            return bout.toByteArray();
        }
    }

    private byte[] loadTemplateBytes() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream input = resource.getInputStream()) {
            return StreamUtils.copyToByteArray(input);
        }
    }

    private String renderDocumentXml(String xml, ContractContext context) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Element body = firstElement(document.getDocumentElement(), "body");
        int tableIndex = 0;
        for (Element child : childElements(body, null)) {
            if ("p".equals(child.getLocalName())) {
                processParagraph(child, context);
            } else if ("tbl".equals(child.getLocalName())) {
                tableIndex++;
                processTable(child, tableIndex, context);
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toString(StandardCharsets.UTF_8);
    }

    private void processParagraph(Element paragraph, ContractContext context) {
        String text = paragraphText(paragraph);
        if ("合同编号：YYYD-HL-2026-________".equals(text)) {
            setParagraphText(paragraph, "合同编号：" + context.contractNo());
        } else if ("签署地点：________________________".equals(text)) {
            setParagraphText(paragraph, "签署地点：" + blankIfEmpty(context.signLocation()));
        } else if (text.startsWith("1. 本合同服务期限为：自 ")) {
            setParagraphText(paragraph, "1. 本合同服务期限为：自 "
                    + formatDate(context.startDate()) + "起至 " + formatDate(context.endDate()) + "止。");
        } else if (text.startsWith("3. 试住期为 ")) {
            setParagraphText(paragraph, "3. 试住期为 " + blankIfEmpty(context.trialDays())
                    + " 天，自入住之日起计算。试住期间，如入住老人明显不适合机构集中照护，双方可协商调整护理方案或解除合同。");
        } else if (text.startsWith("入住房间类型：")) {
            setParagraphText(paragraph, buildRoomTypeLine(context.roomType()));
        } else if (text.startsWith("1. 房间号：")) {
            setParagraphText(paragraph, "1. 房间号：" + blankIfEmpty(context.roomNumber())
                    + "；床位号：" + blankIfEmpty(context.bedCode()) + "。");
        } else if ("经甲方初步评估，入住老人护理等级暂定为以下三类之一：".equals(text)) {
            setParagraphText(paragraph, "经甲方初步评估，入住老人护理等级暂定为：" + blankIfEmpty(context.careLevel()) + "。");
        } else if (text.startsWith("1. 合计每月费用为：")) {
            setParagraphText(paragraph, "1. 合计每月费用为：￥" + formatMoney(context.monthlyFee())
                    + " 元，大写：人民币" + blankIfEmpty(uppercaseMoneyText(context.monthlyFee())) + "整。");
        }
    }

    private void processTable(Element table, int tableIndex, ContractContext context) {
        List<Element> rows = childElements(table, "tr");
        if (tableIndex == 1) {
            setCellText(rows, 1, 1, "颐养云端养老院");
            setCellText(rows, 1, 3, blankIfEmpty(context.institutionPhone()));
            setCellText(rows, 2, 1, blankIfEmpty(context.institutionCreditCode()));
            setCellText(rows, 2, 3, blankIfEmpty(context.institutionAddress()));
            setCellText(rows, 3, 1, blankIfEmpty(context.partyBName()));
            setCellText(rows, 3, 3, blankIfEmpty(context.partyBIdNumber()));
            setCellText(rows, 4, 1, blankIfEmpty(context.partyBPhone()));
            setCellText(rows, 4, 3, blankIfEmpty(context.partyBRelation()));
            setCellText(rows, 5, 1, blankIfEmpty(context.emergencyName()));
            setCellText(rows, 5, 3, blankIfEmpty(context.emergencyPhone()));
        } else if (tableIndex == 2) {
            setCellText(rows, 0, 1, blankIfEmpty(context.elderName()));
            setCellText(rows, 0, 3, blankIfEmpty(context.elderGender()));
            setCellText(rows, 1, 1, blankIfEmpty(context.elderAge()));
            setCellText(rows, 1, 3, blankIfEmpty(context.elderIdNumber()));
            setCellText(rows, 2, 1, blankIfEmpty(context.elderPhone()));
            setCellText(rows, 2, 3, blankIfEmpty(context.elderAddress()));
            setCellText(rows, 3, 1, buildBodyStatusLine(context.bodyStatus()));
            setCellText(rows, 3, 3, blankIfEmpty(context.chronicConditions()));
            setCellText(rows, 4, 1, blankIfEmpty(context.allergies()));
            setCellText(rows, 4, 3, blankIfEmpty(context.medicationInfo()));
            setCellText(rows, 5, 1, blankIfEmpty(context.dietTaboo()));
            setCellText(rows, 5, 3, blankIfEmpty(context.specialCareNeeds()));
        } else if (tableIndex == 4) {
            setCellText(rows, 1, 1, "￥" + formatMoney(context.bedFee()) + " 元/月");
            setCellText(rows, 2, 1, "￥" + formatMoney(context.careFee()) + " 元/月");
            setCellText(rows, 3, 1, "￥" + formatMoney(context.mealFee()) + " 元/月");
            setCellText(rows, 4, 1, "￥" + formatMoney(context.depositAmount()) + " 元");
            setCellText(rows, 5, 1, "￥" + formatMoney(context.specialServiceFee()) + " 元/月");
            setCellText(rows, 6, 1, "￥" + formatMoney(context.otherFee()) + " 元");
        } else if (tableIndex == 5) {
            String signDate = formatDate(context.signDate());
            setCellText(rows, 0, 1, "乙方：" + blankIfEmpty(context.partyBName()));
            setCellText(rows, 2, 0, "负责人签字：" + blankIfEmpty(context.managerName()));
            setCellText(rows, 2, 1, "身份证号：" + blankIfEmpty(context.partyBIdNumber()));
            setCellText(rows, 3, 0, "日期：" + signDate);
            setCellText(rows, 3, 1, "日期：" + signDate);
            setCellText(rows, 4, 0, "入住老人签字或捺印：" + blankIfEmpty(context.elderName()));
            setCellText(rows, 4, 1, "紧急联系人签字：" + blankIfEmpty(context.emergencyName()));
            setCellText(rows, 5, 0, "日期：" + signDate);
            setCellText(rows, 5, 1, "日期：" + signDate);
            setCellText(rows, 6, 0, "经办人：" + blankIfEmpty(context.handlerName()));
            setCellText(rows, 6, 1, "备注：" + blankIfEmpty(context.remark()));
            setCellText(rows, 7, 0, "联系电话：" + blankIfEmpty(context.handlerPhone()));
            setCellText(rows, 7, 1, "联系电话：" + blankIfEmpty(context.partyBPhone()));
        }
    }

    private void setCellText(List<Element> rows, int rowIndex, int cellIndex, String text) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return;
        }
        List<Element> cells = childElements(rows.get(rowIndex), "tc");
        if (cellIndex < 0 || cellIndex >= cells.size()) {
            return;
        }
        Element paragraph = firstElement(cells.get(cellIndex), "p");
        if (paragraph != null) {
            setParagraphText(paragraph, text);
        }
    }

    private Element firstElement(Element parent, String localName) {
        for (Element child : childElements(parent, localName)) {
            return child;
        }
        return null;
    }

    private List<Element> childElements(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) {
                if (localName == null || localName.equals(element.getLocalName())) {
                    elements.add(element);
                }
            }
        }
        return elements;
    }

    private String paragraphText(Element paragraph) {
        StringBuilder sb = new StringBuilder();
        NodeList texts = paragraph.getElementsByTagNameNS(WORD_NS, "t");
        for (int i = 0; i < texts.getLength(); i++) {
            sb.append(texts.item(i).getTextContent());
        }
        return sb.toString();
    }

    private void setParagraphText(Element paragraph, String text) {
        Document document = paragraph.getOwnerDocument();
        Element pPr = firstElement(paragraph, "pPr");
        Element firstRun = firstElement(paragraph, "r");
        Element replacementRun = document.createElementNS(WORD_NS, "w:r");
        if (firstRun != null) {
            Element rPr = firstElement(firstRun, "rPr");
            if (rPr != null) {
                replacementRun.appendChild(rPr.cloneNode(true));
            }
        }
        Element t = document.createElementNS(WORD_NS, "w:t");
        if (text.startsWith(" ") || text.endsWith(" ")) {
            t.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve");
        }
        t.setTextContent(text);
        replacementRun.appendChild(t);

        List<Node> toRemove = new ArrayList<>();
        NodeList children = paragraph.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child != pPr) {
                toRemove.add(child);
            }
        }
        for (Node child : toRemove) {
            paragraph.removeChild(child);
        }
        paragraph.appendChild(replacementRun);
    }

    private String readText(JsonNode formData, String field) {
        if (formData == null) {
            return null;
        }
        JsonNode node = formData.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Optional<BigDecimal> readDecimal(JsonNode formData, String field) {
        String value = readText(formData, field);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> readDate(JsonNode formData, String field) {
        String value = readText(formData, field);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String preferredText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeGender(String gender) {
        String value = preferredText(gender).toLowerCase(Locale.ROOT);
        if ("male".equals(value) || "m".equals(value) || "男".equals(value)) {
            return "男";
        }
        if ("female".equals(value) || "f".equals(value) || "女".equals(value)) {
            return "女";
        }
        return preferredText(gender);
    }

    private String inferBodyStatus(String careLevel) {
        String normalized = preferredText(careLevel).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "L1", "一级护理" -> "自理";
            case "L2", "二级护理" -> "半自理";
            case "L3", "三级护理" -> "失能";
            default -> "";
        };
    }

    private String buildRoomTypeLine(String roomType) {
        String normalized = preferredText(roomType);
        return "入住房间类型：  "
                + mark("单人间", normalized) + " 单人间  "
                + mark("双人间", normalized) + " 双人间  "
                + mark("多人间", normalized) + " 多人间  "
                + mark("护理专区", normalized) + " 护理专区  "
                + markOther(normalized, List.of("单人间", "双人间", "多人间", "护理专区")) + " 其他："
                + otherValue(normalized, List.of("单人间", "双人间", "多人间", "护理专区"));
    }

    private String buildBodyStatusLine(String bodyStatus) {
        String normalized = preferredText(bodyStatus);
        return mark("自理", normalized) + " 自理  "
                + mark("半自理", normalized) + " 半自理  "
                + mark("失能", normalized) + " 失能  "
                + mark("失智", normalized) + " 失智  "
                + markOther(normalized, List.of("自理", "半自理", "失能", "失智")) + " 其他："
                + otherValue(normalized, List.of("自理", "半自理", "失能", "失智"));
    }

    private String mark(String label, String actual) {
        return label.equalsIgnoreCase(preferredText(actual)) ? "☑" : "□";
    }

    private String markOther(String actual, List<String> presets) {
        return otherValue(actual, presets).isBlank() ? "□" : "☑";
    }

    private String otherValue(String actual, List<String> presets) {
        String normalized = preferredText(actual);
        for (String preset : presets) {
            if (preset.equalsIgnoreCase(normalized)) {
                return "";
            }
        }
        return normalized;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "______ 年 ____ 月 ____ 日" : date.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "________";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private String uppercaseMoneyText(BigDecimal amount) {
        if (amount == null) {
            return "________________";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private String blankIfEmpty(String value) {
        return preferredText(value).isBlank() ? "________________" : preferredText(value);
    }

    private record ContractContext(
            String contractNo,
            String signLocation,
            LocalDate signDate,
            LocalDate startDate,
            LocalDate endDate,
            String trialDays,
            String roomType,
            String roomNumber,
            String bedCode,
            String careLevel,
            BigDecimal monthlyFee,
            BigDecimal bedFee,
            BigDecimal careFee,
            BigDecimal mealFee,
            BigDecimal depositAmount,
            BigDecimal specialServiceFee,
            BigDecimal otherFee,
            String packageName,
            String partyBName,
            String partyBPhone,
            String partyBIdNumber,
            String partyBRelation,
            String emergencyName,
            String emergencyPhone,
            String elderName,
            String elderPhone,
            String elderGender,
            String elderAge,
            String elderIdNumber,
            String elderAddress,
            String bodyStatus,
            String chronicConditions,
            String allergies,
            String medicationInfo,
            String dietTaboo,
            String specialCareNeeds,
            String handlerName,
            String handlerPhone,
            String managerName,
            String institutionPhone,
            String institutionAddress,
            String institutionCreditCode,
            String remark) {
    }
}
