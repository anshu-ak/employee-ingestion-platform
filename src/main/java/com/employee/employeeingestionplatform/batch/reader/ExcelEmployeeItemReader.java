package com.employee.employeeingestionplatform.batch.reader;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.infrastructure.item.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ExcelEmployeeItemReader
        implements ItemStreamReader<EmployeeExcelRow> {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "empid",
            "firstname",
            "lastname",
            "email",
            "department",
            "salary"
    );

    private final Path filePath;

    private InputStream inputStream;
    private Workbook workbook;
    private FormulaEvaluator formulaEvaluator;
    private DataFormatter dataFormatter;
    private Iterator<Row> rowIterator;

    public ExcelEmployeeItemReader(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        try {
            inputStream = Files.newInputStream(filePath);
            workbook = WorkbookFactory.create(inputStream);

            if (workbook.getNumberOfSheets() == 0) {
                throw new ItemStreamException(
                        "The Excel workbook does not contain any sheets"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);
            rowIterator = sheet.rowIterator();
            dataFormatter = new DataFormatter(Locale.ROOT);
            formulaEvaluator = workbook
                    .getCreationHelper()
                    .createFormulaEvaluator();

            validateHeader();
        } catch (IOException exception) {
            throw new ItemStreamException(
                    "Could not open Excel file: " + filePath,
                    exception
            );
        }
    }

    @Override
    public EmployeeExcelRow read() {
        while (rowIterator != null && rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (isEmpty(row)) {
                continue;
            }

            return new EmployeeExcelRow(
                    row.getRowNum() + 1,
                    getText(row, 0),
                    getText(row, 1),
                    getText(row, 2),
                    getText(row, 3),
                    getText(row, 4),
                    getSalary(row, 5)
            );
        }

        return null;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // Restart-state support can be added later if required.
    }

    @Override
    public void close() {
        try {
            if (workbook != null) {
                workbook.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException exception) {
            throw new ItemStreamException(
                    "Could not close Excel file",
                    exception
            );
        }
    }

    private void validateHeader() {
        if (!rowIterator.hasNext()) {
            throw new ItemStreamException(
                    "The Excel worksheet is empty"
            );
        }

        Row headerRow = rowIterator.next();

        for (int column = 0;
             column < EXPECTED_HEADERS.size();
             column++) {

            String actualHeader = getText(headerRow, column)
                    .replace("_", "")
                    .replace(" ", "")
                    .toLowerCase(Locale.ROOT);

            String expectedHeader = EXPECTED_HEADERS.get(column);

            if (!expectedHeader.equals(actualHeader)) {
                throw new ItemStreamException(
                        "Invalid Excel header at column "
                                + (column + 1)
                                + ". Expected '"
                                + expectedHeader
                                + "' but found '"
                                + actualHeader
                                + "'"
                );
            }
        }
    }

    private String getText(Row row, int columnIndex) {
        Cell cell = row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
        );

        if (cell == null) {
            return null;
        }

        String value = dataFormatter
                .formatCellValue(cell, formulaEvaluator)
                .trim();

        return value.isEmpty() ? null : value;
    }

    private BigDecimal getSalary(Row row, int columnIndex) {
        String value = getText(row, columnIndex);

        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isEmpty(Row row) {
        for (int column = 0;
             column < EXPECTED_HEADERS.size();
             column++) {

            if (getText(row, column) != null) {
                return false;
            }
        }

        return true;
    }
}