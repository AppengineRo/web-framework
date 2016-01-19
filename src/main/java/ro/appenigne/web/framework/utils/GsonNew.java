package ro.appenigne.web.framework.utils;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GsonNew {
    public static Gson getGson() {
        return getGsonBuilder().create();
    }

    private static GsonBuilder getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls()
                .registerTypeAdapter(Object.class, new NaturalDeserializer())
                .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                    @Override
                    public Date
                    deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return new Date(json.getAsLong());
                    }
                })
                .registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
                    @Override
                    public JsonPrimitive serialize(Date arg0, Type arg1, JsonSerializationContext arg2) {
                        return new JsonPrimitive(arg0.getTime());
                    }
                })
                .registerTypeAdapter(Text.class, new JsonSerializer<Text>() {
                    @Override
                    public JsonPrimitive serialize(Text arg0, Type arg1, JsonSerializationContext arg2) {

                        return new JsonPrimitive(arg0.getValue());
                    }

                })
                .registerTypeAdapter(Text.class, new JsonDeserializer<Text>() {
                    @Override
                    public Text deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {

                        return new Text(json.getAsString());
                    }

                })
                .registerTypeAdapter(Entity.class, new JsonSerializer<Entity>() {
                    @Override
                    public JsonElement serialize(Entity entity, Type arg1, JsonSerializationContext arg2) {
						HashMap<String, Object> returnValue= new HashMap<>();
						for (Map.Entry<String, Object> stringObjectEntry : entity.getProperties().entrySet()) {
							if (stringObjectEntry.getKey().contains(".")) {
								//e belea
								String propName = stringObjectEntry.getKey().substring(0, stringObjectEntry.getKey().indexOf("."));
								String keyName = stringObjectEntry.getKey().substring(stringObjectEntry.getKey().indexOf(".") + 1);
								LinkedHashMap<String, Object> val = new LinkedHashMap<>();
								if (!returnValue.containsKey(propName)) {
									returnValue.put(propName, val);
								} else {
									if(returnValue.get(propName) instanceof LinkedHashMap){
										val = (LinkedHashMap<String, Object>) returnValue.get(propName);
									} else{
										val.put("_", returnValue.get(propName));
										returnValue.put(propName, val);
									}
								}
								val.put(keyName, stringObjectEntry.getValue());
							} else {
								if (returnValue.containsKey(stringObjectEntry.getKey())) {
									((LinkedHashMap)returnValue.get(stringObjectEntry.getKey())).put("_",stringObjectEntry.getValue());
								} else{
									returnValue.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
								}
							}
						}
						returnValue.put("_hash", KeyFactory.keyToString(entity.getKey()));
						return arg2.serialize(returnValue);
                    }

                })
                .registerTypeAdapter(Entity.class, new JsonDeserializer<Entity>() {
                    @Override
                    public Entity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                        Key key = null;
                        HashMap<String, Object> propertyMap = new HashMap<>();
                        for (Map.Entry<String, JsonElement> set : json.getAsJsonObject().entrySet()) {
                            if (set.getKey().equals("_hash")) {
                                key = KeyFactory.stringToKey(set.getValue().getAsString());//context.deserialize(set.getValue(), Key.class);
                            } else {
                                propertyMap.put(set.getKey(), context.deserialize(set.getValue(), JsonPrimitive.class));
                            }

                        }
                        Entity entity = new Entity(key);
                        for (Map.Entry<String, Object> prop : propertyMap.entrySet()) {
                            try {
                                entity.setProperty(prop.getKey(), prop.getValue());
                            } catch (IllegalArgumentException ignored) {

                            }
                        }
                        return entity;
                    }

                })
                .registerTypeAdapter(Key.class, new JsonSerializer<Key>() {
                    @Override
                    public JsonElement serialize(Key key, Type arg1, JsonSerializationContext arg2) {
                        if (key == null) {
                            return null;
                        }

                        return new JsonPrimitive(KeyFactory.keyToString(key));
                    }

                })
                .registerTypeAdapter(Key.class, new JsonDeserializer<Key>() {
                    @Override
                    public Key deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                        if(json==null){
                            return null;
                        }
                        return KeyFactory.stringToKey(json.getAsString());
                    }

                })
                .registerTypeAdapter(BlobKey.class, new JsonSerializer<BlobKey>() {
                    @Override
                    public JsonElement serialize(BlobKey key, Type arg1, JsonSerializationContext arg2) {
                        if (key == null) {
                            return null;
                        }
                        return new JsonPrimitive(key.getKeyString());
                    }

                })
                .registerTypeAdapter(BlobKey.class, new JsonDeserializer<BlobKey>() {
                    @Override
                    public BlobKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                        if(json==null){
                            return null;
                        }
                        return new BlobKey(json.getAsString());
                    }

                });
        return gsonBuilder;
    }

    public static Gson getGsonPrettyPrint() {
        return getGsonBuilder().setPrettyPrinting().create();
    }

    private static class NaturalDeserializer implements JsonDeserializer<Object> {
        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonNull()) {
                return null;
            } else if (json.isJsonPrimitive()) {
                return this.handlePrimitive(json.getAsJsonPrimitive());
            } else if (json.isJsonArray()) {
                return this.handleArray(json.getAsJsonArray(), context);
            } else {
                return this.handleObject(json.getAsJsonObject(), context);
            }
        }

        private Object handlePrimitive(JsonPrimitive json) {
            if (json.isBoolean()) {
                return json.getAsBoolean();
            } else if (json.isString()) {
                return json.getAsString();
            } else {
                BigDecimal bigDec = json.getAsBigDecimal();
                // Find out if it is an int type
                try {
                    bigDec.toBigIntegerExact();
                    try {
                        return bigDec.intValueExact();
                    } catch (ArithmeticException e) {
                    }
                    return bigDec.longValue();
                } catch (ArithmeticException e) {
                }
                // Just return it as a double
                return bigDec.doubleValue();
            }
        }

        private Object handleArray(JsonArray json, JsonDeserializationContext context) {
            Object[] array = new Object[json.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = context.deserialize(json.get(i), Object.class);
            }
            return array;
        }

        private Object handleObject(JsonObject json, JsonDeserializationContext context) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                map.put(entry.getKey(), context.deserialize(entry.getValue(), Object.class));
            }
            return map;
        }
    }
}
