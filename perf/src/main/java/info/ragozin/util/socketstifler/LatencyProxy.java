package info.ragozin.util.socketstifler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import javax.management.MXBean;
import javax.management.ObjectName;

import org.gridkit.internal.com.jcraft.jsch.Packet;

public class LatencyProxy {

    public static LatencyProxy start(SocketAddress accept, SocketAddress forward) throws Exception {
        LatencyProxy proxy = new LatencyProxy();
        int inPort = ((InetSocketAddress)accept).getPort();
        int outPort = ((InetSocketAddress)forward).getPort();
        ObjectName name = ObjectName.getInstance("info.ragozin:type=SocketPoxy,src=" + inPort + ",fwd=" + outPort);

        ServerSocket sock = new ServerSocket();
        sock.bind(accept);

        AcceptThread acc = proxy.new AcceptThread(sock, forward);
        acc.start();

        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
        } catch (Exception e) {
        }

        ManagementFactory.getPlatformMBeanServer().registerMBean(proxy.getMBean(), name);

        return proxy;
    }

    private volatile long latency = 0;

    private int packetSize = 16 << 10;

    private AtomicLong connectionCount = new AtomicLong();
    private AtomicLong client2server = new AtomicLong();
    private AtomicLong server2client = new AtomicLong();

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    private ProxyMBean getMBean() {
        return new ProxyMBean() {

            @Override
            public long getClient2Server() {
                return client2server.get();
            }

            @Override
            public long getServer2Client() {
                return server2client.get();
            }

            @Override
            public long getTotalConnectionCount() {
                return connectionCount.get();
            }

            @Override
            public long getLatencyMicros() {
                return TimeUnit.NANOSECONDS.toMicros(latency);
            }

            @Override
            public void setLatencyMicros(long latencyMicrocs) {
                latency = TimeUnit.MICROSECONDS.toNanos(latencyMicrocs);
            }
        };
    }


    private class AcceptThread extends Thread {

        private final ServerSocket socket;
        private final SocketAddress forward;

        public AcceptThread(ServerSocket socket, SocketAddress forward) {
            this.socket = socket;
            this.forward = forward;
            this.setDaemon(true);
            this.setName("ACCEPT[" + socket.getLocalPort() + "]");
        }

        @Override
        public void run() {
            try {
                while(true) {
                    Socket clientSide = socket.accept();
                    try {
                        if (clientSide != null) {
                            Socket serverSide = new Socket();
                            serverSide.connect(forward);

                            ForwardThread a = new ForwardThread(clientSide, serverSide, client2server);
                            ForwardThread b = new ForwardThread(serverSide, clientSide, server2client);
                            a.start();
                            b.start();
                            connectionCount.incrementAndGet();
                        }
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ForwardThread extends Thread {

        private final Socket input;
        private final Socket output;
        private final AtomicLong byteCounter;

        public ForwardThread(Socket input, Socket output, AtomicLong byteCounter) {
            super();
            this.input = input;
            this.output = output;
            this.byteCounter = byteCounter;
            this.setDaemon(true);
            this.setName("FWD[" + input.getRemoteSocketAddress() + "->" + output.getRemoteSocketAddress() + "]");
        }

        @Override
        public void run() {
            while(true) {
                try {
                    byte[] buf = new byte[packetSize];

                    int n = input.getInputStream().read(buf);

                    if (n > 0) {
                        if (latency > 0) {
                            LockSupport.parkNanos(latency);
                        }
                        byteCounter.addAndGet(n);
                        output.getOutputStream().write(buf, 0, n);
                    }
                    else if (n < 0) {
                        output.shutdownOutput();
                        if (input.isOutputShutdown()) {
                            input.close();
                            output.close();
                        }
                        break;
                    }

                } catch (IOException e) {
                    try {
                        input.close();
                    } catch (IOException e1) {
                    }
                    try {
                        output.close();
                    } catch (IOException e1) {
                    }
                    break;
                }
            }
        }
    }

    @MXBean
    public static interface ProxyMBean {

        public long getClient2Server();

        public long getServer2Client();

        public long getTotalConnectionCount();

        public long getLatencyMicros();

        public void setLatencyMicros(long latencyMicrocs);
    }
}
