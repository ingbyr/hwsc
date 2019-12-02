package com.ingbyr.hwsc.planner.innerplanner.cpg.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ingbyr.hwsc.common.models.Concept;
import com.ingbyr.hwsc.common.models.Service;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ing
 */
@Slf4j
public class CompletePlaningGraph {

    @Setter
    private PlanningGraph planningGraph;

    @Getter
    private Graph<DWGNode, DWGEdge> dwGraph;

    private Queue<DWGNode> nodeQueue;

    private List<Map<String, Set<LeveledService>>> levelCache;

    private DWGNode startNode;

    private DWGNode targetNode;

    private Set<DWGNode> newPreNodes;

    private int level = 0;

    @Setter
    @Getter
    private boolean reverseGraph;

    @Setter
    private Map<String, Double> conceptDistance;

    // debug msg header
    private static final String LINE_INIT = "========================== init ==========================";
    private static final String LINE_LEVEL = "======================== level {} ========================";
    private static final String LINE_NODE = "------------------------ node {} ------------------------";


    @AllArgsConstructor
    @ToString
    private static class NodeServiceHolder {

        Set<LeveledService> selected;

        Set<LeveledService> remaining;

    }

    public CompletePlaningGraph() {
        this.dwGraph = new SimpleDirectedWeightedGraph<>(DWGEdge.class);
        this.nodeQueue = Lists.newLinkedList();
        this.levelCache = Lists.newLinkedList();
        this.reverseGraph = false;
    }

    public DWGNode getStartNode() {
        if (reverseGraph)
            return targetNode;
        else
            return startNode;
    }

    public DWGNode getTargetNode() {
        if (reverseGraph)
            return startNode;
        else
            return targetNode;
    }

    public Graph<DWGNode, DWGEdge> build(PlanningGraph pg) {
        this.planningGraph = pg;
        initData();
        expandGraph();
        return dwGraph;
    }

    private void initData() {
        debug(LINE_INIT);

        info("create start node");
        String startService = planningGraph.start.get(0).getName();
        LeveledService startLeveledService = new LeveledService(startService, level);
        Set<Concept> inputConcepts = new ArrayList<>(planningGraph.propLevels).get(0);
        startLeveledService.setOutputConceptSet(DatasetCache.toStrSet(inputConcepts));

        startNode = DWGNode.builder()
                .services(Collections.singleton(startLeveledService))
                .inputConcepts(Sets.newHashSet())
                .outputConcepts(startLeveledService.getOutputConceptSet())
                .build();
        info("input concepts: {}", startNode.outputConcepts);
        // add node estimated distance
        startNode.setDistance(0.0);

        info("create input concept cache");
        Map<String, Set<LeveledService>> startLevelCache = new HashMap<>();
        inputConcepts.forEach(concept -> {
            cacheConcept(startLevelCache, concept.getName(), startLeveledService);
        });
        levelCache.add(startLevelCache);

        info("create action levels concept cache");
        for (LinkedHashSet<Service> action : planningGraph.actionLevels) {
            level++;
            // get pre level cache
            Map<String, Set<LeveledService>> levelTmpCache = copyCache(level - 1);
            action.forEach(service -> {
                Set<Concept> outputConcepts = service.getOutputConceptSet();
                // cache service output concept
                for (Concept concept : outputConcepts) {
                    cacheConcept(levelTmpCache, concept.getName(), new LeveledService(service.getName(), level));
                }
            });
            levelCache.add(levelTmpCache);
        }

        info("create target node");
        int targetLevel = level + 1;
        Service targetService = planningGraph.target.get(0);
        LeveledService targetLeveledService = new LeveledService(targetService.getName(), targetLevel);
        Set<Concept> requiredConcepts = new HashSet<>(planningGraph.getGoalSet());
        targetLeveledService.setInputConceptSet(DatasetCache.toStrSet(requiredConcepts));

        targetNode = DWGNode.builder()
                .services(Collections.singleton(targetLeveledService))
                .inputConcepts(targetLeveledService.getInputConceptSet())
                .outputConcepts(Sets.newHashSet())
                .build();
        info("target concepts: {}", targetNode.inputConcepts);

        if (reverseGraph) {
            targetNode.setAStarConcepts(Sets.newHashSet(targetNode.getInputConcepts()));

            double distance = 0;
            for (String concept : targetNode.getAStarConcepts()) {
                distance = Math.max(conceptDistance.get(concept), distance);
            }
            targetNode.setDistance(distance);
        }

        info("add target node to node queue");
        nodeQueue.offer(targetNode);
        dwGraph.addVertex(targetNode);

        // display cache
        for (int i = 0; i < levelCache.size(); i++) {
            debug("level {} cache: {}", i, levelCache.get(i));
        }
    }

