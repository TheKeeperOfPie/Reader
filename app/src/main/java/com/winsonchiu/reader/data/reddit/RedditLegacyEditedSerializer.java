/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Created by TheKeeperOfPie on 8/14/2016.
 */
public class RedditLegacyEditedSerializer extends JsonSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        if (value.equals(0L)) {
            gen.writeString("false");
        } else if (value.equals(1L)){
            gen.writeString("true");
        } else {
            gen.writeString(String.valueOf(value));
        }
    }
}
