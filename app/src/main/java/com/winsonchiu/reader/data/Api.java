package com.winsonchiu.reader.data;

import com.winsonchiu.reader.data.api.ApiUtility;
import com.winsonchiu.reader.data.retrofit.ConverterFactory;

import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public class Api {

    private static Api api;

    private ApiUtility apiUtility;

    public static Api getInstance() {
        if (api == null) {
            api = new Api();
        }

        return api;
    }

    private Api() {
        apiUtility = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(new ConverterFactory())
                .build()
                .create(ApiUtility.class);
    }

    public ApiUtility getApiUtility() {
        return apiUtility;
    }
}