    private Map<String, Set<LeveledService>> copyCache(int selectedLevel) {
        Map<String, Set<LeveledService>> cacheDeepCopy = new HashMap<>();
        if (selectedLevel >= 0) {
            levelCache.get(selectedLevel).forEach((k, v) -> cacheDeepCopy.put(k, Sets.newHashSet(v)));
        }
        return cacheDeepCopy;
    }

    private void cacheConcept(Map<String, Set<LeveledService>> cache, String concept, LeveledService leveledService) {
        if (cache.containsKey(concept)) {
            cache.get(concept).add(leveledService);
        } else {
            Set<LeveledService> newLeveledServiceSet = new LinkedHashSet<>();
            newLeveledServiceSet.add(leveledService);
            cache.put(concept, newLeveledServiceSet);
        }
    }

    /**
     * repeat to add pre-nodes for every node in this level
     */
    private void expandGraph() {

        for (; level >= 0; level--) {
            info(LINE_LEVEL, level);
            newPreNodes = new LinkedHashSet<>();
            debug("concept cache: {}", levelCache.get(level));
            while (!nodeQueue.isEmpty()) {
                DWGNode node = nodeQueue.poll();
                createPreNodesForNode(node);
            }
            info("add {} new pre nodes", newPreNodes.size());
            newPreNodes.forEach(nodeQueue::offer);
        }
    }

    private void createPreNodesForNode(DWGNode node) {

        debug(LINE_NODE, node);
        debug("required concepts: {}", node.inputConcepts);

        NodeServiceHolder nodeServiceHolder = selectNodeServices(node.services);

        // when no available service combinations
        if (nodeServiceHolder.selected.size() == 0) {
            // copy current node and set cost equal 0
            debug("copy node {} as pre-node", node);
            DWGNode preNode = DWGNode.from(node);
            addPreNode(node, preNode, 0.0, null);
            return;
        }

        debug("selected services: {}", nodeServiceHolder.selected);

        Set<String> conceptsOfSelectedServices = mergeInputConcepts(nodeServiceHolder.selected);

        // find all available services that removed services required
        List<List<LeveledService>> availableServices = new LinkedList<>();
        int combinationSize = 1;
        for (String concept : conceptsOfSelectedServices) {
            debug("{}: {}", concept, levelCache.get(level).get(concept));
            availableServices.add(Lists.newArrayList(levelCache.get(level).get(concept)));
            combinationSize *= availableServices.size();
            if (combinationSize <= 0) {
                throw new RuntimeException("Too big size of service combination");
            }
        }

        // find service combinations
        debug("require services size {}", availableServices.size());

        Set<Set<LeveledService>> serviceCombinations = combineService(availableServices);

        debug("service combinations size: {}", serviceCombinations.size());

        double cost = calcCost(nodeServiceHolder.selected);
        serviceCombinations.forEach(serviceCombination -> {
            Set<LeveledService> services = Sets.union(serviceCombination, nodeServiceHolder.remaining);
            DWGNode preNode = DWGNode.from(services);
            addPreNode(node, preNode, cost, nodeServiceHolder.selected);
        });
    }

