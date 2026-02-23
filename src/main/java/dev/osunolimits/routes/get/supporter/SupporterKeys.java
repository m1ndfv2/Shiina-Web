package dev.osunolimits.routes.get.supporter;

import java.util.List;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.SEOBuilder;
import spark.Request;
import spark.Response;

public class SupporterKeys extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 0);
        shiina.data.put("seo", new SEOBuilder("Supporter Keys", App.customization.get("homeDescription").toString()));
        shiina.data.put("pageStyles", List.of("/css/supporter-keys.css"));
        shiina.data.put("pageScripts", List.of("/js/supporter-keys.js"));

        return renderTemplate("supporter-keys.html", shiina, res, req);
    }
}
