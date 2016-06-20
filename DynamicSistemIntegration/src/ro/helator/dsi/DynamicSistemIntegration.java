package ro.helator.dsi;

import java.io.File;
import ro.helator.dsi.webservice.WebServiceExtractor;

public class DynamicSistemIntegration {

	public static void main(String[] args) throws Exception {
		File wsdl = WebServiceExtractor
				.getWSDLFromURL("http://www.webservicex.net/geoipservice.asmx", null);
		
		WebServiceExtractor.extractXSDfromWSDL(wsdl);
	}
}
