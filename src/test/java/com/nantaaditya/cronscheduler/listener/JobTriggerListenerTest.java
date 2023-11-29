package com.nantaaditya.cronscheduler.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.cronscheduler.listener.JobTriggerListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

@ExtendWith(MockitoExtension.class)
class JobTriggerListenerTest {

  @InjectMocks
  private JobTriggerListener listener;

  @Mock
  private Trigger trigger;

  @Mock
  private TriggerKey triggerKey;

  @Test
  void triggerMissfired() {
    when(trigger.getKey()).thenReturn(triggerKey);
    listener.triggerMisfired(trigger);
    verify(trigger).getKey();
  }
}