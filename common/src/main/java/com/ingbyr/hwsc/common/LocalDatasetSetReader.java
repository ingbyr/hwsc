package com.ingbyr.hwsc.common;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;

import java.io.File;
import java.util.Map;

@Slf4j
public abstract class LocalDatasetSetReader extends AbstractDataSetReader {
    /**
     * Parse taxonomy file
     *
     * @return Concept map
     * @throws DocumentException
     */
    protected abstract Map<String, Concept> parseTaxonomyDocument() throws DocumentException;

    /**
     * Parse services file
     *
     * @return Service map
     * @throws DocumentException
     */
    protected abstract Map<String, Service> parseServicesDocument() throws DocumentException;

    /**
     * Parse problem or challenge file
     *
     * @return Problem
     * @throws DocumentException
     */
    protected abstract void parseProblemDocument() throws DocumentException;

    public void process() {
        try {
            parseTaxonomyDocument();
            parseServicesDocument();
            parseProblemDocument();
            log.debug("Dataset {}", dataset);
            log.debug("Input set size {}: {}",getInputSet().size(), getInputSet());
            log.debug("Goal set size {}: {}", getGoalSet().size(), getGoalSet());
            log.debug("Service size: {}", getServiceMap().size());
            log.debug("Concept size: {}", getConceptMap().size());
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set concept's parents
     */
    protected void buildConceptIndex() {
        for (Map.Entry<String, Concept> entry : conceptMap.entrySet()) {

            Concept concept = entry.getValue();
            if (concept.isRoot()) {
                concept.addParentConcept(concept);
                continue;
            }

            // Add parent concepts
            for (Concept c = concept; c != null; c = conceptMap.get(c.getDirectParentName())) {
                concept.addParentConcept(c);
            }

        }
    }

    /**
     * Set used and produced by service cache
     */
    protected void buildServiceIndex() {
        serviceMap.forEach((name, service) -> {
            for (Concept concept : service.getInputConceptSet()) {
                concept.addUsedByService(service);
            }

            for (Concept concept : service.getOutputConceptSet()) {
                concept.addProducedByService(service);
            }
        });
    }

    protected File loadFile(String filePath) {
        File file = new File(filePath);
        log.debug("Load file {}", file.getAbsolutePath());
        return file;
    }
}
