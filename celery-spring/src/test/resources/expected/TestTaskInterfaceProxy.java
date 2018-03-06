package com.geneea.celery.test;

import com.geneea.celery.CeleryTaskProxy;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.Void;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TestTaskInterfaceProxy extends CeleryTaskProxy {

    @Override
    public Class<TestTaskInterface> getTaskClass() {
        return TestTaskInterface.class;
    }

    public final ListenableFuture<Integer> sum(int x, int y) throws IOException {
        return submit("sum", new Object[]{x,y});
    }

    public final ListenableFuture<String> asStr(List<String> s) throws IOException {
        return submit("asStr", new Object[]{s});
    }

    public final ListenableFuture<Void> nop() throws IOException {
        return submit("nop", new Object[]{});
    }
}
