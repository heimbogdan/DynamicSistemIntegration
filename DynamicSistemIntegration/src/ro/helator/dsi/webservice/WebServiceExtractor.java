package ro.helator.dsi.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xerces.dom.ElementNSImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import ro.helator.dsi.webservice.jaxb.ObjectFactory;
import ro.helator.dsi.webservice.jaxb.TDefinitions;
import ro.helator.dsi.webservice.jaxb.TDocumentation;
import ro.helator.dsi.webservice.jaxb.TDocumented;
import ro.helator.dsi.webservice.jaxb.TTypes;

public class WebServiceExtractor {

	private static final String TEMP_FOLDER = "Services/temp/";
	private static final String WS_FOLDER = "Services/WebServices/";
	private static final String WSDL_EXTENSION = ".wsdl";
	private static final String XSD_EXTENSION = ".xsd";
	private static final String FW_SLASH = "/";
	private static final String UNDER_SCORE = "_";

	private static JAXBContext ctx;

	static {
		try {
			ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(),
					ObjectFactory.class.getClassLoader());
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public static File getWSDLFromURL(String urlPath, String serviceName) {

		urlPath = checkURLFormat(urlPath);
		serviceName = checkServiceName(serviceName, urlPath);

		try {
			URL wsdlUrl = new URL(urlPath);
			BufferedReader br = new BufferedReader(new InputStreamReader(wsdlUrl.openStream()));

			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			File wsdlFile = null;
			if (sb.length() > 0) {
				wsdlFile = new File(TEMP_FOLDER + serviceName + WSDL_EXTENSION);
				if (checkFileAndPath(wsdlFile)) {
					FileWriter fwr = new FileWriter(wsdlFile);
					fwr.write(sb.toString());
					fwr.close();
				}
			}
			return wsdlFile;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean checkFileAndPath(File file) throws IOException {
		return file.getParentFile().exists() ? true : file.getParentFile().mkdirs() && file.createNewFile();
	}

	private static String checkURLFormat(String urlPath) {
		return urlPath.endsWith("?wsdl") || urlPath.endsWith("?WSDL") ? urlPath : urlPath + "?wsdl";
	}

	private static String checkServiceName(String serviceName, String urlPath) {
		if (serviceName != null && !serviceName.isEmpty()) {
			return serviceName;
		}

		int fromIndex = urlPath.lastIndexOf(FW_SLASH) + 1;
		int toIndex = urlPath.lastIndexOf("?");
		String lastPath = urlPath.substring(fromIndex, toIndex);
		return lastPath.split("\\.")[0];
	}

	public static String convertDocumentToXMLString(Document doc) throws TransformerException {
		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		return writer.toString();
	}

	@SuppressWarnings("unchecked")
	public static void extractXSDfromWSDL(File wsdl) throws JAXBException, IOException, TransformerException {

		String serviceName = wsdl.getName().replace(WSDL_EXTENSION, "");

		TDefinitions obj = ((JAXBElement<TDefinitions>) ctx.createUnmarshaller().unmarshal(new FileReader(wsdl)))
				.getValue();

		List<TDocumented> list = obj.getAnyTopLevelOptionalElement();

		for (TDocumented o : list) {
			if (o instanceof TTypes) {
				getElementsFromTTypes((TTypes) o, serviceName);
			}
		}

		StringBuilder newPath = new StringBuilder(WS_FOLDER).append(serviceName).append(FW_SLASH).append(serviceName)
				.append(WSDL_EXTENSION);

		StringWriter sw = new StringWriter();

		ctx.createMarshaller().marshal(new ObjectFactory().createDefinitions(obj), sw);

		File newWsdl = new File(newPath.toString());
		if (checkFileAndPath(newWsdl)) {
			FileWriter fwr = new FileWriter(newWsdl);
			fwr.write(sw.toString());
			fwr.close();
		}

	}

	private static void getElementsFromTTypes(TTypes types, String serviceName)
			throws IOException, TransformerException {
		List<Object> anyList = types.getAny();
		int index = 1;
		for (Object any : anyList) {
			ElementNSImpl elem = (ElementNSImpl) any;
			if (elem.getLocalName().equals("schema")) {
				if (isLocalDefined(elem)) {
					StringBuilder filePath = new StringBuilder(WS_FOLDER).append(serviceName).append(FW_SLASH)
							.append(serviceName).append(UNDER_SCORE).append(index).append(XSD_EXTENSION);
					File file = new File(filePath.toString());
					Document doc = elem.getOwnerDocument();
					if (checkFileAndPath(file)) {
						FileWriter fwr = new FileWriter(file);
						fwr.write(WebServiceExtractor.convertDocumentToXMLString(doc));
						fwr.close();
						index++;
						// elem.setAttribute("schemaLocation", file.getName());
						
						clearNodeList(elem);
						addSchemaImport(file, elem);
					}
				}
			}
		}
	}

	private static boolean isLocalDefined(ElementNSImpl elem) {
		return elem.getAttributes().getNamedItem("schemaLocation") == null;
	}

	private static void clearNodeList(ElementNSImpl elem) {
		NodeList list = elem.getChildNodes();
		while (list.getLength() > 0) {
			elem.removeChild(list.item(0));
		}
	}

	private static void addSchemaImport(File file, ElementNSImpl elem) {
		ElementNSImpl schemaImp = (ElementNSImpl) elem.getOwnerDocument().createElementNS(elem.getNamespaceURI(), 
				elem.getNodeName().split(":")[0] + ":include");
		schemaImp.setAttribute("schemaLocation", file.getName());
		elem.appendChild(schemaImp);
	}
}
