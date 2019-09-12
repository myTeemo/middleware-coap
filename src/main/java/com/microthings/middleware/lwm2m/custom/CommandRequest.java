package com.microthings.middleware.lwm2m.custom;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.AbstractDownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ExecuteResponse;

/**
 * @author MY-HE
 * @date 2019-09-10 21:23
 */
public class CommandRequest extends AbstractDownlinkRequest<CommandResponse> {
    private final byte[] commands;

    public CommandRequest(String path) {
        this(newPath(path), null);
    }

    public CommandRequest(String path, byte[] commands) {
        this(newPath(path), commands);
    }

    public CommandRequest(int objectId, int objectInstanceId, int resourceId) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), null);
    }

    public CommandRequest(int objectId, int objectInstanceId, int resourceId,
                          byte[] commands) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), commands);
    }

    private CommandRequest(LwM2mPath path, byte[] commands) {
        super(path);
        if (path.isRoot())
            throw new InvalidRequestException(
                    "Command request cannot target root path");

        if (!path.isResource())
            throw new InvalidRequestException(
                    "Invalid path %s : Only resource can be executed.", path);
        this.commands = commands;
    }

    @Override
    public String toString() {
        return String.format("CommandRequest [%s]", getPath());
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }

    public byte[] getCommands() {
        return this.commands;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((commands == null) ? 0 : commands.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandRequest other = (CommandRequest) obj;
        if (commands == null) {
            if (other.commands != null)
                return false;
        } else if (!commands.equals(other.commands))
            return false;
        return true;
    }
}
