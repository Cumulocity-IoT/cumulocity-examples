package notifications2.rest;

import lombok.extern.slf4j.Slf4j;
import notifications2.model.Subscription;
import notifications2.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * This resource allows to dynamically subscribe/unsubscribe notification topics.
 * It's not required to have this to use Notifications2Api - it's just a part of test microservice logic.
 */
@Slf4j
@RestController
public class SubscriptionResource {
    @Autowired
    private SubscriptionService subscriptionService;

    @RequestMapping(
            path = "/subscribe",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Void> subscribe(@RequestBody Subscription subscription) {
        subscriptionService.subscribe(subscription);
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @RequestMapping(
            path = "/unsubscribe",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Void> unsubscribe(@RequestBody Subscription subscription) {
        subscriptionService.unsubscribe(subscription);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
