package com.rd.myweather;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherFetcher {

    /** "2026-09-15" -> "Tue, 15 Sep" (falls back to the raw value). */
    @NonNull
    public static String date(@NonNull String iso) {
        return reformat(iso, "yyyy-MM-dd", "EEE, dd MMM");
    }

    private static String reformat(String value, String inPattern, String outPattern) {
        try {
            Date parsed = new SimpleDateFormat(inPattern, Locale.US).parse(value);
            if (parsed == null) {
                return value;
            }
            return new SimpleDateFormat(outPattern, Locale.getDefault()).format(parsed);
        } catch (ParseException e) {
            return value;
        }
    }

    public interface GeocodeCallback {
        void onCoordinatesFound(double lat, double lon, String cityName);
        void onError(Exception e);
    }

    public void getCoordinatesFromCity(String cityName, GeocodeCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // Encode the string safely to handle spaces (e.g., "New York")
                String encodedCity = URLEncoder.encode(cityName, "UTF-8");
                String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1&language=en&format=json";

                URL url = new URL(geoUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Geocoding failed");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the Geocoding API response structure
                JSONObject jsonResponse = new JSONObject(response.toString());

                if (!jsonResponse.has("results")) {
                    throw new Exception("City location not found.");
                }

                // Get the first result in the array matching the text string
                JSONObject topMatch = jsonResponse.getJSONArray("results").getJSONObject(0);
                double lat = topMatch.getDouble("latitude");
                double lon = topMatch.getDouble("longitude");
                String properName = topMatch.getString("name") + ", " + topMatch.getString("country_code");

                handler.post(() -> callback.onCoordinatesFound(lat, lon, properName));

            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }

    public interface WeatherCallback {
        void onSuccess(String temperature, String windSpeed, String humidity, String condition, int isDay, List<Daily> daily);
        void onError(Exception e);
    }

    public void fetchCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        // Create a background thread executor
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 1. Construct the URL directly via concatenation to avoid formatting bugs
                /*
                String urlString = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=" + latitude +
                        "&longitude=" + longitude +
                        "&current_weather=true";
               */
                // Enhanced query to include relative humidity and standard weather status codes
                String urlString = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=" + latitude +
                        "&longitude=" + longitude +
                        "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation,rain,showers,snowfall,weather_code,is_day" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,wind_speed_10m_max,wind_direction_10m_dominant,precipitation_probability_max" +
                        "&timezone=auto";


                // 2. Establish HTTP Connection
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();

                // 2. Check if the server responded with an error code
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned HTTP Error: " + responseCode);
                }

                // 3. Read the Stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // 4. Parse JSON Data
                JSONObject jsonResponse = new JSONObject(response.toString());
                /*
                JSONObject currentWeather = jsonResponse.getJSONObject("current_weather");
                String temperature = currentWeather.getString("temperature") + "°C";
                String windSpeed = currentWeather.getString("windspeed") + " km/h";
                */

                // 4.a. Extract the "daily" values container block
                //JSONObject dailyData = jsonResponse.getJSONObject("daily");
                // readDaily(jsonResponse.optJSONObject("daily"), daily);
                List<Daily> daily = readDaily(jsonResponse.getJSONObject("daily"));

                // 4.a. Extract the updated "current" values container block
                JSONObject currentData = jsonResponse.getJSONObject("current");

                // 4.b. Extract the numeric values exactly as native data types
                double tempVal = currentData.getDouble("temperature_2m");
                int humidityVal = currentData.getInt("relative_humidity_2m");
                double windVal = currentData.getDouble("wind_speed_10m");
                int windDirection = currentData.getInt("wind_direction_10m");
                int conditionCode = currentData.getInt("weather_code");
                int isDay = currentData.getInt("is_day");

                // 4.c. Clean up variable formatting to make them UI ready
                String temperature = Math.round(tempVal) + "°C";
                String humidity = humidityVal + "%";
                String wDir = compass(windDirection);
                String windSpeed = Math.round(windVal) + " km/h " + wDir;
                String weatherConditionText = getWeatherConditionString(conditionCode);

                if (daily != null) {
                    //daily.get(0).location = "Current Location";
                    daily.get(0).temperatureAverage = tempVal;
                    daily.get(0).isDay = isDay;
                    daily.get(0).weatherCode = conditionCode;
                    daily.get(0).humidity = humidityVal;
                    daily.get(0).windSpeedMax = windVal;
                    //daily.get(0).precipitationProbabilityMax = humidityVal;
                    //daily.get(0).date = "";
                    daily.get(0).windDirectionDominant = windDirection;
                }


                // 5. Return data to the Main UI Thread
                handler.post(() -> callback.onSuccess(temperature, windSpeed, humidity, weatherConditionText, isDay, daily));


            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }

    private static List<Daily> readDaily(JSONObject source) {
        //System.out.println("Daily Weather--> " + source.toString());
        if (source == null) {
            return null;
        }

        JSONArray times = source.optJSONArray("time");
        if (times == null) {
            return null;
        }

        JSONArray codes = source.optJSONArray("weather_code");
        JSONArray maxTemps = source.optJSONArray("temperature_2m_max");
        JSONArray minTemps = source.optJSONArray("temperature_2m_min");
        JSONArray maxWind = source.optJSONArray("wind_speed_10m_max");
        JSONArray windDir = source.optJSONArray("wind_direction_10m_dominant");
        JSONArray rainChance = source.optJSONArray("precipitation_probability_max");

        List<Daily> daily = new ArrayList<>();

        // The API returns parallel arrays; "time" defines how many days we have.
        for (int i = 0; i < times.length(); i++) {
            Daily day = new Daily();
            day.date = times.optString(i, "");
            day.weatherCode = optInt(codes, i, -1);
            day.temperatureMax = optDouble(maxTemps, i);
            day.temperatureMin = optDouble(minTemps, i);
            day.temperatureAverage = (day.temperatureMax + day.temperatureMin) / 2.0;
            day.windSpeedMax = optDouble(maxWind, i);
            day.windDirectionDominant = optInt(windDir, i, 0);
            day.precipitationProbabilityMax = optInt(rainChance, i, 0);
            daily.add(day);
        }
        return daily;
    }

    private static double optDouble(JSONArray array, int index) {
        return array == null ? 0d : array.optDouble(index, 0d);
    }

    private static int optInt(JSONArray array, int index, int fallback) {
        return array == null ? fallback : array.optInt(index, fallback);
    }

    private static final String[] COMPASS = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    };

    public static String compass(int degrees) {
        int index = (int) Math.round(((degrees % 360) / 22.5)) % COMPASS.length;
        return COMPASS[index < 0 ? index + COMPASS.length : index];
    }

    public static String getWeatherConditionString(int code) {
        switch (code) {
            case 0:  return "Clear sky";
            case 1:  return "Mainly clear";
            case 2:  return "Partly cloudy";
            case 3:  return "Overcast";
            case 45: return "Fog";
            case 48: return "Depositing rime fog";
            case 51: return "Light drizzle";
            case 53: return "Moderate drizzle";
            case 55: return "Dense drizzle";
            case 56: return "Light freezing drizzle";
            case 57: return "Dense freezing drizzle";
            case 61: return "Slight rain";
            case 63: return "Moderate rain";
            case 65: return "Heavy rain";
            case 66: return "Light freezing rain";
            case 67: return "Heavy freezing rain";
            case 71: return "Slight snowfall";
            case 73: return "Moderate snowfall";
            case 75: return "Heavy snowfall";
            case 77: return "Snow grains";
            case 80: return "Slight rain showers";
            case 81: return "Moderate rain showers";
            case 82: return "Violent rain showers";
            case 85: return "Slight snow showers";
            case 86: return "Heavy snow showers";
            case 95: return "Thunderstorm";
            case 96: return "Thunderstorm with slight hail";
            case 99: return "Thunderstorm with heavy hail";
            default: return "Unknown";
        }
    }
}
