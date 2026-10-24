package com.rd.myweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;

import android.content.SharedPreferences;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    RelativeLayout rootLayout;
    // private TextView myTemperatureTextView, myTempMaxMin, myWindTextView, tvLocationName,  tvWeatherState, tvHumidityMetric;
    // private View bottomIndicatorBar;

    // private ImageView ivWeatherState;
    private AutoCompleteTextView etCityInput;
    private ProgressBar progressBar;
    // private Button btnFetch;

    // 1. Find the RecyclerView
    RecyclerView recyclerView;

    private static final int LOCATION_PERMISSION_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private WeatherFetcher fetcher;

    // Key constants to prevent typing mistakes
    private static final String PREFS_NAME = "WeatherAppPrefs";
    private static final String KEY_LAST_CITY = "last_searched_city";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Link the individual view references to the layout IDs
        rootLayout = findViewById(R.id.main);

        // 1. Find the RecyclerView
        recyclerView = findViewById(R.id.recyclerView);

        // 2. Set the LayoutManager (already defined in XML, but safe to set here)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        /*
        tvLocationName = (TextView) findViewById(R.id.tvLocationName);
        tvWeatherState = (TextView) findViewById(R.id.tvWeatherState);
        ivWeatherState = (ImageView) findViewById(R.id.ivWeatherCondition);
        tvHumidityMetric = (TextView) findViewById(R.id.tvHumidityMetric);
        myTemperatureTextView = (TextView) findViewById(R.id.tvTemp);
        myTempMaxMin = (TextView) findViewById(R.id.tvTempMaxMin);
        myWindTextView = (TextView) findViewById(R.id.tvWind);
        bottomIndicatorBar = findViewById(R.id.bottomIndicatorBar);
        */

        progressBar = findViewById(R.id.progressBar);
        // btnFetch = findViewById(R.id.btnFetch);

        // 1. Find the AutoCompleteTextView
        etCityInput = findViewById(R.id.etCityInput);

        // 2. Fetch the string array from strings.xml
        String[] suggestions = getResources().getStringArray(R.array.cities);

        // 3. Create the adapter using a built-in Android layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, suggestions);

        // 4. Set the adapter to the AutoCompleteTextView
        etCityInput.setAdapter(adapter);
        etCityInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 1. Get the selected item string
                // String selectedItem = (String) parent.getItemAtPosition(position);
                // Toast.makeText(MainActivity.this, "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
                // 2. Do something with the selected item
                showWeather();
            }
        });

        // Listen tap on drawable right
        etCityInput.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Compound drawables index: 0=left, 1=top, 2=right/end, 3=bottom
                    Drawable drawableRight = etCityInput.getCompoundDrawablesRelative()[2];

                    if (drawableRight != null && event.getRawX() >= (etCityInput.getRight() - drawableRight.getBounds().width())) {
                        // Icon click action goes here
                        // Toast.makeText(MainActivity.this, "Weather icon clicked!", Toast.LENGTH_SHORT).show();
                        showWeather();
                        // Return true so the click isn't passed down to the text input itself
                        return true;
                    }
                }
                return false;
            }
        });



        // 1. Initialize Views and WeatherFetcher
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetcher = new WeatherFetcher();

        // 2. RUN THE AUTOMATED LAUNCH ROUTINE
        checkLocationPermissionAndFetch();
        /*
        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWeather();
            }
        });
        */
    }

    private void showWeather() {
        String cityName = etCityInput.getText().toString().trim();

        if (cityName.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter a location name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide keyboard
        hideKeyboard();

        // STEP 1: Show the loader and hide old text updates
        progressBar.setVisibility(View.VISIBLE);
        // myTemperatureTextView.setText("Loading...");
        // myWindTextView.setText("");

        // Step 1: Turn text location layout input parameters into coordinates
        fetcher.getCoordinatesFromCity(cityName, new WeatherFetcher.GeocodeCallback() {
            @Override
            public void onCoordinatesFound(double lat, double lon, String resolvedCityName) {
                Toast.makeText(MainActivity.this, ""+lat+", "+lon, Toast.LENGTH_SHORT).show();

                // Calling the method and passing Kolkata coordinates (Lat: 22.4665, Lon: 88.3784)
                fetcher.fetchCurrentWeather(lat, lon, new WeatherFetcher.WeatherCallback() {
                    @Override
                    public void onSuccess(String temperature, String windSpeed, String humidity, String condition, int isDay, List<Daily> daily) {
                        /*
                        // Hide the loader
                        progressBar.setVisibility(View.GONE);
                        // Bind the incoming API information parameters straight to UI fields
                        tvLocationName.setText(cityName.substring(0, 1).toUpperCase() + cityName.substring(1).toLowerCase());
                        myTemperatureTextView.setText(temperature);
                        // 1. Check if the ArrayList is not empty
                        if (!daily.isEmpty()) {
                            // 2. Retrieve the first element at index 0
                            WeatherFetcher.Daily firstDay = daily.get(0);
                            System.out.println("Daily Weather--> " + firstDay.temperatureMax);
                            myTempMaxMin.setText("H:" + firstDay.temperatureMax + "° | L:" + firstDay.temperatureMin + "°");
                        }

                        myWindTextView.setText(windSpeed);
                        tvWeatherState.setText(condition);
                        tvHumidityMetric.setText(humidity);
                        */
                        // 1. SAVE THIS NEW CITY TO LOCAL STORAGE FOR NEXT LAUNCH ======
                        saveLastSearchedCity(cityName);

                        // 2. update UI based on weather condition
                        // updateUI(condition, isDay);
                        updateUI(resolvedCityName, temperature, windSpeed, humidity, condition, isDay, daily);
                    }

                    @Override
                    public void onError(Exception e) {
                        // Hide the loader
                        progressBar.setVisibility(View.GONE);
                        // Handle network or parsing errors gracefully
                        // This prints the exact line and reason for failure in Android Studio's Logcat panel
                        e.printStackTrace();

                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                e.printStackTrace();
                System.out.println("Location Error -> " + e.getMessage());
                Toast.makeText(MainActivity.this, "Location not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method 1: Save the city string into the phone's local storage
    private void saveLastSearchedCity(String cityName) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_CITY, cityName);
        editor.apply(); // Saves the data asynchronously in the background
    }

    // Method 2: Load the saved city if GPS is unavailable
    private void loadSavedCityFallback() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Retrieve the saved city. If nothing is found, default to "Kolkata" as a placeholder
        String lastCity = sharedPreferences.getString(KEY_LAST_CITY, "Kolkata");

        Toast.makeText(this, "GPS offline. Loading last city: " + lastCity, Toast.LENGTH_LONG).show();

        // Re-use your existing geocoding logic to load this fallback city
        fetcher.getCoordinatesFromCity(lastCity, new WeatherFetcher.GeocodeCallback() {
            @Override
            public void onCoordinatesFound(double lat, double lon, String resolvedCityName) {
                executeWeatherCall(lat, lon, resolvedCityName);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to load fallback weather.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void checkLocationPermissionAndFetch() {
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted! Get the coordinates.
            getUserCurrentLocation();
        } else {
            // Request permission from the user
            /* ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
             */
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        }
    }

    private void getUserCurrentLocation() {
        progressBar.setVisibility(View.VISIBLE);

        // Fetch last known location from device cache safely
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // GPS Permission not Granted
            progressBar.setVisibility(View.GONE);
            // ====== GPS IS OFF: TRIGGER THE LOCAL CACHE FALLBACK OVERRIDE ======
            Toast.makeText(MainActivity.this, "Could not detect GPS. Please type a city name.", Toast.LENGTH_LONG).show();
            loadSavedCityFallback();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    // Resolve the location name first from the coordinates
                    startReverseGeocoding(lat, lon);
                    // Trigger the weather fetch directly using the coordinates!
                    // executeWeatherCall(lat, lon, "Current Location");
                } else {
                    progressBar.setVisibility(View.GONE);
                    // ====== GPS IS OFF: TRIGGER THE LOCAL CACHE FALLBACK OVERRIDE ======
                    Toast.makeText(MainActivity.this, "Could not detect GPS. Please type a city name.", Toast.LENGTH_LONG).show();
                    loadSavedCityFallback();
                }
            }
        });
    }

    private void startReverseGeocoding(double latitude, double longitude) {
        //locationTextView.setText("Fetching location name...");

        LocationHelper.reverseGeocode(this, latitude, longitude, new LocationHelper.GeocodeCallback() {
            @Override
            public void onAddressFound(String cityName) {
                // public void onAddressFound(String cityName, String fullAddress) {
                // Important: Geocoder callbacks might return on a background thread.
                // Trigger the weather fetch directly using the coordinates and city name fetched from reverse geocoding!
                executeWeatherCall(latitude, longitude, cityName);
                /*
                // Always force UI updates back onto the Main Thread.
                runOnUiThread(() -> {
                    String displayResult = "City: " + cityName + "\nFull Address: " + fullAddress;
                    // locationTextView.setText(displayResult);
                });
                */
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    // locationTextView.setText("Error: " + errorMessage);
                    Toast.makeText(MainActivity.this, "Geocoding failed!", Toast.LENGTH_SHORT).show();
                });
                // Trigger the weather fetch directly using the coordinates!
                executeWeatherCall(latitude, longitude, "Current Location");
            }
        });
    }

    // Helper method to wrap your existing callback architecture cleanly
    private void executeWeatherCall(double lat, double lon, String locationTitle) {
        fetcher.fetchCurrentWeather(lat, lon, new WeatherFetcher.WeatherCallback() {
            @Override
            public void onSuccess(String temp, String wind, String humidity, String condition, int isDay, List<Daily> daily) {

                // update UI based on weather condition
                updateUI(locationTitle, temp, wind, humidity, condition, isDay, daily);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Weather API Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 3. HANDLE THE USER'S PERMISSION RESPONSE
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User clicked "Allow"! Proceed to fetch coordinates.
                getUserCurrentLocation();
            } else {
                // User clicked "Deny". Explain politely or let them use the EditText box.
                Toast.makeText(this, "Permission denied. Please enter a city manually.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUI(String locationTitle, String temp, String wind, String humidity, String condition, int isDay, List<Daily> daily) {
        // Show Weather data
        progressBar.setVisibility(View.GONE);

        if (daily != null) {
            for (Daily d : daily) {
                if (d.location == null || d.location.isEmpty()) {
                    d.location = locationTitle;
                }
            }
        }

        WeatherAdapter adapter = new WeatherAdapter(MainActivity.this, daily);
        recyclerView.setAdapter(adapter);
        // adapter.notifyDataSetChanged();

        /*
        //tvLocationName.setText(locationTitle.substring(0, 1).toUpperCase() + locationTitle.substring(1).toLowerCase());
        tvLocationName.setText(WordUtils.capitalizeFully(locationTitle));
        myTemperatureTextView.setText(temp);

        // 1. Check if the ArrayList is not empty
        if (!daily.isEmpty()) {
            // 2. Retrieve the first element at index 0
            Daily firstDay = daily.get(0);
            myTempMaxMin.setText("H:" + firstDay.temperatureMax + "° | L:" + firstDay.temperatureMin + "°");
        }

        // if(isDay == 1 && condition.contains("clear")) condition = "Sunny";
        tvWeatherState.setText(condition);
        tvHumidityMetric.setText(humidity);
        myWindTextView.setText(wind);
        */
        /*
        // Trigger your animations here...
        // 1. Load the fade-in scale animation framework
        Animation fadeInScaleAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in_scale);

        // 2. Clear any lingering animations and fire it right onto the temperature element
        myTemperatureTextView.clearAnimation();
        myTemperatureTextView.startAnimation(fadeInScaleAnim);

        // 3. Optionally trigger it on the status label text too!
        tvWeatherState.clearAnimation();
        tvWeatherState.startAnimation(fadeInScaleAnim);
        */

        // 4. Change the color accent bar dynamically based on conditions
        if(isDay == 1) { // At Day
            if (condition.toLowerCase(Locale.ROOT).contains("clear")) {
                //ivWeatherState.setImageResource(R.drawable.sunny);
                animateBackgroundTransition(R.drawable.bg_sunny);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#FF9800")); // Orange accent for hot/sunny weather
            } else if (condition.toLowerCase(Locale.ROOT).contains("rain") || condition.toLowerCase(Locale.ROOT).contains("showers") || condition.toLowerCase(Locale.ROOT).contains("drizzle")) {
                //ivWeatherState.setImageResource(R.drawable.scattered_showers_day);
                animateBackgroundTransition(R.drawable.bg_rainy);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Standard blue accent for downpours
            } else if (condition.toLowerCase(Locale.ROOT).contains("thunderstorm")) {
                //ivWeatherState.setImageResource(R.drawable.isolated_scattered_tstorms_day);
                animateBackgroundTransition(R.drawable.bg_rainy);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73c5")); // Standard blue accent for downpours
            } else {
                // Fallback to the original blue gradient if conditions are default/clear
                //ivWeatherState.setImageResource(R.drawable.haze_fog_dust_smoke);
                animateBackgroundTransition(R.drawable.weather_bg);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Original Blue
            }
        } else { // at night
            if (condition.toLowerCase(Locale.ROOT).contains("clear")) {
                //ivWeatherState.setImageResource(R.drawable.clear_night);
                animateBackgroundTransition(R.drawable.bg_mostly_clear_night);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#2A2095")); // Orange accent for hot/sunny weather
            } else if (condition.toLowerCase(Locale.ROOT).contains("rain") || condition.toLowerCase(Locale.ROOT).contains("showers") || condition.toLowerCase(Locale.ROOT).contains("drizzle")) {
                //ivWeatherState.setImageResource(R.drawable.scattered_showers_night);
                animateBackgroundTransition(R.drawable.bg_rainy);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Standard blue accent for downpours
            } else if (condition.toLowerCase(Locale.ROOT).contains("thunderstorm")) {
                //ivWeatherState.setImageResource(R.drawable.isolated_scattered_tstorms_night);
                animateBackgroundTransition(R.drawable.bg_rainy);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73c5")); // Standard blue accent for downpours
            } else {
                // Fallback to the original blue gradient if conditions are default/clear
                //ivWeatherState.setImageResource(R.drawable.haze_fog_dust_smoke);
                animateBackgroundTransition(R.drawable.weather_bg);
                //bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Original Blue
            }
        }
    }

    // Helper method to cross-fade the background smoothly
    private void animateBackgroundTransition(int newDrawableResourceId) {
        // 1. Get the current background drawable of the layout
        Drawable currentBackground = rootLayout.getBackground();

        // 2. Load the new target background drawable
        Drawable newBackground = ContextCompat.getDrawable(this, newDrawableResourceId);

        if (currentBackground != null && newBackground != null) {
            // 3. Create an array containing the starting layer and ending layer
            Drawable[] layers = new Drawable[2];

            // If it's already a transition, grab its top layer to avoid stacking artifacts
            if (currentBackground instanceof TransitionDrawable) {
                layers[0] = ((TransitionDrawable) currentBackground).getDrawable(1);
            } else {
                layers[0] = currentBackground;
            }
            layers[1] = newBackground;

            // 4. Initialize the TransitionDrawable and apply it to the root layout
            TransitionDrawable transition = new TransitionDrawable(layers);
            rootLayout.setBackground(transition);

            // 5. Start the animation over 600 milliseconds
            transition.startTransition(600);
        } else {
            // Fallback case if drawables fail to load
            rootLayout.setBackgroundResource(newDrawableResourceId);
        }
    }


    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}