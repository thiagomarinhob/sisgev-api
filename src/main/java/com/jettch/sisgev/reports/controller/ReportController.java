package com.jettch.sisgev.reports.controller;

import com.jettch.sisgev.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * BE-24 — Exportação do relatório gerencial. Aceita municipalityId (SUPER_ADMIN) e
 * período opcional (startDate/endDate) para o cálculo de km recuperados.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv");
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService service;

    @GetMapping("/summary.csv")
    public ResponseEntity<byte[]> csv(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return download(service.csv(municipalityId, startDate, endDate), CSV, "summary.csv");
    }

    @GetMapping("/summary.xlsx")
    public ResponseEntity<byte[]> xlsx(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return download(service.xlsx(municipalityId, startDate, endDate), XLSX, "summary.xlsx");
    }

    @GetMapping("/summary.pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return download(service.pdf(municipalityId, startDate, endDate), MediaType.APPLICATION_PDF, "summary.pdf");
    }

    private ResponseEntity<byte[]> download(byte[] body, MediaType type, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(type)
                .body(body);
    }
}
