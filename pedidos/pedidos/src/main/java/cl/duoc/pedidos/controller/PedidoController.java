package cl.duoc.pedidos.controller;

import cl.duoc.pedidos.dto.request.PedidoRequest;
import cl.duoc.pedidos.dto.response.PedidoResponse;
import cl.duoc.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearPedido(request));
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }
}