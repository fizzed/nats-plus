package conf;

import com.fizzed.nats.ninja.NinjaNatsModule;
import ninja.conf.FrameworkModule;
import ninja.conf.NinjaClassicModule;
import ninja.utils.NinjaProperties;
import services.RequestConsumers;

public class Module extends FrameworkModule {

    private final NinjaProperties ninjaProperties;
    
    public Module(NinjaProperties ninjaProperties) {
        this.ninjaProperties = ninjaProperties;
    }
    
    @Override
    protected void configure() {
        install(new NinjaClassicModule(ninjaProperties)
            .freemarker(false)
            .xml(false)
            //.jpa(false)
            .cache(false));

        // installs nat module to handle being connected
        install(new NinjaNatsModule());

        // example consumers, and pools
        bind(DemoNatsMigration.class);
        bind(RequestConsumers.class);
    }

}