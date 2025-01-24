package c8y.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.settings.service.MicroserviceSettingsService;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.event.EventFilter;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import net.minidev.json.JSONObject;

@MicroserviceApplication
@RestController
public class App {

	@Autowired
	private MicroserviceSettingsService settingsService;

	@Autowired
	private ContextService<MicroserviceCredentials> contextService;

	@Autowired
	private Platform platform;

	private Map<String, String> c8yEnv;

	public static void main (String[] args) {
		SpringApplication.run(App.class, args);
	}

	
	/**
	 * Get some of the environment variables of the container and load the
	 * microservice settings
	 */
	@PostConstruct
	private void init () {
		// Environment variables
		var env = System.getenv();

		c8yEnv = new HashMap<>();
		c8yEnv.put("app.name", env.get("APPLICATION_NAME"));
		c8yEnv.put("url", env.get("C8Y_BASEURL"));
		c8yEnv.put("jdk", env.get("JAVA_VERSION"));
		c8yEnv.put("tenant", env.get("C8Y_TENANT"));
		c8yEnv.put("user", env.get("C8Y_USER"));
		c8yEnv.put("password", env.get("C8Y_PASSWORD"));
		c8yEnv.put("isolation", env.get("C8Y_MICROSERVICE_ISOLATION"));
		c8yEnv.put("memory.limit", env.get("MEMORY_LIMIT"));

		// Required ID and key
		c8yEnv.put("tracker.id", settingsService.get("tracker.id"));
		c8yEnv.put("ipstack.key", settingsService.get("ipstack.key"));
	}

	
	/**
	 * Create a warning alarm on microservice subscription
	 */
	@EventListener(MicroserviceSubscriptionAddedEvent.class)
	public void createAlarm (MicroserviceSubscriptionAddedEvent event) {
		contextService.callWithinContext(event.getCredentials(), () -> {
			var source = new ManagedObjectRepresentation();
			source.setId(GId.asGId(c8yEnv.get("tracker.id")));

			var alarm = new AlarmRepresentation();
			alarm.setSource(source);
			alarm.setSeverity("WARNING");
			alarm.setStatus("ACTIVE");
			alarm.setDateTime(DateTime.now());
			alarm.setType("c8y_Application__Microservice_subscribed");
			alarm.setText("The microservice " + c8yEnv.get("app.name") + " has been subscribed to tenant "
					+ c8yEnv.get("tenant"));

			platform.getAlarmApi().create(alarm);

			return true;
		});
	}

	
	/**
	 * Create a LocationUpdate event based on the client's IP
	 * 
	 * @param String The public IP of the client
	 * @return The created event
	 */
	public EventRepresentation createLocationUpdateEvent (String ip) {
		// Get location details from ipstack
		var rest = new RestTemplate();
		var apiURL = "http://api.ipstack.com/" + ip + "?access_key=" + c8yEnv.get("ipstack.key");
		var location = rest.getForObject(apiURL, Location.class);

		// Prepare a LocationUpdate event using Cumulocity's API
		var c8y_Position = new JSONObject();
		c8y_Position.put("lat", location.getLatitude());
		c8y_Position.put("lng", location.getLongitude());

		var source = new ManagedObjectRepresentation();
		source.setId(GId.asGId(c8yEnv.get("tracker.id")));

		var event = new EventRepresentation();
		event.setSource(source);
		event.setType("c8y_LocationUpdate");
		event.setDateTime(DateTime.now());
		event.setText("Accessed from " + ip + " (" + (location.getCity() != null ? location.getCity() + ", " : "")
				+ location.getCountry_code() + ")");
		event.setProperty("c8y_Position", c8y_Position);
		event.setProperty("ip", ip);

		// Create the event in the platform
		platform.getEventApi().create(event);

		return event;
	}

	
	/* * * * * * * * * * Application endpoints * * * * * * * * * */

	// Check the microservice status/health (implemented by default)
	// GET /health

	// Greeting endpoints
	@RequestMapping("hello")
	public String greeting (@RequestParam(value = "name", defaultValue = "World") String you) {
		return "Hello " + you + "!";
	}

	@RequestMapping("/")
	public String root () {
		return greeting("World");
	}

	// Return the environment values
	@RequestMapping("environment")
	public Map<String, String> environment () {
		return c8yEnv;
	}

	// Track the client's approximate location
	@RequestMapping(value = "location/track", produces="application/json")
	public String trackLocation (HttpServletRequest request) {
		// Get the public IP address and create the event
		return createLocationUpdateEvent(request.getHeader("x-real-ip")).toJSON();
	}

	// Get the tracked IPs and locations
	@RequestMapping("location/locations")
	public ArrayList<Object> getLocations (@RequestParam(value = "max", defaultValue = "5") int max) {
		var filter = new EventFilter().byType("c8y_LocationUpdate");
		var locations = new ArrayList<Object>();
		var eventCollection = platform.getEventApi().getEventsByFilter(filter).get(max);

		eventCollection.getEvents().forEach((event) -> {
			var map = new HashMap<String, Object>();

			map.put("ip", event.getProperty("ip"));
			map.put("coordinates", event.getProperty("c8y_Position"));
			map.put("when", event.getCreationDateTime().toString("yyyy-MM-dd hh:mm:ss"));

			locations.add(map);
		});

		return locations;
	}
}
