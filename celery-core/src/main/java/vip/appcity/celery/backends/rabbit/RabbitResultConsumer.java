package vip.appcity.celery.backends.rabbit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import vip.appcity.celery.WorkerException;
import vip.appcity.celery.backends.TaskResult;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of RabbitMQ consumer as a result provider for {@link RabbitBackend}.
 */
@Slf4j
class RabbitResultConsumer<R> extends DefaultConsumer implements RabbitBackend.ResultsProvider<R> {

    private final RabbitBackend backend;
    private final LoadingCache<String, SettableFuture<R>> tasks = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build(CacheLoader.from(SettableFuture::create));

    RabbitResultConsumer(RabbitBackend backend) {
        super(backend.channel);
        this.backend = backend;
    }

    @Override
    public ListenableFuture<R> getResult(String taskId) {
        return tasks.getUnchecked(taskId);
    }

    @Override
    public void handleDelivery(
            String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body
    ) throws IOException {
        TaskResult payload;
        try {
            payload = backend.jsonMapper.readValue(body, TaskResult.class);
        } catch (IOException e) {
            log.error(String.format("could not read payload for deliveryTag=%d", envelope.getDeliveryTag()), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            return;
        }
        if (payload.status == TaskResult.Status.STARTED || payload.status == TaskResult.Status.PENDING|| payload.status == TaskResult.Status.RETRY){
            log.debug("{} status {}",payload.taskId, payload.status);
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            return;
        }

        SettableFuture<R> future = tasks.getUnchecked(payload.taskId);
        boolean setAccepted;
        if (payload.status == TaskResult.Status.SUCCESS) {
            setAccepted = future.set(payload.getResult());
        } else {
            Map<String, String> exc = payload.getResult();
            setAccepted = future.setException(new WorkerException(exc.get("exc_type"), exc.get("exc_message")));
        }

        if (setAccepted) {
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } else {
            log.error("setting future was not accepted for deliveryTag={}", envelope.getDeliveryTag());
            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
        }
    }

    @Override
    public RabbitBackend getBackend() {
        return backend;
    }
}
