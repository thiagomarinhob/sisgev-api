package com.jettch.sisgev.reports.service;

import com.jettch.sisgev.dashboard.repository.DashboardRepository;
import com.jettch.sisgev.municipalities.entity.Municipality;
import com.jettch.sisgev.municipalities.repository.MunicipalityRepository;
import com.jettch.sisgev.reports.dto.ReportData;
import com.jettch.sisgev.reports.repository.ReportRepository;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** BE-24 — Geração do relatório gerencial em CSV (Commons CSV), XLSX (POI) e PDF (JasperReports 6). RN-022. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2999, 12, 31);

    private final ReportRepository reportRepository;
    private final DashboardRepository dashboardRepository;
    private final MunicipalityRepository municipalityRepository;
    private final CurrentUserService currentUser;

    private JasperReport compiledReport;

    @PostConstruct
    void compileTemplate() throws Exception {
        try (InputStream in = new ClassPathResource("reports/summary.jrxml").getInputStream()) {
            compiledReport = JasperCompileManager.compileReport(in);
        }
    }

    // ------------------------------------------------------------------ CSV
    @Transactional(readOnly = true)
    public byte[] csv(UUID municipalityId, LocalDate start, LocalDate end) {
        ReportData data = build(municipalityId, start, end);
        StringWriter sw = new StringWriter();
        sw.write('﻿'); // BOM para o Excel exibir acentos
        try (CSVPrinter csv = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            csv.printRecord("Relatório SGEV - Estradas Vicinais");
            csv.printRecord("Município", data.municipalityName() + "/" + data.state());
            csv.printRecord("Período", periodText(data));
            csv.printRecord("Gerado em", DT.format(data.generatedAt()), "por", data.generatedBy());
            csv.println();
            csv.printRecord("Km por condição");
            csv.printRecord("Condição", "Km");
            for (Map.Entry<String, BigDecimal> e : data.kmByCondition().entrySet()) {
                csv.printRecord(e.getKey(), e.getValue());
            }
            csv.printRecord("Total mapeado (km)", data.totalKm());
            csv.printRecord("Km recuperados (km)", data.repairedKm());
            csv.println();
            csv.printRecord("Trechos críticos");
            csv.printRecord("Trecho", "Condição", "Km");
            for (ReportData.CriticalSegment cs : data.criticalSegments()) {
                csv.printRecord(cs.name(), cs.condition(), cs.km());
            }
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_ERROR", "Falha ao gerar CSV");
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ----------------------------------------------------------------- XLSX
    @Transactional(readOnly = true)
    public byte[] xlsx(UUID municipalityId, LocalDate start, LocalDate end) {
        ReportData data = build(municipalityId, start, end);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Resumo");
            int r = 0;
            writeRow(sheet, r++, "Relatório SGEV - Estradas Vicinais");
            writeRow(sheet, r++, "Município", data.municipalityName() + "/" + data.state());
            writeRow(sheet, r++, "Período", periodText(data));
            writeRow(sheet, r++, "Gerado em", DT.format(data.generatedAt()) + " por " + data.generatedBy());
            r++;
            writeRow(sheet, r++, "Km por condição");
            writeRow(sheet, r++, "Condição", "Km");
            for (Map.Entry<String, BigDecimal> e : data.kmByCondition().entrySet()) {
                writeRow(sheet, r++, e.getKey(), e.getValue().toPlainString());
            }
            writeRow(sheet, r++, "Total mapeado (km)", data.totalKm().toPlainString());
            writeRow(sheet, r++, "Km recuperados (km)", data.repairedKm().toPlainString());
            r++;
            writeRow(sheet, r++, "Trechos críticos");
            writeRow(sheet, r++, "Trecho", "Condição", "Km");
            for (ReportData.CriticalSegment cs : data.criticalSegments()) {
                writeRow(sheet, r++, cs.name(), cs.condition(), cs.km().toPlainString());
            }
            sheet.setColumnWidth(0, 12000);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_ERROR", "Falha ao gerar XLSX");
        }
    }

    // ------------------------------------------------------------------ PDF
    @Transactional(readOnly = true)
    public byte[] pdf(UUID municipalityId, LocalDate start, LocalDate end) {
        ReportData data = build(municipalityId, start, end);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("municipality", data.municipalityName() + "/" + data.state());
        params.put("period", periodText(data));
        params.put("generatedInfo", "Gerado em " + DT.format(data.generatedAt()) + " por " + data.generatedBy());
        params.put("totalKm", data.totalKm().toPlainString());
        params.put("repairedKm", data.repairedKm().toPlainString());
        params.put("kmByCondition", kmByConditionText(data.kmByCondition()));

        List<Map<String, ?>> rows = new ArrayList<>();
        for (ReportData.CriticalSegment cs : data.criticalSegments()) {
            rows.add(Map.of("name", cs.name(), "condition", cs.condition(), "km", cs.km()));
        }

        try {
            JasperPrint print = JasperFillManager.fillReport(
                    compiledReport, params, new JRMapCollectionDataSource(rows));
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_ERROR", "Falha ao gerar PDF");
        }
    }

    private String kmByConditionText(Map<String, BigDecimal> km) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, BigDecimal> e : km.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue().toPlainString()).append(" km\n");
        }
        return sb.toString().trim();
    }

    private ReportData build(UUID municipalityId, LocalDate start, LocalDate end) {
        UUID target = resolveMunicipality(municipalityId);
        Municipality municipality = municipalityRepository.findByIdAndDeletedAtIsNull(target)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "MUNICIPALITY_NOT_FOUND", "Município não encontrado"));

        Map<String, BigDecimal> km = new LinkedHashMap<>();
        for (RoadCondition condition : RoadCondition.values()) {
            km.put(condition.name(), BigDecimal.ZERO);
        }
        for (DashboardRepository.KmRow row : dashboardRepository.kmByCondition(target)) {
            km.put(row.getCondition(), scale(row.getKm()));
        }
        BigDecimal total = km.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal repaired = scale(reportRepository.repairedKm(
                target, start != null ? start : MIN_DATE, end != null ? end : MAX_DATE));

        List<ReportData.CriticalSegment> critical = reportRepository.criticalSegments(target).stream()
                .map(c -> new ReportData.CriticalSegment(c.getName(), c.getCondition(), scale(c.getKm())))
                .toList();

        User user = currentUser.getCurrentUser();
        return new ReportData(
                municipality.getName(), municipality.getState(), start, end,
                LocalDateTime.now(), user.getName(),
                km, scale(total), repaired, critical);
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i] != null ? values[i] : "");
        }
    }

    private String periodText(ReportData data) {
        if (data.startDate() == null && data.endDate() == null) {
            return "todo o período";
        }
        String s = data.startDate() != null ? D.format(data.startDate()) : "início";
        String e = data.endDate() != null ? D.format(data.endDate()) : "hoje";
        return s + " a " + e;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private UUID resolveMunicipality(UUID requested) {
        User user = currentUser.getCurrentUser();
        if (user.isSuperAdmin()) {
            if (requested == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "MUNICIPALITY_REQUIRED", "SUPER_ADMIN deve informar municipalityId");
            }
            return requested;
        }
        if (requested != null) {
            currentUser.assertCanAccessMunicipality(requested);
        }
        if (user.getMunicipalityId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_MUNICIPALITY", "Usuário sem município vinculado");
        }
        return user.getMunicipalityId();
    }
}
