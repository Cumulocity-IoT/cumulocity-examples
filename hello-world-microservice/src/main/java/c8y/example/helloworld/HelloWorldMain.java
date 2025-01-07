package c8y.example.helloworld;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@MicroserviceApplication
@ComponentScan(basePackages = {
        "c8y.example.helloworld",
        "com.cumulocity.exporters.platform",
        "com.cumulocity.exporters.common"
})
public class HelloWorldMain {

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldMain.class, args);
    }

    @RequestMapping("hello")
    public String greeting(@RequestParam(value = "who", defaultValue = "world") String who) {
        return "hello " + who + "!";
    }

}
