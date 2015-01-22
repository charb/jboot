package jboot.loader.boot.node;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import jboot.loader.boot.model.Model;

public class DefaultModelLoader implements IModelLoader {
	private Unmarshaller unmarshaller;
	private XMLInputFactory xmlInputFactory;
	private StreamFilter streamFilter;

	public DefaultModelLoader() throws JAXBException {
		JAXBContext ctx = JAXBContext.newInstance(Model.class);
		unmarshaller = ctx.createUnmarshaller();

		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false); // this is needed in order for the StreamFilter to receive entity_reference events.

		// this filters out all entity references (other than the defaults &amp; etc). This was added in order to workaround parse errors due to undeclared entity references (such as &oslash;) in some poms.
		streamFilter = new StreamFilter() {
			@Override
			public boolean accept(XMLStreamReader reader) {
				if (reader.getEventType() == XMLStreamReader.ENTITY_REFERENCE) {
					return false;
				}
				return true;
			}
		};
	}

	@SuppressWarnings("unchecked")
	public Model load(File filePath) throws Exception {
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		Model model = null;
		try {
			fis = new FileInputStream(filePath);
			bis = new BufferedInputStream(fis);
			XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(bis);
			xmlStreamReader = xmlInputFactory.createFilteredReader(xmlStreamReader, streamFilter);
			JAXBElement<Model> jaxbElement = (JAXBElement<Model>) unmarshaller.unmarshal(xmlStreamReader);
			model = jaxbElement.getValue();
		} finally {
			if (bis != null) { 
				bis.close();
			}
			if (fis != null) {
				fis.close();
			}
		}
		return model;
	}
}
