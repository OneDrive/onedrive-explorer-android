package com.microsoft.onedriveaccess;

import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;

public abstract class GsonODConnection extends AbstractODConnection {

    @Override
    protected Converter getConverter() {
        return new GsonConverter(GsonFactory.getGsonInstance());
    }
}
