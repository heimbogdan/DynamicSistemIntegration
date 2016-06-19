package ro.helator.dsi;

import java.io.File;

import ro.helator.dsi.webservice.WebServiceExtractor;

public class DynamicSistemIntegration {

	public static void main(String[] args) {
		File wsdl = WebServiceExtractor
				.getWSDLFromURL("http://www.webservicex.net/geoipservice.asmx", "geoipservice");
		
	}
}
