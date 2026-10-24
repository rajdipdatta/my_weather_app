package com.rd.myweather;

/** One row of the 7-day forecast. */
public class Daily {
    public String location = "";
    public String date = "";
    public int humidity;
    public int weatherCode;
    public double temperatureMax;
    public double temperatureMin;
    public double temperatureAverage;
    public double windSpeedMax;
    public int windDirectionDominant;
    public int precipitationProbabilityMax;
    public int isDay;

    public Daily(String Location, String Date, int WeatherCode, double TemperatureMax, double TemperatureMin, double TemperatureAverage, double WindSpeedMax, int WindDirectionDominant, int PrecipitationProbabilityMax, int Humidity, int IsDay) {
        this.location = Location;
        this.date = Date;
        this.weatherCode = WeatherCode;
        this.temperatureMax = TemperatureMax;
        this.temperatureMin = TemperatureMin;
        this.temperatureAverage = TemperatureAverage;
        this.windSpeedMax = WindSpeedMax;
        this.windDirectionDominant = WindDirectionDominant;
        this.precipitationProbabilityMax = PrecipitationProbabilityMax;
        this.humidity = Humidity;
        this.isDay = IsDay;
    }

    public Daily() {
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public double getTemperatureMax() {
        return temperatureMax;
    }

    public double getTemperatureMin() {
        return temperatureMin;
    }

    public double getTemperatureAverage() {
        return temperatureAverage;
    }

    public double getWindSpeedMax() {
        return windSpeedMax;
    }

    public int getWindDirectionDominant() {
        return windDirectionDominant;
    }

    public int getPrecipitationProbabilityMax() {
        return precipitationProbabilityMax;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getIsDay() {
        return isDay;
    }
}
