/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class used to convert null JsonNodes into default values
 *
 * Created by TheKeeperOfPie on 8/1/2015.
 */
public class UtilsJson {

    public static String getString(JsonNode jsonNode) {
        return jsonNode == null ? "" : jsonNode.asText();
    }

    public static int getInt(JsonNode jsonNode) {
        return jsonNode == null ? 0 : jsonNode.asInt();
    }

    public static boolean getBoolean(JsonNode jsonNode) {
        return jsonNode != null && jsonNode.asBoolean();
    }

    public static long getLong(JsonNode jsonNode) {
        return jsonNode == null ? 0 : jsonNode.asLong();
    }

}
