package ma.ebank.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GatewayRoutesTest {

    @Autowired
    RouteLocator routeLocator;

    @Test
    void staticRoutesAreRegistered() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertEquals("http://localhost:8056",
                routes.stream().filter(r -> r.getId().equals("customer-service-static")).findFirst().get().getUri().toString());
        assertEquals("http://localhost:8057",
                routes.stream().filter(r -> r.getId().equals("ebank-service-static")).findFirst().get().getUri().toString());
    }
}
