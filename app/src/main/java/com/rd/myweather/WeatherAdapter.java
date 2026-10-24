package com.rd.myweather;

import static com.rd.myweather.WeatherFetcher.compass;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.text.WordUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherCardViewHolder>{
    Context context;
    private final List<Daily> daily;

    public WeatherAdapter(Context ctx, List<Daily> daily) {
        this.context = ctx;
        this.daily = daily;
    }

    @NonNull
    @Override
    public WeatherAdapter.WeatherCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new WeatherCardViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static int compareWithToday(String inputDateStr) {
        // 1. Parse the input string into a LocalDate object
        // ISO_LOCAL_DATE matches "YYYY-MM-DD" perfectly by default
        LocalDate inputDate = LocalDate.parse(inputDateStr, DateTimeFormatter.ISO_LOCAL_DATE);

        // 2. Get the current system date
        LocalDate today = LocalDate.now();

        // 3. Compare the dates
        return inputDate.compareTo(today);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherAdapter.WeatherCardViewHolder holder, int position) {
        Daily currentDay = daily.get(position);
        if (currentDay.location != null && !currentDay.location.isEmpty()) {
            holder.tvLocationName.setText(WordUtils.capitalizeFully(currentDay.location));
        }
        if (currentDay.date != null && !currentDay.date.isEmpty()) {
            int comparison = compareWithToday(currentDay.date);
            if (comparison == 0) {
                holder.tvUpdatedStatus.setText("Now");
            } else {
                holder.tvUpdatedStatus.setText(WeatherFetcher.date(currentDay.date));
            }
        }

        // Trigger your animations here...
        // 1. Load the fade-in scale animation framework
        Animation fadeInScaleAnim = AnimationUtils.loadAnimation(context, R.anim.fade_in_scale);

        // 2. Clear any lingering animations and fire it right onto the temperature element
        holder.myTemperatureTextView.clearAnimation();
        holder.myTemperatureTextView.startAnimation(fadeInScaleAnim);

        // 3. Optionally trigger it on the status label text too!
        holder.tvWeatherState.clearAnimation();
        holder.tvWeatherState.startAnimation(fadeInScaleAnim);

        String condition = WeatherFetcher.getWeatherConditionString(currentDay.weatherCode);
        holder.tvWeatherState.setText(condition);

        holder.tvHumidityMetric.setText(currentDay.precipitationProbabilityMax + "%");
        holder.myTemperatureTextView.setText(Math.round(currentDay.temperatureAverage) + "°C");
        holder.myTempMaxMin.setText("H:" + Math.round(currentDay.temperatureMax) + "° | L:" + Math.round(currentDay.temperatureMin) + "°");

        int windDirection = currentDay.windDirectionDominant;
        String wDir = compass(windDirection);
        holder.myWindTextView.setText(Math.round(currentDay.windSpeedMax) + " km/h " + wDir);

        // 4. Change the color accent bar dynamically based on conditions
        if(currentDay.isDay == 1) { // At Day
            if (condition.toLowerCase(Locale.ROOT).contains("clear")) {
                holder.ivWeatherState.setImageResource(R.drawable.sunny);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#FF9800")); // Orange accent for hot/sunny weather
            } else if (condition.toLowerCase(Locale.ROOT).contains("rain") || condition.toLowerCase(Locale.ROOT).contains("showers") || condition.toLowerCase(Locale.ROOT).contains("drizzle")) {
                holder.ivWeatherState.setImageResource(R.drawable.scattered_showers_day);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Standard blue accent for downpours
            } else if (condition.toLowerCase(Locale.ROOT).contains("thunderstorm")) {
                holder.ivWeatherState.setImageResource(R.drawable.isolated_scattered_tstorms_day);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73c5")); // Standard blue accent for downpours
            } else {
                // Fallback to the original blue gradient if conditions are default/clear
                holder.ivWeatherState.setImageResource(R.drawable.haze_fog_dust_smoke);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Original Blue
            }
        } else { // at night
            if (condition.toLowerCase(Locale.ROOT).contains("clear")) {
                holder.ivWeatherState.setImageResource(R.drawable.clear_night);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#2A2095")); // Orange accent for hot/sunny weather
            } else if (condition.toLowerCase(Locale.ROOT).contains("rain") || condition.toLowerCase(Locale.ROOT).contains("showers") || condition.toLowerCase(Locale.ROOT).contains("drizzle")) {
                holder.ivWeatherState.setImageResource(R.drawable.scattered_showers_night);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Standard blue accent for downpours
            } else if (condition.toLowerCase(Locale.ROOT).contains("thunderstorm")) {
                holder.ivWeatherState.setImageResource(R.drawable.isolated_scattered_tstorms_night);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73c5")); // Standard blue accent for downpours
            } else {
                // Fallback to the original blue gradient if conditions are default/clear
                holder.ivWeatherState.setImageResource(R.drawable.haze_fog_dust_smoke);
                holder.bottomIndicatorBar.setBackgroundColor(Color.parseColor("#1A73E8")); // Original Blue
            }
        }
    }

    @Override
    public int getItemCount() {
        return daily != null ? daily.size() : 0;
    }

    // ViewHolder class that holds references to the views inside item_card.xml
    public static class WeatherCardViewHolder extends RecyclerView.ViewHolder {
        private TextView myTemperatureTextView, myTempMaxMin, myWindTextView, tvLocationName, tvUpdatedStatus, tvWeatherState, tvHumidityMetric;
        private View bottomIndicatorBar;
        private ImageView ivWeatherState;


        public WeatherCardViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the individual view references to the layout IDs
            tvLocationName = (TextView) itemView.findViewById(R.id.tvLocationName);
            tvUpdatedStatus = (TextView) itemView.findViewById(R.id.tvUpdatedStatus);
            tvWeatherState = (TextView) itemView.findViewById(R.id.tvWeatherState);
            ivWeatherState = (ImageView) itemView.findViewById(R.id.ivWeatherCondition);
            tvHumidityMetric = (TextView) itemView.findViewById(R.id.tvHumidityMetric);
            myTemperatureTextView = (TextView) itemView.findViewById(R.id.tvTemp);
            myTempMaxMin = (TextView) itemView.findViewById(R.id.tvTempMaxMin);
            myWindTextView = (TextView) itemView.findViewById(R.id.tvWind);
            bottomIndicatorBar = itemView.findViewById(R.id.bottomIndicatorBar);
        }
    }
}
