package c8y;

import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

public class MicroserviceSubscriptionsServiceMock implements MicroserviceSubscriptionsService {

    @Override
    public String getTenant() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void runForEachTenant(Runnable runnable) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void runForTenant(String s, Runnable runnable) {
        runnable.run();
    }

    @Override
    public <T> T callForTenant(String s, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException("Unknown exception", e);
        }
    }

    @Override
    public void subscribe() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Collection<MicroserviceCredentials> getAll() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Optional<MicroserviceCredentials> getCredentials(String s) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isRegisteredSuccessfully() {
        throw new RuntimeException("Not implemented");
    }
}
