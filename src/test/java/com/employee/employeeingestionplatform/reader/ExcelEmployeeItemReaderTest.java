package com.employee.employeeingestionplatform.batch.reader;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelEmployeeItemReaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldReadEmployeeFromExcel() throws Exception {
        Path excelFile = temporaryDirectory.resolve(
                "employees.xlsx"
        );

        createValidWorkbook(excelFile);

        ExcelEmployeeItemReader reader =
                new ExcelEmployeeItemReader(excelFile);

        reader.open(new ExecutionContext());

        EmployeeExcelRow row = reader.read();
        EmployeeExcelRow nextRow = reader.read();

        reader.close();

        assertAll(
                () -> assertEquals(2, row.rowNumber()),
                () -> assertEquals("EMP001", row.empId()),
                () -> assertEquals("Anshu", row.firstName()),
                () -> assertEquals("Kumari", row.lastName()),
                () -> assertEquals(
                        "anshu@example.com",
                        row.email()
                ),
                () -> assertEquals(
                        "Engineering",
                        row.department()
                ),
                () -> assertEquals(
                        new BigDecimal("125000"),
                        row.salary()
                ),
                () -> assertNull(nextRow)
        );
    }

    @Test
    void shouldIgnoreCompletelyEmptyRows() throws Exception {
        Path excelFile = temporaryDirectory.resolve(
                "employees-with-empty-row.xlsx"
        );

        createWorkbookWithEmptyRow(excelFile);

        ExcelEmployeeItemReader reader =
                new ExcelEmployeeItemReader(excelFile);

        reader.open(new ExecutionContext());

        EmployeeExcelRow firstEmployee = reader.read();
        EmployeeExcelRow secondEmployee = reader.read();
        EmployeeExcelRow end = reader.read();

        reader.close();

        assertAll(
                () -> assertEquals(
                        "EMP001",
                        firstEmployee.empId()
                ),
                () -> assertEquals(
                        "EMP002",
                        secondEmployee.empId()
                ),
                () -> assertEquals(
                        4,
                        secondEmployee.rowNumber()
                ),
                () -> assertNull(end)
        );
    }

    @Test
    void shouldRejectInvalidHeader() throws Exception {
        Path excelFile = temporaryDirectory.resolve(
                "invalid-header.xlsx"
        );

        createInvalidHeaderWorkbook(excelFile);

        ExcelEmployeeItemReader reader =
                new ExcelEmployeeItemReader(excelFile);

        ItemStreamException exception = assertThrows(
                ItemStreamException.class,
                () -> reader.open(new ExecutionContext())
        );

        reader.close();

        assertEquals(
                "Invalid Excel header at column 1. "
                        + "Expected 'empid' but found 'employeeid'",
                exception.getMessage()
        );
    }

    private void createValidWorkbook(Path file)
            throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

            createHeader(sheet);

            Row row = sheet.createRow(1);
            createEmployeeRow(
                    row,
                    "EMP001",
                    "Anshu",
                    "Kumari",
                    "anshu@example.com",
                    "Engineering",
                    125000
            );

            writeWorkbook(workbook, file);
        }
    }

    private void createWorkbookWithEmptyRow(Path file)
            throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

            createHeader(sheet);

            Row firstRow = sheet.createRow(1);
            createEmployeeRow(
                    firstRow,
                    "EMP001",
                    "Anshu",
                    "Kumari",
                    "anshu@example.com",
                    "Engineering",
                    125000
            );

            sheet.createRow(2);

            Row secondRow = sheet.createRow(3);
            createEmployeeRow(
                    secondRow,
                    "EMP002",
                    "Test",
                    "Employee",
                    "test@example.com",
                    "Finance",
                    90000
            );

            writeWorkbook(workbook, file);
        }
    }

    private void createInvalidHeaderWorkbook(Path file)
            throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("employeeId");
            header.createCell(1).setCellValue("firstName");
            header.createCell(2).setCellValue("lastName");
            header.createCell(3).setCellValue("email");
            header.createCell(4).setCellValue("department");
            header.createCell(5).setCellValue("salary");

            writeWorkbook(workbook, file);
        }
    }

    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("empId");
        header.createCell(1).setCellValue("firstName");
        header.createCell(2).setCellValue("lastName");
        header.createCell(3).setCellValue("email");
        header.createCell(4).setCellValue("department");
        header.createCell(5).setCellValue("salary");
    }

    private void createEmployeeRow(
            Row row,
            String empId,
            String firstName,
            String lastName,
            String email,
            String department,
            double salary
    ) {
        row.createCell(0).setCellValue(empId);
        row.createCell(1).setCellValue(firstName);
        row.createCell(2).setCellValue(lastName);
        row.createCell(3).setCellValue(email);
        row.createCell(4).setCellValue(department);
        row.createCell(5).setCellValue(salary);
    }

    private void writeWorkbook(
            XSSFWorkbook workbook,
            Path file
    ) throws IOException {
        try (OutputStream outputStream =
                     Files.newOutputStream(file)) {
            workbook.write(outputStream);
        }
    }
}