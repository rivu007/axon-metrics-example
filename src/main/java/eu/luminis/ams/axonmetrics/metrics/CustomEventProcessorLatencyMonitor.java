/*
 * Copyright (c) 2010-2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.luminis.ams.axonmetrics.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.monitoring.MessageMonitor;
import org.axonframework.monitoring.NoOpMessageMonitorCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures the difference in message timestamps between the last ingested and the last processed message.
 *
 * @author Marijn van Zelst
 * @since 3.0
 */
public class CustomEventProcessorLatencyMonitor implements MessageMonitor<EventMessage<?>>, MetricSet {

    private final AtomicLong lastReceivedTime = new AtomicLong(-1);
    private final AtomicLong lastProcessedTime = new AtomicLong(-1);

    @Override
    public MonitorCallback onMessageIngested(EventMessage<?> message) {
        if(message == null){
            return NoOpMessageMonitorCallback.INSTANCE;
        }
        updateIfMaxValue(lastReceivedTime, message.getTimestamp().toEpochMilli());
        return new MonitorCallback() {
            @Override
            public void reportSuccess() {
                update();
            }

            @Override
            public void reportFailure(Throwable cause) {
                update();
            }

            @Override
            public void reportIgnored() {
                update();
            }

            private void update(){
                updateIfMaxValue(lastProcessedTime, message.getTimestamp().toEpochMilli());
            }
        };
    }

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metrics = new HashMap<>();
        metrics.put("latency", (Gauge<Long>) this::getLatency);
        return metrics;
    }

    private Long getLatency(){
        long lastProcessedTime = this.lastProcessedTime.longValue();
        long lastReceivedTime = this.lastReceivedTime.longValue();
        if(lastReceivedTime == -1 || lastProcessedTime == -1){
            return 0L;
        } else {
            return lastReceivedTime - lastProcessedTime;
        }
    }

    private void updateIfMaxValue(AtomicLong atomicLong, long timestamp){
        atomicLong.accumulateAndGet(timestamp, (currentValue, newValue) -> newValue > currentValue ? newValue : currentValue);
    }
}
