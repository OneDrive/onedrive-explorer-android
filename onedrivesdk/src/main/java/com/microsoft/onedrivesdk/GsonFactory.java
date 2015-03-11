package com.microsoft.onedrivesdk;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Produce Gson instances that can parse OneDrive responses
 */
final class GsonFactory {

    /**
     * Default Constructor
     */
    private GsonFactory() {
    }

    /**
     * Creates an instance of Gson
     * @return The new instance
     */
    public static Gson getGsonInstance() {

        final JsonSerializer<Date> dateJsonSerializer = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(final Date src,
                                         final Type typeOfSrc,
                                         final JsonSerializationContext context) {
                if (src == null) {
                    return null;
                }
                return new JsonPrimitive(DateFormat.getDateInstance(DateFormat.FULL).format(src));
            }
        };

        final JsonDeserializer<Date> dateJsonDeserializer = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(final JsonElement json,
                                    final Type typeOfT,
                                    final JsonDeserializationContext context) throws JsonParseException {
                if (json == null) {
                    return null;
                }
                try {
                    return DateFormat.getDateInstance().parse(json.getAsString());
                } catch (final ParseException e) {
                    Log.e("JsonDeserializerDate", "Parsing issue on " + json.getAsString() + " ! " + e.toString());
                    return null;
                }
            }
        };

        return new GsonBuilder()
                .registerTypeAdapter(Date.class, dateJsonSerializer)
                .registerTypeAdapter(Date.class, dateJsonDeserializer)
                .create();
    }
}
