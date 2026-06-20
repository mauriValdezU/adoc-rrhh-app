package com.adoc.rrhh.repository;

import com.adoc.rrhh.entity.DetallePlanilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetallePlanillaRepository extends JpaRepository<DetallePlanilla, Long> {

    List<DetallePlanilla> findByPlanillaId(Long planillaId);

    List<DetallePlanilla> findByEmpleadoId(Long empleadoId);

    boolean existsByEmpleadoIdAndPlanillaPeriodoAndPlanillaQuincenaAndPlanillaTipoPlanilla(
            Long empleadoId, String periodo, Integer quincena, com.adoc.rrhh.entity.enums.TipoPlanilla tipoPlanilla);
}
