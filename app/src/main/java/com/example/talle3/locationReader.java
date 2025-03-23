package com.example.talle3;

import android.content.Context;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class locationReader {
    private final Context context;
    public locationReader(Context context) {
        this.context = context;
    }

    public List<Ubicaciones> readLocations() throws IOException {
        List<Ubicaciones> locations = new ArrayList<>();

        InputStream inputStream = context.getAssets().open("locations.json");
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (key.equals("locationsArray")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    locations.add(readLocation(reader));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        return locations;
    }

    private Ubicaciones readLocation(JsonReader reader) throws IOException {
        double latitude = 0;
        double longitude = 0;
        String name = "";

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            switch (key) {
                case "latitude":
                    latitude = reader.nextDouble();
                    break;
                case "longitude":
                    longitude = reader.nextDouble();
                    break;
                case "name":
                    name = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Ubicaciones(latitude, longitude, name);
    }
}

