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
            @RequestParam(defaultValue = "0.2") double noise,
            Model model) {

        double[] rawSignal = EcgProcessor.generateSyntheticEcg(samplingRate, duration, noise);
        double[] filteredSignal = EcgProcessor.applyFilter(rawSignal, 5);
        List<Integer> peaks = EcgProcessor.detectPeaks(filteredSignal, 0.8);

        double bpm = (peaks.size() / duration) * 60.0;

        model.addAttribute("rawSignal", rawSignal);
        model.addAttribute("filteredSignal", filteredSignal);
        model.addAttribute("bpm", (int) bpm);
        model.addAttribute("peaksCount", peaks.size());
        model.addAttribute("samplingRate", samplingRate);
        model.addAttribute("noise", noise);

        return "index";
    }
}
