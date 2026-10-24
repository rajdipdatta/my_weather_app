package com.rd.myweather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface GeocodeCallback {
        void onAddressFound(String cityName);
        // void onAddressFound(String cityName, String fullAddress);
        void onError(String errorMessage);
    }

    public static void reverseGeocode(Context context, double latitude, double longitude, GeocodeCallback callback) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Asynchronous method for Android 13 (API 33) and above
            geocoder.getFromLocation(latitude, longitude, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(List<Address> addresses) {
                    processAddresses(addresses, callback);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } else {
            // Synchronous fallback method for older versions (Run on a background thread!)
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    processAddresses(addresses, callback);
                } catch (IOException e) {
                    callback.onError(e.getMessage());
                }
            }).start();
        }
    }

    private static void processAddresses(List<Address> addresses, GeocodeCallback callback) {
        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            String cityName = address.getLocality(); // Returns City Name
            callback.onAddressFound(cityName);
            // String fullAddress = address.getAddressLine(0); // Full readable address
            // callback.onAddressFound(cityName, fullAddress);
        } else {
            callback.onError("No address found for these coordinates.");
        }
    }
}