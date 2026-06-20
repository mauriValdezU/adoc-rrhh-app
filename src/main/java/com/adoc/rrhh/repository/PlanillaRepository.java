package com.adoc.rrhh.repository;

import com.adoc.rrhh.entity.Planilla;
import com.adoc.rrhh.entity.enums.TipoPlanilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanillaRepository extends JpaRepository<Planilla, Long> {

    List<Planilla> findByPeriodoAndTipoPlanilla(String periodo, TipoPlanilla tipoPlanilla);

    List<Planilla> findByPeriodo(String periodo);

    List<Planilla> findByTipoPlanilla(TipoPlanilla tipoPlanilla);
}
