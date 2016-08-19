/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.components;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
public class ComponentStatic {

    public static final ComponentData componentData;
    public static ComponentStatic component;

    static {
        componentData = DaggerComponentData.builder().build();
        component = new ComponentStatic();
        componentData.inject(component);
    }

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JsonFactory jsonFactory;

    public ComponentStatic() {
        componentData.inject(this);
    }

    public static ComponentData getComponentData() {
        return componentData;
    }

    public static ObjectMapper getObjectMapper() {
        return component.objectMapper;
    }

    public static JsonFactory getJsonFactory() {
        return component.jsonFactory;
    }
}
