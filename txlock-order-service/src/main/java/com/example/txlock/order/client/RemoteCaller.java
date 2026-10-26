package com.example.txlock.order.client;

import com.example.txlock.common.RemoteOrderRequest;
import com.example.txlock.common.RemoteOrderResponse;
import com.example.txlock.order.config.DemoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RemoteCaller {

    private static final Logger log = LoggerFactory.getLogger(RemoteCaller.class);
    private static final int MAX_ERROR_LENGTH = 450;

    private final RemoteOrderApi api;
    private final DemoProperties properties;

    public RemoteCaller(RemoteOrderApi api, DemoProperties properties) {
        this.api = api;
        this.properties = properties;
    }

    public RemoteCallResult callWithRetry(RemoteOrderRequest request, int maxAttempts) {
        int allowedAttempts = Math.max(1, maxAttempts);
        int attempts = 0;
        String errorType = null;
        String errorMessage = null;

        while (attempts < allowedAttempts) {
            attempts++;
            try {
                RemoteOrderResponse response = api.sync(request);
                return RemoteCallResult.success(attempts, response.duplicate());
            } catch (HttpServerErrorException e) {
                errorType = "REMOTE_5XX";
                errorMessage = describe(e);
            } catch (HttpClientErrorException e) {
                // 4xx 是确定性拒绝，重试不会改变结果；只有网络/超时/5xx 才值得重试。
                errorType = "REJECTED";
                errorMessage = describe(e);
                log.warn("remote rejected bizNo={} status={} body={}",
                        request.bizNo(), e.getStatusCode(), errorMessage);
                break;
            } catch (ResourceAccessException e) {
                errorType = "TIMEOUT_OR_NETWORK";
                errorMessage = describe(e);
            } catch (RuntimeException e) {
                errorType = "UNKNOWN";
                errorMessage = describe(e);
            }

            log.warn("remote call failed bizNo={} attempt={}/{} type={} error={}",
                    request.bizNo(), attempts, allowedAttempts, errorType, errorMessage);
            if (attempts < allowedAttempts && !backoff(attempts)) {
                break;
            }
        }

        return RemoteCallResult.failure(attempts, errorType == null ? "UNKNOWN" : errorType, errorMessage);
    }

    public RemoteLookup lookup(String bizNo) {
        try {
            return RemoteLookup.found(api.get(bizNo));
        } catch (HttpClientErrorException.NotFound e) {
            return RemoteLookup.notFound();
        } catch (RuntimeException e) {
            return RemoteLookup.error(describe(e));
        }
    }

    private boolean backoff(int attempt) {
        try {
            Thread.sleep(properties.remote().backoffMs() * attempt);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String describe(Throwable error) {
        if (error instanceof RestClientResponseException responseError) {
            String body = responseError.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return truncate(body);
            }
            return truncate(responseError.getStatusCode() + " " + responseError.getStatusText());
        }
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return truncate(root.getClass().getSimpleName() + (message == null ? "" : ": " + message));
    }

    private static String truncate(String value) {
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
