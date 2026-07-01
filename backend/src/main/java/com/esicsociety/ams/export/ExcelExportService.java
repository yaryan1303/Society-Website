package com.esicsociety.ams.export;

import com.esicsociety.ams.favourstood.FavourStoodEntry;
import com.esicsociety.ams.favourstood.FavourStoodEntryRepository;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.ledger.AbstractLedgerTxn;
import com.esicsociety.ams.ledger.CompulsoryDepositTxnRepository;
import com.esicsociety.ams.ledger.OtherDepositTxnRepository;
import com.esicsociety.ams.ledger.ShareTxnRepository;
import com.esicsociety.ams.loan.LoanTxn;
import com.esicsociety.ams.loan.LoanTxnRepository;
import com.esicsociety.ams.member.Member;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds audit-ready .xlsx ledgers using Apache POI, with the same column layout
 * as the paper book. Exports a single member's yearly ledger or all members for
 * a chosen year (one sheet per member).
 */
@Service
public class ExcelExportService {

    private static final String SOCIETY_NAME = "ESIC Employees Cooperative Credit & Thrift Society Ltd.";
    private static final String SOCIETY_HINDI = "ईएसआईसी सहकारी समिति, मुख्यालय";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ShareTxnRepository shareRepo;
    private final CompulsoryDepositTxnRepository cdRepo;
    private final OtherDepositTxnRepository odRepo;
    private final LoanTxnRepository loanTxnRepo;
    private final FavourStoodEntryRepository favourRepo;

    public ExcelExportService(ShareTxnRepository shareRepo, CompulsoryDepositTxnRepository cdRepo,
                              OtherDepositTxnRepository odRepo, LoanTxnRepository loanTxnRepo,
                              FavourStoodEntryRepository favourRepo) {
        this.shareRepo = shareRepo;
        this.cdRepo = cdRepo;
        this.odRepo = odRepo;
        this.loanTxnRepo = loanTxnRepo;
        this.favourRepo = favourRepo;
    }

