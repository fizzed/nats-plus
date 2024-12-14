package services;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.core.ExecuteStopException;
import com.fizzed.executors.core.Worker;
import com.fizzed.executors.core.WorkerContext;

import java.time.Duration;
import javax.inject.Inject;

import com.fizzed.nats.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fizzed.nats.core.NatsHelper.dumpMessage;

public class RequestConsumer implements Worker {
    static private final Logger log = LoggerFactory.getLogger(RequestConsumer.class);

    final private NatsReliableStreamPullSubscriber subscriber;
    
    @Inject
    public RequestConsumer(NatsConnectionProvider natsConnectionProvider) {
        this.subscriber = new NatsReliableStreamPullSubscriber(natsConnectionProvider)
            .setSubject("demo.requests.queue")
            .setDurable("demo-requests-queue-consumer");
    }

    @Override
    public Logger getLogger() {
        return log;
    }
    
    @Override
    public void execute(WorkerContext context) throws ExecuteStopException, InterruptedException {
        while (true) {
            try {
                context.idle("Starting consumer...");
                this.subscriber.start();

                while (true) {
                    try {
                        context.idle("Waiting for request...");

                        final NatsReliableMessage message = this.subscriber.nextMessage(Duration.ofSeconds(60));

                        if (message != null) {
                            log.debug("Processing request \n{}", dumpMessage(message));

                            message.ack();

                            context.running("Processing request");

                            Thread.sleep(TimeDuration.seconds(5).asMillis());
                        }
                    } catch (NatsRecoverableException e) {
                        log.warn("Recoverable exception (will wait a bit)", e);
                        context.idle(TimeDuration.seconds(5), "Recoverable exception thrown (waiting a bit to try again)");
                    }
                }
            } catch (NatsUnrecoverableException e) {
                log.warn("Unrecoverable exception (will wait a bit)", e);
                context.idle(TimeDuration.seconds(5), "Unrecoverable exception thrown (waiting a bit to try again)");
            } catch (InterruptedException e) {
                log.debug("Interrupted, exiting");
                return;
            }
        }
    }
    
}