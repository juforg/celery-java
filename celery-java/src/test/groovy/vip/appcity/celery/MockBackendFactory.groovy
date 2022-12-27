package vip.appcity.celery


import vip.appcity.celery.spi.Backend
import vip.appcity.celery.spi.BackendFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException

public class MockBackendFactory implements BackendFactory {
    static backend

    @Override
    Set<String> getProtocols() {
        return ["mock"]
    }

    @Override
    Backend createBackend(URI uri, ExecutorService executor) throws IOException, TimeoutException {
        return backend
    }
}
