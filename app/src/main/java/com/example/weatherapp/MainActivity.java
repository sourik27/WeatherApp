package com.example.weatherapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView temperatureText, showingCitySearched, humidityText, windText;
    private EditText cityNameInput;
    private Button fetchWeatherButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        fetchWeatherButton = findViewById(R.id.fetchWeatherButton);
        temperatureText = findViewById(R.id.temperatureText);
        showingCitySearched = findViewById(R.id.showingCity_searched);
        humidityText = findViewById(R.id.humidityText);
        windText = findViewById(R.id.windText);
        cityNameInput = findViewById(R.id.cityNameInput);

        fetchWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = cityNameInput.getText().toString().trim();
                if (!cityName.isEmpty()) {
                    fetchWeatherData(cityName);
                } else {
                    cityNameInput.setError("Enter a city name");
                }
            }
        });

        // Default initial load
        fetchWeatherData("Nürnberg");
    }

    private void fetchWeatherData(String cityName) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            try {
                // Step 1: Convert typed city name into coordinates (latitude & longitude)
                String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + cityName + "&count=1&language=en&format=json";
                Request geoRequest = new Request.Builder().url(geoUrl).build();
                Response geoResponse = client.newCall(geoRequest).execute();

                if (!geoResponse.isSuccessful() || geoResponse.body() == null) {
                    showToast("Search failed");
                    return;
                }

                String geoResult = geoResponse.body().string();
                JSONObject geoJson = new JSONObject(geoResult);

                if (!geoJson.has("results")) {
                    showToast("City not found");
                    return;
                }

                JSONArray results = geoJson.getJSONArray("results");
                JSONObject location = results.getJSONObject(0);
                double lat = location.getDouble("latitude");
                double lon = location.getDouble("longitude");
                String officialCityName = location.getString("name");

                // Step 2: Fetch actual weather data using the converted coordinates
                String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m";
                Request weatherRequest = new Request.Builder().url(weatherUrl).build();
                Response weatherResponse = client.newCall(weatherRequest).execute();

                if (!weatherResponse.isSuccessful() || weatherResponse.body() == null) {
                    showToast("Weather fetch failed");
                    return;
                }

                String weatherResult = weatherResponse.body().string();
                runOnUiThread(() -> updateUI(officialCityName, weatherResult));

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                showToast("Error retrieving data");
            }
        });
    }

    private void updateUI(String cityName, String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject current = jsonObject.getJSONObject("current");

            double temperature = current.getDouble("temperature_2m");
            double humidity = current.getDouble("relative_humidity_2m");
            double windSpeed = current.getDouble("wind_speed_10m");

            temperatureText.setText(String.format("%.0f°C", temperature));
            showingCitySearched.setText("Current temperature in " + cityName);
            humidityText.setText(String.format("%.0f%%", humidity));
            windText.setText(String.format("%.0f km/h", windSpeed));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}