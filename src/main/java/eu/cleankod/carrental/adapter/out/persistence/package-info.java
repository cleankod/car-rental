/**
 * In-memory implementation of the outbound persistence port — no external database, per this
 * project's decision to simulate persistence rather than stand up a real one. Concurrency-safe per
 * car type: see {@link InMemoryCarInventoryRepository} for the locking strategy.
 */
package eu.cleankod.carrental.adapter.out.persistence;
