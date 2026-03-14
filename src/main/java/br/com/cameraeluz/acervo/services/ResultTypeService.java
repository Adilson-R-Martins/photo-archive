package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.models.ResultType;
import br.com.cameraeluz.acervo.repositories.ResultTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultTypeService {

    private final ResultTypeRepository resultTypeRepository;

    public List<ResultType> findAll() {
        return resultTypeRepository.findAll();
    }

    public ResultType create(ResultType resultType) {
        return resultTypeRepository.save(resultType);
    }
}