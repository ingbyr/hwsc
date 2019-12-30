package com.ingbyr.hwsc.planner;

import com.ingbyr.hwsc.common.*;
import com.ingbyr.hwsc.planner.exception.NotValidSolutionException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ingbyr
 */
@NoArgsConstructor
@Slf4j
public class PlannerAnalyzer {

    // Log
    List<List<Qos>> QosLog = new LinkedList<>();
    List<Qos> BestQosLog = new LinkedList<>();
    List<Double> GDLog = new LinkedList<>();
    List<Double> IGDLog = new LinkedList<>();

    @Getter
    private Instant startTime;

    @Getter
    private Instant endTime;

    @Getter
    private double runtime;

    @Getter
    @Setter
    private Dataset dataset;

    @Setter
    private PlannerIndicator indicator;

    @Setter
    private Fitness fitness;

    @Setter
    @Getter
    private List<Individual> lastPop;

    /**
     * Record every step pop info and return GD as indicator of pop
     *
     * @param pop Population
     * @return The GD of pop
     */
    public Double recordStepInfo(List<Individual> pop) {
        QosLog.add(pop.stream().map(Individual::getQos).collect(Collectors.toList()));

        log.debug("Population :");
        for (Individual individual : pop) {
            log.debug("{}", individual.toSimpleInfo());
        }

        if (fitness instanceof FitnessParetoFront) {
            double stepGD = indicator.GD(pop);
            log.debug("GD: {}", stepGD);
            GDLog.add(stepGD);
            // TODO disable in bench
//            double stepIGD = indicator.IGD(pop);
//            log.debug("IGD: {}", stepIGD);
//            IGDLog.add(stepIGD);

            return stepGD;
        } else {
            BestQosLog.add(pop.get(0).getQos());
            return (double) pop.get(0).getId();
        }
    }

    void recordStartTime() {
        startTime = Instant.now();
    }

    void recordEndTime() {
        endTime = Instant.now();
        runtime = Duration.between(startTime, endTime).toMillis() / 1000.0;
    }

    public void displayLogOnConsole() {
        log.info("Time used {} seconds", getRuntime());
        log.info("Last population:");
        for (Individual ind : lastPop) {
            log.info("{}", ind.toSimpleInfo());
        }

        if (fitness instanceof FitnessParetoFront) {
            log.info("GD: {}", GDLog);
            log.info("IGD: {}", IGDLog);
        } else {
            log.info("Best qos log: {}", BestQosLog);
        }
    }

    public static void checkSolution(Set<Concept> input, Set<Concept> goal, List<Service> services) throws NotValidSolutionException {
        if (services == null) {
            log.error("Service list is null");
            return;
        }

        Set<Concept> concepts = new HashSet<>(input);
        for (Service service : services) {
            if (!concepts.containsAll(service.getInputConceptSet()))
                throw new NotValidSolutionException("Service " + service + " can not proceed because that some input concepts not existed");
            concepts.addAll(service.getOutputConceptSet());
        }

        if (!concepts.containsAll(goal))
            throw new NotValidSolutionException("Some goals are not contained when finishing execution");

        log.debug("The solution is valid");
    }

    public void saveLog2File() {
        // TODO
    }
}
