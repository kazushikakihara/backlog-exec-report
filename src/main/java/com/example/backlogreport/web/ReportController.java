package com.example.backlogreport.web;

import com.example.backlogreport.service.ReportService;
import com.example.backlogreport.service.ReportService.ReportData;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Controller
public class ReportController {
  private final ReportService reportService;
  public ReportController(ReportService reportService) { this.reportService = reportService; }

  @GetMapping("/reports/executive")
  public String executiveHtml(
      @RequestParam(required = false) List<String> projectKeys,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
      Model model) {
    ReportData data = reportService.buildReport(projectKeys, since, until);
    model.addAttribute("data", data);
    return "executive";
  }

  @GetMapping(value = "/reports/executive.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public void executivePdf(
      @RequestParam(required = false) List<String> projectKeys,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
      HttpServletResponse response) throws IOException {
    String html = HtmlTemplates.renderMinimalHtml(reportService.buildReport(projectKeys, since, until));
    byte[] bytes = HtmlTemplates.htmlToPdf(html);
    response.setHeader("Content-Disposition", "attachment; filename=executive-report.pdf");
    response.getOutputStream().write(bytes);
  }

  static class HtmlTemplates {
    static String renderMinimalHtml(ReportData d) {
      StringBuilder sb = new StringBuilder();
      sb.append("<html><head><meta charset='UTF-8'><style>body{font-family:sans-serif}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:6px}th{background:#f5f5f5}</style></head><body>");
      sb.append("<h2>Executive Report</h2>");
      if (d.since != null || d.until != null) {
        sb.append("<p>期間: ").append(d.since==null?"":d.since).append(" ～ ").append(d.until==null?"":d.until).append("</p>");
      }
      sb.append("<table><thead><tr><th>Project</th><th>Open</th><th>Closed</th><th>New (window)</th><th>Overdue</th></tr></thead><tbody>");
      for (var r : d.rows) {
        sb.append("<tr><td>").append(r.projectKey).append(" - ").append(r.projectName)
          .append("</td><td>").append(r.openTotal)
          .append("</td><td>").append(r.closedTotal)
          .append("</td><td>").append(r.createdInWindow)
          .append("</td><td>").append(r.overdue)
          .append("</td></tr>");
      }
      sb.append("</tbody></table></body></html>");
      return sb.toString();
    }

    static byte[] htmlToPdf(String html) throws IOException {
      try (var out = new java.io.ByteArrayOutputStream()) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
      } catch (Exception e) {
        throw new IOException("PDF rendering failed", e);
      }
    }
  }
}