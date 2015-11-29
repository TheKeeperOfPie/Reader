/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit.Converter;

/**
 * Created by TheKeeperOfPie on 11/28/2015.
 */
public class ConverterFactory implements Converter.Factory {

//    @Override
//    public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
//        if (String.class.equals(type)) {
//            return new Converter<ResponseBody, String>() {
//                @Override
//                public String convert(ResponseBody value) throws IOException {
//                    return value.string();
//                }
//            };
//        }
//
//        return super.fromResponseBody(type, annotations);
//    }
//
//    @Override
//    public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
//        if (String.class.equals(type)) {
//            return new Converter<String, RequestBody>() {
//                @Override
//                public RequestBody convert(String value) throws IOException {
//                    return RequestBody.create(MediaType.parse("text/plain"), value);
//                }
//            };
//        }
//        return super.toRequestBody(type, annotations);
//    }

        @Override
    public Converter<?> get(Type type) {
        if (String.class.equals(type)) {
            return new Converter<String>() {
                @Override
                public String fromBody(ResponseBody body) throws IOException {
                    return body.string();
                }

                @Override
                public RequestBody toBody(String value) {
                    return RequestBody.create(MediaType.parse("text/plain"), value);
                }
            };
        }

        return null;
    }
}
