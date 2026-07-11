/**
 * In-memory implementation of the outbound persistence port — no external database, per this
 * assignment's decision to simulate persistence rather than stand up a real one. Concurrency-safety
 * (atomic allocation under concurrent reservation attempts) is added in the {@code in-memory-persistence}
 * stage, not here.
 */
package eu.cleankod.carrental.adapter.out.persistence;
