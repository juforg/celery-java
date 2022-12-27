package vip.appcity.celery.backends.rabbit;

import vip.appcity.celery.spi.BackendFactory;

import org.kohsuke.MetaInfServices;

@MetaInfServices(BackendFactory.class)
public class RabbitBackendService extends RabbitBackendFactory {

}
