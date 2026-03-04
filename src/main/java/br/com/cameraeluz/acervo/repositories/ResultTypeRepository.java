package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.ResultType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultTypeRepository extends JpaRepository<ResultType, Long> {
}