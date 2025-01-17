package com.ingbyr.dscg;

import com.ingbyr.hwsc.common.Dataset;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DscgConfig {

    public static final String EVALUATOR_CLASS_PREFIX = "com.ingbyr.dscg.";

    public static final String FITNESS_CLASS_PREFIX = "com.ingbyr.dscg.";

    public static final String PLANNER_CLASS_PREFIX = "com.ingbyr.dscg.planner.";

    protected Dataset dataset;

    protected int populationSize;

    protected int offspringSize;

    protected int survivalSize;

    protected double crossoverPossibility;

    protected double mutationPossibility;

    protected int mutationAddStateWeight;

    protected int mutationAddConceptWeight;

    protected int mutationDelStateWeight;

    protected int mutationDelConceptWeight;

    protected boolean enableAutoStop;

    protected int maxGen;

    protected int autoStopStep;

    // Mutation config
    protected int mutationAddStateRadius;

    protected double mutationAddConceptAddPossibility;

    protected double mutationAddConceptChangePossibility;

    // Evaluator config
    protected String evaluator;

    // Indicator config
    protected String fitness;

    protected int planMaxStep;

    protected int maxStateSize;

    protected String planner;

}
