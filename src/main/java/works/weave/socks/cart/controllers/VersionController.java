package works.weave.socks.cart.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/")
public class VersionController {

   @Value("${version}")
   private String version;
   @Value("${delayInMillis}")
   private String delayInMillis;
   @Value("${promotionRate}")
   private String promotionRate;

  // limegreen for V1 and V2 90EE90
  // red for V2 DC143C

   private String template = "<html><head><style>html,body{width:100%%;padding:0;margin:0;background-color:#90EE90;}.centered-wrapper{position:relative;text-align:center;margin-top:20px}.centered-content{display:inline-block;vertical-align:top}</style></head><body class=\"centered-wrapper\"><div class=\"centered-content\">"+
      "<table style=\"font-family:sans-serif;font-size:16\" cellspacing=\"5\"><tbody>" +
      "<tr><td rowspan=\"9\" width=\"150\"><img src=\"https://raw.githubusercontent.com/keptn-sockshop/carts/master/cart.png\" width=\"150\"></td>"+
      "<td align=\"right\">Pod name:</td><td><strong>%s</strong></td></tr>"+ //name
      "<tr><td align=\"right\">Container image:</td><td><strong>%s</strong></td></tr>"+ //image
      "<tr><td align=\"right\">Deployment name:</td><td><strong>%s</strong></td></tr>"+ //deployment
      "<tr><td align=\"right\">keptn project:</td><td><strong>%s</strong></td></tr>"+ //keptnProject
      "<tr><td align=\"right\">keptn stage:</td><td><strong>%s</strong></td></tr>"+ //keptnStage
      "<tr><td align=\"right\">keptn service:</td><td><strong>%s</strong></td></tr>"+ //keptnService
      "<tr><td align=\"right\">Version:</td><td><strong>%s</strong></td></tr>"+ //version
      "<tr><td align=\"right\">Delay in ms:</td><td><strong>%s</strong></td></tr>"+ //delayInMillis
      "<tr><td align=\"right\">Promotion rate:</td><td><strong>%s</strong></td></tr>"+ //promotionRate
      "</tbody></table></div></body></html>";

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(method = RequestMethod.GET, path = "/")
   public @ResponseBody String getInformation() {
      String name = getEnvVarValueOrNotFoundMessage("POD_NAME");
      String deployment = getEnvVarValueOrNotFoundMessage("DEPLOYMENT_NAME");
      String image = getEnvVarValueOrNotFoundMessage("CONTAINER_IMAGE");

      String keptnProject = getEnvVarValueOrNotFoundMessage("KEPTN_PROJECT");
      String keptnStage = getEnvVarValueOrNotFoundMessage("KEPTN_STAGE");
      String keptnService = getEnvVarValueOrNotFoundMessage("KEPTN_SERVICE");

      if (version == null) {
         version = "No version found in application.properties";
      }
      if (delayInMillis == null) {
         delayInMillis = "No delayInMillis found in application.properties";
      }
      if (promotionRate == null) {
         promotionRate = "No promotionRate found in application.properties";
      }

      return String.format(template, name, image, deployment, keptnProject, keptnStage, keptnService, version, delayInMillis, promotionRate);
   }

   private String getEnvVarValueOrNotFoundMessage(String var) {
      String value = System.getenv(var);
      if (value == null) {
         value = "No value found in environment variable " + var;
      }
      return value;
   }

   @ResponseStatus(HttpStatus.OK)
   @RequestMapping(method = RequestMethod.GET, path = "/version")
   public @ResponseBody String getVersion() {
      return "Version = " + version;
   }

}
