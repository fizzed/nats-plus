package services;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.core.WorkerService;
import com.fizzed.executors.ninja.NinjaWorkerService;
import com.google.inject.Injector;
import javax.inject.Inject;
import javax.inject.Singleton;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

@Singleton
public class RequestConsumers extends WorkerService<RequestConsumer> {

    private final NinjaProperties ninjaProperties;
    private final Injector injector;
    
    @Inject
    public RequestConsumers(NinjaProperties ninjaProperties, Injector injector) {
        super("RequestConsumer");
        this.ninjaProperties = ninjaProperties;
        this.injector = injector;
        this.setExecuteDelay(TimeDuration.seconds(5));
        this.setInitialDelay(TimeDuration.seconds(5));
        this.setInitialDelayStagger(0.5);
    }
    
    @Override
    public RequestConsumer newWorker() {
        log.info("Building request consumer worker...");
        return this.injector.getInstance(RequestConsumer.class);
    }

    @Override @Start(order = 91)
    public void start() {
        // delegate most configuration to helper method
        NinjaWorkerService.configure("demo.request_consumer", this.ninjaProperties, this);
        super.start();
    }
    
    @Override @Dispose
    public void stop() {
        super.stop();
    }

}