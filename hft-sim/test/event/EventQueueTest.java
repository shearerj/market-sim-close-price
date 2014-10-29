package event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Sets;
import com.google.common.math.LongMath;

public class EventQueueTest {
	private static final Random rand = new Random();
	private EventQueue queue;

	@Before
	public void setup() throws IOException {
		queue = EventQueue.create(Log.nullLogger(), rand);
	}

	@Test
	public void emptyTest() {
		assertEquals(TimeStamp.ZERO, queue.getCurrentTime());
	}

	@Test
	public void executeUntil() {
		final AtomicBoolean first = new AtomicBoolean(false);
		final AtomicBoolean second = new AtomicBoolean(false);

		queue.scheduleActivityIn(TimeStamp.of(10), new Activity() {
			@Override public void execute() { first.set(true); }
		});
		queue.scheduleActivityIn(TimeStamp.of(20), new Activity() {
			@Override public void execute() { second.set(true); }
		});

		queue.executeUntil(TimeStamp.of(9));
		assertFalse(first.get());

		queue.executeUntil(TimeStamp.of(10));
		assertTrue(first.get());
		assertFalse(second.get());

		queue.executeUntil(TimeStamp.of(20));
		assertTrue(second.get());
	}

	/** Activities that insert new activities */
	@Test
	public void chainingActivityExecution() {
		final AtomicBoolean first = new AtomicBoolean(false);
		final AtomicBoolean second = new AtomicBoolean(false);

		queue.scheduleActivityIn(TimeStamp.of(10), new Activity() {
			@Override public void execute() {
				first.set(true);
				queue.scheduleActivityIn(TimeStamp.of(10), new Activity() {
					@Override public void execute() {
						second.set(true);
					}
				});
			}
		});

		queue.executeUntil(TimeStamp.of(9));
		assertFalse(first.get());

		queue.executeUntil(TimeStamp.of(10));
		assertTrue(first.get());
		assertFalse(second.get());

		queue.executeUntil(TimeStamp.of(20));
		assertTrue(second.get());
	}
	
	/** Test instanious happen first */
	@Test
	public void instaniousTest() {
		final AtomicBoolean slow = new AtomicBoolean(false);
		
		queue.scheduleActivityIn(TimeStamp.ZERO, new Activity() {
			@Override public void execute() { slow.set(true); }
		});
		
		queue.scheduleActivityIn(TimeStamp.IMMEDIATE, new Activity() {
			@Override public void execute() {
				if (slow.get())
					fail("Instantanious happened second");
			}
		});
	}
	
	/** Test that events scheduled at the same time get scheduled in order */
	@Test
	public void scheduledInOrder() {
		int n = 10;
		for (int i = 0; i < 1000; ++i) {
			final AtomicInteger sequence = new AtomicInteger(0);
			for (int j = 0; j < n; ++j) {
				final int k = j;
				queue.scheduleActivityIn(TimeStamp.ZERO, new Activity() {
					@Override public void execute() {
						if (k != sequence.get())
							fail("Out of order");
						sequence.set(k + 1);
					}
				});
			}
			queue.executeUntil(TimeStamp.ZERO);
		}
	}
	
	/**
	 * Test that future events are randomly scheduled
	 * 
	 * This looks complicated, but each event has to be created from it's own
	 * activity or they'll get grouped together and executed in order.
	 */
	@Test
	public void randomOrdering() {
		int n = 3;
		Set<Integer> listHashes = Sets.newHashSet();
		
		for (int i = 0; i < 1000; ++i) {
			final Builder<Integer> builder = ImmutableList.builder();
			for (int j = 0; j < n; ++j) {
				final int k = j;
				queue.scheduleActivityIn(TimeStamp.IMMEDIATE, new Activity() {
					@Override public void execute() {
						queue.scheduleActivityIn(TimeStamp.ZERO, new Activity() {
							@Override public void execute() { builder.add(k); }
						});
					}
				});
			}
			queue.executeUntil(TimeStamp.ZERO);
			listHashes.add(builder.build().hashCode());
		}
		
		assertEquals(LongMath.factorial(n), listHashes.size());
	}
	
	/**
	 * Test that future events are randomly scheduled
	 * 
	 * This looks complicated, but essentially whats happening is that this
	 * schedules n activities, which each schedule m more activities. Each
	 * activity makes sure that it happened in the proper order, or it fails.
	 */
	@Test
	public void immediateOrdering() {
		final int n = 3, m = 2;
		for (int i = 0; i < 1000; ++i) {
			final AtomicInteger sequence = new AtomicInteger(0);
			for (int j = 0; j < n; ++j) {
				final int k = j;
				queue.scheduleActivityIn(TimeStamp.IMMEDIATE, new Activity() {
					@Override public void execute() {
						for (int l = 0; l < m; ++l) {
							final int p = l;
							queue.scheduleActivityIn(TimeStamp.IMMEDIATE, new Activity() {
								@Override public void execute() {
									int q = k * m + p;
									if (q != sequence.get())
										fail("Out of order");
									sequence.set(q + 1);
								}
							});
						}
					}
				});
			}
			queue.executeUntil(TimeStamp.IMMEDIATE);
		}
	}
	
}
