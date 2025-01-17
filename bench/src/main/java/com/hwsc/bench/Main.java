package com.hwsc.bench;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.ingbyr.hwsc.common.Dataset;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    @Parameter(names = {"-type", "-t"})
    String type;

    @Parameter(names = {"-bench", "-b"})
    int bench;

    @Parameter(names = {"-beamWidth", "-bm"})
    int beamWidth;

    @Parameter(names = {"-dataset", "-d"})
    String datasetName;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();
    }

    public void run() {
        try {
            Dataset dataset = Dataset.valueOf(datasetName);
            switch (type) {
                case "hwsc":
                    SearchSpace.findByHWSC(dataset, bench);
                    break;
                case "tpg":
                    TaggedPlanGraph.find(dataset, beamWidth, bench);
                    break;
                case "pf":
                    ParetoFront.find(dataset);
                    break;
                default:
                    displayHelpInfo();
            }
        } catch (Exception e) {
            e.printStackTrace();
            displayHelpInfo();
        }
    }

    private static void displayHelpInfo() {
        log.info("[-type, -t] hwsc (HWSC)");
        log.info("[-type, -t] tpg (Tagged qos plan graph with beam)");
        log.info("[-type, -t] pf (Pareto front)");
        log.info("[-bench, -b]: int (Bench size)");
        log.info("[-dataset, -d] dataset (" + Arrays.stream(Dataset.values())
                .map(Enum::name)
                .collect(Collectors.joining(",")) + ")");
        log.info("[-beamWidth, -bw] int (Beam width)");
    }
}
