package info.ragozin.util.socketstifler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class BandwithTracker {

	private long timeWindowNS = TimeUnit.MILLISECONDS.toNanos(100);

	private Deque<PacketInfo> history = new ArrayDeque<PacketInfo>();

	public BandwithTracker() {
	}

	public void setTimeWindowNS(long durationNS) {
		this.timeWindowNS = durationNS;
	}

	public void register(long nowNS, int dataSize) {
		expire(nowNS);
		history.add(new PacketInfo(nowNS, dataSize));
	}

	public long getTransferAmount(long nowNS) {
		expire(nowNS);
		long total = 0;
		for(PacketInfo p: history) {
			total += p.size;
		}
		return total;
	}

	private void expire(long nowNS) {
		while(!history.isEmpty()) {
			if (history.getFirst().timestamp + timeWindowNS < nowNS) {
				history.removeFirst();
			} else {
				break;
			}
		}
	}

	private static class PacketInfo {

		private final long timestamp;

		private final int size;

		public PacketInfo(long timestamp, int size) {
			this.timestamp = timestamp;
			this.size = size;
		}
	}
}
