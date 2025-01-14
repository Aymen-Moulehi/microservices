package tn.esprit;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.broker.producer.galaxy.GalaxySender;
import tn.esprit.dto.GalaxyDto;
import tn.esprit.error.ErrorEntity;
import tn.esprit.error.ErrorHandler;
import tn.esprit.query.GalaxyAddingRequest;
import tn.esprit.query.ServiceInfoResponse;

import java.util.Objects;


@RestController
@RequestMapping("/api/v1/galaxies")
@AllArgsConstructor
@RefreshScope
@SuppressWarnings("unused")
public class GalaxyController {
    private final GalaxyService galaxyService;
    private final Environment env;
    private GalaxySender galaxySender;

    @PostMapping
    @CircuitBreaker(name = "addGalaxy", fallbackMethod = "handleGalaxyControllerError")
    public ResponseEntity<Galaxy> addGalaxy(@RequestBody GalaxyAddingRequest galaxyAddingRequest) {
        Galaxy galaxy = galaxyService.addGalaxy(galaxyAddingRequest);
        galaxySender.publish(galaxy.getName(), galaxy.getId().intValue());
        return new ResponseEntity<>(galaxy, HttpStatus.ACCEPTED);
    }

    @GetMapping("/{galaxyId}")
    @CircuitBreaker(name = "getGalaxy", fallbackMethod = "handleGalaxyControllerError")
    public ResponseEntity<Galaxy> getGalaxy(@PathVariable("galaxyId") int galaxyId) {
        return new ResponseEntity<>(galaxyService.getGalaxyById(galaxyId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/isExist/{galaxyId}")
    @CircuitBreaker(name = "isExistGalaxy", fallbackMethod = "handleGalaxyControllerError")
    public ResponseEntity<Boolean> isExistGalaxy(@PathVariable("galaxyId") int galaxyId) {
        return new ResponseEntity<>(galaxyService.isExistGalaxy(galaxyId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/with-planets/{galaxyId}")
    @CircuitBreaker(name = "getGalaxyByIdWithRelatedPlanets", fallbackMethod = "handleGalaxyControllerError")
    public ResponseEntity<GalaxyDto> getGalaxyByIdWithRelatedPlanets(@PathVariable("galaxyId") int galaxyId) {
        String token = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest().getHeader("Authorization");
        return new ResponseEntity<>(galaxyService.getGalaxyByIdWithRelatedPlanets(galaxyId, token), HttpStatus.ACCEPTED);
    }

    @GetMapping("/info")
    @RefreshScope
    @CircuitBreaker(name = "getServiceInfo", fallbackMethod = "handleGalaxyControllerError")
    public ResponseEntity<ServiceInfoResponse> getServiceInfo() {
        String serviceName = env.getProperty("service-name");
        String description = env.getProperty("description");
        return new ResponseEntity<>(new ServiceInfoResponse(serviceName, description), HttpStatus.ACCEPTED);
    }

    public ResponseEntity<ErrorEntity> handleGalaxyControllerError (Exception exception) {
        return ErrorHandler.handleException(exception);
    }
}
