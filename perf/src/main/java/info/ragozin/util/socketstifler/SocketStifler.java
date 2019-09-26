package info.ragozin.util.socketstifler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class SocketStifler {

    private static final ObjectName MBEAN;
    static {
        try {
            MBEAN = ObjectName.getInstance("info.ragozin:name=Stifler");
        } catch (MalformedObjectNameException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

	public static final SocketStifler start() {
		try {
			SocketStifler stifler = new SocketStifler();
			stifler.startReactor();
			return stifler;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Selector selector;

	private List<Router> routers = new ArrayList<Router>();
	private List<Connection> connections = new ArrayList<Connection>();

	// settings

	private int bufferLimit = 64 << 10;

	private long latency = 0; //TimeUnit.MILLISECONDS.toNanos(100);

	private int transferLimit = 50 << 10; // 100KiB / sec

	private long transferPeriod = TimeUnit.MILLISECONDS.toNanos(500);

	private long totalConnnections = 0;

	private long totalClient2ServerClosed = 0;

	private long totalServer2ClientClosed = 0;

	private SocketStifler() throws IOException {
		selector = Selector.open();
	}

	protected void startReactor() {
		Thread reactor = new Thread(() -> reactorLoop(), "SocketStifler");
		reactor.setDaemon(true);
		reactor.start();

		MXBridge bridge = new MXBridge();
		try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(bridge, MBEAN);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            e.printStackTrace();
        }
	}

	private void reactorLoop() {
		try {
			while(true) {
				long timeout = getTimeout();
				if (timeout > 0) {
				    selector.select(timeout);
				}
				selector.selectNow();

				synchronized(this) {
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> its = keys.iterator();
					while(its.hasNext()) {
						SelectionKey sk = its.next();
						its.remove();
						Selectable ss = (Selectable) sk.attachment();
						ss.onSelector();
					}
					Iterator<Connection> it = connections.iterator();
					while(it.hasNext()) {
						Connection conn = it.next();
						conn.onTimer();
						if (conn.isClosed()) {
						    totalClient2ServerClosed += conn.client2server.totalBytes;
						    totalServer2ClientClosed += conn.server2client.totalBytes;
							it.remove();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// exit
		}
	}

	private synchronized long getTimeout() {
		long deadline = currentTime() + transferPeriod / 2;
		for(Connection conn: connections) {
			long wake = conn.getWakeupTimestamp();
			if (wake < deadline) {
				deadline = wake;
			}
		}
		long to = TimeUnit.NANOSECONDS.toMillis(deadline - currentTime());
		return to <= 0 ? 1 : to;
	}

	public synchronized void addRoute(SocketAddress inboundPort, SocketAddress outboundPort) throws IOException {
		Router router = new Router(inboundPort, outboundPort);
		routers.add(router);
	}

	public void close() {
		try {
			selector.close();
		} catch (IOException e) {
			// ignore
		}
	}

	private long currentTime() {
		return System.nanoTime();
	}

    public synchronized long getTotalBytesToServer() {
        long bytes = totalClient2ServerClosed;
        for(Connection conn: connections) {
            bytes += conn.client2server.totalBytes;
        }
        return bytes;
    }

    public synchronized long getTotalBytesFromServer() {
        long bytes = totalServer2ClientClosed;
        for(Connection conn: connections) {
            bytes += conn.server2client.totalBytes;
        }
        return bytes;
    }

    public synchronized long getConnectionCount() {
        return totalConnnections;
    }

    public synchronized long getConnectionLive() {
        return connections.size();
    }

    public synchronized long getLatency() {
        return TimeUnit.NANOSECONDS.toMicros(latency);
    }

    public synchronized void setLatency(long latency) {
        this.latency = TimeUnit.MICROSECONDS.toNanos(latency);
    }

    public synchronized void resetConnections() {
        for(Connection conn: connections) {
            conn.close();
        }
    }

	private interface Selectable {

		public void onSelector();
	}

	private class Router implements Selectable {

		private final SocketAddress inboundPort;
		private final SocketAddress targetPort;

		private ServerSocket socket;
		private ServerSocketChannel socketChan;

		public Router(SocketAddress inboundPort, SocketAddress targetPort) throws IOException {
			this.inboundPort = inboundPort;
			this.targetPort = targetPort;

			socketChan = ServerSocketChannel.open();
			socket = socketChan.socket();
			socket.setReuseAddress(true);
			socket.bind(inboundPort, 16);

			socketChan.configureBlocking(false);
			socketChan.register(selector, SelectionKey.OP_ACCEPT, this);
		}

		@Override
		@SuppressWarnings("resource")
		public void onSelector() {
			while(true) {
				try {
					SocketChannel clientSide = socketChan.accept();
					if (clientSide == null) {
						return;
					}
					clientSide.configureBlocking(false);

					SocketChannel serverSide = SocketChannel.open();
					serverSide.configureBlocking(false);
					serverSide.connect(targetPort);

					Connection connection = new Connection(clientSide, serverSide);
					connections.add(connection);
					connection.register();
					connection.onSelector();

					totalConnnections++;

				} catch (IOException e) {
					break;
				}
			}
		}
	}

	private class Connection implements Selectable {

		private SocketChannel clientSide;
		private SocketChannel serverSide;

		private SocketFowrader client2server;
		private SelectionKey client2serverWriteKey;
		private SocketFowrader server2client;
		private SelectionKey server2clientWriteKey;

		private boolean closed = false;

		public Connection(SocketChannel clientSide, SocketChannel serverSide) {
			this.clientSide = clientSide;
			this.serverSide = serverSide;

			client2server = new SocketFowrader(selector, clientSide, serverSide);
			server2client = new SocketFowrader(selector, serverSide, clientSide);
		}

		public boolean isClosed() {
			return closed;
		}

		public void register() throws ClosedChannelException {
			clientSide.register(selector, SelectionKey.OP_READ, this);
			serverSide.register(selector, SelectionKey.OP_CONNECT, this);
			serverSide.register(selector, SelectionKey.OP_READ, this);
		}

		@Override
		public void onSelector() {
			if (!serverSide.isOpen() || !clientSide.isOpen()) {
				close();
			}
			if (!serverSide.isConnected()) {
				try {
					serverSide.finishConnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (serverSide.isConnected()) {

				try {
					client2server.deliverOutgoing();
				} catch (IOException e) {
				    close();
				}
				try {
					server2client.deliverOutgoing();
				} catch (IOException e) {
				    close();
				}
				try {
					client2server.collectInbound();
				} catch (IOException e) {
				    close();
				}
				try {
					server2client.collectInbound();
				} catch (IOException e) {
				    close();
				}
                try {
                    client2server.deliverOutgoing();
                } catch (IOException e) {
                    close();
                }
                try {
                    server2client.deliverOutgoing();
                } catch (IOException e) {
                    close();
                }

				try {
					if (client2server.hasPendingWrites()) {
						if (client2serverWriteKey == null) {
							client2serverWriteKey = serverSide.register(selector, SelectionKey.OP_WRITE, this);
						}
					}
					else {
						if (client2serverWriteKey != null) {
							client2serverWriteKey.cancel();
						}
					}

					if (server2client.hasPendingWrites()) {
						if (server2clientWriteKey == null) {
							server2clientWriteKey = clientSide.register(selector, SelectionKey.OP_WRITE, this);
						}
					}
					else {
						if (server2clientWriteKey != null) {
							server2clientWriteKey.cancel();
						}
					}
				} catch (ClosedChannelException e) {
				}

				if (!serverSide.isOpen() || !clientSide.isOpen()) {
					close();
				}
				if (server2client.isEOF() && client2server.isEOF()) {
				    close();
				}
			}
		}

		public void onTimer() {
		    if (server2client.getPending() > 0 || client2server.getPending() > 0) {
		        onSelector();
		    }
		}

		public long getWakeupTimestamp() {
			return Math.max(server2client.getWakeupTimestamp(), client2server.getWakeupTimestamp());
		}

		public void close() {
			try {
				clientSide.close();
			}
			catch(IOException e) {
				// ignore
			}
			try {
				serverSide.close();
			}
			catch(IOException e) {
				// ignore
			}
			closed = true;
		}

		@Override
		public String toString() {
		    StringBuilder sb = new StringBuilder();
		    sb.append("C->S ");
		    sb.append(client2server.getPending());
		    sb.append("[" + (client2server.totalBytes >> 10) + "k]");
		    if (client2server.endOfStream) {
		        sb.append("EOF");
		    }
		    sb.append(" S->C ");
		    sb.append(server2client.getPending());
		    sb.append("[" + (server2client.totalBytes >> 10) + "k]");
		    if (server2client.endOfStream) {
		        sb.append("EOF");
		    }

		    if (closed) {
		        sb.append(" closed");
		    }
		    return sb.toString();
		}
	}

	private class SocketFowrader {

		private final Selector selector;

		private final SocketChannel readChannel;

		private final SocketChannel writeChannel;

		private final DelayBuffer buffer = new DelayBuffer();

		private final BandwithTracker bandWidthTracker = new BandwithTracker();

		private ByteBuffer outgoing;

		private long totalBytes;

		private boolean endOfStream;

		public SocketFowrader(Selector selector, SocketChannel readChannel, SocketChannel writeChannel) {
			this.selector = selector;
			this.readChannel = readChannel;
			this.writeChannel = writeChannel;

			this.bandWidthTracker.setTimeWindowNS(transferPeriod);
		}

		public boolean isEOF() {
            return endOfStream && getPending() == 0;
        }

        public int getPending() {
		    return (int) (buffer.pendingSize + (outgoing == null ? 0 : outgoing.remaining()));
        }

        public void collectInbound() throws IOException {
			while(buffer.pendingSize < bufferLimit) {
				ByteBuffer buf = ByteBuffer.allocate(1024);
				int n = readChannel.read(buf);
				if (n <= 0) {
					if (readChannel.socket().isInputShutdown() || n < 0) {
						endOfStream = true;
					}
					break;
				}
				buf.flip();
				buffer.push(buf, currentTime());
			}
		}

		public boolean hasPendingWrites() {
			return outgoing != null;
		}

		public void deliverOutgoing() throws IOException {
			while(true) {

				if (outgoing != null) {
					int n = writeChannel.write(outgoing);
					if (n > 0) {
						bandWidthTracker.register(currentTime(), n);
						totalBytes += n;
					}
					if (outgoing.remaining() == 0) {
						outgoing = null;
					}
					else {
						break;
					}
				}
				else {
					if (buffer.pendingSize > 0
							&& bandWidthTracker.getTransferAmount(currentTime()) < transferLimit
							&& (buffer.getHeadTimestamp() + latency) <= currentTime()) {
						outgoing = buffer.pop();
					}
					else {
						if (endOfStream && buffer.pendingSize == 0) {
							writeChannel.shutdownOutput();
						}
						break;
					}
				}
			}
		}

		public long getWakeupTimestamp() {
			if (outgoing != null || buffer.pendingSize == 0) {
				return Long.MAX_VALUE;
			}
			else {
				long delay = buffer.getHeadTimestamp() + latency;

				return delay;
			}
		}
	}


	private static class TimedPacket {

		final long inboundTimeStamp;

		final ByteBuffer data;

		public TimedPacket(long inboundTimeStamp, ByteBuffer data) {
			this.inboundTimeStamp = inboundTimeStamp;
			this.data = data;
		}
	}

	private static class DelayBuffer {

		private Deque<TimedPacket> queue = new ArrayDeque<>();

		private long pendingSize;

		public void push(ByteBuffer buffer, long timestamp) {
			queue.add(new TimedPacket(timestamp, buffer));
			pendingSize += buffer.remaining();
		}

		public ByteBuffer pop() {
			TimedPacket tp = queue.removeFirst();
			pendingSize -= tp.data.remaining();

			return tp.data;
		}

		public long getHeadTimestamp() {
			return queue.isEmpty() ? Long.MIN_VALUE : queue.getFirst().inboundTimeStamp;
		}
	}

	@MXBean
	public static interface SocketStiflerMXBean {

	    public long getTotalBytesToServer();

	    public long getTotalBytesFromServer();

	    public long getConnectionCount();

	    public long getConnectionLive();

	    public long getLatency();

	    public void setLatency(long latency);

	    public void resetConnections();
	}

	private class MXBridge implements SocketStiflerMXBean {

        @Override
        public long getTotalBytesToServer() {
            return SocketStifler.this.getTotalBytesToServer();
        }

        @Override
        public long getTotalBytesFromServer() {
            return SocketStifler.this.getTotalBytesFromServer();
        }

        @Override
        public long getConnectionCount() {
            return SocketStifler.this.getConnectionCount();
        }

        @Override
        public long getConnectionLive() {
            return SocketStifler.this.getConnectionLive();
        }

        @Override
        public long getLatency() {
            return SocketStifler.this.getLatency();
        }

        @Override
        public void setLatency(long latency) {
            SocketStifler.this.setLatency(latency);
        }

        @Override
        public void resetConnections() {
            SocketStifler.this.resetConnections();
        }
	}
}
