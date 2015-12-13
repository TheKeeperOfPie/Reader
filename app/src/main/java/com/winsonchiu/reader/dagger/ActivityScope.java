/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Created by TheKeeperOfPie on 12/13/2015.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityScope {
}
