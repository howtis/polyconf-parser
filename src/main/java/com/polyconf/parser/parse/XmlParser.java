package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ValueType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public final class XmlParser implements LenientParser {

    @Override
    public ConfigSection parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        String text = String.join("\n", lines);
        if (text.isBlank()) {
            return new ConfigSection("", null, "");
        }

        Document doc;
        try {
            doc = parseLenient(text);
        } catch (Exception e) {
            return new ConfigSection("", null, "");
        }

        Map<String, ConfigNode> children = new LinkedHashMap<>();
        Element root = doc.getDocumentElement();
        children.put(root.getTagName(), convertElement(root));

        return new ConfigSection("", children, null, "");
    }

    private Document parseLenient(String text) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        return builder.parse(new InputSource(new StringReader(text)));
    }

    private ConfigNode convertElement(Element element) {
        Map<String, ConfigNode> children = new LinkedHashMap<>();

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String key = "@" + attr.getNodeName();
            children.put(key, new ConfigValue(
                    key,
                    attr.getNodeValue(),
                    ValueType.STRING,
                    null,
                    ""
            ));
        }

        NodeList childNodes = element.getChildNodes();
        Map<String, List<Element>> grouped = new LinkedHashMap<>();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) child;
                grouped.computeIfAbsent(childElem.getTagName(), k -> new ArrayList<>()).add(childElem);
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent().strip();
                if (!text.isEmpty()) {
                    children.put("#text", new ConfigValue(
                            "#text",
                            text,
                            ValueType.STRING,
                            null,
                            ""
                    ));
                }
            }
        }

        for (Map.Entry<String, List<Element>> entry : grouped.entrySet()) {
            String tagName = entry.getKey();
            List<Element> elements = entry.getValue();
            if (elements.size() == 1) {
                children.put(tagName, convertElement(elements.get(0)));
            } else {
                List<ConfigNode> items = new ArrayList<>();
                for (Element el : elements) {
                    items.add(convertElement(el));
                }
                children.put(tagName, new ConfigList(tagName, items, null, ""));
            }
        }

        return new ConfigSection(element.getTagName(), children, null, "");
    }
}
