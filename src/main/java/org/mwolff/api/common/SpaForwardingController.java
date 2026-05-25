package org.mwolff.api.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

  private static final String NOT_RESERVED = "^(?!api$|actuator$|assets$)[\\w\\-]+";

  @GetMapping(
      value = {
        "/",
        "/{x:[\\w\\-]+}",
        "/{x:" + NOT_RESERVED + "}/{y:[\\w\\-]+}",
        "/{x:" + NOT_RESERVED + "}/{y:[\\w\\-]+}/{z:[\\w\\-]+}"
      })
  public String forward() {
    return "forward:/index.html";
  }
}
