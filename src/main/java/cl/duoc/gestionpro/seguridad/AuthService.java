package cl.duoc.gestionpro.seguridad;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.seguridad.dto.SesionResponse;
import cl.duoc.gestionpro.seguridad.dto.UsuarioDto;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long horasValidez;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            @Value("${app.jwt.horas-validez}") long horasValidez) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.horasValidez = horasValidez;
    }

    @Transactional(readOnly = true)
    public SesionResponse login(String correo, String clave) {
        Usuario u = usuarios.findByCorreoAndActivoTrue(correo.trim().toLowerCase())
                .filter(x -> passwordEncoder.matches(clave, x.getClaveHash()))
                .orElseThrow(() -> new ReglaNegocioException("CREDENCIALES", "El correo o la contraseña no coinciden."));

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gestion-pro")
                .subject(u.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(horasValidez, ChronoUnit.HOURS))
                .claim("nombre", u.getNombre())
                .claim("rol", u.getRol().name())
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new SesionResponse(token, UsuarioDto.de(u));
    }
}
