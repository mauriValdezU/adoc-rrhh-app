package com.adoc.rrhh.repository;

import com.adoc.rrhh.entity.Ausencia;
import com.adoc.rrhh.entity.enums.EstadoAusencia;
import com.adoc.rrhh.entity.enums.TipoAusencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AusenciaRepository extends JpaRepository<Ausencia, Long> {

    List<Ausencia> findByEmpleadoId(Long empleadoId);

    List<Ausencia> findByEstado(EstadoAusencia estado);

    List<Ausencia> findByTipoAusencia(TipoAusencia tipoAusencia);

    List<Ausencia> findByEstadoAndTipoAusencia(EstadoAusencia estado, TipoAusencia tipoAusencia);

    List<Ausencia> findByEmpleadoIdAndFechaInicioBetween(Long empleadoId, LocalDate desde, LocalDate hasta);

    long countByEmpleadoIdAndTipoAusenciaAndFechaInicioBetween(
            Long empleadoId, TipoAusencia tipoAusencia, LocalDate desde, LocalDate hasta);

    List<Ausencia> findAllByOrderByFechaRegistroDesc();

    List<Ausencia> findByEstadoOrderByFechaRegistroDesc(EstadoAusencia estado);

    List<Ausencia> findByTipoAusenciaOrderByFechaRegistroDesc(TipoAusencia tipoAusencia);

    List<Ausencia> findByEstadoAndTipoAusenciaOrderByFechaRegistroDesc(EstadoAusencia estado, TipoAusencia tipoAusencia);
}