    /**
     * add new before node
     *
     * @param node     node
     * @param preNode  pre node
     * @param cost     cost
     * @param services selected services
     */
    private void addPreNode(DWGNode node, DWGNode preNode, double cost, Set<LeveledService> services) {

        // record distance for a* alg
        updateEstimatedDistance(preNode, node);

        // create pre node
        dwGraph.addVertex(preNode);

        // add edge
        DWGEdge edge;
        if (reverseGraph) {
            edge = dwGraph.addEdge(node, preNode);
        } else {
            edge = dwGraph.addEdge(preNode, node);
        }

        // if edge is null which means the edge was already existed
        if (edge != null) {
            edge.setServices(services);
            dwGraph.setEdgeWeight(edge, cost);
            newPreNodes.add(preNode);
            debug("add node: {}", preNode);
        }
    }

    private void updateEstimatedDistance(DWGNode preNode, DWGNode node) {

        if (reverseGraph) {

            Set<String> preNodeInputConcepts = preNode.getInputConcepts();
            Set<String> preNodeOutputConcepts = preNode.getOutputConcepts();

            Set<String> additionalAStarConcepts = Sets.difference(node.getAStarConcepts(), Sets.intersection(node.getAStarConcepts(), preNodeOutputConcepts));

            Set<String> aStarConcepts = Sets.union(preNodeInputConcepts, additionalAStarConcepts);
            preNode.setAStarConcepts(aStarConcepts);

            double distance = 0;
            for (String concept : aStarConcepts) {
                double d = conceptDistance.get(concept);
                distance = Math.max(d, distance);
            }

            preNode.setDistance(distance);
        }

    }


    public static Set<String> mergeInputConcepts(Set<LeveledService> services) {
        return services.stream()
                .map(LeveledService::getInputConceptSet)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public static Set<String> mergeOutputConcepts(Set<LeveledService> services) {
        return services.stream()
                .map(LeveledService::getOutputConceptSet)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Generate all available service combinations
     *
     * @param services service list
     * @return new service combinations
     */
    private Set<Set<LeveledService>> combineService(List<List<LeveledService>> services) {
        Set<Set<LeveledService>> serviceCombinationResult = new LinkedHashSet<>();
        List<LeveledService> serviceCombinationStepResult = new LinkedList<>();

        combineServiceHelper(services, 0, serviceCombinationResult, serviceCombinationStepResult);

        return serviceCombinationResult;
    }

    private void combineServiceHelper(List<List<LeveledService>> services,
                                      int depth,
                                      Set<Set<LeveledService>> serviceCombinationResult,
                                      List<LeveledService> serviceCombinationStepResult) {

        if (services.size() == 0) return;

        for (int i = 0; i < services.get(depth).size(); i++) {
            LeveledService leveledService = services.get(depth).get(i);
            try {
                serviceCombinationStepResult.set(depth, leveledService);
            } catch (IndexOutOfBoundsException e) {
                serviceCombinationStepResult.add(leveledService);
            }

            if (depth == services.size() - 1) {
                // create new one because that data will be reset in next search
                serviceCombinationResult.add(Sets.newLinkedHashSet(serviceCombinationStepResult));
            } else {
                combineServiceHelper(services, depth + 1, serviceCombinationResult, serviceCombinationStepResult);
            }
        }
    }

    private NodeServiceHolder selectNodeServices(Set<LeveledService> nodeLeveledService) {

        Set<LeveledService> removedServices = Sets.newLinkedHashSet();
        Set<LeveledService> remainingServices = Sets.newLinkedHashSet();

        for (LeveledService leveledService : nodeLeveledService) {
            if (leveledService.getLevel() > level) {
                removedServices.add(leveledService);
            } else {
                remainingServices.add(leveledService);
            }
        }

        return new NodeServiceHolder(removedServices, remainingServices);
    }

    private double calcCost(Set<LeveledService> leveledServices) {
        double cost = 0.0;
        for (LeveledService leveledService : leveledServices) {
            cost += leveledService.getCost();
        }
        return cost;
    }

    private void debug(String fmt, Object... objs) {
        log.debug("[level-" + level + "] " + fmt, objs);
    }

    private void info(String fmt, Object... objs) {
        log.info("[level-" + level + "] " + fmt, objs);
    }

}

