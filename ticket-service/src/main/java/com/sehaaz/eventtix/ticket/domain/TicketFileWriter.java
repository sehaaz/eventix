package com.sehaaz.eventtix.ticket.domain;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Bilet başına QR PNG ve PDF dosyasını diske yazar.
 */
@Component
public class TicketFileWriter {

    private static final int QR_SIZE = 300;

    private final Path storageDir;

    public TicketFileWriter(@Value("${ticket.storage-dir}") String storageDir) {
        this.storageDir = Path.of(storageDir);
    }

    public Path writeQr(String ticketCode) throws IOException, WriterException {
        Files.createDirectories(storageDir);
        Path path = storageDir.resolve(ticketCode + ".png");
        MatrixToImageWriter.writeToPath(
                new QRCodeWriter().encode(ticketCode, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE), "PNG", path);
        return path;
    }

    // Varsayılan Helvetica fontunda Türkçe karakterler yok, metinler ASCII.
    public Path writePdf(Ticket ticket, int total, Path qrPath) throws IOException {
        Path path = storageDir.resolve(ticket.getTicketCode() + ".pdf");
        try (OutputStream out = Files.newOutputStream(path)) {
            Document document = new Document(PageSize.A6);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("EventTix Bilet " + ticket.getSeqNo() + " / " + total));
            document.add(new Paragraph("Etkinlik: #" + ticket.getEventId()));
            document.add(new Paragraph("Siparis: #" + ticket.getOrderId()));
            document.add(new Paragraph("Kod: " + ticket.getTicketCode()));
            Image qr = Image.getInstance(qrPath.toString());
            qr.scaleToFit(180, 180);
            document.add(qr);
            document.close();
        }
        return path;
    }
}
