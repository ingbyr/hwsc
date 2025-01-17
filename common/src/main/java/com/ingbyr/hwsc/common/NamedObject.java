package com.ingbyr.hwsc.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * The class represents an object with a unique name
 */
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public abstract class NamedObject{

    protected String name;

    @Override
    public String toString() {
        return name;
    }

}
