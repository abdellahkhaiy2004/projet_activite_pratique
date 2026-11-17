package ma.ebank.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "eureka.client.enabled=false")
class GatewayRoutesTest {

    @Autowired
    RouteLocator routeLocator;

    @Autowired
    DiscoveryClientRouteDefinitionLocator discoveryLocator;

    @Test
    void dynamicDiscoveryRoutingIsEnabled() {
        assertNotNull(routeLocator);
        assertNotNull(discoveryLocator);
    }
}
