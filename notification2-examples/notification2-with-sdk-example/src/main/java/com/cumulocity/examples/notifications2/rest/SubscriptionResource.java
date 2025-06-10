package com.cumulocity.examples.notifications2.rest;

import com.cumulocity.examples.notifications2.model.ApiSubscription;
import com.cumulocity.examples.notifications2.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Void> subscribe(@RequestBody ApiSubscription apiSubscription) {
        subscriptionService.subscribe(apiSubscription);
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @RequestMapping(
            path = "/disconnect",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Void> disconnect(@RequestBody ApiSubscription apiSubscription) {
        subscriptionService.disconnect(apiSubscription);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(
            path = "/delete",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Void> delete(@RequestBody ApiSubscription apiSubscription) {
        subscriptionService.delete(apiSubscription);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
