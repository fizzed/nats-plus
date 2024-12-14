package conf;

import controllers.ApplicationController;
import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;

public class Routes implements ApplicationRoutes {

    @Override
    public void init(Router router) {
        router.GET().route("/").with(ApplicationController::home);
        router.GET().route("/publish").with(ApplicationController::publish);
        router.GET().route("/start").with(ApplicationController::start);
        router.GET().route("/stop").with(ApplicationController::stop);
        
        // assets
        router.GET().route("/assets/s/{fileName: .*}").with(AssetsController.class, "serveStatic");
        router.GET().route("/assets/j/{fileName: .*}").with(AssetsController.class, "serveWebJars");
    }
    
}