    @Transactional(readOnly = true)
    public byte[] exportMember(Member member, FinancialYear year) {
        try (Workbook wb = new XSSFWorkbook()) {
            writeMemberSheet(wb, member, year);
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportAll(List<Member> members, FinancialYear year) {
        try (Workbook wb = new XSSFWorkbook()) {
            if (members.isEmpty()) {
                wb.createSheet("No members");
            }
            for (Member m : members) {
                writeMemberSheet(wb, m, year);
            }
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------------

    private void writeMemberSheet(Workbook wb, Member member, FinancialYear year) {
        String safeName = (member.getAccountNo() + " " + member.getName()).replaceAll("[\\\\/?*\\[\\]:]", " ");
        if (safeName.length() > 31) safeName = safeName.substring(0, 31);
        Sheet sheet = wb.createSheet(safeName);

        Styles s = new Styles(wb);
        int[] r = {0};

        title(sheet, r, s, SOCIETY_NAME, true);
        title(sheet, r, s, SOCIETY_HINDI, false);
        title(sheet, r, s, "Personal Ledger — " + member.getName()
                + "  (A/c " + member.getAccountNo() + ")   Financial Year " + year.getLabel(), false);
        r[0]++;

        writeLedgerSection(sheet, r, s, "SHARES",
                shareRepo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(member.getId(), year.getId()));
        writeLedgerSection(sheet, r, s, "COMPULSORY DEPOSIT",
                cdRepo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(member.getId(), year.getId()));
        writeLedgerSection(sheet, r, s, "OTHER DEPOSIT",
                odRepo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(member.getId(), year.getId()));
        writeLoanSection(sheet, r, s,
                loanTxnRepo.findByLoan_Member_IdAndFinancialYear_IdOrderByTxnDateAscIdAsc(member.getId(), year.getId()));
        writeFavourSection(sheet, r, s,
                favourRepo.findByMember_IdAndFinancialYear_IdOrderByEntryDateAscIdAsc(member.getId(), year.getId()));

        for (int c = 0; c < 12; c++) sheet.autoSizeColumn(c);
    }

    private void writeLedgerSection(Sheet sheet, int[] r, Styles s, String heading,
                                    List<? extends AbstractLedgerTxn> txns) {
        sectionHeading(sheet, r, s, heading);
        Row header = sheet.createRow(r[0]++);
        headerCells(header, s, "Date", "Particulars", "Dr", "Cr", "Balance");
        for (AbstractLedgerTxn t : txns) {
            Row row = sheet.createRow(r[0]++);
            cell(row, 0, s, t.getTxnDate() == null ? "" : t.getTxnDate().format(DATE));
            cell(row, 1, s, t.getParticulars());
            money(row, 2, s, t.getDr());
            money(row, 3, s, t.getCr());
            money(row, 4, s, t.getBalanceAfter());
        }
        BigDecimal closing = txns.isEmpty() ? BigDecimal.ZERO : txns.get(txns.size() - 1).getBalanceAfter();
        Row total = sheet.createRow(r[0]++);
        cell(total, 1, s, "Closing balance", true);
        money(total, 4, s, closing, true);
        r[0]++;
    }

    private void writeLoanSection(Sheet sheet, int[] r, Styles s, List<LoanTxn> txns) {
        sectionHeading(sheet, r, s, "LOAN & INTEREST");
        Row header = sheet.createRow(r[0]++);
        headerCells(header, s, "Date", "Type", "C.B. Folio", "Loan Dr", "Loan Cr", "Loan Balance",
                "Int. Charged", "Int. Paid", "Int. Balance", "Mode", "Receipt No.");
        for (LoanTxn t : txns) {
            Row row = sheet.createRow(r[0]++);
            cell(row, 0, s, t.getTxnDate() == null ? "" : t.getTxnDate().format(DATE));
            cell(row, 1, s, t.getTxnType() == null ? "" : t.getTxnType().name());
            cell(row, 2, s, t.getCbFolio());
            money(row, 3, s, t.getLoanDr());
            money(row, 4, s, t.getLoanCr());
            money(row, 5, s, t.getLoanBalanceAfter());
            money(row, 6, s, t.getInterestCharged());
            money(row, 7, s, t.getInterestPaid());
            money(row, 8, s, t.getInterestBalanceAfter());
            cell(row, 9, s, t.getPaymentMode() == null ? "" : t.getPaymentMode().name());
            cell(row, 10, s, t.getReceiptNo());
        }
        r[0]++;
    }

    private void writeFavourSection(Sheet sheet, int[] r, Styles s, List<FavourStoodEntry> entries) {
        sectionHeading(sheet, r, s, "MEMBERS IN WHOSE FAVOUR STOOD");
        Row header = sheet.createRow(r[0]++);
        headerCells(header, s, "Date", "Amount", "Note");
        BigDecimal total = BigDecimal.ZERO;
        for (FavourStoodEntry e : entries) {
            Row row = sheet.createRow(r[0]++);
            cell(row, 0, s, e.getEntryDate() == null ? "" : e.getEntryDate().format(DATE));
            money(row, 1, s, e.getAmount());
            cell(row, 2, s, e.getNote());
            total = total.add(e.getAmount() == null ? BigDecimal.ZERO : e.getAmount());
        }
        if (!entries.isEmpty()) {
            Row totalRow = sheet.createRow(r[0]++);
            cell(totalRow, 0, s, "Total", true);
            money(totalRow, 1, s, total, true);
        }
        r[0]++;
    }

    // ---- low-level cell helpers ----

    private void title(Sheet sheet, int[] r, Styles s, String text, boolean big) {
        Row row = sheet.createRow(r[0]++);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(big ? s.title : s.subtitle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 8));
    }

    private void sectionHeading(Sheet sheet, int[] r, Styles s, String text) {
        Row row = sheet.createRow(r[0]++);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(s.section);
    }

    private void headerCells(Row row, Styles s, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(s.header);
        }
    }

    private void cell(Row row, int col, Styles s, String value) {
        cell(row, col, s, value, false);
    }

    private void cell(Row row, int col, Styles s, String value, boolean bold) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        if (bold) c.setCellStyle(s.bold);
    }

    private void money(Row row, int col, Styles s, BigDecimal value) {
        money(row, col, s, value, false);
    }

    private void money(Row row, int col, Styles s, BigDecimal value, boolean bold) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? 0d : value.doubleValue());
        c.setCellStyle(bold ? s.moneyBold : s.money);
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    /** Reusable cell styles for one workbook. */
    private static final class Styles {
        final CellStyle title, subtitle, section, header, money, moneyBold, bold;

        Styles(Workbook wb) {
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = wb.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);

            subtitle = wb.createCellStyle();
            subtitle.setAlignment(HorizontalAlignment.CENTER);

            Font sectionFont = wb.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 12);
            section = wb.createCellStyle();
            section.setFont(sectionFont);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            header = wb.createCellStyle();
            header.setFont(headerFont);

            String fmt = "#,##0.00";
            money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat(fmt));

            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(wb.createDataFormat().getFormat(fmt));
            moneyBold.setFont(boldFont);

            bold = wb.createCellStyle();
            bold.setFont(boldFont);
        }
    }
}
