package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.entity.JobHistory;
import com.nantaaditya.cronscheduler.model.constant.JobStatus;
import com.nantaaditya.cronscheduler.model.dto.JobExecutorDetailDTO;
import com.nantaaditya.cronscheduler.repository.JobDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryRepository;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzInitializerConfiguration {
  private final JobExecutorRepository jobExecutorRepository;

  private final JobDetailRepository jobDetailRepository;

  private final JobHistoryRepository jobHistoryRepository;

  private final Scheduler scheduler;

  private final QuartzUtil quartzUtil;

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() throws SchedulerException {
    scheduler.start();

    jobExecutorRepository.findAllByActiveTrue()
        .flatMap(this::findJobExecutorDetailAndHistory)
        .doOnNext(tuples -> {
          JobExecutorDetailDTO dto = tuples.getT1();
          quartzUtil.createJob(dto.jobDetail(), tuples.getT2());
        })
        .subscribe(result -> log.info("#JOB - initialization completed"));
  }

  @PreDestroy
  public void onStop() throws SchedulerException {
    scheduler.shutdown(true);
    log.info("#JOB - shutdown");
  }

  private Mono<Tuple2<JobExecutorDetailDTO, Boolean>> findJobExecutorDetailAndHistory(JobExecutor jobExecutor) {
    Pageable pageable = PageRequest.of(0, 1, Sort.by(Direction.DESC, "executed_date", "executed_time"));
    return Mono.zip(
        jobDetailRepository.findById(jobExecutor.getJobId()),
        jobHistoryRepository.findByJobExecutorIdAndStatusIn(
            jobExecutor.getJobId(), List.of(JobStatus.STARTING.name(), JobStatus.RUNNING.name()), pageable
        ).collectList(),
        (JobDetail jobDetail, List<JobHistory> jobHistories) -> Tuples.of(
            new JobExecutorDetailDTO(jobExecutor, jobDetail),
            jobHistories.isEmpty()
        )
    );
  }
}
