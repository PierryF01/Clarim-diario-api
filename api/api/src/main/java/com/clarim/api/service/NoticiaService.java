
package com.clarim.api.service;

import com.clarim.api.dto.NoticiaRequest;
import com.clarim.api.dto.NoticiaResponse;
import com.clarim.api.dto.NoticiaResposta;
import com.clarim.api.dto.NoticiaResumo;
import com.clarim.api.exception.RecursoNaoEncontradoException;
import com.clarim.api.model.*;
import com.clarim.api.repository.AssinaturaRepository;
import com.clarim.api.repository.CategoriaRepository;
import com.clarim.api.repository.NoticiaRepository;
import com.clarim.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NoticiaService {

    private final NoticiaRepository noticiaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaRepository assinaturaRepository;

    public NoticiaService(
            NoticiaRepository noticiaRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            AssinaturaRepository assinaturaRepository) {
        this.noticiaRepository = noticiaRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    @Transactional(readOnly = true)
    public List<NoticiaResumo> listarTodas() {
        return noticiaRepository
                .findTop22ByTituloNotContainingIgnoreCaseAndPublicadaEmIsNotNullOrderByPublicadaEmDesc(
                        "noticia de teste"
                )
                .stream()
                .map(this::paraDto)
                .toList();
    }

    private NoticiaResumo paraDto(Noticia noticia) {
        return new NoticiaResumo(
                noticia.getId(),
                noticia.getTitulo(),
                noticia.getSlug(),
                noticia.getResumo(),
                noticia.getCategoria().getNome(),
                noticia.getTexto(),
                noticia.getPremium(),
                noticia.getPublicadaEm()
        );
    }

    @Transactional(readOnly = true)
    public NoticiaResposta buscarPorId(Long id, Usuario leitor) {
        Noticia n = noticiaRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Notícia não encontrada: " + id));

        boolean podeLer = !n.getPremium() || temAcessoPremium(leitor);

        return new NoticiaResposta(
                n.getId(),
                n.getTitulo(),
                n.getSlug(),
                n.getResumo(),
                podeLer ? n.getTexto() : null,
                n.getCategoria().getNome(),
                n.getUsuario().getNome(),
                n.getPremium(),
                n.getPublicadaEm(),
                !podeLer
        );
    }

    private boolean temAcessoPremium(Usuario leitor) {
        if (leitor == null) {
            return false;
        }

        if (leitor.getPapel() != Papel.LEITOR) {
            return true;
        }

        return assinaturaRepository
                .existsByUsuarioIdAndStatusInAndPeriodoFimAfter(
                        leitor.getId(),
                        List.of(
                                StatusAssinatura.ACTIVE,
                                StatusAssinatura.TRIALING
                        ),
                        OffsetDateTime.now()
                );
    }

    public NoticiaResponse criar(NoticiaRequest req) {
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Categoria não encontrada"));

        Usuario autor = usuarioRepository.findById(req.autorId())
                .orElseThrow();

        if (autor.getPapel() == Papel.LEITOR) {
            throw new IllegalArgumentException(
                    "Usuário não tem permissão para criar notícias");
        }

        Noticia noticia = new Noticia();
        noticia.setTitulo(req.titulo());
        noticia.setSlug(req.slug());
        noticia.setResumo(req.resumo());
        noticia.setTexto(req.texto());
        noticia.setPremium(req.premium());
        noticia.setCategoria(categoria);
        noticia.setUsuario(autor);

        Noticia salva = noticiaRepository.save(noticia);

        return new NoticiaResponse(
                salva.getId(),
                salva.getTitulo(),
                salva.getSlug(),
                salva.getResumo(),
                salva.getTexto(),
                salva.getCategoria().getNome(),
                salva.getUsuario().getNome(),
                salva.getPremium(),
                salva.getPublicadaEm()
        );
    }
}
