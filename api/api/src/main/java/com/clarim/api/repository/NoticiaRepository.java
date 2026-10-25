
package com.clarim.api.repository;

import com.clarim.api.model.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    List<Noticia> findTop20ByOrderByPublicadaEmDesc();
}
