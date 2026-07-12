package cl.duoc.login.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import cl.duoc.login.configuration.JwtProperties;

// Pruebas unitarias puras del generador de tokens: no se levanta contexto de Spring.
class JwtServiceTest {

    private JwtProperties jwtProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("secreto-de-prueba-unitaria");
        jwtProperties.setExpiration(3600000L); // 1 hora.
        jwtService = new JwtService(jwtProperties);
    }

    @Test // Caso: el token generado debe estar firmado con el secreto configurado y ser verificable.
    void generarToken_debeGenerarTokenVerificableConElSecretoConfigurado() {
        // GIVEN / WHEN.
        String token = jwtService.generarToken("admin", "ADMIN");

        // THEN: si la firma no coincidiera, verify() lanzaria JWTVerificationException.
        DecodedJWT decodificado = JWT.require(Algorithm.HMAC256("secreto-de-prueba-unitaria"))
                .withIssuer("login-service")
                .build()
                .verify(token);

        assertThat(decodificado.getSubject()).isEqualTo("admin");
    }

    @Test // Caso: el rol debe viajar en el claim "roles" con el prefijo ROLE_.
    void generarToken_debeIncluirRolConPrefijoRole() {
        // GIVEN / WHEN.
        String token = jwtService.generarToken("cajero1", "CAJERO");
        DecodedJWT decodificado = JWT.decode(token);

        // THEN.
        assertThat(decodificado.getClaim("roles").asList(String.class))
                .containsExactly("ROLE_CAJERO");
    }

    @Test // Caso: la expiracion debe ser posterior a la fecha de emision (token con vigencia).
    void generarToken_debeGenerarTokenConExpiracionFutura() {
        // GIVEN / WHEN.
        String token = jwtService.generarToken("admin", "ADMIN");
        DecodedJWT decodificado = JWT.decode(token);

        // THEN.
        assertThat(decodificado.getExpiresAt()).isAfter(decodificado.getIssuedAt());
    }

    @Test // Caso: el emisor del token debe identificar al microservicio de autenticacion.
    void generarToken_debeIncluirIssuerDelServicio() {
        // GIVEN / WHEN.
        String token = jwtService.generarToken("admin", "ADMIN");
        DecodedJWT decodificado = JWT.decode(token);

        // THEN.
        assertThat(decodificado.getIssuer()).isEqualTo("login-service");
    }
}
