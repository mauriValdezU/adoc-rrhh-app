package com.adoc.rrhh.service;

import com.adoc.rrhh.entity.DetallePlanilla;
import com.adoc.rrhh.entity.Planilla;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class PdfGenerationService {

    public byte[] generarColillasPdf(Planilla planilla) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, java.awt.Color.decode("#C8102E"));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.BLACK);
            Font amountFont = FontFactory.getFont(FontFactory.COURIER, 10, java.awt.Color.BLACK);

            NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);

            boolean first = true;
            for (DetallePlanilla detalle : planilla.getDetalles()) {
                if (!first) {
                    document.newPage();
                }
                first = false;

                // Cabecera ADOC
                Paragraph header = new Paragraph("ADOC - Recursos Humanos", titleFont);
                header.setAlignment(Element.ALIGN_CENTER);
                document.add(header);

                Paragraph subheader = new Paragraph("Colilla de Pago - " + planilla.getTipoPlanilla().name() + " (" + planilla.getPeriodo() + ")", headerFont);
                subheader.setAlignment(Element.ALIGN_CENTER);
                subheader.setSpacingAfter(20);
                document.add(subheader);

                // Datos del Empleado
                PdfPTable empTable = new PdfPTable(2);
                empTable.setWidthPercentage(100);
                empTable.setSpacingAfter(20);
                empTable.addCell(getCell("Nombre: " + detalle.getEmpleado().getNombreCompleto(), boldFont));
                empTable.addCell(getCell("DUI: " + detalle.getEmpleado().getDui(), normalFont));
                empTable.addCell(getCell("Cargo: " + detalle.getEmpleado().getCargo(), normalFont));
                empTable.addCell(getCell("Departamento: " + detalle.getEmpleado().getDepartamento(), normalFont));
                document.add(empTable);

                // Tabla principal de Ingresos y Deducciones
                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{70f, 30f});

                // --- INGRESOS ---
                addSectionHeader(table, "INGRESOS", headerFont);
                addRow(table, "Salario Base Mensual", cf.format(detalle.getSalarioBase()), normalFont, amountFont);
                
                if (detalle.getDiasAusenciaDescontados() > 0) {
                    Font redFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.RED);
                    addRow(table, "(-) Descuento por Ausencias (" + detalle.getDiasAusenciaDescontados() + " días)", "-" + cf.format(detalle.getMontoDescuentoAusencias()), redFont, amountFont);
                }
                
                addRow(table, "(+) Otros Ingresos / Extras", cf.format(detalle.getMontoHorasExtras().add(detalle.getOtrosIngresos())), normalFont, amountFont);
                addRow(table, "TOTAL DEVENGADO", cf.format(detalle.getTotalDevengado()), boldFont, amountFont);

                // --- RETENCIONES ---
                addSectionHeader(table, "RETENCIONES DE LEY", headerFont);
                addRow(table, "ISSS (3%)", cf.format(detalle.getDeduccionIsss()), normalFont, amountFont);
                addRow(table, "AFP (7.25%)", cf.format(detalle.getDeduccionAfp()), normalFont, amountFont);
                addRow(table, "Renta (ISR)", cf.format(detalle.getDeduccionRenta()), normalFont, amountFont);
                addRow(table, "TOTAL DEDUCCIONES", cf.format(detalle.getTotalDeducciones()), boldFont, amountFont);

                // --- APORTACIONES PATRONALES ---
                addSectionHeader(table, "APORTACIONES PATRONALES (Informativo)", headerFont);
                addRow(table, "ISSS Patronal (7.5%)", cf.format(detalle.getAportacionPatronalIsss()), normalFont, amountFont);
                addRow(table, "AFP Patronal (8.75%)", cf.format(detalle.getAportacionPatronalAfp()), normalFont, amountFont);

                document.add(table);

                // --- TOTAL LÍQUIDO ---
                Paragraph liquido = new Paragraph("LÍQUIDO A PAGAR: " + cf.format(detalle.getSalarioNeto()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, java.awt.Color.decode("#10b981")));
                liquido.setAlignment(Element.ALIGN_RIGHT);
                liquido.setSpacingBefore(30);
                document.add(liquido);

                // Firmas
                Paragraph firmas = new Paragraph("\n\n\n___________________________\nFirma de Recibido", normalFont);
                firmas.setAlignment(Element.ALIGN_CENTER);
                document.add(firmas);
            }

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private PdfPCell getCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        return cell;
    }

    private void addSectionHeader(PdfPTable table, String title, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setColspan(2);
        cell.setBackgroundColor(java.awt.Color.decode("#f3f6f9"));
        cell.setPadding(8);
        cell.setBorderWidth(0);
        cell.setBorderWidthBottom(1);
        cell.setBorderColorBottom(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String label, String amount, Font labelFont, Font amountFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPadding(5);
        
        PdfPCell cell2 = new PdfPCell(new Phrase(amount, amountFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell2.setPadding(5);
        
        table.addCell(cell1);
        table.addCell(cell2);
    }
}
