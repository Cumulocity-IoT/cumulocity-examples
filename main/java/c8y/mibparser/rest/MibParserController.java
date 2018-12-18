package c8y.mibparser.rest;

import c8y.mibparser.service.MibParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static c8y.mibparser.constants.PlaceHolders.REQUEST_PARAM_NAME;

@RestController
@RequestMapping(value = "/mibparser")
@Slf4j
public class MibParserController {

    @Autowired
    private MibParserService mibParserService;

    @RequestMapping(value = "/health",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String get() {
        return "{\"status\":\"UP\"";
    }

    @RequestMapping(value = "/uploadzip",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String mibZipUpload(@RequestParam(REQUEST_PARAM_NAME) final MultipartFile file) throws Exception {
        return mibParserService.processMibZipFile(file);
    }
}
