package com.example.txlock.mock.web;

import com.example.txlock.mock.MockControlRequest;
import com.example.txlock.mock.MockState;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mock")
public class MockControlController {

    private static final Logger log = LoggerFactory.getLogger(MockControlController.class);

    private final MockState state;

    public MockControlController(MockState state) {
        this.state = state;
    }

    @PostMapping("/control")
    public Map<String, Object> control(@Valid @RequestBody MockControlRequest request) {
        state.configure(request.mode(), request.times(), request.delayMs());
        log.info("mock control changed mode={} times={} delayMs={}",
                request.mode(), request.times(), request.delayMs());
        return state.snapshot();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return state.snapshot();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        state.reset();
        log.info("mock state reset");
        return state.snapshot();
    }
}
