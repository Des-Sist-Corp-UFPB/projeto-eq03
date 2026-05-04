package br.ufpb.dsc.chamados.service;

import br.ufpb.dsc.chamados.domain.Usuario;
import br.ufpb.dsc.chamados.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementação de UserDetailsService que busca usuários do banco de dados.
 *
 * O Spring Security usa este serviço para carregar dados do usuário
 * durante autenticação e autorização.
 *
 * Busca por matrícula (username).
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String matricula) throws UsernameNotFoundException {
        log.debug("Buscando usuário com matrícula: {}", matricula);

        Usuario usuario = usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado com matrícula: {}", matricula);
                    return new UsernameNotFoundException("Usuário não encontrado: " + matricula);
                });

        if (!usuario.getAtivo()) {
            log.warn("Tentativa de login com usuário inativo: {}", matricula);
            throw new UsernameNotFoundException("Usuário inativo: " + matricula);
        }

        log.debug("Usuário carregado com sucesso: {}", matricula);

        // Retorna UserDetails com credenciais do banco
        return User.builder()
                .username(usuario.getMatricula())
                .password(usuario.getSenha())  // Será comparado com encode + salt via AuthenticationManager
                .authorities("ROLE_USER")       // Papel padrão para todos os usuários
                .disabled(!usuario.getAtivo())  // Spring Security 6+: desabilita se inativo
                .build();
    }
}
