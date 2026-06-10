package com.terralite.launcher.net;

import com.terralite.core.logging.Loggers;
import org.slf4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Server-side state for one connected client. */
public final class ClientSession {
    private static final Logger log = Loggers.get(ClientSession.class);

    public final String playerId;
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private volatile boolean alive = true;
    // Last known position and look direction — updated by the game thread from PlayerMoveMessage
    volatile double px, py, pz, pyaw, ppitch;
    final ConcurrentLinkedQueue<NetMessage> inbound = new ConcurrentLinkedQueue<>();

    ClientSession(String playerId, Socket socket) throws IOException {
        this.playerId = playerId;
        this.socket = socket;
        this.writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())), false);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    void startReading() {
        Thread t = new Thread(this::readLoop, "net-client-" + socket.getRemoteSocketAddress());
        t.setDaemon(true);
        t.start();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    inbound.add(MessageCodec.decode(line));
                } catch (IOException e) {
                    log.warn("Bad message from {}: {}", socket.getRemoteSocketAddress(), e.getMessage());
                }
            }
        } catch (IOException e) {
            if (alive) log.debug("Client disconnected: {}", socket.getRemoteSocketAddress());
        } finally {
            alive = false;
        }
    }

    /** Thread-safe: called from the game thread. */
    public void send(NetMessage msg) {
        if (!alive) return;
        try {
            writer.println(MessageCodec.encode(msg));
            writer.flush();
        } catch (IOException e) {
            alive = false;
        }
    }

    public double px() { return px; }
    public double py() { return py; }
    public double pz() { return pz; }
    public double pyaw() { return pyaw; }
    public double ppitch() { return ppitch; }

    public boolean isAlive() { return alive; }

    public void close() {
        alive = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
