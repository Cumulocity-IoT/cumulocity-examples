## Cumulocity IoT SNMP MIB Parser ##

This module consists of [Cumulocity IoT][1] based java microservice. Cumulocity IoT SNMP MIB Parser microservice helps in converting a Managed Information Base (MIB) file to a JSON representation which is then used to create a device protocol. 

This microservice accepts a ZIP file which contains the top-level MIB file along with dependent MIB files and an index file named mib-index which contains the name of the top level MIB file. It exposes the REST endpoint `{{url}}/service/mibparser/mib/uploadzip`

For more information on the usage of this microservice, refer to [SNMP Agent][2] section of the Cumulocity IoT user guide.

### Build Prerequisites ###
Refer to the "**Build Prerequisites**" section of the [Readme][5] under the snmp folder.

### Building ###
Refer to the "**Building**" section of the [Readme][5] under the snmp folder.

### How to deploy? ###
#### Hosted deployment
* After successfully building, you can find `snmp-mib-parser-<version>.zip` file in the build `target` folder.
* Upload this zip file.
* Refer to [Managing Applications][3] section of the Cumulocity IoT user guide for details on how to upload microservice into Cumulocity.

#### External/legacy deployment (Execute as a Java process)

##### Installation
* You can find snmp-mib-parser-<version>-1.noarch.rpm RPM package in the build `target/rpm/c8y-mib-parser/RPMS/noarch/` folder.
* Install the snmp-mib-parser RPM package. 
  
  Not required if you want to directly run the snmp-mib-parser-<version>.jar file found in the build `target` folder. 
* Create `.mibparser` folder in user home directory i.e. `${user.home}/.mibparser`.
* Copy the `mibparer.properties` file from configuration folder to `.mibparser` folder.
* Update the properties, `C8Y.baseURL` to point to the Cumulocity instance and `server.port` to port on which this service should run.

  For e.g `C8Y.baseURL=https://developers.cumulocity.com` and `server.port=6690`
* Start the MIB Parser service using `java -jar <file-name>.jar`
    
##### Post Installation
* For External/Legacy type deployments, one more step of adding a tenant option for management tenant is required.
* This can be achieved by sending a `POST` request to the Cumulocity platform.
  
  POST `{{url}}/tenant/options`
  
  Body
  `{
      "category": "<microservice_name>",
      "key": "microservice.url",
      "value": "<Cumulocity Platform URL:microservice_port>"
  }`
  
  E.g.
  `{
       "category": "mibparser",
       "key": "microservice.url",
       "value": "https://developers.cumulocity.com:6690"
   }`


Once the deployment is complete, the MIB Parser microservice will be listed as an `Own application` under `Administration` tab. Now you should subscribe your tenant to this misroservice in order to use the service. More information on how to subscribe application for the given tenant using REST interface can be found at [Microservice SDK][4] section of the Cumulocity IoT user guide. Subscription can also be achieved using the user interface. `Administration` -> `Tenants` -> `Subtenants` -> `Select user` -> Click on `Application tab` -> Click `subscribe` on mouse hover `Mibparser` from the list of applications.
For more details, refer to [Managing Applications][3] section of the Cumulocity IoT user guide.

[1]: https://www.cumulocity.com/
[2]: https://cumulocity.com/guides/users-guide/optional-services/#snmp
[3]: https://cumulocity.com/guides/users-guide/administration/#managing-applications
[4]: https://cumulocity.com/guides/microservice-sdk/rest/
[5]: ../README.md

