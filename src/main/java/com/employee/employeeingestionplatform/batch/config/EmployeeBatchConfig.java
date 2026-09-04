package com.employee.employeeingestionplatform.batch.config;

import com.employee.employeeingestionplatform.batch.exception.InvalidEmployeeRowException;
import com.employee.employeeingestionplatform.batch.listener.EmployeeJobListener;
import com.employee.employeeingestionplatform.batch.listener.RejectedEmployeeSkipListener;
import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import com.employee.employeeingestionplatform.batch.processor.EmployeeItemProcessor;
import com.employee.employeeingestionplatform.batch.reader.ExcelEmployeeItemReader;
import com.employee.employeeingestionplatform.batch.validation.EmployeeRowValidator;
import com.employee.employeeingestionplatform.batch.writer.EmployeeItemWriter;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import com.employee.employeeingestionplatform.repository.RejectedRecordRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.util.UUID;

@Configuration
public class EmployeeBatchConfig {

    @Bean
    @StepScope
    ExcelEmployeeItemReader employeeExcelItemReader(
            @Value("#{jobParameters['filePath']}") String filePath
    ) {
        return new ExcelEmployeeItemReader(Path.of(filePath));
    }

    @Bean
    EmployeeItemProcessor employeeItemProcessor(
            EmployeeRowValidator rowValidator,
            EmployeeRepository employeeRepository
    ) {
        return new EmployeeItemProcessor(
                rowValidator,
                employeeRepository
        );
    }

    @Bean
    @StepScope
    RejectedEmployeeSkipListener rejectedEmployeeSkipListener(
            @Value("#{jobParameters['trackingId']}")
            String trackingId,
            RejectedRecordRepository rejectedRecordRepository
    ) {
        return new RejectedEmployeeSkipListener(
                UUID.fromString(trackingId),
                rejectedRecordRepository
        );
    }

    @Bean
    Step employeeImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ExcelEmployeeItemReader employeeExcelItemReader,
            EmployeeItemProcessor employeeItemProcessor,
            EmployeeItemWriter employeeItemWriter,
            RejectedEmployeeSkipListener rejectedEmployeeSkipListener,
            @Value("${app.upload.chunk-size}") int chunkSize
    ) {
        return new StepBuilder(
                "employeeImportStep",
                jobRepository
        )
                .<EmployeeExcelRow, Employee>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(employeeExcelItemReader)
                .processor(employeeItemProcessor)
                .writer(employeeItemWriter)
                .faultTolerant()
                .skip(InvalidEmployeeRowException.class)
                .skipLimit(10_000)
                .skipListener(rejectedEmployeeSkipListener)
                .build();
    }

    @Bean
    Job employeeImportJob(
            JobRepository jobRepository,
            Step employeeImportStep,
            EmployeeJobListener employeeJobListener
    ) {
        return new JobBuilder(
                "employeeImportJob",
                jobRepository
        )
                .listener(employeeJobListener)
                .start(employeeImportStep)
                .build();

    }
}