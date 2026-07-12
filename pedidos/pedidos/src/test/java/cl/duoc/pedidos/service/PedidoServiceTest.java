package cl.duoc.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pedidos.client.ProductoClient;
import cl.duoc.pedidos.client.UsuarioClient;
import cl.duoc.pedidos.client.dto.ProductoDTO;
import cl.duoc.pedidos.client.dto.UsuarioDTO;
import cl.duoc.pedidos.dto.request.PedidoRequest;
import cl.duoc.pedidos.dto.response.PedidoResponse;
import cl.duoc.pedidos.model.PedidoModel;
import cl.duoc.pedidos.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository; // Persistencia local de Pedidos.

    @Mock
    private ProductoClient productoClient; // Cliente Feign hacia productos-service (se simula, no se llama por red).

    @Mock
    private UsuarioClient usuarioClient; // Cliente Feign hacia usuarios-service (se simula, no se llama por red).

    @InjectMocks
    private PedidoService pedidoService;

    private UsuarioDTO crearUsuarioDto(Long id, boolean activo) {
        UsuarioDTO usuario = new UsuarioDTO();
        usuario.setIdUsuario(id);
        usuario.setUsername("cliente_test");
        usuario.setEmail("cliente@test.cl");
        usuario.setActivo(activo);
        return usuario;
    }

    private ProductoDTO crearProductoDto(Long id, int precio, boolean activo) {
        ProductoDTO producto = new ProductoDTO();
        producto.setIdProducto(id);
        producto.setNombre("Cafe Latte");
        producto.setPrecio(precio);
        producto.setActivo(activo);
        return producto;
    }

    private PedidoRequest crearRequest(Long idUsuario, Long idProducto, int cantidad) {
        PedidoRequest request = new PedidoRequest();
        request.setIdUsuario(idUsuario);
        request.setIdProducto(idProducto);
        request.setCantidad(cantidad);
        return request;
    }

    @Test // Caso feliz: usuario activo + producto activo -> se calcula el total y se guarda el pedido.
    void crearPedido_debeCrearPedidoYCalcularTotalCuandoUsuarioYProductoSonValidos() {
        // GIVEN.
        when(usuarioClient.obtenerUsuarioPorId(1L)).thenReturn(crearUsuarioDto(1L, true));
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(crearProductoDto(10L, 2500, true));
        PedidoModel pedidoGuardado = PedidoModel.builder()
                .idPedido(100L)
                .idUsuario(1L)
                .idProducto(10L)
                .cantidad(2)
                .total(5000)
                .estado("PENDIENTE")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(pedidoRepository.save(any(PedidoModel.class))).thenReturn(pedidoGuardado);

        // WHEN.
        PedidoResponse resultado = pedidoService.crearPedido(crearRequest(1L, 10L, 2));

        // THEN: el total debe ser precio * cantidad (2500 * 2 = 5000).
        assertThat(resultado.getTotal()).isEqualTo(5000);
        assertThat(resultado.getEstado()).isEqualTo("PENDIENTE");
        verify(usuarioClient).obtenerUsuarioPorId(1L);
        verify(productoClient).obtenerProductoPorId(10L);
        verify(pedidoRepository).save(any(PedidoModel.class));
    }

    @Test // Caso de error: el usuario remoto esta inactivo, no debe crearse el pedido.
    void crearPedido_debeLanzarExcepcionCuandoUsuarioEstaInactivo() {
        // GIVEN.
        when(usuarioClient.obtenerUsuarioPorId(1L)).thenReturn(crearUsuarioDto(1L, false));

        // WHEN + THEN.
        assertThatThrownBy(() -> pedidoService.crearPedido(crearRequest(1L, 10L, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactivo");

        verify(productoClient, never()).obtenerProductoPorId(any());
        verify(pedidoRepository, never()).save(any(PedidoModel.class));
    }

    @Test // Caso de error: el producto remoto no esta disponible, no debe crearse el pedido.
    void crearPedido_debeLanzarExcepcionCuandoProductoNoEstaDisponible() {
        // GIVEN.
        when(usuarioClient.obtenerUsuarioPorId(1L)).thenReturn(crearUsuarioDto(1L, true));
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(crearProductoDto(10L, 2500, false));

        // WHEN + THEN.
        assertThatThrownBy(() -> pedidoService.crearPedido(crearRequest(1L, 10L, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disponible");

        verify(pedidoRepository, never()).save(any(PedidoModel.class));
    }

    @Test // Caso: listar pedidos enriquece cada registro local con datos remotos de usuario y producto.
    void listarPedidos_debeEnriquecerCadaPedidoConDatosRemotos() {
        // GIVEN.
        PedidoModel pedido = PedidoModel.builder()
                .idPedido(100L)
                .idUsuario(1L)
                .idProducto(10L)
                .cantidad(1)
                .total(2500)
                .estado("PENDIENTE")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        when(usuarioClient.obtenerUsuarioPorId(1L)).thenReturn(crearUsuarioDto(1L, true));
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(crearProductoDto(10L, 2500, true));

        // WHEN.
        List<PedidoResponse> resultado = pedidoService.listarPedidos();

        // THEN.
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUsuario().getIdUsuario()).isEqualTo(1L);
        assertThat(resultado.get(0).getProducto().getIdProducto()).isEqualTo(10L);
    }
}
