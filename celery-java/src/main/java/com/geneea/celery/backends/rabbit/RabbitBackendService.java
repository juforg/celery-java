package com.geneea.celery.backends.rabbit;

import com.geneea.celery.spi.BackendFactory;

import org.kohsuke.MetaInfServices;

@MetaInfServices(BackendFactory.class)
class RabbitBackendService extends RabbitBackendFactory {

}
