package com.ingbyr.hwsc.dataset;

import com.ingbyr.hwsc.common.models.Concept;
import com.ingbyr.hwsc.common.models.Param;
import com.ingbyr.hwsc.common.models.Service;
import com.ingbyr.hwsc.common.models.Thing;
import com.ingbyr.hwsc.dataset.util.XMLFileUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.util.*;

@Getter
@Slf4j
public class WSDLDataSetReader extends AbstractDataSetReader implements DataSetReader {

    private final String TAXONOMY_URL;
    private final String SERVICES_URL;
    private final String PROBLEM_URL;

    public WSDLDataSetReader(Dataset dataset) {
        this.TAXONOMY_URL = dataset.getPath() + "//Taxonomy.owl";
        this.SERVICES_URL = dataset.getPath() + "//Services.wsdl";
        this.PROBLEM_URL = dataset.getPath() + "//Challenge.wsdl";
    }

    @Override
    public Map<String, Concept> parseTaxonomyDocument() throws DocumentException {

        Element taxonomyRoot = XMLFileUtils.loadRootElement(loadFile(TAXONOMY_URL));

        /**
         * loop through semantic elements to check taxonomy
         */
        for (Iterator i = taxonomyRoot.elementIterator(); i.hasNext(); ) {
            Element el = (Element) i.next();
            if (el.getName().equals("Class")) {
                Concept concept = new Concept(el.attribute("ID").getText());//Get the concept name(ID)
                List<Element> list = el.elements();
                if (list.size() != 0) {
                    for (Element dummy : list) {
                        concept.setDirectParentName(dummy
                                .attribute("resource").getText()
                                .replaceAll("#", ""));//Get the direct parant class

                    }
                } else {
                    concept.setRoot(true);
                }
                conceptMap.put(concept.getName(), concept);

            } else if (el.getName().equals("Thing")) {
                Thing thing = new Thing(el.attribute("ID").getText());

                thing.setType(el.element("type").attribute("resource")
                        .getText().replaceAll("#", ""));//type = parant class
                thingMap.put(thing.getName(), thing);
            }
        }

        buildConceptIndex();
        return getConceptMap();
    }

    @Override
    public Map<String, Service> parseServicesDocument() throws DocumentException {

        Element servicesRoot = XMLFileUtils.loadRootElement(loadFile(SERVICES_URL));
        Element semRoot = servicesRoot.element("semExtension");

        /**
         * loop through semantic elements
         */
        Service service = null;

        for (Iterator i = semRoot.elementIterator(); i.hasNext(); ) {
            Element semMsgExtEl = (Element) i.next();
            if (semMsgExtEl.getName().equals("semMessageExt")) {
                boolean isRequestParam;
                if (semMsgExtEl.attribute("id").getText().contains(
                        "RequestMessage")) {
                    service = new Service(semMsgExtEl.attribute("id").getText()
                            .replaceAll("RequestMessage", ""));
                    isRequestParam = true;
                } else {
                    isRequestParam = false;
                }

                for (Iterator j = semMsgExtEl.elementIterator(); j.hasNext(); ) {
                    Element semExtEl = (Element) j.next();
                    if (semExtEl.getName().equals("semExt")) {
                        Param param = new Param(semExtEl.attribute("id")
                                .getText());
                        Thing thing = thingMap.get(semExtEl.element(
                                "ontologyRef").getText().replaceAll(
                                "http://www.ws-challenge.org/wsc08.owl#", ""));

                        param.setThing(thing);
                        paramMap.put(param.getName(), param);
                        if (isRequestParam) {
                            service.addInputParam(param);
                            service.addInputConcept(conceptMap.get(thing.getType()));
                        } else {
                            service.addOutputParam(param);
                            for (Concept c : conceptMap.get(thing.getType()).getParentConceptsIndex()) {
                                service.addOutputConcept(c);//outputs indexing
                            }
                        }
                    }
                }
                if (semMsgExtEl.attribute("id").getText().contains(
                        "ResponseMessage")) {
                    serviceMap.put(service.getName(), service);
                }

            }
        }
        return getServiceMap();
    }

    @Override
    public Pair<Set<Concept>, Set<Concept>> parseProblemDocument() throws DocumentException {

        Set<Concept> initPLevel = new HashSet<>();
        Set<Concept> goalSet = new HashSet<>();

        Element servicesRoot = XMLFileUtils.loadRootElement(loadFile(PROBLEM_URL));
        Element semRoot = servicesRoot.element("semExtension");

        for (Iterator i = semRoot.elementIterator(); i.hasNext(); ) {
            Element semMsgExtEl = (Element) i.next();
            if (semMsgExtEl.getName().equals("semMessageExt")) {
                boolean isRequestParam;
                if (semMsgExtEl.attribute("id").getText().contains(
                        "RequestMessage")) {
                    isRequestParam = true;
                } else {
                    isRequestParam = false;
                }

                for (Iterator j = semMsgExtEl.elementIterator(); j.hasNext(); ) {
                    Element semExtEl = (Element) j.next();
                    if (semExtEl.getName().equals("semExt")) {
                        Param param = new Param(semExtEl.attribute("id")
                                .getText());
                        Thing thing = thingMap.get(semExtEl.element(
                                "ontologyRef").getText().replaceAll(
                                "http://www.ws-challenge.org/wsc08.owl#", ""));

                        param.setThing(thing);
                        paramMap.put(param.getName(), param);
                        if (isRequestParam) {
                            initPLevel.addAll(conceptMap.get(thing.getType()).getParentConceptsIndex());
                        } else {
                            goalSet.add(conceptMap.get(thing.getType()));
                        }
                    }
                }
            }
        }
        problem = new ImmutablePair<>(initPLevel, goalSet);
        return getProblem();
    }

    @Override
    public Set<Concept> getInputSet() {
        return problem.getLeft();
    }

    @Override
    public Set<Concept> getGoalSet() {
        return problem.getRight();
    }
}