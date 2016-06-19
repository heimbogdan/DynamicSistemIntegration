package ro.helator.dsi.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class WebServiceExtractor {

	private static final String TEMP_FOLDER = "Services/temp/";
	
	public static File getWSDLFromURL(String urlPath, String serviceName) {

		urlPath = urlPath.endsWith("?wsdl") ? urlPath : urlPath + "?wsdl";
		
		try {
			URL wsdlUrl = new URL(urlPath);
			BufferedReader br = new BufferedReader(
					new InputStreamReader(wsdlUrl.openStream()));
			
			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = br.readLine()) != null){
				sb.append(line);
			}
			
			File wsdlFile = null;
			if(sb.length() > 0){
				wsdlFile = new File(TEMP_FOLDER + serviceName + ".wsdl");
				FileWriter fwr = new FileWriter(wsdlFile);
				fwr.write(sb.toString());
				fwr.close();
				
			}
			return wsdlFile;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
