package com.ece.dsp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class EcgController {

    @GetMapping("/")
    public String index(
            @RequestParam(defaultValue = "500") int samplingRate,
            @RequestParam(defaultValue = "5") double duration,
            @RequestParam(defaultValue = "0.3") double noise,
            @RequestParam(defaultValue = "75") double targetBpm,
            Model model) {

        // 1. Generate Signal
        double[] rawSignal = EcgProcessor.generateSyntheticEcg(samplingRate, duration, noise, targetBpm);

        // 2. Cascade DSP Filtering Pipeline
        double[] stage1 = EcgProcessor.applyButterworthLowPass(rawSignal, samplingRate, 35.0);
        double[] filteredSignal = EcgProcessor.applyNotchFilter(stage1, samplingRate, 50.0);

        // 3. Pan-Tompkins Peak Detection & Metrics
        List<Integer> peaks = EcgProcessor.panTompkinsPeakDetection(filteredSignal, samplingRate);
        double calculatedBpm = (peaks.size() / duration) * 60.0;
        double hrv = EcgProcessor.calculateHRV(peaks, samplingRate);

        // 4. Clinical Condition Evaluation
        String status = "Normal Sinus Rhythm";
        if (calculatedBpm > 100) status = "Tachycardia Detected (High Heart Rate)";
        else if (calculatedBpm < 60) status = "Bradycardia Detected (Low Heart Rate)";

        // 5. FFT Spectrum Analysis
        double[] rawFFT = EcgProcessor.computeFFTSpectrum(rawSignal);
        double[] filteredFFT = EcgProcessor.computeFFTSpectrum(filteredSignal);

        model.addAttribute("rawSignal", rawSignal);
        model.addAttribute("filteredSignal", filteredSignal);
        model.addAttribute("rawFFT", rawFFT);
        model.addAttribute("filteredFFT", filteredFFT);
        model.addAttribute("bpm", (int) calculatedBpm);
        model.addAttribute("hrv", String.format("%.2f", hrv));
        model.addAttribute("status", status);
        model.addAttribute("peaksCount", peaks.size());
        model.addAttribute("samplingRate", samplingRate);
        model.addAttribute("noise", noise);
        model.addAttribute("targetBpm", (int) targetBpm);

        return "index";
    }
}
