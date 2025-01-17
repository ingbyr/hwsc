package com.ingbyr.hwsc.graphplan.qgp.models;

import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Set;

/**
 * @author ingbyr
 */
public class DWGEdge extends DefaultWeightedEdge {

    @Getter
    @Setter
    Set<LeveledService> services;

    @Override
    public DWGNode getSource() {
        return (DWGNode) super.getSource();
    }

    @Override
    public DWGNode getTarget() {
        return (DWGNode) super.getTarget();
    }

    @Override
    public double getWeight() {
        return super.getWeight();
    }
}
