package com.adoc.rrhh.repository;

import com.adoc.rrhh.entity.Empleado;
import com.adoc.rrhh.entity.enums.EstadoEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    List<Empleado> findByEstado(EstadoEmpleado estado);

    Optional<Empleado> findByDui(String dui);

    Optional<Empleado> findByNit(String nit);

    List<Empleado> findByDepartamento(String departamento);
}
