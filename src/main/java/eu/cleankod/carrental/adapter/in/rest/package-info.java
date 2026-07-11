/**
 * Minimal REST adapter exposing {@link eu.cleankod.carrental.application.port.in.ReserveCarUseCase}:
 * one endpoint to create a reservation, plus a global exception handler mapping domain exceptions and
 * request-parsing/validation failures to a consistent error response.
 */
package eu.cleankod.carrental.adapter.in.rest;
