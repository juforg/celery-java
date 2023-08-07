package vip.appcity.celery;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * celery任务发布器，支持多个不同队列
 * </p>
 *
 * @author songjie
 * @date 2023/8/7 10:38
 * SJ编程规范
 * 命名：
 * 1. 见名思意，变量的名字必须准确反映它的含义和内容
 * 2. 遵循当前语言的变量命名规则
 * 3. 不要对不同使用目的的变量使用同一个变量名
 * 4. 同个项目不要使用不同名称表述同个东西
 * 5. 函数/方法 使用动词+名词组合，其它使用名词组合
 * 设计原则：
 * 1. KISS原则： Keep it simple and stupid !
 * 2. SOLID原则：S: 单一职责 O: 开闭原则 L: 迪米特法则 I: 接口隔离原则 D: 依赖倒置原则
 */
@Slf4j
public class CeleryTaskProducer implements Closeable {
    private final Map<String, Celery> taskQueueClientMap = new ConcurrentHashMap<>();

    private Map<String, String> taskQueueMaps = new HashMap<>();
    private String broker;
    private String backend;
    private String defaultQueueName = "celery";

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public void setDefaultQueueName(String defaultQueueName) {
        this.defaultQueueName = defaultQueueName;
    }

    public void setTaskQueueMaps(Map<String, String> taskQueueMaps) {
        this.taskQueueMaps = taskQueueMaps;
    }
    public final <R> ListenableFuture<R> submit(String taskName, Object[] args) throws IOException {
        return this.submit(taskName, UUID.randomUUID().toString(), args);
    }
    public final <R> ListenableFuture<R> submit(String taskName, String taskId, Object[] args) throws IOException {
        String queueName = taskQueueMaps.get(taskName);
        queueName = StringUtils.isNotBlank(queueName)? queueName : this.defaultQueueName;
        Celery celery = taskQueueClientMap.get(queueName);
        if (celery==null){
            celery = creatNewQueueClient(queueName);
            taskQueueClientMap.put(queueName, celery);
        }
        return celery.submit(taskName, taskId, args);
    }

    public final <R> ListenableFuture<R> submit(String queueName, String taskName, String taskId, Object[] args) throws IOException {
        Celery celery = taskQueueClientMap.get(queueName);
        if (celery==null){
            celery = creatNewQueueClient(queueName);
            taskQueueClientMap.put(queueName, celery);
        }
        return celery.submit(taskName, taskId, args);
    }

    public void addQueueClient(String queue, Celery celery){
        taskQueueClientMap.put(queue, celery);
    }
    @Override
    public void close() throws IOException {
        for (Celery celery : taskQueueClientMap.values()) {
            celery.close();
        }
    }

    private Celery creatNewQueueClient(String queueName){
        Celery.CeleryBuilder builder = Celery.builder().brokerUri(this.broker);
        if(!StringUtils.isEmpty(this.broker)) {
            builder.brokerUri(this.broker);
            log.info("[celery] {} use broker: {}", queueName, this.broker);
        }else{
            throw new RuntimeException("no broker config");
        }

        if(!StringUtils.isEmpty(this.backend)) {
            builder.backendUri(this.backend);
            log.info("[celery] {} use backend: {}", queueName, this.backend);
        }else{
            log.debug("[celery] dont use backend");
        }

        if(!StringUtils.isEmpty(queueName)) {
            builder.queue(queueName);
        }else{
            throw new RuntimeException("no queueName config");
        }
        log.info("[celery] {} init finished", queueName);
        return builder.build();
    }
}
