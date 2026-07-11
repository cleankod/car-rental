package eu.cleankod.carrental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalizes the limited-inventory numbers (see {@code car-rental.fleet.*} in {@code application.yml})
 * rather than hardcoding the fleet size in the wiring itself.
 */
@ConfigurationProperties(prefix = "car-rental.fleet")
public record FleetProperties(int sedanUnits, int suvUnits, int vanUnits) {
}
