package eu.cleankod.carrental.config;

import eu.cleankod.carrental.adapter.out.persistence.InMemoryCarInventoryRepository;
import eu.cleankod.carrental.application.ReservationService;
import eu.cleankod.carrental.application.port.in.ReserveCarUseCase;
import eu.cleankod.carrental.application.port.out.CarInventoryRepository;
import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.CarTypeInventory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FleetProperties.class)
public class CarRentalConfiguration {

    @Bean
    public CarInventoryRepository carInventoryRepository(FleetProperties fleetProperties) {
        return new InMemoryCarInventoryRepository(
                new CarTypeInventory(CarType.SEDAN, fleetProperties.sedanUnits()),
                new CarTypeInventory(CarType.SUV, fleetProperties.suvUnits()),
                new CarTypeInventory(CarType.VAN, fleetProperties.vanUnits()));
    }

    @Bean
    public ReserveCarUseCase reserveCarUseCase(CarInventoryRepository carInventoryRepository) {
        return new ReservationService(carInventoryRepository);
    }
}
