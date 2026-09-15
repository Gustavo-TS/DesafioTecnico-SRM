package com.srm.creditengine.repository;

import com.srm.creditengine.model.Liquidacao;
import com.srm.creditengine.model.Moeda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiquidacaoRepository extends JpaRepository<Liquidacao, UUID> {

    Optional<Liquidacao> findByIdempotencyKey(String idempotencyKey);

    boolean existsByRecebivelId(UUID recebivelId);

    @Query("""
    	    select l
    	    from Liquidacao l
    	    where l.liquidadoEm >= coalesce(:dataInicio, l.liquidadoEm)
    	      and l.liquidadoEm <= coalesce(:dataFim, l.liquidadoEm)
    	      and l.recebivel.cedente.id = coalesce(:cedenteId, l.recebivel.cedente.id)
    	      and l.moedaPagamento = coalesce(:moeda, l.moedaPagamento)
    	    order by l.liquidadoEm desc
    	""")
    	List<Liquidacao> buscarComFiltros(
    	        @Param("dataInicio") OffsetDateTime dataInicio,
    	        @Param("dataFim") OffsetDateTime dataFim,
    	        @Param("cedenteId") UUID cedenteId,
    	        @Param("moeda") Moeda moeda
    	);
}