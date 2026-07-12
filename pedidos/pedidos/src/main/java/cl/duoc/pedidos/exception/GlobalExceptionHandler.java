package cl.duoc.pedidos.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Regla de negocio rechazada en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Solicitud incorrecta")
                .mensajes(List.of(ex.getMessage()))
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidaciones(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> mensajes = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.warn("Error de validacion en {}: {}", request.getRequestURI(), mensajes);

        ErrorResponse error = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Error de validacion")
                .mensajes(mensajes)
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponse> manejarRecursoRemotoNoEncontrado(
            FeignException.NotFound ex,
            HttpServletRequest request
    ) {
        log.warn("Recurso remoto no encontrado al procesar {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estado(HttpStatus.NOT_FOUND.value())
                .error("Recurso remoto no encontrado")
                .mensajes(List.of("El usuario o producto asociado al pedido no existe en el servicio remoto"))
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> manejarErrorComunicacionRemota(
            FeignException ex,
            HttpServletRequest request
    ) {
        log.error("Fallo de comunicacion con un microservicio remoto en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estado(HttpStatus.BAD_GATEWAY.value())
                .error("Error de comunicacion entre microservicios")
                .mensajes(List.of("No fue posible obtener la informacion remota necesaria para procesar el pedido"))
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estado(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error interno del servidor")
                .mensajes(List.of("Ocurrio un error inesperado"))
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
