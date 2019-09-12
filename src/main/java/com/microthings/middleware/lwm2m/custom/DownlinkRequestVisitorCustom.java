package com.microthings.middleware.lwm2m.custom;

import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;

/**
 * @author MY-HE
 * @date 2019-09-10 21:31
 */
public interface DownlinkRequestVisitorCustom extends DownlinkRequestVisitor {
    void visit(CommandRequest request);
}
