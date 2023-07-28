# Introduction
This example microservice demonstrates how to create a subscription and consume a stream of notifications using WebSocket protocol. The example microservice first creates a subscription and then proceeds to connect to the WebSocket server after obtaining the authorization token.

The example microservice creates the following subscription to the specified device:

```json
{
  "source": { "id": "<DEVICE_ID>" },
  "context": "mo",
  "subscription": "<SUBSCRIPTION_NAME>",
  "subscriptionFilter": {
    "apis": ["measurements"],
    "typeFilter": "'c8y_Speed'"
  },
  "fragmentsToCopy": ["c8y_SpeedMeasurement", "c8y_MaxSpeedMeasurement"]
}
```

The example microservice first creates the above subscription and then uses it to obtain a token. This token is used to access the subscription's WebSocket channel. Next a WebSocket client is connected; the client will listen for notifications of messages that meet the subscription criteria as they are sent to Cumulocity IoT by the device.

In the above subscription example, we have expressed interest in receiving only measurements that bear the type of `'c8y_speed'`. The `fragmentsToCopy` property further transforms the filtered measurement to *only* include c8y_SpeedMeasurement and c8y_MaxSpeedMeasurement fragments.

As an example, if we post the following measurement from the specified device that meets our filter criteria above:




```json
{
  "c8y_SpeedMeasurement": {
    "T": {
      "value": 100,
      "unit": "km/h"
    }
  },
  "c8y_Speed2Measurement": {
    "T": {
      "value": 150,
      "unit": "km/h"
    }
  },
  "c8y_Speed3Measurement": {
    "T": {
      "value": 200,
      "unit": "km/h"
    }
  },
  "c8y_MaxSpeedMeasurement": {
    "T": {
      "value": 300,
      "unit": "km/h"
    }
  },
  "time":"2021-06-11T17:03:14.000+02:00",
  "source": {"id":"<DEVICE_ID>"},
  "type": "c8y_Speed"
}
```
we will receive the following message as we have specified to transform the filtered measurement to only include c8y_SpeedMeasurement and c8y_MaxSpeedMeasurement.

```json
{
  "c8y_SpeedMeasurement": {
    "T": {
      "value": 100,
      "unit": "km/h"
    }
  },
  "c8y_MaxSpeedMeasurement": {
    "T": {
      "value": 300,
      "unit": "km/h"
    }
  },
  "time":"2021-06-11T17:03:14.000+02:00",
  "source": {"id":"<DEVICE ID>"},
  "type": "c8y_Speed"
}
```


## Prerequisite
- Cumulocity IoT tenant with microservice hosting feature enabled
  > Important: The Microservice hosting feature must be activated on your tenant, otherwise your request will return an error message like “security/Forbidden, access is denied”. This feature is not assigned to tenants by default, so trial accounts won’t have it. Contact [product support](https://cumulocity.com/guides/welcome/contacting-support/) so that we can assist you with the activation. Note that this is a paid feature. Microservice SDK for Java - Cumulocity IoT Guides
- contents of this repository

### Instructions
1. Create a test user using the Cumulocity IoT Management UI
    - From the Application Switcher select Administration
    - In the Navigation pane expand Accounts and go to Users
    - Click on 'Add user', fill in the form for the new user, untick "Send password reset link as email" and provide a password
    - Click Save
    - Enable notification support:
        - Using Navigation pane, go to Accounts and then Roles, click on "Add global role"
        - Name the role "Notifications" and from the list of permissions tick "Notification 2" in Admin column
        - click Save
        - go to Accounts then Users
        - in the list, find the username created a few steps before and under "Global roles" open the dropdown
        - find and tick "Notifications" and click "Apply"
2. Creating a test device
    - open the demo directory
    - edit the `device.py` script and provide values for the following variables for your newly created user as well as the platform url:
      ```python
      _tenant_id = ''
      _username = ''
      _password = ''
      _platform_url = ''
        ```
    - create a device using `python3 device.py device` and make note of its id, example:
       ```console
       user@host:~$ python3 device.py device
       user@host:~$ Device id: 203
       ```
3. Build the microservice
    - edit the `application.properties` located in `src/main/resources`
        - replace the device id `203` in `example.source.id` with your device id
    - build the microservice using `mvn clean install`
        - the microservice will build under `target/` and will be named `hello-notification-<VERSION>.zip`
4. Deploy the microservice either
    - by uploading application to the platform:
        - back in your browser, in Administration dashboard, from Navigation expand Applications and select Own applications
        - click on 'Add application', 'Upload Microservice' and 'Upload file'
        - select the `zip` microservice file built in step 3
        - when prompted to subscribe select 'Don't subscribe'
        - switch to the test user, under Applications, Own applications select `Hello-notification` and subscribe
    - by running the application locally:
        - edit the `application.properties`
        - uncomment the line containing `C8Y.baseURL` entry and provide the cumulocity platform url
        - start the application
5. Start sending measurements
    - run the script again with the following parameters `python3 device.py send <DEVICE_ID> <DURATION_IN_SECONDS>`:
        ```console
        user@host:~$ python device.py send 203 10
        user@host:~$ Measurement created [201]
        user@host:~$ Measurement created [201]
                     ...
        ```
6. Observe the microservice log to see notifications 
   - in the browser, from Navigation expand Applications, Own applications, select 'Hello-notification' and go to Logs tab
7. (Optional) Unsubscribe the microservice
   - in the Properties tab of 'Hello-notification' application (Navigation, expand Applications, go to Own applications)
   - click 'Unsubscribe'
8. (Optional) Delete the device
    - run the script once more using `python3 device.py delete <DEVICE_ID>`, the output will look similar to:
      ```console
      user@host:~$ 
      user@host:~$ Device id: 203 deleted
      ```

