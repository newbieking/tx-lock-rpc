package com.example.txlock.mock.web;

import com.example.txlock.common.ApiError;
import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.common.RemoteOrderResponse;
import com.example.txlock.common.RemoteOrderView;
import com.example.txlock.mock.MockMode;
import com.example.txlock.mock.MockState;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/remote/orders")
public class RemoteOrderController {

    private static final Logger log = LoggerFactory.getLogger(RemoteOrderController.class);

    private final MockState state;

    public RemoteOrderController(MockState state) {
        this.state = state;
    }

    @PostMapping
    public ResponseEntity<?> accept(@Valid @RequestBody RemoteOrderRequest request) throws InterruptedException {
        state.recordRequest();
        MockMode decision = state.nextDecision();
        long delay = state.delayFor(decision);
        log.info("remote request bizNo={} eventId={} decision={} delayMs={}",
                request.bizNo(), request.eventId(), decision, delay);

        if (delay > 0) {
            Thread.sleep(delay);
        }

        if (decision == MockMode.FAIL) {
            state.recordFailed();
            return error(500, "MOCK_FAIL", "mock provider forced 500");
        }
        if (decision == MockMode.REJECT) {
            state.recordRejected();
            return error(409, "MOCK_REJECT", "mock provider rejected this order");
        }
        if (decision == MockMode.TIMEOUT) {
            // 客户端已经超时，但服务端依然完成了落库，用来演示“超时不等于失败”。
            state.recordTimeout();
        }

        MockState.StoreResult stored = state.store(request);
        log.info("remote stored bizNo={} remoteNo={} duplicate={}",
                request.bizNo(), stored.view().remoteNo(), stored.duplicate());
        return ResponseEntity.ok(new RemoteOrderResponse(
                stored.view().remoteNo(),
                request.bizNo(),
                stored.duplicate(),
                stored.view().receivedAt()));
    }

    @GetMapping("/{bizNo}")
    public ResponseEntity<?> get(@PathVariable String bizNo) {
        RemoteOrderView view = state.find(bizNo);
        if (view == null) {
            return error(404, "NOT_FOUND", "remote order not found: " + bizNo);
        }
        return ResponseEntity.ok(view);
    }

    @GetMapping
    public List<RemoteOrderView> list() {
        return state.allOrders();
    }

    private static ResponseEntity<ApiError> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
