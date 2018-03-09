package ch.cyberduck.core;

import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.threading.CancelCallback;

public class NullSession extends Session<Void> {

    public NullSession(Host h) {
        super(h);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public Void open(final HostKeyCallback c, final LoginCallback login) throws BackgroundException {
        return null;
    }

    @Override
    protected Void connect(final Proxy proxy, final HostKeyCallback key, final LoginCallback prompt) throws BackgroundException {
        return null;
    }

    @Override
    public void login(final Proxy proxy, final HostPasswordStore keychain, final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        throw new LoginCanceledException();
    }

    @Override
    protected void logout() {
        //
    }

    public AttributedList<Path> list(final Path file, final ListProgressListener listener) throws BackgroundException {
        return new AttributedList<Path>() {
            @Override
            public boolean contains(final Path file) {
                return true;
            }

            @Override
            public Path get(final Path reference) {
                return reference;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(Class<T> type) {
        if(type == Write.class) {
            return (T) new NullWriteFeature(this);
        }
        if(type == Read.class) {
            return (T) new NullReadFeature();
        }
        if(type == Move.class) {
            return (T) new NullMoveFeature();
        }
        return super._getFeature(type);
    }

}

