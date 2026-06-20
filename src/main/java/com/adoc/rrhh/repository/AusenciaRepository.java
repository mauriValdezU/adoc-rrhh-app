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

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Ausencia a WHERE a.empleado.id = :empleadoId AND a.fechaInicio <= :hasta AND a.fechaFin >= :desde")
    List<Ausencia> findOverlappingAusencias(@org.springframework.data.repository.query.Param("empleadoId") Long empleadoId, 
                                            @org.springframework.data.repository.query.Param("desde") LocalDate desde, 
                                            @org.springframework.data.repository.query.Param("hasta") LocalDate hasta);

    long countByEmpleadoIdAndTipoAusenciaAndFechaInicioBetween(
            Long empleadoId, TipoAusencia tipoAusencia, LocalDate desde, LocalDate hasta);

    List<Ausencia> findAllByOrderByFechaRegistroDesc();

    List<Ausencia> findByEstadoOrderByFechaRegistroDesc(EstadoAusencia estado);

    List<Ausencia> findByTipoAusenciaOrderByFechaRegistroDesc(TipoAusencia tipoAusencia);

    List<Ausencia> findByEstadoAndTipoAusenciaOrderByFechaRegistroDesc(EstadoAusencia estado,
            TipoAusencia tipoAusencia);
}
