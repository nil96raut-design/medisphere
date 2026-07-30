package com.healthtrack.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClinicalAiAssistantService {

    public Map<String, Object> analyzePatientVitalsAndLabs(
            String bloodPressure, Integer heartRate, BigDecimal temperature, Integer spo2, String labResultSummary) {

        Map<String, Object> analysis = new HashMap<>();
        List<String> diagnosticHints = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();

        if (spo2 != null && spo2 < 92) {
            anomalies.add("Hypoxemia Risk: SpO2 is critically low (" + spo2 + "%)");
            diagnosticHints.add("Consider Supplemental Oxygen & Chest X-Ray / ABG evaluation.");
        }

        if (heartRate != null && heartRate > 120) {
            anomalies.add("Severe Tachycardia: Heart rate is " + heartRate + " bpm");
            diagnosticHints.add("Check for Sepsis, Dehydration, or Pulmonary Embolism.");
        }

        if (temperature != null && temperature.compareTo(new BigDecimal("38.5")) > 0) {
            anomalies.add("High Fever (Pyrexia): " + temperature + "°C");
            diagnosticHints.add("Screen for Blood/Urinary tract infection. Consider antipyretics.");
        }

        if (bloodPressure != null && bloodPressure.contains("/")) {
            try {
                String[] parts = bloodPressure.split("/");
                int sys = Integer.parseInt(parts[0].trim());
                int dia = Integer.parseInt(parts[1].trim());
                if (sys > 160 || dia > 100) {
                    anomalies.add("Hypertensive Crisis Risk: BP " + bloodPressure);
                    diagnosticHints.add("Administer IV Antihypertensives if symptomatic.");
                }
            } catch (Exception ignored) {}
        }

        analysis.put("anomaliesDetected", anomalies);
        analysis.put("aiDiagnosticHints", diagnosticHints);
        analysis.put("riskLevel", anomalies.isEmpty() ? "LOW" : (anomalies.size() > 2 ? "HIGH" : "MODERATE"));
        return analysis;
    }
}
